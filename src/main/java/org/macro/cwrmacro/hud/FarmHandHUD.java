package org.macro.cwrmacro.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.macro.cwrmacro.config.FarmHandConfig;
import org.macro.cwrmacro.module.AutoSellModule;
import org.macro.cwrmacro.module.TriggerBotModule;

public class FarmHandHUD {
    private static final int HUD_COLOR_ENABLED = 0xFF55FF55;  // Green
    private static final int HUD_COLOR_DISABLED = 0xFFFF5555; // Red
    private static final int HUD_COLOR_ACTIVE = 0xFFFFFF55;   // Yellow
    private static final int HUD_COLOR_BACKGROUND = 0x88000000; // Semi-transparent black

    public static void register() {
        HudRenderCallback.EVENT.register(FarmHandHUD::renderHUD);
    }

    private static void renderHUD(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
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

        // HUD position (top-left corner)
        int hudX = 5;
        int hudY = 5;
        int lineHeight = 10;
        int padding = 2;

        // Background
        int maxWidth = 120;
        int hudHeight = (config.autoSellEnabled || config.triggerBotEnabled) ? 
            (2 + (config.autoSellEnabled ? 1 : 0) + (config.triggerBotEnabled ? 1 : 0)) * lineHeight + padding * 2 : 
            2 * lineHeight + padding * 2;
        
        context.fill(hudX - 1, hudY - 1, hudX + maxWidth + 1, hudY + hudHeight + 1, HUD_COLOR_BACKGROUND);

        // Title
        context.drawTextWithShadow(textRenderer, 
            Text.literal("§6FarmHand").formatted(Formatting.GOLD), 
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
            boolean isProcessing = AutoSellModule.isProcessing();
            String autoSellStatus = isProcessing ? "§eSELLING" : "§aREADY";
            context.drawTextWithShadow(textRenderer, 
                Text.literal("AutoSell: " + autoSellStatus), 
                hudX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;
        }

        // TriggerBot Status
        if (config.triggerBotEnabled) {
            boolean isAttacking = TriggerBotModule.isAttacking();
            String triggerBotStatus = isAttacking ? "§eATTACK" : "§aREADY";
            String speedText = config.triggerBotSpeed == 0 ? "INSTANT" : config.triggerBotSpeed + "ms";
            context.drawTextWithShadow(textRenderer, 
                Text.literal("TriggerBot: " + triggerBotStatus + " (" + speedText + ")"), 
                hudX + padding, currentY, 0xFFFFFF);
            currentY += lineHeight;
        }

        // Statistics (smaller text)
        if (config.autoSellEnabled || config.triggerBotEnabled) {
            String stats = "";
            if (config.autoSellEnabled) {
                stats += "§7Sales: §f" + AutoSellModule.getSellCount();
            }
            if (config.triggerBotEnabled) {
                if (!stats.isEmpty()) stats += " ";
                stats += "§7Attacks: §f" + TriggerBotModule.getAttackCount();
            }
            
            if (!stats.isEmpty()) {
                context.drawTextWithShadow(textRenderer, 
                    Text.literal(stats), 
                    hudX + padding, currentY, 0xFFFFFF);
            }
        }
    }
}