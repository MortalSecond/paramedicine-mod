package net.invinciblemoebius.traumaparamedicinemod.wound;

public enum DressingType
{
    RAG,            // High contamination risk. Needs changing every 10 minutes IRL time.
    BANDAGE,        // Clean fabric. Moderate absorption. Low infection risk if changed at least every 20mins.
    GAUZE,          // Sterile gauze. High absorption, low infection risk.
    HEMOSTATIC,     // Gauze with clotting agent, boosts clotting rate. For wounds that can't close on their own.
    OCCLUSIVE,      // Prevents air from getting into the chest cavity. Usable for sucking chest wounds.
    NONADHERENT;    // Dressing for burns. Removing it from a burn wound resets it to BLEEDING.

    public Dressing create()
    {
        return switch (this)
        {
            case RAG -> Dressing.builder()
                    .cleanliness(0.25f).adherence(0.5f).absorption(0.4f).pressure(0.3f).occlusion(0.1f).build();
            case BANDAGE -> Dressing.builder()
                    .cleanliness(0.6f).adherence(0.4f).absorption(0.5f).pressure(0.5f).occlusion(0.15f).build();
            case GAUZE -> Dressing.builder()
                    .cleanliness(0.9f).antiseptic(0.1f).adherence(0.6f).absorption(0.8f).pressure(0.3f).occlusion(0.1f).build();
            case HEMOSTATIC -> Dressing.builder()
                    .cleanliness(0.9f).hemostatic(0.9f).antiseptic(0.2f).adherence(0.7f).absorption(0.7f).pressure(0.4f).occlusion(0.2f).build();
            case OCCLUSIVE -> Dressing.builder()
                    .cleanliness(0.85f).occlusion(0.95f).adherence(0.5f).absorption(0.1f).pressure(0.4f).build();
            case NONADHERENT -> Dressing.builder()
                    .cleanliness(0.9f).antiseptic(0.15f).adherence(0.05f).absorption(0.5f).pressure(0.2f).occlusion(0.2f).build();
        };
        }
}
