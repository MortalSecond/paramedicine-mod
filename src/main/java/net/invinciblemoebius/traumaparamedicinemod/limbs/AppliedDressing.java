package net.invinciblemoebius.traumaparamedicinemod.limbs;

import net.invinciblemoebius.traumaparamedicinemod.wound.Dressing;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

// One dressing applied to a limb, covering one or more wounds by length.
public class AppliedDressing
{
    private final Dressing dressing;
    private final List<Integer> coveredWoundIds;

    public AppliedDressing(Dressing dressing, List<Integer> coveredWoundIds)
    {
        this.dressing = dressing;
        this.coveredWoundIds = coveredWoundIds;
    }

    // === ACESSORS ===

    public Dressing getDressing() { return dressing; }
    public List<Integer> getCoveredWoundIds() { return coveredWoundIds; }

    // === SAVING STUFF ===

    public void saveToNBT(CompoundTag tag)
    {
        CompoundTag dt = new CompoundTag();
        dressing.writeToNBT(dt);
        tag.put("Dressing", dt);

        int[] ids = new int[coveredWoundIds.size()];
        for (int i = 0; i < ids.length; i++)
            ids[i] = coveredWoundIds.get(i);

        tag.putIntArray("Covered", ids);
    }

    public static AppliedDressing loadFromNBT(CompoundTag tag)
    {
        Dressing d = Dressing.readFromNBT(tag.getCompound("Dressing"));
        List<Integer> ids = new ArrayList<>();
        for (int v : tag.getIntArray("Covered"))
            ids.add(v);
        return new AppliedDressing(d, ids);
    }

    public AppliedDressing copy()
    {
        CompoundTag t = new CompoundTag();
        saveToNBT(t);
        return loadFromNBT(t);
    }
}