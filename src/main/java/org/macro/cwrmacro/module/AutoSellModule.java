package org.macro.cwrmacro.module;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.macro.cwrmacro.CWRXPMactro;
import org.macro.cwrmacro.config.FarmHandConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class AutoSellModule {
    private static final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private static final AtomicLong lastProcessTime = new AtomicLong(0);
    private static final AtomicLong sellCount = new AtomicLong(0);
    private static final AtomicLong lastErrorTime = new AtomicLong(0);

    // Configuration-driven constants
    private static final long DEFAULT_COOLDOWN_MS = 3000;
    private static final long ERROR_COOLDOWN_MS = 5000;
    private static final int MAX_RETRIES = 2;
    private static final int INVENTORY_FULL_THRESHOLD = 30;

    // Timing ranges for human-like behavior
    private static final int[] SWITCH_DELAY_RANGE = {100, 300};
    private static final int[] COMMAND_DELAY_RANGE = {200, 500};
    private static final int[] RESTORE_DELAY_RANGE = {150, 350};

    private static volatile boolean moduleEnabled = true;
    private static volatile String lastError = null;
    private static volatile long lastSuccessTime = 0;
    private static volatile int debugTickCount = 0;
    private static volatile boolean registrationError = false;

    public static void register() {
        try {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                try {
                    processTick(client);
                } catch (Exception e) {
                    handleError("Tick processing error", e);
                }
            });

            CWRXPMactro.LOGGER.info("AutoSellModule registered successfully");
        } catch (Exception e) {
            CWRXPMactro.LOGGER.error("Failed to register AutoSellModule", e);
            registrationError = true;
        }
    }

    private static void processTick(MinecraftClient client) {
        if (registrationError) {
            return;
        }

        try {
            // Null safety checks
            if (client == null) {
                return;
            }

            FarmHandConfig config = FarmHandConfig.getInstance();
            if (config == null) {
                return;
            }
            
            // Debug logging with safety checks
            debugTickCount++;
            if (debugTickCount % 100 == 0 && config.enableLogging) {
                debugTickCount = 0;
                try {
                    logDebug("AutoSell Status: Enabled=" + moduleEnabled + " ConfigEnabled=" + config.enabled + 
                            " AutoSellEnabled=" + config.autoSellEnabled + " Processing=" + isProcessing.get());
                } catch (Exception e) {
                    // Ignore debug logging errors
                }
            }

            // Check if module is enabled
            if (!moduleEnabled || !config.enabled || !config.autoSellEnabled) {
                return;
            }

            // Check if we're already processing
            if (isProcessing.get()) {
                return;
            }

            // Check cooldown
            long currentTime = System.currentTimeMillis();
            long cooldownMs = config.autoSellDelay > 0 ? config.autoSellDelay : DEFAULT_COOLDOWN_MS;

            // Extended cooldown after errors
            if (lastErrorTime.get() > 0 && currentTime - lastErrorTime.get() < ERROR_COOLDOWN_MS) {
                return;
            }

            if (currentTime - lastProcessTime.get() < cooldownMs) {
                return;
            }

            ClientPlayerEntity player = client.player;
            if (player == null || client.world == null) {
                return;
            }

            // Check if inventory meets the threshold for selling
            int filledSlots = countFilledInventorySlots(player);
            if (filledSlots < INVENTORY_FULL_THRESHOLD) {
                return;
            }

            // Find the configured item in hotbar
            int itemSlot = findItemInHotbar(player, config.autoSellItemId);
            if (itemSlot == -1) {
                if (config.enableLogging) {
                    logInfo("AutoSell item not found in hotbar: " + config.autoSellItemId + " (Inventory: " + filledSlots + "/36)");
                }
                return;
            }

            // Start the auto-sell process
            if (config.enableLogging) {
                logInfo("Starting auto-sell process (Inventory: " + filledSlots + "/36, Item slot: " + itemSlot + ")");
            }
            startAutoSellProcess(client, player, itemSlot, config);

        } catch (Exception e) {
            handleError("Critical error in processTick", e);
        }
    }

    private static int countFilledInventorySlots(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) {
            return 0;
        }

        try {
            int filledSlots = 0;
            
            // Check main inventory slots (9-44, which is 36 slots)
            for (int i = 9; i < 45; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack != null && !stack.isEmpty()) {
                    filledSlots++;
                }
            }

            return filledSlots;
        } catch (Exception e) {
            CWRXPMactro.LOGGER.debug("Error counting inventory slots", e);
            return 0;
        }
    }

    private static int findItemInHotbar(ClientPlayerEntity player, String itemId) {
        if (player == null || player.getInventory() == null) {
            return -1;
        }

        try {
            if (itemId == null || itemId.trim().isEmpty()) {
                logError("Item ID is null or empty");
                return -1;
            }

            // Validate item ID format
            if (!FarmHandConfig.isValidItemId(itemId)) {
                logError("Invalid item ID format: " + itemId);
                return -1;
            }

            Identifier identifier = Identifier.tryParse(itemId.toLowerCase().trim());
            if (identifier == null) {
                logError("Failed to parse item ID: " + itemId);
                return -1;
            }

            Item targetItem = Registries.ITEM.get(identifier);
            if (targetItem == null) {
                logError("Item not found in registry: " + itemId);
                return -1;
            }

            // Check hotbar slots (0-8)
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack != null && !stack.isEmpty() && stack.getItem() == targetItem) {
                    return i;
                }
            }
        } catch (Exception e) {
            handleError("Error finding item in hotbar: " + itemId, e);
        }

        return -1;
    }

    private static void startAutoSellProcess(MinecraftClient client, ClientPlayerEntity player, int itemSlot, FarmHandConfig config) {
        if (!isProcessing.compareAndSet(false, true)) {
            return; // Already processing
        }

        lastProcessTime.set(System.currentTimeMillis());
        int originalSlot = player.getInventory().selectedSlot;

        CompletableFuture.runAsync(() -> {
            int attempts = 0;
            boolean success = false;

            while (attempts < MAX_RETRIES && !success) {
                attempts++;

                try {
                    success = executeAutoSellSequence(client, itemSlot, originalSlot, config);

                    if (success) {
                        handleSuccess(config);
                    } else if (attempts < MAX_RETRIES) {
                        // Wait before retry
                        Thread.sleep(500 + ThreadLocalRandom.current().nextInt(500));
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    handleError("Auto-sell process interrupted", e);
                    break;
                } catch (Exception e) {
                    handleError("Error in auto-sell process (attempt " + attempts + ")", e);

                    if (attempts < MAX_RETRIES) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            if (!success) {
                handleError("Auto-sell failed after " + MAX_RETRIES + " attempts", null);
            }

        }).whenComplete((result, throwable) -> {
            isProcessing.set(false);

            if (throwable != null) {
                handleError("Auto-sell completion error", throwable);
            }
        });
    }

    private static boolean executeAutoSellSequence(MinecraftClient client, int itemSlot, int originalSlot, FarmHandConfig config)
            throws InterruptedException {

        if (client == null || client.player == null) {
            return false;
        }

        try {
            // Step 1: Switch to item slot
            client.execute(() -> {
                try {
                    if (client.player != null && client.player.getInventory() != null) {
                        client.player.getInventory().selectedSlot = itemSlot;
                    }
                } catch (Exception e) {
                    CWRXPMactro.LOGGER.debug("Error switching to item slot", e);
                }
            });

            // Human-like delay
            Thread.sleep(getRandomDelay(SWITCH_DELAY_RANGE));

            // Step 2: Execute sell command
            final boolean[] commandSent = {false};
            client.execute(() -> {
                try {
                    if (client.player != null && client.player.networkHandler != null) {
                        client.player.networkHandler.sendChatCommand("sell hand");
                        commandSent[0] = true;
                        if (config.enableLogging) {
                            logInfo("Sent command: /sell hand");
                        }
                    }
                } catch (Exception e) {
                    CWRXPMactro.LOGGER.debug("Error sending sell command", e);
                }
            });

            // Wait for command to be sent
            Thread.sleep(getRandomDelay(COMMAND_DELAY_RANGE));

            if (!commandSent[0]) {
                logError("Failed to send sell command");
                return false;
            }

            // Step 3: Switch back to original slot
            client.execute(() -> {
                try {
                    if (client.player != null && client.player.getInventory() != null) {
                        client.player.getInventory().selectedSlot = originalSlot;
                    }
                } catch (Exception e) {
                    CWRXPMactro.LOGGER.debug("Error switching back to original slot", e);
                }
            });

            // Final delay to complete the sequence
            Thread.sleep(getRandomDelay(RESTORE_DELAY_RANGE));

            return true;

        } catch (Exception e) {
            CWRXPMactro.LOGGER.error("Error in sell sequence", e);
            return false;
        }
    }

    private static int getRandomDelay(int[] range) {
        try {
            return ThreadLocalRandom.current().nextInt(range[0], range[1] + 1);
        } catch (Exception e) {
            return range[0]; // Fallback to minimum delay
        }
    }

    private static void handleSuccess(FarmHandConfig config) {
        try {
            lastSuccessTime = System.currentTimeMillis();
            sellCount.incrementAndGet();
            lastError = null;

            if (config != null && config.enableLogging) {
                logInfo("Auto-sell completed successfully! (Total: " + sellCount.get() + ")");
            }

            // Play success sound if enabled
            if (config != null && config.enableSounds) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    client.execute(() -> {
                        try {
                            if (client.player != null) {
                                client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                            }
                        } catch (Exception e) {
                            // Ignore sound errors
                        }
                    });
                }
            }
        } catch (Exception e) {
            CWRXPMactro.LOGGER.debug("Error in handleSuccess", e);
        }
    }

    private static void handleError(String message, Throwable error) {
        try {
            lastErrorTime.set(System.currentTimeMillis());
            lastError = message;

            if (error != null) {
                CWRXPMactro.LOGGER.error(message, error);
            } else {
                CWRXPMactro.LOGGER.error(message);
            }

            // Send error message to player
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.execute(() -> {
                    try {
                        if (client.player != null) {
                            client.player.sendMessage(
                                    Text.literal("[AutoSell] " + message).formatted(Formatting.RED),
                                    false
                            );
                        }
                    } catch (Exception e) {
                        // Ignore message errors
                    }
                });
            }
        } catch (Exception e) {
            // Last resort error handling
            CWRXPMactro.LOGGER.error("Critical error in error handler", e);
        }
    }

    private static void logInfo(String message) {
        try {
            CWRXPMactro.LOGGER.info("[AutoSell] " + message);

            // Send info message to player
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.execute(() -> {
                    try {
                        if (client.player != null) {
                            client.player.sendMessage(
                                    Text.literal("[AutoSell] " + message).formatted(Formatting.GREEN),
                                    false
                            );
                        }
                    } catch (Exception e) {
                        // Ignore message errors
                    }
                });
            }
        } catch (Exception e) {
            // Ignore logging errors
        }
    }

    private static void logError(String message) {
        try {
            CWRXPMactro.LOGGER.error("[AutoSell] " + message);
        } catch (Exception e) {
            // Ignore logging errors
        }
    }

    private static void logDebug(String message) {
        try {
            CWRXPMactro.LOGGER.info("[AutoSell Debug] " + message);
        } catch (Exception e) {
            // Ignore logging errors
        }
    }

    // Public API methods with null safety
    public static boolean isProcessing() {
        return isProcessing.get();
    }

    public static long getLastProcessTime() {
        return lastProcessTime.get();
    }

    public static long getSellCount() {
        return sellCount.get();
    }

    public static String getLastError() {
        return lastError;
    }

    public static long getLastSuccessTime() {
        return lastSuccessTime;
    }

    public static void resetStats() {
        sellCount.set(0);
        lastError = null;
        lastSuccessTime = 0;
        lastErrorTime.set(0);
    }

    public static void forceStop() {
        isProcessing.set(false);
        CWRXPMactro.LOGGER.info("AutoSell module force stopped");
    }

    public static void setEnabled(boolean enabled) {
        moduleEnabled = enabled;
        if (!enabled) {
            forceStop();
        }
        CWRXPMactro.LOGGER.info("AutoSell module " + (enabled ? "enabled" : "disabled"));
    }

    public static boolean isEnabled() {
        return moduleEnabled;
    }

    public static String getStatusSummary() {
        try {
            return String.format(
                    "AutoSell Status: %s | Processing: %s | Sales: %d | Last Success: %s | Last Error: %s",
                    moduleEnabled ? "ENABLED" : "DISABLED",
                    isProcessing.get() ? "YES" : "NO",
                    sellCount.get(),
                    lastSuccessTime > 0 ? "Yes" : "Never",
                    lastError != null ? lastError : "None"
            );
        } catch (Exception e) {
            return "AutoSell Status: ERROR";
        }
    }
}