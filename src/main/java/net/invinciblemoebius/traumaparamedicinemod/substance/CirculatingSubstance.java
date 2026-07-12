package net.invinciblemoebius.traumaparamedicinemod.substance;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
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
    // Mililiters currently remaining in the body.
    private float amountML;
    public static final float NEGLIGIBLE_THRESHOLD = 0.0001f;
    // Seconds until this substance reaches full effect.
    private float onsetSeconds;
    private float ageSeconds = 0f;

    // === CONSTRUCTORS ===

    public CirculatingSubstance(){}

    public CirculatingSubstance(SubstanceType type, float amountML, float onsetSeconds)
    {
        this.type = type;
        this.amountML = amountML;
        this.onsetSeconds = onsetSeconds;
    }

    // === TICK METHODS ===

    // Returns true if this instance should be removed.
    // ambientVolume = local blood volume if substance is compartmentalized in a node, otherwise it's the total blood volume.
    // locationLimb = the node the substance currently occupies. Null if it's systemic.
    public boolean tick(float ambientVolume, float dt, float perfusionFactor, PlayerHealthData data, @Nullable LimbData locationLimb)
    {
        ageSeconds += dt;

        float consumed;

        if (type.isVolumeInfusion())
        {
            // No concentration effect. The bolus infuses into the bloodstream at a perfusion-scaled rate.
            // Epi speeds it, vasodilation slows it, asystole halts it.
            consumed = Math.min(amountML, ModConstants.INFUSION_RATE_ML_PER_SEC * perfusionFactor * dt);
            type.applyEffects(0f, consumed, dt, data, locationLimb);
        }
        else
        {
            // Drug. Concentration-driven effect, eliminated by half-life. Pharmacology unchanged.
            float onsetProgress = (onsetSeconds > 0f) ? Math.min(1f, ageSeconds / onsetSeconds) : 1f;
            float concentration = (ambientVolume > 0f) ? (amountML / ambientVolume) * onsetProgress : 0f;

            float lambda = (float) Math.log(2) / type.halfLifeSeconds;
            consumed = Math.min(amountML, amountML * lambda * dt);

            type.applyEffects(concentration, consumed, dt, data, locationLimb);
        }

        // Every consumed millilitre is fluid and joins the bloodstream per the substance's yields — the
        // drug's solvent as plasma, the expander's volume as plasma + cells. Systemic substances deposit
        // into the core, redistribution spreads it next tick..
        if (consumed > 0f && (type.plasmaYield > 0f || type.redCellYield > 0f))
        {
            LimbData target = (locationLimb != null) ? locationLimb : data.getLimb(LimbNode.UPPER_TORSO);
            if (target != null)
            {
                target.setPlasmaVolume(target.getPlasmaVolume() + consumed * type.plasmaYield);
                target.setRedCellVolume(target.getRedCellVolume() + consumed * type.redCellYield);
            }
        }

        amountML = Math.max(0f, amountML - consumed);
        return amountML < NEGLIGIBLE_THRESHOLD;
    }

    public CirculatingSubstance splitOff(float ml)
    {
        CirculatingSubstance substance = new CirculatingSubstance(type, ml, onsetSeconds);
        substance.ageSeconds = this.ageSeconds;
        return substance;
    }

    public void reduceAmount(float ml)
    {
        amountML = Math.max(0f, amountML - ml);
    }

    public void setAmountML(float ml)
    {
        amountML = Math.max(0f, ml);
    }

    public void setAgeSeconds(float s)
    {
        ageSeconds = s;
    }

    // === ACCESSORS ===

    public SubstanceType getType() { return type; }
    public float getAmountML() { return amountML; }
    public float getAgeSeconds() { return ageSeconds; }

    // === SAVING STUFF ===

    public void saveToNBT(CompoundTag tag)
    {
        tag.putString("Type", type.name());
        tag.putFloat ("AmountML", amountML);
        tag.putFloat ("OnsetSeconds", onsetSeconds);
        tag.putFloat ("AgeSeconds", ageSeconds);
    }

    public void loadFromNBT(CompoundTag tag)
    {
        type  = SubstanceType.valueOf(tag.getString("Type"));
        amountML = tag.getFloat("AmountML");
        onsetSeconds = tag.getFloat("OnsetSeconds");
        ageSeconds = tag.getFloat("AgeSeconds");
    }
}
