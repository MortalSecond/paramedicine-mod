package net.invinciblemoebius.traumaparamedicinemod.wound;

public enum WoundType
{
    LACERATION, // Slicing force. Clean edges, bleeds freely. Suturable.
    PUNCTURE,   // Penetrating force. Small entry, deep. High infectivity, can bleed internally.
    BLUNT,      // No skin breach. Causes muscular damage.
    BURN,       // Destroys tissue layers. Extreme infection risk.
    ABRASION,   // Surface scraping. Low bleed, high infectivity.
    AVULSION    // Tissue torn off. Can't be sutured, requires packing.
}
