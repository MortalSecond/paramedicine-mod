package net.invinciblemoebius.traumaparamedicinemod.client;

import net.invinciblemoebius.traumaparamedicinemod.interactions.NodeAction;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;

// A timed interaction in progress. Pure client UX: the radial timer renders from this,
// and on completion the screen fires the action packet it was going to fire anyway.
public final class ClientCastState
{
    private ClientCastState() {}

    private static boolean active = false;
    private static LimbNode node;
    private static NodeAction action;
    private static long startAt;
    private static long durationMs;

    // === INTERACTION METHODS ===

    public static void start(LimbNode node, NodeAction action, long durationMs)
    {
        ClientCastState.node = node;
        ClientCastState.action = action;
        ClientCastState.durationMs = durationMs;
        ClientCastState.startAt = System.currentTimeMillis();
        ClientCastState.active = true;
    }

    public static float progress()
    {
        if (!active) return 0f;
        return Math.min(1f, (System.currentTimeMillis() - startAt) / (float) durationMs);
    }

    // === ACCESSORS ===

    public static void cancel() { active = false; }
    public static boolean isActive() { return active; }
    public static LimbNode node() { return node; }
    public static NodeAction action() { return action; }
    public static boolean isComplete() { return active && progress() >= 1f; }
}
