package org.macro.cwrmacro;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.macro.cwrmacro.config.FarmHandConfig;
import org.macro.cwrmacro.hud.FarmHandHUD;
import org.macro.cwrmacro.keybind.FarmHandKeybind;
import org.macro.cwrmacro.module.AutoSellModule;
import org.macro.cwrmacro.module.TriggerBotModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class CWRXPMactro implements ClientModInitializer {
        public static final String MOD_ID = "cwr-xp-mactro";
        public static final String MOD_NAME = "FarmHand";
        public static final String VERSION = "1.0.0";

        // This logger is used to write text to the console and the log file.
        public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

        @Override
        public void onInitializeClient() {
                try {
                        LOGGER.info("Initializing {} v{} for client...", MOD_NAME, VERSION);
                        
                        // Check if we're in a valid client environment
                        if (!isClientEnvironment()) {
                                LOGGER.warn("Not in client environment, skipping client initialization");
                                return;
                        }
                        
                        // Initialize configuration with error handling
                        try {
                                FarmHandConfig.getInstance();
                                LOGGER.info("Configuration loaded successfully");
                        } catch (Exception e) {
                                LOGGER.error("Failed to load configuration, using defaults", e);
                        }
                        
                        // Register keybinds with error handling
                        try {
                                FarmHandKeybind.register();
                                LOGGER.info("Keybinds registered successfully");
                        } catch (Exception e) {
                                LOGGER.error("Failed to register keybinds", e);
                        }
                        
                        // Register modules with error handling
                        try {
                                AutoSellModule.register();
                                LOGGER.info("AutoSell module registered successfully");
                        } catch (Exception e) {
                                LOGGER.error("Failed to register AutoSell module", e);
                        }
                        
                        try {
                                TriggerBotModule.register();
                                LOGGER.info("TriggerBot module registered successfully");
                        } catch (Exception e) {
                                LOGGER.error("Failed to register TriggerBot module", e);
                        }
                        
                        // Register HUD with error handling
                        try {
                                FarmHandHUD.register();
                                LOGGER.info("HUD registered successfully");
                        } catch (Exception e) {
                                LOGGER.error("Failed to register HUD", e);
                        }

                        LOGGER.info("{} v{} initialized successfully!", MOD_NAME, VERSION);
                        
                } catch (Exception e) {
                        LOGGER.error("Critical error during mod initialization", e);
                        throw new RuntimeException("Failed to initialize " + MOD_NAME, e);
                }
        }
        
        private boolean isClientEnvironment() {
                try {
                        return net.fabricmc.api.EnvType.CLIENT.equals(net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType());
                } catch (Exception e) {
                        LOGGER.warn("Could not determine environment type", e);
                        return true; // Assume client if we can't determine
                }
        }
}