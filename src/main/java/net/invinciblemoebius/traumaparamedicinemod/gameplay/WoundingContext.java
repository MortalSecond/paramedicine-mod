package net.invinciblemoebius.traumaparamedicinemod.gameplay;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbCategory;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

// This is just the info about an attack. If you're looking for
// the physiological data of individual wounds and types, it's in /wound/Wound.
public final class WoundingContext
{
    public final LimbNode attackedNode;
    public final PlayerHealthData data;
    @Nullable public final Entity attacker;
    @Nullable public final ItemStack sourceWeapon;
    public final ArmorTier nodeArmorTier;
    public final ArmorTier helmetTier;
    public final ArmorTier chestTier;
    public final ArmorTier leggingsTier;
    public final ArmorTier bootsTier;
    public final float preArmorDamage;
    public final float postArmorDamage;
    public final AttackCategory category;
    public final DamageSource source;

    public final Vec3 hitDirection;
    public final float hitU;
    public final float hitV;
    public final float explosionProximity;

    // === CONSTRUCTOR ===
    public WoundingContext(LimbNode attackedNode, PlayerHealthData data, ArmorTier nodeArmorTier,
            ArmorTier helmetTier, ArmorTier chestTier, ArmorTier leggingsTier, ArmorTier bootsTier,
            float preArmorDamage, float postArmorDamage, AttackCategory category, DamageSource source,
            @Nullable Entity attacker, @Nullable ItemStack sourceWeapon, float hitU, float hitV,
            Vec3 hitDirection, float explosionProximity)
    {
        this.attackedNode = attackedNode;
        this.data = data;
        this.nodeArmorTier = nodeArmorTier;
        this.helmetTier = helmetTier;
        this.chestTier = chestTier;
        this.leggingsTier = leggingsTier;
        this.bootsTier = bootsTier;
        this.preArmorDamage = preArmorDamage;
        this.postArmorDamage = postArmorDamage;
        this.category = category;
        this.source = source;
        this.attacker = attacker;
        this.sourceWeapon = sourceWeapon;
        this.hitU = hitU;
        this.hitV = hitV;
        this.hitDirection = hitDirection;
        this.explosionProximity = explosionProximity;
    }

    // === QUERY METHODS ===

    public boolean isTorsoHit()
    {
        return attackedNode == LimbNode.UPPER_TORSO || attackedNode == LimbNode.LOWER_TORSO;
    }

    public boolean isHeadHit()
    {
        return attackedNode == LimbNode.NECK || attackedNode == LimbNode.HEAD;
    }

    public boolean isLimbHit()
    {
        return attackedNode.category == LimbCategory.ARM || attackedNode.category == LimbCategory.LEG;
    }

    public boolean isFrontalHit()
    {
        return hitDirection != null && hitDirection.z > 0;
    }

    public boolean isRightSideHit()
    {
        return hitDirection != null && hitDirection.x < 0;
    }

    public boolean isExtremityHit()
    {
        return attackedNode == LimbNode.LEFT_HAND || attackedNode == LimbNode.RIGHT_HAND
                || attackedNode == LimbNode.LEFT_FOOT || attackedNode == LimbNode.RIGHT_FOOT;
    }
}
