package net.invinciblemoebius.traumaparamedicinemod.network.packets;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.network.DeathCause;
import net.invinciblemoebius.traumaparamedicinemod.network.ParamedicineDeath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundDeathPacket
{
    private final DeathCause cause;

    public ServerboundDeathPacket(DeathCause cause)
    {
        this.cause = cause;
    }

    public ServerboundDeathPacket(FriendlyByteBuf buf)
    {
        this.cause = buf.readEnum(DeathCause.class);
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeEnum(cause);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null)
                return;

            sender.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
            {
                // Client requests, server verifies. A hacked client can't force a death it hasn't earned.
                if (!cause.isWarranted(data))
                    return;

                ParamedicineDeath.kill(sender, cause);
            });
        });

        return true;
    }
}