package net.invinciblemoebius.traumaparamedicinemod.wound;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public class Wound
{
    // === WOUND STATE ===
    private WoundType type;
    private WoundDepth depth;
    // Size of the wound. 0.0 - Trivial, 1.0 - massive.
    private float size;
    private boolean isArterial;
    private float bleedRateML;
    private WoundStage stage = WoundStage.FRESH;
    private float stageProgress = 0f;
    // 0 = open, 1 = sealed.
    private float clotIntegrity = 0f;
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
    // Snapshot of the applied dressing, or null when undressed.
    private Dressing dressing = null;
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

    private float computeInitialBleedRate()
    {
        float base = switch (depth)
        {
            case SUPERFICIAL -> 1.0f;
            case DERMAL -> 2.5f;
            case SUBDERMAL -> 5.0f;
            case MUSCULAR -> 10.0f;
            case ARTERIAL -> 18.0f;
            case VISCERAL -> 10.0f;
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
            case BURN -> 0.00f;
        };
    }

    // Bleed rate (ml/s) under current circulatory conditions.
    public float computeBleedRate(BleedContext ctx)
    {
        if (isClosed)
            return 0f;

        float raw = bleedRateML * pressureFactor(ctx) * (1f - clotIntegrity) * spasmFactor();
        return (dressing != null) ? raw * dressing.pressureBleedFactor(isArterial) : raw;
    }

    private float pressureFactor(BleedContext ctx)
    {
        // Pulsatile. Bleeds during systolic, slows during diastolic, stops at arrest.
        if (isArterial)
        {
            float envelope = arterialSpurtEnvelope(ctx.cardiacPhase());
            float instantP = ctx.hasPulse() ? ctx.diastolic() + (ctx.systolic() - ctx.diastolic()) * envelope : 0f;

            return Math.max(0f, instantP / ModConstants.NORMAL_MAP);
        }

        // Venous/Capillary. Continuous, driven by perfusion pressure.
        return Math.max(0f, ctx.map() / ModConstants.NORMAL_MAP);
    }

    // Peaks at systole (phase 0), slows during diastole. It uses ^2 to read as a spurt, not a sine.
    private static float arterialSpurtEnvelope(float phase)
    {
        float c = (float) ((1.0 + Math.cos(2.0 * Math.PI * phase)) * 0.5);
        return c * c;
    }

    // Vascular spasm. The vein (or artery) clamps in the first 30 seconds, strongest for small vessels.
    // This is the "slows then resumes" behavior, and the early breather
    // that lets small wounds clot before flow returns.
    private float spasmFactor()
    {
        float strength = switch (depth)
        {
            case SUPERFICIAL, DERMAL -> 0.60f;
            case SUBDERMAL -> 0.40f;
            case MUSCULAR -> 0.25f;
            case VISCERAL -> 0.20f;
            case ARTERIAL -> 0.10f;
        };
        float ageSeconds = ageTicks / 20f;

        return 1f - strength * (float) Math.exp(-ageSeconds / 30.0);
    }

    // === CLOTTING COMPUTATION ===

    // Derived bleed/clot readout from stored clot integrity (works client-side too).
    public HemostasisTrend hemostasisTrend()
    {
        if (isClosed || clotIntegrity >= 0.90f)
            return HemostasisTrend.CLOTTED;
        if (clotIntegrity >= 0.15f)
            return HemostasisTrend.CLOTTING;

        return HemostasisTrend.BLEEDING;
    }

    public void tickClotting(BleedContext ctx, float dt)
    {
        // Edge case in case the wound is either already closed or visceral (can't clot).
        if (isClosed || clotIntegrity >= 1f || depth == WoundDepth.VISCERAL)
            return;

        // An arterial wound can't seal while it's still flowing hard.
        float cap = (isArterial && computeBleedRate(ctx) > ModConstants.ARTERIAL_FLOW_CEILING) ? ModConstants.ARTERIAL_CLOT_CAP : 1f;
        if (clotIntegrity >= cap)
            return;

        clotIntegrity = Math.min(cap, clotIntegrity + clotGrowthRate(ctx) * dt);
    }

    private float clotGrowthRate(BleedContext ctx)
    {
        // Roughly 50secs to seal a small clean wound in a healthy patient
        float base = 0.02f;
        float nutrition = 0.6f + (ctx.nutrition() * 0.4f);
        float sizePenalty = 1.0f - (size * 0.55f);
        float dressingBonus = (dressing != null) ? (1f + dressing.getHemostatic() * ModConstants.DRESSING_HEMOSTATIC_CLOT_MULT) : 1f;

        return base * computeTempFactor(ctx.coreTemp()) * computeOxygenationFactor(ctx.spo2()) * nutrition * sizePenalty * dressingBonus * ctx.systemicClottingFactor();
    }

    // IRL clotting enzymes slow down below 35C°, hence why it's part of clotting calculation.
    private float computeTempFactor(float coreTemp)
    {
        if (coreTemp >= ModConstants.TEMP_HYPOTHERMIA)
            return 1.0f;
        if (coreTemp <= ModConstants.TEMP_SEVERE_HYPOTHERMIA)
            return 0.0f;

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

    // Nociception this wound generates. Peaks in inflammation and fades to zero as
    // it heals. This is just the signal generation, modulation happens at limb/systemic.
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
        float base = depthScale * typeScale * size;

        // Infection amplifies pain.
        float infectionScale = (1.0f + infectionLevel * 0.8f);
        return Math.min(1.5f, base * stagePainFactor() * infectionScale);
    }

    // Pain envelope across the healing phases. Fresh is sharp, inflammation is the throbbing peak,
    // then it tapers into no pain at all.
    private float stagePainFactor()
    {
        return switch (stage)
        {
            case FRESH -> 1.0f;
            case INFLAMED -> 1.3f;
            case SCABBING -> 0.5f;
            case SCARRING -> 0.1f;
            case HEALED -> 0.0f;
        };
    }

    // === TICK ADVANCEMENT ===

    public boolean tickAdvance(float currentBleedML)
    {
        boolean changed = false;
        ageTicks++;

        // Dressing wear, fouling, active antiseptic, and anaerobic occlusion risk.
        if (dressing != null)
        {
            dressing.tickWear(currentBleedML, ModConstants.SECONDS_PER_TICK);

            // Once the dressing becomes due for a change, it starts adding contamination.
            if (dressing.isOverdue())
            {
                contamination = Math.min(1f, contamination + ModConstants.DRESSING_FOUL_CONTAM_RISE);
                changed = true;
            }

            // If the dressing had antiseptic on it, it decreases contamination.
            float antiseptic = dressing.getAntiseptic();
            if (antiseptic > 0f && contamination > 0f)
            {
                contamination = Math.max(0f, contamination - antiseptic * ModConstants.DRESSING_ANTISEPTIC_DECONTAM_PER_SECOND * ModConstants.SECONDS_PER_TICK);
                changed = true;
            }

            // A sealed dressing over a dirty wound traps bacteria (anaerobic).
            if (dressing.getOcclusion() > 0.5f && contamination > 0.3f)
            {
                contamination = Math.min(1f, contamination + ModConstants.DRESSING_OCCLUSION_ANAEROBIC_RISE);
                changed = true;
            }
        }

        // Stage advancement.
        changed |= tickStageProgress();
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

    private boolean tickStageProgress()
    {
        boolean changed = false;
        switch (stage)
        {
            case FRESH ->
            {
                // Inflammation can't begin until hemostasis is achieved.
                if (clotIntegrity >= 0.95f)
                {
                    advanceTo(WoundStage.INFLAMED);
                    changed = true;
                }
            }
            case INFLAMED ->
            {
                // Roughly 1 in-game day. Infection stalls it though.
                float rate = 1f / 24000f * (1f - (infectionLevel * 0.8f));
                stageProgress = Math.min(1f, stageProgress + rate);
                if (stageProgress >= 1f)
                    advanceTo(WoundStage.SCABBING);
                changed = true;
            }
            case SCABBING ->
            {
                // Roughly 3 days.
                stageProgress = Math.min(1f, stageProgress + 1f / 72000f);
                if (stageProgress >= 1f)
                    advanceTo(WoundStage.SCARRING);
                changed = true;
            }
            case SCARRING ->
            {
                // Roughly 5 days.
                stageProgress = Math.min(1f, stageProgress + 1f / 120000f);
                if (stageProgress >= 1f)
                    advanceTo(WoundStage.HEALED);
                changed = true;
            }
            case HEALED -> { }
        }

        return changed;
    }

    private void advanceTo(WoundStage next)
    {
        stage = next;
        stageProgress = 0f;
    }

    // === TREATMENT ACTIONS ===

    public void applyDressing(Dressing dressing)
    {
        // Dressing snapshot.
        this.dressing = dressing.copy();

        // A dirty dressing is a contamination source, so it drags contamination toward (1 - cleanliness).
        float floor = 1f - dressing.getCleanliness();
        if (contamination < floor)
            contamination = Math.min(1f, contamination + (floor - contamination) * ModConstants.DRESSING_DIRTY_APPLY_FRACTION);
    }

    // Returns true if removal ripped the wound open.
    public boolean removeDressing(RandomSource rand)
    {
        if (dressing == null)
            return false;

        boolean ripped = false;
        float adherence = dressing.getAdherence();
        boolean healing = (stage == WoundStage.INFLAMED || stage == WoundStage.SCABBING || stage == WoundStage.SCARRING);
        if (adherence > 0f && healing && rand.nextFloat() < adherence * ModConstants.DRESSING_ADHERENCE_REOPEN_CHANCE)
        {
            reopen();
            ripped = true;
        }

        dressing = null;
        return ripped;
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
        stage = WoundStage.FRESH;
        stageProgress = 0f;
        bleedRateML = computeInitialBleedRate() * 0.60f;
        clotIntegrity = 0f;
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
    public float getClotIntegrity() { return clotIntegrity; }
    public float getStageProgress() { return stageProgress; }
    public float getContamination() { return contamination; }
    public float getInfectionLevel() { return infectionLevel; }
    public boolean hasDressing() { return dressing != null; }
    public Dressing getDressing() { return dressing; }
    public boolean isDressingOverdue() { return dressing != null && dressing.isOverdue(); }
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
        tag.putFloat("ClotIntegrity", clotIntegrity);
        tag.putLong  ("AgeTicks", ageTicks);
        tag.putFloat ("Contamination", contamination);
        tag.putFloat ("InfectionLevel", infectionLevel);
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

        if (dressing != null)
        {
            CompoundTag dt = new CompoundTag();
            dressing.writeToNBT(dt);
            tag.put("Dressing", dt);
        }
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
        clotIntegrity = tag.getFloat("ClotIntegrity");
        ageTicks = tag.getLong("AgeTicks");
        contamination = tag.getFloat("Contamination");
        infectionLevel = tag.getFloat("InfectionLevel");
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
        dressing = tag.contains("Dressing") ? Dressing.readFromNBT(tag.getCompound("Dressing")) : null;
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
                infectionLevel, contamination, dressing != null
        );
    }
}
