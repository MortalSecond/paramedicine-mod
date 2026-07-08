package net.invinciblemoebius.traumaparamedicinemod.interactions;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.network.packets.ClientboundInteractionFeedbackPacket;
import net.invinciblemoebius.traumaparamedicinemod.network.ModNetwork;
import net.invinciblemoebius.traumaparamedicinemod.network.packets.ClientboundPulseReadingPacket;
import net.invinciblemoebius.traumaparamedicinemod.treatment.Treatments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;

// The physiology behind each action. Server-authoritative — only the server has true BP.
// Dispatches on NodeAction; knows nothing about menus or packets.
public final class NodeInteractions
{
    private NodeInteractions() {}

    public static void handle(ServerPlayer player, PlayerHealthData data, LimbNode node, NodeAction action, int woundId)
    {
        switch (action)
        {
            case CHECK_PULSE -> checkPulse(player, data, node);
            case HEAR_PULSE -> hearPulse(player, data);
            case CHECK_BREATHING -> checkBreathing(player, data);
            case HOLD_PRESSURE -> holdPressure(player, data, node);
            case CHEST_COMPRESSIONS -> chestCompressions(player, data);
            case REMOVE_DRESSING -> Treatments.removeDressing(player, data, node, woundId);
        }
    }

    private static void checkPulse(ServerPlayer player, PlayerHealthData data, LimbNode node)
    {
        int threshold = (int) pulseSiteThreshold(node);
        boolean present = data.hasPulse() && data.getSystolicBP() >= threshold;

        feedback(player, present ? "You feel a " + siteName(node) + " pulse."
                : "There is no " + siteName(node) + " pulse.");

        ModNetwork.CHANNEL.sendTo(new ClientboundPulseReadingPacket(threshold, present), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static void hearPulse(ServerPlayer player, PlayerHealthData data)
    {
        if (!data.hasPulse())
        {
            feedback(player, "You hear no heartbeat.");
            return;
        }

        float bpm = data.getHeartRateBPM();
        String rate = bpm > 100f ? "rapid" : bpm < 60f ? "slow" : "steady";
        String rhythm = switch (data.getRhythm())
        {
            case SINUS, SINUS_TACHYCARDIA, SINUS_BRADYCARDIA -> "regular";
            default -> "irregular";
        };

        feedback(player, "The heartbeat is " + rate + " and " + rhythm + ".");
    }

    // Palpation heuristic. Radial = systolic ~80. Femoral = ~70. Carotid = ~60.
    private static float pulseSiteThreshold(LimbNode node)
    {
        return switch (node)
        {
            case NECK -> 60f;
            case GROIN -> 70f;
            default -> 80f; // Radial
        };
    }

    private static String siteName(LimbNode node)
    {
        return switch (node)
        {
            case NECK -> "carotid";
            case GROIN -> "femoral";
            default -> "radial";
        };
    }

    private static void checkBreathing(ServerPlayer player, PlayerHealthData data)
    {
        float rr = data.getActualRespiratoryRate();
        String desc = rr <= 0f ? "not breathing"
                : rr < 8f ? "breathing slowly and shallowly"
                  : rr > 25f ? "breathing fast"
                    : "breathing normally";

        feedback(player, "They are " + desc + ".");
    }

    private static void holdPressure(ServerPlayer player, PlayerHealthData data, LimbNode node)
    {
        // TODO: Real effect (transient bleed suppression) belongs to the treatment system.
        feedback(player, "You apply firm pressure to the wound.");
    }

    private static void chestCompressions(ServerPlayer player, PlayerHealthData data)
    {
        // TODO: Open up the CPR minigame. Upscale the cprCompressionSupport modifier for sustained CPR.
        feedback(player, "You perform a cycle of chest compressions.");
    }

    private static void feedback(ServerPlayer player, String msg)
    {
        ModNetwork.CHANNEL.sendTo(new ClientboundInteractionFeedbackPacket(msg), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}