package net.invinciblemoebius.traumaparamedicinemod.substance;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;

import javax.annotation.Nullable;

// This are the actual physiological effects that a substance can apply.
// Essentially to make SubstanceType be more readable and exercise good separation of concerns.
public class SubstanceEffects
{
    private float painUnderdose, painTherapeutic, painStrength, painLerp;
    private boolean painLocalOnly = false;
    private float respUnderdose, respTherapeutic, respTherapeuticFloor;
    private float respToxicThreshold, respToxicCoefficient;
    private float plasmaFraction = 0f, redCellFraction = 0f;
    private float chronoUnderdose, chronoTherapeutic, chronoTherapeuticDelta;
    private float chronoToxicThreshold, chronoToxicCoefficient;
    private float vasoDeltaPerUnit = 0f;
    private float reversalUnderdose, reversalTherapeutic, reversalLerp, reversalSpo2PerUnit;
    private float woundInfectionKill = 0f;
    private boolean woundDeepOnly = false;
    private float infectionGrowthMult = 1f;
    private float bacteremiaKill = 0f;
    private float clottingBoost = 1f;
    private float clotUnderdose, clotTherapeutic;

    // === CONFIGURATION METHODS ===

    public SubstanceEffects reducesPain(float underdose, float therapeutic, float strength, float lerp)
    {
        painUnderdose = underdose;
        painTherapeutic = therapeutic;
        painStrength = strength;
        painLerp = lerp;
        return this;
    }

    public SubstanceEffects locallyOnly()
    {
        painLocalOnly = true;
        return this;
    }

    public SubstanceEffects suppressesRespiration(float underdose, float therapeutic, float therapeuticFloor, float toxicThreshold, float toxicCoefficient)
    {
        respUnderdose = underdose;
        respTherapeutic = therapeutic;
        respTherapeuticFloor = therapeuticFloor;
        respToxicThreshold = toxicThreshold;
        respToxicCoefficient = toxicCoefficient;
        return this;
    }

    public SubstanceEffects increasesPlasma(float fraction)
    {
        plasmaFraction = fraction;
        return this;
    }
    public SubstanceEffects increasesRedCells(float fraction)
    {
        redCellFraction = fraction;
        return this;
    }

    public SubstanceEffects chronotropic(float underdose, float therapeutic, float therapeuticDelta, float toxicThreshold, float toxicCoefficient)
    {
        chronoUnderdose = underdose;
        chronoTherapeutic = therapeutic;
        chronoTherapeuticDelta = therapeuticDelta;
        chronoToxicThreshold = toxicThreshold;
        chronoToxicCoefficient = toxicCoefficient;
        return this;
    }

    public SubstanceEffects vasoactive(float toneDeltaPerUnit)
    {
        vasoDeltaPerUnit = toneDeltaPerUnit;
        return this;
    }

    public SubstanceEffects reversesOpioidEffects(float underdose, float therapeutic, float lerp, float spo2PerUnit)
    {
        reversalUnderdose = underdose;
        reversalTherapeutic = therapeutic;
        reversalLerp = lerp;
        reversalSpo2PerUnit = spo2PerUnit;
        return this;
    }

    public SubstanceEffects reducesWoundInfection(float perSecAtFullConc, boolean deepOnly)
    {
        woundInfectionKill = perSecAtFullConc;
        woundDeepOnly = deepOnly;
        return this;
    }

    public SubstanceEffects bacteriostatic(float growthMultiplier)
    {
        infectionGrowthMult = Math.max(0f, Math.min(1f, growthMultiplier));
        return this;
    }

    public SubstanceEffects reducesBacteremia(float perSecAtFullConc)
    {
        bacteremiaKill = perSecAtFullConc;
        return this;
    }

    public SubstanceEffects boostsClotting(float maxMultiplier, float underdose, float therapeutic)
    {
        clottingBoost = maxMultiplier;
        clotUnderdose = underdose;
        clotTherapeutic = therapeutic;
        return this;
    }

    // === APPLICATION METHODS ===

    public void apply(float concentration, float eliminatedThisTick, float dt, PlayerHealthData data, @Nullable LimbData locationLimb)
    {
        boolean systemic = (locationLimb == null);

        if (painStrength > 0f)
            applyPain(concentration, dt, data, locationLimb);
        if (respTherapeuticFloor > 0f || respToxicCoefficient > 0f)
            applyRespiratorySuppression(concentration, data);
        if (plasmaFraction > 0f || redCellFraction > 0f)
            applyBulkFluid(eliminatedThisTick, locationLimb, data);
        if (chronoTherapeuticDelta != 0f && systemic)
            applyChronotropic(concentration, data);
        if (vasoDeltaPerUnit != 0f && systemic)
            data.addVascularToneModifier(vasoDeltaPerUnit * concentration);
        if (reversalLerp > 0f)
            applyReversal(concentration, dt, data);
        if (woundInfectionKill > 0f && systemic)
            data.applyAntibioticToReachableWounds(woundInfectionKill * concentration * dt, woundDeepOnly);
        if (infectionGrowthMult < 1f && systemic)
            data.applyInfectionGrowthModifier(infectionGrowthMult);
        if (bacteremiaKill > 0f && systemic)
            data.setBacteremia(data.getBacteremia() - bacteremiaKill * concentration * dt);
        if (clottingBoost > 1f && systemic)
            applyClottingBoost(concentration, data);
    }

    private void applyPain(float concentration, float dt, PlayerHealthData data, @Nullable LimbData location)
    {
        float relief = therapeuticCurve(concentration, painUnderdose, painTherapeutic) * painStrength;
        if (relief <= 0f)
            return;

        if (painLocalOnly)
        {
            if (location == null)
                return;

            float current = location.getSensitivity();
            location.setSensitivity(lerp(current, Math.max(0f, current - relief), dt * painLerp));
        }
        else
        {
            data.getLimbs().values().forEach(limb ->
            {
                float current = limb.getSensitivity();
                limb.setSensitivity(lerp(current, Math.max(0.05f, current - relief), dt * painLerp));
            });
        }
    }

    // Writes a ceiling to actual respiratory rate.
    // Therapeutic zone: ceiling lerps from 1.0 to therapeuticFloor (survivable slow breathing).
    // Toxic zone: ceiling continues past the floor toward 0 (apnea). No lower bound.
    // Effect is systemic: a substance in a local limb has no respiratory effect until it migrates.
    private void applyRespiratorySuppression(float concentration, PlayerHealthData data)
    {
        // Therapeutic suppression. Lowers ceiling from 1.0 to therapeuticFloor.
        float potency = therapeuticCurve(concentration, respUnderdose, respTherapeutic);
        float ceiling = 1.0f - potency * (1.0f - respTherapeuticFloor);

        // Toxic tail. No floor. Past the threshold, ceiling keeps falling toward 0.
        float toxicExcess = Math.max(0f, concentration - respToxicThreshold);
        ceiling -= toxicExcess * respToxicCoefficient;
        ceiling = Math.max(0f, ceiling);

        data.applyRespiratorySuppression(ceiling);
    }

    // UHHHH. Let me explain this one.
    // IRL saline doesn't become 100% plasma, it's only like 1/3rd. The rest typically
    // leaks into the surrounding tissue. It then, literally, gets pissed out through urine.
    // HOWEVER. Since i am NEVER going to implement a piss system, i am instead making it
    // so the 'unused' saline (or any bulk in general) vanishes overtime.
    // So, the increasePlasma/RBC methods take in the FRACTION of which this substance
    // will actually become plasma, then the rest gets voided.
    private void applyBulkFluid(float eliminated, @Nullable LimbData location, PlayerHealthData data)
    {
        if (eliminated <= 0f)
            return;

        LimbData target = (location != null) ? location : data.getLimb(LimbNode.UPPER_TORSO);
        if (target == null)
            return;

        if (plasmaFraction > 0f)
            target.setPlasmaVolume(target.getPlasmaVolume() + eliminated * plasmaFraction);
        if (redCellFraction > 0f)
            target.setRedCellVolume(target.getRedCellVolume() + eliminated * redCellFraction);
    }

    private void applyChronotropic(float concentration, PlayerHealthData data)
    {
        float therapeutic = therapeuticCurve(concentration, chronoUnderdose, chronoTherapeutic) * chronoTherapeuticDelta;
        float toxicExcess = Math.max(0f, concentration - chronoToxicThreshold);
        float toxic = toxicExcess * chronoToxicCoefficient;
        data.addChronotropicModifier(therapeutic + toxic);
    }

    // Reversal: uses its own therapeutic curve so a proper dose produces full reversal potency.
    private void applyReversal(float concentration, float dt, PlayerHealthData data)
    {
        float potency = therapeuticCurve(concentration, reversalUnderdose, reversalTherapeutic);
        if (potency <= 0f)
            return;

        // Lifts respiratory suppression by pushing a high ceiling.
        // This races against whatever opioid is still present.
        data.applyRespiratorySuppression(lerp(1.0f, 0.8f, 1.0f - potency)); // approaches 1.0 (no suppression).

        data.getLimbs().values().forEach(limb ->
        {
            float current = limb.getSensitivity();
            if (current < 1.0f) limb.setSensitivity(lerp(current, 1.0f, potency * dt * reversalLerp));
        });

        if (reversalSpo2PerUnit > 0f)
            data.setOxygenSaturation(Math.min(data.getOxygenSaturation() + potency * reversalSpo2PerUnit * dt, ModConstants.SPO2_NORMAL));
    }

    private void applyClottingBoost(float concentration, PlayerHealthData data)
    {
        float potency = therapeuticCurve(concentration, clotUnderdose, clotTherapeutic);
        data.applyClottingModifier(1f + (clottingBoost - 1f) * potency);
    }

    // === HELPER METHODS ===

    private static float therapeuticCurve(float concentration, float underdose, float therapeutic)
    {
        if (concentration <= underdose)
            return 0f;
        if (concentration >= therapeutic)
            return 1f;

        return (concentration - underdose) / (therapeutic - underdose);
    }

    private static float lerp(float a, float b, float t)
    {
        return a + (b - a) * Math.max(0f, Math.min(1f, t));
    }
}
