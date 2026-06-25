package net.invinciblemoebius.traumaparamedicinemod.network.packets;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.interactions.NodeAction;
import net.invinciblemoebius.traumaparamedicinemod.interactions.NodeInteractions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Client tells server "Perform <action> on <node> of the body I'm inspecting."
// For now the target is always self; medic view later passes a target id too.
public class ServerboundNodeActionPacket
{
    private final LimbNode node;
    private final NodeAction action;

    public ServerboundNodeActionPacket(LimbNode node, NodeAction action)
    {
        this.node = node;
        this.action = action;
    }

    public static void encode(ServerboundNodeActionPacket p, FriendlyByteBuf buf)
    {
        buf.writeEnum(p.node);
        buf.writeEnum(p.action);
    }

    public static ServerboundNodeActionPacket decode(FriendlyByteBuf buf)
    {
        return new ServerboundNodeActionPacket(buf.readEnum(LimbNode.class), buf.readEnum(NodeAction.class));
    }

    public static void handle(ServerboundNodeActionPacket p, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null)
                return;

            sender.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                    NodeInteractions.handle(sender, data, p.node, p.action));
        });
        ctx.get().setPacketHandled(true);
    }
}