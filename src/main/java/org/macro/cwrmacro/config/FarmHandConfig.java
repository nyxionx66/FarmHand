package org.macro.cwrmacro.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.macro.cwrmacro.CWRXPMactro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class FarmHandConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_VERSION = "1.0.0";
    private static final String CONFIG_FILE_NAME = "farmhand.json";

    // Configuration metadata
    public String configVersion = CONFIG_VERSION;
    public long lastModified = System.currentTimeMillis();

    // Configuration fields with safe defaults
    public boolean enabled = true;
    public boolean autoSellEnabled = true;
    public String autoSellItemId = "minecraft:diamond";
    public boolean triggerBotEnabled = false;
    public String triggerBotEntityId = "minecraft:zombie";

    // Advanced settings
    public int autoSellDelay = 3000;
    public int triggerBotDelay = 500;
    public int triggerBotSpeed = 0;
    public boolean enableLogging = true;
    public boolean enableSounds = true;
    public int inventoryThreshold = 30;

    private static FarmHandConfig instance;
    private static Path configPath;
    private boolean isDirty = false;

    public static FarmHandConfig getInstance() {
        if (instance == null) {
            instance = new FarmHandConfig();
            instance.load();
        }
        return instance;
    }

    /**
     * Get config path with error handling
     */
    private static Path getConfigPath() {
        if (configPath == null) {
            try {
                Path configDir = FabricLoader.getInstance().getConfigDir();
                configPath = configDir.resolve(CONFIG_FILE_NAME);
                
                // Ensure config directory exists
                if (!Files.exists(configDir)) {
                    Files.createDirectories(configDir);
                }
            } catch (Exception e) {
                CWRXPMactro.LOGGER.error("Failed to get config path, using fallback", e);
                // Fallback to current directory
                configPath = Path.of(System.getProperty("user.dir"), CONFIG_FILE_NAME);
            }
        }
        return configPath;
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
     * Load configuration from file with comprehensive error handling
     */
    public void load() {
        try {
            Path configFile = getConfigPath();
            
            if (Files.exists(configFile)) {
                try {
                    String json = Files.readString(configFile);
                    if (json != null && !json.trim().isEmpty()) {
                        FarmHandConfig loaded = GSON.fromJson(json, FarmHandConfig.class);

                        if (loaded != null) {
                            copyFrom(loaded);

                            // Validate loaded configuration
                            if (!isValid()) {
                                CWRXPMactro.LOGGER.warn("Invalid configuration detected, using defaults for invalid fields");
                                sanitizeConfig();
                            }

                            CWRXPMactro.LOGGER.info("Configuration loaded successfully from: {}", configFile);
                        } else {
                            CWRXPMactro.LOGGER.warn("Configuration file is empty or corrupted, using defaults");
                            resetToDefaults();
                        }
                    } else {
                        CWRXPMactro.LOGGER.warn("Configuration file is empty, using defaults");
                        resetToDefaults();
                    }
                } catch (JsonSyntaxException e) {
                    CWRXPMactro.LOGGER.error("Invalid JSON in configuration file, using defaults", e);
                    backupCorruptedConfig();
                    resetToDefaults();
                } catch (IOException e) {
                    CWRXPMactro.LOGGER.error("Failed to read configuration file, using defaults", e);
                    resetToDefaults();
                }
            } else {
                CWRXPMactro.LOGGER.info("No configuration file found, creating with defaults");
                resetToDefaults();
                save(); // Create the file with defaults
            }
        } catch (Exception e) {
            CWRXPMactro.LOGGER.error("Unexpected error loading configuration, using defaults", e);
            resetToDefaults();
        }

        isDirty = false;
    }

    /**
     * Save configuration to file with error handling
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

            Path configFile = getConfigPath();
            String json = GSON.toJson(this);
            
            // Ensure parent directory exists
            Path parentDir = configFile.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Write to temporary file first, then move (atomic operation)
            Path tempFile = configFile.resolveSibling(configFile.getFileName() + ".tmp");
            Files.writeString(tempFile, json);
            Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING);

            isDirty = false;
            CWRXPMactro.LOGGER.info("Configuration saved successfully to: {}", configFile);

        } catch (Exception e) {
            CWRXPMactro.LOGGER.error("Failed to save configuration", e);
        }
    }

    /**
     * Reset all settings to safe default values
     */
    public void resetToDefaults() {
        enabled = true;
        autoSellEnabled = true;
        autoSellItemId = "minecraft:diamond";
        triggerBotEnabled = false;
        triggerBotEntityId = "minecraft:zombie";
        autoSellDelay = 3000;
        triggerBotDelay = 500;
        triggerBotSpeed = 0;
        enableLogging = true;
        enableSounds = true;
        inventoryThreshold = 30;
        configVersion = CONFIG_VERSION;
        lastModified = System.currentTimeMillis();

        markDirty();
        CWRXPMactro.LOGGER.info("Configuration reset to defaults");
    }

    /**
     * Copy values from another config instance with null safety
     */
    public void copyFrom(FarmHandConfig other) {
        if (other == null) {
            resetToDefaults();
            return;
        }

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
        this.inventoryThreshold = other.inventoryThreshold > 0 && other.inventoryThreshold <= 36 ? other.inventoryThreshold : 30;
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
     * Validate item ID format with null safety
     */
    public static boolean isValidItemId(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return false;
        }

        try {
            String[] parts = itemId.split(":");
            if (parts.length != 2) {
                return false;
            }

            String namespace = parts[0];
            String name = parts[1];

            return namespace.matches("^[a-z0-9_]+$") &&
                    name.matches("^[a-z0-9_/]+$");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate entity ID format with null safety
     */
    public static boolean isValidEntityId(String entityId) {
        if (entityId == null || entityId.trim().isEmpty()) {
            return false;
        }

        try {
            String[] parts = entityId.split(":");
            if (parts.length != 2) {
                return false;
            }

            String namespace = parts[0];
            String name = parts[1];

            return namespace.matches("^[a-z0-9_]+$") &&
                    name.matches("^[a-z0-9_/]+$");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Backup corrupted configuration file
     */
    private void backupCorruptedConfig() {
        try {
            Path configFile = getConfigPath();
            if (Files.exists(configFile)) {
                Path backupPath = configFile.resolveSibling(configFile.getFileName() + ".corrupted");
                Files.copy(configFile, backupPath, StandardCopyOption.REPLACE_EXISTING);
                CWRXPMactro.LOGGER.info("Corrupted configuration backed up to: {}", backupPath);
            }
        } catch (Exception e) {
            CWRXPMactro.LOGGER.error("Failed to backup corrupted configuration", e);
        }
    }

    public void markDirty() {
        isDirty = true;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void createBackup() {
        try {
            Path configFile = getConfigPath();
            if (Files.exists(configFile)) {
                Path backupPath = configFile.resolveSibling(configFile.getFileName() + ".backup");
                Files.copy(configFile, backupPath, StandardCopyOption.REPLACE_EXISTING);
                CWRXPMactro.LOGGER.info("Configuration backup created at: {}", backupPath);
            }
        } catch (Exception e) {
            CWRXPMactro.LOGGER.error("Failed to create configuration backup", e);
        }
    }

    public void restoreFromBackup() {
        try {
            Path configFile = getConfigPath();
            Path backupPath = configFile.resolveSibling(configFile.getFileName() + ".backup");
            if (Files.exists(backupPath)) {
                Files.copy(backupPath, configFile, StandardCopyOption.REPLACE_EXISTING);
                load(); // Reload from the restored file
                CWRXPMactro.LOGGER.info("Configuration restored from backup");
            } else {
                CWRXPMactro.LOGGER.warn("No backup file found");
            }
        } catch (Exception e) {
            CWRXPMactro.LOGGER.error("Failed to restore configuration from backup", e);
        }
    }

    public String getConfigSummary() {
        return String.format(
                "%s Config [Version: %s, Master: %s, AutoSell: %s (%s, %dms), TriggerBot: %s (%s, %s), Threshold: %d]",
                CWRXPMactro.MOD_NAME,
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