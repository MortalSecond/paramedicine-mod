package net.invinciblemoebius.traumaparamedicinemod.interactions;

import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;

public record InteractionOption(String label, LimbNode node, NodeAction action, int woundId)
{
    public InteractionOption(String label, LimbNode node, NodeAction action)
    {
        this(label, node, action, -1);
    }
}