package net.invinciblemoebius.traumaparamedicinemod.ui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

// Reusable click-to-open menu. Pure client UI: the opener supplies a list of
// (label, callback, enabled) rows. The menu handles layout, hover, hit-testing,
// edge-clamping, and click-outside-to-close. Population is the caller's job, so the
// same widget serves right-click diagnostics AND drag-onto-wound disambiguation later.
public class ContextMenuComponent
{
    public record MenuOption(String label, Runnable onClick)
    {
        public MenuOption(String label, Runnable onClick)
        {
            this.label = label;
            this.onClick = onClick;
        }
    }

    // LAYOUT
    private static final int ROW_H = 16;
    private static final int PAD_X = 8;
    private static final int MIN_W = 90;
    // COLORS
    private static final int C_BG = 0xF01A1A1A;
    private static final int C_BORDER = 0xFFC83232;
    private static final int C_ROW_HOVER = 0xFF333333;
    private static final int C_TEXT = 0xFFE0E0E0;
    private static final int C_DIVIDER = 0xFF000000;
    // STATE
    private boolean open = false;
    private int anchorX, anchorY;
    private List<MenuOption> options = List.of();

    // Resolved (clamped) geometry from the last render, used for hit-testing.
    private int renderX, renderY, renderW;

    public boolean isOpen()
    {
        return open;
    }

    public void open(int x, int y, List<MenuOption> options)
    {
        this.open = true;
        this.anchorX = x;
        this.anchorY = y;
        this.options = options;
    }

    public void close()
    {
        this.open = false;
        this.options = List.of();
    }

    private int width(Font font)
    {
        int width = MIN_W;
        for (MenuOption option : options)
            width = Math.max(width, font.width(option.label()) + PAD_X * 2);
        return width;
    }

    private int height()
    {
        return options.size() * ROW_H;
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, int screenW, int screenH)
    {
        if (!open)
            return;

        int w = width(font);
        int h = height();

        int x = anchorX;
        int y = anchorY;
        if (x + w > screenW) x = screenW - w - 2;
        if (y + h > screenH) y = screenH - h - 2;
        x = Math.max(2, x);
        y = Math.max(2, y);
        renderX = x; renderY = y; renderW = w;

        // Background, then rows, then borders LAST so highlights never cover them.
        g.fill(x, y, x + w, y + h, C_BG);

        for (int i = 0; i < options.size(); i++)
        {
            int rowY = y + i * ROW_H;
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + ROW_H;

            if (hover)
                g.fill(x + 1, rowY, x + w - 1, rowY + ROW_H, C_ROW_HOVER);
            if (i > 0)
                g.fill(x + 1, rowY, x + w - 1, rowY + 1, C_DIVIDER);

            g.drawString(font, options.get(i).label(), x + PAD_X, rowY + (ROW_H - 8) / 2, C_TEXT, false);
        }

        g.fill(x, y, x + w, y + 1, C_BORDER);
        g.fill(x, y + h - 1, x + w, y + h, C_BORDER);
        g.fill(x, y, x + 1, y + h, C_BORDER);
        g.fill(x + w - 1, y, x + w, y + h, C_BORDER);
    }

    // Returns true if the click was consumed (selecting a row, or closing on outside-click).
    public boolean mouseClicked(double mouseX, double mouseY)
    {
        if (!open)
            return false;

        boolean inside = mouseX >= renderX && mouseX < renderX + renderW && mouseY >= renderY && mouseY < renderY + height();

        if (inside)
        {
            int i = (int) ((mouseY - renderY) / ROW_H);
            if (i >= 0 && i < options.size())
            {
                MenuOption chosen = options.get(i);
                close();
                chosen.onClick().run();
                return true;
            }
        }

        // Outside click closes.
        // It also gets consumed so it doesn't also reselect a node.
        close();
        return true;
    }
}