package net.invinciblemoebius.traumaparamedicinemod.health;

// This is the central loop for Paramedicine's health system.
// It is NOT the health sytem itself. If you're looking for the physiological processes,
// go to PlayerHealthData or CirculatingSubstance or Wound or LimbData.
// The point of this particular class is JUST to orchestrate the per-tick updates in a fixed order.
// Do NOT reorder without understanding the dependencies because each step feeds the next. Ordering isn't arbitrary.

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbTraversal;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LungData;
import net.invinciblemoebius.traumaparamedicinemod.network.packets.ClientboundSyncHealthPacket;
import net.invinciblemoebius.traumaparamedicinemod.network.ModNetwork;
import net.invinciblemoebius.traumaparamedicinemod.network.packets.ClientboundSyncDetailPacket;
import net.invinciblemoebius.traumaparamedicinemod.substance.CirculatingSubstance;
import net.invinciblemoebius.traumaparamedicinemod.wound.BleedContext;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundDepth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

import java.util.*;

// STEP ORDER:
// 1. Tick the cardiac phase and build the bleed context for wounds.
// 2. Tick wounds. Advance lifecycle, infection, dressing age, etc.
// 3. Measure bleeding. Sum each wound's loss and update the display rate (no draining here).
// 4. Apply loss and redistribute. Remove the loss from the connected pool, re-pour by priority.
// 5. Migrate substances proximally, at a perfusion-scaled rate.
// 6. Hemothorax sync. Updates lung content from viscera-level chest wounds.
// 7. Recompute aggregated pain.
// 8. Tick pain shock and septic shock.
// 9. Tick immune system and sepsis progression.
// 10. Tick stamina and energy.
// 11. Recompute respiratory drive.
// 12. Recompute actual respiratory rate.
// 13. Reset transient modifiers.
// 14. Tick gastric absorption. Empties the stomach buffer. The bioavailable fraction crosses to blood.
// 15. Tick substances. Applies medical effects, decays concentrations.
// 16. Recompute blood volume.
// 17. Recompute blood composition/hematocrit and the resulting blood viscosity.
// 18. Tick respiratory rate's effect on oxygenation.
// 19. Recompute core temperature.
// 20. Recompute heart rate.
// 21. Recompute blood pressure.
// 22. Recompute consciousness.
// 23. Tick fibrillations.
// 24. Recompute total health on all limbs.
// 25. Sync and dispatch the packet if marked dirty.
@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HealthTickSystem
{
    public static BleedContext bleedCtx;

    // === TRAVERSAL ORDER ===
    // DISTAL TO PROXIMAL. This is because i wanted to simulate homeostasis cascades and
    // hardcoding nodes proximal-to-distal like other mods seemed very unfun.
    private static final LimbNode[] DISTAL_TO_PROXIMAL =
            {
                    // Terminal nodes, nothing distal to pull from.
                    LimbNode.LEFT_HAND, LimbNode.RIGHT_HAND,
                    LimbNode.LEFT_FOOT, LimbNode.RIGHT_FOOT,
                    LimbNode.HEAD,

                    // Mid-limb nodes.
                    LimbNode.LEFT_FOREARM, LimbNode.RIGHT_FOREARM,
                    LimbNode.LEFT_LOWER_LEG, LimbNode.RIGHT_LOWER_LEG,
                    LimbNode.NECK,

                    // Proximal limb nodes.
                    LimbNode.LEFT_UPPER_ARM, LimbNode.RIGHT_UPPER_ARM,
                    LimbNode.LEFT_UPPER_LEG, LimbNode.RIGHT_UPPER_LEG,

                    // Trunk. Proximal to distal. Upper torso (heart) always last.
                    LimbNode.GROIN,
                    LimbNode.LOWER_TORSO,
                    LimbNode.UPPER_TORSO
            };

    // === BLOOD PRIORITY ===
    // Blood fills these nodes top-down to their resting volume each tick, so deficits land on
    // the lowest-priority nodes first and the heart is defended until total volume falls
    // below the torso's own resting need. LOWER_TORSO sits last, because IRL
    // the splanchnic reservoir is the body's first sacrifice in shock.
    private static final LimbNode[] PRIORITY_HIGH_TO_LOW =
            {
                    LimbNode.UPPER_TORSO,
                    LimbNode.HEAD,
                    LimbNode.NECK,
                    LimbNode.GROIN,
                    LimbNode.LEFT_UPPER_LEG, LimbNode.RIGHT_UPPER_LEG,
                    LimbNode.LEFT_UPPER_ARM, LimbNode.RIGHT_UPPER_ARM,
                    LimbNode.LEFT_LOWER_LEG, LimbNode.RIGHT_LOWER_LEG,
                    LimbNode.LEFT_FOREARM, LimbNode.RIGHT_FOREARM,
                    LimbNode.LEFT_HAND, LimbNode.RIGHT_HAND,
                    LimbNode.LEFT_FOOT, LimbNode.RIGHT_FOOT,
                    LimbNode.LOWER_TORSO
            };

    // === EVENT ENTRY POINT ===
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        // Serverside only, end of tick only, real players only.
        if (event.phase != TickEvent.Phase.END)
            return;
        if (event.player.level().isClientSide())
            return;
        if (!(event.player instanceof ServerPlayer sp))
            return;

        // Creative and spectator players aren't simulated.
        if (sp.isCreative() || sp.isSpectator())
            return;

        sp.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data -> tickPlayer(sp, data));
    }

    // === MAIN PER-PLAYER UPDATE ===
    private static void tickPlayer(ServerPlayer player, PlayerHealthData data)
    {
        float dt = ModConstants.SECONDS_PER_TICK;
        Map<LimbNode, LimbData> limbs = data.getLimbsInternal();
        boolean isUnderwater = player.isEyeInFluid(FluidTags.WATER);

        data.tickCardiacPhase(dt);
        bleedCtx = new BleedContext(data.getMAP(), data.getSystolicBP(), data.getDiastolicBP(),
                data.getCardiacPhase(), data.hasPulse(), data.getCoreTemperature(), data.getOxygenSaturation(),
                data.getNutritionLevel(), data.computeSystemicClottingFactor());
        tickAllWounds(data, limbs, dt);
        float bleedTotal = computeBleeding(limbs, dt);
        redistributeBlood(limbs, bleedTotal);
        float perfusionFactor = Math.max(0f, Math.min(ModConstants.MAX_PERFUSION_FACTOR, data.getMAP() / ModConstants.NORMAL_MAP_MMHG));
        migrateSubstances(data, limbs, perfusionFactor, dt);
        syncHemothorax(data, limbs);
        data.recomputeAgreggatedPain();
        data.tickPainShock(dt);
        data.tickImmuneSystem(dt);
        data.tickSepticShock(dt);
        data.tickStamina(player.isSprinting(), data.consumeJumpFlag(), dt);
        data.tickEnergy(dt);
        data.resetTransientModifiers();
        data.tickGastricAbsorption(dt);
        tickAllSubstances(data, limbs, perfusionFactor);
        data.tickNausea(dt);
        tickVomit(player, data);
        data.recomputeBloodVolume();
        data.recomputeHematocritAndViscosity();
        data.recomputeRespiratoryDrive();
        data.recomputeActualRespiratoryRate(isUnderwater);
        data.tickRespiratorySpO2Effect();
        data.tickCoreTemperature(dt);
        data.recomputeHeartRate();
        data.recomputeBloodPressure();
        data.recomputeConsciousness();
        data.tickCardiacRhythm(dt);
        recomputeAllLimbHealth(data, limbs);
        syncIfDirty(player, data);

        // === VANILLA INTERACTIONS ===
        if (isUnderwater)
            suppressVanillaDrowning(player);
    }

    // STEP 2: WOUND TICKING.
    private static void tickAllWounds(PlayerHealthData data, Map<LimbNode, LimbData> limbs, float dt)
    {
        float immunity = data.getImmunity();

        for (LimbData limb: limbs.values())
        {
            List<Wound> wounds = limb.getWounds();
            boolean syncDirty = false;

            for (Wound wound: wounds)
            {
                wound.tickClotting(bleedCtx, dt);
                if (wound.tickAdvance())
                    syncDirty = true;
            }

            limb.recomputeRawPain();

            if (syncDirty) limb.markDirty();
        }
    }

    // STEP 3: BLEEDING.
    // Sums each connected wound's loss for this tick and updates the smoothed display rate. Does NOT
    // drain here. A wound bleeds the connected circulation, not its own node's cup, so the actual
    // removal happens in redistribution (step 4). computeNetBleedRate already returns 0 for nodes with
    // no proximal circulation, so tourniqueted limbs contribute nothing. Returns total mL lost this tick.
    private static float computeBleeding(Map<LimbNode, LimbData> limbs, float dt)
    {
        float bleedTotal = 0f;
        for (Map.Entry<LimbNode, LimbData> entry : limbs.entrySet())
        {
            float instant = entry.getValue().computeNetBleedRate(entry.getKey(), limbs, bleedCtx);
            entry.getValue().updateBleedDisplay(instant);
            if (instant > 0f)
                bleedTotal += instant * dt;
        }
        return bleedTotal;
    }

    // STEP 4: BLEED LOSS + PRIORITY REDISTRIBUTION.
    // The connected circulation is one well-mixed pool. This tick's loss is removed from it at its
    // current composition, and the remainder is re-poured into nodes in PRIORITY_HIGH_TO_LOW order,
    // each filled to resting before the next gets anything. So loss falls on the lowest-priority nodes
    // first (gut, then extremities) and the heart is defended until total volume drops below its own
    // resting need. Disconnected nodes (tourniquet/severed) are isolated and left untouched.
    private static void redistributeBlood(Map<LimbNode, LimbData> limbs, float bleedTotal)
    {
        // Gather the connected pool, in priority order.
        List<LimbNode> connected = new ArrayList<>();
        float poolPlasma = 0f, poolCells = 0f, connectedResting = 0f;
        for (LimbNode node : PRIORITY_HIGH_TO_LOW)
        {
            if (!LimbTraversal.hasProximalCirculation(node, limbs))
                continue;

            LimbData limb = limbs.get(node);
            connected.add(node);
            poolPlasma += limb.getPlasmaVolume();
            poolCells += limb.getRedCellVolume();
            connectedResting += limb.getRestingBloodVolume();
        }
        float poolTotal = poolPlasma + poolCells;
        if (poolTotal <= 0f)
            return;

        // Remove this tick's loss at the pool's current composition (whole blood leaves the wound).
        if (bleedTotal > 0f)
        {
            float lost = Math.min(bleedTotal, poolTotal);
            float cellFrac = poolCells / poolTotal;
            poolCells = Math.max(0f, poolCells - lost * cellFrac);
            poolPlasma = Math.max(0f, poolPlasma - lost * (1f - cellFrac));
            poolTotal = poolPlasma + poolCells;
        }

        // Priority waterfall. Fill each connected node to resting, top of the list down.
        float remaining = poolTotal;
        Map<LimbNode, Float> target = new EnumMap<>(LimbNode.class);
        for (LimbNode node : connected)
        {
            float give = Math.min(limbs.get(node).getRestingBloodVolume(), remaining);
            target.put(node, give);
            remaining -= give;
        }
        // Hypervolemia. Blood beyond everyone's resting spreads proportionally above resting.
        if (remaining > 0f && connectedResting > 0f)
            for (LimbNode node : connected)
                target.merge(node, remaining * (limbs.get(node).getRestingBloodVolume() / connectedResting), Float::sum);

        // Apply each target at the pool's uniform composition (connected blood mixes).
        float plasmaFrac = poolTotal > 0f ? poolPlasma / poolTotal : 0f;
        float cellFrac   = poolTotal > 0f ? poolCells  / poolTotal : 0f;
        for (LimbNode node : connected)
        {
            float t = target.get(node);
            limbs.get(node).setPlasmaVolume(t * plasmaFrac);
            limbs.get(node).setRedCellVolume(t * cellFrac);
        }
    }

    // STEP 5: SUBSTANCE MIGRATION.
    // Dissolved substances ride the circulation one node proximal per tick, at a rate that scales with
    // perfusionFactor so transfusions accelerate under pressure (epi), slow under vasodilation (morphine),
    // and stop at asystole or death.
    // This pass carries only substances. Compartmentalized nodes keep theirs.
    private static void migrateSubstances(PlayerHealthData data, Map<LimbNode, LimbData> limbs, float perfusionFactor, float dt)
    {
        float fraction = Math.min(1f, ModConstants.BASE_ADVECTION_RATE * perfusionFactor * dt);
        if (fraction <= 0f)
            return;

        for (LimbNode node : DISTAL_TO_PROXIMAL)
        {
            if (node == LimbNode.UPPER_TORSO)
                continue;

            LimbData limb = limbs.get(node);
            if (limb == null)
                continue;
            if (!LimbTraversal.hasProximalCirculation(node, limbs))
                continue;
            if (limb.getActualBloodVolume() <= 0f)
                continue;
            if (limb.getLocalSubstances().isEmpty())
                continue;

            LimbNode proximalNode = node.proximalNode;
            if (proximalNode == null)
                continue;

            LimbData proximalLimb = limbs.get(proximalNode);
            if (proximalLimb == null)
                continue;

            List<CirculatingSubstance> destination = (proximalNode == LimbNode.UPPER_TORSO)
                    ? data.getSubstancesInternal()
                    : proximalLimb.getLocalSubstances();

            migrateSubstance(limb.getLocalSubstances(), destination, fraction);
        }
    }

    // Moves a little bit of each substance and merges them, so concentrations stay additive.
    // Oh, and one record per location too.
    private static void migrateSubstance(List<CirculatingSubstance> source, List<CirculatingSubstance> destination, float fraction)
    {
        Iterator<CirculatingSubstance> iterator = source.iterator();
        while (iterator.hasNext())
        {
            CirculatingSubstance substance = iterator.next();
            float moved = substance.getAmountML() * fraction;
            if (moved <= CirculatingSubstance.NEGLIGIBLE_THRESHOLD)
                continue;
            substance.reduceAmount(moved);

            CirculatingSubstance existing = null;
            for (CirculatingSubstance destinationSubstance : destination)
            {
                if (destinationSubstance.getType() == substance.getType())
                {
                    existing = destinationSubstance;
                    break;
                }
            }

            if (existing != null)
            {
                float totalAmount = existing.getAmountML() + moved;
                float blendedAge = (existing.getAmountML() * existing.getAgeSeconds() + moved * substance.getAgeSeconds()) / totalAmount;
                existing.setAmountML(totalAmount);
                existing.setAgeSeconds(blendedAge);
            }
            else
            {
                destination.add(substance.splitOff(moved));
            }

            if (substance.getAmountML() < CirculatingSubstance.NEGLIGIBLE_THRESHOLD)
                iterator.remove();
        }
    }

    // STEP 6: HEMOTHORAX SYNC.
    // Each VISCERAL wound on UPPER_TORSO contributes its bleed rate to one lung.
    private static void syncHemothorax(PlayerHealthData data, Map<LimbNode, LimbData> limbs)
    {
        float dt = ModConstants.SECONDS_PER_TICK;
        LimbData torso = limbs.get(LimbNode.UPPER_TORSO);
        if (torso == null) return;

        for (Wound wound: torso.getWounds())
        {
            // Only visceral wounds bleed into the pleural cavity.
            if (wound.getDepth() != WoundDepth.VISCERAL)
                continue;

            float mlThisTick = wound.getBleedRateML() * dt;
            if (wound.isRightSide())
                data.getRightLung().addBlood(mlThisTick);
            else
                data.getLeftLung().addBlood(mlThisTick);
        }

        // Sepsis exudate. It precedes actual sepsis in Paramedicine so it works more
        // like a tell than just regular pneumonia. Bilateral.
        float pulmonaryLoad = Math.max(data.getBacteremia() * 0.5f, data.getSepticShock());
        if (pulmonaryLoad > 0.05f)
        {
            float fluidRate = pulmonaryLoad * 0.8f * dt;
            data.getLeftLung().addFluid(fluidRate);
            data.getRightLung().addFluid(fluidRate);
        }

        // Tension pneumothorax if the lung has significant air AND no open occlusive-dressed wound.
        // TODO: Query wound dressing state once treatment system is built.
        // For now, flag pneumo when airML exceeds 20% of max capacity.
        float leftAirFraction = data.getLeftLung().getAirML() / LungData.MAX_AIR_ML;
        float rightAirFraction = data.getRightLung().getAirML() / LungData.MAX_AIR_ML;
        if (leftAirFraction  > 0.20f)
            data.getLeftLung().setTensionPneumothorax(true);
        if (rightAirFraction > 0.20f)
            data.getRightLung().setTensionPneumothorax(true);

        data.getLeftLung().decayBlood(0.05f, dt);
        data.getRightLung().decayBlood(0.05f, dt);
        data.getLeftLung().decayAir(0.03f, dt);
        data.getRightLung().decayAir(0.03f, dt);
        data.getLeftLung().decayFluid(0.03f, dt);
        data.getRightLung().decayFluid(0.03f, dt);
    }

    // STEP 15: SUBSTANCES.
    private static void tickAllSubstances(PlayerHealthData data, Map<LimbNode, LimbData> limbs, float perfusionFactor)
    {
        float dt = ModConstants.SECONDS_PER_TICK;

        for (Map.Entry<LimbNode, LimbData> entry : limbs.entrySet())
        {
            LimbData limb = entry.getValue();
            List<CirculatingSubstance> local = limb.getLocalSubstances();
            if (local.isEmpty())
                continue;

            float localVolume = limb.getActualBloodVolume();
            local.removeIf(substance -> substance.tick(localVolume, dt, perfusionFactor, data, limb));
        }

        List<CirculatingSubstance> systemic = data.getSubstancesInternal();
        if (!systemic.isEmpty())
        {
            float totalVolume = 0f;
            for (LimbData limb : limbs.values())
                totalVolume += limb.getActualBloodVolume();

            final float finalTotalVolume = totalVolume;
            systemic.removeIf(substance -> substance.tick(finalTotalVolume, dt, perfusionFactor, data, null));
        }
    }

    // STEP 17: VOMIT.
    // Rolls for a vomiting episode when nausea is high enough.
    private static void tickVomit(ServerPlayer player, PlayerHealthData data)
    {
        if (data.getNausea() < ModConstants.NAUSEA_VOMIT_THRESHOLD)
            return;

        float severity = (data.getNausea() - ModConstants.NAUSEA_VOMIT_THRESHOLD) / (1f - ModConstants.NAUSEA_VOMIT_THRESHOLD);
        float chance = severity * ModConstants.NAUSEA_VOMIT_CHANCE_PER_SECOND * ModConstants.SECONDS_PER_TICK;

        if (player.getRandom().nextFloat() >= chance)
            return;

        float volume = data.triggerVomit();
    }

    // STEP 24: LIMB HEALTH RECOMPUTE
    private static void recomputeAllLimbHealth(PlayerHealthData data, Map<LimbNode, LimbData> limbs)
    {
        for (Map.Entry<LimbNode, LimbData> entry: limbs.entrySet())
            entry.getValue().recomputeTotalHealth(entry.getKey(), limbs);
    }

    // STEP 25: SYNC
    private static void syncIfDirty(ServerPlayer player, PlayerHealthData data)
    {
        if (!data.consumeSyncFlag())
            return;

        ModNetwork.CHANNEL.sendTo(
                ClientboundSyncHealthPacket.fromData(data),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );

        // Heavy detail stream: only while this player is inspecting themselves (self-view).
        // Theoretically, a medic will have a different view of a different player. More limited.
        if (net.invinciblemoebius.traumaparamedicinemod.network.InspectionTracker.isViewingSelf(player.getUUID(), player.getId()))
        {
            ModNetwork.CHANNEL.sendTo(
                    ClientboundSyncDetailPacket.fromData(data),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
            );
        }
    }

    // === VANILLA SUPPRESSION ===

    // Keeps vanilla air supply at max so Minecraft never triggers its own drowning damage.
    private static void suppressVanillaDrowning(ServerPlayer player)
    {
        player.setAirSupply(player.getMaxAirSupply());
    }
}
