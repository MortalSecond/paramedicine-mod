package net.invinciblemoebius.traumaparamedicinemod.interactions;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;

import java.util.ArrayList;
import java.util.List;

// NO physiological logic here. It's the same abstraction from
// WoundingBehavior and SubstanceType. This produces a list of InteractionOption
// that NodeInteractions uses to cause physiological/UI effects.
public class NodeInteractionOptions
{
    private NodeInteractionOptions() {}

    public static List<InteractionOption> forNode(LimbNode node, PlayerHealthData data)
    {
        List<InteractionOption> options = new ArrayList<>();

        // ANATOMY-BASED DEFAULTS.
        switch (node)
        {
            case HEAD ->
            {
                options.add(new InteractionOption("Check Breathing", node, NodeAction.CHECK_BREATHING));
            }
            case NECK ->
            {
                options.add(new InteractionOption("Check Carotid Pulse", node, NodeAction.CHECK_PULSE));
            }
            case GROIN ->
            {
                options.add(new InteractionOption("Check Femoral Pulse", node, NodeAction.CHECK_PULSE));
            }
            case LEFT_HAND, RIGHT_HAND ->
            {
                options.add(new InteractionOption("Check Radial Pulse", node, NodeAction.CHECK_PULSE));
            }
            case UPPER_TORSO ->
            {
                options.add(new InteractionOption("Listen To Heart", node, NodeAction.HEAR_PULSE));
                options.add(new InteractionOption("Chest Compressions", node, NodeAction.CHEST_COMPRESSIONS));
            }
            default -> {}
        }

        // EMERGENT OPTIONS

        // Any wound present in a node shows a Hold Pressure option.
        if (data != null && data.getLimb(node).getLastNetBleedRateML() > 0f)
            options.add(new InteractionOption("Hold Pressure", node, NodeAction.HOLD_PRESSURE));

        return options;
    }

    // Base cast seconds per action; 0 = instant (fires immediately, no timer).
    public static float baseCastSeconds(NodeAction action)
    {
        return switch (action)
        {
            case CHECK_PULSE -> 2f;
            case HEAR_PULSE -> 3f;
            case CHECK_BREATHING -> 2f;

            default -> 0f;
        };
    }

    public static long castDurationMs(NodeAction action, PlayerHealthData self)
    {
        float base = baseCastSeconds(action);
        if (base <= 0f || self == null)
            return 0L;

        float arms = 0.5f * (handCapability(self, LimbNode.LEFT_FOREARM, LimbNode.LEFT_HAND) + handCapability(self, LimbNode.RIGHT_FOREARM, LimbNode.RIGHT_HAND));
        float dexterity = arms * self.getConsciousness();

        return (long) (base * 1000f / Math.max(0.2f, dexterity));
    }

    private static float handCapability(PlayerHealthData d, LimbNode forearm, LimbNode hand)
    {
        return 0.5f * (d.getLimb(forearm).getMuscleHealth() + d.getLimb(hand).getMuscleHealth());
    }
}
