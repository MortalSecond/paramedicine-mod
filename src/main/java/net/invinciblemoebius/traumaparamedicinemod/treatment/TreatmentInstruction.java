package net.invinciblemoebius.traumaparamedicinemod.treatment;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.substance.CirculatingSubstance;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;

import javax.annotation.Nullable;
import java.util.Map;

// Administers a fluid payload by a chosen route. Each route crosses a different barrier:
// IV none, IM perfusion, ORAL the gut, TOPICAL none (surface-acting).
public class TreatmentInstruction
{
    private final RouteOfEntry route;
    private final FluidMixture payload;
    @Nullable private final LimbNode node;

    private TreatmentInstruction(RouteOfEntry route, FluidMixture payload, @Nullable LimbNode node)
    {
        this.route = route;
        this.payload = payload;
        this.node = node;
    }

    // === FACTORIES ===

    public static TreatmentInstruction intravenous(FluidMixture payload)
    {
        return new TreatmentInstruction(RouteOfEntry.IV, payload, null);
    }
    public static TreatmentInstruction intramuscular(FluidMixture payload, LimbNode node)
    {
        return new TreatmentInstruction(RouteOfEntry.IM, payload, node);
    }
    public static TreatmentInstruction oral(FluidMixture payload)
    {
        return new TreatmentInstruction(RouteOfEntry.ORAL, payload, null);
    }
    public static TreatmentInstruction topical(FluidMixture payload, LimbNode node)
    {
        return new TreatmentInstruction(RouteOfEntry.TOPICAL, payload, node);
    }

    // === APPLY ===

    public boolean apply(PlayerHealthData data)
    {
        if (payload == null || payload.isEmpty())
            return false;

        return switch (route)
        {
            case IV -> applyIV(data);
            case IM -> applyIM(data);
            case ORAL -> applyOral(data);
            case TOPICAL -> applyTopical(data);
        };
    }

    // Straight to blood, no gate.
    private boolean applyIV(PlayerHealthData data)
    {
        for (Map.Entry<SubstanceType, Float> entry : payload.getComponents().entrySet())
            data.depositSystemicSubstance(entry.getKey(), entry.getValue(), ModConstants.IV_ONSET_SECONDS);

        payload.clear();
        return true;
    }

    // Muscle depot. Absorption is perfusion-gated because the local pool drains by perfusion,
    // so IM into a tourniqueted or shut-down limb barely absorbs.
    private boolean applyIM(PlayerHealthData data)
    {
        LimbData limb = (node != null) ? data.getLimb(node) : null;
        if (limb == null)
            return false;

        for (Map.Entry<SubstanceType, Float> entry : payload.getComponents().entrySet())
            limb.addLocalSubstance(new CirculatingSubstance(entry.getKey(), entry.getValue(), ModConstants.IM_ONSET_SECONDS));

        payload.clear();
        return true;
    }

    // Into the stomach. The gut tick applies oralBioavailability over the emptying curve.
    private boolean applyOral(PlayerHealthData data)
    {
        data.ingestOrally(payload);
        payload.clear();
        return true;
    }

    // Surface-acting. Irrigation flushes debris (contamination down), but the fluid's own bacterial
    // load re-dirties so clean water helps and pond water hurts.
    // Consumes ONLY the volume the node's wounds need, leaving the rest so a 1 mL splash can't
    // clean a whole ass limb.
    private boolean applyTopical(PlayerHealthData data)
    {
        LimbData limb = (node != null) ? data.getLimb(node) : null;
        if (limb == null || limb.getWounds().isEmpty())
            return false;

        float available = payload.totalVolume();
        if (available <= 0f)
            return false;

        // What this node's wounds need to flush properly.
        float needed = 0f;
        for (Wound wound : limb.getWounds())
            needed += ModConstants.IRRIGATION_ML_PER_WOUND + wound.getSize() * ModConstants.IRRIGATION_SIZE_FACTOR;

        // Dirtiness per unit of THIS fluid (before draining).
        float total = payload.totalVolume();
        float load = 0f;
        for (Map.Entry<SubstanceType, Float> entry : payload.getComponents().entrySet())
            load += (entry.getValue() / total) * TopicalProfile.contaminationLoad(entry.getKey());

        // Remnants stays in the container
        float used = Math.min(needed, available);
        float met = (needed > 0f) ? used / needed : 0f;
        FluidMixture usedSlice = payload.drain(used);

        // A negative number equals a net cleaner.
        float flush = ModConstants.TOPICAL_MAX_FLUSH * met;
        float dirty = load * met * ModConstants.TOPICAL_DIRTY_STRENGTH;
        float delta = dirty - flush;

        // Sets their irrigation state to true.
        for (Wound wound : limb.getWounds())
        {
            wound.setContamination(wound.getContamination() + delta);
            if (delta < 0f)
                wound.setIrrigated(true);
        }
        limb.markDirty();

        // Transdermal (default 0 just in case).
        for (Map.Entry<SubstanceType, Float> entry : usedSlice.getComponents().entrySet())
        {
            float absorption = TopicalProfile.absorption(entry.getKey());
            if (absorption > 0f)
                data.depositSystemicSubstance(entry.getKey(), entry.getValue() * absorption, ModConstants.TOPICAL_ONSET_SECONDS);
        }
        return true;
    }
}