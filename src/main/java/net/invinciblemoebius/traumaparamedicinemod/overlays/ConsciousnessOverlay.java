package net.invinciblemoebius.traumaparamedicinemod.overlays;

import com.mojang.blaze3d.systems.RenderSystem;
import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class ConsciousnessOverlay
{
    private static final ResourceLocation APERTURE = new ResourceLocation(ParamedicineMod.MOD_ID, "textures/gui/vignette_aperture.png");
    private static final int APERTURE_TEX = 256;
    private static final float ONSET = ModConstants.CONSCIOUSNESS_ALERT;
    private static final float FULL = ModConstants.CONSCIOUSNESS_PAIN;
    private static final float OPACITY_RAMP = 0.6f;
    private static final float START_SCALE = 1.25f;
    private static final float END_SCALE = 0.18f;

    // === GRAPHICS ===

    // How does it work, you ask?
    // Well, basically i made a static vignette PNG. This is the actual aperture, it isn't
    // dynamically created. It shrinks and expands relative to consciousness. Now, you probably
    // realized that this has a critical issue: What happens to the edges? If a static PNG keeps
    // shrinking then it gets removed. Well, simple! You draw a black border at its edges. That way,
    // it gives the illusion that the blackness of the PNG continues past the static edge.
    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        float consciousness = mc.player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                .map(PlayerHealthData::getConsciousness).orElse(1.0f);

        // Alert: nothing to draw
        if (consciousness >= ONSET)
            return;

        // 0 at onset, 1 at blackout
        float t = clamp01((ONSET - consciousness) / (ONSET - FULL));
        // Closes faster as it deepens.
        float eased = t * t;
        // How "faded in" the vignette is. So it doesn't just pop up at full opacity.
        float strength = clamp01(t/OPACITY_RAMP);

        // Aperture box shrinks from "off-screen huge" (invisible) to a tight focal point.
        float diagonal = (float) Math.sqrt((double) w * w + (double) h * h);
        int box = (int) lerp(diagonal * START_SCALE, h * END_SCALE, eased);
        int half = box / 2;
        int cx = w / 2, cy = h / 2;
        int x0 = cx - half, y0 = cy - half, x1 = cx + half, y1 = cy + half;

        // Blend-in focal point.
        RenderSystem.enableBlend();
        // This fades the 'outer border' too.
        RenderSystem.setShaderColor(1f, 1f, 1f, strength);
        g.blit(APERTURE, x0, y0, box, box, 0f, 0f, APERTURE_TEX, APERTURE_TEX, APERTURE_TEX, APERTURE_TEX);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // Transparency-weighted black outside the box.
        int blackA = ((int) (strength * 255)) << 24;
        if (y0 > 0)
            g.fill(0, 0, w, y0, blackA);
        if (y1 < h)
            g.fill(0, y1, w, h, blackA);
        if (x0 > 0)
            g.fill(0, Math.max(0, y0), x0, Math.min(h, y1), blackA);
        if (x1 < w)
            g.fill(x1, Math.max(0, y0), w, Math.min(h, y1), blackA);

        // Final close to total black over the last stretch so the clear dot fully shuts.
        float fade = clamp01((t - 0.85f) / 0.15f);
        if (fade > 0f)
            g.fill(0, 0, w, h, ((int) (fade * 255) << 24));
    }

    // Utils, but like. I may make them their own Utils.java at this point.
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
