package onemunanoyo.translatormod.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class TranslatorKeyBinding {

    public static KeyMapping openGuiKey;

    public static void register() {
        KeyMapping.Category category = new KeyMapping.Category(
                Identifier.fromNamespaceAndPath("translatormod", "general")
        );

        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.translatormod.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new TranslatorScreen(null));
                }
            }
        });
    }
}
