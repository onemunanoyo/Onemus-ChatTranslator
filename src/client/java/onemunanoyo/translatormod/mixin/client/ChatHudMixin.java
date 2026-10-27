package onemunanoyo.translatormod.mixin.client;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import onemunanoyo.translatormod.TranslatorModClient;
import onemunanoyo.translatormod.command.TranslateCommand;
import onemunanoyo.translatormod.config.TranslatorConfig;
import onemunanoyo.translatormod.util.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-ORx]");

    @Inject(
            method = "addToMessageHistory(Ljava/lang/String;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddToMessageHistory(String message, CallbackInfo ci) {
        if (TranslateCommand.shouldSkipMessageHistory(message)) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text injectTranslateButton(Text message) {
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
        String safeCommand = TranslateCommand.buildTranslateCommand(translateTarget);

        MutableText button = I18n.translate("translatormod.chat.button").copy()
                .styled(style -> style
                        .withClickEvent(new ClickEvent.RunCommand(safeCommand))
                        .withFormatting(Formatting.GRAY)
                );

        // 既存のボタンがすでにある場合は付与しない（重複防止）
        String buttonLabel = STRIP_COLOR_PATTERN.matcher(I18n.getString("translatormod.chat.button")).replaceAll("").trim();
        if (rawText.contains(buttonLabel)) {
            // System.out.println("[DEBUG_LOG] Message already contains button label: " + buttonLabel);
            return message;
        }

        // 元の Text コンポーネントの末尾にボタンを付与する。
        // Text.literal() で再構築しないことで Hypixel のランク色など元の色情報を保持する。
        MutableText result = message.copy().append(Text.literal(" ")).append(button);
        return result;
    }

    // 既存の shouldIgnore メソッドは削除する（TranslatorModClient のものを使う）
}
