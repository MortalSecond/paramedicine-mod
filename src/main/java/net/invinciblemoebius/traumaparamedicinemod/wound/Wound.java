package net.invinciblemoebius.traumaparamedicinemod.wound;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.minecraft.nbt.CompoundTag;

public class Wound
{
    // === WOUND STATE ===
    private WoundType type;
    private WoundDepth depth;
    // Size of the wound. 0.0 - Trivial, 1.0 - massive.
    private float size;
    private boolean isArterial;
    private float bleedRateML;
    private WoundStage stage = WoundStage.BLEEDING;
    private float stageProgress = 0f;
    private long ageTicks = 0L;
    private float contamination;
    private float infectionLevel;
    private boolean isEntry = false;
    private boolean isExit = false;

    // This represents the circumference of the limb. Goes from 0.0 to 2.0.
    // 0.0 = Anterior, 1.0 = Posterior. Thus, 0.5 = right, 1.5 = left, and 2.0 wraps back to anterior.
    private float woundPositionU = 0.0f;
    // V represents the vertical axis.
    // 0.0 = proximal end (e.g. the elbow), 1.0 = distal end (e.g. the wrist)
    private float woundPositionV = 0.5f;

    // === TREATMENT STATE ===
    private boolean hasDressing = false;
    private DressingType dressingType = DressingType.NONE;
    // Ticks since the current dressing was applied. Old dressing raises contamination; threshold varies by quality.
    private float dressingAgeTicks = 0f;
    // Whether wound edges have been closed (sutures, staples, or strips).
    // Closing an infected wound traps bacteria inside.
    private boolean isClosed = false;
    // Whether the wound cavity has been packed with gauze.
    private boolean isPacked = false;
    // Whether the wound has been cleaned with water.
    private boolean hasBeenIrrigated = false;
    private boolean hasAntiseptic = false;

    // === FOREIGN BODIES ===
    private boolean hasShrapnel = false;
    private boolean hasBullet = false;
    private boolean hasArrow = false;
    private boolean bleedingManaged = false;
    private float forcedBleedingRateML = 0f;

    // === CONSTRUCTORS ===

    // This one is set by loadFromNBT().
    public Wound(){}

    public Wound(WoundType type, WoundDepth depth, float size)
    {
        this.type = type;
        this.depth = depth;
        this.size = Math.max(0f, Math.min(1f, size));
        this.isArterial = (depth == WoundDepth.ARTERIAL);
        this.bleedRateML = computeInitialBleedRate();
        this.contamination = computeInitialContamination();
    }

    // === BLEED RATE COMPUTATION ===

    // Base Rates (ml/s at size 1.0):
    //  SUPERFICIAL:    0.05    (~1 ml/s)
    //  DERMAL:         0.15    (~3 ml/s)
    //  SUBDERMAL:      0.40    (~8 ml/s)
    //  MUSCULAR:       1.00    (~20 ml/s)
    //  ARTERIAL:       5.00    (~100 ml/s)
    //  VISCERAL:       0.60    (~12 ml/s)
    // BLUNT wounds at non-arterial depths have 30% of the base rates,
    // basically because they're bruising rather than hemorrhaging.
    // BURNS have 20% of the base rate, since it destroys tissue outright.
    private float computeInitialBleedRate()
    {
        float base = switch (depth)
        {
            case SUPERFICIAL -> 1.0f;
            case DERMAL -> 3.0f;
            case SUBDERMAL -> 8.0f;
            case MUSCULAR -> 20.0f;
            case ARTERIAL -> 80.0f;
            case VISCERAL -> 12.0f;
        };
        float typeModifier = switch (type)
        {
            case BLUNT -> 0.30f;
            case BURN -> 0.20f;
            default -> 1.00f;
        };

        if (bleedingManaged)
            return forcedBleedingRateML;
        else
            return base * typeModifier * size;
    }

    // Initial contamination of a wound at the moment of damage,
    // NOT to be confused with how contaminated a wound is.
    // PUNCTURES drag bacteria deep into the tissue.
    // ABRASIONS pick up ground bacteria.
    // BURNS destroy the skin so everything that touches it is a contamination vector, BUT the actual
    // immediate wound itself is pretty much sterile.
    private float computeInitialContamination()
    {
        return switch (type)
        {
            case PUNCTURE -> 0.30f + (depth.ordinal() * 0.05f);
            case ABRASION -> 0.25f;
            case AVULSION -> 0.20f;
            case LACERATION -> 0.10f;
            case BLUNT -> 0.05f;
            case BURN -> 0.00f; // Starts sterile because heat kills off the pre-existing bacteria.
        };
    }

    // === CLOTTING COMPUTATION ===

    // The rate at which a wound's bleeding rate is being reduced by clotting, in ml/s.
    public float computeClottingRate(float coreTemp, float spo2, float nutritionLevel, float systemicFactor)
    {
        if (isArterial)
            return 0f;
        if (depth == WoundDepth.VISCERAL)
            return 0f;
        if (stage != WoundStage.BLEEDING && stage != WoundStage.CLOTTING)
            return 0f;

        boolean hasHemostatic = (dressingType == DressingType.HEMOSTATIC);

        // Base clotting on a 1.0 size wound in a perfectly healthy patient.
        float base = 0.08f;
        float tempFactor = computeTempFactor(coreTemp);
        float spo2Factor = computeOxygenationFactor(spo2);
        float nutritionFactor = 0.6f + (nutritionLevel * 0.4f);
        float sizePenalty = 1.0f - (size * 0.55f);
        float dressingBonus = hasHemostatic ? 2.5f : 1.0f;

        return base * tempFactor * spo2Factor * nutritionFactor * sizePenalty * dressingBonus * systemicFactor;
    }

    // IRL clotting enzymes slow down below 35C°, hence why it's part of clotting calculation.
    private float computeTempFactor(float coreTemp)
    {
        if (coreTemp >= ModConstants.TEMP_HYPOTHERMIA) return 1.0f;
        if (coreTemp <= ModConstants.TEMP_SEVERE_HYPOTHERMIA) return 0.0f;

        // Linear falloff
        return (coreTemp - 28.0f) / 7.0f;
    }

    // Hypoxia causes acidosis, which 'destroys' clotting proteins, hence the penalty.
    // Even though it's stored as 1.0, normal values are usually from 0.95 and above.
    // Below 0.85 SpO2, clotting is severely affected.
    private float computeOxygenationFactor(float spo2)
    {
        if (spo2 >= ModConstants.SPO2_HYPOXIA) return 1.0f;
        if (spo2 <= ModConstants.SPO2_FLOOR) return 0.0f;

        return (spo2 - ModConstants.SPO2_FLOOR) / (ModConstants.SPO2_HYPOXIA - ModConstants.SPO2_FLOOR);
    }

    // === PAIN CONTRIBUTION ===

    // How much pain this wound contributes to its node's rawPain, scaled by depth and scale.
    public float getPainContribution()
    {
        boolean isBurn = (type == WoundType.BURN);
        float depthScale = switch (depth)
        {
            case SUPERFICIAL -> 0.05f;
            case DERMAL -> 0.10f;
            case SUBDERMAL -> 0.20f;
            case MUSCULAR -> 0.40f;
            case ARTERIAL -> 0.70f;
            case VISCERAL -> 0.50f;
        };

        // Burns are disproportionately painful relative to their bleed rate.
        float typeScale = isBurn ? 1.5f : 1.0f;

        // Infection amplifies pain.
        float infectionScale = 1.0f + (infectionLevel * 0.8f);

        return Math.min(1.0f, depthScale * typeScale * size * infectionScale);
    }

    // === TICK ADVANCEMENT ===

    public boolean tickAdvance(float netClottingRate)
    {
        boolean changed = false;
        ageTicks++;

        // Dressing age.
        if (hasDressing)
        {
            dressingAgeTicks++;
            // Dressings accumulate contamination past their useful life.
            float changeInterval = dressingChangeIntervalTicks();
            if (dressingAgeTicks > changeInterval)
            {
                float overdue = (dressingAgeTicks - changeInterval) / changeInterval;
                float contaminationRise = 0.001f * overdue;
                contamination = Math.min(1.0f, contamination + contaminationRise);
                changed = true;
            }
        }

        // Stage advancement.
        changed |= tickStageProgress(netClottingRate);

        return changed;
    }

    public float infectionSuceptibility()
    {
        return switch (depth)
        {
            case SUPERFICIAL -> 0.10f;
            case DERMAL -> 0.25f;
            case SUBDERMAL -> 0.50f;
            case MUSCULAR -> 0.85f;
            case ARTERIAL -> 1.00f;
            case VISCERAL -> 0.00f;
        };
    }

    private boolean tickStageProgress(float netClottingRate)
    {
        boolean changed = false;

        switch (stage)
        {
            case BLEEDING ->
            {
                // Clotting rate moves the wound towards CLOTTING stage.
                if (netClottingRate > 0f && bleedRateML > 0f)
                {
                    float progressRate = (netClottingRate / bleedRateML) * 0.002f;
                    stageProgress = Math.min(1f, stageProgress + progressRate);
                    if (stageProgress > 1f) {advanceTo(WoundStage.CLOTTING);}
                    changed = true;
                }
            }
            case CLOTTING ->
            {
                stageProgress = Math.min(1f, stageProgress + 0.0005f);
                if (stageProgress > 1f) {advanceTo(WoundStage.INFLAMED);}
                changed = true;
            }
            case INFLAMED ->
            {
                // Inflammation resolves in roughly 1 in-game day.
                // Infection slows this down though.
                float rate = 1f / 24000f * (1f - (infectionLevel * 0.8f));
                stageProgress = Math.min(1f, stageProgress + rate);
                if (stageProgress > 1f) {advanceTo(WoundStage.SCABBING);}
                changed = true;
            }
            case SCABBING ->
            {
                // Scabbing resolves after ~2 in-game days.
                float rate = 1f / 48000f;
                stageProgress = Math.min(1f, stageProgress + rate);
                if (stageProgress > 1f) {advanceTo(WoundStage.SCARRING);}
                changed = true;
            }
            case SCARRING ->
            {
                // Scarring resolves after ~5 in-game days.
                float rate = 1f / 120000f;
                stageProgress = Math.min(1f, stageProgress + rate);
                if (stageProgress > 1f) {advanceTo(WoundStage.HEALED);}
                changed = true;
            }
            case HEALED -> { /* Terminal stage, no advancement. */ }
        }

        return changed;
    }

    private void advanceTo(WoundStage next)
    {
        stage = next;
        stageProgress = 0f;
    }

    // How many ticks before a dressing of this type is overdue.
    private float dressingChangeIntervalTicks()
    {
        return switch (dressingType)
        {
            case RAG -> 6_000f;
            case BANDAGE -> 12_000f;
            case GAUZE -> 24_000f;
            case HEMOSTATIC -> 24_000f;
            case OCCLUSIVE -> 36_000f;
            case NONADHERENT -> 18_000f;
            case NONE -> Float.MAX_VALUE;
        };
    }

    // === TREATMENT ACTIONS ===

    public void applyDressing(DressingType type)
    {
        this.hasDressing = true;
        this.dressingType = type;
        this.dressingAgeTicks = 0f;
    }

    public void removeDressing()
    {
        this.hasDressing = false;
        this.dressingType = DressingType.NONE;
        this.dressingAgeTicks = 0f;
    }

    public void irrigate()
    {
        contamination = Math.max(0f, contamination - 0.60f);
        hasBeenIrrigated = true;
    }

    public void applyAntiseptic()
    {
        hasAntiseptic = true;
        contamination = Math.max(0f, contamination - 0.20f);
    }

    public void applyPacking()
    {
        isPacked = true;
        bleedRateML *= 0.20f;
    }

    public void close()
    {
        isClosed = true;
        bleedRateML = 0f;
    }

    public void reopen()
    {
        isClosed = false;
        isPacked = false;
        stage = WoundStage.BLEEDING;
        stageProgress = 0f;
        bleedRateML = computeInitialBleedRate() * 0.60f;
    }

    public void removeForeignBody()
    {
        hasArrow = false;
        hasShrapnel = false;
        hasBullet = false;

        bleedingManaged = false;
        bleedRateML = computeInitialBleedRate();
    }

    // === ACCESSORS ===

    public WoundType getType() { return type; }
    public WoundDepth getDepth() { return depth; }
    public float getSize() { return size; }
    public boolean isArterial() { return isArterial; }
    public float getBleedRateML() { return bleedRateML; }
    public WoundStage getStage() { return stage; }
    public float getStageProgress() { return stageProgress; }
    public float getContamination() { return contamination; }
    public float getInfectionLevel() { return infectionLevel; }
    public boolean hasDressing() { return hasDressing; }
    public DressingType getDressingType() { return dressingType; }
    public boolean isClosed() { return isClosed; }
    public boolean isPacked() { return isPacked; }
    public boolean hasBeenIrrigated() { return hasBeenIrrigated; }
    public boolean hasShrapnel() { return hasShrapnel; }
    public boolean hasBullet() { return hasBullet; }
    public boolean hasArrow() { return hasArrow; }
    public boolean isBleedingManaged() { return bleedingManaged; }
    public float getForcedBleedingRateML() { return forcedBleedingRateML; }
    public boolean isEntry() { return isEntry; }
    public boolean isExit() { return isExit; }
    public float getWoundPositionU() { return woundPositionU; }
    public float getWoundPositionV() { return woundPositionV; }
    public boolean isRightSide() { return woundPositionU > 1.0f; }

    public void setHasShrapnel(boolean v) { hasShrapnel = v; }
    public void setHasBullet(boolean v) { hasBullet = v; }
    public void setHasArrow(boolean v) { hasArrow = v; }
    public void setBleedRateML(float v) { bleedRateML = Math.max(0f, v); }
    public void setContamination(float v) { contamination = Math.max(0f, Math.min(1f, v)); }
    public void setBleedingManaged(boolean v) { bleedingManaged = v; }
    public void setForcedBleedingRateML(float v) { forcedBleedingRateML = v; }
    public void setIsEntry(boolean v) { this.isEntry = v; }
    public void setIsExit(boolean v) { this.isExit = v; }
    public void setWoundPositionU(float v) { this.woundPositionU = v; }
    public void setWoundPositionV(float v) { this.woundPositionV = v; }
    public void setInfectionLevel(float v) { infectionLevel = Math.max(0f, Math.min(1f, v)); }

    // === SAVING STUFF ===

    public void saveToNBT(CompoundTag tag)
    {
        tag.putString("Type", type.name());
        tag.putString("Depth", depth.name());
        tag.putFloat ("Size", size);
        tag.putBoolean("IsArterial", isArterial);
        tag.putFloat ("BleedRateML", bleedRateML);
        tag.putString("Stage", stage.name());
        tag.putFloat ("StageProgress", stageProgress);
        tag.putLong  ("AgeTicks", ageTicks);
        tag.putFloat ("Contamination", contamination);
        tag.putFloat ("InfectionLevel", infectionLevel);
        tag.putBoolean("HasDressing", hasDressing);
        tag.putString("DressingType", dressingType.name());
        tag.putFloat ("DressingAgeTicks", dressingAgeTicks);
        tag.putBoolean("IsClosed", isClosed);
        tag.putBoolean("IsPacked", isPacked);
        tag.putBoolean("Irrigated", hasBeenIrrigated);
        tag.putBoolean("HasAntiseptic", hasAntiseptic);
        tag.putBoolean("HasShrapnel", hasShrapnel);
        tag.putBoolean("HasBullet", hasBullet);
        tag.putBoolean("HasArrow", hasArrow);
        tag.putBoolean("isManagedBleeding", bleedingManaged);
        tag.putFloat("forcedBleedingRate", forcedBleedingRateML);
        tag.putBoolean("isEntry", isEntry);
        tag.putBoolean("isExit", isExit);
        tag.putFloat("woundPositionU", woundPositionU);
        tag.putFloat("woundPositionV", woundPositionV);
    }

    public void loadFromNBT(CompoundTag tag)
    {
        type = WoundType.valueOf(tag.getString("Type"));
        depth = WoundDepth.valueOf(tag.getString("Depth"));
        size = tag.getFloat("Size");
        isArterial = tag.getBoolean("IsArterial");
        bleedRateML = tag.getFloat("BleedRateML");
        stage = WoundStage.valueOf(tag.getString("Stage"));
        stageProgress = tag.getFloat("StageProgress");
        ageTicks = tag.getLong("AgeTicks");
        contamination = tag.getFloat("Contamination");
        infectionLevel = tag.getFloat("InfectionLevel");
        hasDressing = tag.getBoolean("HasDressing");
        dressingType = DressingType.valueOf(tag.getString("DressingType"));
        dressingAgeTicks = tag.getFloat("DressingAgeTicks");
        isClosed = tag.getBoolean("IsClosed");
        isPacked = tag.getBoolean("IsPacked");
        hasBeenIrrigated = tag.getBoolean("Irrigated");
        hasAntiseptic = tag.getBoolean("HasAntiseptic");
        hasShrapnel = tag.getBoolean("HasShrapnel");
        hasBullet = tag.getBoolean("HasBullet");
        hasArrow = tag.getBoolean("HasArrow");
        bleedingManaged = tag.getBoolean("isManagedBleeding");
        forcedBleedingRateML = tag.getFloat("forcedBleedingRate");
        isEntry = tag.getBoolean("isEntry");
        isExit = tag.getBoolean("isExit");
        woundPositionU = tag.getFloat("woundPositionU");
        woundPositionV = tag.getFloat("woundPositionV");
    }

    public Wound copy()
    {
        Wound w = new Wound();
        CompoundTag tag = new CompoundTag();
        this.saveToNBT(tag);
        w.loadFromNBT(tag);
        return w;
    }

    // === DEBUG ===

    @Override
    public String toString()
    {
        return String.format(
                "Wound{%s %s size=%.2f art=%b bleed=%.3fml/t stage=%s(%.0f%%) inf=%.2f cont=%.2f dressed=%s}",
                type, depth, size, isArterial, bleedRateML, stage, stageProgress * 100f,
                infectionLevel, contamination, dressingType
        );
    }
}
