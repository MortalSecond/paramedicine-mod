package net.invinciblemoebius.traumaparamedicinemod.health;
import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.limbs.AirwayState;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LungData;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

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
    // How thick the blood is. ALWAYS 1.0 UNLESS THERE'S A HYDRATION MOD.
    // It's a placeholder because, ngl, i don't think there's any hemotoxic mobs in MC.
    // Over 1.4 value means there's an increased risk of thrombosis and stroke.
    private float bloodViscosity = 1.0f;

    // LIMB NODES.
    private final Map<LimbNode, LimbData> limbData = new EnumMap<>(LimbNode.class);
    private final LungData leftLung = new LungData();
    private final LungData rightLung = new LungData();

    // CARDIOVASCULAR VALUES
    // The blood volume value is DERIVED, this is just cache.
    private float bloodVolume = 5000f;
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
    private float immunity = 1.0f;
    private float aggregatedPain = 0f;

    // When any field changes, this indicates if the packet should be sent.
    // P.S. This idea was shamelessly stolen from Casualties: Cubed.
    private boolean syncNeeded = true;

    // === CONSTRUCTOR ===

    public PlayerHealthData()
    {
        for (LimbNode node: LimbNode.values())
            limbData.put(node, new LimbData(node));
    }

    // === COMPUTATION METHODS ===

    // Sum of all the limb's blood volume values.
    public void recomputeBloodVolume()
    {
        float sum = 0f;

        for (LimbData limb: limbData.values())
            sum += limb.getActualBloodVolume();

        if (this.bloodVolume != sum)
        {
            this.bloodVolume = sum;
            markDirty();
        }
    }

    // Really complicated BP math that i REALLY encourage understanding bit-by-bit.
    // I'm not very satisfied with it, but my research didn't turn up any concrete formula.
    // At least, not one that fit Paramedicine's variables. If there's an IRL medic that
    // could help get a simpler calculation for BP, i'd appreciate the help.
    public void recomputeBloodPressure()
    {
        float bloodFraction = bloodVolume / ModConstants.BLOOD_NORMAL;

        // Base dystolic from volume. Roughly linear; 100% volume = 120, while 30% volume = 40.
        float volumeBasedSystolic = 40f + (bloodFraction * 0.80f);
        // Cardiac efficiency from rhythm quality. Perfect rhythm = 1.0, full V-Fib = 0.05.
        float cardiacEfficiency = 1.0f - (fibrillations * 0.95f);
        // Heart rate contribution to systolic. Tachy raises systolic, but REALLY severe tachy reduces it.
        float rateModifier;
        if (heartRateBPM <= ModConstants.BPM_NORMAL_MAX)
            rateModifier = 1.0f;
        else if (heartRateBPM <= ModConstants.BPM_SEVERE_TACHICARDIA)
            rateModifier = 1.0f + ((heartRateBPM - ModConstants.BPM_TACHYCARDIA)  / 100f) * 0.1f;
        else
            rateModifier = 1.1f - ((heartRateBPM - ModConstants.BPM_SEVERE_TACHICARDIA) / 100f) * 0.3f;
        rateModifier = Math.max(0.5f, rateModifier);

        // Computation.
        systolicBP = volumeBasedSystolic * vascularTone * cardiacEfficiency * rateModifier * bloodViscosity;

        // Diastolic widens in vasodilation but narrows during vasoconstriction.
        float pulsePressureRatio = 0.5f + (0.2f * (1.0f / Math.max(0.1f, vascularTone)));
        // Diastolic is proportional to systolic at roughly 0.6 ratio.
        diastolicBP = systolicBP * Math.min(0.85f, pulsePressureRatio);

        markDirty();
    }

    public void recomputeRespiratoryDrive()
    {
        float base = ModConstants.RESPIRATORY_NORMAL;

        // Hypoxia response. Up to +20 breaths per minute under severe hypoxia.
        float hypoxiaResponse = 0f;
        if (oxygenSaturation < ModConstants.SPO2_HYPOXIA)
        {
            float deficit = (ModConstants.SPO2_HYPOXIA - oxygenSaturation) / (ModConstants.SPO2_HYPOXIA - ModConstants.SPO2_FLOOR);
            hypoxiaResponse = deficit * 20f;
        }

        // Blood loss response.
        float bloodResponse = 0f;
        float bloodFraction = bloodVolume / ModConstants.BLOOD_NORMAL;
        if (bloodFraction < ModConstants.BLOOD_MODERATE_HYPOVOLEMIA)
            bloodResponse = (ModConstants.BLOOD_MODERATE_HYPOVOLEMIA - bloodFraction) / ModConstants.BLOOD_MODERATE_HYPOVOLEMIA * 0.14f;

        // Pain response.
        float painResponse = aggregatedPain * 8f;

        respiratoryDrive = Math.min(35f, base + hypoxiaResponse + bloodResponse + painResponse);

        markDirty();
    }

    public void recomputeActualRespiratoryRate(boolean isUnderwater)
    {
        float dt = ModConstants.SECONDS_PER_TICK;

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
            lungCeiling *= 0.6f;

        // Ceiling from consciousness (to simulate agonal breathing).
        float consciousnessCeiling;
        if (consciousness >= ModConstants.CONSCIOUSNESS_VOICE)
            consciousnessCeiling = 1.0f;
        else if (consciousness >= ModConstants.CONSCIOUSNESS_PAIN)
            consciousnessCeiling = 0.6f;
        else
            consciousnessCeiling = 0.2f;

        // Forced zero conditions.
        boolean forcedZero = isUnderwater || airwayState == AirwayState.FULLY_OBSTRUCTED;

        float maxRate = respiratoryDrive * Math.max(airwayCeiling, Math.min(lungCeiling, consciousnessCeiling));

        if (forcedZero)
        {
            actualRespiratoryRate = 0f;

            // Drain breath reserve.
            float drainRate = respiratoryDrive / BREATH_RESERVE_MAX;
            breathReserveSeconds = Math.max(0f, breathReserveSeconds - (drainRate - dt));
        }
        else
        {
            actualRespiratoryRate = maxRate;

            // Replenish breath reserve when breathing normally. Five seconds of recovery at a normal rate.
            float replenishRate = (actualRespiratoryRate / ModConstants.RESPIRATORY_NORMAL) * 5f;
            breathReserveSeconds = Math.min(BREATH_RESERVE_MAX, breathReserveSeconds + (replenishRate * dt));
        }

        markDirty();
    }

    // Sum of all the limb's pain values.
    public void recomputeAgreggatedPain()
    {
        float sum = 0f;

        for (LimbData limb: limbData.values())
            sum += limb.getEffectivePain();

        // Clamping it because, in theory, many small wounds together could go above 1.0.
        this.aggregatedPain = Math.min(1.0f, sum);
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
            bloodCeiling = (bloodFraction - ModConstants.BLOOD_SEVERE_HYPOVOLEMIA) / 0.30f;

        // SpO2 ceiling.
        float spo2Ceiling;
        if (oxygenSaturation >= ModConstants.SPO2_HYPOXIA)
            spo2Ceiling = 1.0f;
        else if (oxygenSaturation <= ModConstants.SPO2_FLOOR)
            spo2Ceiling = 0.0f;
        else
            spo2Ceiling = (oxygenSaturation - ModConstants.SPO2_FLOOR) / 0.19f;

        // Pain ceiling.
        float painCeiling;
        if (aggregatedPain <= 0.70f)
            painCeiling = 1.0f;
        else
            painCeiling = 1.0f -((aggregatedPain - 0.70f)/0.30f) * 0.40f;

        // Brain damage ceiling.
        LimbData headNode = limbData.get(LimbNode.HEAD);
        float brainCeiling = (headNode != null) ? headNode.getTotalHealth() : 1.0f;

        // Lowest ceiling wins.
        consciousnessTarget = Math.min(
                Math.min(brainCeiling, spo2Ceiling),
                Math.min(painCeiling, brainCeiling)
        );

        // Apply inertia.
        float inertiaRate = 0.001f;
        if (consciousness < consciousnessTarget)
            consciousness = Math.min(consciousnessTarget, consciousness + inertiaRate);
        else if (consciousness > consciousnessTarget)
            consciousness = Math.max(consciousnessTarget, consciousness - inertiaRate);

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
            bloodResponse = (1.0f - Math.max(ModConstants.BLOOD_CRITICAL_HYPOVOLEMIA, bloodResponse)) / 0.70f * 80f;

        // Pain-driven sympathetic response.
        // Scales to +30 BPM at maxiumum normal pain.
        float painResponse = aggregatedPain * 30f;

        // Hypoxia compensation.
        // Scales to +40 BPM when critically hypoxic.
        float spo2Response = 0f;
        if (oxygenSaturation < ModConstants.SPO2_HYPOXIA)
            spo2Response = (ModConstants.SPO2_HYPOXIA - oxygenSaturation) / 0.19f * 40f;

        // Hypothermia suppression.
        // Scales down to -40 BPM when hypothermic.
        float tempSuppression = 0f;
        if (coreTemperature < ModConstants.TEMP_HYPOTHERMIA)
            tempSuppression = (ModConstants.TEMP_HYPOTHERMIA - coreTemperature) / 7.0f * 40f;

        // Computation.
        float computed = base + bloodResponse + painResponse + spo2Response - tempSuppression;
        this.heartRateBPM = Math.max(0f, Math.min(220f, computed));
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
            float respiratoryEfficiency = (actualRespiratoryRate / respiratoryDrive) * (1.0f - (getTotalLungCompromise() * 0.5f));
            float recovery = respiratoryEfficiency * 0.01f * dt;
            setOxygenSaturation(Math.min(ModConstants.SPO2_NORMAL, oxygenSaturation + recovery));
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
            float fastDrop = 0.0003f * dt;
            setOxygenSaturation(Math.max(ModConstants.SPO2_FLOOR, oxygenSaturation - fastDrop));
        }
    }

    public void tickFibrillations()
    {
        if (!fibrillationsForced) return;
        if (fibrillations >= fibrillationsForcedTarget) return;

        // Returns to forced level after 30secs post-defib.
        float dt = ModConstants.SECONDS_PER_TICK;
        float driftRate = 0.002f * dt;
        fibrillations = Math.min(fibrillationsForcedTarget, fibrillations + driftRate);

        markDirty();
    }

    public void defibrillate()
    {
        fibrillations = 0.0f;
        markDirty();
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

    // === ACCESSORS ===

    public float getBloodVolume() { return bloodVolume; }
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
    public float getHeartRateBPM() { return heartRateBPM; }
    public LungData getLeftLung() { return leftLung; }
    public LungData getRightLung() { return rightLung; }
    public float getCoreTemperature() { return coreTemperature; }
    public float getConsciousness() { return consciousness; }
    public float getImmunity() { return immunity; }
    public float getAggregatedPain() { return aggregatedPain; }
    public float getNutritionLevel() { return NUTRITION_PLACEHOLDER; }

    public void setVascularTone(float v)
    {
        float c = Math.max(0.1f, Math.min(3.0f, v));
        if (vascularTone != c) { vascularTone = c; markDirty(); }
    }

    public void setFibrillations(float v)
    {
        float c = Math.max(0f, Math.min(1f, v));
        if (fibrillations != c) { fibrillations = c; markDirty(); }
    }

    public void setFibrillationsForced(boolean forced, float target)
    {
        fibrillationsForced = forced;
        fibrillationsForcedTarget = Math.max(0f, Math.min(1f, target));
        markDirty();
    }

    public void setAirwayState(AirwayState state)
    {
        if (airwayState != state) { airwayState = state; markDirty(); }
    }

    public void setCoreTemperature(float v)
    {
        // Clamp to survivable range.
        float c = Math.max(20f, Math.min(42f, v));
        if (coreTemperature != c) {coreTemperature = c; markDirty();}
    }

    public void setOxygenSaturation(float v)
    {
        float c = Math.max(0f, Math.min(1f, v));
        if (oxygenSaturation != c) {oxygenSaturation = c; markDirty();}
    }

    public void setImmunity(float v)
    {
        float c = Math.max(0f, Math.min(1f, v));
        if (immunity != c) {immunity = c; markDirty();}
    }

    // === PACKET SYNC STUFF ===

    // Returns true and clears the flag.
    public boolean consumeSyncFlag()
    {
        if(!syncNeeded)
        {
            for (LimbData limb: limbData.values())
                if (limb.consumeSyncFlag())
                    return true;
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

        // Reinitialize limbs to their defaults.
        for (LimbNode node: LimbNode.values())
            limbData.put(node, new LimbData(node));
        leftLung.reset();
        rightLung.reset();

        recomputeBloodVolume();
        recomputeAgreggatedPain();
        markDirty();
    }

    @Override
    public String toString()
    {
        return String.format(
                """
                        PlayerHealth{
                          blood=%.0fml  BP=%d/%d  BPM=%.0f  fibs=%.2f%s
                          SpO2=%.0f%%  RR=%.0f/%.0f bpm  breath=%.0fs  airway=%s
                          lungs=L[%s] R[%s]
                          temp=%.1f°C  conscious=%.0f%%  immunity=%.0f%%  pain=%.0f%%
                        }""",
                bloodVolume,
                (int) systolicBP, (int) diastolicBP,
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
                aggregatedPain * 100f
        );
    }

    // === SAVING ===

    public void saveToNBT(CompoundTag tag)
    {
        tag.putFloat ("SystolicBP", systolicBP);
        tag.putFloat ("DiastolicBP", diastolicBP);
        tag.putFloat ("VascularTone", vascularTone);
        tag.putFloat ("Fibrillations", fibrillations);
        tag.putBoolean ("FibrillationsForced", fibrillationsForced);
        tag.putFloat ("FibrillationsForcedTarget", fibrillationsForcedTarget);
        tag.putFloat ("RespiratoryDrive", respiratoryDrive);
        tag.putFloat ("ActualRespiratoryRate", actualRespiratoryRate);
        tag.putFloat ("BreathReserveSeconds", breathReserveSeconds);
        tag.putString ("AirwayState", airwayState.name());
        tag.putFloat("CoreTemperature", coreTemperature);
        tag.putFloat("OxygenSaturation", oxygenSaturation);
        tag.putFloat("HeartRateBPM", heartRateBPM);
        tag.putFloat("Consciousness", consciousness);
        tag.putFloat("Immunity", immunity);

        // Limbs.
        CompoundTag limbsTag = new CompoundTag();
        for (Map.Entry<LimbNode, LimbData> entry: limbData.entrySet())
        {
            CompoundTag limbTag = new CompoundTag();
            entry.getValue().saveToNBT(limbTag);
            limbsTag.put(entry.getKey().name(), limbTag);
        }
        tag.put("Limbs", limbsTag);
        CompoundTag leftLungTag = new CompoundTag();
        CompoundTag rightLungTag = new CompoundTag();
        leftLung.saveToNBT(leftLungTag);
        rightLung.saveToNBT(rightLungTag);
        tag.put("LeftLung", leftLungTag);
        tag.put("RightLung", rightLungTag);
    }

    public void loadFromNBT(CompoundTag tag)
    {
        systolicBP = tag.getFloat ("SystolicBP");
        diastolicBP = tag.getFloat ("DiastolicBP");
        vascularTone = tag.getFloat ("VascularTone");
        fibrillations = tag.getFloat ("Fibrillations");
        fibrillationsForced = tag.getBoolean ("FibrillationsForced");
        fibrillationsForcedTarget = tag.getFloat ("FibrillationsForcedTarget");
        respiratoryDrive = tag.getFloat ("RespiratoryDrive");
        actualRespiratoryRate = tag.getFloat ("ActualRespiratoryRate");
        breathReserveSeconds = tag.getFloat ("BreathReserveSeconds");
        airwayState = AirwayState.valueOf(tag.getString("AirwayState"));
        coreTemperature = tag.getFloat("CoreTemperature");
        oxygenSaturation = tag.getFloat("OxygenSaturation");
        heartRateBPM = tag.getFloat("HeartRateBPM");
        consciousness = tag.getFloat("Consciousness");
        immunity = tag.getFloat("Immunity");

        // This has a guard against missing limb data, in case i ever
        // implement some sort of amputation system. Unlikely, but yknow.
        if (tag.contains("Limbs"))
        {
            CompoundTag limbsTag = tag.getCompound("Limbs");
            for (LimbNode node: LimbNode.values())
            {
                // If the node is missing from the NBT, the default initialized LimbData stays.
                if (limbsTag.contains(node.name()))
                    limbData.get(node).loadFromNBT(limbsTag.getCompound(node.name()));
            }
        }
        if (tag.contains("LeftLung"))
            leftLung.loadFromNBT(tag.getCompound("LeftLung"));
        if (tag.contains("RightLung"))
            rightLung.loadFromNBT(tag.getCompound("RightLung"));

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

        for (LimbNode node: LimbNode.values())
            this.limbData.get(node).copyFrom(other.limbData.get(node));

        recomputeBloodVolume();
        recomputeAgreggatedPain();
        markDirty();
    }
}
