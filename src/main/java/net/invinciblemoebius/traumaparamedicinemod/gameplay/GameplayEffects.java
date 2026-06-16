package net.invinciblemoebius.traumaparamedicinemod.gameplay;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

// This is the class that bridges the health system and actual gameplay effects.
// If you're looking for the effects of substances in Paramedicine, go take a peek
// at SubstanceType instead.
@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GameplayEffects
{
    private static final UUID SPEED_UUID = UUID.fromString("a1b2c3d4-1234-5678-abcd-ef1234567890");
    private static final String SPEED_NAME = "paramedicine_speed";
    private static final UUID ATTACK_UUID = UUID.fromString("b2c3d4e5-2345-6789-bcde-f12345678901");
    private static final String ATTACK_NAME = "paramedicine_attack_damage";

    // === ENTRY ===

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        // Prevent Creative and Spectator players, and bots from
        // getting Paramedicine's capability.
        if (event.phase == TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (event.player.isCreative()) return;
        if (event.player.isSpectator()) return;

        event.player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data -> applyAll(event.player, data));
    }

    private static void applyAll(Player player, PlayerHealthData data)
    {
        applyMovementSpeed(player, data);
        applySprintGating(player, data);
        applyAttackDamage(player, data);
    }

    // === MOVEMENT SPEED METHODS ===

    private static void applyMovementSpeed(Player player, PlayerHealthData data)
    {
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;

        float legCeiling = computeLegCeiling(data);
        float systemicFactor = computeSystemicFactor(data);
        float finalMultiplier = Math.max(0f, Math.min(1f, Math.min(legCeiling, systemicFactor)));

        double modifier = finalMultiplier - 1.0f;

        attr.removeModifier(SPEED_UUID);
        if (modifier < -0.001)
            attr.addTransientModifier(new AttributeModifier(SPEED_UUID, SPEED_NAME, modifier, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    // This calculates the CEILING of the speed. This is as fast
    // as a player can move even while completely health.
    // One completely compromised leg equals roughly 50% speed,
    // while both legs destroyed equals a top 5% speed, before penalties.
    private static float computeLegCeiling(PlayerHealthData data)
    {
        float left = minLegNodeHealth(data, true);
        float right = minLegNodeHealth(data, false);
        return 0.05f + (left * 0.475f) + (right * 0.475f);
    }

    private static float minLegNodeHealth(PlayerHealthData data, boolean left)
    {
        LimbData thigh = data.getLimb(left ? LimbNode.LEFT_UPPER_LEG : LimbNode.RIGHT_UPPER_LEG);
        LimbData shin = data.getLimb(left ? LimbNode.LEFT_LOWER_LEG : LimbNode.RIGHT_LOWER_LEG);
        LimbData foot = data.getLimb(left ? LimbNode.LEFT_FOOT : LimbNode.RIGHT_FOOT);

        if (thigh == null || shin == null || foot == null) return 1.0f;

        return Math.min(thigh.getTotalHealth(), Math.min(shin.getTotalHealth(), foot.getTotalHealth()));
    }

    // Systemic speed factor. This is how fast the player can muster to
    // move, so a person with both healthy legs will move slowly due to
    // stacking penalties from shock, low oxygenation, confusion, etc.
    private static float computeSystemicFactor(PlayerHealthData data)
    {
        return consciousnessSpeedFactor(data.getConsciousness())
                * painShockSpeedFactor(data.getPainShock())
                * staminaSpeedFactor(data.getStamina())
                * hypovolemiaSpeedFactor(data);
    }

    // Linear fall to 0 at UNRESPONSIVE.
    private static float consciousnessSpeedFactor(float consciousness)
    {
        return Math.max(0f, consciousness / ModConstants.CONSCIOUSNESS_ALERT);
    }

    // Linear, but player retains 30% speed. They can drag themselves, but just barely.
    private static float painShockSpeedFactor(float painShock)
    {
        return 1.0f - (painShock * 0.70f);
    }

    // Quadratic curve, so healthy players barely notice small stamina drops, but
    // penalty becomes increasingly more noticeable. 0.35 floor.
    private static float staminaSpeedFactor(float stamina)
    {
        float depletion = 1.0f - stamina;
        return 1.0f - (depletion * depletion * 0.65f);
    }

    // No effect until moderate hypovolemia, then it falls to 0.40
    // at complete exsanguination.
    private static float hypovolemiaSpeedFactor(PlayerHealthData data)
    {
        float fraction = data.getBloodVolume() / ModConstants.BLOOD_NORMAL;

        if (fraction >= ModConstants.BLOOD_MODERATE_HYPOVOLEMIA)
            return 1.0f;

        if (fraction >= ModConstants.BLOOD_SEVERE_HYPOVOLEMIA)
        {
            // MODERATE TO SEVERE = 1.0 - 0.75
            float t = (ModConstants.BLOOD_MODERATE_HYPOVOLEMIA - fraction) / (ModConstants.BLOOD_MODERATE_HYPOVOLEMIA - ModConstants.BLOOD_SEVERE_HYPOVOLEMIA);
            return 1.0f - (t * 0.25f);
        }

        // SEVERE to zero = 0.75 - 0.40
        float t = fraction / ModConstants.BLOOD_SEVERE_HYPOVOLEMIA;
        return 0.40f + (t * 0.35f);
    }

    // === SPRINT GATING ===

    private static void applySprintGating(Player player, PlayerHealthData data)
    {
        if (!player.isSprinting()) return;

        boolean cancel = false;

        // Exhaustion.
        if (data.getStamina() < 0.10f)
            cancel = true;

        // Confusion.
        if (data.getConsciousness() < ModConstants.CONSCIOUSNESS_VOICE)
            cancel = true;

        // Legs destruction. Both must be bad, because you can still limp-sprint with one.
        if (minLegNodeHealth(data, true) < 0.40f && minLegNodeHealth(data, false) < 0.40f)
            cancel = true;

        // Breathlessness.
        if (data.getActualRespiratoryRate() < data.getRespiratoryDrive() * 0.50f)
            cancel = true;

        if (cancel)
            player.setSprinting(false);
    }

    // === ATTACK DAMAGE ===

    // Applies an attack modifier on the main hand. The 30% floor is because
    // theoretically they can still push and shove even with a destroyed arm.
    private static void applyAttackDamage(Player player, PlayerHealthData data)
    {
        var attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) return;

        float armHealth = minArmNodeHealth(data);
        float damageMultiplier = 0.30f + (armHealth * 0.70f);

        attr.removeModifier(ATTACK_UUID);
        if (damageMultiplier < 0.999f)
            attr.addTransientModifier(new AttributeModifier(ATTACK_UUID, ATTACK_NAME, damageMultiplier - 1.0f, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static float minArmNodeHealth(PlayerHealthData data)
    {
        LimbData shoulder = data.getLimb(LimbNode.RIGHT_UPPER_ARM);
        LimbData forearm = data.getLimb(LimbNode.RIGHT_FOREARM);
        LimbData hand = data.getLimb(LimbNode.RIGHT_HAND);

        if (shoulder == null || forearm == null || hand == null) return 1.0f;
        return Math.min(shoulder.getTotalHealth(), Math.min(forearm.getTotalHealth(), hand.getTotalHealth()));
    }

    // === MINING SPEED ===

    // Because mining is mostly a grip task, the hand health is the modifier. 20% floor.
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event)
    {
        if (event.getEntity().level().isClientSide) return;

        event.getEntity().getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
        {
            LimbData hand =  data.getLimb(LimbNode.RIGHT_HAND);
            if (hand == null) return;

            float handHealth = hand.getTotalHealth();
            float miningMultiplier = 0.20f + (handHealth * 0.80f);
            event.setNewSpeed(event.getNewSpeed() * miningMultiplier);
        });
    }
}
