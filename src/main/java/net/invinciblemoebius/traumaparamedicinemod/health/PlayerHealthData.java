package net.invinciblemoebius.traumaparamedicinemod.health;
import net.minecraft.nbt.CompoundTag;

// Holds all the physiological data of a player.
// One instance per player entity.
public class PlayerHealthData
{
    // === BLOOD ===
    private float bloodVolume = 5.0f;
    public static final float BLOOD_MAX = 5.0f;
    public static final float BLOOD_MIN = 0.0f;

    // When any field changes, this indicates if the packet should be sent.
    // P.S. This idea was shamelessly stolen from Casualties: Cubed.
    private boolean syncNeeded = true;

    // === ACCESSORS ===

    public float getBloodVolume()
    {
        return bloodVolume;
    }

    public void setBloodVolume(float liters)
    {
        float clamped = Math.max(BLOOD_MIN, Math.min(BLOOD_MAX, liters));

        if(this.bloodVolume != clamped)
        {
            this.bloodVolume = clamped;
            this.syncNeeded = true;
        }
    }

    // === PACKET SYNC STUFF ===

    // Returns true and clears the flag.
    public boolean consumeSyncFlag()
    {
        if(!syncNeeded) return false;

        syncNeeded = false;
        return true;
    }

    public void markDirty()
    {
        syncNeeded = true;
    }

    // === DEBUG ===

    public float drainBlood(float liters)
    {
        // Edge case in case someone inputs negative values.
        if(liters <= 0)
            return 0;

        float before = bloodVolume;
        setBloodVolume(bloodVolume - liters);

        return before - bloodVolume;
    }

    public void addBlood(float liters)
    {
        if(liters > 0)
            setBloodVolume(bloodVolume + liters);
    }

    public void resetToDefaults()
    {
        bloodVolume = BLOOD_MAX;
        syncNeeded = true;
    }

    // === SAVING ===

    public void saveToNBT(CompoundTag tag)
    {
        tag.putFloat("BloodVolume", bloodVolume);
    }

    public void loadFromNBT(CompoundTag tag)
    {
        if(tag.contains("BloodVolume"))
        {
            bloodVolume = tag.getFloat("BloodVolume");
        }
    }

    // Copies all values from Other into this instance, to preserve
    // all the health data through death or dimension transfer.
    public void copyFrom(PlayerHealthData other)
    {
        this.bloodVolume = other.bloodVolume;
        this.syncNeeded = true;
    }

    @Override
    public String toString()
    {
        return String.format("PlayerHealthData{blood=%.3fL}", bloodVolume);
    }
}
