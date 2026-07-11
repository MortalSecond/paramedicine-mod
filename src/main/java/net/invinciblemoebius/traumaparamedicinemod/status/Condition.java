package net.invinciblemoebius.traumaparamedicinemod.status;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.*;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundDepth;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

// Tiered conditions like hypertension have one entry per tier.
// Mind that these values are tuned for Minecraft's timescale, so for example
// even though IRL Class 3 hemorrhage happens at 0.2ml/min, in this mod
// it's 15ml/s, since players will be able to stop the bleeding in a
// fraction of the time that IRL medicine would.
public enum     Condition
{
    // === POSITIVE CONDITIONS ===

    IMMUNOCOMPETENT(ConditionSeverity.GREEN, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float immunity = data.getImmunity();
                    return immunity >= 1.5f;
                }
            },

    SLAKED(ConditionSeverity.GREEN, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float thirst = data.getThirst();
                    return thirst <= 0.1f;
                }
            },

    FULL(ConditionSeverity.GREEN, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float hunger = data.getHunger();
                    return hunger <= 0.1f;
                }
            },

    // === PAIN ===

    DISCOMFORT(ConditionSeverity.NEUTRAL, ObservabilityLevel.SUBJECTIVE)
    {
        @Override
        public boolean evaluate(PlayerHealthData data)
        {
            float pain = data.getAggregatedPain();
            return pain >= 0.03f && pain < 0.30f;
        }
    },

    PAIN(ConditionSeverity.MILD, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float pain = data.getAggregatedPain();
                    return pain >= 0.30f && pain < 0.55f;
                }
            },

    SEVERE_PAIN(ConditionSeverity.MODERATE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float pain = data.getAggregatedPain();
                    return pain >= 0.55f && pain < 0.80f;
                }
            },

    AGONY(ConditionSeverity.SEVERE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float pain = data.getAggregatedPain();
                    return pain >= 0.80f;
                }
            },

    PAIN_SHOCK(ConditionSeverity.CRITICAL, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float shock = data.getPainShock();
                    return shock > 0f;
                }
            },

    // === CONSCIOUSNESS ===

    CONFUSED(ConditionSeverity.NEUTRAL, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float consciousness = data.getConsciousness();
                    return consciousness < 0.90f && consciousness >= ModConstants.CONSCIOUSNESS_ALERT;
                }
            },

    VERY_CONFUSED(ConditionSeverity.MILD, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float consciousness = data.getConsciousness();
                    return consciousness < ModConstants.CONSCIOUSNESS_ALERT && consciousness >= ModConstants.CONSCIOUSNESS_VOICE;
                }
            },

    FAINTING(ConditionSeverity.MODERATE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float consciousness = data.getConsciousness();
                    return consciousness < ModConstants.CONSCIOUSNESS_VOICE && consciousness >= 0.40f;
                }
            },

    INCAPACITATED(ConditionSeverity.SEVERE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float consciousness = data.getConsciousness();
                    return consciousness < 0.40f && consciousness >= ModConstants.CONSCIOUSNESS_PAIN;
                }
            },

    UNCONSCIOUS(ConditionSeverity.CRITICAL, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float consciousness = data.getConsciousness();
                    return consciousness < ModConstants.CONSCIOUSNESS_PAIN;
                }
            },

    // === CARDIOVASCULAR - HEART RATE ===

    MILD_TACHYCARDIA(ConditionSeverity.NEUTRAL, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float heartRateBPM = data.getHeartRateBPM();
                    return heartRateBPM > ModConstants.BPM_TACHYCARDIA && heartRateBPM <= 130f;
                }
            },

    TACHYCARDIA(ConditionSeverity.MILD, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float heartRateBPM = data.getHeartRateBPM();
                    return heartRateBPM > 130f && heartRateBPM <= ModConstants.BPM_SEVERE_TACHYCARDIA;
                }
            },

    SEVERE_TACHYCARDIA(ConditionSeverity.MODERATE, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float heartRateBPM = data.getHeartRateBPM();
                    CardiacRhythm rhythm = data.getRhythm();
                    return heartRateBPM > ModConstants.BPM_SEVERE_TACHYCARDIA && rhythm == CardiacRhythm.SINUS_TACHYCARDIA;
                }
            },

    BRADYCARDIA(ConditionSeverity.MILD, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float heartRateBPM = data.getHeartRateBPM();
                    return heartRateBPM < ModConstants.BPM_BRADYCARDIA && heartRateBPM >= 40f;
                }
            },

    SEVERE_BRADYCARDIA(ConditionSeverity.MODERATE, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float heartRateBPM = data.getHeartRateBPM();
                    return heartRateBPM < 40f && heartRateBPM > ModConstants.BPM_CARDIAC_ARREST;
                }
            },

    // === CARDIOVASCULAR - RHYTHM ===

    ARRYTHMIA(ConditionSeverity.MILD, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float instability = data.getElectricalInstability();
                    return instability >= 0.15f && instability < 0.50f;
                }
            },

    VENTRICULAR_TACHYCARDIA(ConditionSeverity.MODERATE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    CardiacRhythm rhythm = data.getRhythm();
                    return rhythm == CardiacRhythm.VENTRICULAR_TACHYCARDIA;
                }
            },

    PALPITATIONS(ConditionSeverity.MODERATE, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float instability = data.getElectricalInstability();
                    return instability >= 0.50 && !data.isArrested();
                }
            },

    VENTRICULAR_FIBRILLATIONS(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    CardiacRhythm rhythm = data.getRhythm();
                    return rhythm == CardiacRhythm.VENTRICULAR_FIBRILLATION;
                }
            },

    SENSE_OF_IMPENDING_DOOM(ConditionSeverity.SEVERE, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float instability = data.getElectricalInstability();
                    return instability >= 0.70f && !data.isArrested();
                }
            },

    MYOCARDIAL_EXHAUSTION(ConditionSeverity.CRITICAL, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float reserve = data.getHeartReserve();
                    return reserve < ModConstants.RESERVE_WEAK && reserve > ModConstants.RESERVE_ASYSTOLE && !data.isArrested();
                }
            },

    PULSELESS_ELECTRICAL_ACTIVITY(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    CardiacRhythm rhythm = data.getRhythm();
                    return rhythm == CardiacRhythm.PULSELESS_ELECTRICAL_ACTIVITY;
                }
            },

    CARDIAC_ARREST(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    CardiacRhythm rhythm = data.getRhythm();
                    return rhythm == CardiacRhythm.ASYSTOLE;
                }
            },

    // === CARDIOVASCULAR - BLOOD PRESSURE ===

    MILD_HYPOTENSION(ConditionSeverity.NEUTRAL, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float systolicBP = data.getSystolicBP();
                    boolean cardiacArrest = data.isArrested();
                    return systolicBP < 110f && systolicBP >= 96f && !cardiacArrest;
                }
            },

    HYPOTENSION(ConditionSeverity.MILD, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float systolicBP = data.getSystolicBP();
                    boolean cardiacArrest = data.isArrested();
                    return systolicBP < 96f && systolicBP >= 83f && !cardiacArrest;
                }
            },

    SEVERE_HYPOTENSION(ConditionSeverity.MODERATE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float systolicBP = data.getSystolicBP();
                    boolean cardiacArrest = data.isArrested();
                    return systolicBP < 83f && systolicBP >= 60f && !cardiacArrest;
                }
            },

    CIRCULATORY_COLLAPSE(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float systolicBP = data.getSystolicBP();
                    boolean cardiacArrest = data.isArrested();
                    return systolicBP < 60f && !cardiacArrest;
                }
            },

    MILD_HYPERTENSION(ConditionSeverity.NEUTRAL, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float systolicBP = data.getSystolicBP();
                    boolean cardiacArrest = data.isArrested();
                    return systolicBP > 130f && systolicBP <= 145f && !cardiacArrest;
                }
            },

    HYPERTENSION(ConditionSeverity.MILD, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float systolicBP = data.getSystolicBP();
                    boolean cardiacArrest = data.isArrested();
                    return systolicBP > 145f && systolicBP <= 162f && !cardiacArrest;
                }
            },

    SEVERE_HYPERTENSION(ConditionSeverity.MODERATE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float systolicBP = data.getSystolicBP();
                    return systolicBP >= 162f && systolicBP < 180f;
                }
            },

    HYPERTENSIVE_CRISIS(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float systolicBP = data.getSystolicBP();
                    return systolicBP >= 180f;
                }
            },

    // === CARDIOVASCULAR - BLOOD VOLUME ===

    MILD_HYPOVOLEMIA(ConditionSeverity.NEUTRAL, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bloodVolumeFraction = data.getBloodVolume() / ModConstants.BLOOD_NORMAL;
                    return bloodVolumeFraction < ModConstants.BLOOD_MILD_HYPOVOLEMIA && bloodVolumeFraction >= ModConstants.BLOOD_MODERATE_HYPOVOLEMIA;
                }
            },

    HYPOVOLEMIA(ConditionSeverity.MILD, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bloodVolumeFraction = data.getBloodVolume() / ModConstants.BLOOD_NORMAL;
                    return bloodVolumeFraction < ModConstants.BLOOD_MODERATE_HYPOVOLEMIA && bloodVolumeFraction >= ModConstants.BLOOD_SEVERE_HYPOVOLEMIA;
                }
            },

    SEVERE_HYPOVOLEMIA(ConditionSeverity.SEVERE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bloodVolumeFraction = data.getBloodVolume() / ModConstants.BLOOD_NORMAL;
                    return bloodVolumeFraction < ModConstants.BLOOD_SEVERE_HYPOVOLEMIA && bloodVolumeFraction >= ModConstants.BLOOD_CRITICAL_HYPOVOLEMIA;
                }
            },

    CRITICAL_HYPOVOLEMIA(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bloodVolumeFraction = data.getBloodVolume() / ModConstants.BLOOD_NORMAL;
                    return bloodVolumeFraction < ModConstants.BLOOD_CRITICAL_HYPOVOLEMIA;
                }
            },

    MILD_HYPERVOLEMIA(ConditionSeverity.NEUTRAL, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bloodVolume = data.getBloodVolume();
                    float normalBlood = ModConstants.BLOOD_NORMAL;
                    return bloodVolume > normalBlood * 1.05 && bloodVolume <= normalBlood * 1.10;
                }
            },

    HYPERVOLEMIA(ConditionSeverity.MILD, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bloodVolume = data.getBloodVolume();
                    float normalBlood = ModConstants.BLOOD_NORMAL;
                    return bloodVolume > normalBlood * 1.10 && bloodVolume <= normalBlood * 1.15;
                }
            },

    CRITICAL_HYPERVOLEMIA(ConditionSeverity.SEVERE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bloodVolume = data.getBloodVolume();
                    float normalBlood = ModConstants.BLOOD_NORMAL;
                    return bloodVolume > normalBlood * 1.15f;
                }
            },

    // === CARDIOVASCULAR - BLOOD COMPOSITION ===

    ANEMIA(ConditionSeverity.MILD, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float redCellsFraction = data.getRedCellFraction();
                    return redCellsFraction < ModConstants.ANEMIA_MILD && redCellsFraction >= ModConstants.ANEMIA_MODERATE;
                }
            },

    MODERATE_ANEMIA(ConditionSeverity.MODERATE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float redCellsFraction = data.getRedCellFraction();
                    return redCellsFraction < ModConstants.ANEMIA_MODERATE && redCellsFraction >= ModConstants.ANEMIA_SEVERE;
                }
            },

    SEVERE_ANEMIA(ConditionSeverity.SEVERE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float redCellsFraction = data.getRedCellFraction();
                    return redCellsFraction < ModConstants.ANEMIA_SEVERE;
                }
            },

    HEMODILUTION(ConditionSeverity.MILD, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float hematocrit = data.getHematocrit();
                    float bloodVolume = data.getBloodVolume();
                    return hematocrit < ModConstants.HEMATOCRIT_DILUTION && bloodVolume >= ModConstants.BLOOD_NORMAL * ModConstants.BLOOD_MODERATE_HYPOVOLEMIA;
                }
            },

    HEMOCONCENTRATION(ConditionSeverity.MILD, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float hematocrit = data.getHematocrit();
                    return hematocrit > ModConstants.HEMATOCRIT_CONCENTRATION;
                }
            },

    // === WOUNDS - HEMORRHAGE ===

    MINOR_BLEEDING(ConditionSeverity.NEUTRAL, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bleedRate = computeExternalBleedRate(data);
                    return bleedRate >= 1f && bleedRate < 3f;
                }
            },

    BLEEDING(ConditionSeverity.MODERATE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bleedRate = computeExternalBleedRate(data);
                    return bleedRate >= 3f && bleedRate < 8f;
                }
            },

    HEAVY_BLEEDING(ConditionSeverity.CRITICAL, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bleedRate = computeExternalBleedRate(data);
                    return bleedRate >= 8f && bleedRate < 15f;
                }
            },

    CATASTROPHIC_BLEEDING(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bleedRate = computeExternalBleedRate(data);
                    return bleedRate >= 15f;
                }
            },

    MINOR_INTERNAL_BLEEDING(ConditionSeverity.SEVERE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bleedRate = computeVisceralBleedRate(data);
                    return bleedRate > 0f && bleedRate < 3f;
                }
            },

    INTERNAL_BLEEDING(ConditionSeverity.CRITICAL, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bleedRate = computeVisceralBleedRate(data);
                    return bleedRate >= 3f && bleedRate < 10f;
                }
            },

    MASSIVE_INTERNAL_BLEEDING(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bleedRate = computeVisceralBleedRate(data);
                    return bleedRate >= 10f;
                }
            },

    // === WOUNDS - BONE ===

    FRACTURE(ConditionSeverity.MODERATE, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    return hasBoneState(data, BoneState.FRACTURED);
                }
            },

    COMPOUND_FRACTURE(ConditionSeverity.SEVERE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    return hasBoneState(data, BoneState.COMPOUND);
                }
            },

    DISLOCATION(ConditionSeverity.MODERATE, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    return hasBoneState(data, BoneState.DISLOCATED);
                }
            },

    // === WOUNDS - NEUROLOGICAL ===

    CONCUSSION(ConditionSeverity.MODERATE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    LimbData head = data.getLimb(LimbNode.HEAD);
                    float headHealth = head.getMuscleHealth();
                    return headHealth < 0.70f;
                }
            },

    SEVERE_HEAD_INJURY(ConditionSeverity.SEVERE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    LimbData head = data.getLimb(LimbNode.HEAD);
                    float headHealth = head.getMuscleHealth();
                    return headHealth < 0.40f;
                }
            },

    // === RESPIRATORY - BREATH ===

    CONTROLLED_APNOEA(ConditionSeverity.NEUTRAL, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float respirations = data.getActualRespiratoryRate();
                    float breathReserve = data.getBreathReserveSeconds();
                    return respirations <= 0f && breathReserve > 0f;
                }
            },

    UNCONTROLLED_APNOEA(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float respirations = data.getActualRespiratoryRate();
                    float breathReserve = data.getBreathReserveSeconds();
                    return respirations <= 0f && breathReserve <= 0f;
                }
            },

    TACHYPNOEA(ConditionSeverity.MILD, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float respirations = data.getActualRespiratoryRate();
                    return respirations > ModConstants.RESPIRATORY_HIGH && respirations <= ModConstants.RESPIRATORY_HYPERVENTILATION;
                }
            },

    SEVERE_TACHYPNOEA(ConditionSeverity.SEVERE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float respirations = data.getActualRespiratoryRate();
                    return respirations > ModConstants.RESPIRATORY_HYPERVENTILATION;
                }
            },

    HYPERVENTILATION(ConditionSeverity.MODERATE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float respirations = data.getActualRespiratoryRate();
                    float breathingUrge = data.getRespiratoryDrive();
                    return respirations > breathingUrge * 2;
                }
            },

    CHOKING(ConditionSeverity.MODERATE, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float respirations = data.getActualRespiratoryRate();
                    float breathingUrge = data.getRespiratoryDrive();
                    float breathReserve = data.getBreathReserveSeconds();
                    return respirations < (breathingUrge * 0.85f) && breathReserve <= PlayerHealthData.BREATH_RESERVE_MAX / 2;
                }
            },

    // === RESPIRATORY - OXYGENATION ===

    MILD_HYPOXIA(ConditionSeverity.NEUTRAL, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float spo2 = data.getOxygenSaturation();
                    return spo2 < ModConstants.SPO2_HYPOXIA && spo2 >= ModConstants.SPO2_SERIOUS;
                }
            },

    HYPOXIA(ConditionSeverity.SEVERE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float spo2 = data.getOxygenSaturation();
                    return spo2 < ModConstants.SPO2_SERIOUS && spo2 >= ModConstants.SPO2_CRITICAL;
                }
            },

    SEVERE_HYPOXIA(ConditionSeverity.CRITICAL, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float spo2 = data.getOxygenSaturation();
                    return spo2 < ModConstants.SPO2_CRITICAL && spo2 >= ModConstants.SPO2_FLOOR;
                }
            },

    OXYGEN_STARVATION(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float spo2 = data.getOxygenSaturation();
                    return spo2 < ModConstants.SPO2_FLOOR;
                }
            },

    HYPEROXIA(ConditionSeverity.MODERATE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float spo2 = data.getOxygenSaturation();
                    return spo2 > ModConstants.SPO2_HYPEROXIA;
                }
            },

    OXYGEN_POISONING(ConditionSeverity.SEVERE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float spo2 = data.getOxygenSaturation();
                    return spo2 > ModConstants.SPO2_OXYGEN_POISONING;
                }
            },

    // === BREATHING - LUNG COLLAPSE ===

    HEMOTHORAX(ConditionSeverity.SEVERE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bloodInLeftLung = data.getLeftLung().getBloodML();
                    float bloodInRightLung = data.getRightLung().getBloodML();
                    return bloodInLeftLung >= 10f || bloodInRightLung >= 10f;
                }
            },

    SERIOUS_HEMOTHORAX(ConditionSeverity.CRITICAL, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bloodInLeftLung = data.getLeftLung().getBloodML();
                    float bloodInRightLung = data.getRightLung().getBloodML();
                    return bloodInLeftLung >= 100f || bloodInRightLung >= 100f;
                }
            },

    PNEUMOTHORAX(ConditionSeverity.MODERATE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float airInLeftLung = data.getLeftLung().getAirML();
                    float airInRightLung = data.getRightLung().getAirML();
                    return airInLeftLung >= 100f || airInRightLung >= 100f;
                }
            },

    SERIOUS_PNEUMOTHORAX(ConditionSeverity.CRITICAL, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float airInLeftLung = data.getLeftLung().getAirML();
                    float airInRightLung = data.getRightLung().getAirML();
                    return airInLeftLung >= 500f || airInRightLung >= 500f;
                }
            },

    TENSION_PNEUMOTHORAX(ConditionSeverity.CRITICAL, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    boolean hasPneumoInLeftLung = data.getLeftLung().hasTensionPneumothorax();
                    boolean hasPneumoInRightLung = data.getRightLung().hasTensionPneumothorax();
                    return hasPneumoInLeftLung || hasPneumoInRightLung;
                }
            },

    LUNG_FLUID(ConditionSeverity.MODERATE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float fluidInLeftLung = data.getLeftLung().getFluidML();
                    float fluidInRightLung = data.getRightLung().getFluidML();
                    return fluidInLeftLung >= 100f || fluidInRightLung >= 100f;
                }
            },

    SERIOUS_LUNG_FLUID(ConditionSeverity.MODERATE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float fluidInLeftLung = data.getLeftLung().getFluidML();
                    float fluidInRightLung = data.getRightLung().getFluidML();
                    return fluidInLeftLung >= 500f || fluidInRightLung >= 500f;
                }
            },

    // === RESPIRATORY - AIRWAY ===

    COMPROMISED_AIRWAY(ConditionSeverity.MODERATE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    AirwayState airwayState = data.getAirwayState();
                    return airwayState == AirwayState.PARTIALLY_OBSTRUCTED;
                }
            },

    BLOCKED_AIRWAY(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    AirwayState airwayState = data.getAirwayState();
                    return airwayState == AirwayState.FULLY_OBSTRUCTED;
                }
            },

    // === TEMPERATURE ===

    MILD_HYPOTHERMIA(ConditionSeverity.MILD, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float coreTemp = data.getCoreTemperature();
                    return coreTemp < ModConstants.TEMP_HYPOTHERMIA && coreTemp >= 32f;
                }
            },

    HYPOTHERMIA(ConditionSeverity.SEVERE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float coreTemp = data.getCoreTemperature();
                    return coreTemp < 32f && coreTemp >= ModConstants.TEMP_SEVERE_HYPOTHERMIA;
                }
            },

    SEVERE_HYPOTHERMIA(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float coreTemp = data.getCoreTemperature();
                    return coreTemp < ModConstants.TEMP_SEVERE_HYPOTHERMIA;
                }
            },

    FEVER(ConditionSeverity.MILD, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float coreTemp = data.getCoreTemperature();
                    return coreTemp > ModConstants.TEMP_FEVER && coreTemp <= ModConstants.TEMP_HEAT_STROKE;
                }
            },

    HEAT_STROKE(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float coreTemp = data.getCoreTemperature();
                    return coreTemp >= ModConstants.TEMP_HEAT_STROKE;
                }
            },

    // === HYDRATION ===

    MILD_THIRST(ConditionSeverity.MILD, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float thirst = data.getThirst();
                    return thirst >= 0.2f && thirst < 0.4f;
                }
            },

    THIRST(ConditionSeverity.MODERATE, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float thirst = data.getThirst();
                    return thirst >= 0.4f && thirst < 0.7f;
                }
            },

    SEVERE_THIRST(ConditionSeverity.SEVERE, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float thirst = data.getThirst();
                    return thirst >= 0.7f;
                }
            },

    // === NUTRITION ===

    MILD_HUNGER(ConditionSeverity.MILD, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float hunger = data.getHunger();
                    return hunger >= 0.2f && hunger < 0.4f;
                }
            },

    HUNGER(ConditionSeverity.MODERATE, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float hunger = data.getHunger();
                    return hunger >= 0.4f && hunger < 0.7f;
                }
            },

    SEVERE_HUNGER(ConditionSeverity.SEVERE, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float hunger = data.getHunger();
                    return hunger >= 0.7f;
                }
            },

    EMACIATED(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float weight = data.getAdiposity();
                    return weight < 0.7 && weight > ModConstants.ADIPOSITY_EMACIATED;
                }
            },

    UNDERWEIGHT(ConditionSeverity.MODERATE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float weight = data.getAdiposity();
                    return weight < ModConstants.ADIPOSITY_UNDERWEIGHT && weight > ModConstants.ADIPOSITY_EMACIATED;
                }
            },

    OVERWEIGHT(ConditionSeverity.MILD, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float weight = data.getAdiposity();
                    return weight > ModConstants.ADIPOSITY_OVERWEIGHT && weight < ModConstants.ADIPOSITY_OBESE;
                }
            },

    OBESE(ConditionSeverity.MODERATE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float weight = data.getAdiposity();
                    return weight > ModConstants.ADIPOSITY_OBESE && weight < ModConstants.ADIPOSITY_MORBID;
                }
            },

    MORBIDLY_OBESE(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float weight = data.getAdiposity();
                    return weight > ModConstants.ADIPOSITY_OBESE;
                }
            },

    // === INFECTION ===

    INFECTION(ConditionSeverity.NEUTRAL, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float worstInfection = highestInfectionLevel(data);
                    return worstInfection >= 0.3f && worstInfection < 0.40f;
                }
            },

    PAINFUL_INFECTION(ConditionSeverity.MODERATE, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float worstInfection = highestInfectionLevel(data);
                    return worstInfection >= 0.40f && worstInfection < 0.60f;
                }
            },

    SEVERE_INFECTION(ConditionSeverity.SEVERE, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float worstInfection = highestInfectionLevel(data);
                    return worstInfection >= 0.60f && worstInfection < 0.80f;
                }
            },

    LIFE_THREATENING_INFECTION(ConditionSeverity.CRITICAL, ObservabilityLevel.VISIBLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float worstInfection = highestInfectionLevel(data);
                    float sepsisLevel = data.getSepticShock();
                    return worstInfection >= 0.80f && sepsisLevel >= 0.10f;
                }
            },

    BACTEREMIA(ConditionSeverity.MODERATE, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float bacteria = data.getBacteremia();
                    float sepsis = data.getSepticShock();
                    return bacteria >= 0.02f && sepsis <= 0f;
                }
            },

    SEPSIS(ConditionSeverity.SEVERE, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float sepsisLevel = data.getSepticShock();
                    return sepsisLevel > 0f && sepsisLevel < 0.50f;
                }
            },

    SEVERE_SEPSIS(ConditionSeverity.CRITICAL, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float sepsisLevel = data.getSepticShock();
                    return sepsisLevel >= 0.50f && sepsisLevel <= 0.80f;
                }
            },

    SEPTIC_SHOCK(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.PALPABLE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float sepsisLevel = data.getSepticShock();
                    return sepsisLevel >= 0.80f;
                }
            },

    // === IMMUNITY ===

    IMMUNOCOMPROMISED(ConditionSeverity.MILD, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float immunity = data.getImmunity();
                    return immunity < 0.50f;
                }
            },

    IMMUNE_EXHAUSTION(ConditionSeverity.SEVERE, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    float immuneReserves = data.getImmuneReserve();
                    float sepsis = data.getSepticShock();
                    return immuneReserves < ModConstants.IMMUNE_EXHAUSTION_MIN && sepsis <= 0f && hasAnyActiveInfection(data);
                }
            },

    // === SUBSTANCES - OPIATES ===
    // I'm actually a little afraid that CurseForge might content-block me
    // if i directly namedrop opiates, that's why i'm naming it "analgesia."

    ANALGESIA(ConditionSeverity.NEUTRAL, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    // True when opiates are in the bloodstream at therapeutic levels.
                    // TODO: Query activeSubstances for MORPHINE/KETAMINE
                    return false;
                }
            },

    DEEP_ANALGESIA(ConditionSeverity.MILD, ObservabilityLevel.SUBJECTIVE)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    // Opiate concentration above deep sedation threshold.
                    return false;
                }
            },

    ANALGESIC_TOXICITY(ConditionSeverity.CRITICAL_GLOW, ObservabilityLevel.DIAGNOSTIC)
            {
                @Override
                public boolean evaluate(PlayerHealthData data)
                {
                    // Opiate concentration above lethal threshold.
                    return false;
                }
            };

    // === STRUCTURE ===

    public final ConditionSeverity severity;
    public final ObservabilityLevel observability;
    Condition(ConditionSeverity severity, ObservabilityLevel observability)
    {
        this.severity = severity;
        this.observability = observability;
    }
    public abstract boolean evaluate(PlayerHealthData data);

    // === HELPER METHODS ===

    protected static float highestInfectionLevel(PlayerHealthData data)
    {
        float max = 0f;
        for (LimbData limb: data.getLimbs().values())
            for (Wound wound: limb.getWounds())
                if (wound.getInfectionLevel() > max)
                    max = wound.getInfectionLevel();

        return max;
    }

    protected static float computeExternalBleedRate(PlayerHealthData data)
    {
        float total = 0f;

        for (Map.Entry<LimbNode, LimbData> entry: data.getLimbs().entrySet())
            for (Wound wound: entry.getValue().getWounds())
                // Visceral wounds don't have visible bleeding, hence the guard.
                if (wound.getDepth() != WoundDepth.VISCERAL)
                    total += wound.getBleedRateML();

        return total;
    }

    protected static float computeVisceralBleedRate(PlayerHealthData data)
    {
        float total = 0f;

        for (LimbData limb: data.getLimbs().values())
            for (Wound wound: limb.getWounds())
                if (wound.getDepth() == WoundDepth.VISCERAL)
                    total += wound.getBleedRateML();

        return total;
    }

    protected static boolean hasAnyActiveInfection(PlayerHealthData data)
    {
        return highestInfectionLevel(data) > 0.05f;
    }

    protected static boolean hasAnyActiveWounds(PlayerHealthData data)
    {
        for (LimbData limb: data.getLimbs().values())
            if (limb.hasActiveWounds()) return true;

        return false;
    }

    protected static boolean hasBoneState(PlayerHealthData data, BoneState state)
    {
        for (LimbData limb: data.getLimbs().values())
            if (limb.getBoneState() == state)
                return true;

        return false;
    }
}
