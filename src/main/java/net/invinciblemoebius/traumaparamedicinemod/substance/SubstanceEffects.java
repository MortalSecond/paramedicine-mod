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
    private float painMin, painMax, painStrength, painLerp;
    private boolean painLocalOnly = false;
    private float respThreshold, respCoefficient;
    private float plasmaFraction = 0f;
    private float redCellFraction = 0f;
    private float chronoMin, chronoMax, chronoMaxDelta;
    private float vasoDeltaPerUnit = 0f;
    private float reversalLerp = 0f;
    private float reversalSpo2PerUnit = 0f;
    private float woundInfectionKill = 0f;
    private boolean woundDeepOnly = false;
    private float infectionGrowthMult = 1f;
    private float bacteremiaKill = 0f;

    // === CONFIGURATION METHODS ===

    public SubstanceEffects reducesPain(float min, float max, float strength, float lerp)
    {
        painMin = min;
        painMax = max;
        painStrength = strength;
        painLerp = lerp;
        return this;
    }

    public SubstanceEffects locallyOnly()
    {
        painLocalOnly = true;
        return this;
    }

    public SubstanceEffects respiratoryDepression(float threshold, float coefficient)
    {
        respThreshold = threshold;
        respCoefficient = coefficient;
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

    public SubstanceEffects chronotropic(float min, float max, float maxBPMDelta)
    {
        chronoMin = min;
        chronoMax = max;
        chronoMaxDelta = maxBPMDelta;
        return this;
    }

    public SubstanceEffects vasoactive(float vascularToneDeltaPerUnit)
    {
        vasoDeltaPerUnit = vascularToneDeltaPerUnit;
        return this;
    }

    public SubstanceEffects reversesOpioidEffects(float lerp, float spo2PerUnit)
    {
        reversalLerp = lerp;
        reversalSpo2PerUnit = spo2PerUnit;
        return this;
    }

    public SubstanceEffects reducesWoundInfection(float perSecAtFullConcentration, boolean deepOnly)
    {
        woundInfectionKill = perSecAtFullConcentration;
        woundDeepOnly = deepOnly;
        return this;
    }

    public SubstanceEffects bacteriostatic(float growthMultiplier)
    {
        infectionGrowthMult = Math.max(0f, Math.min(1f, growthMultiplier));
        return this;
    }

    public SubstanceEffects reducesBacteremia(float perSecAtFullConcentration)
    {
        bacteremiaKill = perSecAtFullConcentration;
        return this;
    }

    // === APPLICATION METHODS ===

    public void apply(float concentration, float eliminatedThisTick, float dt, PlayerHealthData data, @Nullable LimbData locationLimb)
    {
        boolean systemic = (locationLimb == null);

        if (painStrength > 0f)
            applyPain(concentration, dt, data, locationLimb);
        if (respCoefficient > 0f && systemic)
            applyRespDepression(concentration, dt, data);
        if (plasmaFraction > 0f || redCellFraction > 0f)
            applyBulkFluid(eliminatedThisTick, locationLimb, data);
        if (chronoMaxDelta != 0f && systemic)
            data.addChronotropicModifier(therapeuticCurve(concentration, chronoMin, chronoMax) * chronoMaxDelta);
        if (vasoDeltaPerUnit != 0f && systemic)
            data.addVascularToneModifier(vasoDeltaPerUnit * concentration);
        if (reversalLerp > 0f)
            applyReversal(concentration, dt, data);
        if (woundInfectionKill > 0f && systemic)
            data.applyAntibioticToReachableWounds(woundInfectionKill * concentration * dt, woundDeepOnly);
        if (infectionGrowthMult > 0f && systemic)
            data.applyInfectionGrowthModifier(infectionGrowthMult);
        if (bacteremiaKill > 0f && systemic)
            data.setBacteremia(data.getBacteremia() - bacteremiaKill *  concentration * dt);
    }

    private void applyPain(float concentration, float dt, PlayerHealthData data, @Nullable LimbData loc)
    {
        float relief = therapeuticCurve(concentration, painMin, painMax) * painStrength;
        if (relief <= 0f)
            return;

        if (painLocalOnly)
        {
            if (loc == null)
                return;

            float current = loc.getSensitivity();
            loc.setSensitivity(lerp(current, Math.max(0f, current - relief), dt * painLerp));
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

    private void applyRespDepression(float concentration, float dt, PlayerHealthData data)
    {
        if (concentration <= respThreshold)
            return;

        float excess = concentration - respThreshold;
        data.setOxygenSaturation(data.getOxygenSaturation() - (excess * excess * respCoefficient * dt));
    }

    // UHHHH. Let me explain this one.
    // IRL saline doesn't become 100% plasma, it's only like 1/3rd. The rest typically
    // leaks into the surrounding tissue. It then, literally, gets pissed out through urine.
    // HOWEVER. Since i am NEVER going to implement a piss system, i am instead making it
    // so the 'unused' saline (or any bulk in general) vanishes overtime.
    // So, the increasePlasma/RBC methods take in the FRACTION of which this substance
    // will actually become plasma, then the rest gets voided.
    private void applyBulkFluid(float eliminated, @Nullable LimbData loc, PlayerHealthData data)
    {
        if (eliminated <= 0f)
            return;

        LimbData target = (loc != null) ? loc : data.getLimb(LimbNode.UPPER_TORSO);
        if (target == null)
            return;

        if (plasmaFraction > 0f)
            target.setPlasmaVolume(target.getPlasmaVolume() + eliminated * plasmaFraction);
        if (redCellFraction > 0f)
            target.setRedCellVolume(target.getRedCellVolume() + eliminated * redCellFraction);
    }

    private void applyReversal(float concentration, float dt, PlayerHealthData data)
    {
        if (concentration < 0.0001f)
            return;

        data.getLimbs().values().forEach(limb ->
        {
            float current = limb.getSensitivity();
            if (current < 1.0f)
                limb.setSensitivity(lerp(current, 1.0f, concentration * dt * reversalLerp));
        });

        if (reversalSpo2PerUnit > 0f)
            data.setOxygenSaturation(Math.min(data.getOxygenSaturation() + concentration * reversalSpo2PerUnit * dt, ModConstants.SPO2_NORMAL));
    }

    // === HELPER METHODS ===

    private static float therapeuticCurve(float concentration, float min, float max)
    {
        if (concentration <= min)
            return 0f;
        if (concentration >= max)
            return 1f;

        return (concentration - min) / (max - min);
    }

    private static float lerp(float a, float b, float t)
    {
        return a + (b - a) * Math.max(0f, Math.min(1f, t));
    }
}
