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

public class TriggerBotModule {
    private static boolean isAttacking = false;
    private static long lastAttackTime = 0;
    private static final long MIN_ATTACK_INTERVAL = 100; // Minimum 100ms between attacks

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FarmHandConfig config = FarmHandConfig.getInstance();

            // Check if module is enabled
            if (!config.enabled || !config.triggerBotEnabled) {
                return;
            }

            // Check if we're already attacking or too soon since last attack
            if (isAttacking || System.currentTimeMillis() - lastAttackTime < MIN_ATTACK_INTERVAL) {
                return;
            }

            ClientPlayerEntity player = client.player;
            if (player == null || client.world == null) {
                return;
            }

            // Check attack cooldown
            if (player.getAttackCooldownProgress(0.5F) < 1.0F) {
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

            // Start attack process with human-like delay
            startAttackProcess(client, player, targetEntity);
        });
    }

    private static boolean isTargetEntity(Entity entity, String entityId) {
        try {
            Identifier identifier = Identifier.tryParse(entityId);
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

    private static void startAttackProcess(MinecraftClient client, ClientPlayerEntity player, Entity target) {
        isAttacking = true;
        lastAttackTime = System.currentTimeMillis();

        CompletableFuture.runAsync(() -> {
            try {
                // Human-like reaction delay (50-200ms)
                int delay = ThreadLocalRandom.current().nextInt(50, 201);
                Thread.sleep(delay);

                // Execute attack on main thread
                client.execute(() -> {
                    if (client.player != null && client.interactionManager != null) {
                        // Double-check cooldown and target validity
                        if (client.player.getAttackCooldownProgress(0.5F) >= 1.0F &&
                            target.isAlive() && !target.isRemoved()) {

                            // Attack the entity
                            client.interactionManager.attackEntity(client.player, target);
                            client.player.swingHand(Hand.MAIN_HAND);
                        }
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                CWRXPMactro.LOGGER.error("Attack process interrupted", e);
            } catch (Exception e) {
                CWRXPMactro.LOGGER.error("Error in attack process", e);
            } finally {
                isAttacking = false;
            }
        });
    }
}