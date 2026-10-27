package onemunanoyo.translatormod.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public class TranslatorKeyBinding {

    public static KeyMapping openGuiKey;

    public static void init(IEventBus modEventBus) {
        KeyMapping.Category category = new KeyMapping.Category(
                Identifier.fromNamespaceAndPath("translatormod", "general")
        );

        openGuiKey = new KeyMapping(
                "key.translatormod.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                category
        );

        // キーマッピング登録はMODイベントバス
        modEventBus.addListener(TranslatorKeyBinding::onRegisterKeyMappings);

        // ティックイベントはゲームイベントバス
        NeoForge.EVENT_BUS.addListener(TranslatorKeyBinding::onClientTick);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(openGuiKey);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        while (openGuiKey.consumeClick()) {
            if (client.screen == null) {
                client.setScreen(new TranslatorScreen(null));
            }
        }
    }
}
