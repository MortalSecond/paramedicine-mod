package net.invinciblemoebius.traumaparamedicinemod.overlays;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.ui.MoodleHudOverlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Single registration point for every Paramedicine GUI bit or overlay.
// Draw order = registration order here, bottom-up.
@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OverlayRegistry
{
    private OverlayRegistry() {}

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event)
    {
        // Base of the stack: the moodle bar, above all vanilla layers.
        event.registerAboveAll("moodle_bar", MoodleHudOverlay::render);

        // The Give Up button. Registers above all of the physiological overlays,
        // so the unconsciousness overlay doesn't hide the button.
        event.registerAboveAll("give_up", GiveUpOverlay::render);

        // "Vibe" overlays stack on top, each above the previous line.
        event.registerAbove(id("moodle_bar"), "consciousness_vignette", ConsciousnessOverlay::render);
    }

    private static ResourceLocation id(String path)
    {
        return new ResourceLocation(ParamedicineMod.MOD_ID, path);
    }
}