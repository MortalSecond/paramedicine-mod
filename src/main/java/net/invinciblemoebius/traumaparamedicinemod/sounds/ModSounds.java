package net.invinciblemoebius.traumaparamedicinemod.sounds;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds
{
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ParamedicineMod.MOD_ID);;

    public static final RegistryObject<SoundEvent> STEWPOT_BOIL = register("stewpot_boiling");
    public static final RegistryObject<SoundEvent> HEALTHSCREEN_OPEN = register("beep_on");
    public static final RegistryObject<SoundEvent> HEALTHSCREEN_CLOSE = register("click_off");

    // I'm planning for these sounds to play as the health screen is open. They worsen in
    // quality and interference as the body enters worse shock.
    // TODO: Body crisis accrual.
    public static final RegistryObject<SoundEvent> HEALTHSCREEN_SUSTAIN_MILD = register("radio_mild");
    public static final RegistryObject<SoundEvent> HEALTHSCREEN_SUSTAIN_MID = register("radio_mid");

    private static RegistryObject<SoundEvent> register(String name)
    {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ParamedicineMod.MOD_ID, name)));
    }
    public static void register(IEventBus modEventBus)
    {
        SOUNDS.register(modEventBus);
    }
}
