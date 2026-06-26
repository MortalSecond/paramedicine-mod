package net.invinciblemoebius.traumaparamedicinemod.wound;

// Circulatory context given to most wounds, so the Wound file doesn't have
// to reach to PlayerHealthData to do its calculations.
public record BleedContext(float map, float systolic, float diastolic, float cardiacPhase, boolean hasPulse, float coreTemp, float spo2, float nutrition, float systemicClottingFactor)
{
}
