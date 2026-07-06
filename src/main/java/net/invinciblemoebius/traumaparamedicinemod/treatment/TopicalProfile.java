package net.invinciblemoebius.traumaparamedicinemod.treatment;

import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;

import java.util.EnumMap;
import java.util.Map;

// External-route properties of a substance. How much crosses intact skin (transdermal),
// and how much bacterial load it carries onto a wound.
// Giving it no params is the same as giving it (0, 0). By default, sterile and surface-only.
public final class TopicalProfile
{
    private TopicalProfile() {}

    public record Properties(float absorption, float contaminationLoad) {}

    private static final Map<SubstanceType, Properties> TABLE = new EnumMap<>(SubstanceType.class);

    static
    {
        // Dirty fluids carry bacteria onto the wound.
        TABLE.put(SubstanceType.JUNK, new Properties(0f, 0.90f));
        TABLE.put(SubstanceType.FOREIGN_FLUID, new Properties(0f, 0.40f));
        TABLE.put(SubstanceType.WATER, new Properties(0f, 0.25f));
        TABLE.put(SubstanceType.PLANT_MATTER, new Properties(0f, 0.20f));

        // Sterile/clean irrigants (BOILED_WATER, SALINE, REHYDRATION_FLUID, etc) default to 0 load.
        // Right now there's no real absorption over zero, but in the future for stuff like lidocaine patches,
        // absorption would be the >0 cases because it gets absorbed past the skin.
    }

    public static float absorption(SubstanceType type)
    {
        Properties properties = TABLE.get(type);
        return properties == null ? 0f : properties.absorption();
    }
    public static float contaminationLoad(SubstanceType type)
    {
        Properties properties = TABLE.get(type);
        return properties == null ? 0f : properties.contaminationLoad();
    }
}