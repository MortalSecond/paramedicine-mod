package net.invinciblemoebius.traumaparamedicinemod.item;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class SyringeItem extends FluidContainerItem
{
    public SyringeItem(Properties properties)
    {
        super(properties, ModConstants.SYRINGE_CAPACITY_ML);
    }

    public boolean injectAll(ItemStack stack, PlayerHealthData target)
    {
        FluidMixture mixture = getMixture(stack);
        if (mixture.isEmpty())
            return false;

        for (Map.Entry<SubstanceType, Float> entry : mixture.getComponents().entrySet())
            target.depositSystemicSubstance(entry.getKey(), entry.getValue(), ModConstants.IV_ONSET_SECONDS);

        setMixture(stack, new FluidMixture());
        return true;
    }
}
