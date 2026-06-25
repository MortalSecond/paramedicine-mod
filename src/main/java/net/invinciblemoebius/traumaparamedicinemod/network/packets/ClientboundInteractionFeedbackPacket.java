package net.invinciblemoebius.traumaparamedicinemod.network.packets;

import net.invinciblemoebius.traumaparamedicinemod.client.ClientDiagnosticState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundInteractionFeedbackPacket
{
    private final String text;

    public ClientboundInteractionFeedbackPacket(String text)
    {
        this.text = text;
    }

    public static void encode(ClientboundInteractionFeedbackPacket p, FriendlyByteBuf buf)
    {
        buf.writeUtf(p.text);
    }
    public static ClientboundInteractionFeedbackPacket decode(FriendlyByteBuf buf)
    {
        return new ClientboundInteractionFeedbackPacket(buf.readUtf());
    }

    public static void handle(ClientboundInteractionFeedbackPacket p, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> ClientDiagnosticState.showFeedback(p.text));
        ctx.get().setPacketHandled(true);
    }
}