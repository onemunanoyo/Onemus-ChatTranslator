package onemunanoyo.translatormod;

import onemunanoyo.translatormod.command.TranslateCommand;
import onemunanoyo.translatormod.config.TranslatorConfig;
import onemunanoyo.translatormod.config.encryption.EncryptionNotice;
import onemunanoyo.translatormod.gui.TranslatorKeyBinding;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import onemunanoyo.translatormod.util.I18n;
import java.util.regex.Pattern;

public class TranslatorModClient implements ClientModInitializer {

	private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-ORx]");

	@Override
	public void onInitializeClient() {
		TranslateCommand.register();
		TranslatorKeyBinding.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			EncryptionNotice.Notice notice = EncryptionNotice.poll();
			if (notice != null) {
				client.player.sendSystemMessage(I18n.translate(notice.translationKey(), notice.args()));
			}
		});

		// 公開チャット
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			String content = message.getString();
			if (shouldIgnore(content)) return;

			// 自分のチャットはスキップ
			Minecraft client = Minecraft.getInstance();
			if (sender != null && client.player != null
					&& sender.id().equals(client.player.getGameProfile().id())) return;

			TranslatorConfig config = TranslatorConfig.getInstance();
			if (!config.enabled) return;

 		// Hypixel モード時はシステムメッセージ（sender=null）をスキップ
 		if (config.hypixelMode && sender == null) return;
		
 		// addTranslateButton(client, content); // Mixin で追加するため不要
 	});

		// システムメッセージ
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay) return;
			
			String content = message.getString();
			if (shouldIgnore(content)) return;

			TranslatorConfig config = TranslatorConfig.getInstance();
			if (!config.enabled) return;
			
			// 通常モードかつシステムメッセージ翻訳OFFなら何もしない
			if (!config.hypixelMode && !config.translateSystemMessages) return;

			Minecraft client = Minecraft.getInstance();

			if (config.hypixelMode) {
				String mode = getHypixelChannelMode(content);
				if ("OFF".equals(mode)) return;
				
				if ("AUTO".equals(mode)) {
					autoTranslate(client, content);
				} else if ("CLICK".equals(mode)) {
					// addTranslateButton(client, content); // Mixin で追加するため不要
				}
			} else {
				// addTranslateButton(client, content); // Mixin で追加するため不要
			}
		});
	}

	/** 無限ループ防止のため翻訳対象外かどうか判定する */
	public static boolean shouldIgnore(String content) {
		if (content == null || content.isBlank()) return true;
		
		// 色記号 § を除いたプレーンテキストで比較するために正規表現で除去
		String plain = STRIP_COLOR_PATTERN.matcher(content).replaceAll("");
		
		// 翻訳済みメッセージ（プレフィックス [訳] や [Tr] など）を再翻訳しない
		String translatedPrefix = I18n.getString("translatormod.chat.translated_prefix");
		if (plain.contains(translatedPrefix)) return true;
		
		// [翻訳中...] もスキップする
		String waitingMsg = I18n.getString("translatormod.command.trt.waiting");
		if (plain.contains(waitingMsg)) return true;
		
		// 自分の翻訳コマンド結果などを無視
		String msgPrefix = I18n.getString("translatormod.msg.prefix");
		String usageInfo = I18n.getString("translatormod.usage.info");
		// フォーマット文字列の最初の動的部分（%）より前を静的ラベルとして使う
		int pctIdx = usageInfo.indexOf('%');
		String usageLabel = pctIdx > 0 ? usageInfo.substring(0, pctIdx).trim() : usageInfo;
        if (plain.startsWith(msgPrefix) && !plain.contains(usageLabel)) {
            return true;
        }

        // ボタンそのものを無視の対象にはしない（Mixin側で判断する）
        // ただし、既にボタンが付いているメッセージに対して、再度 addMessage が呼ばれた場合に
        // 無限ループにならないようにする必要がある。
        // 現状、Mixin は addMessage の引数を書き換えるだけなので、再帰的に addMessage を呼ばない限り安全。

        return false;
    }

	/**
	 * 自動翻訳してチャットに表示する。
	 */
	private static void autoTranslate(Minecraft client, String content) {
		TranslationHandler.translateAsync(content).thenAccept(translated -> {
			if (translated == null) return;
			client.execute(() -> {
				if (client.player == null) return;
				if (translated.startsWith(TranslationHandler.ERR_SENTINEL)) {
					String errorMsg = translated.substring(TranslationHandler.ERR_SENTINEL.length());
					client.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("§c" + errorMsg));
					return;
				}
				client.player.sendSystemMessage(
						I18n.translate("translatormod.chat.translated", translated)
				);
			});
		});
	}

	/**
	 * チャットに [訳?] クリッカブルボタンを追加する。
	 * クリックすると /trt <原文> が実行されて翻訳結果が表示される。
	 */


	/**
	 * Hypixel モード時にチャンネルのモードを返す。
	 * @return "AUTO" / "CLICK" / "OFF"
	 */
	public static String getHypixelChannelMode(String raw) {
		TranslatorConfig config = TranslatorConfig.getInstance();
		String text = STRIP_COLOR_PATTERN.matcher(raw).replaceAll("").trim();

		if (text.startsWith("Party >"))                                    return config.hypixelParty;
		if (text.startsWith("Guild >"))                                    return config.hypixelGuild;
		if (text.startsWith("From ") || text.startsWith("To "))           return config.hypixelDM;
		if (text.contains(": ")) {
			String[] parts = text.split(": ", 2);
			if (parts.length > 0 && parts[0].matches("^[A-Za-z0-9_]{3,16}$")) {
				return config.hypixelAllChat;
			}
		}
		// バニラ形式 <PlayerName> message もAll Chatとして扱う
		if (text.startsWith("<") && text.contains("> "))                  return config.hypixelAllChat;

		return "OFF";
	}
}

