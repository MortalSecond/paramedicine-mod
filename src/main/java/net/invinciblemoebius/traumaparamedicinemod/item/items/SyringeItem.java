package net.invinciblemoebius.traumaparamedicinemod.item.items;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.item.FluidContainerItem;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.treatment.RouteOfEntry;
import net.invinciblemoebius.traumaparamedicinemod.treatment.TreatmentInstruction;
import net.minecraft.world.item.ItemStack;

public class SyringeItem extends FluidContainerItem
{
    public SyringeItem(Properties properties)
    {
        super(properties, ModConstants.SYRINGE_CAPACITY_ML);
    }

    @Override
    public RouteOfEntry[] supportedRoutes()
    {
        return new RouteOfEntry[]
                {
                        RouteOfEntry.IV,
                        RouteOfEntry.IM
                };
    }

    public boolean injectAll(ItemStack stack, PlayerHealthData target)
    {
        FluidMixture mixture = getMixture(stack);
        if (!TreatmentInstruction.intravenous(mixture).apply(target))
            return false;

        setMixture(stack, new FluidMixture());
        return true;
    }
}