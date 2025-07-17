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
    public boolean enabled = true; // CHANGED: Default to true for easier use
    public boolean autoSellEnabled = true; // CHANGED: Default to true for easier use
    public String autoSellItemId = "minecraft:diamond"; // CHANGED: More common item
    public boolean triggerBotEnabled = false;
    public String triggerBotEntityId = "minecraft:zombie"; // CHANGED: More common entity

    // Advanced settings - IMPROVED: Better default values
    public int autoSellDelay = 3000; // 3 seconds - reasonable default
    public int triggerBotDelay = 500; // milliseconds
    public boolean enableLogging = true;
    public boolean enableSounds = true;
    public int inventoryThreshold = 30; // NEW: Configurable inventory threshold

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
     * @return A new FarmHandConfig instance with copied values
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
                save(); // Create the file with defaults
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
            // Update metadata
            lastModified = System.currentTimeMillis();
            configVersion = CONFIG_VERSION;

            // Validate before saving
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

        // Basic validation: must contain namespace:name format
        String[] parts = itemId.split(":");
        if (parts.length != 2) {
            return false;
        }

        String namespace = parts[0];
        String name = parts[1];

        // Check for valid characters (lowercase letters, numbers, underscores, forward slashes)
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

        // Basic validation: must contain namespace:name format
        String[] parts = entityId.split(":");
        if (parts.length != 2) {
            return false;
        }

        String namespace = parts[0];
        String name = parts[1];

        // Check for valid characters (lowercase letters, numbers, underscores, forward slashes)
        return namespace.matches("^[a-z0-9_]+$") &&
                name.matches("^[a-z0-9_/]+$");
    }

    /**
     * Mark configuration as dirty (needs saving)
     */
    public void markDirty() {
        isDirty = true;
    }

    /**
     * Check if configuration has unsaved changes
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Create a backup of the current configuration
     */
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

    /**
     * Restore from backup
     */
    public void restoreFromBackup() {
        try {
            Path backupPath = CONFIG_PATH.getParent().resolve("farmhand.json.backup");
            if (Files.exists(backupPath)) {
                Files.copy(backupPath, CONFIG_PATH);
                load(); // Reload from the restored file
                CWRXPMactro.LOGGER.info("Configuration restored from backup");
            } else {
                CWRXPMactro.LOGGER.warn("No backup file found");
            }
        } catch (IOException e) {
            CWRXPMactro.LOGGER.error("Failed to restore configuration from backup", e);
        }
    }

    /**
     * Get configuration summary for debugging
     */
    public String getConfigSummary() {
        return String.format(
                "FarmHand Config [Version: %s, Master: %s, AutoSell: %s (%s, %dms), TriggerBot: %s (%s, %dms), Threshold: %d]",
                configVersion,
                enabled ? "ON" : "OFF",
                autoSellEnabled ? "ON" : "OFF",
                autoSellItemId,
                autoSellDelay,
                triggerBotEnabled ? "ON" : "OFF",
                triggerBotEntityId,
                triggerBotDelay,
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
                autoSellDelay, triggerBotDelay, enableLogging, enableSounds, inventoryThreshold);
    }

    @Override
    public String toString() {
        return getConfigSummary();
    }
}