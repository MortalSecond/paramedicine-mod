package net.invinciblemoebius.traumaparamedicinemod.health;
import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.limbs.AirwayState;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LungData;
import net.invinciblemoebius.traumaparamedicinemod.substance.CirculatingSubstance;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundDepth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

// Holds all the physiological data of a player.
// One instance per player entity.
public class PlayerHealthData
{
    // PLACEHOLDERS
    // How well-supplied with nutrients the body is, NOT how hungry a player feels.
    // 0.8 is the normal amount, below 0.8 means lacking nutrients (penalty),
    // while above 0.8 means having a healthy nutrient reserve (boost).
    // It's set up like that because nutrition is a clotting multiplier.
    private static final float NUTRITION_PLACEHOLDER = 0.8f;
    // This is the long-term fatigue level. Basically sleep debt. It's a placeholder
    // because i don't know how to implement this to Minecraft without forcing the player
    // to go to bed and interrupt their regular gameplay annoyingly. Full sleep system TBD.
    private float energy = 1.0f;

    // SPECIAL VARIABLES
    private boolean justJumped = false;
    public List<SubstanceType> clientActiveSubstances = new ArrayList<>();

    // LIMB NODES
    private final Map<LimbNode, LimbData> limbData = new EnumMap<>(LimbNode.class);
    private final LungData leftLung = new LungData();
    private final LungData rightLung = new LungData();
    private final List<CirculatingSubstance> activeSubstances = new ArrayList<>();

    // CARDIOVASCULAR VALUES
    // The blood volume value is DERIVED, this is just cache.
    private float bloodVolume = 5000f;
    private float systemicHematocrit = ModConstants.COMP_RESTING_HEMATOCRIT;
    private float redCellFraction = 1.0f;
    // How thick the blood is. Over 1.4 value means there's an
    // increased risk of thrombosis and stroke.
    private float bloodViscosity = 1.0f;
    // Blood pressure. Both values are DERIVED, don't set directly.
    private float systolicBP = 120f;
    private float diastolicBP = 80f;
    // Vascular tone value, basically how relaxed the blood vessels are.
    // Under 1.0, vasodilation. Sepsis, morphine, anaphylaxis, spinal shock, etc.
    // Over 1.0, vasoconstriction. Blood loss compensation, cold, epinephrine, so and so.
    private float vascularTone = 1.0f;
    // Rhythm quality. 0.0 is perfect sinus, while 1.0 is vfib.
    private float fibrillations = 0.0f;
    // Whether an underlying cause is maintaining fibs
    private boolean fibrillationsForced = false;
    private float fibrillationsForcedTarget = 0.0f;

    // TRANSIENT MODIFIERS
    // These reset every tick before substances run.
    private float chronotropicModifier = 0f;
    private float vascularToneModifier = 0f;
    private float infectionGrowthModifier = 1f;
    private float clottingBoostModifier = 1.0f;
    private float respiratorySuppressionCeiling = 1.0f;

    // RESPIRATORY VALUES
    // Represents the "urge" to breathe, or how many breaths the body "wants."
    private float respiratoryDrive = 16f;
    // Breaths per minute actually occurring.
    private float actualRespiratoryRate = 16f;
    // Seconds of breath available after actualRespiratoryRate is forced to zero.
    private float breathReserveSeconds = 50f;
    public static final float BREATH_RESERVE_MAX = 50f;
    private AirwayState airwayState = AirwayState.CLEAR;

    // SYSTEMIC VALUES.
    private float coreTemperature = 36.8f;
    private float oxygenSaturation = 0.97f;
    private float heartRateBPM = 72f;
    private float consciousness = 1.0f;
    private float consciousnessTarget = 1.0f;
    // Immune STRENGTH. This is the regen rate of the immune system.
    private float immunity = 1.0f;
    // The deployable "resources" of the immune system.
    private float immuneReserve = ModConstants.IMMUNE_RESERVE_MAX;
    // Systemic pathogen load.
    private float bacteremia = 0f;
    private float aggregatedPain = 0f;
    private float overexertionPain = 0f;
    private float stamina = 1.0f;
    // NOT to be confused with regular pain, this is neurogenic pain; it only rises after pain crosses a threshold.
    private float painShock = 0f;
    // NOT to be confused with regular infection. This is systemic infection.
    private float septicShock = 0f;

    // When any field changes, this indicates if the packet should be sent.
    // P.S. This idea was shamelessly stolen from Casualties: Cubed.
    private boolean syncNeeded = true;

    // === CONSTRUCTOR ===

    public PlayerHealthData()
    {
        for (LimbNode node : LimbNode.values())
        {
            limbData.put(node, new LimbData(node));
        }
    }

    // === COMPUTATION METHODS ===

    // Sum of all the limb's blood volume values.
    public void recomputeBloodVolume()
    {
        float sum = 0f;

        for (LimbData limb : limbData.values())
        {
            sum += limb.getActualBloodVolume();
        }

        if (this.bloodVolume != sum)
        {
            this.bloodVolume = sum;
            markDirty();
        }
    }

    public void recomputeHematocritAndViscosity()
    {
        float plasma = 0f;
        float cells =  0f;
        for (LimbData limb : limbData.values())
        {
            plasma += limb.getPlasmaVolume();
            cells += limb.getRedCellVolume();
        }
        float total = plasma + cells;

        systemicHematocrit = total > 0f ? cells / total : 0f;
        redCellFraction = cells / ModConstants.COMP_RESTING_REDCELL_MASS;

        // Higher RBC ratio = thicker blood. Higher plasma ratio = thinner blood.
        // Thick, thick, thick, thick blood, yummy-yum.
        float v = 0.5f + (systemicHematocrit / ModConstants.COMP_RESTING_HEMATOCRIT) * 0.5f;
        bloodViscosity = Math.max(0.3f, Math.min(2.0f, v));
    }

    // Really complicated BP math that i REALLY encourage understanding bit-by-bit.
    // I'm not very satisfied with it, but my research didn't turn up any concrete formula.
    // At least, not one that fit Paramedicine's variables. If there's an IRL medic that
    // could help get a simpler calculation for BP, i'd appreciate the help.
    public void recomputeBloodPressure()
    {
        float bloodFraction = bloodVolume / ModConstants.BLOOD_NORMAL;

        // Base dystolic from volume. Roughly linear; 100% volume = 120, while 30% volume = 40.
        float volumeBasedSystolic = 40f + (bloodFraction * 80f);
        // Cardiac efficiency from rhythm quality. Perfect rhythm = 1.0, full V-Fib = 0.05.
        float cardiacEfficiency = 1.0f - (fibrillations * 0.95f);
        // Heart rate contribution to systolic. Tachy raises systolic, but REALLY severe tachy reduces it.
        float rateModifier;
        if (heartRateBPM <= ModConstants.BPM_NORMAL_MAX)
        {
            rateModifier = 1.0f;
        }
        else if (heartRateBPM <= ModConstants.BPM_SEVERE_TACHYCARDIA)
        {
            rateModifier = 1.0f + ((heartRateBPM - ModConstants.BPM_TACHYCARDIA) / 100f) * 0.1f;
        }
        else
        {
            rateModifier = 1.1f - ((heartRateBPM - ModConstants.BPM_SEVERE_TACHYCARDIA) / 100f) * 0.3f;
        }
        rateModifier = Math.max(0.5f, rateModifier);

        // High BPM Response. Diastolic filling collapse.
        // Past 220 BPM, actual filling efficiency fails linearly
        // until only 15% efficiency at 280 BPM.
        float fillingEfficiency = 1.0f;
        if (heartRateBPM > 200f)
            fillingEfficiency = Math.max(0.15f, 1.0f - ((heartRateBPM - 200f) / 80f) * 0.85f);

        // Pain response. Up to 15% systolic at max pain..
        float painPressor = 1.0f + (aggregatedPain * 0.15f);

        // Computation.
        float effectiveTone = Math.max(0.1f, Math.min(3.0f, vascularTone + vascularToneModifier));
        float newSystolicBP = volumeBasedSystolic * effectiveTone * cardiacEfficiency
                * rateModifier * bloodViscosity * painPressor * fillingEfficiency;

        // Diastolic widens in vasodilation but narrows during vasoconstriction.
        float pulsePressureRatio = 0.5f + (0.2f * (1.0f / Math.max(0.1f, effectiveTone)));
        // Diastolic is proportional to systolic at roughly 0.6 ratio.
        float newDiastolicBP = systolicBP * Math.min(0.85f, pulsePressureRatio);

        if (newSystolicBP != systolicBP || newDiastolicBP != diastolicBP)
        {
            systolicBP = newSystolicBP;
            diastolicBP = newDiastolicBP;
            markDirty();
        }
    }

    public void recomputeRespiratoryDrive()
    {
        float base = ModConstants.RESPIRATORY_NORMAL;

        // Hypoxia response. Up to +20 breaths per minute under severe hypoxia.
        float hypoxiaResponse = 0f;
        float delivery = getOxygenDelivery();
        if (delivery < ModConstants.SPO2_HYPOXIA)
        {
            float deficit = (ModConstants.SPO2_HYPOXIA - delivery) / (ModConstants.SPO2_HYPOXIA - ModConstants.SPO2_FLOOR);
            hypoxiaResponse = deficit * 20f;
        }

        // Blood loss response.
        float bloodResponse = 0f;
        float bloodFraction = bloodVolume / ModConstants.BLOOD_NORMAL;
        if (bloodFraction < ModConstants.BLOOD_MODERATE_HYPOVOLEMIA)
            bloodResponse = (ModConstants.BLOOD_MODERATE_HYPOVOLEMIA - bloodFraction) / ModConstants.BLOOD_MODERATE_HYPOVOLEMIA * 10f;

        // Sepsis/fever response. Up to +10 breaths per minute.
        float sepsisResponse = septicShock * 8f;
        boolean hasFever = coreTemperature > ModConstants.TEMP_FEVER;
        float feverResponse = hasFever ? Math.min(6f, (coreTemperature - ModConstants.TEMP_FEVER) * 3f) : 0f;

        // Pain response. Up to +8 breaths per miunte.
        float painResponse = aggregatedPain * 8f;

        // Exertion response. Up to +14 breaths per minute.
        float exertionResponse = 0f;
        if (stamina < 0.70f)
            exertionResponse = ((0.70f - stamina) / 0.70f) * 14f;

        float newDrive = Math.min(40f, base + hypoxiaResponse + bloodResponse + painResponse
                + exertionResponse + sepsisResponse + feverResponse);
        newDrive = Math.max(0f, newDrive);

        if (newDrive != respiratoryDrive)
        {
            respiratoryDrive = newDrive;
            markDirty();
        }
    }

    public void recomputeActualRespiratoryRate(boolean isUnderwater)
    {
        float dt = ModConstants.SECONDS_PER_TICK;
        float beforeRate = actualRespiratoryRate;
        float beforeReserve = breathReserveSeconds;

        // Maximum respiratory rate from airway.
        float airwayCeiling = switch (airwayState)
        {
            case CLEAR -> 1.0f;
            case PARTIALLY_OBSTRUCTED -> 0.50f;
            case FULLY_OBSTRUCTED -> 0.0f;
        };

        // Ceiling from lung damage.
        float lungCeiling = 1.0f - (getTotalLungCompromise() / 2.0f);

        // Ceiling from tension pneumothorax.
        if (leftLung.hasTensionPneumothorax() || rightLung.hasTensionPneumothorax())
        {
            lungCeiling *= 0.6f;
        }

        // Ceiling from consciousness (to simulate agonal breathing).
        float consciousnessCeiling;
        if (consciousness >= ModConstants.CONSCIOUSNESS_VOICE)
        {
            consciousnessCeiling = 1.0f;
        }
        else if (consciousness >= ModConstants.CONSCIOUSNESS_PAIN)
        {
            consciousnessCeiling = 0.6f;
        }
        else
        {
            consciousnessCeiling = 0.2f;
        }

        // Forced zero conditions.
        boolean forcedZero = isUnderwater || airwayState == AirwayState.FULLY_OBSTRUCTED;

        float maxRate = respiratoryDrive * Math.min(
                Math.min(airwayCeiling, lungCeiling),
                Math.min(consciousnessCeiling, respiratorySuppressionCeiling));

        if (forcedZero)
            actualRespiratoryRate = 0f;
        else
            actualRespiratoryRate = maxRate;

        if (actualRespiratoryRate < 4f)
        {
            // Drain breath reserve.
            float drainRate = respiratoryDrive / ModConstants.RESPIRATORY_NORMAL;
            breathReserveSeconds = Math.max(0f, breathReserveSeconds - (drainRate * dt));
        }
        else
        {
            // Replenish breath reserve when breathing normally. Five seconds of recovery at a normal rate.
            float replenishRate = (actualRespiratoryRate / ModConstants.RESPIRATORY_NORMAL) * 5f;
            breathReserveSeconds = Math.min(BREATH_RESERVE_MAX, breathReserveSeconds + (replenishRate * dt));
        }

        if (actualRespiratoryRate != beforeRate || breathReserveSeconds != beforeReserve)
            markDirty();
    }

    // Sum of all the limb's pain values.
    public void recomputeAgreggatedPain()
    {
        float sum = 0f;

        for (LimbData limb : limbData.values())
        {
            sum += limb.getEffectivePain();
        }

        this.aggregatedPain = Math.min(2.0f, sum + overexertionPain);
    }

    // Computes the target consciousness from the current systemic state,
    // then applies inertia to move the actual value towards the target.
    // Blood volume loss, hypoxia, extreme pain, and brain damage reduce the ceiling.
    // It takes roughly 50secs to actually fully faint, so ~0.001 loss/tick.
    public void recomputeConsciousness()
    {
        // Blood volume ceiling
        // Full consciousness up to Class 2 hemorrhage. Below that,
        // linear drop to 0 with a Class 4 hemorrhage.
        float bloodFraction = bloodVolume / ModConstants.BLOOD_NORMAL;
        float bloodCeiling;
        if (bloodFraction >= ModConstants.BLOOD_MODERATE_HYPOVOLEMIA)
            bloodCeiling = 1.0f;
        else if (bloodFraction <= ModConstants.BLOOD_SEVERE_HYPOVOLEMIA)
            bloodCeiling = 0.0f;
        else
            bloodCeiling = (bloodFraction - ModConstants.BLOOD_SEVERE_HYPOVOLEMIA) / (ModConstants.BLOOD_MODERATE_HYPOVOLEMIA - ModConstants.BLOOD_SEVERE_HYPOVOLEMIA);

        // Blood pressure ceiling.
        float map = diastolicBP + (systolicBP - diastolicBP) / 3f;
        float bpCeiling;
        if (map >= 70f)
            bpCeiling = 1.0f;
        else if (map <= 30f)
            bpCeiling = 0.0f;
        else
            bpCeiling = (map - 30f) / 40f;

        // SpO2 ceiling.
        float delivery = getOxygenDelivery();
        float spo2Ceiling;
        if (delivery >= ModConstants.SPO2_HYPOXIA)
            spo2Ceiling = 1.0f;
        else if (delivery <= ModConstants.SPO2_FLOOR)
            spo2Ceiling = 0.0f;
        else
            spo2Ceiling = (delivery - ModConstants.SPO2_FLOOR) / 0.19f;

        // Pain ceiling.
        float painCeiling;
        if (aggregatedPain <= 0.70f)
        {
            painCeiling = 1.0f;
        }
        else
        {
            painCeiling = 1.0f - ((aggregatedPain - 0.70f) / 0.30f) * 0.40f;
        }

        // Temperature ceiling.
        float tempCeiling = 1.0f;
        if (coreTemperature < ModConstants.TEMP_HYPOTHERMIA)
            tempCeiling = Math.max(0.0f, (coreTemperature - 28.0f) / (ModConstants.TEMP_HYPOTHERMIA - 28.0f));
        else if (coreTemperature > ModConstants.TEMP_HEAT_STROKE)
            tempCeiling = Math.max(0.2f, 1.0f - ((coreTemperature - ModConstants.TEMP_HEAT_STROKE) / 3.0f));

        // Sepsis ceiling.
        float sepsisCeiling = 1.0f - (septicShock * 0.5f);

        // Brain damage ceiling.
        LimbData headNode = limbData.get(LimbNode.HEAD);
        float brainCeiling = (headNode != null) ? headNode.getTotalHealth() : 1.0f;

        // Pain shock ceiling.
        float shockCeiling = stamina >= 0f ? 1.0f - (Math.max(0f, painShock - 0.30f) / 0.70f * 0.70f) : 1.0f;

        // Lowest ceiling wins.
        consciousnessTarget = Math.min(
                Math.min(Math.min(bloodCeiling, spo2Ceiling), Math.min(painCeiling, brainCeiling)),
                Math.min(Math.min(shockCeiling, sepsisCeiling), Math.min(tempCeiling, bpCeiling)));

        // Apply inertia.
        float inertiaRate = 0.001f;
        float before = consciousness;
        if (consciousness < consciousnessTarget)
            consciousness = Math.min(consciousnessTarget, consciousness + inertiaRate);
        else if (consciousness > consciousnessTarget)
            consciousness = Math.max(consciousnessTarget, consciousness - inertiaRate);

        if (consciousness != before)
            markDirty();
    }

    // Computes the heartrate from the current systemic state.
    // MIND: In Paramedicine, BPM is mostly diagnostic, not effectual.
    // Blood loss, hypoxia, and makes the heart compensate, while
    // hypothermia makes the heart slow down.
    public void recomputeHeartRate()
    {
        float base = 72f;

        // Bloos loss compensation.
        // Scales from +0 BPM to +80 BPM at 30% of blood volume.
        float bloodFraction = bloodVolume / ModConstants.BLOOD_NORMAL;
        float bloodResponse = 0f;
        if (bloodFraction < 1.0f)
            bloodResponse = (1.0f - Math.max(ModConstants.BLOOD_CRITICAL_HYPOVOLEMIA, bloodFraction)) / 0.70f * 80f;

        // Pain-driven sympathetic response.
        // Scales to +30 BPM at maxiumum normal pain.
        float painResponse = aggregatedPain * 30f;

        // Hypoxia compensation.
        // Scales to +40 BPM when critically hypoxic.
        float spo2Response = 0f;
        float delivery = getOxygenDelivery();
        if (delivery < ModConstants.SPO2_HYPOXIA)
            spo2Response = (ModConstants.SPO2_HYPOXIA - delivery) / 0.19f * 40f;

        // Exertion from stamina depletion.
        // Up to +50 BPM at zero stamina.
        float staminaResponse = 0f;
        if (stamina < 0.80f)
            staminaResponse = ((0.80f - stamina) / 0.80f) * 50f;

        // Sepsis tachycardia.
        // Up to +35 BPM at full septic shock.
        float sepsisResponse = septicShock * 35f;

        // Hypothermia suppression.
        // Scales down to -40 BPM when hypothermic.
        float tempSuppression = 0f;
        if (coreTemperature < ModConstants.TEMP_HYPOTHERMIA)
            tempSuppression = (ModConstants.TEMP_HYPOTHERMIA - coreTemperature) / 7.0f * 40f;

        // Baroreflex, blood pressure response.
        // High BP slows BPM, low BP increases BPM.
        float bloodPressureResponse = 0f;
        float normalSystolic = 120f;
        bloodPressureResponse = -((systolicBP - normalSystolic) / normalSystolic) * 40f;
        bloodPressureResponse = Math.max(-25f, Math.min(25f, bloodPressureResponse));

        // Computation.
        float computed = base + bloodResponse + painResponse + spo2Response + staminaResponse
                + sepsisResponse - tempSuppression + chronotropicModifier;
        // This little edge case exists because baroreflex was making BPM go lower when
        // given low doses of epi. Now, it's not totally wrong since IRL it can happen,
        // but it's very counterintuitive.
        if (chronotropicModifier <= 0f)
            computed += bloodPressureResponse;

        float newBPM = Math.max(0f, Math.min(300f, computed));

        if (newBPM != this.heartRateBPM)
        {
            this.heartRateBPM = newBPM;
            markDirty();
        }
    }

    // Systemic multiplier on every wound's clotting rate. Consolidates coagulopathy
    // that's for the most part system-wide rather than per-wound.
    public float computeSystemicClottingFactor()
    {
        // Hemodilution penalty. Viscosity below 1.0 means thin, watery blood.
        // Thick blood gets, at most, a 20% bonus to clotting.
        float dilutionFactor = Math.min(1.2f, bloodViscosity);

        return clottingBoostModifier * dilutionFactor;
    }

    // === DIRECT SETTERS ===

    // Applies pain DIRECTLY, circumventing the per-wound accumulation of recomputeAggregatedPain().
    public void spikeAggregatedPain(float amount)
    {
        aggregatedPain = Math.min(1.0f, aggregatedPain + amount);
        markDirty();
    }

    // Overwrites consciousness directly, circumventing the inertia system. This one's instantaneous.
    public void setConsciousnessDirectly(float value)
    {
        consciousness = Math.max(0f, Math.min(1f, value));
        consciousnessTarget = consciousness;
        markDirty();
    }

    // === TICK METHODS ===

    // Applies respiratory effect on oxygenation each tick.
    public void tickRespiratorySpO2Effect()
    {
        float dt = ModConstants.SECONDS_PER_TICK;

        // Normal breathing, normal SpO2 recovery.
        if (actualRespiratoryRate > 0f)
        {
            // How much of the body's urge to breathe is actually being met?
            float breathingRatio = actualRespiratoryRate / Math.max(0.1f, respiratoryDrive);
            breathingRatio = Math.max(0f, Math.min(1f, breathingRatio));

            float targetSpO2 = 0.80f + (breathingRatio * (ModConstants.SPO2_NORMAL - 0.80f));

            // Drift toward target. Recovery is faster than drop.
            if (oxygenSaturation < targetSpO2)
            {
                float recovery = 0.008f * dt;
                setOxygenSaturation(Math.min(targetSpO2, oxygenSaturation + recovery));
            }
            else if (oxygenSaturation > targetSpO2)
            {
                float drop = 0.004f * dt;
                setOxygenSaturation(Math.max(targetSpO2, oxygenSaturation - drop));
            }
        }
        // No breathing, but with breath reserves. SpO2 drops but very slowly.
        else if (breathReserveSeconds > 0)
        {
            float slowDrop = 0.001f * dt;
            setOxygenSaturation(Math.max(ModConstants.SPO2_FLOOR, oxygenSaturation - slowDrop));
        }
        // No breathing at all, SpO2 freefalls.
        else
        {
            float fastDrop = 0.003f * dt;
            setOxygenSaturation(Math.max(ModConstants.SPO2_FLOOR, oxygenSaturation - fastDrop));
        }
    }

    public void tickCoreTemperature(float dt)
    {
        float target = 36.8f;

        // Fever from infection/sepsis.
        // Up to +2.5°C.
        float infectionLoad = Math.max(septicShock, bacteremia * 0.3f);
        target += infectionLoad * 2.5f;

        // Shock cools the core. It's that clammy, cold sweat.
        // Up to -2°C.
        float bloodFraction = bloodVolume / ModConstants.BLOOD_NORMAL;
        if (bloodFraction < ModConstants.BLOOD_MODERATE_HYPOVOLEMIA)
            target -= ((ModConstants.BLOOD_MODERATE_HYPOVOLEMIA - bloodFraction) / ModConstants.BLOOD_MODERATE_HYPOVOLEMIA) * 2.0f;

        // Exertion warms the core slightly.
        // Up to +0.5°C.
        if (stamina < 0.5f)
            target += (0.5f - stamina) * 1.0f;

        // Slow drift towards target.
        // Mind that temp changes are much slower compared to the other systemic values.
        // About 0.02°C/sec or 1.2°C/min at full gap.
        float driftRate = 0.02f * dt;
        if (coreTemperature < target)
            setCoreTemperature(Math.min(target, coreTemperature + driftRate));
        else if (coreTemperature > target)
            setCoreTemperature(Math.max(target, coreTemperature - driftRate));
    }

    public void tickFibrillations()
    {
        float dt = ModConstants.SECONDS_PER_TICK;

        // High BPM stress. Extreme tachy degrades rhythm quality.
        // Only above 200 BPM, to kinda emulate SVT becoming VT.
        float stressAccrual = 0f;
        if (heartRateBPM > 200f)
            stressAccrual += ((heartRateBPM - 200f) / 20f) * 0.08f * dt;

        // Severe hypoxia degradation.
        float delivery = getOxygenDelivery();
        if (delivery < ModConstants.SPO2_CRITICAL)
            stressAccrual += ((ModConstants.SPO2_CRITICAL - delivery) / 0.25f) * 0.010f * dt;

        // Hyperalkemia. This is from shock acidosis, bbbbbbbbuuuuut, since i don't directly
        // emulate blood pH, i'm going to tie it to hypervolemia.
        float bloodFraction = bloodVolume / ModConstants.BLOOD_NORMAL;
        if (bloodFraction < ModConstants.BLOOD_CRITICAL_HYPOVOLEMIA)
            stressAccrual += 0.06f * dt;

        // Increase fibs according to stress level.
        if (stressAccrual > 0f)
            setFibrillations(fibrillations + stressAccrual);

        // Returns to forced level after 30secs post-defib.
        if (fibrillationsForced && fibrillations < fibrillationsForcedTarget)
        {
            float driftRate = 0.2f * dt;
            fibrillations = Math.min(fibrillationsForcedTarget, fibrillations + driftRate);
            markDirty();
        }

        // Healthy heart recovers back to normal sinus overtime.
        if (!fibrillationsForced && stressAccrual <= 0f && fibrillations > 0f)
        {
            fibrillations = Math.max(0f, fibrillations - 0.04f * dt);
            markDirty();
        }
    }

    public void defibrillate()
    {
        fibrillations = 0.0f;
        markDirty();
    }

    public void tickPainShock(float dt)
    {
        // If the pain is over the threshold, increase shock.
        if (aggregatedPain > 0.70f)
        {
            float excess = (aggregatedPain - 0.70f) / 0.30f;
            painShock = Math.min(1f, painShock + (excess * 0.0003f * dt));
        }
        // If under the threshold, decrease it.
        else if (painShock > 0f)
        {
            painShock = Math.max(0f, painShock - (0.0001f * dt));
        }

        if (painShock > 0.0f)
            markDirty();
    }

    public void tickImmuneSystem(float dt)
    {
        // Total demand from reachable, infected wounds + bacteremia.
        float totalDemand = 0f;

        for (Map.Entry<LimbNode, LimbData> entry : limbData.entrySet())
        {
            LimbData limb = entry.getValue();
            if (!limb.hasProximalCirculation(entry.getKey(), limbData))
                continue;

            for (Wound wound : limb.getWounds())
            {
                float weight = wound.infectionSuceptibility();
                if (weight <= 0f)
                    continue;
                totalDemand += wound.getInfectionLevel() * weight;
            }
        }

        float bacteremiaDemand = bacteremia * ModConstants.BACTEREMIA_SYSTEMIC_WEIGHT;
        totalDemand += bacteremiaDemand;
        float deployed = Math.min(immuneReserve, totalDemand);

        // Growth vs. Suppression per wound; seed bacteremia on overflow.
        for (Map.Entry<LimbNode, LimbData> entry : limbData.entrySet())
        {
            LimbData limb = entry.getValue();
            boolean reachable = limb.hasProximalCirculation(entry.getKey(), limbData);

            for (Wound wound : limb.getWounds())
            {
                float weight = wound.infectionSuceptibility();
                if (weight <= 0f)
                    continue;

                float growth = wound.getContamination() * ModConstants.INFECTION_GROWTH_RATE * getInfectionGrowthModifier();
                float suppress = 0f;

                if (reachable && totalDemand > 0f)
                {
                    float demandRate = wound.getInfectionLevel() * weight;
                    float share = deployed * (demandRate / totalDemand);
                    suppress = share * ModConstants.IMMUNE_SUPPRESS_EFF;
                }

                float net = (growth - suppress) * dt;
                if (net != 0f)
                    wound.setInfectionLevel(wound.getInfectionLevel() + net);

                // Only a "reachable" wound can spill into the bloodstream.
                if (reachable && growth > suppress)
                    bacteremia += (growth - suppress) * weight * ModConstants.BACTEREMIA_SPILL * dt;
            }
        }

        // Bacteremia suppression from its own share.
        if (bacteremiaDemand > 0f && totalDemand > 0f)
        {
            float bacteremiaShare = deployed * (bacteremiaDemand / totalDemand);
            bacteremia -= bacteremiaShare * ModConstants.BACTEREMIA_SUPPRESS_EFF * dt;
        }
        bacteremia = Math.max(0f, bacteremia);

        // Reserves. Consume on deployment, regen by strength (and multiply by emergency if septic).
        float regenMult = (septicShock > 0f) ? ModConstants.SEPSIS_EMERGENCY_REGEN_MULT : 1.0f;
        float regen = immunity * ModConstants.IMMUNE_REGEN_PER_SECOND * regenMult * dt;
        float consumption = deployed * ModConstants.IMMUNE_CONSUMPTION * dt;

        immuneReserve = Math.max(0f, Math.min(ModConstants.IMMUNE_RESERVE_MAX, immuneReserve + regen - consumption));
    }

    public void tickSepticShock(float dt)
    {
        boolean reserveExhausted = immuneReserve <= ModConstants.IMMUNE_EXHAUSTION_MIN;

        // Enter/sustain. Bacteria in the blood the reserve can't clean.
        if (bacteremia >= ModConstants.SEPSIS_ENTER_LOAD && reserveExhausted)
        {
            float severity = Math.min(1.0f, (bacteremia - ModConstants.SEPSIS_ENTER_LOAD) / (1.0f - ModConstants.SEPSIS_ENTER_LOAD));
            septicShock = Math.min(1.0f, septicShock + (severity * 0.0002f * dt));

            boolean isWarmPhase = septicShock < 0.50f;
            float targetTone = isWarmPhase ? (1.0f - (septicShock * 0.60f)) : (0.70f - ((septicShock - 0.50f) * 1.0f));
            setVascularTone(Math.max(0.15f, targetTone));
        }
        // Recover only once load drops below the lower threshold.
        else if (bacteremia < ModConstants.SEPSIS_EXIT_LOAD)
        {
            septicShock = Math.max(0f, septicShock - (0.0001f * dt));
            if (vascularTone < 1.0f)
                setVascularTone(Math.min(1f, vascularTone + (0.0002f * dt)));
        }

        // Generalist recovery. Fires up when the body is healthy and
        // something other than sepsis moved vascular tone, like liver-shots or epi.
        if (bacteremia <= 0f && vascularTone < 1.0f)
            setVascularTone(Math.min(1f, vascularTone + (0.0005f * dt)));

        if (septicShock > 0f)
            markDirty();
    }

    public void tickStamina(boolean isSprinting, boolean justJumped, float dt)
    {
        float lungEfficiency = Math.max(0f, 1.0f - (getTotalLungCompromise() * 0.80f));
        float energyModifier = 0.60f + (energy * 0.40f);

        if (isSprinting)
        {
            float compressionPenalty = (1.0f - lungEfficiency) * (1.0f - lungEfficiency) * ModConstants.STAMINA_LUNG_DRAIN_MAX;
            float totalDrainPerSecond = ModConstants.STAMINA_SPRINT_DRAIN + compressionPenalty;
            stamina = Math.max(0f, stamina - (totalDrainPerSecond * dt));
        }
        else
        {
            float recoveryPerSecond = ModConstants.STAMINA_BASE_RECOVERY * lungEfficiency * energyModifier;
            stamina = Math.min(1.0f, stamina + (recoveryPerSecond * dt));
        }

        if (justJumped)
        {
            stamina = Math.max(0f, stamina - ModConstants.STAMINA_JUMP_DRAIN);
        }

        // Overexertion pain.
        if (isSprinting && stamina < 0.15f)
        {
            float burnIntensity = ((0.15f - stamina) / 0.15f) * 0.25f;
            overexertionPain = Math.min(0.30f, burnIntensity);
        }
        else
            overexertionPain = Math.max(0f, overexertionPain - (0.05f * dt));

        if (stamina != 1.0f || overexertionPain != 0f)
            markDirty();
    }

    public void tickEnergy(float dt)
    {
        // Decays over roughly 3 real-time hours from full to empty without sleep.
        float decayRate = 1.0f / 10800;
        energy = Math.max(0f, energy - (decayRate * dt));
        if (energy < 1.0f)
        {
            markDirty();
        }
    }

    // === TRANSIENT METHODS ===

    public void addChronotropicModifier(float dt)
    {
        chronotropicModifier += dt;
    }

    public void addVascularToneModifier(float dt)
    {
        vascularToneModifier += dt;
    }

    public void applyInfectionGrowthModifier(float modifier)
    {
        infectionGrowthModifier *= modifier;
    }

    public void applyClottingModifier(float modifier)
    {
        clottingBoostModifier *= modifier;
    }

    public void applyRespiratorySuppression(float ceiling)
    {
        respiratorySuppressionCeiling = Math.min(respiratorySuppressionCeiling, ceiling);
    }

    public void resetTransientModifiers()
    {
        chronotropicModifier = 0f;
        vascularToneModifier = 0f;
        infectionGrowthModifier = 1f;
        clottingBoostModifier = 1.0f;
        respiratorySuppressionCeiling = 1.0f;
    }

    public void applyAntibioticToReachableWounds(float amountPerWound, boolean deepOnly)
    {
        if (amountPerWound <= 0f)
            return;

        for (Map.Entry<LimbNode, LimbData> entry : limbData.entrySet())
        {
            LimbData limb = entry.getValue();
            if (!limb.hasProximalCirculation(entry.getKey(), limbData))
                continue;

            for (Wound wound : limb.getWounds())
            {
                if (wound.getInfectionLevel() <= 0f)
                    continue;
                if (deepOnly && wound.getDepth().ordinal() < WoundDepth.MUSCULAR.ordinal())
                    continue;

                wound.setInfectionLevel(wound.getInfectionLevel() - amountPerWound);
            }
        }
    }

    // === LIMB METHODS ===

    public Map<LimbNode, LimbData> getLimbs()
    {
        return Collections.unmodifiableMap(limbData);
    }

    public LimbData getLimb(LimbNode node)
    {
        return limbData.get(node);
    }

    public float getTotalLungCompromise()
    {
        return leftLung.getCompromise() + rightLung.getCompromise();
    }

    public void addSubstance(CirculatingSubstance substance)
    {
        activeSubstances.add(substance);
        markDirty();
    }

    // === ACCESSORS ===

    public float getBloodVolume() { return bloodVolume; }
    public float getHematocrit() { return systemicHematocrit; }
    public float getRedCellFraction() { return redCellFraction; }
    public float getSystolicBP() { return systolicBP; }
    public float getDiastolicBP() { return diastolicBP; }
    public float getVascularTone() { return vascularTone; }
    public float getFibrillations() { return fibrillations; }
    public boolean isFibrillationsForced() { return fibrillationsForced; }
    public float getRespiratoryDrive() { return respiratoryDrive; }
    public float getActualRespiratoryRate() { return actualRespiratoryRate; }
    public float getBreathReserveSeconds() { return breathReserveSeconds; }
    public AirwayState getAirwayState() { return airwayState; }
    public float getOxygenSaturation() { return oxygenSaturation; }
    public float getOxygenDelivery() { return oxygenSaturation * Math.min(1.0f, redCellFraction); }
    public float getHeartRateBPM() { return heartRateBPM; }
    public LungData getLeftLung() { return leftLung; }
    public LungData getRightLung() { return rightLung; }
    public float getCoreTemperature() { return coreTemperature; }
    public float getConsciousness() { return consciousness; }
    public float getImmunity() { return immunity; }
    public float getImmuneReserve() { return immuneReserve; }
    public float getBacteremia() { return bacteremia; }
    public float getAggregatedPain() { return aggregatedPain; }
    public float getNutritionLevel() { return NUTRITION_PLACEHOLDER; }
    public float getStamina() { return stamina; }
    public float getEnergy() { return energy; }
    public float getPainShock() { return painShock; }
    public float getSepticShock() { return septicShock; }
    public float getOverexertionPain() { return overexertionPain; }
    public float getInfectionGrowthModifier() { return infectionGrowthModifier; }
    public float getClottingModifier() { return clottingBoostModifier; }

    // Special accessors.
    Map<LimbNode, LimbData> getLimbsInternal() { return limbData; }
    List<CirculatingSubstance> getSubstancesInternal() { return activeSubstances; }
    public List<CirculatingSubstance> getActiveSubstances() { return Collections.unmodifiableList(activeSubstances); }
    public List<SubstanceType> getClientActiveSubstances() { return clientActiveSubstances;}
    public void setJustJumped() { justJumped = true; }
    public boolean consumeJumpFlag()
    {
        if (!justJumped) return false;
        justJumped = false;
        return true;
    }

    public void setVascularTone(float v)
    {
        float c = Math.max(0.1f, Math.min(3.0f, v));
        if (vascularTone != c)
        {
            vascularTone = c;
            markDirty();
        }
    }

    public void setFibrillations(float v)
    {
        float c = Math.max(0f, Math.min(1f, v));
        if (fibrillations != c)
        {
            fibrillations = c;
            markDirty();
        }
    }

    public void setFibrillationsForced(boolean forced, float target)
    {
        fibrillationsForced = forced;
        fibrillationsForcedTarget = Math.max(0f, Math.min(1f, target));
        markDirty();
    }

    public void setAirwayState(AirwayState state)
    {
        if (airwayState != state)
        {
            airwayState = state;
            markDirty();
        }
    }

    public void setCoreTemperature(float v)
    {
        // Clamp to survivable range.
        float c = Math.max(20f, Math.min(42f, v));
        if (coreTemperature != c)
        {
            coreTemperature = c;
            markDirty();
        }
    }

    public void setOxygenSaturation(float v)
    {
        float c = Math.max(0f, Math.min(ModConstants.SPO2_MAX, v));
        if (oxygenSaturation != c)
        {
            oxygenSaturation = c;
            markDirty();
        }
    }

    public void setImmunity(float v)
    {
        float c = Math.max(0f, Math.min(1f, v));
        if (immunity != c)
        {
            immunity = c;
            markDirty();
        }
    }

    public void setImmuneReserve(float v)
    {
        float c = Math.max(0f, Math.min(ModConstants.IMMUNE_RESERVE_MAX, v));
        if (immuneReserve != c)
        {
            immuneReserve = c;
            markDirty();
        }
    }

    public void setBacteremia(float v)
    {
        float c = Math.max(0f, v);
        if (bacteremia != c)
        {
            bacteremia = c;
            markDirty();
        }
    }

    public void setOverexertionPain(float v)
    {
        float c = Math.max(0f, Math.min(1f, v));
        if (overexertionPain != c)
        {
            overexertionPain = c;
            markDirty();
        }
    }

    public List<SubstanceType> collectActiveSubstanceTypes()
    {
        LinkedHashSet<SubstanceType> types = new LinkedHashSet<>();

        for (CirculatingSubstance substance : getSubstancesInternal())
            types.add(substance.getType());

        for (LimbData limb : getLimbsInternal().values())
            for (CirculatingSubstance substance : limb.getLocalSubstances())
                types.add(substance.getType());

        return new ArrayList<>(types);
    }

    // === CLIENT-ONLY SETTERS ===
    // These write received packet values directly into cached fields.

    public void setBloodVolumeClientOnly(float v) { bloodVolume = v; }
    public void setHeartRateBPMClientOnly(float v) { heartRateBPM = v; }
    public void setSystolicBPClientOnly(float v) { systolicBP = v; }
    public void setDiastolicBPClientOnly(float v) { diastolicBP = v; }
    public void setActualRespiratoryRateClientOnly(float v) { actualRespiratoryRate = v; }
    public void setBreathReserveSecondsClientOnly(float v) { breathReserveSeconds = v; }
    public void setConsciousnessClientOnly(float v) { consciousness = v; }
    public void setStamina(float v) { stamina = v; }
    public void setEnergy(float v) { energy = v; }
    public void setPainShock(float v) { painShock = v; }
    public void setSepticShock(float v) { septicShock = v; }
    public void setAggregatedPainClientOnly(float v) { aggregatedPain = v; }
    public void setClientActiveSubstances(List<SubstanceType> types)
    {
        clientActiveSubstances.clear();
        clientActiveSubstances.addAll(types);
    }

    // === PACKET SYNC STUFF ===

    // Returns true and clears the flag.
    public boolean consumeSyncFlag()
    {
        if (!syncNeeded)
        {
            for (LimbData limb : limbData.values())
            {
                if (limb.consumeSyncFlag())
                {
                    return true;
                }
            }
            return false;
        }

        syncNeeded = false;
        return true;
    }

    public void markDirty()
    {
        syncNeeded = true;
    }

    // === DEBUG ===

    public void resetToDefaults()
    {
        systolicBP = 120f;
        diastolicBP = 80f;
        vascularTone = 1.0f;
        bloodViscosity = 1.0f;
        systemicHematocrit = ModConstants.COMP_RESTING_HEMATOCRIT;
        redCellFraction = 1.0f;
        fibrillations = 0.0f;
        fibrillationsForced = false;
        fibrillationsForcedTarget = 0.0f;
        respiratoryDrive = 16f;
        actualRespiratoryRate = 16f;
        breathReserveSeconds = BREATH_RESERVE_MAX;
        airwayState = AirwayState.CLEAR;
        coreTemperature = 36.8f;
        oxygenSaturation = 0.97f;
        heartRateBPM = 72f;
        consciousness = 1.0f;
        immunity = 1.0f;
        immuneReserve = ModConstants.IMMUNE_RESERVE_MAX;
        bacteremia = 0f;
        stamina = 1.0f;
        energy = 1.0f;
        painShock = 0;
        septicShock = 0;
        overexertionPain = 0;

        // Reinitialize limbs to their defaults.
        for (LimbNode node : LimbNode.values())
        {
            limbData.put(node, new LimbData(node));
        }
        leftLung.reset();
        rightLung.reset();

        // Remove substances.
        activeSubstances.clear();

        recomputeBloodVolume();
        recomputeAgreggatedPain();
        resetTransientModifiers();
        markDirty();
    }

    @Override
    public String toString()
    {
        return String.format(
                """
                        PlayerHealth{
                            CARDIOVASCULAR
                                Blood Volume = %.0fml
                                Blood Pressure = %d/%d
                                Blood Viscosity = %.0f
                                Heart Rate = %.0f BPM
                                Fibrillations = %.2f%s
                            RESPIRATORY:
                                  SpO2 = %.0f%%
                                  Respiratory Rate = %.0f/%.0f breaths per minute
                                  Breath = %.0fs
                                  Airway = %s
                                  Lungs =
                                    L[%s]
                                    R[%s]
                            SYSTEMIC:
                                  Temperature = %.1f°C
                                  Consciousness = %.0f%%
                                  Immunity = %.0f%%
                                  Immune Reserves = %.0f
                                  Bacteremia = %.0f%%
                                  Stamina = %.0f%%
                                  Pain = %.0f%%
                                  Overexertion Pain = %.0f%%
                                  Shock = %.0f%%
                                  Sepsis = %.0f%%
                        }""",
                bloodVolume,
                (int) systolicBP, (int) diastolicBP,
                bloodViscosity,
                heartRateBPM,
                fibrillations, fibrillationsForced ? "(forced)" : "",
                oxygenSaturation * 100f,
                actualRespiratoryRate, respiratoryDrive,
                breathReserveSeconds,
                airwayState,
                leftLung, rightLung,
                coreTemperature,
                consciousness * 100f,
                immunity * 100f,
                immuneReserve * 100f,
                bacteremia * 100f,
                stamina * 100f,
                aggregatedPain * 100f,
                overexertionPain * 100f,
                painShock * 100f,
                septicShock * 100f
        );
    }

    // === SAVING ===

    public void saveToNBT(CompoundTag tag)
    {
        tag.putFloat("SystolicBP", systolicBP);
        tag.putFloat("DiastolicBP", diastolicBP);
        tag.putFloat("VascularTone", vascularTone);
        tag.putFloat("Fibrillations", fibrillations);
        tag.putBoolean("FibrillationsForced", fibrillationsForced);
        tag.putFloat("FibrillationsForcedTarget", fibrillationsForcedTarget);
        tag.putFloat("RespiratoryDrive", respiratoryDrive);
        tag.putFloat("ActualRespiratoryRate", actualRespiratoryRate);
        tag.putFloat("BreathReserveSeconds", breathReserveSeconds);
        tag.putString("AirwayState", airwayState.name());
        tag.putFloat("CoreTemperature", coreTemperature);
        tag.putFloat("OxygenSaturation", oxygenSaturation);
        tag.putFloat("HeartRateBPM", heartRateBPM);
        tag.putFloat("Consciousness", consciousness);
        tag.putFloat("Immunity", immunity);
        tag.putFloat("Stamina", stamina);
        tag.putFloat("Energy", energy);
        tag.putFloat("PainShock", painShock);
        tag.putFloat("SepticShock", septicShock);
        tag.putFloat("OverexertionPain", overexertionPain);
        tag.putFloat("ImmuneReserve", immuneReserve);
        tag.putFloat("Bacteremia", bacteremia);

        // Limbs.
        CompoundTag limbsTag = new CompoundTag();
        for (Map.Entry<LimbNode, LimbData> entry : limbData.entrySet())
        {
            CompoundTag limbTag = new CompoundTag();
            entry.getValue().saveToNBT(limbTag);
            limbsTag.put(entry.getKey().name(), limbTag);
        }
        tag.put("Limbs", limbsTag);

        // Lungs.
        CompoundTag leftLungTag = new CompoundTag();
        CompoundTag rightLungTag = new CompoundTag();
        leftLung.saveToNBT(leftLungTag);
        rightLung.saveToNBT(rightLungTag);
        tag.put("LeftLung", leftLungTag);
        tag.put("RightLung", rightLungTag);

        // Substances.
        ListTag substanceList = new ListTag();
        for (CirculatingSubstance s : activeSubstances)
        {
            CompoundTag st = new CompoundTag();
            s.saveToNBT(st);
            substanceList.add(st);
        }
        tag.put("Substances", substanceList);
    }

    public void loadFromNBT(CompoundTag tag)
    {
        systolicBP = tag.getFloat("SystolicBP");
        diastolicBP = tag.getFloat("DiastolicBP");
        vascularTone = tag.getFloat("VascularTone");
        fibrillations = tag.getFloat("Fibrillations");
        fibrillationsForced = tag.getBoolean("FibrillationsForced");
        fibrillationsForcedTarget = tag.getFloat("FibrillationsForcedTarget");
        respiratoryDrive = tag.getFloat("RespiratoryDrive");
        actualRespiratoryRate = tag.getFloat("ActualRespiratoryRate");
        breathReserveSeconds = tag.getFloat("BreathReserveSeconds");
        airwayState = AirwayState.valueOf(tag.getString("AirwayState"));
        coreTemperature = tag.getFloat("CoreTemperature");
        oxygenSaturation = tag.getFloat("OxygenSaturation");
        heartRateBPM = tag.getFloat("HeartRateBPM");
        consciousness = tag.getFloat("Consciousness");
        immunity = tag.getFloat("Immunity");
        immuneReserve = tag.getFloat("ImmuneReserve");
        bacteremia = tag.getFloat("Bacteremia");
        stamina = tag.getFloat("Stamina");
        energy = tag.getFloat("Energy");
        painShock = tag.getFloat("PainShock");
        septicShock = tag.getFloat("SepticShock");
        overexertionPain = tag.getFloat("OverexertionPain");

        // This has a guard against missing limb data, in case i ever
        // implement some sort of amputation system. Unlikely, but yknow.
        if (tag.contains("Limbs"))
        {
            CompoundTag limbsTag = tag.getCompound("Limbs");
            for (LimbNode node : LimbNode.values())
            {
                // If the node is missing from the NBT, the default initialized LimbData stays.
                if (limbsTag.contains(node.name()))
                {
                    limbData.get(node).loadFromNBT(limbsTag.getCompound(node.name()));
                }
            }
        }
        if (tag.contains("LeftLung"))
        {
            leftLung.loadFromNBT(tag.getCompound("LeftLung"));
        }
        if (tag.contains("RightLung"))
        {
            rightLung.loadFromNBT(tag.getCompound("RightLung"));
        }

        // Substances.
        activeSubstances.clear();
        if (tag.contains("Substances"))
        {
            ListTag substanceList = tag.getList("Substances", Tag.TAG_COMPOUND);
            for (int i = 0; i < substanceList.size(); i++)
            {
                CirculatingSubstance s = new CirculatingSubstance();
                s.loadFromNBT(substanceList.getCompound(i));
                activeSubstances.add(s);
            }
        }

        // Recompute derived values from loaded state.
        recomputeBloodVolume();
        recomputeAgreggatedPain();
    }

    // Copies all values from Other into this instance, to preserve
    // all the health data through death or dimension transfer.
    public void copyFrom(PlayerHealthData other)
    {
        this.coreTemperature = other.coreTemperature;
        this.oxygenSaturation = other.oxygenSaturation;
        this.heartRateBPM = other.heartRateBPM;
        this.consciousness = other.consciousness;
        this.immunity = other.immunity;
        this.immuneReserve = other.immuneReserve;
        this.bacteremia = other.bacteremia;
        this.stamina = other.stamina;
        this.energy = other.energy;
        this.painShock = other.painShock;
        this.septicShock = other.septicShock;
        this.overexertionPain = other.overexertionPain;
        this.systolicBP = other.systolicBP;
        this.diastolicBP = other.diastolicBP;
        this.vascularTone = other.vascularTone;
        this.bloodViscosity = other.bloodViscosity;
        this.fibrillations = other.fibrillations;
        this.fibrillationsForced = other.fibrillationsForced;
        this.fibrillationsForcedTarget = other.fibrillationsForcedTarget;
        this.respiratoryDrive = other.respiratoryDrive;
        this.actualRespiratoryRate = other.actualRespiratoryRate;
        this.breathReserveSeconds = other.breathReserveSeconds;
        this.airwayState = other.airwayState;

        for (LimbNode node : LimbNode.values())
        {
            this.limbData.get(node).copyFrom(other.limbData.get(node));
        }

        this.leftLung.copyFrom(other.leftLung);
        this.rightLung.copyFrom(other.rightLung);

        this.activeSubstances.clear();
        for (CirculatingSubstance s : other.activeSubstances)
        {
            CirculatingSubstance copy = new CirculatingSubstance();
            CompoundTag t = new CompoundTag();
            s.saveToNBT(t);
            copy.loadFromNBT(t);
            this.activeSubstances.add(copy);
        }

        recomputeBloodVolume();
        recomputeAgreggatedPain();
        markDirty();
    }
}
