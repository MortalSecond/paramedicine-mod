package net.invinciblemoebius.traumaparamedicinemod.substance;

import net.minecraft.nbt.CompoundTag;

// This represents a SOLID. All mechanism is in SubstanceStorage, this
// just sets the fluid's vocabulary. "mL", and the "Mixture" NBT key.
public class PowderMixture extends SubstanceStorage<PowderMixture>
{
    private static final String NBT_KEY = "Powder";

    @Override
    protected PowderMixture createEmpty() { return new PowderMixture(); }

    // === UI ===

    public String describe() { return describe("mg"); }

    // === ACESSORS ===

    public float totalMass() { return total(); }

    // === SAVING STUFF ===

    public void writeToNBT(CompoundTag tag)
    {
        writeComponents(tag, NBT_KEY);
    }

    public static FluidMixture readFromNBT(CompoundTag tag)
    {
        FluidMixture mix = new FluidMixture();
        mix.readComponents(tag, NBT_KEY);
        return mix;
    }
}
