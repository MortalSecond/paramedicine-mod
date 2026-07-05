package net.invinciblemoebius.traumaparamedicinemod.item.items;

import net.invinciblemoebius.traumaparamedicinemod.substance.PowderMixture;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class PowderContainerItem extends Item
{
    private final float capacityMg;

    public PowderContainerItem(Properties properties, float capacityMg)
    {
        super(properties);
        this.capacityMg = capacityMg;
    }

    public float getCapacityMg()
    {
        return capacityMg;
    }

    public static PowderMixture getPowder(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        return (tag == null) ? new PowderMixture() : PowderMixture.readFromNBT(tag);
    }

    public static void setPowder(ItemStack stack, PowderMixture mix)
    {
        CompoundTag tag = stack.getOrCreateTag();
        mix.writeToNBT(tag);
        if (tag.isEmpty())
            stack.setTag(null);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag)
    {
        PowderMixture mix = getPowder(stack);
        if (mix.isEmpty())
            tip.add(Component.literal("Empty").withStyle(ChatFormatting.DARK_GRAY));
        else
        {
            tip.add(Component.literal(mix.describe()).withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal(String.format("%.1f / %.0f mg", mix.totalMass(), capacityMg)).withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, level, tip, flag);
    }
}