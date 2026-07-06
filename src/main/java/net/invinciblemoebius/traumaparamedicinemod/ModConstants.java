package net.invinciblemoebius.traumaparamedicinemod;

public final class ModConstants
{
    private ModConstants() {}

    // TIMING
    public static final float TICKS_PER_SECOND = 20f;
    public static final float SECONDS_PER_TICK = 1f / TICKS_PER_SECOND;

    // BRAIN
    public static final float BRAIN_PERFUSION_MAP = 60f; // MAP where cerebral perfusion is full.
    public static final float BRAIN_SUPPLY_FLOOR = 0.70f; // Below this: Brain injury begins.
    public static final float BRAIN_DRAIN_MAX = 0.08f; // Brain lost/sec at absolutely zero supply.
    public static final float BRAIN_RECOVERY = 0.02f; // Brain regained/sec when adequately supplied.
    public static final float BRAIN_GIVE_UP = 0.50f; // Below this: the Give Up button becomes available.

    // BLOOD LOSS
    public static final float BLOOD_NORMAL = 5000f; // IRL this ranges, but 5L seems like a nice universal value.
    public static final float BLOOD_MILD_HYPOVOLEMIA = 0.85f; // 15% Blood loss, Class 1 hemorrhage.
    public static final float BLOOD_MODERATE_HYPOVOLEMIA = 0.60f; // 40% Blood loss, Class 2 hemorrhage.
    public static final float BLOOD_SEVERE_HYPOVOLEMIA = 0.40f; // 60% Blood loss, Class 3 hemorrhage.
    public static final float BLOOD_CRITICAL_HYPOVOLEMIA = 0.30f; // 70% Blood loss, Class 4 hemorrhage.
    public static final float BLOOD_MIN = 0f; // Total exsanguination.
    public static final float NORMAL_MAP = 93f; // 120/80 mean arterial pressure
    public static final float ARTERIAL_CLOT_CAP = 0.15f; // Max clot maturity while an arterial wound still flows
    public static final float ARTERIAL_FLOW_CEILING = 4f; // ml/s. Above this an arterial wound can't seal

    // BLOOD COMPOSITION
    public static final float COMP_RESTING_HEMATOCRIT = 0.45f; // Red-cell fraction of resting whole blood.
    public static final float COMP_HYPERVOLEMIA_CEILING_MULT = 2.0f; // Per-node max.
    public static final float COMP_PERFUSION_RATE_FACTOR = 0.04f; // Arterial resupply per node.
    public static final float COMP_VENOUS_RETURN_FACTOR  = 0.04f; // Venous return per node.
    public static final float COMP_RESTING_REDCELL_MASS = BLOOD_NORMAL * COMP_RESTING_HEMATOCRIT; // 2250ml baseline carrier mass.
    public static final float ANEMIA_MILD = 0.80f;
    public static final float ANEMIA_MODERATE = 0.60f;
    public static final float ANEMIA_SEVERE = 0.40f;
    public static final float HEMATOCRIT_DILUTION = 0.35f; // Below: thin, watery blood
    public static final float HEMATOCRIT_CONCENTRATION = 0.55f; // Above: thick, sticky blood

    // HEART RATE (BPM)
    public static final float BPM_NORMAL_MIN = 60f;
    public static final float BPM_NORMAL_MAX = 100f;
    public static final float BPM_TACHYCARDIA = 100f;
    public static final float BPM_SEVERE_TACHYCARDIA = 150f;
    public static final float BPM_BRADYCARDIA = 60f;
    public static final float BPM_CARDIAC_ARREST = 0f;

    // CARDIAC RHYTHM
    public static final float RHYTHM_INSTABILITY_VT = 0.7f;
    public static final float RHYTHM_INSTABILITY_VF = 0.8f;
    public static final float RHYTHM_OUTPUT_PERFUSION_FLOOR = 0.25f;
    public static final float RHYTHM_BPM_RESTING = 72f;
    public static final float RHYTHM_CORONARY_ISCHEMIA_MAP = 50f; // MAP below which the myocardium starves
    public static final float RHYTHM_CORONARY_PERFUSION_DIASTOLIC = 60f; // Diastolic for full coronary fill

    // HEART RESERVE
    public static final float RESERVE_ASYSTOLE = 0.02f; // Below this: Flatline.
    public static final float RESERVE_WEAK = 0.40f; // Below this: contractility starts failing
    public static final float RESERVE_PERFUSION_NEED = 0.50f; // Below this drains reserve
    public static final float RESERVE_DRAIN_RATE = 0.02f; // Per sec, scales with under-perfusion
    public static final float RESERVE_ARREST_DRAIN = 0.004f; // Per sec in a non-perfusing rhythm (4min to flat)
    public static final float RESERVE_RECOVERY = 0.01f; // Per sec when perfused and pumping

    // CIRCULATION
    public static final float NORMAL_MAP_MMHG = 93f; // Mean arterial pressure of a healthy adult. Just for reference.
    public static final float MAX_PERFUSION_FACTOR = 1.75f; // Cap on hyperdynamic transport, so a high MAP can't speed flow without limit.
    public static final float BASE_ADVECTION_RATE = 0.35f; // Fraction of a substance moving one node proximal per second at normal perfusion.
    public static final float INFUSION_RATE_ML_PER_SEC = 25f; // Transfusion speed. Bolus volume converted to blood per second at normal perfusion.

    // RESPIRATION
    public static final float RESPIRATORY_NORMAL = 16f; // Up to here, typical breaths per minute.
    public static final float RESPIRATORY_FLOOR = 2.5f; // Below this, total respiratory collapse. Agonal breaths.
    public static final float RESPIRATORY_HIGH = 25f; // Up to here, high but normal respiratory rate.
    public static final float RESPIRATORY_HYPERVENTILATION = 35f; // Above this, respiratory crisis.

    // GASTROINTESTINAL
    public static final float GASTRIC_CAPACITY_ML = 2000f; // A full stomach. Caps how much can be swallowed before the rest is rejected.
    public static final float GASTRIC_EMPTYING_PER_SECOND = 0.015f; // Fraction of stomach volume that leaves per second. The gradual drain IS the oral absorption curve.
    public static final float NAUSEA_FULLNESS_FRACTION = 0.75f; // Stomach fill fraction above which overfilling starts to nauseate.
    public static final float NAUSEA_OVERFILL_RATE = 0.20f; // Nausea/sec per unit of over-fullness.
    public static final float NAUSEA_DECAY_PER_SECOND = 0.02f; // Nausea relief per second when no source is present.
    public static final float NAUSEA_VOMIT_THRESHOLD = 0.60f; // Below this nausea, no vomiting rolls.
    public static final float NAUSEA_VOMIT_CHANCE_PER_SECOND = 0.25f; // Peak per-second vomit probability at full nausea.
    public static final float NAUSEA_VOMIT_RELIEF = 0.55f; // Nausea removed by one vomiting episode.
    public static final float VOMIT_DRAIN_FRACTION = 0.80f; // Fraction of stomach contents expelled per episode.
    public static final float VOMIT_ASPIRATION_FRACTION = 0.25f; // Fraction of vomit aspirated into the lungs when the airway is unprotected.

    // STAMINA
    public static final float STAMINA_SPRINT_DRAIN = 0.001f; // Per second while sprinting.
    public static final float STAMINA_JUMP_DRAIN = 0.015f; // Per second per jump.
    public static final float STAMINA_BASE_RECOVERY = 0.15f; // Per second while NOT sprinting.
    public static final float STAMINA_LUNG_DRAIN_MAX = 0.50f;

    // CORE TEMPERATURE
    public static final float TEMP_NORMAL_MIN = 36.1f; // In Centigrade
    public static final float TEMP_NORMAL_MAX = 37.2f; // In Centigrade
    public static final float TEMP_HYPOTHERMIA = 35.0f; // Below this: Mild hypothermia
    public static final float TEMP_SEVERE_HYPOTHERMIA = 28.0f; // Below this: Clotting failure
    public static final float TEMP_FEVER = 38.0f; // Above this: Fever
    public static final float TEMP_HEAT_STROKE = 40.0f; // Above this: Heat stroke

    // OXYGEN SATURATION (SpO2), stored 0.0 - 1.0.
    public static final float SPO2_NORMAL = 0.97f; // Resting healthy adult
    public static final float SPO2_HYPOXIA = 0.94f; // Below this: Clinical hypoxia
    public static final float SPO2_SERIOUS = 0.90f; // Below this: Serious impairment
    public static final float SPO2_CRITICAL = 0.85f; // Below this: Cyanosis, consciousness drops
    public static final float SPO2_FLOOR = 0.60f; // Below this: Non-survable without intervention
    public static final float SPO2_HYPEROXIA = 1.0f; // Above this: Excessive oxygen saturation
    public static final float SPO2_OXYGEN_POISONING = 1.2f; // Above this: Clinical hypoxia
    public static final float SPO2_MAX = 1.3f; // Theoretical max.

    // CONSCIOUSNESS, in AVPU thresholds
    public static final float CONSCIOUSNESS_ALERT = 0.80f; // Above this: Fully consciouss
    public static final float CONSCIOUSNESS_VOICE = 0.50f; // Above this: Responds to voice
    public static final float CONSCIOUSNESS_PAIN = 0.20f; // Above this: Responds to pain
    public static final float CONSCIOUSNESS_UNRESPONSIVE = 0.00f; // Above this: No response

    // IMMUNE SYSTEM
    public static final float IMMUNE_RESERVE_MAX = 2.0f; // Ceiling of the reserve pool.
    public static final float IMMUNE_REGEN_PER_SECOND = 0.03f; // Reserve regained/sec at full immunity strength.
    public static final float IMMUNE_CONSUMPTION = 0.5f; // Fraction of deployed reserve spent/sec.
    public static final float IMMUNE_SUPPRESS_EFF = 0.04f; // How hard deployed reserve fights wound infection.
    public static final float IMMUNE_EXHAUSTION_MIN = 0.1f; // Reserve at/below this counts as exhausted.
    public static final float INFECTION_GROWTH_RATE = 0.02f; // How fast contamination feeds local infection.
    public static final float BACTEREMIA_SPILL = 0.10f; // Fraction of unfought growth that seeds the blood.
    public static final float BACTEREMIA_SYSTEMIC_WEIGHT = 1.5f; // How aggressively bacteremia competes for reserve.
    public static final float BACTEREMIA_SUPPRESS_EFF = 0.05f; // How hard deployed reserve clears systemic load.
    public static final float SEPSIS_ENTER_LOAD = 0.3f; // Bacteremia to begin septic shock (with reserve exhausted).
    public static final float SEPSIS_EXIT_LOAD = 0.1f; // Bacteremia below which septic shock recedes.
    public static final float SEPSIS_EMERGENCY_REGEN_MULT = 3.0f; // Regen boost while septic.

    // NUTRITION
    public static final float NUTRITION_HEALTHY = 1.0f;
    public static final float NUTRITION_FLOOR = 0.0f;
    public static final float NUTRITION_DECAY_PER_SECOND = 1f / (7f * 24000 / TICKS_PER_SECOND);

    // WOUND POSITION
    public static final float WOUND_U_ANTERIOR = 0.0f;
    public static final float WOUND_U_RIGHT_LATERAL = 0.5f;
    public static final float WOUND_U_POSTERIOR = 1.0f;
    public static final float WOUND_U_LEFT_LATERAL = 1.5f;
    public static final float WOUND_U_MAX = 2.0f;

    // GAMEPLAY
    public static final float DROWN_SINK_ACCELERATION_MAX = 0.040f; // Downward acceleration when breath runs out.
    public static final float DROWN_RISE_CEILEING_ALERT = 0.060f; // Max upward Y/tick a fully-conscious swimmer can still reach.
    public static final float DROWN_RISE_CEILEING_OUT = -0.020f; // Upward Y/tick ceiling when fully unconscious. Negative = can't rise, sinks.
    public static final float DROWN_PUSHOFF_IMPULSE = 0.25f; // Upward lunge off the seabed (pre-drag).
    public static final float DROWN_MAX_SINK_SPEED  = 0.25f; // Cap on sink speed.

    // ROUTES / TREATMENT
    public static final float IV_ONSET_SECONDS = 1f;
    public static final float IM_ONSET_SECONDS = 25f; // Local limb fluid onset. Perfusion does the absorption.
    public static final float TOPICAL_ONSET_SECONDS = 5f; // TRANSdermal onset, not surface onset in and of itself.
    public static final float TOPICAL_FLUSH_PER_ML = 0.003f; // Contamination flushed per ml of irrigant.
    public static final float TOPICAL_MAX_FLUSH = 0.90f; // Cap on a single irrigation's flush.
    public static final float TOPICAL_REF_ML = 100f; // Volume at which a fluid's own dirtiness fully expresses.
    public static final float TOPICAL_DIRTY_STRENGTH = 0.6f; // How hard a dirty irrigant re-contaminates.

    // CONTAINERS
    public static final float SYRINGE_CAPACITY_ML = 5f; // 5ml.
    public static final float GLASS_CAPACITY_ML = 250f; // Oral bulk: stews, teas, water.
    public static final float JAR_CAPACITY_MG = 250f; // 250mg of powder.
    public static final float STEWPOT_CAPACITY_ML = 10000f; // 10L bulk stock.
    public static final float WATER_BUCKET_ML = 10000f; // One bucket = a full pot of water.
    public static final float MOLCAJETE_CAPACITY_MG = 50f; // Internal grind buffer cap.

    // DRESSING
    public static final float DRESSING_PRESSURE_STOP_VENOUS = 0.90f; // Max venous bleed a perfect pressure dressing removes.
    public static final float DRESSING_PRESSURE_STOP_ARTERIAL = 0.50f; // Arterial resists pressure so it still needs packing/tourniquet.
    public static final float DRESSING_HEMOSTATIC_CLOT_MULT = 3.5f; // Clotting multiplier at perfect 1.0f hemostatic.
    public static final float DRESSING_ANTISEPTIC_DECONTAM_PER_SECOND = 0.01f; // Contamination cleared per sec at antiseptic = 1f.
    public static final float DRESSING_FOUL_BASE_TICKS = 24000f; // Change interval at cleanliness = 1f (scales with cleanliness).
    public static final float DRESSING_FOUL_CONTAM_RISE = 0.0001f; // Contamination per tick once overdue.
    public static final float DRESSING_OCCLUSION_ANAEROBIC_RISE = 0.0008f; // Contamination per tick when a sealed dressing traps a dirty wound.
    public static final float DRESSING_DIRTY_APPLY_FRACTION = 0.5f; // How far a dirty dressing drags contamination toward its floor on application.
    public static final float DRESSING_ADHERENCE_REOPEN_CHANCE = 0.8f; // Reopen probability at adherence = 1f when pulled off a healing wound.
    public static final float DRESSING_ABSORB_CAPACITY_ML = 60f; // Blood an absorption = 1f dressing holds before it's soaked.

    // STEWPOT
    public static final float STEWPOT_BOIL_CONVERT_PER_SECOND = 100f; // Rate of conversion from one substance to another substance.
    public static final float STEWPOT_EVAPORATION_PER_SECOND = 50f; // Boiled water lost to steam.
    public static final float STEWPOT_TEMP_RISE_PER_SECOND = 0.10f; // 10 secs from cold to boiling.
    public static final float STEWPOT_TEMP_FALL_PER_SECOND = 0.05f; // 20 secs to cool once the heat is gone.
    public static final int STEWPOT_SYNC_INTERVAL_TICKS = 5; // Quarter-second UI sync while changing.
    public static final float STEWPOT_BOIL_THRESHOLD = 0.9f; // Temperature needed to boil. Below this, boiling pauses.
    public static final float STEWPOT_INFUSE_PER_SECOND = 0.5f;

    // DRYING RACK
    public static final float DRYER_DRYING_SECONDS = 7f; // Time for a fresh plant to turn into its dried variant.
    public static final float DRYER_WATER_REMOVAL_FRACTION = 0.90f; // Fraction of the WATER component dried off.
    public static final float DRYER_MIN_WATER_TO_DRY = 0.5f; // Below this WATER, a stack isn't dryable (stops re-drying).
    public static final int DRYER_SYNC_INTERVAL_TICKS = 5; // Throttled client sync.

    // DRESSING STATION
    public static final int DRESSING_MAX_COATS = 2;
    public static final float DRESSING_POWDER_CAPACITY_MG = 40f; // Powder a dressing holds per coat.
    public static final float DRESSING_AGENT_REF_FLUID_ML = 30f; // Dose that gives full agent effect (fluid).
    public static final float DRESSING_AGENT_REF_POWDER_MG = 30f; // Dose that gives full agent effect (powder).
    public static final float DRESSING_SATURATION_OCCLUSION = 0.25f; // Occlusion gained from a full soak (less porous).
}
