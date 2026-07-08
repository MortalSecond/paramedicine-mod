package net.invinciblemoebius.traumaparamedicinemod.network.packets;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.treatment.RouteOfEntry;
import net.invinciblemoebius.traumaparamedicinemod.treatment.Treatments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

// Client tells server "I dragged the item in hotbar slot <slot> onto <node>"
// Optionally with a chosen administration route (for fluid containers).
public class ServerboundApplyItemToNodePacket
{
    private final int slot;
    private final LimbNode node;
    @Nullable private final RouteOfEntry route;

    public ServerboundApplyItemToNodePacket(int slot, LimbNode node)
    {
        this(slot, node, null);
    }

    public ServerboundApplyItemToNodePacket(int slot, LimbNode node, @Nullable RouteOfEntry route)
    {
        this.slot = slot;
        this.node = node;
        this.route = route;
    }

    public static void encode(ServerboundApplyItemToNodePacket p, FriendlyByteBuf buf)
    {
        buf.writeVarInt(p.slot);
        buf.writeEnum(p.node);
        buf.writeBoolean(p.route != null);
        if (p.route != null)
            buf.writeEnum(p.route);
    }

    public static ServerboundApplyItemToNodePacket decode(FriendlyByteBuf buf)
    {
        int slot = buf.readVarInt();
        LimbNode node = buf.readEnum(LimbNode.class);
        RouteOfEntry route = buf.readBoolean() ? buf.readEnum(RouteOfEntry.class) : null;
        return new ServerboundApplyItemToNodePacket(slot, node, route);
    }

    public static void handle(ServerboundApplyItemToNodePacket p, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || p.slot < 0 || p.slot > 8)
                return;

            sender.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                    Treatments.applyItem(sender, data, p.slot, p.node, p.route));
        });
        ctx.get().setPacketHandled(true);
    }
}