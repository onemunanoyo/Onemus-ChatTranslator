package onemunanoyo.translatormod;

import onemunanoyo.translatormod.command.TranslateCommand;
import onemunanoyo.translatormod.config.TranslatorConfig;
import onemunanoyo.translatormod.gui.TranslatorKeyBinding;
import onemunanoyo.translatormod.util.I18n;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.regex.Pattern;

public class TranslatorModClient {

	private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

	/**
	 * NeoForge エントリーポイント。TranslatorMod のコンストラクタから呼ばれる（クライアント時のみ）。
	 */
	public static void init(IEventBus modEventBus) {
		TranslateCommand.init(modEventBus);
		TranslatorKeyBinding.init(modEventBus);

		// チャット受信イベント（ゲームイベントバス）
		NeoForge.EVENT_BUS.addListener(TranslatorModClient::onChatReceived);
	}

	/**
	 * NeoForge のチャット受信イベント。
	 * Fabric の ClientReceiveMessageEvents.CHAT + GAME の両方に相当する。
	 * オーバーレイメッセージ（アクションバー）は除外する。
	 */
	private static void onChatReceived(ClientChatReceivedEvent event) {
		Component message = event.getMessage();
		String content = message.getString();

		if (shouldIgnore(content)) return;

		TranslatorConfig config = TranslatorConfig.getInstance();
		if (!config.enabled) return;

		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;

		// Hypixel モードの AUTO チャンネルのみ自動翻訳する
		// CLICK チャンネルと通常モードはボタン注入（Mixin 側で処理済み）
		if (config.hypixelMode) {
			String mode = getHypixelChannelMode(content);
			if ("AUTO".equals(mode)) {
				autoTranslate(client, content);
			}
		}
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
		int pctIdx = usageInfo.indexOf('%');
		String usageLabel = pctIdx > 0 ? usageInfo.substring(0, pctIdx).trim() : usageInfo;
		if (plain.startsWith(msgPrefix) && !plain.contains(usageLabel)) {
			return true;
		}

		return false;
	}

	/**
	 * 自動翻訳してチャットに表示する（Hypixel AUTO モード用）。
	 */
	private static void autoTranslate(Minecraft client, String content) {
		TranslationHandler.translateAsync(content).thenAccept(translated -> {
			if (translated == null) return;
			client.execute(() -> {
				if (client.player == null) return;
				if (translated.startsWith(TranslationHandler.ERR_SENTINEL)) {
					String errorMsg = translated.substring(TranslationHandler.ERR_SENTINEL.length());
					client.player.sendOverlayMessage(Component.literal("§c" + errorMsg));
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
	 * （現在は Mixin 側で処理するためこのメソッドは呼ばれていない）
	 */
	private static void addTranslateButton(Minecraft client, String content) {
		String rawText = STRIP_COLOR_PATTERN.matcher(content).replaceAll("").trim();
		String truncated = rawText.length() > 200 ? rawText.substring(0, 200) : rawText;
		String safeCommand = "/trt \"" + truncated.replace("\"", "\\\"") + "\"";

		MutableComponent button = I18n.translate("translatormod.chat.button").copy()
				.withStyle(s -> s
						.withClickEvent(new ClickEvent.RunCommand(safeCommand))
						.withColor(ChatFormatting.GRAY)
				);

		client.execute(() -> {
			if (client.player != null) {
				client.player.sendSystemMessage(button);
			}
		});
	}

	/**
	 * Hypixel モード時にチャンネルのモードを返す。
	 * @return "AUTO" / "CLICK" / "OFF"
	 */
	public static String getHypixelChannelMode(String raw) {
		TranslatorConfig config = TranslatorConfig.getInstance();
		String text = STRIP_COLOR_PATTERN.matcher(raw).replaceAll("").trim();

		if (text.startsWith("Party >"))                               return config.hypixelParty;
		if (text.startsWith("Guild >"))                               return config.hypixelGuild;
		if (text.startsWith("From ") || text.startsWith("To "))       return config.hypixelDM;
		if (text.contains(": "))                                       return config.hypixelAllChat;
		if (text.startsWith("<") && text.contains("> "))              return config.hypixelAllChat;

		return "OFF";
	}
}
