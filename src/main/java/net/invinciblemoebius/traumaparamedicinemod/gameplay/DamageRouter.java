package net.invinciblemoebius.traumaparamedicinemod.gameplay;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.wounding.ArmorTier;
import net.invinciblemoebius.traumaparamedicinemod.wounding.AttackCategory;
import net.invinciblemoebius.traumaparamedicinemod.wounding.WoundingBehavior;
import net.invinciblemoebius.traumaparamedicinemod.wounding.WoundingContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

// This removes the vanilla damage system and replaces it with
// Paramedicine's wound system. Mind, this doesn't wire up
// the physiological effects, it only builds a WoundingContext that
// then gets used in WoundingBehavior to produce a WoundingInstruction.
@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DamageRouter
{
    // === ENTRY/HOOK ===
    // Has a low priority so that mods that increase/decrease damage have already
    // done their calculations and Paramedicine works with them.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        DamageSource source = event.getSource();

        // Some damage should never route through the wound system.
        if (shouldBypass(source))
            return;

        player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
        {
            float preArmorDamage = event.getAmount();
            float postArmorDamage = estimatePostArmor(player, source, preArmorDamage);
            AttackCategory category = classify(source);
            LimbNode hitNode = resolveHitNode(player, source, category);
            float hitU = computeHitU(player, source.getDirectEntity());
            float hitV = computeHitV();
            Vec3 hitDir = computeHitDirection(player, source.getDirectEntity());
            float proximity = (category == AttackCategory.EXPLOSION) ? computeExplosionProximity(player, source) : 0f;

            WoundingContext ctx = new WoundingContext(hitNode, data, armorTierForNode(player, hitNode),
                    armorTierForSlot(player, 3), armorTierForSlot(player, 2), armorTierForSlot(player, 1),
                    armorTierForSlot(player, 0), preArmorDamage, postArmorDamage, category, source,
                    source.getEntity(), resolveWeapon(source), hitU, hitV, hitDir, proximity);

            WoundingBehavior.handle(ctx);
        });

        // Cancel the vanilla damage system.
        event.setCanceled(true);
    }

    // === HIT NODE METHODS ===

    private static LimbNode resolveHitNode(ServerPlayer player, DamageSource source, AttackCategory category)
    {
        // Fall damage always lands on the feet. Since WoundingBehavior makes a roll check for which
        // of the two legs gets damaged, it's largely not important which foot gets returned.
        if (category == AttackCategory.FALL)
            return LimbNode.LEFT_FOOT;

        // Explosions distribute across the body, handled in WoundingBehavior.
        if (category == AttackCategory.EXPLOSION)
            return LimbNode.UPPER_TORSO;

        Entity attacker = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();

        if (attacker == null)
            return weightedRandomNode();

        // Height-based zone from relative position.
        double attackerY = attacker.getEyeY();
        double defenderFootY = player.getY();
        double defenderHeight = player.getBbHeight();
        double relativeHeight = (attackerY - defenderFootY) / defenderHeight;

        return sampleNodeForHeight(relativeHeight, player, attacker);
    }

    // Given a relative height (0.0 = feed, 1.0 = head) and geometry, returns a
    // sample node. Uses probability, so hits feel natural rather than snapping to hard zones.
    private static LimbNode sampleNodeForHeight(double relativeHeight, ServerPlayer player, Entity attacker)
    {
        // Determine left/right side from horizontal geometry.
        boolean rightSide = isAttackerOnRight(player, attacker);
        float r = (float) Math.random();

        // Head and neck zone.
        if (relativeHeight > 0.85f)
            return r < 0.7f ? LimbNode.HEAD : LimbNode.NECK;

        // Upper body. Chest and arms.
        if (relativeHeight > 0.55f)
        {
            if (r < 0.55f)
                return LimbNode.UPPER_TORSO;

            if (r < 0.78f)
                return rightSide ? LimbNode.RIGHT_UPPER_ARM : LimbNode.LEFT_UPPER_ARM;

            return rightSide ? LimbNode.RIGHT_FOREARM : LimbNode.LEFT_FOREARM;
        }

        // Mid-body. Abdomen and hands.
        if (relativeHeight > 0.35f)
        {
            if (r < 0.55f)
                return LimbNode.LOWER_TORSO;

            if (r < 0.75f)
                return LimbNode.GROIN;

            if (r < 0.88f)
                return rightSide ? LimbNode.RIGHT_FOREARM : LimbNode.LEFT_FOREARM;

            return rightSide ? LimbNode.RIGHT_HAND : LimbNode.LEFT_HAND;
        }

        // Lower body. Legs.
        if (relativeHeight > 0.15f)
        {
            if (r < 0.6f)
                return rightSide ? LimbNode.RIGHT_UPPER_LEG : LimbNode.LEFT_UPPER_LEG;

            return rightSide ? LimbNode.RIGHT_LOWER_LEG : LimbNode.LEFT_LOWER_LEG;
        }

        // Feet.
        return rightSide ? LimbNode.RIGHT_FOOT : LimbNode.LEFT_FOOT;
    }

    // Random node for attacks with no geometry (status effects and environmental stuff)
    // Weighted so it doesn't feel truly random. Torso biggest, extremities smallest.
    private static LimbNode weightedRandomNode()
    {
        float r = (float) Math.random();

        if (r < 0.30f)
            return LimbNode.UPPER_TORSO;
        if (r < 0.45f)
            return LimbNode.LOWER_TORSO;
        if (r < 0.55f)
            return Math.random() < 0.5 ? LimbNode.LEFT_UPPER_LEG : LimbNode.RIGHT_UPPER_LEG;
        if (r < 0.65f)
            return Math.random() < 0.5 ? LimbNode.LEFT_UPPER_ARM : LimbNode.RIGHT_UPPER_ARM;
        if (r < 0.73f)
            return Math.random() < 0.5 ? LimbNode.LEFT_LOWER_LEG : LimbNode.RIGHT_LOWER_LEG;
        if (r < 0.80f)
            return Math.random() < 0.5 ? LimbNode.LEFT_FOREARM : LimbNode.RIGHT_FOREARM;
        if (r < 0.86f)
            return LimbNode.HEAD;
        if (r < 0.90f)
            return LimbNode.NECK;
        if (r < 0.94f)
            return LimbNode.GROIN;
        if (r < 0.97f)
            return Math.random() < 0.5 ? LimbNode.LEFT_FOOT : LimbNode.RIGHT_FOOT;

        return Math.random() < 0.5 ? LimbNode.LEFT_HAND : LimbNode.RIGHT_HAND;
    }

    // === GEOMETRY METHODS ===

    private static boolean isAttackerOnRight(ServerPlayer player, Entity attacker)
    {
        Vec3 forward = player.getLookAngle();
        Vec3 toAttacker = attacker.position().subtract(player.position()).normalize();

        double cross = forward.x * toAttacker.z - forward.z * toAttacker.x;
        return cross < 0;
    }

    private static float computeHitU(ServerPlayer defender, @Nullable Entity attacker)
    {
        if (attacker == null)
            return ModConstants.WOUND_U_ANTERIOR;

        Vec3 forward = defender.getLookAngle();
        Vec3 toAttacker = attacker.position().subtract(defender.position()).normalize();
        double forwardDot = forward.dot(toAttacker);
        double rightDot = forward.x * toAttacker.z - forward.z * toAttacker.x;
        double angle = Math.atan2(forwardDot, rightDot);

        float u = (float) (angle / Math.PI);
        return ((u % 2.0f) + 2.0f) % 2.0f;
    }

    private static float computeHitV()
    {
        return 0.35f + (float) Math.random() * 0.3f;
    }

    private static Vec3 computeHitDirection(ServerPlayer defender, @Nullable Entity attacker)
    {
        if (attacker == null)
            return new Vec3(0, 0, 1);

        return attacker.position().subtract(defender.position()).normalize();
    }

    private static float computeExplosionProximity(ServerPlayer player, DamageSource source)
    {
        Vec3 explosionPos = source.getSourcePosition();

        if (explosionPos == null)
            return 0f;

        return (float) player.position().distanceTo(explosionPos);
    }

    // === HELPER METHODS ===

    private static boolean shouldBypass(DamageSource source)
    {
        // Right now, the things that get bypassed are drowning, freezing,
        // and damage stuff. In the future, starvation damage, and probably
        // magic will be added once i get those systems going.
        return source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypeTags.BYPASSES_ARMOR) && source.is(DamageTypeTags.WITCH_RESISTANT_TO);
    }

    private static AttackCategory classify(DamageSource source)
    {
        if (source.is(DamageTypeTags.IS_EXPLOSION))
            return AttackCategory.EXPLOSION;

        if (source.is(DamageTypeTags.IS_FIRE))
            return AttackCategory.FIRE;

        if (source.is(DamageTypeTags.IS_FALL))
            return AttackCategory.FALL;

        if (source.is(DamageTypeTags.IS_PROJECTILE))
        {
            Entity direct =  source.getDirectEntity();

            if (direct instanceof ThrownTrident)
                return AttackCategory.PROJECTILE_TRIDENT;

            if (direct instanceof AbstractArrow)
                return AttackCategory.PROJECTILE_ARROW;

            return AttackCategory.PROJECTILE_ARROW;
        }

        if (isAbrasionSource(source))
            return AttackCategory.BUSH;

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living)
        {
           ItemStack weapon = living.getMainHandItem();

           if (weapon.isEmpty())
               return AttackCategory.MELEE_UNARMED;

           if (weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem)
               return AttackCategory.MELEE_BLADED;

           if (weapon.getItem() instanceof PickaxeItem || weapon.getItem() instanceof HoeItem
                   || weapon.getItem() instanceof ShovelItem)
               return AttackCategory.MELEE_BLUNT;

           // Sorry, modded weapons. But this is the generic return type, made
           // unarmed so people don't cheese the game by using dirt blocks as weapons.
           return AttackCategory.MELEE_UNARMED;
        }

        // Anvils, suffocation from gravel, uhhh, just anything left over, really.
        return AttackCategory.ENVIRONMENTAL;
    }

    private static ArmorTier armorTierForNode(Player player, LimbNode node)
    {
        int slot = switch (node.category)
        {
            case HEAD -> 3;
            case TRUNK -> node == LimbNode.GROIN ? 1 : 2;
            case ARM -> 2;
            case LEG -> node.proximalNode == LimbNode.GROIN ? 1 : 0;
        };

        return armorTierForSlot(player, slot);
    }

    private static ArmorTier armorTierForSlot(Player player, int slot)
    {
        ItemStack stack = player.getInventory().getArmor(slot);

        if (stack.isEmpty())
            return ArmorTier.NONE;
        if (!(stack.getItem() instanceof ArmorItem armor))
            return ArmorTier.NONE;

        var material = armor.getMaterial();
        if (material == ArmorMaterials.LEATHER)
            return ArmorTier.LEATHER;
        if (material == ArmorMaterials.CHAIN)
            return ArmorTier.CHAIN;

        // Any armor that isn't the above will get classified as hard. Modded armor
        // theoretically has native compatibility with Paramedicine this way.
        return ArmorTier.HARD;
    }

    private static boolean isAbrasionSource(DamageSource source)
    {
        String msgId = source.getMsgId();

        boolean wasBush = msgId.equals("sweetBerryBush");
        boolean wasCactus = msgId.equals("cactus");

        return wasBush || wasCactus;
    }

    @Nullable
    private static ItemStack resolveWeapon(DamageSource source)
    {
        if (source.getEntity() instanceof LivingEntity living)
        {
            ItemStack weapon =  living.getMainHandItem();
            return weapon.isEmpty() ? null : weapon;
        }

        return null;
    }

    private static float estimatePostArmor(Player player, DamageSource source, float preArmor)
    {
        if (source.is(DamageTypeTags.BYPASSES_ARMOR))
            return preArmor;

        float armor = (float) player.getArmorValue();
        float toughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        // Vanilla formula.
        float reduction = Math.min(20f, Math.max(armor / 5f, armor - preArmor / (2f + toughness / 4f)));

        return preArmor * (1f - reduction / 25f);
    }
}
