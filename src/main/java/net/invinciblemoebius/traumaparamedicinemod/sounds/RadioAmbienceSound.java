package net.invinciblemoebius.traumaparamedicinemod.sounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class RadioAmbienceSound extends AbstractTickableSoundInstance
{
    public RadioAmbienceSound(SoundEvent event)
    {
        super(event, SoundSource.PLAYERS, RandomSource.create());

        this.looping = true;
        this.delay = 0;
        this.volume = 0.6f;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    @Override
    public void tick()
    {
        // OKAY SO. I've been thinking that once i get the total body crisis accrual system
        // up and running, i'm going to wire it up to the health screen's ambience. I've
        // really been considering about making Paramedicine very radio-themed, but like,
        // orange hues and OCR mono of specialzied pagers, not the generic green console radios.
        // Soooooooooo for that to work out, i've been thinking that the health screen's ambience
        // should thus be radio-sounding. Like a walkie talkie. Normal body health = clean white noise,
        // but as the crisis value accrues, we switch into a different sound file, one with more
        // violent static and more interference. All that gets wired up here.
    }
}
