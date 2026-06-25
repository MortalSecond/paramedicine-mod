package net.invinciblemoebius.traumaparamedicinemod.interactions;

import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;

public record InteractionOption(String label, LimbNode node, NodeAction action)
{
}
