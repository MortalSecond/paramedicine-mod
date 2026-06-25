package net.invinciblemoebius.traumaparamedicinemod.ui.components;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.client.ClientDiagnosticState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

// The diagnostic gauge stack: tiny icon + tiny reading, top-left, stacked downward.
// Each gauge is one row that returns its consumed height, so adding O2/HR/temp later
// is just another renderXGauge call in render(). Snapshot gauges (e.g. pulse) fade to
// declutter; live tool gauges (e.g. BP cuff) will simply not fade as long as the tool is still applied.
public class DiagnosticsComponent
{
    private static final ResourceLocation BP_ICON = new ResourceLocation(ParamedicineMod.MOD_ID, "textures/gui/icons/bp_icon.png");
    private static final int BP_ICON_W = 29;
    private static final int BP_ICON_H = 28;

    // In-game gauge icon height
    private static final int ICON_H = 9;
    private static final float TEXT_SCALE = 0.75f;
    private static final int ROW_GAP = 2;
    private static final int C_READ = 0xC83232;

    public void render(GuiGraphics g, Font font, int x, int y)
    {
        int cursorY = y;
        cursorY += renderBpGauge(g, font, x, cursorY);
    }

    private int renderBpGauge(GuiGraphics g, Font font, int x, int y)
    {
        if (!ClientDiagnosticState.hasBpEstimate())
            return 0;

        // Fade out. Data stays frozen until re-checked
        long age = System.currentTimeMillis() - ClientDiagnosticState.getBpUpdatedAt();
        if (age > 15000)
            return 0;

        float alpha = age < 10000 ? 1f : Math.max(0f, 1f - (age - 10000) / 5000f);

        String reading = formatBpEstimate(ClientDiagnosticState.getLowSystolic(), ClientDiagnosticState.getHighSystolic());
        if (reading == null)
            return 0;

        drawGauge(g, font, x, y, BP_ICON, BP_ICON_W, BP_ICON_H, reading, alpha);
        return ICON_H + ROW_GAP;
    }

    // Shared small-gauge drawer: scaled icon + scaled red text, vertically centered to the icon.
    private void drawGauge(GuiGraphics g, Font font, int x, int y, ResourceLocation icon, int srcW, int srcH, String text, float alpha)
    {
        int iconW = Math.round(ICON_H * (srcW / (float) srcH));

        g.setColor(1f, 1f, 1f, alpha);
        g.blit(icon, x, y, iconW, ICON_H, 0f, 0f, srcW, srcH, srcW, srcH); // 10-arg = scaled
        g.setColor(1f, 1f, 1f, 1f);

        int a = (int) (alpha * 255);
        float textY = y + (ICON_H - 8 * TEXT_SCALE) / 2f;

        g.pose().pushPose();
        g.pose().translate(x + iconW + 3, textY, 0);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1f);
        g.drawString(font, text, 0, 0, (a << 24) | C_READ, false);
        g.pose().popPose();
    }

    // Brackets per the palpation heuristic. Diastolic shown as (systolic - 20).
    private String formatBpEstimate(Integer low, Integer high)
    {
        // Bracketed ("Around this")
        if (low != null && high != null)
            return "~" + low + "/" + (low - 20);
        // Floor ("At least this")
        if (low != null)
            return low + "?/" + (low - 20) + "?";
        // Ceiling ("Below this")
        if (high != null)
            return "<" + high + "/" + (high - 20);

        return null;
    }
}