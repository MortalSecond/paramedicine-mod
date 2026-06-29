package net.invinciblemoebius.traumaparamedicinemod.substance;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

// Any block that boils a fluid drives substances through here.
// Two INDEPENDENT properties per substance:
// - What it converts INTO (identity change, volume kept),
// - Whether it's EVAPORABLE (volume lost as steam).
// A substance can be both, either, or neither.
// Unmapped substances are inert to conversion and don't evaporate, so an addon substance nobody
// mapped is safe. When a pot boils dry of all solvent, the remainder scorches to JUNK.
public final class SubstanceEbullition
{
    private SubstanceEbullition() {}

    // What each substance boils into. Absent = no conversion (terminal or non-converting).
    private static final Map<SubstanceType, SubstanceType> CONVERTS = new EnumMap<>(SubstanceType.class);
    // Substances that steam off as volume. Independent of conversion.
    private static final EnumSet<SubstanceType> EVAPORABLE = EnumSet.noneOf(SubstanceType.class);

    static
    {
        // THINGS THAT BOIL INTO OTHER SUBSTANCES
        CONVERTS.put(SubstanceType.WATER, SubstanceType.BOILED_WATER);
        CONVERTS.put(SubstanceType.MILK, SubstanceType.CARAMEL);

        // THINGS THAT CAN EVAPORATE
        EVAPORABLE.add(SubstanceType.WATER);
        EVAPORABLE.add(SubstanceType.BOILED_WATER);
        EVAPORABLE.add(SubstanceType.PURIFIED_WATER);
        EVAPORABLE.add(SubstanceType.MILK);
    }

    // === MUTATION METHODS ===

    // Advance every convertible component by up to ml. Iterates a snapshot so a
    // freshly-made product doesn't fully re-convert in the same tick.
    public static boolean convert(FluidMixture store, float ml, float capacity)
    {
        boolean changed = false;
        for (Map.Entry<SubstanceType, Float> entry : new EnumMap<>(store.getComponents()).entrySet())
        {
            SubstanceType target = CONVERTS.get(entry.getKey());
            if (target == null)
                continue;

            float moved = store.remove(entry.getKey(), ml);
            if (moved > 0f)
            {
                store.add(target, moved, capacity);
                changed = true;
            }
        }

        return changed;
    }

    // Remove ml from the largest EVAPORABLE component (meds aren't evaporable,
    // so they stay and concentrate). If no solvent remains, scorch the largest
    // remaining substance into JUNK instead.
    public static boolean evaporate(FluidMixture store, float ml, float capacity)
    {
        SubstanceType solvent = largest(store, true);
        if (solvent != null)
            return store.remove(solvent, ml) > 0f;

        SubstanceType remainder = largestNonJunk(store);
        if (remainder != null)
        {
            float moved = store.remove(remainder, ml);
            if (moved > 0f)
            {
                store.add(SubstanceType.JUNK, moved, capacity);
                return true;
            }
        }
        return false;
    }

    // === HELPER METHODS ===

    private static SubstanceType largest(FluidMixture store, boolean evaporableOnly)
    {
        SubstanceType best = null;
        float bestAmount = 0f;
        for (Map.Entry<SubstanceType, Float> entry : store.getComponents().entrySet())
        {
            if (evaporableOnly && !EVAPORABLE.contains(entry.getKey()))
                continue;

            if (entry.getValue() > bestAmount)
            {
                bestAmount = entry.getValue();
                best = entry.getKey();
            }
        }

        return best;
    }

    private static SubstanceType largestNonJunk(FluidMixture store)
    {
        SubstanceType best = null;
        float bestAmount = 0f;
        for (Map.Entry<SubstanceType, Float> entry : store.getComponents().entrySet())
        {
            if (entry.getKey() == SubstanceType.JUNK)
                continue;

            if (entry.getValue() > bestAmount)
            {
                bestAmount = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    // === ACESSORS ===

    public static SubstanceType conversionTarget(SubstanceType type) { return CONVERTS.get(type); }
    public static boolean isEvaporable(SubstanceType type) { return EVAPORABLE.contains(type); }
}