package net.invinciblemoebius.traumaparamedicinemod.network;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSyncHealthPacket
{
    private final float bloodVolume;

    public ClientboundSyncHealthPacket(float bloodVolume)
    {
        this.bloodVolume = bloodVolume;
    }

    public static void encode(ClientboundSyncHealthPacket packet, FriendlyByteBuf buf)
    {
        buf.writeFloat(packet.bloodVolume);
    }

    public static ClientboundSyncHealthPacket decode(FriendlyByteBuf buf)
    {
        return new ClientboundSyncHealthPacket(buf.readFloat());
    }

    public static void handle(ClientboundSyncHealthPacket packet, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                    data.setBloodVolume(packet.bloodVolume));
        });
        ctx.get().setPacketHandled(true);
    }
}
