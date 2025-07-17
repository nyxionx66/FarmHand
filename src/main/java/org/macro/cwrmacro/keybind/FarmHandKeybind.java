package org.macro.cwrmacro.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.macro.cwrmacro.config.FarmHandConfig;
import org.macro.cwrmacro.gui.FarmHandConfigScreen;

public class FarmHandKeybind {
    private static KeyBinding toggleKeybind;
    private static KeyBinding configKeybind;
    
    public static void register() {
        // Master toggle keybind
        toggleKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.farmhand.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.farmhand"
        ));
        
        // Configuration screen keybind
        configKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.farmhand.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.farmhand"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle toggle keybind
            if (toggleKeybind.wasPressed()) {
                FarmHandConfig config = FarmHandConfig.getInstance();
                config.enabled = !config.enabled;
                config.save();
                
                if (client.player != null) {
                    String status = config.enabled ? "enabled" : "disabled";
                    client.player.sendMessage(
                        net.minecraft.text.Text.literal("§6[FarmHand] §fMod " + status), 
                        false
                    );
                }
            }
            
            // Handle config keybind
            if (configKeybind.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new FarmHandConfigScreen(null));
                }
            }
        });
    }
}