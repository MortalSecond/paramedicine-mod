package net.invinciblemoebius.traumaparamedicinemod.wound;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.PowderMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Dressing
{
    // PLACEHOLDERS
    // Basically, since i think applying individual bandages per wound would cause serious
    // bandage shortages, i think it should be applied per-node. However, to calculate
    // HOW many wounds in a node the length of a bandage can cover without the bandaging
    // minigame, this is a good in-between proxy.
    private float length;
    // Compression proxy. Mechanically reduces bleed. Becomes wrap-tightness later.
    private float pressure;
    // Related to the above. This shows how fast or how tight the bandage can be applied
    // without breaking. Low elasticity means low maximum pressure because trying to
    // apply more pressure than its elasticity can meet will tear the bandage.
    private float elasticity;
    // PROPERTIES
    // Barrier quality. Drives fouling interval, and is a contamination source when low.
    private float cleanliness;
    // How much it helps clotting.
    private float hemostatic;
    // Actively pulls contamination down while worn.
    private float antiseptic;
    // How airtight of a seal it has; 1.0 is for chest wounds. Traps bacteria on a dirty wound.
    private float occlusion;
    // Sticks to the wound. High adherence can rip it open on removal.
    private float adherence;
    // Exudate capacity before it's soaked.
    private float absorption;
    // STATE
    private long ageTicks = 0L;
    // How many "coats" of enhancements it has.
    private int coats = 0;
    private float absorbedML = 0f;

    private Dressing() {}

    // === BUILDER ===

    public static Builder builder() { return new Builder(); }

    public static class Builder
    {
        private final Dressing d = new Dressing();
        public Builder cleanliness(float v) { d.cleanliness = v; return this; }
        public Builder hemostatic(float v) { d.hemostatic = v; return this; }
        public Builder antiseptic(float v) { d.antiseptic = v; return this; }
        public Builder occlusion(float v) { d.occlusion = v; return this; }
        public Builder adherence(float v) { d.adherence = v; return this; }
        public Builder absorption(float v) { d.absorption = v; return this; }
        public Builder pressure(float v) { d.pressure = v; return this; }
        public Builder length(float v) { d.length = v; return this; }
        public Builder elasticity(float v) { d.elasticity = v; return this; }
        public Dressing build() { return d; }
    }

    // === ENHANCEMENT METHODS ===

    public void enhanceFluid(FluidMixture taken)
    {
        float total = taken.total();
        float dose = Math.min(1f, total / ModConstants.DRESSING_AGENT_REF_FLUID_ML);
        applyDeltas(taken.getComponents(), total, dose);

        // Soaking makes the dressing less porous, thus becomes more occlusive.
        occlusion = clamp(occlusion + ModConstants.DRESSING_SATURATION_OCCLUSION * Math.min(1f, total / ModConstants.DRESSING_ABSORB_CAPACITY_ML));
        coats++;
    }

    public void enhancePowder(PowderMixture taken)
    {
        float total = taken.total();
        float dose = Math.min(1f, total / ModConstants.DRESSING_AGENT_REF_POWDER_MG);
        applyDeltas(taken.getComponents(), total, dose);

        coats++;
    }

    private void applyDeltas(Map<SubstanceType, Float> comp, float total, float dose)
    {
        if (total <= 0f)
            return;

        for (Map.Entry<SubstanceType, Float> entry : comp.entrySet())
        {
            DressingEnhancement.Delta delta = DressingEnhancement.get(entry.getKey());
            if (delta == null)
                continue;

            // Fraction of dose times how full the dose is
            float w = (entry.getValue() / total) * dose;
            cleanliness = clamp(cleanliness + delta.clean() * w);
            hemostatic = clamp(hemostatic + delta.hemo() * w);
            antiseptic = clamp(antiseptic + delta.anti() * w);
            occlusion = clamp(occlusion + delta.occ() * w);
            adherence = clamp(adherence + delta.adh() * w);
            absorption = clamp(absorption + delta.abs() * w);
        }
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    // === SUMMARY ===

    // Qualitative bands for the dressing station UI. Trivial properties are hidden.
    public List<String> summaryBands()
    {
        List<String> out = new ArrayList<>();
        band(out, cleanliness, "filthy", "grubby", "clean", "sterile");
        band(out, pressure,    null,     "light pressure", "holds pressure", "holds strong pressure");
        band(out, hemostatic,  null,     "mild clotting aid", "clotting agent", "strong clotting agent");
        band(out, antiseptic,  null,     "mildly antiseptic", "antiseptic", "strongly antiseptic");
        band(out, occlusion,   null,     "semi-breathable", "semi-occlusive", "airtight seal");
        band(out, adherence,   null,     "barely sticks", "sticks", "clings hard");
        band(out, absorption,  null,     "low absorption", "absorbent", "very absorbent");

        if (saturationFraction() >= 1f)
            out.add("soaked through");
        if (isOverdue())
            out.add("needs changing");

        return out;
    }

    // Four-rung ladder. A null low-label hides the property when it's negligible.
    private static void band(List<String> out, float v, String lo, String mid, String hi, String top)
    {
        String label;
        if (v >= 0.75f) label = top;
        else if (v >= 0.5f) label = hi;
        else if (v >= 0.2f) label = mid;
        else if (v >= 0.05f) label = lo;
        else label = null;

        if (label != null) out.add(label);
    }

    // === QUERIES ===

    // Fraction of exudate capacity used. A non-absorbent dressing counts as instantly soaked.
    public float saturationFraction()
    {
        if (absorption <= 0f)
            return 1f;

        float cap = absorption * ModConstants.DRESSING_ABSORB_CAPACITY_ML;
        return Math.min(1f, absorbedML / cap);
    }

    // Ticks before this dressing is overdue. Cleaner lasts longer, soaked halves its life.
    public float changeIntervalTicks()
    {
        float base = ModConstants.DRESSING_FOUL_BASE_TICKS * Math.max(0.05f, cleanliness);
        return base * (1f - 0.5f * saturationFraction());
    }

    public boolean isOverdue()
    {
        return ageTicks > changeIntervalTicks() || saturationFraction() >= 1f;
    }

    // Multiplier applied to bleed rate. Arterial bleeds resist compression.
    public float pressureBleedFactor(boolean arterial)
    {
        float max = arterial ? ModConstants.DRESSING_PRESSURE_STOP_ARTERIAL : ModConstants.DRESSING_PRESSURE_STOP_VENOUS;
        return 1f - pressure * max;
    }

    // Ages the dressing and soaks up the blood flowing through it.
    public void tickWear(float bleedRateML, float dt)
    {
        ageTicks++;
        if (absorption > 0f && bleedRateML > 0f)
            absorbedML += bleedRateML * dt;
    }

    // === ACCESSORS ===

    public float getCleanliness() { return cleanliness; }
    public float getHemostatic() { return hemostatic; }
    public float getAntiseptic() { return antiseptic; }
    public float getOcclusion() { return occlusion; }
    public float getAdherence() { return adherence; }
    public float getAbsorption() { return absorption; }
    public float getPressure() { return pressure; }
    public float getLength() { return length; }
    public float getElasticity() { return elasticity; }
    public long getAgeTicks() { return ageTicks; }
    public int getCoats() { return coats; }
    public boolean canEnhance() { return coats < ModConstants.DRESSING_MAX_COATS; }

    // === SAVING STUFF ===

    public Dressing copy()
    {
        CompoundTag t = new CompoundTag();
        writeToNBT(t);
        return readFromNBT(t);
    }

    // It's quantized.
    private static float q(float v) { return Math.round(v * 100f) / 100f; }

    public void writeToNBT(CompoundTag tag)
    {
        tag.putFloat("Cleanliness", q(cleanliness));
        tag.putFloat("Hemostatic", q(hemostatic));
        tag.putFloat("Antiseptic", q(antiseptic));
        tag.putFloat("Occlusion", q(occlusion));
        tag.putFloat("Adherence", q(adherence));
        tag.putFloat("Absorption", q(absorption));
        tag.putFloat("Pressure", q(pressure));
        tag.putFloat("Length", q(length));
        tag.putFloat("Elastic", q(elasticity));
        tag.putLong ("Age", ageTicks);
        tag.putFloat("Soaked", q(absorbedML));
        tag.putInt("Coats", coats);
    }

    public static Dressing readFromNBT(CompoundTag tag)
    {
        Dressing d = new Dressing();
        d.cleanliness = tag.getFloat("Cleanliness");
        d.hemostatic = tag.getFloat("Hemostatic");
        d.antiseptic = tag.getFloat("Antiseptic");
        d.occlusion = tag.getFloat("Occlusion");
        d.adherence = tag.getFloat("Adherence");
        d.absorption = tag.getFloat("Absorption");
        d.pressure = tag.getFloat("Pressure");
        d.length = tag.getFloat("Length");
        d.elasticity = tag.getFloat("Elastic");
        d.ageTicks = tag.getLong("Age");
        d.absorbedML = tag.getFloat("Soaked");
        d.coats = tag.getInt("Coats");

        return d;
    }
}