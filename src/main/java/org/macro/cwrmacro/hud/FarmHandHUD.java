package org.macro.cwrmacro.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.macro.cwrmacro.CWRXPMactro;
import org.macro.cwrmacro.config.FarmHandConfig;
import org.macro.cwrmacro.module.AutoSellModule;
import org.macro.cwrmacro.module.TriggerBotModule;

public class FarmHandHUD {
    private static final int HUD_COLOR_BACKGROUND = 0x88000000;
    private static boolean hudEnabled = true;
    private static boolean renderError = false;

    public static void register() {
        try {
            HudRenderCallback.EVENT.register(FarmHandHUD::renderHUD);
            CWRXPMactro.LOGGER.info("HUD registered successfully");
        } catch (Exception e) {
            CWRXPMactro.LOGGER.error("Failed to register HUD", e);
        }
    }

    private static void renderHUD(DrawContext context, RenderTickCounter tickCounter) {
        if (!hudEnabled || renderError) {
            return;
        }

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.world == null) {
                return;
            }

            // Check if client is in valid state
            if (client.getWindow() == null || client.textRenderer == null) {
                return;
            }

            FarmHandConfig config = FarmHandConfig.getInstance();
            
            // Don't show HUD if master is disabled
            if (!config.enabled) {
                return;
            }

            TextRenderer textRenderer = client.textRenderer;
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            // Ensure screen dimensions are valid
            if (screenWidth <= 0 || screenHeight <= 0) {
                return;
            }

            // HUD position (top-left corner with safe margins)
            int hudX = Math.max(5, screenWidth / 200);
            int hudY = Math.max(5, screenHeight / 200);
            int lineHeight = Math.max(10, textRenderer.fontHeight + 2);
            int padding = 2;

            // Calculate HUD dimensions
            int maxWidth = Math.min(150, screenWidth / 6);
            int activeModules = 0;
            if (config.autoSellEnabled) activeModules++;
            if (config.triggerBotEnabled) activeModules++;
            
            int hudHeight = (2 + activeModules) * lineHeight + padding * 2;
            
            // Ensure HUD fits on screen
            if (hudX + maxWidth > screenWidth) {
                hudX = screenWidth - maxWidth - 5;
            }
            if (hudY + hudHeight > screenHeight) {
                hudY = screenHeight - hudHeight - 5;
            }

            // Background
            context.fill(hudX - 1, hudY - 1, hudX + maxWidth + 1, hudY + hudHeight + 1, HUD_COLOR_BACKGROUND);

            // Title
            context.drawTextWithShadow(textRenderer, 
                Text.literal("§6" + CWRXPMactro.MOD_NAME).formatted(Formatting.GOLD), 
                hudX + padding, hudY + padding, 0xFFFFFF);
            
            int currentY = hudY + padding + lineHeight;

            // Master Status
            String masterStatus = config.enabled ? "§aON" : "§cOFF";
            context.drawTextWithShadow(textRenderer, 
                Text.literal("Master: " + masterStatus), 
                hudX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;

            // AutoSell Status
            if (config.autoSellEnabled) {
                try {
                    boolean isProcessing = AutoSellModule.isProcessing();
                    String autoSellStatus = isProcessing ? "§eSELLING" : "§aREADY";
                    context.drawTextWithShadow(textRenderer, 
                        Text.literal("AutoSell: " + autoSellStatus), 
                        hudX + padding, currentY, 0xFFFFFF);
                    currentY += lineHeight;
                } catch (Exception e) {
                    CWRXPMactro.LOGGER.debug("Error getting AutoSell status", e);
                }
            }

            // TriggerBot Status
            if (config.triggerBotEnabled) {
                try {
                    boolean isAttacking = TriggerBotModule.isAttacking();
                    String triggerBotStatus = isAttacking ? "§eATTACK" : "§aREADY";
                    String speedText = config.triggerBotSpeed == 0 ? "INSTANT" : config.triggerBotSpeed + "ms";
                    context.drawTextWithShadow(textRenderer, 
                        Text.literal("TriggerBot: " + triggerBotStatus), 
                        hudX + padding, currentY, 0xFFFFFF);
                    currentY += lineHeight;
                } catch (Exception e) {
                    CWRXPMactro.LOGGER.debug("Error getting TriggerBot status", e);
                }
            }

        } catch (Exception e) {
            CWRXPMactro.LOGGER.error("Error rendering HUD, disabling", e);
            renderError = true;
        }
    }

    public static void setEnabled(boolean enabled) {
        hudEnabled = enabled;
    }

    public static boolean isEnabled() {
        return hudEnabled;
    }

    public static void resetError() {
        renderError = false;
    }
}