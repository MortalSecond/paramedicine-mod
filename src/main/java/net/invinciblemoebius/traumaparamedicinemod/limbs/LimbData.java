package net.invinciblemoebius.traumaparamedicinemod.limbs;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.substance.CirculatingSubstance;
import net.invinciblemoebius.traumaparamedicinemod.wound.BleedContext;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LimbData
{
    // INTEGRITY
    // Soft tissue integrity. 0.0 = destroyed, 1.0 = healthy.
    private float muscleHealth = 1.0f;
    // Bone integrity. 0.0 = compound fracture, 1.0 = intact.
    private float boneHealth = 1.0f;
    private BoneState boneState = BoneState.INTACT;
    // CIRCULATION
    private boolean isCirculatingProximally = true;
    private boolean isCirculatingDistally = true;
    // Actual amount of blood inside this node's tissue (ml).
    // Now it's divided between plasma and RBCs, at a 45% RBC : 65% plasma ratio.
    private float plasmaVolume;
    private float redCellVolume;
    // Ideal healthy amount of blood for this node (ml).
    private final float restingBloodVolume;
    // Maximum amount of blood the node can take before hypervolemia kicks in (ml).
    private final float maxBloodVolume;
    private float perfusionRate;
    private float venousReturnRate;
    // LOCAL SUBSTANCES
    // Compartmentalized, they only become systemic once they reach the heart.
    private final List<CirculatingSubstance> localSubstances = new ArrayList<>();
    // WOUNDS
    private final List<Wound> wounds = new ArrayList<>();
    // PAIN
    private float rawPain = 0.0f;
    // Pain multiplier. 1.0 = Normal sensation, 0.0 = Anesthesized, >1.0 = Hyperalgesia.
    private float sensitivity = 1.0f;
    // SUMMARIES
    private float totalHealth = 1.0f;
    private float lastNetBleedRateML = 0f;
    private WoundType clientWorstWoundType = null;
    private int clientWoundCount = 0;
    private boolean syncNeeded = true;

    // === CONSTRUCTOR ===
    public LimbData(LimbNode node)
    {
        float resting = restingVolume(node);
        this.restingBloodVolume = resting;
        this.maxBloodVolume = resting * ModConstants.COMP_HYPERVOLEMIA_CEILING_MULT;
        this.plasmaVolume = resting * (1f - ModConstants.COMP_RESTING_HEMATOCRIT);
        this.redCellVolume = resting * ModConstants.COMP_RESTING_HEMATOCRIT;
        this.perfusionRate = resting * ModConstants.COMP_PERFUSION_RATE_FACTOR;
        this.venousReturnRate = resting * ModConstants.COMP_VENOUS_RETURN_FACTOR;
    }

    // Returns the resting and maximum blood volume values for each node.
    private static float restingVolume(LimbNode node)
    {
        return switch (node)
        {
            case UPPER_TORSO -> 1650f;
            case LOWER_TORSO -> 1100f;
            case GROIN -> 400f;
            case NECK -> 100f;
            case HEAD -> 350f;
            case LEFT_UPPER_ARM, RIGHT_UPPER_ARM -> 100f;
            case LEFT_FOREARM, RIGHT_FOREARM -> 70f;
            case LEFT_HAND, RIGHT_HAND -> 30f;
            case LEFT_UPPER_LEG, RIGHT_UPPER_LEG -> 300f;
            case LEFT_LOWER_LEG, RIGHT_LOWER_LEG -> 150f;
            case LEFT_FOOT, RIGHT_FOOT -> 50f;
        };
    }

    // === TICK METHODS ===

    public float computeNetBleedRate(LimbNode self, Map<LimbNode, LimbData> allLimbs, BleedContext ctx)
    {
        // Edge case for when there's no wounds.
        if (wounds.isEmpty())
            return 0f;
        if (!LimbTraversal.hasProximalCirculation(self, allLimbs))
            return 0f;

        float total = 0f;
        for (Wound wound: wounds)
            total += wound.computeBleedRate(ctx);

        return total;
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

    // === SUMMARY METHODS ===

    // Muscle Health = 50% Weight
    // Bone Health = 35% Weight
    // Circulation = 15% Weight
    public void recomputeTotalHealth(LimbNode self, Map<LimbNode, LimbData> allLimbs)
    {
        float circulationScore = LimbTraversal.hasProximalCirculation(self, allLimbs) ? 1.0f: 0.0f;

        this.totalHealth = (muscleHealth * 0.50f) + (boneHealth * 0.35f) + (circulationScore * 0.15f);

        markDirty();
    }

    public WoundType computeWorstWoundType()
    {
        WoundType worst = null;
        float worstScore = -1f;
        for (Wound wound: wounds)
        {
            float score = wound.getDepth().ordinal() + wound.getSize();
            if (score > worstScore)
            {
                worstScore = score;
                worst = wound.getType();
            }
        }

        return worst;
    }

    // Display value: EMA of the instantaneous (pulsatile) rate so moodles/drops don't
    // freak out between CATASTROPHIC BLEEDING and Dripping Blood.
    public void updateBleedDisplay(float instant)
    {
        float smoothed = lastNetBleedRateML + (instant - lastNetBleedRateML) * 0.1f;
        if (Math.abs(smoothed - lastNetBleedRateML) > 0.001f)
            markDirty();

        lastNetBleedRateML = smoothed;
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

    public void addLocalSubstance(CirculatingSubstance substance)
    {
        localSubstances.add(substance);
        markDirty();
    }

    // === ACCESSORS ===

    public float getMuscleHealth() {return muscleHealth;}
    public float getBoneHealth() {return boneHealth;}
    public BoneState getBoneState() {return boneState;}
    public boolean isCirculatingProximally() {return isCirculatingProximally;}
    public boolean isCirculatingDistally() {return isCirculatingDistally;}
    public float getActualBloodVolume() {return plasmaVolume + redCellVolume;}
    public float getPlasmaVolume() { return plasmaVolume; }
    public float getRedCellVolume() { return redCellVolume; }
    public float getHematocrit()
    {
        float total = plasmaVolume + redCellVolume;
        return total > 0f ? redCellVolume / total : 0f;
    }
    public float getVenousReturnRate() { return venousReturnRate; }
    public float getRestingBloodVolume() {return restingBloodVolume;}
    public float getMaxBloodVolume() {return maxBloodVolume;}
    public float getPerfusionRate() {return perfusionRate;}
    public float getRawPain() {return rawPain;}
    public float getEffectivePain() { return rawPain * sensitivity; }
    public float getSensitivity() {return sensitivity;}
    public float getTotalHealth() {return totalHealth;}
    public List<Wound> getWounds() { return wounds; }
    public boolean hasActiveWounds() { return !wounds.isEmpty(); }
    public float getLastNetBleedRateML()  { return lastNetBleedRateML; }
    public WoundType getClientWorstWoundType() { return clientWorstWoundType; }
    public int getClientWoundCount() { return clientWoundCount; }
    public List<CirculatingSubstance> getLocalSubstances() {return localSubstances;}

    public void setMuscleHealth(float v)
    {
        float c = clamp01(v);
        if (muscleHealth != c)
        {
            muscleHealth=c;
            markDirty();
        }
    }

    public void setBoneHealth(float v)
    {
        float c = clamp01(v);
        if (boneHealth != c)
        {
            boneHealth=c;
            markDirty();
        }
    }

    public void setBoneState(BoneState s)
    {
        if (boneState != s)
        {
            boneState=s;
            markDirty();
        }
    }

    public void setCirculatingProximally(boolean v)
    {
        if (isCirculatingProximally != v)
        {
            isCirculatingProximally=v;
            markDirty();
        }
    }

    public void setPlasmaVolume(float v)
    {
        float c = Math.max(0f, v);
        if (plasmaVolume != c)
        {
            plasmaVolume = c;
            markDirty();
        }
    }

    public void setRedCellVolume(float v)
    {
        float c = Math.max(0f, v);
        if (redCellVolume != c)
        {
            redCellVolume = c;
            markDirty();
        }
    }

    public void setActualBloodVolume(float v)
    {
        float target = Math.max(0f, Math.min(maxBloodVolume, v));
        float current = plasmaVolume + redCellVolume;
        if (current <= 0f)
        {
            plasmaVolume = target * (1f - ModConstants.COMP_RESTING_HEMATOCRIT);
            redCellVolume = target * ModConstants.COMP_RESTING_HEMATOCRIT;
        }
        else
        {
            float scale = target / current;
            plasmaVolume *= scale;
            redCellVolume *= scale;
        }
        markDirty();
    }

    public void setSensitivity(float v)
    {
        float c = Math.max(0f, Math.min(2.0f, v));
        if (sensitivity != c)
        {
            sensitivity=c;
            markDirty();
        }
    }

    public void setLastNetBleedRateML(float v)
    {
        if (lastNetBleedRateML != v)
        {
            lastNetBleedRateML = Math.max(0f, v);
            markDirty();
        }
    }

    public void setWoundsClientOnly(List<Wound> incoming)
    {
        wounds.clear();
        wounds.addAll(incoming);
    }

    public void setClientWoundSummaryOnly(WoundType worst, int count)
    {
        this.clientWorstWoundType = worst;
        this.clientWoundCount = count;
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
        tag.putFloat("PlasmaVolume", plasmaVolume);
        tag.putFloat("RedCellVolume", redCellVolume);
        tag.putFloat("VenousReturnRate", venousReturnRate);

        // Wounds serialized as a list of compound tags.
        ListTag woundList = new ListTag();
        for (Wound wound: wounds)
        {
            CompoundTag woundTag = new CompoundTag();
            wound.saveToNBT(woundTag);
            woundList.add(woundTag);
        }
        tag.put("Wounds", woundList);

        // Substances serialized as a list of compound tags as well.
        ListTag subList = new ListTag();
        for (CirculatingSubstance s : localSubstances)
        {
            CompoundTag st = new CompoundTag();
            s.saveToNBT(st);
            subList.add(st);
        }
        tag.put("LocalSubstances", subList);
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
        plasmaVolume = tag.getFloat("PlasmaVolume");
        redCellVolume = tag.getFloat("RedCellVolume");
        venousReturnRate = tag.getFloat("VenousReturnRate");

        wounds.clear();
        ListTag woundList = tag.getList("Wounds", Tag.TAG_COMPOUND);
        for (int i = 0; i < woundList.size(); i++)
        {
            Wound wound = new Wound();
            wound.loadFromNBT(woundList.getCompound(i));
            wounds.add(wound);
        }

        localSubstances.clear();
        ListTag subList = tag.getList("LocalSubstances", Tag.TAG_COMPOUND);
        for (int i = 0; i < subList.size(); i++)
        {
            CirculatingSubstance s = new CirculatingSubstance();
            s.loadFromNBT(subList.getCompound(i));
            localSubstances.add(s);
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
        this.plasmaVolume = other.plasmaVolume;
        this.redCellVolume = other.redCellVolume;
        this.venousReturnRate = other.venousReturnRate;

        this.wounds.clear();
        for (Wound w: other.wounds)
            this.wounds.add(w.copy());

        this.localSubstances.clear();
        for (CirculatingSubstance s : other.localSubstances)
        {
            CirculatingSubstance copy = new CirculatingSubstance();
            CompoundTag t = new CompoundTag();
            s.saveToNBT(t);
            copy.loadFromNBT(t);
            this.localSubstances.add(copy);
        }

        markDirty();
    }

    // === DEBUG ===

    public static float clamp01(float v)
    {
        return Math.max(0f, Math.min(1f, v));
    }

    @Override
    public String toString()
    {
        return String.format(
                "LimbData{muscle=%.2f, bone=%.2f(%s), blood=%.1fml, pain=%.2f*%.2f, health=%.2f, circ=[P:%b D:%b]}",
                muscleHealth, boneHealth, boneState, (plasmaVolume + redCellVolume), rawPain, sensitivity,
                totalHealth, isCirculatingProximally, isCirculatingDistally
        );
    }
}
