package net.invinciblemoebius.traumaparamedicinemod.limbs;

// Organized = EKG shows complexes.
// Perfusing = produces a pulse.
// Shockable = Defibrillation can help.
public enum CardiacRhythm
{
    SINUS_BRADYCARDIA(true, true, false),
    SINUS(true, true, false),
    SINUS_TACHYCARDIA(true, true, false),
    VENTRICULAR_TACHYCARDIA(false, false, true),
    VENTRICULAR_FIBRILLATION(false, false, true),
    PULSELESS_ELECTRICAL_ACTIVITY(true, false, false),
    ASYSTOLE(false, false, false);

    public final boolean organized;
    public final boolean perfusing;
    public final boolean shockable;

    CardiacRhythm(boolean organized, boolean perfusing, boolean shockable)
    {
        this.organized = organized;
        this.perfusing = perfusing;
        this.shockable = shockable;
    }
}
