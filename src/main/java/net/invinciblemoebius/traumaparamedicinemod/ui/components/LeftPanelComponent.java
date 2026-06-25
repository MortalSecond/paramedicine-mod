package net.invinciblemoebius.traumaparamedicinemod.ui.components;

import net.invinciblemoebius.traumaparamedicinemod.status.Condition;
import net.invinciblemoebius.traumaparamedicinemod.ui.MoodleAnimState;
import net.invinciblemoebius.traumaparamedicinemod.ui.MoodleDefinition;
import net.invinciblemoebius.traumaparamedicinemod.ui.MoodleHudOverlay;
import net.invinciblemoebius.traumaparamedicinemod.ui.MoodleRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

// Reuses the shared MoodleRenderer and MoodleHudOverlay's anim state.
// So like. If you wanna modify the way the moodles look, it'll be affected here too.
public class LeftPanelComponent
{
    private static final int MOODLE_SIZE = 14;
    private static final int MOODLE_PAD = 4;
    private static final int LABEL_H = 12;

    private static final int C_LABEL    = 0xFF6A6A6A;
    private static final int C_DIVIDER  = 0xFF2A2A2A;
    private static final int C_TIP_BG   = 0xF0140A0A;

    private Condition hovered = null;

    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY)
    {
        hovered = null;

        // TODO: SYMPTOMS
        g.drawString(font, "Symptoms", x, y, C_LABEL, false);
        g.fill(x, y + LABEL_H, x + w, y + LABEL_H + 1, C_DIVIDER);

        // CONDITIONS
        int condTop = y + Math.round(h * 0.42f);
        g.drawString(font, "Conditions", x, condTop, C_LABEL, false);
        g.fill(x, condTop + LABEL_H, x + w, condTop + LABEL_H + 1, C_DIVIDER);

        int gridTop = condTop + LABEL_H + 6;
        int cols = Math.max(1, (w + MOODLE_PAD) / (MOODLE_SIZE + MOODLE_PAD));

        List<Condition> active = MoodleHudOverlay.activeSortedConditions();
        for (int i = 0; i < active.size(); i++)
        {
            Condition condition = active.get(i);
            MoodleAnimState anim = MoodleHudOverlay.getAnimState(condition);
            if (anim == null)
                continue;

            int mx = x + (i % cols) * (MOODLE_SIZE + MOODLE_PAD);
            int my = gridTop + (i / cols) * (MOODLE_SIZE + MOODLE_PAD);

            MoodleRenderer.drawMoodle(g, mx, my, MOODLE_SIZE, condition, anim);

            if (mouseX >= mx && mouseX < mx + MOODLE_SIZE && mouseY >= my && mouseY < my + MOODLE_SIZE)
                hovered = condition;
        }
    }

    // Called last by the screen so it overlays everything.
    public void renderTooltip(GuiGraphics g, Font font, int mouseX, int mouseY)
    {
        if (hovered == null)
            return;
        MoodleDefinition def = MoodleDefinition.get(hovered);

        int boxW = 168;
        List<FormattedCharSequence> body = font.split(FormattedText.of(def.flavorText), boxW - 10);
        int boxH = 6 + 10 + 3 + body.size() * 9 + 5;

        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int tx = mouseX + 10;
        int ty = mouseY + 10;

        if (tx + boxW > sw)
            tx = mouseX - boxW - 10;
        if (ty + boxH > sh)
            ty = sh - boxH - 2;
        if (ty < 2)
            ty = 2;

        int border = MoodleDefinition.severityColor(hovered.severity);
        g.fill(tx, ty, tx + boxW, ty + boxH, C_TIP_BG);
        g.fill(tx, ty, tx + boxW, ty + 1, border);
        g.fill(tx, ty + boxH - 1, tx + boxW, ty + boxH, border);
        g.fill(tx, ty, tx + 1, ty + boxH, border);
        g.fill(tx + boxW - 1, ty, tx + boxW, ty + boxH, border);

        g.drawString(font, def.displayName, tx + 5, ty + 5, border, false);
        int ly = ty + 18;
        for (FormattedCharSequence line : body)
        {
            g.drawString(font, line, tx + 5, ly, 0xFFCfCfCf, false);
            ly += 9;
        }
    }
}