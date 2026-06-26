package net.invinciblemoebius.traumaparamedicinemod.wounding;

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
public record WoundingContext(LimbNode attackedNode, PlayerHealthData data, ArmorTier nodeArmorTier,
                              ArmorTier helmetTier, ArmorTier chestTier, ArmorTier leggingsTier, ArmorTier bootsTier,
                              float preArmorDamage, float postArmorDamage, AttackCategory category, DamageSource source,
                              @Nullable Entity attacker, @Nullable ItemStack sourceWeapon, float hitU, float hitV,
                              Vec3 hitDirection, float explosionProximity)
{
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
