package org.macro.cwrmacro.module;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.macro.cwrmacro.CWRXPMactro;
import org.macro.cwrmacro.config.FarmHandConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TriggerBotModule {
    private static final AtomicBoolean isAttacking = new AtomicBoolean(false);
    private static final AtomicLong lastAttackTime = new AtomicLong(0);
    private static final AtomicLong attackCount = new AtomicLong(0);
    private static final long MIN_ATTACK_INTERVAL = 10; // Very fast minimum interval
    private static volatile int debugTickCount = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                processTick(client);
            } catch (Exception e) {
                CWRXPMactro.LOGGER.error("Error in TriggerBot tick processing", e);
            }
        });

        CWRXPMactro.LOGGER.info("TriggerBotModule registered successfully");
    }

    private static void processTick(MinecraftClient client) {
        FarmHandConfig config = FarmHandConfig.getInstance();

        // Debug logging every 20 seconds (400 ticks) - less frequent
        debugTickCount++;
        if (debugTickCount % 400 == 0 && config.enableLogging) {
            debugTickCount = 0;
            logDebug("TriggerBot Status: Enabled=" + config.enabled + " TriggerBotEnabled=" + config.triggerBotEnabled + 
                    " Speed=" + config.triggerBotSpeed);
        }

        // Check if module is enabled
        if (!config.enabled || !config.triggerBotEnabled) {
            return;
        }

        // Check if we're already attacking or too soon since last attack
        if (isAttacking.get() || System.currentTimeMillis() - lastAttackTime.get() < MIN_ATTACK_INTERVAL) {
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }

        // Check attack cooldown - if speed is 0 (instant), skip cooldown check
        if (config.triggerBotSpeed > 0 && player.getAttackCooldownProgress(0.5F) < 1.0F) {
            return;
        }

        // Check if crosshair is targeting the configured entity
        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
            return;
        }

        EntityHitResult entityHitResult = (EntityHitResult) hitResult;
        Entity targetEntity = entityHitResult.getEntity();

        if (!isTargetEntity(targetEntity, config.triggerBotEntityId)) {
            return;
        }

        // Start attack process
        startAttackProcess(client, player, targetEntity, config);
    }

    private static boolean isTargetEntity(Entity entity, String entityId) {
        try {
            if (entityId == null || entityId.trim().isEmpty()) {
                return false;
            }

            Identifier identifier = Identifier.tryParse(entityId.toLowerCase().trim());
            if (identifier == null) {
                return false;
            }

            EntityType<?> targetType = Registries.ENTITY_TYPE.get(identifier);
            if (targetType == null) {
                return false;
            }

            return entity.getType() == targetType && entity instanceof LivingEntity;
        } catch (Exception e) {
            CWRXPMactro.LOGGER.error("Error checking target entity: " + entityId, e);
            return false;
        }
    }

    private static void startAttackProcess(MinecraftClient client, ClientPlayerEntity player, Entity target, FarmHandConfig config) {
        if (!isAttacking.compareAndSet(false, true)) {
            return; // Already attacking
        }

        lastAttackTime.set(System.currentTimeMillis());

        // If speed is 0, attack instantly
        if (config.triggerBotSpeed == 0) {
            executeAttack(client, target);
            isAttacking.set(false);
            return;
        }

        // Otherwise use configured delay
        CompletableFuture.runAsync(() -> {
            try {
                // Use configured speed as delay
                int delay = config.triggerBotSpeed + ThreadLocalRandom.current().nextInt(50); // Small random variance
                Thread.sleep(delay);

                executeAttack(client, target);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                CWRXPMactro.LOGGER.error("Attack process interrupted", e);
            } catch (Exception e) {
                CWRXPMactro.LOGGER.error("Error in attack process", e);
            } finally {
                isAttacking.set(false);
            }
        });
    }

    private static void executeAttack(MinecraftClient client, Entity target) {
        client.execute(() -> {
            if (client.player != null && client.interactionManager != null) {
                // Double-check target validity
                if (target.isAlive() && !target.isRemoved()) {
                    // Attack the entity
                    client.interactionManager.attackEntity(client.player, target);
                    client.player.swingHand(Hand.MAIN_HAND);
                    
                    attackCount.incrementAndGet();
                    // REMOVED: No more attack messages spam
                }
            }
        });
    }

    private static void logDebug(String message) {
        CWRXPMactro.LOGGER.info("[TriggerBot Debug] " + message);
    }

    // Public API methods for monitoring and control
    public static boolean isAttacking() {
        return isAttacking.get();
    }

    public static long getLastAttackTime() {
        return lastAttackTime.get();
    }

    public static long getAttackCount() {
        return attackCount.get();
    }

    public static void resetStats() {
        attackCount.set(0);
        lastAttackTime.set(0);
    }

    public static void forceStop() {
        isAttacking.set(false);
        CWRXPMactro.LOGGER.info("TriggerBot module force stopped");
    }

    public static String getStatusSummary() {
        FarmHandConfig config = FarmHandConfig.getInstance();
        return String.format(
                "TriggerBot Status: %s | Speed: %s | Attacks: %d | Target: %s",
                config.triggerBotEnabled ? "ENABLED" : "DISABLED",
                config.triggerBotSpeed == 0 ? "INSTANT" : config.triggerBotSpeed + "ms",
                attackCount.get(),
                config.triggerBotEntityId
        );
    }
}