package net.invinciblemoebius.traumaparamedicinemod.item;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.wound.DressingType;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class LongLeafItem extends Item
{
    public LongLeafItem(Properties properties)
    {
        super(properties);
    }

    // This is the functionaly ONLY for the MVP. I'm planning on completely reworking how
    // dressings work. So, for now this will dress the worst still-undressed wound in the node.
    // Set to HEMOSTATIC for now purely so the clotting boost is visible while testing,
    // the dressing rework will get rid of dressing "types" entirely.
    public boolean applyToNode(ItemStack stack, PlayerHealthData data, LimbNode node)
    {
        LimbData limb = data.getLimb(node);
        if (limb == null)
            return false;

        Wound target = worstUndressedWound(limb);

        // Nothing to dress, so don't waste the leaf.
        if (target == null)
            return false;

        target.applyDressing(DressingType.HEMOSTATIC);
        limb.markDirty();
        stack.shrink(1);

        return true;
    }

    // Highest (depth + size) wound that isn't already dressed.
    private static Wound worstUndressedWound(LimbData limb)
    {
        Wound worst = null;
        float worstScore = -1f;
        for (Wound wound : limb.getWounds())
        {
            if (wound.hasDressing())
                continue;
            float score = wound.getDepth().ordinal() + wound.getSize();
            if (score > worstScore)
            {
                worstScore = score;
                worst = wound;
            }
        }
        return worst;
    }
}