package net.invinciblemoebius.traumaparamedicinemod.wound;

// Ordered by progression.
public enum WoundStage
{
    FRESH,      // Hemostasis not yet achieved. Bleeding and/or clotting.
    INFLAMED,   // Immune response active, peak pain, highest infection risk window.
    SCABBING,   // New tissue; abrasions and blunt force can reopen the wound. Infection risk falling.
    SCARRING,   // Wound is 'closed', but tissue is still weaker than before.
    HEALED      // Full resolution.
}
