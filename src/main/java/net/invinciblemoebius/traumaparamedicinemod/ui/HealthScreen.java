package net.invinciblemoebius.traumaparamedicinemod.ui;

import net.invinciblemoebius.traumaparamedicinemod.status.Condition;
import net.invinciblemoebius.traumaparamedicinemod.status.PlayerStatus;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HealthScreen extends Screen
{
    // DIVS.
    private static final float LEFT_FRACTION = 0.27f;
    private static final float CENTER_FRACTION = 0.44f;
    // Right side takes the rest.
    private static final int PANEL_MARGIN = 8;
    private static final int PANEL_PADDING = 6;
    private static final int PANEL_GAP = 4;
    private static final int HOTBAR_HEIGHT = 4;

    // MOODLE DISPLAY
    private static final int M_SIZE = 14;
    private static final int M_PADDING = 5;

    // PANEL BOUNDS
    private int leftX, leftY, leftW, leftH;
    private int centerX, centerY, centerW, centerH;
    private int rightX, rightY, rightW, rightH;
    private int hotbarX, hotbarY, hotbarW, hotbarH;

    // STATE
    private PlayerStatus status = null;
    private Condition hoverMoodle = null;

    // === CONSTRUCTOR ===
    public HealthScreen()
    {
        super(Component.empty());
    }
}
