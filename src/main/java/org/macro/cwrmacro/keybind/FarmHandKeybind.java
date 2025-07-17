package org.macro.cwrmacro.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import org.macro.cwrmacro.config.FarmHandConfig;
import org.macro.cwrmacro.gui.FarmHandConfigScreen;
import org.macro.cwrmacro.module.AutoSellModule;
import org.macro.cwrmacro.module.TriggerBotModule;

public class FarmHandKeybind {
    private static KeyBinding toggleKeybind;
    private static KeyBinding configKeybind;
    private static KeyBinding statusKeybind;
    private static KeyBinding autoSellToggleKeybind;
    
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

        // Status display keybind
        statusKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.farmhand.status",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.farmhand"
        ));

        // Auto-sell toggle keybind
        autoSellToggleKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.farmhand.autosell",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                "category.farmhand"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle master toggle keybind
            if (toggleKeybind.wasPressed()) {
                FarmHandConfig config = FarmHandConfig.getInstance();
                config.enabled = !config.enabled;
                config.save();
                
                if (client.player != null) {
                    String status = config.enabled ? "enabled" : "disabled";
                    client.player.sendMessage(
                        Text.literal("§6[FarmHand] §fMod " + status).formatted(Formatting.GOLD), 
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

            // Handle status keybind
            if (statusKeybind.wasPressed()) {
                if (client.player != null) {
                    FarmHandConfig config = FarmHandConfig.getInstance();
                    
                    // Display comprehensive status
                    client.player.sendMessage(
                        Text.literal("§6=== FarmHand Status ===").formatted(Formatting.GOLD), 
                        false
                    );
                    
                    client.player.sendMessage(
                        Text.literal("§7Master: " + (config.enabled ? "§aENABLED" : "§cDISABLED")), 
                        false
                    );
                    
                    client.player.sendMessage(
                        Text.literal("§7AutoSell: " + (config.autoSellEnabled ? "§aENABLED" : "§cDISABLED") + 
                                " §7Item: §e" + config.autoSellItemId), 
                        false
                    );
                    
                    client.player.sendMessage(
                        Text.literal("§7TriggerBot: " + (config.triggerBotEnabled ? "§aENABLED" : "§cDISABLED") + 
                                " §7Entity: §e" + config.triggerBotEntityId), 
                        false
                    );
                    
                    client.player.sendMessage(
                        Text.literal("§7Delays: §eAutoSell=" + config.autoSellDelay + "ms §7TriggerBot=" + config.triggerBotDelay + "ms"), 
                        false
                    );
                    
                    client.player.sendMessage(
                        Text.literal("§7Threshold: §e" + config.inventoryThreshold + "/36 slots"), 
                        false
                    );
                    
                    // Show module statistics
                    client.player.sendMessage(
                        Text.literal("§7AutoSell Sales: §e" + AutoSellModule.getSellCount() + 
                                " §7TriggerBot Attacks: §e" + TriggerBotModule.getAttackCount()), 
                        false
                    );
                    
                    client.player.sendMessage(
                        Text.literal("§6===================").formatted(Formatting.GOLD), 
                        false
                    );
                }
            }

            // Handle auto-sell toggle keybind
            if (autoSellToggleKeybind.wasPressed()) {
                FarmHandConfig config = FarmHandConfig.getInstance();
                config.autoSellEnabled = !config.autoSellEnabled;
                config.save();
                
                if (client.player != null) {
                    String status = config.autoSellEnabled ? "enabled" : "disabled";
                    client.player.sendMessage(
                        Text.literal("§6[FarmHand] §fAutoSell " + status).formatted(Formatting.GOLD), 
                        false
                    );
                }
            }
        });
    }
}