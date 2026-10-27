package onemunanoyo.translatormod.mixin.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import onemunanoyo.translatormod.TranslatorModClient;
import onemunanoyo.translatormod.config.TranslatorConfig;
import onemunanoyo.translatormod.util.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

@Mixin(ChatComponent.class)
public class ChatHudMixin {

    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-ORx]");

    @Inject(
            method = "addRecentChat(Ljava/lang/String;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddRecentChat(String message, CallbackInfo ci) {
        if (message != null && message.trim().startsWith("/translate key ")) {
            ci.cancel();
        }
    }

    /**
     * 文字列を Minecraft のコマンド引数（ダブルクォート囲み）として安全にエスケープする。
     * ダブルクォートとバックスラッシュをエスケープし、制御文字を取り除く。
     */
    private String escapeCommandArg(String input) {
        if (input == null) return "";
        // 改行・タブ等の制御文字を除去してからエスケープ（コマンド破壊防止）
        return input.replaceAll("[\\x00-\\x1F\\x7F]", "")
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
    }

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component injectTranslateButton(Component message) {
        if (message == null) return null;
        String content = message.getString();

        TranslatorConfig config = TranslatorConfig.getInstance();
        if (config.apiKey.isBlank()) {
            return message;
        }

        if (TranslatorModClient.shouldIgnore(content)) {
            return message;
        }

        // Hypixel モード時の判定
        if (config.hypixelMode) {
            String mode = TranslatorModClient.getHypixelChannelMode(content);
            // AUTO は GAMEイベントハンドラの autoTranslate() が担当するためボタン不要
            // OFF はそもそも翻訳しない
            if ("OFF".equals(mode) || "AUTO".equals(mode)) return message;
        }

        // 翻訳対象のテキスト抽出（プレイヤー名を除去しようと試みる）
        // 一般的なチャット形式 "<Player> Message" or "Player: Message"
        String rawText = STRIP_COLOR_PATTERN.matcher(content).replaceAll("").trim();
        String translateTarget = rawText;
        int colonIndex = rawText.indexOf(": ");
        if (colonIndex != -1) {
            translateTarget = rawText.substring(colonIndex + 2).trim();
        } else if (rawText.startsWith("<") && rawText.contains("> ")) {
            int bracketEnd = rawText.indexOf("> ");
            translateTarget = rawText.substring(bracketEnd + 2).trim();
        }

        // スペースのみ、または短すぎるメッセージは除外
        if (translateTarget.length() <= 2) {
            return message;
        }

        // ボタンの作成
        String truncated = translateTarget.length() > 200 ? translateTarget.substring(0, 200) : translateTarget;
        String safeCommand = "/tr \"" + escapeCommandArg(truncated) + "\"";

        MutableComponent button = I18n.translate("translatormod.chat.button").copy()
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand(safeCommand))
                        .withColor(ChatFormatting.GRAY)
                );

        // 既存のボタンがすでにある場合は付与しない（重複防止）
        String buttonLabel = STRIP_COLOR_PATTERN.matcher(I18n.getString("translatormod.chat.button")).replaceAll("").trim();
        if (rawText.contains(buttonLabel)) {
            return message;
        }

        // 元の Component の末尾にボタンを付与する。
        // Component.literal() で再構築しないことで Hypixel のランク色など元の色情報を保持する。
        MutableComponent result = message.copy().append(Component.literal(" ")).append(button);
        return result;
    }
}

