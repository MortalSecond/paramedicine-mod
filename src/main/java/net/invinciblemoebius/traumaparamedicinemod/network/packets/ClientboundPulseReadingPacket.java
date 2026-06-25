package net.invinciblemoebius.traumaparamedicinemod.network.packets;

import net.invinciblemoebius.traumaparamedicinemod.client.ClientDiagnosticState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundPulseReadingPacket
{
    private final int threshold;
    private final boolean present;

    public ClientboundPulseReadingPacket(int threshold, boolean present)
    {
        this.threshold = threshold;
        this.present = present;
    }

    public static void encode(ClientboundPulseReadingPacket p, FriendlyByteBuf buf)
    {
        buf.writeVarInt(p.threshold);
        buf.writeBoolean(p.present);
    }

    public static ClientboundPulseReadingPacket decode(FriendlyByteBuf buf)
    {
        return new ClientboundPulseReadingPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(ClientboundPulseReadingPacket p, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> ClientDiagnosticState.applyPulseReading(p.threshold, p.present));
        ctx.get().setPacketHandled(true);
    }
}
