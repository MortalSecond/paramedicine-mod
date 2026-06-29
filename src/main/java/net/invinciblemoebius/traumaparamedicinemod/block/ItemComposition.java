package net.invinciblemoebius.traumaparamedicinemod.block;

import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

// What a WORLD ITEM (as in, not a FluidMixture or PowderMixture) is MADE OF: substance -> amount (dose-units).
public final class ItemComposition
{
    private ItemComposition() {}

    private static final Map<Item, Map<SubstanceType, Float>> MAP = new HashMap<>();

    static
    {
        // TEST. Boiling poppies does NOT make morphine (opium is dried pod latex, real extraction
        // is orgchem's job). TODO: replace MORPHINE with a weak analgesic precursor. Here only to
        // prove the loop, so it's mostly plant matter, as a raw flower should be.
        MAP.put(Items.POPPY, Map.of(
                SubstanceType.PLANT_MATTER, 9f,
                SubstanceType.MORPHINE, 1f));
    }

    public static boolean has(Item item) { return MAP.containsKey(item); }

    // Fills the buffer with the item's composition. The buffer is a transient, uncapped holder.
    public static void materializeInto(FluidMixture buffer, Item item)
    {
        Map<SubstanceType, Float> comp = MAP.get(item);
        if (comp == null)
            return;
        for (Map.Entry<SubstanceType, Float> e : comp.entrySet())
            buffer.add(e.getKey(), e.getValue(), Float.MAX_VALUE);
    }
}