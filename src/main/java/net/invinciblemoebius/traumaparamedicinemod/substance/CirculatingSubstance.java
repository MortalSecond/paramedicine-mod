package net.invinciblemoebius.traumaparamedicinemod.substance;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

// Represents ONE substance inside the player's bloodstream.
// A player can have multiple substances simultanously.
// Concentration is DERIVED each tick, calculated as (amount / blood volume).
// "Blood volume" refers to the whole body's total blood volume when the substance
// is systemic (AKA if it can reach the heart).
// But it refers to a limb's local blood reserve if it's compartmentalized.
public class CirculatingSubstance
{
    // === IDENTITY ===
    private SubstanceType type;
    private LimbNode entryPoint;
    // Mililiters currently remaining in the body.
    private float amountML;
    public static final float NEGLIGIBLE_THRESHOLD = 0.0001f;
    // Seconds until this substance reaches full effect.
    private float onsetSeconds;
    private float ageSeconds = 0f;
    // Whether this substance can reach systemic circulation.
    private boolean isSystemic;

    // === CONSTRUCTORS ===

    public CirculatingSubstance(){}

    public CirculatingSubstance(SubstanceType type, float amountML, LimbNode entryPoint, float onsetSeconds, boolean isSystemic)
    {
        this.type = type;
        this.amountML = amountML;
        this.entryPoint = entryPoint;
        this.onsetSeconds = onsetSeconds;
        this.isSystemic = isSystemic;
    }

    // === TICK METHODS ===

    // Returns true if this instance should be removed.
    public boolean tick(PlayerHealthData data, Map<LimbNode, LimbData> allLimbs)
    {
        float dt = ModConstants.SECONDS_PER_TICK;

        // Re-evaluate systemic status.
        if (entryPoint != null)
        {
            LimbData entryLimb = allLimbs.get(entryPoint);
            if (entryLimb != null)
                isSystemic = entryLimb.hasProximalCirculation(entryPoint, allLimbs);
            else
                isSystemic = true; // Null entry point = inject it directly to the central line.
        }

        // Advance onset.
        ageSeconds += dt;
        float onsetProgress = (onsetSeconds > 0f) ? Math.min(1f, ageSeconds / onsetSeconds) : 1f;

        // Compute effective concentration.
        float effectiveVolume = computeEffectiveBloodVolume(data, allLimbs);
        float rawConcentration = (effectiveVolume > 0f) ? (amountML / effectiveVolume) : 0f;
        float concentration = rawConcentration * onsetProgress;

        // Apply effects.
        type.applyEffects(concentration, dt, data, entryPoint, isSystemic);

        // First-order elimination.
        // Looks strange? It should! This uses lambda (the rate of decay)
        // to calculate the remaining amount after being consumed.
        float lambda = (float) (Math.log(2) / type.halfLifeSeconds);
        amountML *= (1f - lambda * dt);
        amountML = Math.max(0f, amountML);

        return amountML < NEGLIGIBLE_THRESHOLD;
    }

    // Returns the blood volume this substance is being dissolved in.
    // If systemic, it uses the total blood volume.
    // If compartmentalized, it uses only the entry limb's blood volume.
    // This method makes it so dilution, overdose, and bolus effects arise naturally.
    private float computeEffectiveBloodVolume(PlayerHealthData data, Map<LimbNode, LimbData> allLimbs)
    {
        if (isSystemic)
            return data.getBloodVolume();

        if (entryPoint != null)
        {
            LimbData limb = allLimbs.get(entryPoint);
            if (limb != null)
                return Math.max(1f, limb.getActualBloodVolume());
        }

        // A fallback, just in case.
        return data.getBloodVolume();
    }

    // === ACCESSORS ===

    public SubstanceType getType() { return type; }
    public LimbNode getEntryPoint() { return entryPoint; }
    public float getAmountML() { return amountML; }
    public boolean isSystemic() { return isSystemic; }
    public float getAgeSeconds() { return ageSeconds; }

    public float getConcentration(float bloodVolumeML)
    {
        // Edge case for exsanguination.
        if (bloodVolumeML <= 0f) return 0f;

        float onsetProgress = (onsetSeconds > 0f) ? Math.min(1f, ageSeconds / onsetSeconds) : 1f;

        return (amountML / bloodVolumeML) * onsetProgress;
    }

    // === SAVING STUFF ===

    public void saveToNBT(CompoundTag tag)
    {
        tag.putString("Type", type.name());
        tag.putString("EntryPoint", entryPoint != null ? entryPoint.name() : "NONE");
        tag.putFloat ("AmountML", amountML);
        tag.putFloat ("OnsetSeconds", onsetSeconds);
        tag.putFloat ("AgeSeconds", ageSeconds);
        tag.putBoolean("IsSystemic", isSystemic);
    }

    public void loadFromNBT(CompoundTag tag)
    {
        type  = SubstanceType.valueOf(tag.getString("Type"));
        String ep = tag.getString("EntryPoint");
        entryPoint = ep.equals("NONE") ? null : LimbNode.valueOf(ep);
        amountML = tag.getFloat("AmountML");
        onsetSeconds = tag.getFloat("OnsetSeconds");
        ageSeconds = tag.getFloat("AgeSeconds");
        isSystemic = tag.getBoolean("IsSystemic");
    }

    // === DEBUG ===

    @Override
    public String toString()
    {
        return String.format(
                "CirculatingSubstance{%s %.4fml via %s systemic=%b age=%.1fs onset=%.1fs}",
                type, amountML, entryPoint, isSystemic, ageSeconds, onsetSeconds
        );
    }
}
