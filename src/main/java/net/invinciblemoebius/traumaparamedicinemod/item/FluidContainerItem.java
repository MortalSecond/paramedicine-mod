package net.invinciblemoebius.traumaparamedicinemod.item;

import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.treatment.RouteOfEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class FluidContainerItem extends Item
{
    private final float capacityML;
    public RouteOfEntry[] supportedRoutes()
    {
        return new RouteOfEntry[]
                {
                        RouteOfEntry.ORAL,
                        RouteOfEntry.TOPICAL
                };
    }

    public FluidContainerItem(Properties properties, float capacityML)
    {
        super(properties);
        this.capacityML = capacityML;
    }

    public float getCapacityML() { return capacityML; }

    // === NBT ===

    // An empty tag gives an empty stack.
    public static FluidMixture getMixture(ItemStack stack)
    {
        CompoundTag compoundTag = stack.getTag();
        return (compoundTag == null) ? new FluidMixture() : FluidMixture.readFromNBT(compoundTag);
    }

    // If the result is empty, the entire tag is stripped so the stack returns to its stackable state.
    public static void setMixture(ItemStack stack, FluidMixture mixture)
    {
        CompoundTag compoundTag = stack.getOrCreateTag();
        mixture.writeToNBT(compoundTag);

        if (compoundTag.isEmpty())
            stack.setTag(null);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        FluidMixture mixture = getMixture(stack);

        if (mixture.isEmpty())
            tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.DARK_GRAY));
        else
        {
            tooltip.add(Component.literal(mixture.describe()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(String.format("%.1f / %.0f mL", mixture.totalVolume(), capacityML)).withStyle(ChatFormatting.DARK_GRAY));
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }
}
