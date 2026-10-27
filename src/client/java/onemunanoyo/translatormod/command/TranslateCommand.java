package onemunanoyo.translatormod.command;

import onemunanoyo.translatormod.TranslationHandler;
import onemunanoyo.translatormod.config.TranslatorConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import onemunanoyo.translatormod.util.I18n;
import onemunanoyo.translatormod.util.DebugLog;

import java.util.Map;
import java.util.Objects;

public class TranslateCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommands.literal("translate")
                            .then(ClientCommands.literal("on").executes(TranslateCommand::executeOn))
                            .then(ClientCommands.literal("off").executes(TranslateCommand::executeOff))
                            .then(ClientCommands.literal("status").executes(TranslateCommand::executeStatus))
                            .then(ClientCommands.literal("key")
                                    .then(ClientCommands.argument("apiKey", StringArgumentType.greedyString())
                                            .executes(TranslateCommand::executeKey)))
                            .then(ClientCommands.literal("rule")
                                    .then(ClientCommands.literal("add")
                                            .then(ClientCommands.argument("from", StringArgumentType.word())
                                                    .then(ClientCommands.argument("to", StringArgumentType.word())
                                                            .executes(TranslateCommand::executeRuleAdd))))
                                    .then(ClientCommands.literal("remove")
                                            .then(ClientCommands.argument("from", StringArgumentType.word())
                                                    .executes(TranslateCommand::executeRuleRemove)))
                                    .then(ClientCommands.literal("list").executes(TranslateCommand::executeRuleList)))
                            .then(ClientCommands.literal("tooltip")
                                    .then(ClientCommands.literal("on").executes(ctx -> executeTooltip(ctx, true)))
                                    .then(ClientCommands.literal("off").executes(ctx -> executeTooltip(ctx, false))))
                            .then(ClientCommands.literal("waitmsg") // ★追加: 待機メッセージ設定
                                    .then(ClientCommands.literal("on").executes(ctx -> executeWaitMsg(ctx, true)))
                                    .then(ClientCommands.literal("off").executes(ctx -> executeWaitMsg(ctx, false))))
                            .then(ClientCommands.literal("cache")
                                    .then(ClientCommands.literal("clear").executes(TranslateCommand::executeCacheClear)))
                            .then(ClientCommands.literal("help").executes(TranslateCommand::executeHelp))
                            .executes(TranslateCommand::executeHelp)
            );

            dispatcher.register(
                    ClientCommands.literal("tr")
                            .then(ClientCommands.argument("text", StringArgumentType.greedyString())
                                    .executes(TranslateCommand::executeTr))
            );

            dispatcher.register(
                    ClientCommands.literal("trt")
                            .then(ClientCommands.argument("text", StringArgumentType.greedyString())
                                    .executes(TranslateCommand::executeTrt))
            );
        });
    }

    private static int executeTrt(CommandContext<FabricClientCommandSource> ctx) {
        String input = StringArgumentType.getString(ctx, "text");
        Minecraft client = ctx.getSource().getClient();

        TranslatorConfig config = TranslatorConfig.getInstance();
        if (config.apiKey.isBlank()) {
            sendMsg(ctx, I18n.getString("translatormod.command.trt.no_api"));
            return 0;
        }

        String targetLang = config.trTargetLang != null && !config.trTargetLang.isBlank()
                ? config.trTargetLang : "EN-US";
        if (targetLang.isBlank()) {
            sendMsg(ctx, I18n.getString("translatormod.command.tr.no_target"));
            return 0;
        }

        TranslationHandler.translateAsync(input, targetLang).thenAccept(translated -> client.execute(() -> {
            // null = 同言語 or スキップ対象 → 元のテキストをそのまま入力
            if (translated == null) {
                client.gui.setScreen(new ChatScreen(input, false));
                return;
            }
            // エラー系
            if (translated.startsWith(TranslationHandler.ERR_SENTINEL)) {
                String errorMsg = translated.substring(TranslationHandler.ERR_SENTINEL.length());
                if (client.player != null) {
                    client.player.sendOverlayMessage(Component.literal("§c" + errorMsg));
                }
                return;
            }
            // 成功 → 翻訳結果をチャット欄に入力
            client.gui.setScreen(new ChatScreen(translated, false));
        }));

        return Command.SINGLE_SUCCESS;
    }

    private static int executeTr(CommandContext<FabricClientCommandSource> ctx) {
        String input = StringArgumentType.getString(ctx, "text");
        Minecraft client = ctx.getSource().getClient();

        TranslatorConfig config = TranslatorConfig.getInstance();
        if (config.apiKey.isBlank()) {
            sendMsg(ctx, I18n.getString("translatormod.command.tr.no_api"));
            return 0;
        }

        String targetLang = config.receiveTargetLang != null && !config.receiveTargetLang.isBlank()
                ? config.receiveTargetLang : "JA";

        TranslationHandler.translateAsync(input, targetLang).thenAccept(translated -> client.execute(() -> {
            if (translated == null) {
                sendMsg(ctx, I18n.getString("translatormod.chat.not_needed", input));
                return;
            }
            if (translated.equals(I18n.getString("translatormod.command.trt.waiting"))) {
                if (config.showWaitMessage) {
                    sendMsg(ctx, I18n.getString("translatormod.command.wait"));
                }
                return;
            }
            // エラー系
            if (translated.startsWith(TranslationHandler.ERR_SENTINEL)) {
                String errorMsg = translated.substring(TranslationHandler.ERR_SENTINEL.length());
                if (client.player != null) {
                    client.player.sendOverlayMessage(Component.literal("§c" + errorMsg));
                }
                return;
            }
            sendMsg(ctx, I18n.getString("translatormod.chat.translated", translated));
        }));

        return Command.SINGLE_SUCCESS;
    }

    private static int executeOn(CommandContext<FabricClientCommandSource> ctx) {
        TranslatorConfig config = TranslatorConfig.getInstance();
        if (config.apiKey.isBlank()) {
            sendMsg(ctx, I18n.getString("translatormod.command.on.no_api"));
            return 0;
        }
        if (config.resolveTargetLang() == null) {
            sendMsg(ctx, I18n.getString("translatormod.command.on.no_rules"));
            return 0;
        }
        config.enabled = true;
        config.save();
        sendMsg(ctx, I18n.getString("translatormod.command.on.success"));
        return 1;
    }

    private static int executeOff(CommandContext<FabricClientCommandSource> ctx) {
        TranslatorConfig.getInstance().enabled = false;
        TranslatorConfig.getInstance().save();
        sendMsg(ctx, I18n.getString("translatormod.command.off.success"));
        return 1;
    }

    private static int executeStatus(CommandContext<FabricClientCommandSource> ctx) {
        TranslatorConfig config = TranslatorConfig.getInstance();
        StringBuilder sb = new StringBuilder(I18n.getString("translatormod.command.status.header"));
        sb.append(I18n.getString("translatormod.command.status.state", config.enabled ? "§aON" : "§cOFF"));
        sb.append(I18n.getString("translatormod.command.status.api_key", config.apiKey.isBlank()
                ? I18n.getString("translatormod.command.status.api_key_unset")
                : I18n.getString("translatormod.command.status.api_key_set", maskKey(config.apiKey))));
        sb.append(I18n.getString("translatormod.command.status.tooltip", config.translateTooltips ? "§aON" : "§cOFF"));
        sb.append(I18n.getString("translatormod.command.status.waitmsg", config.showWaitMessage ? "§aON" : "§cOFF"));
        sb.append(I18n.getString("translatormod.command.status.rules"));
        if (config.rules.isEmpty()) {
            sb.append(I18n.getString("translatormod.command.status.rules_none"));
        } else {
            for (Map.Entry<String, String> e : config.rules.entrySet()) {
                sb.append(I18n.getString("translatormod.command.status.rules_entry", e.getKey(), e.getValue()));
            }
        }
        sendMsg(ctx, sb.toString());
        return 1;
    }

    private static int executeKey(CommandContext<FabricClientCommandSource> ctx) {
        String key = StringArgumentType.getString(ctx, "apiKey");
        // サニタイズ
        key = key.trim().replaceAll("^DeepL-Auth-Key\\s+", ""); // プレフィックス誤入力対応
        
        TranslatorConfig config = TranslatorConfig.getInstance();
        if (!config.setApiKey(key)) return 0;
        
        TranslationHandler.clearCache();
        sendMsg(ctx, I18n.getString("translatormod.command.key.success"));
        return 1;
    }

    private static int executeRuleAdd(CommandContext<FabricClientCommandSource> ctx) {
        String from = StringArgumentType.getString(ctx, "from").toUpperCase();
        String to   = StringArgumentType.getString(ctx, "to").toUpperCase();

        if (!TranslatorConfig.isValidLang(from) && !"AUTO".equals(from)) {
            sendMsg(ctx, I18n.getString("translatormod.command.rule.invalid_from", from));
            return 0;
        }
        if (!TranslatorConfig.isValidLang(to)) {
            sendMsg(ctx, I18n.getString("translatormod.command.rule.invalid_to", to));
            return 0;
        }

        TranslatorConfig.getInstance().putRule(from, to);
        sendMsg(ctx, I18n.getString("translatormod.command.rule.add_success", from, to));
        return 1;
    }

    private static int executeRuleRemove(CommandContext<FabricClientCommandSource> ctx) {
        String from = StringArgumentType.getString(ctx, "from").toUpperCase();
        boolean removed = TranslatorConfig.getInstance().removeRule(from);
        if (removed) sendMsg(ctx, I18n.getString("translatormod.command.rule.remove_success", from));
        else sendMsg(ctx, I18n.getString("translatormod.command.rule.remove_not_found", from));
        return removed ? 1 : 0;
    }

    private static int executeRuleList(CommandContext<FabricClientCommandSource> ctx) {
        Map<String, String> rules = TranslatorConfig.getInstance().rules;
        if (rules.isEmpty()) {
            sendMsg(ctx, I18n.getString("translatormod.command.rule.list_none"));
        } else {
            StringBuilder sb = new StringBuilder(I18n.getString("translatormod.command.rule.list_header"));
            for (Map.Entry<String, String> e : rules.entrySet()) {
                sb.append(I18n.getString("translatormod.command.rule.list_entry", e.getKey(), e.getValue()));
            }
            sendMsg(ctx, sb.toString().trim());
        }
        return 1;
    }

    private static int executeTooltip(CommandContext<FabricClientCommandSource> ctx, boolean enabled) {
        TranslatorConfig config = TranslatorConfig.getInstance();
        config.translateTooltips = enabled;
        config.save();
        sendMsg(ctx, I18n.getString("translatormod.command.tooltip.state", enabled ? "§aON" : "§cOFF"));
        return 1;
    }

    private static int executeWaitMsg(CommandContext<FabricClientCommandSource> ctx, boolean enabled) {
        TranslatorConfig config = TranslatorConfig.getInstance();
        config.showWaitMessage = enabled;
        config.save();
        sendMsg(ctx, I18n.getString("translatormod.command.waitmsg.state", enabled ? "§aON" : "§cOFF"));
        return 1;
    }

    private static int executeCacheClear(CommandContext<FabricClientCommandSource> ctx) {
        TranslationHandler.clearCache();
        sendMsg(ctx, I18n.getString("translatormod.command.cache.clear.success"));
        return 1;
    }

    private static int executeHelp(CommandContext<FabricClientCommandSource> ctx) {
        sendMsg(ctx, I18n.getString("translatormod.command.help.header"));
        sendMsg(ctx, I18n.getString("translatormod.command.help.tr"));
        sendMsg(ctx, I18n.getString("translatormod.command.help.trt"));
        sendMsg(ctx, I18n.getString("translatormod.command.help.on"));
        sendMsg(ctx, I18n.getString("translatormod.command.help.off"));
        sendMsg(ctx, I18n.getString("translatormod.command.help.status"));
        sendMsg(ctx, I18n.getString("translatormod.command.help.key"));
        sendMsg(ctx, I18n.getString("translatormod.command.help.cache"));
        sendMsg(ctx, I18n.getString("translatormod.command.help.gui"));
        return 1;
    }

    private static void sendMsg(CommandContext<FabricClientCommandSource> ctx, String msg) {
        ctx.getSource().sendFeedback(Component.literal(msg));
    }

    private static String maskKey(String key) {
        if (key.length() <= 4) return "****";
        return "****" + key.substring(key.length() - 4);
    }
}

