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
            .reducesPain(0.0005f, 0.0020f, 1.0f, 0.2f)),

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

    PLASMA(1.0f, 0f),
    RBC_CONCENTRATE(0f, 0.90f),
    WHOLE_BLOOD(0.55f, 0.45f),

    // === NON-MEDICAL FLUIDS ===

    // 3-for-1 rule. Only 30% becomes plasma, the rest third-spaces.
    SALINE(0.30f, 0f),

    // Regular old water. This is the default water, not too dirty, not too clean.
    WATER(1000f, 0.9f, 0f, 0f, 0f, new SubstanceEffects()
            .givesHydration(1.0f)
            .tonicity(-1.0f)
            .addsBacteremia(0.4f)),
    // Boiling kills the bacteria, but doesn't fix the tonicity. Safe to drink but still lethal in IVs.
    BOILED_WATER(1000f, 0.9f, 0f, 0f, 0f, new SubstanceEffects()
            .givesHydration(1.0f)
            .tonicity(-1.0f)),
    // Purified water, BUT MIND: Purified to drink does NOT mean isotonic for IVs.
    PURIFIED_WATER(1000f, 0.95f, 0f, 0f, 0f, new SubstanceEffects()
            .givesHydration(1.0f)
            .tonicity(-1.0f)),
    // Ringer's solution. Actual proper sterile and isotonic IV water.
    REHYDRATION_FLUID(1000f, 0.95f, 0f, 0f, 0f, new SubstanceEffects()
            .givesHydration(2.0f)),

    // Mud, slime, and any solid thing that's definitely not meant to be inside the body.
    // Default "nonsense" fluid. Painful and septic.
    JUNK(200f, 0.2f, 0.02f, 0f, 0f, new SubstanceEffects()
            .addsBacteremia(1.2f)
            .causesPain(6f)
            .causesNausea(1.5f)),

    // Generic "plant stuff" from flowers and bark and whatnot.
    PLANT_MATTER(10000f, 0.3f, 0.01f, 0f, 0f, new SubstanceEffects()
            .causesNausea(1f)),

    // Saliva, apple juice, empty potions, and just about any fluid that isn't meant to be in the body.
    FOREIGN_FLUID(120f, 0.3f, 0f, 0f, 0f, new SubstanceEffects()
            .causesPain(5f)
            .addsBacteremia(0.6f)),

    // Air for embolism, or water vapor, or smoke, or really anything that isn't clean air or oxygen.
    FOREIGN_GAS(120f, 0f, 0f, 0f, 0f, new SubstanceEffects()
            .causesPain(10f)),

    // === FOODSTUFFS ===

    // Generic food fluid that gets added whenever a food item is added into a mixture.
    // Vanilla food value maps here.
    FOOD(400f, 0.9f, 0f, 0f, 0f, new SubstanceEffects()
            .addsNutrition(0.0015f)),
    // Long half-life nutrition. Releases slowly, so a fatty meal sustains
    // where a FOOD-heavy one spikes and crashes. Vanilla saturation maps here.
    FAT(1800f, 0.85f, 0f, 0f, 0f, new SubstanceEffects()
            .addsNutrition(0.0012f)),
    MILK(600f, 0.9f, 0f, 0f, 0f, new SubstanceEffects()
            .addsNutrition(0.0018f).givesHydration(0.5f)),
    CARAMEL(200f, 0.95f, 0f, 0f, 0f, new SubstanceEffects()
            .addsNutrition(0.0025f)),

    // === INERT / BINDERS ===

    CLAY(120f, 0f, 0.004f, new SubstanceEffects());

    // === FIELDS ===

    public final float halfLifeSeconds;
    public final float oralBioavailability;
    public final float gutIrritation;
    // Fraction of each consumed ml that becomes plasma or red cells. Aqueous drugs are 1.0 plasma.
    // Expanders carry the product's ratio; gas, water, food and binders carry none (0 = no volume added).
    public final float plasmaYield;
    public final float redCellYield;
    private final SubstanceEffects effects;

    // === CONSTRUCTORS ===

    // Drug. Aqueous (1.0 plasma yield).
    SubstanceType(float halfLifeSeconds, SubstanceEffects effects)
    {
        this(halfLifeSeconds, 0f, 0f, 1f, 0f, effects);
    }

    SubstanceType(float halfLifeSeconds, float oralBioavailability, SubstanceEffects effects)
    {
        this(halfLifeSeconds, oralBioavailability, 0f, 1f, 0f, effects);
    }

    SubstanceType(float halfLifeSeconds, float oralBioavailability, float gutIrritation, SubstanceEffects effects)
    {
        this(halfLifeSeconds, oralBioavailability, gutIrritation, 1f, 0f, effects);
    }

    // Volume expander. No half-life (clears by infusion, not decay), no effect, explicit yields.
    SubstanceType(float plasmaYield, float redCellYield)
    {
        this(0f, 0f, 0f, plasmaYield, redCellYield, new SubstanceEffects());
    }

    SubstanceType(float halfLifeSeconds, float oralBioavailability, float gutIrritation, float plasmaYield, float redCellYield, SubstanceEffects effects)
    {
        this.halfLifeSeconds = halfLifeSeconds;
        this.oralBioavailability = oralBioavailability;
        this.gutIrritation = gutIrritation;
        this.plasmaYield = plasmaYield;
        this.redCellYield = redCellYield;
        this.effects = effects;
    }

    public boolean isVolumeInfusion()
    {
        return halfLifeSeconds <= 0f;
    }

    public void applyEffects(float concentration, float eliminatedThisTick, float dt, PlayerHealthData data, LimbData locationLimb)
    {
        effects.apply(concentration, eliminatedThisTick, dt, data, locationLimb);
    }
}
