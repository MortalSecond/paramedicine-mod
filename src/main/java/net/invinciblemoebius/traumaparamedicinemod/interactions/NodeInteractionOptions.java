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
}
