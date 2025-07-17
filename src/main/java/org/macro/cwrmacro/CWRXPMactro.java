package org.macro.cwrmacro;

import net.fabricmc.api.ModInitializer;
import org.macro.cwrmacro.config.FarmHandConfig;
import org.macro.cwrmacro.hud.FarmHandHUD;
import org.macro.cwrmacro.keybind.FarmHandKeybind;
import org.macro.cwrmacro.module.AutoSellModule;
import org.macro.cwrmacro.module.TriggerBotModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CWRXPMactro implements ModInitializer {
	public static final String MOD_ID = "cwr-xp-mactro";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing FarmHand mod...");
		
		// Initialize configuration
		FarmHandConfig.getInstance();
		
		// Register keybinds
		FarmHandKeybind.register();
		
		// Register modules
		AutoSellModule.register();
		TriggerBotModule.register();
		
		// Register HUD
		FarmHandHUD.register();

		LOGGER.info("FarmHand mod initialized successfully!");
	}
}