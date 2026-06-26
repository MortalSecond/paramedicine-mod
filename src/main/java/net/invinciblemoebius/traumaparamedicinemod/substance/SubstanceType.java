package net.invinciblemoebius.traumaparamedicinemod.substance;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;

// Concentrations are expressed in mililiter of substance per mililiter of blood (ml/ml).
// They are NOT designed to be realistic, they're mostly for gameplay feel.
// Don't try these at home, kids!
public enum SubstanceType
{
    // === ANALGESICS ===

    MORPHINE(600f, 0.25f, new SubstanceEffects()
            .reducesPain(0.0002f, 0.0006f, 0.80f, 0.1f)
            .vasoactive(-500f)
            .suppressesRespiration(0.0004f, 0.0008f, 0.35f, 0.0008f, 290f)),

    KETAMINE(900f, 0.20f, new SubstanceEffects()
            .reducesPain(0.0001f, 0.0005f, 0.75f, 0.08f)),

    LIDOCAINE(300f, new SubstanceEffects()
            .reducesPain(0.0005f, 0.0020f, 1.0f, 0.2f)
            .locallyOnly()),

    // === ANTIBIOTICS ===

    AMOXICILLIN_CLAVULANATE(14400f, 0.60f, new SubstanceEffects()
            .reducesWoundInfection(0.04f, false)
            .bacteriostatic(0.5f)),

    METRONIDAZOLE(7200f, 0.90f, new SubstanceEffects()
            .reducesBacteremia(0.08f)
            .reducesWoundInfection(0.10f, true)),

    // === VASOPRESSORS ===

    EPINEPHRINE(60f, new SubstanceEffects()
            .chronotropic(0.0001f, 0.0003f, 80f, 0.0003f, 60000f)
            .vasoactive(1500f)),

    NALOXONE(300f, new SubstanceEffects()
            .reversesOpioidEffects(0.0001f, 0.0004f, 0.3f, 0.002f)),

    // === ANTIFIBRINOLYTICS ===

    TRANEXAMIC_ACID(2000f, new SubstanceEffects()
            .boostsClotting(1.2f, 0.0005f, 0.0015f)),

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

    // Saliva, apple juice, empty potions, and just about any fluid that isn't meant to be in the body.
    FOREIGN_FLUID(120f, new SubstanceEffects()
            .suppressesRespiration(0.00001f, 0.0008f, 0.35f, 0.0008f, 290f)),

    // === INERT / BINDERS ===

    CLAY(120f, 0f, 0.004f, new SubstanceEffects()),

    // === STUBS ===

    // Air for embolism, or water vapor, or smoke, or really anything that isn't clean air or oxygen.
    FOREIGN_GAS(120f, new SubstanceEffects());

    // === CONSTRUCTORS ===

    public final float halfLifeSeconds;
    // Fraction of a dose that survives the gut and first pass and enters the blood. 0 = parenteral only.
    public final float oralBioavailability;
    // Nausea per ml/s while it's in the stomach. 0 = not an irritant.
    public final float gutIrritation;
    private final SubstanceEffects effects;

    // By default, nothing survives swallowing and gives no nausea. Only if the substance specifies
    // a fraction will it change from zero fraction survival or zero irritation.
    SubstanceType(float halfLifeSeconds, SubstanceEffects effects)
    {
        this(halfLifeSeconds, 0f, 0f, effects);
    }

    // Orally absorbed, non-irritant.
    SubstanceType(float halfLifeSeconds, float oralBioavailability, SubstanceEffects effects)
    {
        this(halfLifeSeconds, oralBioavailability, 0f, effects);
    }

    SubstanceType(float halfLifeSeconds, float oralBioavailability, float gutIrritation, SubstanceEffects effects)
    {
        this.halfLifeSeconds = halfLifeSeconds;
        this.oralBioavailability = oralBioavailability;
        this.gutIrritation = gutIrritation;
        this.effects = effects;
    }

    public void applyEffects(float concentration, float eliminatedThisTick, float dt, PlayerHealthData data, LimbData locationLimb)
    {
        effects.apply(concentration, eliminatedThisTick, dt, data, locationLimb);
    }
}
