package com.mark07.SystemCall;

import com.mark07.SystemCall.commands.SystemCallCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SystemCall implements ModInitializer {
	static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			SystemCallCommand.register(dispatcher);
			LOGGER.info("SystemCall Loaded!");
		});
	}
}
