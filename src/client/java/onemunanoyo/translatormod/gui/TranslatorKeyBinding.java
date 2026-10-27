package onemunanoyo.translatormod.gui;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class TranslatorKeyBinding {

    public static KeyBinding openGuiKey;

    public static void register() {
        KeyBinding.Category category = new KeyBinding.Category(
                Identifier.of("translatormod", "general")
        );

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.translatormod.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new TranslatorScreen(null));
                }
            }
        });
    }
}