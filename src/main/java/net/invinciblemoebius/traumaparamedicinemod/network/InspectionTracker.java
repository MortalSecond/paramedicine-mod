package net.invinciblemoebius.traumaparamedicinemod.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InspectionTracker
{
    private static final Map<UUID, Integer> VIEWING = new HashMap<>();

    public static void set(UUID viewer, int targetID)
    {
        VIEWING.put(viewer, targetID);
    }

    public static void clear(UUID viewer)
    {
        VIEWING.remove(viewer);
    }

    public static boolean isViewingSelf(UUID viewer, int selfID)
    {
        Integer targetID = VIEWING.get(viewer);
        return targetID != null && targetID == selfID;
    }
}
