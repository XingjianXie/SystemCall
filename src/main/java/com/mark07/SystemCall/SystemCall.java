package com.mark07.SystemCall;

import com.mark07.SystemCall.commands.SystemCallCommand;
import com.mark07.SystemCall.effect.AerialStreamEffect;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SystemCall implements ModInitializer {
	static final Logger LOGGER = LogManager.getLogger();
	public static final MobEffect AERIAL_STREAM = new AerialStreamEffect();

	@Override
	public void onInitialize() {

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			SystemCallCommand.register(dispatcher);
			//LOGGER.info("SystemCall Loaded!");
		});

		Registry.register(
				Registry.MOB_EFFECT,
				new ResourceLocation("system_call", "aerial_stream"),
				AERIAL_STREAM
		);
	}
}
