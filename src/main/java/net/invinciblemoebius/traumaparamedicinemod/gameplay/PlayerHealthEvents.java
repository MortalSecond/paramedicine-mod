package net.invinciblemoebius.traumaparamedicinemod.gameplay;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerHealthEvents
{

    // === ENTRY ===

    // Attach a fresh capability instance to every player entity that loads.
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event)
    {
        if(!(event.getObject() instanceof Player)) return;

        ResourceLocation key = new ResourceLocation(ParamedicineMod.MOD_ID, "player_health");
        if(!event.getCapabilities().containsKey(key))
        {
            PlayerHealthCapability provider = new PlayerHealthCapability();
            event.addCapability(key, provider);
            event.addListener(provider::invalidate);
        }
    }

    // === INPUT REACTIONS ===

    // Detect when the player jumps.
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(PlayerHealthData::setJustJumped);
    }

    // Intercept item usage.
    @SubscribeEvent
    public static void onFinishUsingItem(LivingEntityUseItemEvent.Finish event)
    {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player))
            return;

        // FOOD INTERCEPTION
        // Eating solid food deposits fuel directly, NOT through the gut. Vanilla food is meant to feel
        // vanilla-fast. The slow SubstanceStorage route is for broths, meds, and mixtures with half lives.
        FoodProperties food = event.getItem().getItem().getFoodProperties(event.getItem(), player);
        if (food == null)
            return;

        float bulk = food.getNutrition() * ModConstants.NUTRITION_PER_FOOD_POINT;
        float fat = food.getNutrition() * food.getSaturationModifier() * ModConstants.NUTRITION_PER_SATURATION;

        player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                .ifPresent(data -> data.addNutrition(bulk + fat));
    }

    // Underwater drowning check.
    @SubscribeEvent
    public static void onPlayerAspiration(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
            return;
        if (event.player.level().isClientSide)
            return;
        if (!(event.player instanceof ServerPlayer player))
            return;
        if (player.isCreative() || player.isSpectator())
            return;

        boolean submerged = player.isEyeInFluid(FluidTags.WATER);
        if (!submerged)
            return;

        player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
        {
            // This returns the method if there's still breath remaining.
            // Once reserves run out, people start breathing and aspirating water.
            if (data.getBreathReserveSeconds() > 0f)
                return;

            float dt = ModConstants.SECONDS_PER_TICK;
            float aspirationRate = 8f * dt;
            data.getLeftLung().addFluid(aspirationRate);
            data.getRightLung().addFluid(aspirationRate);
            data.markDirty();
        });
    }

    // Intercept the vanilla food bar.
    @SubscribeEvent
    public static void onVanillaHungerOverride(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (event.player.level().isClientSide)
            return;
        if (!(event.player instanceof ServerPlayer player))
            return;
        if (player.isCreative() || player.isSpectator())
            return;

        // Drive the vanilla hunger bar from the FELT hunger, the same way the bubble
        // bar is driven by the breath reserve.
        player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
        {
            FoodData food = player.getFoodData();

            // Vanilla deals starvation damage at 0 and disables sprinting at <= 6, so they're clamped to (20, 7).
            // We do NOT want vanilla's starvation fighting Paramedicine's own sprinting gate.
            float span = 20 - 7;
            int shown = Math.round(7 + span * (1f - data.getHunger()));

            food.setFoodLevel(Math.max(7, Math.min(20, shown)));
            food.setSaturation(0f);
            food.setExhaustion(0f);
        });
    }

    // === PERSISTENCE STUFF ===

    // Copies health data on death and respawn so values persist through death.
    // (Or resets, it really depends on how things turn out)
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
        event.getOriginal().reviveCaps();

        event.getOriginal().getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(oldData ->
                event.getEntity().getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(newData ->
                {
                    if (event.isWasDeath())
                    {
                        // Death is a clean slate for now. I'll add stuff to it once
                        // i get around to doing the Give Up functionality.
                        newData.resetToDefaults();
                    }
                    else
                    {
                        // Dimension change. Preserve everything.
                        newData.copyFrom(oldData);
                    }
                }));

        event.getOriginal().invalidateCaps();
    }

    // Syncs to client when a player enters a level (either spawn or dimension jump).
    @SubscribeEvent
    public static void onPlayerJoinLevel(EntityJoinLevelEvent event)
    {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;
        if(event.getLevel().isClientSide()) return;

        player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(PlayerHealthData::markDirty);
    }
}
