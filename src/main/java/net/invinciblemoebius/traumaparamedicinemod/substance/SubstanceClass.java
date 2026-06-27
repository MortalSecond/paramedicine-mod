package net.invinciblemoebius.traumaparamedicinemod.substance;

// DRUG: acts by concentration, eliminated by half-life (pharmacology).
// CRYSTALLOID / BLOOD_PRODUCT: a fluid whose volume infuses into the bloodstream and stays as blood.
public enum SubstanceClass
{
    DRUG,
    CRYSTALLOID,
    BLOOD_PRODUCT;

    public boolean isVolumeExpander()
    {
        return this != DRUG;
    }
}