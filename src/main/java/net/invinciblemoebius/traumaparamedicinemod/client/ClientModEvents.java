package net.invinciblemoebius.traumaparamedicinemod.client;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.ui.HealthScreen;
import net.invinciblemoebius.traumaparamedicinemod.ui.MoodleHudOverlay;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientModEvents
{
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Advance the moodles.
        MoodleHudOverlay.clientTick();

        // Prevent spam-opening the health screen.
        while(ModKeybinds.OPEN_HEALTH_SCREEN.consumeClick())
        {
            if (mc.screen instanceof HealthScreen)
            {
                // Already open, close.
                mc.setScreen(null);
            }
            else if (mc.screen == null)
            {
                // ONLY open if there's no other screen instance on.
                mc.setScreen(new HealthScreen(mc.player));
            }
        }
    }
}
