package net.invinciblemoebius.traumaparamedicinemod.status;

public enum ObservabilityLevel
{
    SUBJECTIVE, // Only the patient is aware of the condition.
    VISIBLE,    // Condition is visible to anyone without tools.
    PALPABLE,   // Condition is visible after hands-on diagnosis like checking pulse.
    DIAGNOSTIC  // Condition is visible ONLY with tools.
}
