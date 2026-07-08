package net.invinciblemoebius.traumaparamedicinemod.treatment;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.item.DressingItem;
import net.invinciblemoebius.traumaparamedicinemod.item.FluidContainerItem;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.network.ModNetwork;
import net.invinciblemoebius.traumaparamedicinemod.network.packets.ClientboundInteractionFeedbackPacket;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;

import javax.annotation.Nullable;

// Orchestration layer for treatments. Owns item-type dispatch, the scope, and feedback,
// so packets stay slim and Wound keeps only its mechanical primitives.
public final class Treatments
{
    private Treatments() {}

    // "The player applied the item in <slot> to <node>", optionally by a route.
    public static void applyItem(ServerPlayer sender, PlayerHealthData data, int slot, LimbNode node, @Nullable RouteOfEntry route)
    {
        ItemStack stack = sender.getInventory().getItem(slot);
        if (stack.isEmpty())
            return;

        // A fluid administered by a chosen route.
        if (route != null && stack.getItem() instanceof FluidContainerItem)
        {
            FluidMixture mix = FluidContainerItem.getMixture(stack);
            if (mix.isEmpty())
                return;

            TreatmentInstruction instruction = switch (route)
            {
                case IV -> TreatmentInstruction.intravenous(mix);
                case IM -> TreatmentInstruction.intramuscular(mix, node);
                case ORAL -> TreatmentInstruction.oral(mix);
                case TOPICAL -> TreatmentInstruction.topical(mix, node);
            };

            if (instruction.apply(data))
            {
                // The instruction drained only what it used. Persist the remnants.
                FluidContainerItem.setMixture(stack, mix);
                sender.inventoryMenu.broadcastChanges();
                feedback(sender, routeFeedback(route));
            }
            return;
        }

        // A dressing applied to the node, covering wounds worst-first by its length.
        if (stack.getItem() instanceof DressingItem)
        {
            LimbData limb = data.getLimb(node);
            if (limb != null && limb.applyDressing(DressingItem.getDressing(stack)))
            {
                stack.shrink(1);
                sender.inventoryMenu.broadcastChanges();
            }
        }
    }

    public static void removeDressing(ServerPlayer sender, PlayerHealthData data, LimbNode node, int woundId)
    {
        LimbData limb = data.getLimb(node);
        if (limb == null)
            return;

        boolean ripped = limb.removeDressingCovering(woundId, sender.getRandom());
        feedback(sender, ripped ? "You peel the dressing away and the wound tears open." : "You remove the dressing.");
    }

    // === UI ===

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
        ModNetwork.CHANNEL.sendTo(new ClientboundInteractionFeedbackPacket(msg),
                player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}