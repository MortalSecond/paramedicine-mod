package net.invinciblemoebius.traumaparamedicinemod.limbs;

import java.util.Map;

public final class LimbTraversal
{
    private LimbTraversal() {}

    // Does this node currently receive circulation from the heart? True only if the node itself is
    // circulating proximally AND every node up the chain to the core is distally patent. A tourniquet
    // or severed vessel anywhere on the path returns false.
    public static boolean hasProximalCirculation(LimbNode node, Map<LimbNode, LimbData> limbs)
    {
        LimbData limb = limbs.get(node);
        if (limb == null)
            return false;
        if (!limb.isCirculatingProximally())
            return false;
        // UPPER_TORSO. The core always has central supply.
        if (node.proximalNode == null)
            return true;

        LimbData proximal = limbs.get(node.proximalNode);
        if (proximal == null)
            return false;

        return proximal.isCirculatingDistally() && hasProximalCirculation(node.proximalNode, limbs);
    }
}