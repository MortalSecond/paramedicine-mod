package net.invinciblemoebius.traumaparamedicinemod.network.packets;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.item.items.DressingItem;
import net.invinciblemoebius.traumaparamedicinemod.item.items.SyringeItem;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
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
            else if (stack.getItem() instanceof DressingItem)
            {
                sender.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                {
                    if (applyDressingToNode(stack, data, p.node))
                        sender.inventoryMenu.broadcastChanges();
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // Applies the held dressing to the worst still-undressed wound in the node.
    // Interim until TreatmentContext/Instruction path replaces this dispatch.
    private static boolean applyDressingToNode(ItemStack stack, PlayerHealthData data, LimbNode node)
    {
        LimbData limb = data.getLimb(node);
        if (limb == null)
            return false;

        Wound target = worstUndressedWound(limb);
        if (target == null)
            return false;

        target.applyDressing(DressingItem.getDressing(stack)); // applyDressing snapshots internally
        limb.markDirty();
        stack.shrink(1);
        return true;
    }

    private static Wound worstUndressedWound(LimbData limb)
    {
        Wound worst = null;
        float bestScore = -1f;
        for (Wound wound : limb.getWounds())
        {
            if (wound.hasDressing())
                continue;
            float score = wound.getDepth().ordinal() + wound.getSize();
            if (score > bestScore)
            {
                bestScore = score;
                worst = wound;
            }
        }
        return worst;
    }
}