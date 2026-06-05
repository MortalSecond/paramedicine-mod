package net.invinciblemoebius.traumaparamedicinemod.wound;

public enum DressingType
{
    NONE,
    RAG,        // High contamination risk. Needs changing every 10 minutes IRL time.
    BANDAGE,    // Clean fabric. Moderate absorption. Low infection risk if changed at least every 20mins.
    GAUZE,      // Sterile gauze. High absorption, low infection risk.
    HEMOSTATIC, // Gauze with clotting agent, boosts clotting rate. For wounds that can't close on their own.
    OCCLUSIVE,  // Prevents air from getting into the chest cavity. Usable for sucking chest wounds.
    NONADHERENT // Dressing for burns. Removing it from a burn wound resets it to BLEEDING.
}
