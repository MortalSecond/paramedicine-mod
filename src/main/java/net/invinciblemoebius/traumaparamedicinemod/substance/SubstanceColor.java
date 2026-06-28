package net.invinciblemoebius.traumaparamedicinemod.substance;

import java.util.EnumMap;
import java.util.Map;

// This is what gives colors to the substances. Now, by default, the color
// is white, BUT, it can be overriden by the cases inside the static.
public class SubstanceColor
{
    private SubstanceColor()
    {
    }

    private static final int DEFAULT = 0xFFE8E8E8;
    private static final Map<SubstanceType, Integer> COLORS = new EnumMap<>(SubstanceType.class);

    static
    {
        COLORS.put(SubstanceType.WATER, 0xFF3A7CC4);
        COLORS.put(SubstanceType.BOILED_WATER, 0xFF5B97D6);
    }

    public static int get(SubstanceType type)
    {
        return COLORS.getOrDefault(type, DEFAULT);
    }
}
