package net.invinciblemoebius.traumaparamedicinemod.block;

import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

// Maps an item into what it releases into a boiling pot.
public final class IngredientInfusions
{
    private IngredientInfusions() {}

    public record Infusion(SubstanceType substance, float amountPerItem, float extractionEfficiency) {}

    private static final Map<Item, Infusion> MAP = new HashMap<>();

    static
    {
        // TEST PAYLOAD. Boiling poppies does NOT yield morphine. Opium is dried pod latex, and
        // real extraction is orgchem's gated endgame.
        // TODO: replace with a weak POPPY_EXTRACT trace analgesic once real flower
        //  substances exist. MORPHINE here only to prove the loop.
        MAP.put(Items.POPPY, new Infusion(SubstanceType.MORPHINE, 10f, 0.5f));
    }

    // === ACESSORS ===

    public static Infusion forItem(Item item) { return MAP.get(item); }
    public static boolean isInfusable(Item item) { return MAP.containsKey(item); }
}