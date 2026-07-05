package net.invinciblemoebius.traumaparamedicinemod.item.items;

import net.invinciblemoebius.traumaparamedicinemod.block.ItemComposition;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class DriedPlantItem extends Item
{
    public DriedPlantItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag)
    {
        Map<SubstanceType, Float> composition = ItemComposition.read(stack);
        boolean any = false;
        for (Map.Entry<SubstanceType, Float> entry : composition.entrySet())
        {
            tip.add(Component.literal(pretty(entry.getKey()) + " " + String.format("%.1f", entry.getValue()) + " mg")
                    .withStyle(ChatFormatting.GRAY));
            any = true;
        }

        if (!any)
            tip.add(Component.translatable("item.traumaparamedicinemod.dried_plant.inert").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String pretty(SubstanceType type)
    {
        String[] parts = type.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts)
        {
            if (sb.length() > 0)
                sb.append(' ');

            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }

        return sb.toString();
    }
}