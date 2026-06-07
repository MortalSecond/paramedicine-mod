package net.invinciblemoebius.traumaparamedicinemod.status;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

// Unlike ActiveSubstance or LimbNode, PlayerStatus is a snapshot of ALL
// the active conditions in one player in one moment.
public final class PlayerStatus
{
    private final Set<Condition> activeConditions;
    private final ObservabilityLevel viewerLevel;

    // === CONSTRUCTOR ===
    public PlayerStatus (Set<Condition> activeConditions, ObservabilityLevel viewerLevel)
        {
            this.activeConditions = Collections.unmodifiableSet(activeConditions);
            this.viewerLevel = viewerLevel;
        }

    // Returns true if the condition is true AND visible to the viewer.
    public boolean has(Condition condition)
    {
        if (!activeConditions.contains(condition)) return false;
        return condition.observability.ordinal() <= viewerLevel.ordinal();
    }

    // Returns all active conditions to the viewer, sorted by descending severity.
    public Set<Condition> getVisibleConditions()
    {
        Set<Condition> visibleConditions = EnumSet.noneOf(Condition.class);
        for (Condition condition : activeConditions)
        {
            if (condition.observability.ordinal() <= viewerLevel.ordinal())
                visibleConditions.add(condition);
        }

        return Collections.unmodifiableSet(visibleConditions);
    }

    // Returns the highest severity active condition visible to the viewer.
    public ConditionSeverity getHighestSeverity()
    {
        ConditionSeverity highestSeverity = ConditionSeverity.NEUTRAL;

        for (Condition c : activeConditions)
        {
            if (c.observability.ordinal() > viewerLevel.ordinal())
                continue;
            if (c.severity.ordinal() > highestSeverity.ordinal())
                highestSeverity = c.severity;
        }

        return highestSeverity;
    }

    // Whether any condition requires immediate action.
    public boolean isCritical()
    {
        int highestSeverity = getHighestSeverity().ordinal();
        return highestSeverity >= ConditionSeverity.CRITICAL.ordinal();
    }

    // === FACTORY ===
    public static PlayerStatus buildSelf(PlayerHealthData data)
    {
        return StatusBuilder.build(data, ObservabilityLevel.SUBJECTIVE);
    }
}
