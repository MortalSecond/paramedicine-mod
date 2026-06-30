package net.invinciblemoebius.traumaparamedicinemod.network.packets;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.item.LongLeafItem;
import net.invinciblemoebius.traumaparamedicinemod.item.SyringeItem;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Client tells server "I dragged the item in hotbar slot <slot> onto <node>."
// Tthe server dispatches by item type.
public class ServerboundApplyItemToNodePacket
{
    private final int slot;
    private final LimbNode node;

    public ServerboundApplyItemToNodePacket(int slot, LimbNode node)
    {
        this.slot = slot;
        this.node = node;
    }

    public static void encode(ServerboundApplyItemToNodePacket p, FriendlyByteBuf buf)
    {
        buf.writeVarInt(p.slot);
        buf.writeEnum(p.node);
    }

    public static ServerboundApplyItemToNodePacket decode(FriendlyByteBuf buf)
    {
        return new ServerboundApplyItemToNodePacket(buf.readVarInt(), buf.readEnum(LimbNode.class));
    }

    public static void handle(ServerboundApplyItemToNodePacket p, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null)
                return;

            // Validate the slot is a real hotbar slot before touching the inventory.
            if (p.slot < 0 || p.slot > 8)
                return;

            ItemStack stack = sender.getInventory().getItem(p.slot);

            // Dispatch by item type. Only the syringe does anything for now.
            if (stack.getItem() instanceof SyringeItem syringe)
            {
                sender.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                {
                    // Push the now-empty syringe back to the client
                    if (syringe.injectAll(stack, data))
                        sender.inventoryMenu.broadcastChanges();
                });
            }
            else if (stack.getItem() instanceof LongLeafItem leaf)
            {
                sender.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                {
                    if (leaf.applyToNode(stack, data, p.node))
                        sender.inventoryMenu.broadcastChanges();
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}