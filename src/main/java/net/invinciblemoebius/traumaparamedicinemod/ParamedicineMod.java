package net.invinciblemoebius.traumaparamedicinemod;

import com.mojang.logging.LogUtils;
import net.invinciblemoebius.traumaparamedicinemod.block.ModBlockEntities;
import net.invinciblemoebius.traumaparamedicinemod.block.ModBlocks;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.item.ModCreativeTabs;
import net.invinciblemoebius.traumaparamedicinemod.item.ModItems;
import net.invinciblemoebius.traumaparamedicinemod.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ParamedicineMod.MOD_ID)
public class ParamedicineMod
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "traumaparamedicinemod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public ParamedicineMod(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // === PARAMEDICINE ===

        // Attach the deferred registers.
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterCapabilities);
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        ModNetwork.register();
        LOGGER.info("Paramedicine Mod initialized.");
    }

    public void onRegisterCapabilities(RegisterCapabilitiesEvent event)
    {
        event.register(PlayerHealthData.class);
    }
}
