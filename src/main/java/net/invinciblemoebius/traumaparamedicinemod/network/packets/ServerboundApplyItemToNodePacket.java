package net.invinciblemoebius.traumaparamedicinemod.network.packets;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.item.DressingItem;
import net.invinciblemoebius.traumaparamedicinemod.item.FluidContainerItem;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.network.ModNetwork;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.treatment.RouteOfEntry;
import net.invinciblemoebius.traumaparamedicinemod.treatment.TreatmentInstruction;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
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
            if (sender == null)
                return;

            if (p.slot < 0 || p.slot > 8)
                return;

            ItemStack stack = sender.getInventory().getItem(p.slot);

            // A fluid container administered by a chosen route.
            if (p.route != null && stack.getItem() instanceof FluidContainerItem)
            {
                sender.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                {
                    FluidMixture mix = FluidContainerItem.getMixture(stack);
                    if (mix.isEmpty())
                        return;

                    TreatmentInstruction instruction = switch (p.route)
                    {
                        case IV -> TreatmentInstruction.intravenous(mix);
                        case IM -> TreatmentInstruction.intramuscular(mix, p.node);
                        case ORAL -> TreatmentInstruction.oral(mix);
                        case TOPICAL -> TreatmentInstruction.topical(mix, p.node);
                    };

                    if (instruction.apply(data))
                    {
                        FluidContainerItem.setMixture(stack, new FluidMixture());
                        sender.inventoryMenu.broadcastChanges();
                        feedback(sender, routeFeedback(p.route));
                    }
                });
            }
            // Dressings apply to the worst undressed wound in the node (no route).
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

    private static String routeFeedback(RouteOfEntry route)
    {
        return switch (route)
        {
            case IV -> "You inject the solution into a vein.";
            case IM -> "You inject the solution into the muscle.";
            case TOPICAL -> "You irrigate the site.";
            case ORAL -> "You drink the solution.";
        };
    }

    private static void feedback(ServerPlayer player, String msg)
    {
        ModNetwork.CHANNEL.sendTo(new ClientboundInteractionFeedbackPacket(msg), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static boolean applyDressingToNode(ItemStack stack, PlayerHealthData data, LimbNode node)
    {
        LimbData limb = data.getLimb(node);
        if (limb == null)
            return false;

        Wound target = worstUndressedWound(limb);
        if (target == null)
            return false;

        target.applyDressing(DressingItem.getDressing(stack));
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