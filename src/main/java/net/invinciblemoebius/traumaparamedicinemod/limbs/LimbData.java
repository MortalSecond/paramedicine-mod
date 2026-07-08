package net.invinciblemoebius.traumaparamedicinemod.limbs;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.substance.CirculatingSubstance;
import net.invinciblemoebius.traumaparamedicinemod.wound.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;

import java.util.*;

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
    private int nextWoundId = 0;
    private final List<Wound> wounds = new ArrayList<>();
    private final List<AppliedDressing> dressings = new ArrayList<>();
    // PAIN
    private float rawPain = 0.0f;
    // Pain multiplier. 1.0 = Normal sensation, 0.0 = Anesthesized, >1.0 = Hyperalgesia.
    private float sensitivity = 1.0f;
    // Local anesthetic in this limb's depot.
    private float localAnalgesia = 0f;
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
            total += wound.computeBleedRate(ctx) * dressingBleedFactor(wound);

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

    // === DRESSINGS MANAGEMENT ===

    // Applies a dressing to the node, covering undressed wounds worst-first until its length runs out.
    // Returns true if it covered at least one wound.
    public boolean applyDressing(Dressing snapshot)
    {
        Set<Integer> alreadyCovered = new HashSet<>();
        for (AppliedDressing appliedDressing : dressings)
            alreadyCovered.addAll(appliedDressing.getCoveredWoundIds());

        List<Wound> candidates = new ArrayList<>();
        for (Wound wound : wounds)
            if (!alreadyCovered.contains(wound.getId()))
                candidates.add(wound);
        candidates.sort((a, b) -> Float.compare(severityScore(b), severityScore(a)));

        Dressing dressing = snapshot.copy();
        float remaining = dressing.getLength();
        List<Integer> covered = new ArrayList<>();
        for (Wound wound : candidates)
        {
            if (remaining <= 0f)
                break;
            covered.add(wound.getId());
            remaining -= wound.getSize();

            // A dirty dressing drags contamination toward (1 - cleanliness).
            float floor = 1f - dressing.getCleanliness();
            if (wound.getContamination() < floor)
                wound.setContamination(wound.getContamination() + (floor - wound.getContamination()) * ModConstants.DRESSING_DIRTY_APPLY_FRACTION);
        }

        if (covered.isEmpty())
            return false;

        dressings.add(new AppliedDressing(dressing, covered));
        markDirty();
        return true;
    }

    // Removes the dressing covering the given wound. Adhered dressings can rip healing wounds open.
    // Returns true if anything reopened.
    public boolean removeDressingCovering(int woundId, RandomSource rand)
    {
        Iterator<AppliedDressing> iterator = dressings.iterator();
        while (iterator.hasNext())
        {
            AppliedDressing appliedDressing = iterator.next();
            if (!appliedDressing.getCoveredWoundIds().contains(woundId))
                continue;

            boolean ripped = false;
            float adherence = appliedDressing.getDressing().getAdherence();
            for (int id : appliedDressing.getCoveredWoundIds())
            {
                Wound wound = woundById(id);
                if (wound == null)
                    continue;

                boolean healing = wound.getStage() == WoundStage.INFLAMED || wound.getStage() == WoundStage.SCABBING || wound.getStage() == WoundStage.SCARRING;
                if (adherence > 0f && healing && rand.nextFloat() < adherence * ModConstants.DRESSING_ADHERENCE_REOPEN_CHANCE)
                {
                    wound.reopen();
                    ripped = true;
                }
            }
            iterator.remove();
            markDirty();
            return ripped;
        }
        return false;
    }

    public Dressing coveringDressing(int woundId)
    {
        for (AppliedDressing appliedDressing : dressings)
            if (appliedDressing.getCoveredWoundIds().contains(woundId))
                return appliedDressing.getDressing();

        return null;
    }

    // Bleed multiplier a wound gets from its covering dressing (1.0 = uncovered).
    public float dressingBleedFactor(Wound wound)
    {
        Dressing d = coveringDressing(wound.getId());
        return (d != null) ? d.pressureBleedFactor(wound.isArterial()) : 1f;
    }

    // Clot multiplier a wound gets from its covering dressing's hemostatic agent (1.0 = uncovered).
    public float dressingClotMult(Wound wound)
    {
        Dressing dressing = coveringDressing(wound.getId());
        return (dressing != null) ? (1f + dressing.getHemostatic() * ModConstants.DRESSING_HEMOSTATIC_CLOT_MULT) : 1f;
    }

    // Wear, fouling, antiseptic, and anaerobic occlusion, per dressing across its covered wounds.
    // Prunes dead wound IDs and drops dressings that no longer cover anything.
    public void tickDressings(float dt)
    {
        if (dressings.isEmpty())
            return;

        boolean changed = false;
        Iterator<AppliedDressing> iterator = dressings.iterator();
        while (iterator.hasNext())
        {
            AppliedDressing appliedDressing = iterator.next();

            List<Wound> covered = new ArrayList<>();
            Iterator<Integer> idIterator = appliedDressing.getCoveredWoundIds().iterator();
            while (idIterator.hasNext())
            {
                Wound wound = woundById(idIterator.next());
                if (wound == null) idIterator.remove();
                else covered.add(wound);
            }
            if (covered.isEmpty())
            {
                iterator.remove();
                changed = true;
                continue;
            }

            Dressing dressing = appliedDressing.getDressing();
            float sumBleed = 0f;
            for (Wound coveredWound : covered)
                sumBleed += coveredWound.getBleedRateML();
            dressing.tickWear(sumBleed, dt);

            boolean overdue = dressing.isOverdue();
            float antiseptic = dressing.getAntiseptic();
            boolean occlusive = dressing.getOcclusion() > 0.5f;
            for (Wound coveredWound : covered)
            {
                if (overdue)
                {
                    coveredWound.setContamination(coveredWound.getContamination() + ModConstants.DRESSING_FOUL_CONTAM_RISE);
                    changed = true;
                }
                if (antiseptic > 0f && coveredWound.getContamination() > 0f)
                {
                    coveredWound.setContamination(coveredWound.getContamination() - antiseptic * ModConstants.DRESSING_ANTISEPTIC_DECONTAM_PER_SECOND * dt);
                    changed = true;
                }
                if (occlusive && coveredWound.getContamination() > 0.3f)
                {
                    coveredWound.setContamination(coveredWound.getContamination() + ModConstants.DRESSING_OCCLUSION_ANAEROBIC_RISE);
                    changed = true;
                }
            }
        }

        if (changed)
            markDirty();
    }

    private Wound woundById(int id)
    {
        for (Wound wound : wounds)
            if (wound.getId() == id)
                return wound;

        return null;
    }

    private static float severityScore(Wound wound)
    {
        return wound.getDepth().ordinal() + wound.getSize();
    }

    // === WOUND MANAGEMENT ===

    public void addWound(Wound wound)
    {
        wound.setId(nextWoundId++);
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
    public float getEffectivePain() { return rawPain * sensitivity * (1f - localAnalgesia); }
    public float getSensitivity() {return sensitivity;}
    public float getTotalHealth() {return totalHealth;}
    public List<Wound> getWounds() { return wounds; }
    public boolean hasActiveWounds() { return !wounds.isEmpty(); }
    public float getLastNetBleedRateML()  { return lastNetBleedRateML; }
    public WoundType getClientWorstWoundType() { return clientWorstWoundType; }
    public int getClientWoundCount() { return clientWoundCount; }
    public void resetPainTransient() { localAnalgesia = 0f; }
    public void addLocalAnalgesia(float v) { localAnalgesia = Math.min(0.95f, localAnalgesia + Math.max(0f, v)); }
    public List<CirculatingSubstance> getLocalSubstances() {return localSubstances;}
    public List<AppliedDressing> getDressings() { return dressings; }
    public void setDressingsClientOnly(List<AppliedDressing> incoming) { dressings.clear(); dressings.addAll(incoming); }

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
        tag.putInt("NextWoundId", nextWoundId);
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
        for (CirculatingSubstance substance : localSubstances)
        {
            CompoundTag st = new CompoundTag();
            substance.saveToNBT(st);
            subList.add(st);
        }
        tag.put("LocalSubstances", subList);

        // Dressings, compound tag, and yadda yadda.
        ListTag dressingList = new ListTag();
        for (AppliedDressing appliedDressing : dressings)
        {
            CompoundTag dt = new CompoundTag();
            appliedDressing.saveToNBT(dt);
            dressingList.add(dt);
        }
        tag.put("Dressings", dressingList);
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
        nextWoundId = tag.getInt("NextWoundId");

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
            CirculatingSubstance substance = new CirculatingSubstance();
            substance.loadFromNBT(subList.getCompound(i));
            localSubstances.add(substance);
        }

        dressings.clear();
        ListTag dressingList = tag.getList("Dressings", Tag.TAG_COMPOUND);
        for (int i = 0; i < dressingList.size(); i++)
            dressings.add(AppliedDressing.loadFromNBT(dressingList.getCompound(i)));
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
        this.nextWoundId = other.nextWoundId;

        this.wounds.clear();
        for (Wound wound: other.wounds)
            this.wounds.add(wound.copy());

        this.localSubstances.clear();
        for (CirculatingSubstance substance : other.localSubstances)
        {
            CirculatingSubstance copy = new CirculatingSubstance();
            CompoundTag t = new CompoundTag();
            substance.saveToNBT(t);
            copy.loadFromNBT(t);
            this.localSubstances.add(copy);
        }

        this.dressings.clear();
        for (AppliedDressing appliedDressing : other.dressings)
            this.dressings.add(appliedDressing.copy());

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
