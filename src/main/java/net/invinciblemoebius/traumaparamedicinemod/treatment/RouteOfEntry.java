package net.invinciblemoebius.traumaparamedicinemod.treatment;

// How a payload enters the body. Chosen at the administration event, NOT a property of the substance.
// The same drug behaves differently by route because each route crosses a different barrier.
// Saves me having to create duplicate SubstanceType files per route.
public enum RouteOfEntry
{
    IV("Inject IV"),       // Straight to blood, 100% bioavailable, no barrier.
    IM("Inject IM"),       // Stays in the local limb volume. Absorption is gated by that limb's perfusion.
    ORAL("Drink"),         // Into the gut. Gated by oralBioavailability and the gut's bacterial barrier.
    TOPICAL("Irrigate");   // Surface-acting on the wound; barely enters blood.

    public final String actionLabel;

    RouteOfEntry(String actionLabel) { this.actionLabel = actionLabel; }
}