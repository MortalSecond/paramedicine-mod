package net.invinciblemoebius.traumaparamedicinemod.network;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// "I opened the inspect screen on entity ID"
public class ServerboundInspectPacket
{
    private final int targetID;

    public ServerboundInspectPacket(int targetID)
    {
        this.targetID = targetID;
    }

    public static void encode(ServerboundInspectPacket p, FriendlyByteBuf buf)
    {
        buf.writeVarInt(p.targetID);
    }

    public static ServerboundInspectPacket decode(FriendlyByteBuf buf)
    {
        return new ServerboundInspectPacket(buf.readVarInt());
    }

    public static void handle(ServerboundInspectPacket p, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null)
                return;

            if (p.targetID < 0)
                InspectionTracker.clear(sender.getUUID());
            else
            {
                InspectionTracker.set(sender.getUUID(), p.targetID);
                // Force a fresh detail stream instead of waiting for the next markDirty()'d tick.
                sender.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(PlayerHealthData::markDirty);
            }
        });

        ctx.get().setPacketHandled(true);
    }
}
