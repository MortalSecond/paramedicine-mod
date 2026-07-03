package net.invinciblemoebius.traumaparamedicinemod.network;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;

public enum DeathCause
{
    GIVE_UP,
    BRAIN_DEATH;

    public boolean isWarranted(PlayerHealthData data)
    {
        return switch (this)
        {
            case GIVE_UP -> data.canGiveUp();
            case BRAIN_DEATH -> data.isBrainDead();
        };
    }
}