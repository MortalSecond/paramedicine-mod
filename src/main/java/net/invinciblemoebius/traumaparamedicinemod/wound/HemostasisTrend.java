package net.invinciblemoebius.traumaparamedicinemod.wound;

public enum HemostasisTrend
{
    BLEEDING,   // No stable clot yet (or an arterial wound can't form one).
    CLOTTING,   // Clot forming, winning against the bleed.
    CLOTTED     // Sealed.
}
