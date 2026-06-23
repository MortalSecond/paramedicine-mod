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
// 3. Perfusion pass. Moves whole blood distally-to-proximally.
// 4. Advect substances and surplus blood products distal-to-proximal.
// 5. Hemothorax sync. Updates lung content from viscera-level chest wounds.
// 6. Recompute aggregated pain.
// 7. Tick pain shock and septic shock.
// 8. Tick immune system and sepsis progression.
// 9. Tick stamina and energy.
// 10. Recompute respiratory drive.
// 11. Recompute actual respiratory rate.
// 12. Reset transient modifiers.
// 13. Tick substances. Applies medical effects, decays concentrations.
// 14. Recompute blood volume.
// 15. Recompute blood composition/hematocrit and the resulting blood viscosity.
// 16. Tick respiratory rate's effect on oxygenation.
// 17. Recompute core temperature.
// 18. Recompute heart rate.
// 19. Recompute blood pressure.
// 20. Recompute consciousness.
// 21. Tick fibrillations.
// 22. Recompute total health on all limbs.
// 23. Sync and dispatch the packet if marked dirty.
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
        advectFluids(data, limbs, dt);
        syncHemothorax(data, limbs);
        data.recomputeAgreggatedPain();
        data.tickPainShock(dt);
        data.tickImmuneSystem(dt);
        data.tickSepticShock(dt);
        data.tickStamina(player.isSprinting(), data.consumeJumpFlag(), dt);
        data.tickEnergy(dt);
        data.resetTransientModifiers();
        tickAllSubstances(data, limbs);
        data.recomputeBloodVolume();
        data.recomputeHematocritAndViscosity();
        data.recomputeRespiratoryDrive();
        data.recomputeActualRespiratoryRate(isUnderwater);
        data.tickRespiratorySpO2Effect();
        data.tickCoreTemperature(dt);
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
                        data.getNutritionLevel(),
                        data.computeSystemicClottingFactor()
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
                    data.getNutritionLevel(),
                    data.computeSystemicClottingFactor()
            );

            limb.setLastNetBleedRateML(netBleedPerSecond);

            if (netBleedPerSecond <= 0f)
                continue;

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

            transferWholeBlood(proximalLimb, limb, actualFlow);
        }
    }

    // STEP 4: VENOUS RETURN.
    // Surplus above resting drains distal to proximal, carrying dissolved substances.
    // Compartmentalized nodes keep their substances and fluid.
    private static void advectFluids(PlayerHealthData data, Map<LimbNode, LimbData> limbs, float dt)
    {
        for (LimbNode node: DISTAL_TO_PROXIMAL)
        {
            if (node == LimbNode.UPPER_TORSO)
                continue;

            LimbData limb = limbs.get(node);
            if (limb == null)
                continue;
            if (!limb.hasProximalCirculation(node, limbs))
                continue;

            LimbNode proximalNode = node.proximalNode;
            if (proximalNode == null)
                continue;

            LimbData proximalLimb = limbs.get(proximalNode);
            if (proximalLimb == null)
                continue;

            float total = limb.getActualBloodVolume();
            float surplus = total - limb.getRestingBloodVolume();

            // Bulk fluid. Surplus rebalances.
            if (surplus > 0f && total > 0f)
            {
                float flow = Math.min(surplus, limb.getVenousReturnRate() * dt);
                transferWholeBlood(limb, proximalLimb, flow);
            }

            // Substances. Ride the perfusion.
            if (total > 0f && !limb.getLocalSubstances().isEmpty())
            {
                // 0.25 stands for 25% of a node's substance moves proximal per second.
                // E.g. clears a node in 4 seconds and reaches the torso in 12-ish seconds.
                float fraction = Math.min(1f, 0.25f * dt);
                // Substances reaching the torso enter the systemic pool,
                // otherwise they move up one node.
                List<CirculatingSubstance> destination = (proximalNode == LimbNode.UPPER_TORSO) ? data.getSubstancesInternal() : proximalLimb.getLocalSubstances();
                migrateSubstance(limb.getLocalSubstances(), destination, fraction);
            }
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

    private static void transferWholeBlood(LimbData from, LimbData to, float ml)
    {
        float available = from.getActualBloodVolume();
        if (available <= 0f || ml <= 0f)
            return;

        float fraction = Math.min(1f, ml / available);
        float plasmaMoved = from.getPlasmaVolume() * fraction;
        float cellsMoved = from.getRedCellVolume() * fraction;
        from.setPlasmaVolume(from.getPlasmaVolume() - plasmaMoved);
        from.setRedCellVolume(from.getRedCellVolume() - cellsMoved);
        to.setPlasmaVolume(to.getPlasmaVolume() + plasmaMoved);
        to.setRedCellVolume(to.getRedCellVolume() + cellsMoved);
    }

    // STEP 5: HEMOTHORAX SYNC.
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

    // STEP 13: SUBSTANCES.
    private static void tickAllSubstances(PlayerHealthData data, Map<LimbNode, LimbData> limbs)
    {
        float dt = ModConstants.SECONDS_PER_TICK;

        for (Map.Entry<LimbNode, LimbData> entry : limbs.entrySet())
        {
            LimbData limb = entry.getValue();
            List<CirculatingSubstance> local = limb.getLocalSubstances();
            if (local.isEmpty())
                continue;

            float localVolume = limb.getActualBloodVolume();
            local.removeIf(substance -> substance.tick(localVolume, dt, data, limb));
        }

        List<CirculatingSubstance> systemic = data.getSubstancesInternal();
        if (!systemic.isEmpty())
        {
            float totalVolume = 0f;
            for (LimbData limb : limbs.values())
                totalVolume += limb.getActualBloodVolume();

            final float finalTotalVolume = totalVolume;
            systemic.removeIf(substance -> substance.tick(finalTotalVolume, dt, data, null));
        }
    }

    // STEP 22: LIMB HEALTH RECOMPUTE
    private static void recomputeAllLimbHealth(PlayerHealthData data, Map<LimbNode, LimbData> limbs)
    {
        for (Map.Entry<LimbNode, LimbData> entry: limbs.entrySet())
            entry.getValue().recomputeTotalHealth(entry.getKey(), limbs);
    }

    // STEP 23: SYNC
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
                    net.invinciblemoebius.traumaparamedicinemod.network.ClientboundSyncDetailPacket.fromData(data),
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
