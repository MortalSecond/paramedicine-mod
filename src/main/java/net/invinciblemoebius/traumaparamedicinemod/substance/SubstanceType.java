package net.invinciblemoebius.traumaparamedicinemod.substance;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;

// Concentrations are expressed in mililiter of substance per mililiter of blood (ml/ml).
// They are NOT designed to be realistic, they're mostly for gameplay feel.
// Don't try these at home, kids!
public enum SubstanceType
{
    // === ANALGESICS ===

    MORPHINE(600f, new SubstanceEffects()
            .reducesPain(0.0002f, 0.0006f, 0.80f, 0.1f)
            .respiratoryDepression(0.0006f, 0.003f)),

    KETAMINE(900f, new SubstanceEffects()
            .reducesPain(0.0001f, 0.0005f, 0.75f, 0.08f)),

    LIDOCAINE(300f, new SubstanceEffects()
            .reducesPain(0.0005f, 0.0020f, 1.0f, 0.2f)
            .locallyOnly()),

    // === ANTIBIOTICS ===

    AMOXICILLIN_CLAVULANATE(14400f, new SubstanceEffects()
            .reducesWoundInfection(0.04f, false)
            .bacteriostatic(0.5f)),

    METRONIDAZOLE(7200f, new SubstanceEffects()
            .reducesBacteremia(0.08f)
            .reducesWoundInfection(0.10f, true)),

    // === VASOPRESSORS ===

    EPINEPHRINE(60f, new SubstanceEffects()
            .chronotropic(0.0001f, 0.0003f, 80f)
            .vasoactive(1500f)),

    NALOXONE(300f, new SubstanceEffects()
            .reversesOpioidEffects(0.3f, 0.002f)),

    // === BLOOD PRODUCTS ===

    PLASMA(180f, new SubstanceEffects()
            .increasesPlasma(1.0f)),

    RBC_CONCENTRATE(240f, new SubstanceEffects()
            .increasesRedCells(0.90f)),

    WHOLE_BLOOD(210f, new SubstanceEffects()
            .increasesPlasma(0.55f)
            .increasesRedCells(0.45f)),

    // === NON-MEDICAL FLUIDS ===

    SALINE(420, new SubstanceEffects()
            .increasesPlasma(0.30f)), // 3-for-1 rule. Only 30% becomes actual plasma.

    FOREIGN_FLUID(120f, new SubstanceEffects()
            .respiratoryDepression(0.00001f, 5f)),

    // === STUBS ===

    TRANEXAMIC_ACID(1800f, new SubstanceEffects()),

    FOREIGN_GAS(120f, new SubstanceEffects());

    public final float halfLifeSeconds;
    private final SubstanceEffects effects;

    SubstanceType(float halfLifeSeconds, SubstanceEffects effects)
    {
        this.halfLifeSeconds = halfLifeSeconds;
        this.effects = effects;
    }

    public void applyEffects(float concentration, float eliminatedThisTick, float dt, PlayerHealthData data, LimbData locationLimb)
    {
        effects.apply(concentration, eliminatedThisTick, dt, data, locationLimb);
    }
}
