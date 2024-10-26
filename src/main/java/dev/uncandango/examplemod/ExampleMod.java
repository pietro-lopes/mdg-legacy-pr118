package dev.uncandango.examplemod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ExampleMod.MOD_ID)
public final class ExampleMod {
	public static final String MOD_ID = "examplemod";
	public static final Logger LOGGER = LogUtils.getLogger();

	public ExampleMod() {
		final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		final IEventBus gameBus = MinecraftForge.EVENT_BUS;
	}
}
