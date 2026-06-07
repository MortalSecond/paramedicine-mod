package net.invinciblemoebius.traumaparamedicinemod;

public final class ModConstants
{
    private ModConstants() {}

    // TIMING
    public static final float TICKS_PER_SECOND = 20f;
    public static final float SECONDS_PER_TICK = 1f / TICKS_PER_SECOND;

    // BLOOD LOSS
    public static final float BLOOD_NORMAL = 5000f; // IRL this ranges, but 5L seems like a nice universal value.
    public static final float BLOOD_MILD_HYPOVOLEMIA = 0.85f; // 15% Blood loss, Class 1 hemorrhage.
    public static final float BLOOD_MODERATE_HYPOVOLEMIA = 0.60f; // 40% Blood loss, Class 2 hemorrhage.
    public static final float BLOOD_SEVERE_HYPOVOLEMIA = 0.40f; // 60% Blood loss, Class 3 hemorrhage.
    public static final float BLOOD_CRITICAL_HYPOVOLEMIA = 0.30f; // 70% Blood loss, Class 4 hemorrhage.
    public static final float BLOOD_MIN = 0f; // Total exsanguination.

    // HEART RATE (BPM)
    public static final float BPM_NORMAL_MIN = 60f;
    public static final float BPM_NORMAL_MAX = 100f;
    public static final float BPM_TACHYCARDIA = 100f;
    public static final float BPM_SEVERE_TACHYCARDIA = 150f;
    public static final float BPM_BRADYCARDIA = 60f;
    public static final float BPM_CARDIAC_ARREST = 0f;

    // FIBRILLATIONS
    public static final float FIBS_NORMAL = 0.2f; // Up to here, normal variation.
    public static final float FIBS_AFIB = 0.5f; // Up to here, atrial fibrillations.
    public static final float FIBS_VTACHY = 0.7f; // Up to here, ventricular tachychardia.
    public static final float FIBS_VFIB = 0.8f; // Above this, ventricular fibrillations.

    // RESPIRATION
    public static final float RESPIRATORY_NORMAL = 16f; // Up to here, typical breaths per minute.
    public static final float RESPIRATORY_FLOOR = 2.5f; // Below this, total respiratory collapse. Agonal breaths.
    public static final float RESPIRATORY_HIGH = 25f; // Up to here, high but normal respiratory rate.
    public static final float RESPIRATORY_HYPERVENTILATION = 35f; // Above this, respiratory crisis.

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

    // CONSCIOUSNESS, in AVPU thresholds
    public static final float CONSCIOUSNESS_ALERT = 0.80f; // Above this: Fully consciouss
    public static final float CONSCIOUSNESS_VOICE = 0.50f; // Above this: Responds to voice
    public static final float CONSCIOUSNESS_PAIN = 0.20f; // Above this: Responds to pain
    public static final float CONSCIOUSNESS_UNRESPONSIVE = 0.00f; // Above this: No response

    // IMMUNITY
    public static final float IMMUNITY_HEALTHY = 1.0f;
    public static final float IMMUNITY_FLOOR = 0.0f;

    // NUTRITION
    public static final float NUTRITION_HEALTHY = 1.0f;
    public static final float NUTRITION_FLOOR = 0.0f;
    public static final float NUTRITION_DECAY_PER_SECOND = 1f / (7f * 24000 / TICKS_PER_SECOND);
}
