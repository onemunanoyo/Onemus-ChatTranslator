package onemunanoyo.translatormod.command;

import onemunanoyo.translatormod.TranslationHandler;
import onemunanoyo.translatormod.config.TranslatorConfig;
import onemunanoyo.translatormod.util.I18n;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Map;
import java.util.Objects;

public class TranslateCommand {

    /**
     * NeoForge 用の登録エントリーポイント。
     * RegisterClientCommandsEvent はゲームイベントバス（NeoForge.EVENT_BUS）で発火する。
     */
    public static void init(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(TranslateCommand::onRegisterClientCommands);
    }

    private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("translate")
                        .then(Commands.literal("on").executes(TranslateCommand::executeOn))
                        .then(Commands.literal("off").executes(TranslateCommand::executeOff))
                        .then(Commands.literal("status").executes(TranslateCommand::executeStatus))
                        .then(Commands.literal("key")
                                .then(Commands.argument("apiKey", StringArgumentType.greedyString())
                                        .executes(TranslateCommand::executeKey)))
                        .then(Commands.literal("rule")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("from", StringArgumentType.word())
                                                .then(Commands.argument("to", StringArgumentType.word())
                                                        .executes(TranslateCommand::executeRuleAdd))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("from", StringArgumentType.word())
                                                .executes(TranslateCommand::executeRuleRemove)))
                                .then(Commands.literal("list").executes(TranslateCommand::executeRuleList)))
                        .then(Commands.literal("tooltip")
                                .then(Commands.literal("on").executes(ctx -> executeTooltip(ctx, true)))
                                .then(Commands.literal("off").executes(ctx -> executeTooltip(ctx, false))))
                        .then(Commands.literal("waitmsg")
                                .then(Commands.literal("on").executes(ctx -> executeWaitMsg(ctx, true)))
                                .then(Commands.literal("off").executes(ctx -> executeWaitMsg(ctx, false))))
                        .then(Commands.literal("cache")
                                .then(Commands.literal("clear").executes(TranslateCommand::executeCacheClear)))
                        .then(Commands.literal("help").executes(TranslateCommand::executeHelp))
                        .executes(TranslateCommand::executeHelp)
        );

        dispatcher.register(
                Commands.literal("tr")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(TranslateCommand::executeTr))
        );

        dispatcher.register(
                Commands.literal("trt")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(TranslateCommand::executeTrt))
        );
    }

    private static int executeTrt(CommandContext<CommandSourceStack> ctx) {
        String input = StringArgumentType.getString(ctx, "text");
        Minecraft client = Minecraft.getInstance();

        TranslatorConfig config = TranslatorConfig.getInstance();
        if (config.apiKey.isBlank()) {
            sendMsg(I18n.getString("translatormod.command.trt.no_api"));
            return 0;
        }

        String targetLang = config.receiveTargetLang != null && !config.receiveTargetLang.isBlank()
                ? config.receiveTargetLang : "JA";

        TranslationHandler.translateAsync(input, targetLang).thenAccept(translated -> client.execute(() -> {
            if (translated == null) return;
            if (translated.equals(I18n.getString("translatormod.command.trt.waiting"))) {
                if (config.showWaitMessage) {
                    sendMsg(I18n.getString("translatormod.command.wait"));
                }
                return;
            }
            if (translated.startsWith(TranslationHandler.ERR_SENTINEL)) {
                String errorMsg = translated.substring(TranslationHandler.ERR_SENTINEL.length());
                Objects.requireNonNull(client.player).sendOverlayMessage(Component.literal("§c" + errorMsg));
                return;
            }
            sendMsg(I18n.getString("translatormod.chat.translated", translated));
        }));

        return Command.SINGLE_SUCCESS;
    }

    private static int executeTr(CommandContext<CommandSourceStack> ctx) {
        String input = StringArgumentType.getString(ctx, "text");
        Minecraft client = Minecraft.getInstance();

        TranslatorConfig config = TranslatorConfig.getInstance();
        if (config.apiKey.isBlank()) {
            sendMsg(I18n.getString("translatormod.command.tr.no_api"));
            return 0;
        }

        String targetLang = config.trTargetLang != null && !config.trTargetLang.isBlank()
                ? config.trTargetLang : "EN";
        if (targetLang.isBlank()) {
            sendMsg(I18n.getString("translatormod.command.tr.no_target"));
            return 0;
        }

        TranslationHandler.translateAsync(input, targetLang).thenAccept(translated -> client.execute(() -> {
            if (translated == null) {
                client.setScreen(new ChatScreen(input, false));
                return;
            }
            if (translated.startsWith(TranslationHandler.ERR_SENTINEL)) {
                String errorMsg = translated.substring(TranslationHandler.ERR_SENTINEL.length());
                Objects.requireNonNull(client.player).sendOverlayMessage(Component.literal("§c" + errorMsg));
                return;
            }
            client.setScreen(new ChatScreen(translated, false));
        }));

        return Command.SINGLE_SUCCESS;
    }

    private static int executeOn(CommandContext<CommandSourceStack> ctx) {
        TranslatorConfig config = TranslatorConfig.getInstance();
        if (config.apiKey.isBlank()) {
            sendMsg(I18n.getString("translatormod.command.on.no_api"));
            return 0;
        }
        if (config.resolveTargetLang() == null) {
            sendMsg(I18n.getString("translatormod.command.on.no_rules"));
            return 0;
        }
        config.enabled = true;
        config.save();
        sendMsg(I18n.getString("translatormod.command.on.success"));
        return 1;
    }

    private static int executeOff(CommandContext<CommandSourceStack> ctx) {
        TranslatorConfig.getInstance().enabled = false;
        TranslatorConfig.getInstance().save();
        sendMsg(I18n.getString("translatormod.command.off.success"));
        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> ctx) {
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
        sendMsg(sb.toString());
        return 1;
    }

    private static int executeKey(CommandContext<CommandSourceStack> ctx) {
        String key = StringArgumentType.getString(ctx, "apiKey");
        key = key.trim().replaceAll("^DeepL-Auth-Key\\s+", "");

        TranslatorConfig config = TranslatorConfig.getInstance();
        config.setApiKey(key);

        TranslationHandler.clearCache();
        sendMsg(I18n.getString("translatormod.command.key.success"));
        return 1;
    }

    private static int executeRuleAdd(CommandContext<CommandSourceStack> ctx) {
        String from = StringArgumentType.getString(ctx, "from").toUpperCase();
        String to   = StringArgumentType.getString(ctx, "to").toUpperCase();

        if (!TranslatorConfig.isValidLang(from) && !"AUTO".equals(from)) {
            sendMsg(I18n.getString("translatormod.command.rule.invalid_from", from));
            return 0;
        }
        if (!TranslatorConfig.isValidLang(to)) {
            sendMsg(I18n.getString("translatormod.command.rule.invalid_to", to));
            return 0;
        }

        TranslatorConfig.getInstance().putRule(from, to);
        sendMsg(I18n.getString("translatormod.command.rule.add_success", from, to));
        return 1;
    }

    private static int executeRuleRemove(CommandContext<CommandSourceStack> ctx) {
        String from = StringArgumentType.getString(ctx, "from").toUpperCase();
        boolean removed = TranslatorConfig.getInstance().removeRule(from);
        if (removed) sendMsg(I18n.getString("translatormod.command.rule.remove_success", from));
        else sendMsg(I18n.getString("translatormod.command.rule.remove_not_found", from));
        return removed ? 1 : 0;
    }

    private static int executeRuleList(CommandContext<CommandSourceStack> ctx) {
        Map<String, String> rules = TranslatorConfig.getInstance().rules;
        if (rules.isEmpty()) {
            sendMsg(I18n.getString("translatormod.command.rule.list_none"));
        } else {
            StringBuilder sb = new StringBuilder(I18n.getString("translatormod.command.rule.list_header"));
            for (Map.Entry<String, String> e : rules.entrySet()) {
                sb.append(I18n.getString("translatormod.command.rule.list_entry", e.getKey(), e.getValue()));
            }
            sendMsg(sb.toString().trim());
        }
        return 1;
    }

    private static int executeTooltip(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        TranslatorConfig config = TranslatorConfig.getInstance();
        config.translateTooltips = enabled;
        config.save();
        sendMsg(I18n.getString("translatormod.command.tooltip.state", enabled ? "§aON" : "§cOFF"));
        return 1;
    }

    private static int executeWaitMsg(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        TranslatorConfig config = TranslatorConfig.getInstance();
        config.showWaitMessage = enabled;
        config.save();
        sendMsg(I18n.getString("translatormod.command.waitmsg.state", enabled ? "§aON" : "§cOFF"));
        return 1;
    }

    private static int executeCacheClear(CommandContext<CommandSourceStack> ctx) {
        TranslationHandler.clearCache();
        sendMsg(I18n.getString("translatormod.command.cache.clear.success"));
        return 1;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> ctx) {
        sendMsg(I18n.getString("translatormod.command.help.header"));
        sendMsg(I18n.getString("translatormod.command.help.tr"));
        sendMsg(I18n.getString("translatormod.command.help.trt"));
        sendMsg(I18n.getString("translatormod.command.help.on"));
        sendMsg(I18n.getString("translatormod.command.help.off"));
        sendMsg(I18n.getString("translatormod.command.help.status"));
        sendMsg(I18n.getString("translatormod.command.help.key"));
        sendMsg(I18n.getString("translatormod.command.help.cache"));
        sendMsg(I18n.getString("translatormod.command.help.gui"));
        return 1;
    }

    /**
     * Minecraft.getInstance().player.sendSystemMessage を使うことで
     * CommandSourceStack の型に依存しないメッセージ送信を行う。
     */
    private static void sendMsg(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(msg));
        }
    }

    private static String maskKey(String key) {
        if (key.length() <= 4) return "****";
        return key.substring(key.length() - 4);
    }
}
