package net.invinciblemoebius.traumaparamedicinemod.status;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public final class StatusBuilder
{
    private StatusBuilder(){}
    public static PlayerStatus build(PlayerHealthData data, ObservabilityLevel viewerLevel)
    {
        Set<Condition> activeConditions = EnumSet.noneOf(Condition.class);

        for (Condition condition : Condition.values())
        {
            try
            {
                if (condition.evaluate(data))
                    activeConditions.add(condition);
            }
            catch (Exception e)
            {
                // Uhmnnbbn. Debug log or something.
                // I don't have a logger yet, so imagine there's information here.
            }
        }

        return new PlayerStatus(activeConditions, viewerLevel);
    }
}
