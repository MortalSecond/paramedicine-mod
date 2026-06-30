package net.invinciblemoebius.traumaparamedicinemod.client;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// CLIENT-SIDE physiological effect. Player vertical motion is has to happen clientside, since
// a server-applied downward push would just rubberband.
@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BuoyancyHandler
{
    @SubscribeEvent
    public static void onClientPlayerTick(TickEvent.PlayerTickEvent event)
    {
        // START phase so we change motion BEFORE travel() integrates it this same tick.
        if (event.phase != TickEvent.Phase.START)
            return;
        if (!event.player.level().isClientSide)
            return;

        // Only the local, client-side player
        Minecraft mc = Minecraft.getInstance();
        if (event.player != mc.player)
            return;
        LocalPlayer player = mc.player;
        if (player.isCreative() || player.isSpectator())
            return;

        // Only acts while the head is actually submerged. The instant you break the surface,
        // buoyancy returns to normal, which is what lets a desperate swimmer escape at the shore.
        if (!player.isEyeInFluid(FluidTags.WATER))
            return;

        player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data -> apply(player, data));
    }

    private static void apply(LocalPlayer player, PlayerHealthData data)
    {
        float breathFraction = clamp01(data.getBreathReserveSeconds() / PlayerHealthData.BREATH_RESERVE_MAX);
        float consciousness  = clamp01(data.getConsciousness());

        if (breathFraction >= 1.0f && consciousness >= ModConstants.CONSCIOUSNESS_ALERT)
            return;

        Vec3 motion = player.getDeltaMovement();
        double newY = motion.y;

        // Emptier lungs = more negative buoyancy.
        float sink = (1.0f - breathFraction);
        sink = sink * sink;
        newY -= sink * ModConstants.DROWN_SINK_ACCELERATION_MAX;

        float surfacingCapacity = clamp01(consciousness / ModConstants.CONSCIOUSNESS_ALERT);

        // Surfacing speed ceiling only bites once consciousness slips below ALERT. A fully-alert swimmer is never
        // capped, so they can fight the sink AND their kick momentum survives the next tick.
        if (surfacingCapacity < 1.0f)
        {
            double riseCeiling = lerp(ModConstants.DROWN_RISE_CEILEING_OUT, ModConstants.DROWN_RISE_CEILEING_ALERT, surfacingCapacity);
            if (newY > riseCeiling) newY = riseCeiling;
        }

        // Kick off the seabed to lunge upward, the way people scrabble toward shore.
        // Bypasses the ceiling for the launch tick, but drag bleeds decreases it, causing a kick-and-sink
        // curve, and a way out of one-deep holes. Needs VOICE-level consciousness.
        boolean kicking = Minecraft.getInstance().options.keyJump.isDown();
        if (kicking && player.onGround() && consciousness >= ModConstants.CONSCIOUSNESS_VOICE)
        {
            double kick = ModConstants.DROWN_PUSHOFF_IMPULSE * surfacingCapacity;
            if (newY < kick) newY = kick;
        }

        // Never sink faster than this, so the player is never truly frozen.
        if (newY < -ModConstants.DROWN_MAX_SINK_SPEED) newY = -ModConstants.DROWN_MAX_SINK_SPEED;

        player.setDeltaMovement(motion.x, newY, motion.z);
    }

    private static float clamp01(float v)
    {
        return Math.max(0f, Math.min(1f, v));
    }

    private static double lerp(double a, double b, float t)
    {
        return a + (b - a) * t;
    }
}