package net.invinciblemoebius.traumaparamedicinemod.limbs;

import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LimbData
{
    // === INTEGRITY ===
    // Soft tissue integrity. 0.0 = destroyed, 1.0 = healthy.
    private float muscleHealth = 1.0f;
    // Bone integrity. 0.0 = compound fracture, 1.0 = intact.
    private float boneHealth = 1.0f;
    private BoneState boneState = BoneState.INTACT;
    // === CIRCULATION ===
    private boolean isCirculatingProximally = true;
    private boolean isCirculatingDistally = true;
    // Actual amount of blood inside this node's tissue (ml).
    private float actualBloodVolume;
    // Ideal healthy amount of blood for this node (ml).
    private final float restingBloodVolume;
    // Maximum amount of blood the node can take before hypervolemia kicks in (ml).
    private final float maxBloodVolume;
    private float perfusionRate;
    // === WOUNDS ===
    private final List<Wound> wounds = new ArrayList<>();
    // === PAIN ===
    private float rawPain = 0.0f;
    // Pain multiplier. 1.0 = Normal sensation, 0.0 = Anesthesized, >1.0 = Hyperalgesia.
    private float sensitivity = 1.0f;
    // === DERIVED ===
    private float totalHealth = 1.0f;
    private boolean syncNeeded = true;

    // === CONSTRUCTOR ===
    public LimbData(LimbNode node)
    {
        float[] volumes = restingAndMaxVolumes(node);
        this.restingBloodVolume = volumes[0];
        this.maxBloodVolume = volumes[1];
        this.actualBloodVolume = volumes[0];
        this.perfusionRate = restingBloodVolume * 0.04f;
    }

    // Returns the resting and maximum blood volume values for each node.
    private static float[] restingAndMaxVolumes(LimbNode node)
    {
        return switch (node)
        {
            case UPPER_TORSO -> new float[]{1650f, 1950f};
            case LOWER_TORSO -> new float[]{1100f, 1400f};
            case GROIN -> new float[]{400f, 550f};
            case NECK -> new float[]{100f, 160f};
            case HEAD -> new float[]{350f, 420f};
            case LEFT_UPPER_ARM, RIGHT_UPPER_ARM -> new float[]{100f, 160f};
            case LEFT_FOREARM, RIGHT_FOREARM -> new float[]{70f, 120f};
            case LEFT_HAND, RIGHT_HAND -> new float[]{30f, 60f};
            case LEFT_UPPER_LEG, RIGHT_UPPER_LEG -> new float[]{300f, 450f};
            case LEFT_LOWER_LEG, RIGHT_LOWER_LEG -> new float[]{150f, 240f};
            case LEFT_FOOT, RIGHT_FOOT -> new float[]{50f, 90f};
        };
    }

    // === GRAPH NAVIGATOIN ===

    // Does this node currently receive oxygenated blood from the heart?
    // True ONLY if every node up to the upper chest has isCirculatingDistally = true,
    // AND if this node's own isCirculatingProximally is true.
    public boolean hasProximalCirculation(LimbNode self, Map<LimbNode, LimbData> allLimbs)
    {
        if (!isCirculatingProximally) return false;
        if (self.proximalNode == null) return true; // UPPER_TORSO always has central supply.

        LimbData proximalData = allLimbs.get(self.proximalNode);
        if (proximalData == null) return false;

        return proximalData.isCirculatingDistally() && proximalData.hasProximalCirculation(self.proximalNode, allLimbs);
    }

    // Does this node drain venous flow distally?
    public boolean hasDistalCirculation()
    {
        return isCirculatingDistally;
    }

    // === TICK METHODS ===

    public float computeNetBleedRate(LimbNode self, Map<LimbNode, LimbData> allLimbs, float coreTemp, float spo2, float nutritionLevel)
    {
        // Edge case for when there's no wounds.
        if (wounds.isEmpty()) return 0f;
        if (!hasProximalCirculation(self, allLimbs)) return 0f;

        float totalBleed = 0f;
        float totalClotting = 0f;

        for (Wound wound: wounds)
        {
            totalBleed += wound.getBleedRateML();
            totalClotting += wound.computeClottingRate(coreTemp, spo2, nutritionLevel);
        }

        return Math.max(0f, totalBleed - totalClotting);
    }

    public void recomputeRawPain()
    {
        float pain = 0f;
        float bonePain = 0f;

        // Wounds pain.
        for (Wound wound: wounds)
            pain += wound.getPainContribution();

        // Fracture pain.
        switch (boneState)
        {
            case INTACT -> bonePain = 0f;
            case HAIRLINE -> bonePain = 0.1f;
            case FRACTURED -> bonePain = 0.35f;
            case COMPOUND -> bonePain = 0.65f;
            case DISLOCATED -> bonePain = 0.45f;
        }
        pain += bonePain;

        // Ischemia pain.

        this.rawPain = Math.min(1.0f, pain);
        markDirty();
    }

    public float getEffectivePain()
    {
        return rawPain * sensitivity;
    }

    // === SUMMARY METHODS ===

    // Muscle Health = 50% Weight
    // Bone Health = 35% Weight
    // Circulation = 15% Weight
    public void recomputeTotalHealth(LimbNode self, Map<LimbNode, LimbData> allLimbs)
    {
        float circulationScore = hasProximalCirculation(self, allLimbs) ? 1.0f: 0.0f;

        this.totalHealth = (muscleHealth * 0.50f) + (boneHealth * 0.35f) + (circulationScore * 0.15f);

        markDirty();
    }

    // === WOUND MANAGEMENT ===

    public void addWound(Wound wound)
    {
        wounds.add(wound);
        markDirty();
    }

    public void removeWound(Wound wound)
    {
        wounds.remove(wound);
        markDirty();
    }

    public List<Wound> getWounds()
    {
        return wounds;
    }

    public boolean hasActiveWounds()
    {
        return !wounds.isEmpty();
    }

    // === ACCESSORS ===

    public float getMuscleHealth() {return muscleHealth;}
    public float getBoneHealth() {return boneHealth;}
    public BoneState getBoneState() {return boneState;}
    public boolean isCirculatingProximally() {return isCirculatingProximally;}
    public boolean isCirculatingDistally() {return isCirculatingDistally;}
    public float getActualBloodVolume() {return actualBloodVolume;}
    public float getRestingBloodVolume() {return restingBloodVolume;}
    public float getMaxBloodVolume() {return maxBloodVolume;}
    public float getPerfusionRate() {return perfusionRate;}
    public float getRawPain() {return rawPain;}
    public float getSensitivity() {return sensitivity;}
    public float getTotalHealth() {return totalHealth;}

    public void setMuscleHealth(float v)
    {
        float c = clamp01(v);
        if (muscleHealth != c){muscleHealth=c; markDirty();}
    }

    public void setBoneHealth(float v)
    {
        float c = clamp01(v);
        if (boneHealth != c){boneHealth=c; markDirty();}
    }

    public void setBoneState(BoneState s)
    {
        if (boneState != s){boneState=s; markDirty();}
    }

    public void setCirculatingProximally(boolean v)
    {
        if (isCirculatingProximally != v){isCirculatingProximally=v; markDirty();}
    }

    public void setCirculatingDistally(boolean v)
    {
        if (isCirculatingDistally != v){isCirculatingDistally=v; markDirty();}
    }

    public void setActualBloodVolume(float v)
    {
        float c = Math.max(0f, Math.min(maxBloodVolume, v));
        if (actualBloodVolume != c){actualBloodVolume=c; markDirty();}
    }

    public void setSensitivity(float v)
    {
        float c = Math.max(0f, Math.min(2.0f, v));
        if (sensitivity != c){sensitivity=c; markDirty();}
    }

    // === SYNC STUFF ===

    public boolean consumeSyncFlag()
    {
        if (!syncNeeded) return false;
        syncNeeded = false;
        return true;
    }

    public void markDirty()
    {
        syncNeeded = true;
    }

    // === SAVING STUFF ===

    public void saveToNBT(CompoundTag tag)
    {
        tag.putFloat("MuscleHealth", muscleHealth);
        tag.putFloat("boneHealth", boneHealth);
        tag.putString("BoneState", boneState.name());
        tag.putBoolean("CirculatingProximally", isCirculatingProximally);
        tag.putBoolean("CirculatingDistally", isCirculatingDistally);
        tag.putFloat("PerfusionRate", perfusionRate);
        tag.putFloat("RawPain", rawPain);
        tag.putFloat("Sensitivity", sensitivity);
        tag.putFloat("TotalHealth", totalHealth);
        tag.putFloat("ActualBloodVolume", actualBloodVolume);

        // Wounds serialized as a list of compound tags.
        ListTag woundList = new ListTag();
        for (Wound wound: wounds)
        {
            CompoundTag woundTag = new CompoundTag();
            wound.saveToNBT(woundTag);
            woundList.add(woundTag);
        }
        tag.put("Wounds", woundList);
    }

    public void loadFromNBT(CompoundTag tag)
    {
        muscleHealth = tag.getFloat("MuscleHealth");
        boneHealth = tag.getFloat("boneHealth");
        boneState = BoneState.valueOf(tag.getString("BoneState"));
        isCirculatingProximally = tag.getBoolean("CirculatingProximally");
        isCirculatingDistally = tag.getBoolean("CirculatingDistally");
        perfusionRate = tag.getFloat("PerfusionRate");
        rawPain = tag.getFloat("RawPain");
        sensitivity = tag.getFloat("Sensitivity");
        totalHealth = tag.getFloat("TotalHealth");
        actualBloodVolume = tag.getFloat("ActualBloodVolume");

        wounds.clear();
        ListTag woundList = tag.getList("Wounds", Tag.TAG_COMPOUND);
        for (int i = 0; i < woundList.size(); i++)
        {
            Wound wound = new Wound();
            wound.loadFromNBT(woundList.getCompound(i));
            wounds.add(wound);
        }
    }

    public void copyFrom(LimbData other)
    {
        this.muscleHealth = other.muscleHealth;
        this.boneHealth = other.boneHealth;
        this.boneState = other.boneState;
        this.isCirculatingProximally = other.isCirculatingProximally;
        this.isCirculatingDistally = other.isCirculatingDistally;
        this.perfusionRate = other.perfusionRate;
        this.rawPain = other.rawPain;
        this.sensitivity = other.sensitivity;
        this.totalHealth = other.totalHealth;
        this.actualBloodVolume = other.actualBloodVolume;

        this.wounds.clear();
        for (Wound w: other.wounds)
            this.wounds.add(w.copy());

        markDirty();
    }

    // === UTILITY ===

    public static float clamp01(float v)
    {
        return Math.max(0f, Math.min(1f, v));
    }

    @Override
    public String toString()
    {
        return String.format(
                "LimbData{muscle=%.2f, bone=%.2f(%s), blood=%.1fml, pain=%.2f*%.2f, health=%.2f, circ=[P:%b D:%b]}",
                muscleHealth, boneHealth, boneState, actualBloodVolume, rawPain, sensitivity,
                totalHealth, isCirculatingProximally, isCirculatingDistally
        );
    }
}
