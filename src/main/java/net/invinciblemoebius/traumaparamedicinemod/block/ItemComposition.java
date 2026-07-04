package net.invinciblemoebius.traumaparamedicinemod.block;

import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

// What a WORLD ITEM (as in, not a FluidMixture or PowderMixture) is MADE OF: substance -> amount.
public final class ItemComposition
{
    private ItemComposition() {}

    public static final String NBT_KEY = "Composition";
    private static final float GRAIN = 100f; // 1/100 mg.

    private static final Map<Item, Map<SubstanceType, Float>> MAP = new HashMap<>();

    static
    {
        // TEST. Boiling poppies does NOT make morphine (opium is dried pod latex, real extraction
        // is orgchem's job). TODO: replace MORPHINE with a weak analgesic precursor. Here only to
        // prove the loop, so it's mostly plant matter, as a raw flower should be.
        MAP.put(Items.POPPY, Map.of(
                SubstanceType.WATER, 7f,
                SubstanceType.PLANT_MATTER, 2f,
                SubstanceType.MORPHINE, 1f));
    }

    // === QUERIES ===

    public static boolean has(ItemStack stack)
    {
        if (stack.isEmpty())
            return false;
        if (hasNbt(stack))
            return true;

        return MAP.containsKey(stack.getItem());
    }

    // Back-compat for callers that only have an Item type.
    public static boolean has(Item item)
    {
        return MAP.containsKey(item);
    }

    // Fill a transient buffer from a stack. NBT overrides the static map.
    public static void materializeInto(FluidMixture buffer, ItemStack stack)
    {
        if (stack.isEmpty())
            return;

        if (hasNbt(stack))
        {
            readNbtInto(buffer, stack);
            return;
        }

        materializeInto(buffer, stack.getItem());
    }

    public static void materializeInto(FluidMixture buffer, Item item)
    {
        Map<SubstanceType, Float> composition = MAP.get(item);
        if (composition == null)
            return;

        for (Map.Entry<SubstanceType, Float> entry : composition.entrySet())
            buffer.add(entry.getKey(), entry.getValue(), Float.MAX_VALUE);
    }

    // A fresh copy of a stack's composition (for drying, grinding, etc).
    public static Map<SubstanceType, Float> read(ItemStack stack)
    {
        Map<SubstanceType, Float> out = new EnumMap<>(SubstanceType.class);
        if (stack.isEmpty())
            return out;

        if (hasNbt(stack))
        {
            ListTag list = stack.getTag().getList(NBT_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++)
            {
                CompoundTag c = list.getCompound(i);
                SubstanceType t = parse(c.getString("Type"));
                if (t != null)
                    out.merge(t, c.getFloat("Amount"), Float::sum);
            }
        }
        else
        {
            Map<SubstanceType, Float> comp = MAP.get(stack.getItem());
            if (comp != null)
                out.putAll(comp);
        }

        return out;
    }

    // === MUTATION ===

    // Stamp a composition onto a stack's NBT, quantized so identical results stack.
    public static void stamp(ItemStack stack, Map<SubstanceType, Float> composition)
    {
        ListTag list = new ListTag();
        for (Map.Entry<SubstanceType, Float> entry : composition.entrySet())
        {
            float amount = Math.round(entry.getValue() * GRAIN) / GRAIN;
            if (amount <= 0f)
                continue;

            CompoundTag c = new CompoundTag();
            c.putString("Type", entry.getKey().name());
            c.putFloat("Amount", amount);
            list.add(c);
        }

        if (list.isEmpty())
        {
            if (stack.getTag() != null) stack.removeTagKey(NBT_KEY);
        }
        else stack.getOrCreateTag().put(NBT_KEY, list);
    }

    // === HELPERS ===

    private static boolean hasNbt(ItemStack stack)
    {
        return stack.getTag() != null && stack.getTag().contains(NBT_KEY);
    }

    private static void readNbtInto(FluidMixture buffer, ItemStack stack)
    {
        ListTag list = stack.getTag().getList(NBT_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag c = list.getCompound(i);
            SubstanceType t = parse(c.getString("Type"));
            if (t != null)
                buffer.add(t, c.getFloat("Amount"), Float.MAX_VALUE);
        }
    }

    private static SubstanceType parse(String name)
    {
        try
        {
            return SubstanceType.valueOf(name);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }
}