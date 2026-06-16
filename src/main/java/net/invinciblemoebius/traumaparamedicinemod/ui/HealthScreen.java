package net.invinciblemoebius.traumaparamedicinemod.ui;

import net.invinciblemoebius.traumaparamedicinemod.client.ModKeybinds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class HealthScreen extends Screen
{
    private final Player target;

    // LAYOUT
    private static final float FRAC_LEFT = 0.28f;
    private static final float FRAC_RIGHT = 0.28f;
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_BORDER = 1;
    private static final int SLOT_STRIDE = SLOT_SIZE + SLOT_BORDER * 2;
    private static final int SLOT_GAP = 3;
    private static final int HOTBAR_PAD_V = 6;
    private static final int BORDER = 1;
    private static final int PAD = 6;
    // COLORS
    // Semi-transparent overlay.
    private static final int C_DIM = 0xCC0A0A0A;
    // Panel background.
    private static final int C_PANEL_BG = 0xDD111111;
    // Panel border.
    private static final int C_PANEL_BORDER = 0xFF2A2A2A;
    // Slots.
    private static final int C_SLOT_BG = 0x55000000;
    private static final int C_SLOT_BORDER = 0xFF333333;
    private static final int C_SLOT_SELECTED = 0x55FFFFFF;
    private static final int C_HOTBAR_DIVIDER = 0xFF222222;
    // Label color.
    private static final int C_LABEL = 0xFF555555;

    // === CONSTRUCTOR ===
    public HealthScreen(Player target)
    {
        super(Component.literal("paramedicine.health_screen"));
        this.target = target;
    }

    // === INTERACTIONS ===

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        // Escape closes normally.
        if (keyCode == 256)
        {
            onClose();
            return true;
        }

        // Keybind toggles closed.
        if (ModKeybinds.OPEN_HEALTH_SCREEN.matches(keyCode, scanCode))
        {
            onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // === RENDERING METHODS ===

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks)
    {
        g.fill(0, 0, width, height, C_DIM);

        // Panel bounds.
        int leftW = (int) (width * FRAC_LEFT);
        int rightW = (int) (width * FRAC_RIGHT);
        int centerX = leftW;
        int centerW = width - leftW - rightW;
        int rightX = centerX + centerW;

        // Three panels.
        drawPanel(g, 0, 0, leftW, height);
        drawPanel(g, centerX, 0, centerW, height);
        drawPanel(g, rightX, 0, rightW, height);

        // Hotbar strip.
        int hotbarAreaH = computeHotbarAreaHeight(centerW);
        int hotbarAreaY = height - hotbarAreaH;

        // Divider line between the anatomy map and the hotbar.
        g.fill(centerX + PAD, hotbarAreaY, centerX + centerW - PAD,
                hotbarAreaY + BORDER, C_HOTBAR_DIVIDER);

        // Hotbar items.
        renderHotbarSlots(g, centerX, centerW, hotbarAreaY, hotbarAreaH);

        // PLACEHOLDER LABELS.
        g.drawString(minecraft.font, "Symptoms / Conditions", PAD, PAD, C_LABEL, false);
        g.drawString(minecraft.font, "Anatomy", centerX + PAD, PAD, C_LABEL, false);
        g.drawString(minecraft.font, "Overview", rightX + PAD, PAD, C_LABEL, false);

        super.render(g, mouseX, mouseY, partialTicks);
    }

    private void drawPanel(GuiGraphics g, int x, int y, int w, int h)
    {
        // Background.
        g.fill(x, y, x + w, y + h, C_PANEL_BG);

        // Borders
        g.fill(x, y, x + w, y + BORDER, C_PANEL_BORDER); // Top.
        g.fill(x, y + h - BORDER, x + w, y + h, C_PANEL_BORDER); // Bottom.
        g.fill(x, y, x + BORDER, y + h, C_PANEL_BORDER); // Left.
        g.fill(x + w - BORDER, y, x + w, y + h, C_PANEL_BORDER); // Right.
    }

    // This checks if all nine slots fit in the area between the right and left panels.
    // If it can't, then it wraps into a second line.
    private int computeHotbarAreaHeight(int centerW)
    {
        int usable = centerW - PAD * 2;
        int oneRow = 9 * SLOT_STRIDE + 8 * SLOT_GAP;
        int rows = oneRow <= usable ? 1 : 2;
        return rows * SLOT_STRIDE + (rows - 1) * SLOT_GAP + HOTBAR_PAD_V * 2;
    }

    // Attempts to center the slots. Think of like using margin-right: auto; and margin-left: auto; on CSS.
    private void renderHotbarSlots(GuiGraphics g, int centerX, int centerW, int hotbarAreaY, int hotbarAreaH)
    {
        int usable = centerW - PAD * 2;
        int oneRow = 9 * SLOT_STRIDE + 8 * SLOT_GAP;
        int selected = target.getInventory().selected;

        // Single centered row.
        if (oneRow <= usable)
        {
            int rowStartX = centerX + (centerW - oneRow) / 2;
            int rowY = hotbarAreaY + HOTBAR_PAD_V;

            for (int i = 0; i < 9; i++)
            {
                int slotX = rowStartX + i * (SLOT_STRIDE + SLOT_GAP);
                drawSlot(g, slotX, rowY, i, selected);
            }
        }
        // Two rows.
        else
        {
            int row1Count = 5;
            int row2Count = 4;
            int row1W = row1Count * SLOT_STRIDE + (row1Count - 1) * SLOT_GAP;
            int row2W = row2Count * SLOT_STRIDE + (row2Count - 1) * SLOT_GAP;
            int row1StartX = centerX + (centerW - row1W) / 2;
            int row2StartX = centerX + (centerW - row2W) / 2;
            int row1Y = hotbarAreaY + HOTBAR_PAD_V;
            int row2Y = row1Y + SLOT_STRIDE + SLOT_GAP;

            for (int i = 0; i < row1Count; i++)
            {
                int slotX = row1StartX + i * (SLOT_STRIDE + SLOT_GAP);
                drawSlot(g, slotX, row1Y, i, selected);
            }
            for (int i = 0; i < row2Count; i++)
            {
                int slotX = row2StartX + i * (SLOT_STRIDE + SLOT_GAP);
                drawSlot(g, slotX, row2Y, row1Count + i, selected);
            }
        }
    }

    private void drawSlot(GuiGraphics g, int x, int y, int slotIndex, int selectedSlot)
    {
        // Selection highlight.
        if (slotIndex == selectedSlot)
            g.fill(x - 1, y - 1, x + SLOT_STRIDE + 1, y + SLOT_STRIDE + 1, C_SLOT_SELECTED);

        // Border.
        g.fill(x, y, x + SLOT_STRIDE, y + SLOT_BORDER, C_SLOT_BORDER);
        g.fill(x, y + SLOT_STRIDE - SLOT_BORDER, x + SLOT_STRIDE, y + SLOT_STRIDE, C_SLOT_BORDER);
        g.fill(x, y, x + SLOT_STRIDE, y + SLOT_STRIDE, C_SLOT_BORDER);
        g.fill(x + SLOT_STRIDE - SLOT_BORDER, y, x + SLOT_STRIDE, y + SLOT_STRIDE, C_SLOT_BORDER);

        // Slot background inside the border.
        g.fill(x + SLOT_BORDER, y + SLOT_BORDER, x + SLOT_STRIDE - SLOT_BORDER, y + SLOT_STRIDE - SLOT_BORDER, C_SLOT_BG);

        // Item icon.
        var stack = target.getInventory().getItem(slotIndex);
        if (!stack.isEmpty())
        {
            g.renderItem(stack, x + SLOT_BORDER, y + SLOT_BORDER);
            g.renderItemDecorations(minecraft.font, stack, x + SLOT_BORDER, y + SLOT_BORDER);
        }
    }
}
