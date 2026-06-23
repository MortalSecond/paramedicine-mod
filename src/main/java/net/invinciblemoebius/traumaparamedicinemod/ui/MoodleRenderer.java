package net.invinciblemoebius.traumaparamedicinemod.ui;

import net.invinciblemoebius.traumaparamedicinemod.status.Condition;
import net.invinciblemoebius.traumaparamedicinemod.status.ConditionSeverity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

// Shared per-moodle draw. The HUD overlay and the health-screen panel both call this,
// so any styling change here is universal. Basically an Angular component.
public final class MoodleRenderer
{
    public static final int GLOW_BORDER = 3;

    public static void drawMoodle(GuiGraphics g, int x, int y, int size, Condition condition, MoodleAnimState anim)
    {
        // Fade out alpha masking. Or compositing. Whatever.
        int alpha255 = (int) (anim.alpha * 255);
        int drawY = y + (int) anim.shakeOffset;

        // Glow border for CRITICAL_GLOW
        if (condition.severity == ConditionSeverity.CRITICAL_GLOW)
        {
            int glowAlpha = (int) (anim.glowAlpha() * alpha255);
            g.fill(x - GLOW_BORDER, drawY - GLOW_BORDER, x + size + GLOW_BORDER, drawY + size + GLOW_BORDER, applyAlpha(0xFFFF2244, glowAlpha));
        }

        // Square background.
        g.fill(x, drawY, x + size, drawY + size, applyAlpha(MoodleDefinition.severityColor(condition.severity), alpha255));

        // Icon character
        var font = Minecraft.getInstance().font;
        String icon = String.valueOf(MoodleDefinition.get(condition).icon);
        g.drawString(font, icon, x + size / 2 - font.width(icon) / 2, drawY + size / 2 - 4, applyAlpha(0xFFFFFFFF, alpha255), false);
    }

    public static int applyAlpha(int argb, int alpha255)
    {
        return (argb & 0x00FFFFFF) | (alpha255 << 24);
    }
}