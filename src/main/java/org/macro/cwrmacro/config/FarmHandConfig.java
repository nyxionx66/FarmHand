package org.macro.cwrmacro.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.macro.cwrmacro.CWRXPMactro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class FarmHandConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("farmhand.json");
    private static final String CONFIG_VERSION = "1.0.0";

    // Configuration metadata
    public String configVersion = CONFIG_VERSION;
    public long lastModified = System.currentTimeMillis();

    // Configuration fields with defaults
    public boolean enabled = true;
    public boolean autoSellEnabled = true;
    public String autoSellItemId = "minecraft:diamond";
    public boolean triggerBotEnabled = false;
    public String triggerBotEntityId = "minecraft:zombie";

    // Advanced settings
    public int autoSellDelay = 3000; // 3 seconds
    public int triggerBotDelay = 500; // DEPRECATED - use triggerBotSpeed instead
    public int triggerBotSpeed = 0; // NEW: 0 = instant, higher = slower
    public boolean enableLogging = true;
    public boolean enableSounds = true;
    public int inventoryThreshold = 30;

    private static FarmHandConfig instance;
    private boolean isDirty = false;

    public static FarmHandConfig getInstance() {
        if (instance == null) {
            instance = new FarmHandConfig();
            instance.load();
        }
        return instance;
    }

    /**
     * Create a deep copy of this configuration
     */
    public FarmHandConfig copy() {
        FarmHandConfig copy = new FarmHandConfig();
        copy.copyFrom(this);
        return copy;
    }

    /**
     * Load configuration from file
     */
    public void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                FarmHandConfig loaded = GSON.fromJson(json, FarmHandConfig.class);

                if (loaded != null) {
                    copyFrom(loaded);

                    // Validate loaded configuration
                    if (!isValid()) {
                        CWRXPMactro.LOGGER.warn("Invalid configuration detected, using defaults for invalid fields");
                        sanitizeConfig();
                    }

                    CWRXPMactro.LOGGER.info("FarmHand configuration loaded successfully");
                } else {
                    CWRXPMactro.LOGGER.warn("Configuration file is empty or corrupted, using defaults");
                    resetToDefaults();
                }
            } else {
                CWRXPMactro.LOGGER.info("No configuration file found, creating with defaults");
                resetToDefaults();
                save();
            }
        } catch (IOException e) {
            CWRXPMactro.LOGGER.error("Failed to read FarmHand config file", e);
            resetToDefaults();
        } catch (JsonSyntaxException e) {
            CWRXPMactro.LOGGER.error("Invalid JSON in FarmHand config file", e);
            resetToDefaults();
        }

        isDirty = false;
    }

    /**
     * Save configuration to file
     */
    public void save() {
        try {
            lastModified = System.currentTimeMillis();
            configVersion = CONFIG_VERSION;

            if (!isValid()) {
                CWRXPMactro.LOGGER.warn("Attempting to save invalid configuration, sanitizing first");
                sanitizeConfig();
            }

            String json = GSON.toJson(this);
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, json);

            isDirty = false;
            CWRXPMactro.LOGGER.info("FarmHand configuration saved successfully");

        } catch (IOException e) {
            CWRXPMactro.LOGGER.error("Failed to save FarmHand config", e);
        }
    }

    /**
     * Reset all settings to default values
     */
    public void resetToDefaults() {
        enabled = true;
        autoSellEnabled = true;
        autoSellItemId = "minecraft:diamond";
        triggerBotEnabled = false;
        triggerBotEntityId = "minecraft:zombie";
        autoSellDelay = 3000;
        triggerBotDelay = 500;
        triggerBotSpeed = 0; // Instant by default
        enableLogging = true;
        enableSounds = true;
        inventoryThreshold = 30;
        configVersion = CONFIG_VERSION;
        lastModified = System.currentTimeMillis();

        markDirty();
        CWRXPMactro.LOGGER.info("FarmHand configuration reset to defaults");
    }

    /**
     * Copy values from another config instance
     */
    public void copyFrom(FarmHandConfig other) {
        this.configVersion = other.configVersion != null ? other.configVersion : CONFIG_VERSION;
        this.lastModified = other.lastModified;
        this.enabled = other.enabled;
        this.autoSellEnabled = other.autoSellEnabled;
        this.autoSellItemId = other.autoSellItemId != null ? other.autoSellItemId : "minecraft:diamond";
        this.triggerBotEnabled = other.triggerBotEnabled;
        this.triggerBotEntityId = other.triggerBotEntityId != null ? other.triggerBotEntityId : "minecraft:zombie";
        this.autoSellDelay = other.autoSellDelay > 0 ? other.autoSellDelay : 3000;
        this.triggerBotDelay = other.triggerBotDelay > 0 ? other.triggerBotDelay : 500;
        this.triggerBotSpeed = other.triggerBotSpeed >= 0 ? other.triggerBotSpeed : 0;
        this.enableLogging = other.enableLogging;
        this.enableSounds = other.enableSounds;
        this.inventoryThreshold = other.inventoryThreshold > 0 ? other.inventoryThreshold : 30;
    }

    /**
     * Validate configuration values
     */
    private boolean isValid() {
        return isValidItemId(autoSellItemId) &&
                isValidEntityId(triggerBotEntityId) &&
                autoSellDelay > 0 &&
                triggerBotDelay > 0 &&
                triggerBotSpeed >= 0 &&
                inventoryThreshold > 0 && inventoryThreshold <= 36;
    }

    /**
     * Sanitize configuration by replacing invalid values with defaults
     */
    private void sanitizeConfig() {
        if (!isValidItemId(autoSellItemId)) {
            autoSellItemId = "minecraft:diamond";
        }

        if (!isValidEntityId(triggerBotEntityId)) {
            triggerBotEntityId = "minecraft:zombie";
        }

        if (autoSellDelay <= 0) {
            autoSellDelay = 3000;
        }

        if (triggerBotDelay <= 0) {
            triggerBotDelay = 500;
        }

        if (triggerBotSpeed < 0) {
            triggerBotSpeed = 0;
        }

        if (inventoryThreshold <= 0 || inventoryThreshold > 36) {
            inventoryThreshold = 30;
        }
    }

    /**
     * Validate item ID format
     */
    public static boolean isValidItemId(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return false;
        }

        String[] parts = itemId.split(":");
        if (parts.length != 2) {
            return false;
        }

        String namespace = parts[0];
        String name = parts[1];

        return namespace.matches("^[a-z0-9_]+$") &&
                name.matches("^[a-z0-9_/]+$");
    }

    /**
     * Validate entity ID format
     */
    public static boolean isValidEntityId(String entityId) {
        if (entityId == null || entityId.trim().isEmpty()) {
            return false;
        }

        String[] parts = entityId.split(":");
        if (parts.length != 2) {
            return false;
        }

        String namespace = parts[0];
        String name = parts[1];

        return namespace.matches("^[a-z0-9_]+$") &&
                name.matches("^[a-z0-9_/]+$");
    }

    public void markDirty() {
        isDirty = true;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void createBackup() {
        try {
            Path backupPath = CONFIG_PATH.getParent().resolve("farmhand.json.backup");
            if (Files.exists(CONFIG_PATH)) {
                Files.copy(CONFIG_PATH, backupPath);
                CWRXPMactro.LOGGER.info("Configuration backup created at: " + backupPath);
            }
        } catch (IOException e) {
            CWRXPMactro.LOGGER.error("Failed to create configuration backup", e);
        }
    }

    public void restoreFromBackup() {
        try {
            Path backupPath = CONFIG_PATH.getParent().resolve("farmhand.json.backup");
            if (Files.exists(backupPath)) {
                Files.copy(backupPath, CONFIG_PATH);
                load();
                CWRXPMactro.LOGGER.info("Configuration restored from backup");
            } else {
                CWRXPMactro.LOGGER.warn("No backup file found");
            }
        } catch (IOException e) {
            CWRXPMactro.LOGGER.error("Failed to restore configuration from backup", e);
        }
    }

    public String getConfigSummary() {
        return String.format(
                "FarmHand Config [Version: %s, Master: %s, AutoSell: %s (%s, %dms), TriggerBot: %s (%s, %s), Threshold: %d]",
                configVersion,
                enabled ? "ON" : "OFF",
                autoSellEnabled ? "ON" : "OFF",
                autoSellItemId,
                autoSellDelay,
                triggerBotEnabled ? "ON" : "OFF",
                triggerBotEntityId,
                triggerBotSpeed == 0 ? "INSTANT" : triggerBotSpeed + "ms",
                inventoryThreshold
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        FarmHandConfig that = (FarmHandConfig) obj;
        return enabled == that.enabled &&
                autoSellEnabled == that.autoSellEnabled &&
                triggerBotEnabled == that.triggerBotEnabled &&
                autoSellDelay == that.autoSellDelay &&
                triggerBotDelay == that.triggerBotDelay &&
                triggerBotSpeed == that.triggerBotSpeed &&
                enableLogging == that.enableLogging &&
                enableSounds == that.enableSounds &&
                inventoryThreshold == that.inventoryThreshold &&
                Objects.equals(autoSellItemId, that.autoSellItemId) &&
                Objects.equals(triggerBotEntityId, that.triggerBotEntityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, autoSellEnabled, autoSellItemId,
                triggerBotEnabled, triggerBotEntityId,
                autoSellDelay, triggerBotDelay, triggerBotSpeed, enableLogging, enableSounds, inventoryThreshold);
    }

    @Override
    public String toString() {
        return getConfigSummary();
    }
}