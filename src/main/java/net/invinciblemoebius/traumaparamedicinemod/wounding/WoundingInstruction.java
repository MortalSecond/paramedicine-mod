package net.invinciblemoebius.traumaparamedicinemod.wounding;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.BoneState;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundDepth;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundType;

import javax.annotation.Nullable;

// These are the blueprints for the kinda damage a single attack vector can produce,
// NOT the actual systemic values themselves. That's in WoundingBehavior.
// This class really just exists to make WoundingBehavior easier to read and add stuff to.
// Mind, that not every attack can generate a Wound. An Instruction can either:
//      A) Create a Wound on a specific limb node.
//      B) Directly mutate the PlayerHealthData values without a Wound.
//      C) Both.
// Thus, things like liver-punching someone can produce a vagal response, or a headshot can knock
// out someone, or an explosion can cause a tension pneumothorax. They can happen without a Wound instance.
public class WoundingInstruction
{
    private final WoundType type;
    private final WoundDepth depth;
    private final float size;
    private float contamination = 0f;
    private boolean hasArrow = false;
    private boolean hasBullet = false;
    private boolean hasShrapnel = false;
    private boolean bleedingManaged = false;
    private float forcedBleedingRateML = 0f;
    private boolean isEntry = false;
    private boolean isExit = false;
    private float woundPositionU = 0f;
    private float woundPositionV = 0f;

    private float consciousnessDrop = 0f;
    private float painSpike = 0f;
    private float fibrillationAmount = 0f;
    private boolean fibrillationsForced = false;
    private float vascularToneDelta = 0f;
    private float lungAirML = 0f;
    private float lungBloodML = 0f;
    private boolean isTensionPneumo = false;
    private boolean leftLungChanges = false;
    private boolean rightLungChanges = false;
    @Nullable private BoneState forceBoneState = null;
    private float muscleHealthDamage = 0f;

    // === CONSTRUCTOR ===
    public WoundingInstruction(WoundType type, WoundDepth depth, float size)
    {
        this.type = type;
        this.depth = depth;
        this.size = Math.max(0f, Math.min(1f, size));
    }

    // === CONFIG METHODS ===

    public WoundingInstruction withContamination(float amount)
    {
        this.contamination = Math.max(0f, Math.min(1f, amount));
        return this;
    }

    public WoundingInstruction withArrow()
    {
        this.hasArrow = true;
        return this;
    }

    public WoundingInstruction withBullet()
    {
        this.hasBullet = true;
        return this;
    }

    public WoundingInstruction withShrapnel()
    {
        this.hasShrapnel = true;
        return this;
    }

    public WoundingInstruction managedBleeding(float amount)
    {
        this.bleedingManaged = true;
        this.forcedBleedingRateML = amount;
        return this;
    }

    public WoundingInstruction asEntry()
    {
        this.isEntry = true;
        return this;
    }

    public WoundingInstruction asExit()
    {
        this.isExit = true;
        return this;
    }

    public WoundingInstruction consciousnessDrop(float amount)
    {
        this.consciousnessDrop = Math.max(0f, amount);
        return this;
    }

    public WoundingInstruction painSpike(float amount)
    {
        this.painSpike = Math.max(0f, amount);
        return this;
    }

    public WoundingInstruction givesFibrillations(float amount, boolean forced)
    {
        this.fibrillationAmount = Math.max(0f, Math.min(1f, amount));
        this.fibrillationsForced = forced;
        return this;
    }

    public WoundingInstruction vascularToneDelta(float delta)
    {
        this.vascularToneDelta = delta;
        return this;
    }

    public WoundingInstruction addsAirToLeftLung(float ml)
    {
        leftLungChanges = true;
        this.lungAirML = Math.max(0f, ml);
        return this;
    }

    public WoundingInstruction addsBloodToLeftLung(float ml)
    {
        leftLungChanges = true;
        this.lungBloodML = Math.max(0f, ml);
        return this;
    }

    public WoundingInstruction addsAirToRightLung(float ml)
    {
        rightLungChanges = true;
        this.lungAirML = Math.max(0f, ml);
        return this;
    }

    public WoundingInstruction addsBloodToRightLung(float ml)
    {
        rightLungChanges = true;
        this.lungBloodML = Math.max(0f, ml);
        return this;
    }

    public WoundingInstruction addsAirToBothLungs(float ml)
    {
        rightLungChanges = true;
        leftLungChanges = true;
        this.lungAirML = Math.max(0f, ml);
        return this;
    }

    public WoundingInstruction addsBloodToBothLungs(float ml)
    {
        rightLungChanges = true;
        leftLungChanges = true;
        this.lungBloodML = Math.max(0f, ml);
        return this;
    }

    public WoundingInstruction isTensionPneumo()
    {
        this.isTensionPneumo = true;
        return this;
    }

    public WoundingInstruction forceBoneState(BoneState state)
    {
        this.forceBoneState = state;
        return this;
    }

    public WoundingInstruction muscleHealthDamage(float amount)
    {
        this.muscleHealthDamage = Math.max(0f, amount);
        return this;
    }

    public WoundingInstruction atPosition(float u, float v)
    {
        this.woundPositionU = ((u % ModConstants.WOUND_U_MAX) + ModConstants.WOUND_U_MAX) % ModConstants.WOUND_U_MAX;
        this.woundPositionV = Math.max(0f, Math.min(1f, v));
        return this;
    }

    // === APPLICATION ===

    public void apply(@Nullable LimbData limb, PlayerHealthData data)
    {
        if (limb != null)
        {
            Wound wound = new Wound(type, depth, size);
            wound.setContamination(contamination);
            wound.setHasArrow(hasArrow);
            wound.setHasBullet(hasBullet);
            wound.setHasShrapnel(hasShrapnel);

            if (bleedingManaged)
            {
                wound.setBleedingManaged(true);
                wound.setForcedBleedingRateML(forcedBleedingRateML);
            }

            if (isEntry)
            {
                wound.setIsEntry(true);
            }
            if (isExit)
            {
                wound.setIsExit(true);
            }

            wound.setWoundPositionU(woundPositionU);
            wound.setWoundPositionV(woundPositionV);

            limb.addWound(wound);
        }

        // Direct systemic mutations to the LIMB.
        if (muscleHealthDamage > 0f)
        {
            limb.setMuscleHealth(limb.getMuscleHealth() - muscleHealthDamage);
        }

        if (forceBoneState != null && forceBoneState.ordinal() > limb.getBoneState().ordinal())
        {
            limb.setBoneState(forceBoneState);
        }

        // Direct systemic mutations to the WHOLE BODY.
        if (consciousnessDrop > 0f)
        {
            data.setConsciousnessDirectly(Math.max(0f, data.getConsciousness() - consciousnessDrop));
        }

        if (painSpike > 0f)
        {
            data.spikeAggregatedPain(painSpike);
        }

        if (fibrillationAmount > 0f && fibrillationAmount > data.getFibrillations())
        {
            data.setFibrillations(fibrillationAmount);
        }

        if (fibrillationsForced)
        {
            data.setFibrillationsForced(true, fibrillationAmount);
        }

        if (vascularToneDelta > 0f)
        {
            data.setVascularTone(data.getVascularTone() + vascularToneDelta);
        }

        if (leftLungChanges && !rightLungChanges)
        {
            if (lungAirML > 0f)
            {
                data.getLeftLung().addAir(lungAirML);
                if (isTensionPneumo)
                {
                    data.getLeftLung().setTensionPneumothorax(true);
                }
            }
            if (lungBloodML > 0f)
            {
                data.getLeftLung().addBlood(lungBloodML);
            }
        }

        if (rightLungChanges && !leftLungChanges)
        {
            if (lungAirML > 0f)
            {
                data.getRightLung().addAir(lungAirML);
                if (isTensionPneumo)
                {
                    data.getRightLung().setTensionPneumothorax(true);
                }
            }
            if (lungBloodML > 0f)
            {
                data.getRightLung().addBlood(lungBloodML);
            }
        }

        if (leftLungChanges && rightLungChanges)
        {
            if (lungAirML > 0f)
            {
                data.getRightLung().addAir(lungAirML);
                data.getLeftLung().addAir(lungAirML);
                if (isTensionPneumo)
                {
                    data.getLeftLung().setTensionPneumothorax(true);
                    data.getRightLung().setTensionPneumothorax(true);
                }
            }
            if (lungBloodML > 0f)
            {
                data.getRightLung().addBlood(lungBloodML);
                data.getLeftLung().addBlood(lungBloodML);
            }
        }
    }

    public void applyMutationsOnly(PlayerHealthData data)
    {
        apply(null, data);
    }
}
