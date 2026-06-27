package net.invinciblemoebius.traumaparamedicinemod.limbs;

import javax.annotation.Nullable;

// This represents a single anatomical node in the player's body.
public enum LimbNode
{
    // TRUNK
    UPPER_TORSO    (null, LimbCategory.TRUNK, false),
    LOWER_TORSO    (UPPER_TORSO, LimbCategory.TRUNK, false),
    GROIN          (LOWER_TORSO, LimbCategory.TRUNK, false),

    // HEAD
    NECK           (UPPER_TORSO, LimbCategory.HEAD, false),
    HEAD           (NECK, LimbCategory.HEAD, false),

    // LEFT ARM
    LEFT_UPPER_ARM (UPPER_TORSO, LimbCategory.ARM, true),
    LEFT_FOREARM   (LEFT_UPPER_ARM, LimbCategory.ARM, true),
    LEFT_HAND      (LEFT_FOREARM, LimbCategory.ARM, false),

    // RIGHT ARM
    RIGHT_UPPER_ARM(UPPER_TORSO, LimbCategory.ARM, true),
    RIGHT_FOREARM  (RIGHT_UPPER_ARM, LimbCategory.ARM, true),
    RIGHT_HAND     (RIGHT_FOREARM, LimbCategory.ARM, false),

    // LEFT LEG
    LEFT_UPPER_LEG (GROIN, LimbCategory.LEG, true),
    LEFT_LOWER_LEG (LEFT_UPPER_LEG, LimbCategory.LEG, true),
    LEFT_FOOT      (LEFT_LOWER_LEG, LimbCategory.LEG, false),

    // RIGHT LEG
    RIGHT_UPPER_LEG(GROIN, LimbCategory.LEG, true),
    RIGHT_LOWER_LEG(RIGHT_UPPER_LEG, LimbCategory.LEG, true),
    RIGHT_FOOT     (RIGHT_LOWER_LEG, LimbCategory.LEG, false);

    // ===

    @Nullable
    public final LimbNode proximalNode;
    public final LimbCategory category;
    public final boolean canApplyTourniquet;

    LimbNode(@Nullable LimbNode proximalNode, LimbCategory category, boolean canApplyTourniquet)
    {
        this.proximalNode = proximalNode;
        this.category = category;
        this.canApplyTourniquet = canApplyTourniquet;
    }
}
