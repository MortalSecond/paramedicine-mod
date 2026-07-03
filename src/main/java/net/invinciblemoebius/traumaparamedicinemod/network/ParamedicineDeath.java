package net.invinciblemoebius.traumaparamedicinemod.network;

import net.invinciblemoebius.traumaparamedicinemod.network.DeathCause;
import net.minecraft.server.level.ServerPlayer;

public final class ParamedicineDeath
{
    private ParamedicineDeath() {}

    public static void kill(ServerPlayer player, DeathCause cause)
    {
        if (player == null || !player.isAlive())
            return;

        player.hurt(player.damageSources().genericKill(), player.getMaxHealth() * 4f);
    }
}