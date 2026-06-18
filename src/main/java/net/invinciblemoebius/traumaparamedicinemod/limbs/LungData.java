package net.invinciblemoebius.traumaparamedicinemod.limbs;

import net.minecraft.nbt.CompoundTag;

// This represents ONE lung.
public class LungData
{
    // The maximum volumes are NOT realistic, they're tuned primarily for gameplay feel.
    // AKA don't yell at me, i know that having both your legs' worth of blood in your lungs
    // is impossible, but it makes for nice emergent storytelling moments.

    // Max ml of blood before full hemothorax.
    public static final float MAX_BLOOD_ML = 1500f;
    // Max ml of air before full tension pneumothorax.
    public static final float MAX_AIR_ML = 2000f;
    // Max ml of fluid before full compromise. Smaller because this refers to fluid in the alveoli.
    public static final float MAX_FLUID_ML = 400f;
    // Blood in the plueral cavity.
    private float bloodML = 0f;
    // Air in the pleural cavity.
    private float airML = 0f;
    // Non-blood fluid in the alveoli.
    private float fluidML = 0f;
    // Whether this has air in the pleural space BUT no sucking chest wound.
    private boolean hasTensionPneumothorax = false;

    // SYNC STUFF
    private float clientCompromiseOverride = -1f; // -1 means "use computed value"
    public void setCompromiseClientOnly(float v) { clientCompromiseOverride = v; }

    // === DAMAGE COMPUTATION ===

    // Returns a 0.0 - 1.0 value of how damaged this lung is.
    public float getCompromise()
    {
        if (clientCompromiseOverride >= 0f) return clientCompromiseOverride;

        float bloodFraction = bloodML/MAX_BLOOD_ML;
        float airFraction = airML/MAX_AIR_ML;
        float fluidFraction = fluidML/MAX_FLUID_ML;

        return Math.min(1.0f, bloodFraction + airFraction + fluidFraction);
    }

    // === MODIFIERS ===

    public void addBlood(float ml)
    {
        bloodML = Math.max(0f, Math.min(MAX_BLOOD_ML, bloodML + ml));
    }

    public void addAir(float ml)
    {
        airML = Math.max(0f, Math.min(MAX_AIR_ML, airML + ml));
    }

    public void addFluid(float ml)
    {
        fluidML = Math.max(0f, Math.min(MAX_FLUID_ML, fluidML + ml));
    }

    // Chest drain. Returns mililiters actually removed.
    public float drainBlood(float ml)
    {
        float before = bloodML;
        bloodML = Math.max(0f, bloodML - ml);
        return before - bloodML;
    }

    // Needle decompression, my beloved. Returns mililiters actually vented.
    public float ventAir(float ml)
    {
        float before = airML;
        airML = Math.max(0f, airML - ml);

        // Small flag to remove tension pneumothorax status if all air is cleared.
        if (airML <= MAX_AIR_ML * 0.20f)
            hasTensionPneumothorax = false;

        return before - airML;
    }

    // Aspirated fluid clears on its own overtime.
    public void decayFluid(float mlPerSecond, float dt)
    {
        fluidML = Math.max(0f, fluidML - (mlPerSecond * dt));
    }

    public void setTensionPneumothorax(boolean value)
    {
        hasTensionPneumothorax = value;
    }

    // === ACCESSORS ===

    public float getBloodML() { return bloodML; }
    public float getAirML() { return airML; }
    public float getFluidML() { return fluidML; }
    public boolean hasTensionPneumothorax() { return hasTensionPneumothorax; }

    public boolean isHealthy()
    {
        return getCompromise() < 0.05f;
    }

    // === SAVING STUFF ===

    public void saveToNBT(CompoundTag tag)
    {
        tag.putFloat ("BloodML", bloodML);
        tag.putFloat ("AirML", airML);
        tag.putFloat ("FluidML", fluidML);
        tag.putBoolean ("TensionPneumothorax", hasTensionPneumothorax);
    }

    public void loadFromNBT(CompoundTag tag)
    {
        bloodML = tag.getFloat ("BloodML");
        airML = tag.getFloat ("AirML");
        fluidML = tag.getFloat ("FluidML");
        hasTensionPneumothorax = tag.getBoolean ("TensionPneumothorax");
    }

    public void copyFrom(LungData other)
    {
        this.bloodML = other.bloodML;
        this.airML = other.airML;
        this.fluidML = other.fluidML;
        this.hasTensionPneumothorax = other.hasTensionPneumothorax;
    }

    public void reset()
    {
        bloodML = 0f;
        airML = 0f;
        fluidML = 0f;
        hasTensionPneumothorax = false;
    }

    // === DEBUG ===
    @Override
    public String toString()
    {
        return String.format(
            "Lung{compromise=%.0f%% blood=%.0fml air=%.0fml fluid=%.0fml tension=%b}",
            getCompromise() * 100f, bloodML, airML, fluidML, hasTensionPneumothorax
        );
    }
}
