package net.invinciblemoebius.traumaparamedicinemod.item;

import net.invinciblemoebius.traumaparamedicinemod.wound.Dressing;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class DressingItem extends Item
{
    private final Supplier<Dressing> basePreset;

    public DressingItem(Properties properties, Supplier<Dressing> basePreset)
    {
        super(properties);
        this.basePreset = basePreset;
    }

    // A fresh (unmodified) dressing reads its item's preset, but an enhanced one reads NBT.
    public static Dressing getDressing(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();

        if (tag != null && tag.contains("Dressing"))
            return Dressing.readFromNBT(tag.getCompound("Dressing"));

        if (stack.getItem() instanceof DressingItem dressing)
            return dressing.basePreset.get();

        return Dressing.builder().build();
    }

    public static void setDressing(ItemStack stack, Dressing dressing)
    {
        CompoundTag dt = new CompoundTag();
        dressing.writeToNBT(dt);
        stack.getOrCreateTag().put("Dressing", dt);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag)
    {
        for (String band : getDressing(stack).summaryBands())
            tip.add(Component.literal(band).withStyle(ChatFormatting.GRAY));

        super.appendHoverText(stack, level, tip, flag);
    }
}