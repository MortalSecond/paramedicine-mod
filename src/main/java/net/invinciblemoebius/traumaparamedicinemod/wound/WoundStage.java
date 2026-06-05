package net.invinciblemoebius.traumaparamedicinemod.wound;

// Ordered by progression.
public enum WoundStage
{
    BLEEDING,   // Active bleeding. Bleeding rate >0.
    CLOTTING,   // Bleeding rate dropping. Disturbance resets to BLEEDING.
    INFLAMED,   // Immune response active, highest infection risk window.
    SCABBING,   // New tissue; abrasions and blunt force can reopen the wound. Infection risk falling.
    SCARRING,   // Wound is 'closed', but tissue is still weaker than before.
    HEALED      // Full resolution.
}
