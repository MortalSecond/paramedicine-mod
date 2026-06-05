package net.invinciblemoebius.traumaparamedicinemod.substance;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;

// Concentrations are expressed in mililiter of substance per mililiter of blood (ml/ml).
// They are NOT designed to be realistic, they're mostly for gameplay feel.
// Don't try these at home, kids!
public enum SubstanceType
{
    // === ANALGESICS - OPIOIDS ===

    MORPHINE(600)
            {
                // Half-life: 10 minutes
                // Therapeutic: 0.0002 - 0.0006 ml/ml
                // Toxic: Over 0.0010 ml/ml
                // Lethal: Over 0.0020 ml/ml
                @Override
                public void applyEffects(float concentration, float dt, PlayerHealthData data, LimbNode entryPoint, boolean isSystemic)
                {
                    // Compartmentalized; no systemic effect.
                    if (!isSystemic) return;

                    // Pain relief, scales down sensitivty across ALL nodes.
                    float reliefFactor = therapeuticCurve(concentration, 0.0002f, 0.0006f);
                    if (reliefFactor > 0f)
                    {
                        data.getLimbs().values().forEach(limb ->
                        {
                            float current = limb.getSensitivity();
                            float target = Math.max(0.05f, current - (reliefFactor * 0.8f));
                            limb.setSensitivity(lerp(current, target, dt * 0.1f));
                        });
                    }

                    // Respiratory depression above theraputic dose. SpO2 drops.
                    // It's quadratic, so it has a slow onset, but a sharp collapse.
                    if (concentration > 0.0006f)
                    {
                        float excess = (concentration - 0.0006f) / 0.0014f;
                        float spo2Drop = excess * excess * 0.003f * dt;
                        data.setOxygenSaturation(data.getOxygenSaturation() - spo2Drop);
                    }

                    // TODO: Drops blood pressure.
                }
            },
    KETAMINE(900f)
            {
                // Half-life: 15 minutes
                // Therapeutic: 0.0001 - 0.0005 ml/ml
                // Anesthetic: Over 0.0008 ml/ml
                // Toxic: Over 0.0020 ml/ml
                // Lethal: Comically large, so i'm not gonna implement it.
                @Override
                public void applyEffects(float concentration, float dt, PlayerHealthData data, LimbNode entryPoint, boolean isSystemic)
                {
                    // Compartmentalized; no effect.
                    if (!isSystemic) return;

                    // Pain relief, scales down sensitivty across ALL nodes.
                    float reliefFactor = therapeuticCurve(concentration, 0.0001f, 0.0005f);
                    if (reliefFactor > 0f)
                    {
                        data.getLimbs().values().forEach(limb ->
                        {
                            float current = limb.getSensitivity();
                            float target = Math.max(0.05f, current - (reliefFactor * 0.75f));
                            limb.setSensitivity(lerp(current, target, dt * 0.08f));
                        });
                    }
                }

                // TODO: Dissociation/Loss of consciousness.
            },

    // === ANALGESICS - LOCAL ANESTHETICS ===

    LIDOCAINE(300f)
            {
                // Half-life: 5 minutes
                // Therapeutic: 0.0005 - 0.0020 ml/ml
                // It technically can't be toxic since it's only local,
                // BUT it is toxic when introduced systemically.
                // If the player somehow gets a lidocaine IV, then:
                // Toxicity: Above 0.0010 ml/ml
                @Override
                public void applyEffects(float concentration, float dt, PlayerHealthData data, LimbNode entryPoint, boolean isSystemic)
                {
                    // Local effect regardless of entry point.
                    if (entryPoint != null)
                    {
                        var limb = data.getLimb(entryPoint);
                        if (limb != null)
                        {
                            float localRelief = therapeuticCurve(concentration, 0.0005f, 0.002f);
                            float current = limb.getSensitivity();
                            limb.setSensitivity(lerp(current, Math.max(0f, current - localRelief), dt * 0.2f));
                        }
                    }

                    // System toxicity.
                    if (isSystemic && concentration > 0.0010f)
                    {
                        // TODO: ARRYTHMIA
                    }
                }
            },

    // === HEMOSTATIC AGENTS ===

    TRANEXAMIC_ACID(1800f)
            {
                // Half-life: 30 minutes
                // Therapeutic: 0.0001 - 0.0005 ml/ml
                // Toxic: Theoretically, any amount is toxic, due to thrombosis risk.
                @Override
                public void applyEffects(float concentration, float dt, PlayerHealthData data, LimbNode entryPoint, boolean isSystemic)
                {
                    if (!isSystemic)
                        return;

                    float benefit = therapeuticCurve(concentration, 0.0001f, 0.0005f);
                    // TODO: clottingBoostModifier in PlayerHealthData.

                    // TODO: Probably cardiac arrest or a heart attack, due to thrombosis?
                }
            },

    // === VASOPRESSORS ===

    EPINEPHRINE(60f)
            {
                // Half-life: 60 seconds
                // Therapeutic: 0.0001 - 0.0003 ml/ml
                // Toxic: Over 0.0008 ml/ml
                @Override
                public void applyEffects(float concentration, float dt, PlayerHealthData data, LimbNode entryPoint, boolean isSystemic)
                {
                    if (!isSystemic) return;

                    float therapeutic = therapeuticCurve(concentration, 0.0001f, 0.0003f);

                    // TODO: Raise BPM, which is gonna be read by the heartrate computation

                    // At toxic concentration, vfib.
                    if (concentration > 0.0008f)
                    {
                        // TODO: ARRYTHMIA
                    }
                }
            },
    NALOXONE(300f)
            {
                // Half-life: 5 minutes
                // No overdose; it reverses overdoses.
                @Override
                public void applyEffects(float concentration, float dt, PlayerHealthData data, LimbNode entryPoint, boolean isSystemic)
                {
                    if (!isSystemic) return;
                    if (concentration < 0.0001f) return;

                    // Reverse sensitivity suppression on all limbs towards baseline.
                    float reversalRate = therapeuticCurve(concentration, 0.0001f, 0.0004f);
                    data.getLimbs().values().forEach(limb ->
                    {
                        float current = limb.getSensitivity();
                        if (current < 1.0f)
                            limb.setSensitivity(lerp(current, 1.0f, reversalRate * dt * 0.3f));
                    });

                    // Also reverses respiratory depression.
                    float spo2Recovery = reversalRate * 0.002f * dt;
                    data.setOxygenSaturation(Math.min(data.getOxygenSaturation() + spo2Recovery, ModConstants.SPO2_NORMAL));
                }
            },

    // === ANTIBIOTICS ===

    AMOXICILLIN_CLAVULANATE(14400f)
            {
                // Half-life: 4 hours
                // I dddddddddon't think there's a directly lethal dose...?
                @Override
                public void applyEffects(float concentration, float dt, PlayerHealthData data, LimbNode entryPoint, boolean isSystemic)
                {
                    if (!isSystemic) return;
                    if (concentration < 0.00005f) return;

                    // TODO: Boost immunity.
                }
            },

    METRONIDAZOLE(7200f)
            {
                // Half-life: 2 hours.
                // No directly lethal dose.
                @Override
                public void applyEffects(float concentration, float dt, PlayerHealthData data, LimbNode entryPoint, boolean isSystemic)
                {
                    if (!isSystemic) return;

                    // TODO: Boost immunity.
                }
            },

    // === NON-MEDICAL FLUIDS ===

    SALINE(Float.MAX_VALUE)
            {
                // Half-life: Does not decay.
                // Toxicity is handled by LimbNode, causing compartment syndrome.
                @Override
                public void applyEffects(float concentration, float dt, PlayerHealthData data, LimbNode entryPoint, boolean isSystemic)
                {
                    // Itself, saline doesn't do anything. However, it dilutes other substances,
                    // and increases the total fluid volume in a LimbNode. Go beyond the maximum volume,
                    // and you get overpressure issues.

                    // TODO: Add volume to LimbNode.
                    //  Or maybe make a separate salineVolume to complement bloodVolume?
                }
            },

    FOREIGN_FLUID(120f)
            {
                // Half-life: 2 minutes
                // Refers to stuff like water, potions, apple juice, whatever. Stuff
                // that shouldn't be inside the body.
                @Override
                public void applyEffects(float concentration, float dt, PlayerHealthData data, LimbNode entryPoint, boolean isSystemic)
                {
                    if (!isSystemic) return;
                    if (concentration < 0.00001f) return;

                    // SpO2 drops proportional to the amount injected.
                    float dropRate = concentration * 5f * dt;
                    data.setOxygenSaturation(Math.max(ModConstants.SPO2_FLOOR, data.getOxygenSaturation()) - dropRate);

                    // TODO: Immune response.
                }
            },

    FOREIGN_GAS(120f)
            {
                // Half-life: 2 minutes.
                // Refers to air, CO2, vapor, and gases that aren't meant to be inside organs.
                @Override
                public void applyEffects(float concentration, float dt, PlayerHealthData data, LimbNode entryPoint, boolean isSystemic)
                {
                    if (!isSystemic) return;
                    if (concentration < 0.00001f) return;

                    // TODO: Embolism?
                }
            };

    // === PHARMACOKINETICS ===

    public final float halfLifeSeconds;

    SubstanceType(float halfLifeSeconds)
    {
        this.halfLifeSeconds = halfLifeSeconds;
    }

    public abstract void applyEffects(float concentration, float dt, PlayerHealthData data, LimbNode entryPoint, boolean isSystemic);


    // === UTILITY METHODS ===

    // Returns a 0.0 - 1.0 value depending on how strong a dose is.
    // 0.0 = No effect, 1.0 = at or above peak effect.
    // NOT to be confused with toxicity, this is just how strong the effect of something is.
    protected static float therapeuticCurve(float concentration, float minTherapeutic, float maxTherapeutic)
    {
        if (concentration <= minTherapeutic)
            return 0f;
        if (concentration >= maxTherapeutic)
            return 1f;
        return (concentration - minTherapeutic) / (maxTherapeutic - minTherapeutic);
    }

    // Linear interpolation.
    protected static float lerp(float a, float b, float t)
    {
        return a + (b-a) * Math.max(0f, Math.min(1f, t));
    }
}
