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
import net.invinciblemoebius.traumaparamedicinemod.limbs.LungData;
import net.invinciblemoebius.traumaparamedicinemod.network.ClientboundSyncHealthPacket;
import net.invinciblemoebius.traumaparamedicinemod.network.ModNetwork;
import net.invinciblemoebius.traumaparamedicinemod.substance.CirculatingSubstance;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundDepth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

// STEP ORDER:
// 1. Tick wounds. Advance lifecycle, infection, dressing age, etc.
// 2. Apply hemorrhage. Drain blood volume from wound bleed rates.
// 3. Perfusion pass. Redistribute blood distally-to-proximally.
// 4. Hemothorax sync. Updates lung content from viscera-level chest wounds.
// 5. Recompute blood volume.
// 6. Recompute aggregated pain.
// 7. Tick pain shock and septic shock.
// 8. Tick immune system and sepsis progression.
// 9. Tick stamina and energy.
// 10. Tick substances. Applies medical effects, decays concentrations.
// 11. Recompute respiratory drive.
// 12. Recompute actual respiratory rate.
// 13. Tick respiratory rate's effect on oxygenation.
// 14. Recompute heart rate.
// 15. Recompute blood pressure.
// 16. Recompute consciousness.
// 17. Tick fibrillations.
// 18. Recompute total health on all limbs.
// 19. Sync and dispatch the packet if marked dirty.
@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HealthTickSystem
{
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

    // === EVENT ENTRY POINT ===
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        // Serverside only, end of tick only, real players only.
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        // Creative and spectator players aren't simulated.
        if (sp.isCreative() || sp.isSpectator()) return;

        sp.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data -> tickPlayer(sp, data));
    }

    // === MAIN PER-PLAYER UPDATE ===
    private static void tickPlayer(ServerPlayer player, PlayerHealthData data)
    {
        float dt = ModConstants.SECONDS_PER_TICK;
        Map<LimbNode, LimbData> limbs = data.getLimbsInternal();
        boolean isUnderwater = player.isEyeInFluid(FluidTags.WATER);

        tickAllWounds(data, limbs, dt);
        applyHemorrhage(data, limbs, dt);
        runPerfusionPass(limbs, dt);
        syncHemothorax(data, limbs);
        data.recomputeBloodVolume();
        data.recomputeAgreggatedPain();
        data.tickPainShock(dt);
        data.tickImmuneSystem(dt);
        data.tickSepticShock(dt);
        data.tickStamina(player.isSprinting(), data.consumeJumpFlag(), dt);
        data.tickEnergy(dt);
        tickSubstances(data, limbs);
        data.recomputeRespiratoryDrive();
        data.recomputeActualRespiratoryRate(isUnderwater);
        data.tickRespiratorySpO2Effect();
        data.recomputeHeartRate();
        data.recomputeBloodPressure();
        data.recomputeConsciousness();
        data.tickFibrillations();
        recomputeAllLimbHealth(data, limbs);
        syncIfDirty(player, data);


        // === VANILLA INTERACTIONS ===
        if (isUnderwater)
            suppressVanillaDrowning(player);
    }

    // STEP 1: WOUND TICKING.
    private static void tickAllWounds(PlayerHealthData data, Map<LimbNode, LimbData> limbs, float dt)
    {
        float immunity = data.getImmunity();

        for (LimbData limb: limbs.values())
        {
            List<Wound> wounds = limb.getWounds();
            boolean syncDirty = false;

            for (Wound wound: wounds)
            {
                float clottingRate = wound.computeClottingRate(
                        data.getCoreTemperature(),
                        data.getOxygenSaturation(),
                        data.getNutritionLevel()
                );

                boolean changed = wound.tickAdvance(clottingRate);
                if (changed)
                    syncDirty = true;
            }

            limb.recomputeRawPain();

            if (syncDirty) limb.markDirty();
        }
    }

    private static void drainProximally(LimbNode node, float mlToDrain, Map<LimbNode, LimbData> limbs)
    {
        if (mlToDrain <= 0f) return;

        LimbData limb = limbs.get(node);
        if (limb == null) return;

        float available = limb.getActualBloodVolume();

        if (available >= mlToDrain)
        {
            limb.setActualBloodVolume(available -  mlToDrain);
            return;
        }

        // Local volume insufficient, drain what's here, pass remains up.
        limb.setActualBloodVolume(0f);
        float remains = mlToDrain - available;

        // Only propagate if this node has proximal circulation.
        if (node.proximalNode != null && limb.isCirculatingProximally())
            drainProximally(node.proximalNode, remains, limbs);
    }

    // STEP 2: HEMORRHAGE.
    private static void applyHemorrhage(PlayerHealthData data, Map<LimbNode, LimbData> limbs, float dt)
    {
        for (Map.Entry<LimbNode, LimbData> entry: limbs.entrySet())
        {
            LimbNode node = entry.getKey();
            LimbData limb = entry.getValue();

            float netBleedPerSecond = limb.computeNetBleedRate(
                    node, limbs,
                    data.getCoreTemperature(),
                    data.getOxygenSaturation(),
                    data.getNutritionLevel()
            );

            if (netBleedPerSecond <= 0f) continue;

            float mlLostThisTick = netBleedPerSecond * dt;
            drainProximally(node, mlLostThisTick, limbs);
        }
    }

    // STEP 3: PERFUSION.
    // Each node that has a deficit below its resting volume requests blood from its neighbor,
    // up to the node's perfusion rate. The proximal node's volume is reduced by that same amount,
    // propagating deficit toward the trunk.
    // Nodes without proximal circulation (tourniquet or severed vessel) skip the request.
    // There is also a rate limiting guardrail. To prevent a heavily bleeding node from
    // completely draining its neighbor in one tick.
    private static void runPerfusionPass(Map<LimbNode, LimbData> limbs, float dt)
    {
        for (LimbNode node: DISTAL_TO_PROXIMAL)
        {
            LimbData limb = limbs.get(node);
            if (limb == null) continue;

            // No proximal neighbor to pull from.
            if (node.proximalNode == null) continue;
            if (!limb.hasProximalCirculation(node, limbs)) continue;

            float deficit = limb.getRestingBloodVolume() - limb.getActualBloodVolume();
            // No pull needed if it's already at or above resting.
            if (deficit <= 0f) continue;

            LimbData proximalLimb = limbs.get(node.proximalNode);
            if (proximalLimb == null) continue;

            // How much can flow this tick, limited by perfusion rate and by how
            // much the proximal node has above its critical floor (20% resting volume).
            float proximalFloor = proximalLimb.getRestingBloodVolume() * 0.20f;
            float proximalSurplus = proximalLimb.getActualBloodVolume() - proximalFloor;
            if (proximalSurplus <= 0f) continue;

            float maxFlowThisTick = limb.getPerfusionRate() * dt;
            float actualFlow = Math.min(deficit, Math.min(maxFlowThisTick, proximalSurplus));

            limb.setActualBloodVolume(limb.getActualBloodVolume() + actualFlow);
            proximalLimb.setActualBloodVolume(proximalLimb.getActualBloodVolume() - actualFlow);
        }
    }

    // STEP 4: HEMOTHORAX SYNC.
    // Each VISCERAL wound on UPPER_TORSO contributes its bleed rate to one lung.
    private static void syncHemothorax(PlayerHealthData data, Map<LimbNode, LimbData> limbs)
    {
        float dt = ModConstants.SECONDS_PER_TICK;
        LimbData torso = limbs.get(LimbNode.UPPER_TORSO);
        if (torso == null) return;

        for (Wound wound: torso.getWounds())
        {
            // Only visceral wounds bleed into the pleural cavity.
            if (wound.getDepth() != WoundDepth.VISCERAL) continue;

            float mlThisTick = wound.getBleedRateML() * dt;
            // TODO: Split rate between left and right lung based on wound side.
            data.getLeftLung().addBlood(mlThisTick);
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
    }

    // STEP 9: SUBSTANCES
    private static void tickSubstances(PlayerHealthData data, Map<LimbNode, LimbData> limbs)
    {
        List<CirculatingSubstance> substances = data.getSubstancesInternal();
        if (substances.isEmpty()) return;

        Iterator<CirculatingSubstance> it = substances.iterator();
        while (it.hasNext())
        {
            CirculatingSubstance substance = it.next();
            boolean eliminated = substance.tick(data, limbs);
            if (eliminated) it.remove();
        }
    }

    // STEP 17: LIMB HEALTH RECOMPUTE
    private static void recomputeAllLimbHealth(PlayerHealthData data, Map<LimbNode, LimbData> limbs)
    {
        for (Map.Entry<LimbNode, LimbData> entry: limbs.entrySet())
            entry.getValue().recomputeTotalHealth(entry.getKey(), limbs);
    }

    // STEP 18: SYNC
    private static void syncIfDirty(ServerPlayer player, PlayerHealthData data)
    {
        if (!data.consumeSyncFlag()) return;

        ModNetwork.CHANNEL.sendTo(
                ClientboundSyncHealthPacket.fromData(data),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );
    }

    // === VANILLA SUPPRESSION ===

    // Keeps vanilla air supply at max so Minecraft never triggers its own drowning damage.
    private static void suppressVanillaDrowning(ServerPlayer player)
    {
        player.setAirSupply(player.getMaxAirSupply());
    }
}
