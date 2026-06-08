package net.invinciblemoebius.traumaparamedicinemod.status;

public enum ObservabilityLevel
{
    VISIBLE,        // Condition is visible to anyone without tools.
    PALPABLE,       // Condition is visible after hands-on diagnosis like checking pulse.
    DIAGNOSTIC,     // Condition is visible ONLY with tools.
    SUBJECTIVE      // Only the patient is aware of the condition.
}
