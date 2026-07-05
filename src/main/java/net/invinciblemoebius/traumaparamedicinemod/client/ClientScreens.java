package net.invinciblemoebius.traumaparamedicinemod.client;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.menu.ModMenus;
import net.invinciblemoebius.traumaparamedicinemod.ui.MolcajeteScreen;
import net.invinciblemoebius.traumaparamedicinemod.ui.StewpotScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientScreens
{
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
                MenuScreens.register(ModMenus.STEWPOT.get(), StewpotScreen::new));
                MenuScreens.register(ModMenus.MOLCAJETE.get(), MolcajeteScreen::new);
    }
}