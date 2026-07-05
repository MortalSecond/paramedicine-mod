package net.invinciblemoebius.traumaparamedicinemod.wound;

import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;

import java.util.EnumMap;
import java.util.Map;

// Which substance nudges which dressing property, per full dose. Pressure is mechanical, so it's absent here.
public final class DressingEnhancement
{
    private DressingEnhancement() {}

    public record Delta(float clean, float hemo, float anti, float occ, float adh, float abs) {}

    private static final Map<SubstanceType, Delta> TABLE = new EnumMap<>(SubstanceType.class);

    static
    {
        TABLE.put(SubstanceType.TRANEXAMIC_ACID, new Delta(0f, 0.6f, 0f, 0f, 0f,  0f));
        TABLE.put(SubstanceType.METRONIDAZOLE, new Delta(0f, 0f, 0.5f, 0f, 0f, 0f));
        TABLE.put(SubstanceType.AMOXICILLIN_CLAVULANATE, new Delta(0f, 0f, 0.4f, 0f, 0f, 0f));
        TABLE.put(SubstanceType.CLAY, new Delta(-0.1f, 0f, 0f, 0.4f, 0.3f, 0f));
        TABLE.put(SubstanceType.BOILED_WATER, new Delta(0.3f, 0f, 0f, 0f, 0f, 0f));
        TABLE.put(SubstanceType.PURIFIED_WATER, new Delta(0.2f, 0f, 0f, 0f, 0f, 0f));
        TABLE.put(SubstanceType.REHYDRATION_FLUID, new Delta(0.35f, 0f, 0f, 0f, 0f, 0f));
        TABLE.put(SubstanceType.WATER, new Delta(-0.2f, 0f, 0f, 0f, 0f, 0f));
        TABLE.put(SubstanceType.JUNK, new Delta(-0.5f, 0f, 0f, 0f, 0f, 0f));
        TABLE.put(SubstanceType.PLANT_MATTER, new Delta(0f, 0f, 0f, 0f, 0f, 0.1f));
    }

    // Null = this substance does nothing to a dressing.
    public static Delta get(SubstanceType type) { return TABLE.get(type); }
}