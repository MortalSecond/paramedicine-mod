package net.invinciblemoebius.traumaparamedicinemod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeybinds
{
    // Open health screen.
    public static final KeyMapping OPEN_HEALTH_SCREEN = new KeyMapping(
            "key.paramedicine.health_screen",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.paramedicine");

    // Hold to give up when mortally wounded.
    public static final KeyMapping GIVE_UP = new KeyMapping(
            "key.paramedicine.give_up",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.paramedicine");

    // === REGISTRATION ===
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(OPEN_HEALTH_SCREEN);
        event.register(GIVE_UP);
    }
}
