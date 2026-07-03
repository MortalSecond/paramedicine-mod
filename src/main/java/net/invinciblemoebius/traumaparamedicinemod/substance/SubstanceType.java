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
            .chronotropic(0f, 0.0003f, 80f, 0.0003f, 60000f)
            .vasoactive(1500f)),

    NALOXONE(300f, new SubstanceEffects()
            .reversesOpioidEffects(0.0001f, 0.0004f, 0.3f, 0.002f)),

    // === ANTIFIBRINOLYTICS ===

    TRANEXAMIC_ACID(2000f, new SubstanceEffects()
            .boostsClotting(1.2f, 0.0005f, 0.0015f)),

    // === BLOOD PRODUCTS ===

    PLASMA(SubstanceClass.BLOOD_PRODUCT, 1.0f, 0f),
    RBC_CONCENTRATE(SubstanceClass.BLOOD_PRODUCT, 0f, 0.90f),
    WHOLE_BLOOD(SubstanceClass.BLOOD_PRODUCT, 0.55f, 0.45f),

    // === NON-MEDICAL FLUIDS ===

    // 3-for-1 rule. Only 30% becomes plasma, the rest third-spaces.
    SALINE(SubstanceClass.CRYSTALLOID, 0.30f, 0f),

    // Regular old water. This is the default water, not too dirty, not too clean.
    // I'm thinking they should probably cause a direct pain spike, but also give some amount of hydration.
    // Probably increase bactermia slightly?
    WATER(1000f, new SubstanceEffects()),
    BOILED_WATER(1000f, new SubstanceEffects()),
    // Purified water, BUT MIND: Purified to drink does NOT mean sterile for IVs.
    PURIFIED_WATER(1000f, new SubstanceEffects()),
    // Ringer's solution. Actual proper sterile IV water.
    REHYDRATION_FLUID(1000f, new SubstanceEffects()),

    // Mud, slime, and any solid thing that's definitely not meant to be inside the body.
    // Default "nonsense" fluid. Should probably cause direct bactermia and intense pain.
    JUNK(200f, new SubstanceEffects()),

    // Generic "plant stuff" from flowers and bark and whatnot.
    PLANT_MATTER(10000f, new SubstanceEffects()
            .causesNausea(1f)),

    // Saliva, apple juice, empty potions, and just about any fluid that isn't meant to be in the body.
    FOREIGN_FLUID(120f, new SubstanceEffects()
            .suppressesRespiration(0.00001f, 0.0008f,
                    0.35f, 0.0008f, 290f)),

    // Air for embolism, or water vapor, or smoke, or really anything that isn't clean air or oxygen.
    // Should probably cause a very direct pain spike, or whichever problem embolism causes when injected directly.
    FOREIGN_GAS(SubstanceClass.DRUG, 120f, 0f,
            0f, 0f, 0f, new SubstanceEffects()),

    // === FOODSTUFFS ===

    // Generic food fluid that gets added whenever a food item is added into a mixture.
    FOOD(1000f, new SubstanceEffects()),
    MILK(1000f, new SubstanceEffects()),
    CARAMEL(1000f, new SubstanceEffects()),

    // === INERT / BINDERS ===

    CLAY(120f, 0f, 0.004f, new SubstanceEffects());

    // === CONSTRUCTORS ===

    public final SubstanceClass substanceClass;
    public final float halfLifeSeconds;
    public final float oralBioavailability;
    public final float gutIrritation;
    // Fraction of each consumed ml that becomes plasma or red cells. Drugs are aqueous (1.0 plasma).
    // Expanders carry the product's ratio, gas carries none. The rest is excreted/third-spaced.
    public final float plasmaYield;
    public final float redCellYield;
    private final SubstanceEffects effects;

    // Drug. Aqueous (1.0 plasma yield).
    SubstanceType(float halfLifeSeconds, SubstanceEffects effects)
    {
        this(SubstanceClass.DRUG, halfLifeSeconds, 0f, 0f, 1f, 0f, effects);
    }

    SubstanceType(float halfLifeSeconds, float oralBioavailability, SubstanceEffects effects)
    {
        this(SubstanceClass.DRUG, halfLifeSeconds, oralBioavailability, 0f, 1f, 0f, effects);
    }

    SubstanceType(float halfLifeSeconds, float oralBioavailability, float gutIrritation, SubstanceEffects effects)
    {
        this(SubstanceClass.DRUG, halfLifeSeconds, oralBioavailability, gutIrritation, 1f, 0f, effects);
    }

    // Volume expander. No half-life, no effect, explicit yields.
    SubstanceType(SubstanceClass substanceClass, float plasmaYield, float redCellYield)
    {
        this(substanceClass, 0f, 0f, 0f, plasmaYield, redCellYield, new SubstanceEffects());
    }

    SubstanceType(SubstanceClass substanceClass, float halfLifeSeconds, float oralBioavailability,
            float gutIrritation, float plasmaYield, float redCellYield, SubstanceEffects effects)
    {
        this.substanceClass = substanceClass;
        this.halfLifeSeconds = halfLifeSeconds;
        this.oralBioavailability = oralBioavailability;
        this.gutIrritation = gutIrritation;
        this.plasmaYield = plasmaYield;
        this.redCellYield = redCellYield;
        this.effects = effects;
    }

    public void applyEffects(float concentration, float eliminatedThisTick, float dt, PlayerHealthData data, LimbData locationLimb)
    {
        effects.apply(concentration, eliminatedThisTick, dt, data, locationLimb);
    }
}
