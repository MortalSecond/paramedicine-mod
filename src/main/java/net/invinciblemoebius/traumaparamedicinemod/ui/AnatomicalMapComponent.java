package net.invinciblemoebius.traumaparamedicinemod.ui;

import com.mojang.math.Axis;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.BoneState;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.EnumMap;
import java.util.Map;

// Programmatic A-pose body map. Core parts are axis-aligned rounded boxes;
// limbs are joint-to-joint segments (thick rotated rects) so they read as
// continuous limbs and hit-test by point-to-segment distance (exact, cheap).
//
// THREE NON-COLLIDING CHANNELS:
//   FILL  = soft tissue (muscleHealth), outline -> red as damaged.
//   DROP  = per-node net bleed, sized + darkened by severity.
//   GLYPH = worst wound type pip + count badge (upright).
// Bone shows as its own corner pip so it never touches the fill channel.
//
// ORIENTATION: authored with patient-right on the viewer's LEFT.
// Flip MIRROR to mirror the whole body if you ever want anatomical orientation.
public class AnatomicalMapComponent
{
    private static final boolean MIRROR = false;

    private record Box(float nx, float ny, float nw, float nh) {}
    private record Seg(float ax, float ay, float bx, float by, float thick) {}

    // === CORE BOXES (normalized 0..1 within the map rect) ===
    private static final Map<LimbNode, Box> BOXES = new EnumMap<>(LimbNode.class);
    // === LIMB SEGMENTS (joint A -> joint B, thickness normalized to width) ===
    private static final Map<LimbNode, Seg> SEGS = new EnumMap<>(LimbNode.class);
    static
    {
        // Central nodes.
        BOXES.put(LimbNode.HEAD,        new Box(0.42f, 0.04f, 0.16f, 0.115f));
        BOXES.put(LimbNode.NECK,        new Box(0.465f, 0.15f, 0.07f, 0.03f));
        BOXES.put(LimbNode.UPPER_TORSO, new Box(0.40f, 0.18f, 0.20f, 0.16f));
        BOXES.put(LimbNode.LOWER_TORSO, new Box(0.415f, 0.345f, 0.17f, 0.10f));
        BOXES.put(LimbNode.GROIN,       new Box(0.4075f, 0.45f, 0.185f, 0.06f));

        // Patient RIGHT arm -> viewer left.
        SEGS.put(LimbNode.RIGHT_UPPER_ARM, new Seg(0.405f, 0.205f, 0.35f, 0.37f, 0.075f));
        SEGS.put(LimbNode.RIGHT_FOREARM,   new Seg(0.35f, 0.37f, 0.315f, 0.50f, 0.065f));
        SEGS.put(LimbNode.RIGHT_HAND,      new Seg(0.315f, 0.50f, 0.295f, 0.565f, 0.06f));

        // Patient LEFT arm -> viewer right.
        SEGS.put(LimbNode.LEFT_UPPER_ARM,  new Seg(0.595f, 0.205f, 0.65f, 0.37f, 0.075f));
        SEGS.put(LimbNode.LEFT_FOREARM,    new Seg(0.65f, 0.37f, 0.685f, 0.50f, 0.065f));
        SEGS.put(LimbNode.LEFT_HAND,       new Seg(0.685f, 0.50f, 0.705f, 0.565f, 0.06f));

        // Patient RIGHT leg -> viewer left.
        SEGS.put(LimbNode.RIGHT_UPPER_LEG, new Seg(0.455f, 0.50f, 0.44f, 0.69f, 0.085f));
        SEGS.put(LimbNode.RIGHT_LOWER_LEG, new Seg(0.44f, 0.69f, 0.435f, 0.86f, 0.07f));
        SEGS.put(LimbNode.RIGHT_FOOT,      new Seg(0.435f, 0.86f, 0.418f, 0.905f, 0.07f));

        // Patient LEFT leg -> viewer right.
        SEGS.put(LimbNode.LEFT_UPPER_LEG,  new Seg(0.545f, 0.50f, 0.56f, 0.69f, 0.085f));
        SEGS.put(LimbNode.LEFT_LOWER_LEG,  new Seg(0.56f, 0.69f, 0.565f, 0.86f, 0.07f));
        SEGS.put(LimbNode.LEFT_FOOT,       new Seg(0.565f, 0.86f, 0.582f, 0.905f, 0.07f));
    }

    private static final int C_OUTLINE        = 0xFFFFFFFF;
    private static final int C_OUTLINE_HOVER  = 0xFFFFE08A;
    private static final int C_OUTLINE_SELECT = 0xFF6AB7FF;
    private static final int C_NO_CIRC        = 0xFF6A6A6A;
    private static final int C_BODY_BASE = 0xFF0E0E10;
    private static final int CORNER_R = 2;

    // Recomputed each render.
    private final Map<LimbNode, int[]>   boxPx = new EnumMap<>(LimbNode.class);  // x1,y1,x2,y2
    private final Map<LimbNode, float[]> segPx = new EnumMap<>(LimbNode.class);  // ax,ay,bx,by,thickPx

    private LimbNode hovered = null;
    private LimbNode selected = null;

    public void render(GuiGraphics g, int areaX, int areaY, int areaW, int areaH,
            int mouseX, int mouseY, PlayerHealthData data)
    {
        layout(areaX, areaY, areaW, areaH);
        hovered = nodeAt(mouseX, mouseY);
        float t = (System.currentTimeMillis() % 100000L) / 1000f;

        // PASS 1: limb segments first, so the core boxes occlude the shoulder/hip joins.
        for (LimbNode node : LimbNode.values())
            if (segPx.containsKey(node)) renderSegment(g, node, data.getLimb(node), t);

        // PASS 2: core boxes, opaque, so tucked limb ends vanish cleanly behind them.
        for (LimbNode node : LimbNode.values())
            if (boxPx.containsKey(node)) renderBox(g, node, data.getLimb(node), t);
    }

    private void renderBox(GuiGraphics g, LimbNode node, LimbData limb, float t)
    {
        int[] r = boxPx.get(node);
        int[] sh = shake(node, limb, t);
        int x1 = r[0] + sh[0], y1 = r[1] + sh[1], x2 = r[2] + sh[0], y2 = r[3] + sh[1];

        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, C_BODY_BASE); // opaque occluder base
        int fill = fillColor(limb);
        if (fill != 0) g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill); // damage overlay
        drawRoundedOutline(g, x1, y1, x2, y2, outlineColor(node, limb));

        decorate(g, limb, (x1 + x2) / 2, (y1 + y2) / 2, x1, y1, x1, y2);
    }

    private void renderSegment(GuiGraphics g, LimbNode node, LimbData limb, float t)
    {
        float[] s = segPx.get(node);
        int[] sh = shake(node, limb, t);
        float ax = s[0] + sh[0], ay = s[1] + sh[1], bx = s[2] + sh[0], by = s[3] + sh[1];
        float dx = bx - ax, dy = by - ay;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
        float mx = (ax + bx) / 2f, my = (ay + by) / 2f;
        int hl = Math.round(len / 2f), ht = Math.round(s[4] / 2f);

        g.pose().pushPose();
        g.pose().translate(mx, my, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(ang));
        int fill = fillColor(limb);
        if (fill != 0) g.fill(-hl + 1, -ht + 1, hl - 1, ht - 1, fill);
        drawRoundedOutline(g, -hl, -ht, hl, ht, outlineColor(node, limb));
        g.pose().popPose();

        decorate(g, limb, Math.round(mx), Math.round(my),
                Math.round(ax), Math.round(ay), Math.round(bx), Math.round(by));
    }

    private int[] shake(LimbNode node, LimbData limb, float t)
    {
        float bleed = limb.getLastNetBleedRateML();
        if (bleed <= 20f) return new int[]{0, 0};
        float amp = Math.min(2f, bleed / 40f);
        return new int[]{
                Math.round((float) Math.sin(t * 18f + node.ordinal()) * amp),
                Math.round((float) Math.cos(t * 15f + node.ordinal()) * amp)};
    }

    private int fillColor(LimbData limb)
    {
        int a = (int) ((1f - limb.getMuscleHealth()) * 200f);
        return a > 4 ? ((a << 24) | 0x00C81E1E) : 0;
    }

    private int outlineColor(LimbNode node, LimbData limb)
    {
        if (node == selected) return C_OUTLINE_SELECT;
        if (node == hovered)  return C_OUTLINE_HOVER;
        if (!limb.isCirculatingProximally()) return C_NO_CIRC;
        return C_OUTLINE;
    }

    private void decorate(GuiGraphics g, LimbData limb, int cx, int cy,
            int proxX, int proxY, int distX, int distY)
    {
        if (limb.getBoneState() != BoneState.INTACT)
            drawBoneGlyph(g, proxX, proxY, limb.getBoneState());
        float bleed = limb.getLastNetBleedRateML();
        if (bleed > 0.01f) drawBleedDrop(g, cx, cy, bleed);
        WoundType worst = limb.getClientWorstWoundType();
        if (worst != null) drawWoundGlyph(g, distX, distY, worst, limb.getClientWoundCount());
    }

    private void layout(int areaX, int areaY, int areaW, int areaH)
    {
        boxPx.clear(); segPx.clear();
        for (Map.Entry<LimbNode, Box> e : BOXES.entrySet())
        {
            Box b = e.getValue();
            float nx = MIRROR ? (1f - b.nx() - b.nw()) : b.nx();
            int x1 = areaX + Math.round(nx * areaW);
            int y1 = areaY + Math.round(b.ny() * areaH);
            boxPx.put(e.getKey(), new int[]{
                    x1, y1, x1 + Math.round(b.nw() * areaW), y1 + Math.round(b.nh() * areaH)});
        }
        for (Map.Entry<LimbNode, Seg> e : SEGS.entrySet())
        {
            Seg s = e.getValue();
            float ax = MIRROR ? 1f - s.ax() : s.ax();
            float bx = MIRROR ? 1f - s.bx() : s.bx();
            segPx.put(e.getKey(), new float[]{
                    areaX + ax * areaW, areaY + s.ay() * areaH,
                    areaX + bx * areaW, areaY + s.by() * areaH,
                    s.thick() * areaW});
        }
    }

    public LimbNode nodeAt(int mx, int my)
    {
        for (Map.Entry<LimbNode, int[]> e : boxPx.entrySet())
        {
            int[] r = e.getValue();
            if (mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3]) return e.getKey();
        }
        for (Map.Entry<LimbNode, float[]> e : segPx.entrySet())
        {
            float[] s = e.getValue();
            if (distToSegment(mx, my, s[0], s[1], s[2], s[3]) <= s[4] / 2f) return e.getKey();
        }
        return null;
    }

    public LimbNode getSelected() { return selected; }
    public LimbNode getHovered()  { return hovered; }
    public void setSelected(LimbNode node) { this.selected = node; }

    private static float distToSegment(float px, float py, float ax, float ay, float bx, float by)
    {
        float dx = bx - ax, dy = by - ay;
        float lenSq = dx * dx + dy * dy;
        if (lenSq < 1e-4f) return (float) Math.hypot(px - ax, py - ay);
        float tt = Math.max(0f, Math.min(1f, ((px - ax) * dx + (py - ay) * dy) / lenSq));
        float projX = ax + tt * dx, projY = ay + tt * dy;
        return (float) Math.hypot(px - projX, py - projY);
    }

    private void drawRoundedOutline(GuiGraphics g, int x1, int y1, int x2, int y2, int col)
    {
        g.fill(x1 + CORNER_R, y1, x2 - CORNER_R, y1 + 1, col);
        g.fill(x1 + CORNER_R, y2 - 1, x2 - CORNER_R, y2, col);
        g.fill(x1, y1 + CORNER_R, x1 + 1, y2 - CORNER_R, col);
        g.fill(x2 - 1, y1 + CORNER_R, x2, y2 - CORNER_R, col);
        g.fill(x1 + 1, y1 + 1, x1 + 2, y1 + 2, col);
        g.fill(x2 - 2, y1 + 1, x2 - 1, y1 + 2, col);
        g.fill(x1 + 1, y2 - 2, x1 + 2, y2 - 1, col);
        g.fill(x2 - 2, y2 - 2, x2 - 1, y2 - 1, col);
    }

    private void drawBleedDrop(GuiGraphics g, int cx, int cy, float bleedML)
    {
        int half = bleedML >= 50f ? 6 : bleedML >= 20f ? 5 : bleedML >= 5f ? 4 : 3;
        float k = Math.min(1f, bleedML / 80f);
        int red = (int) (220 - k * 110);
        int col = 0xFF000000 | (red << 16) | (0x10 << 8) | 0x10;
        for (int i = -half; i <= half; i++)
        {
            int w = half - Math.abs(i);
            g.fill(cx - w, cy + i, cx + w + 1, cy + i + 1, col);
        }
    }

    private void drawWoundGlyph(GuiGraphics g, int x, int y, WoundType type, int count)
    {
        int col = switch (type)
        {
            case LACERATION -> 0xFFE03030;
            case PUNCTURE   -> 0xFFB02060;
            case AVULSION   -> 0xFF8B0000;
            case ABRASION   -> 0xFFE08040;
            case BURN       -> 0xFFFF6010;
            case BLUNT      -> 0xFF9050C0;
        };
        g.fill(x - 2, y - 5, x + 1, y - 2, col);
        if (count > 1)
            g.drawString(Minecraft.getInstance().font, "x" + count, x + 2, y - 9, 0xFFFFFFFF, true);
    }

    private void drawBoneGlyph(GuiGraphics g, int x, int y, BoneState state)
    {
        int col = switch (state)
        {
            case HAIRLINE   -> 0xFFD0D0D0;
            case FRACTURED  -> 0xFFFFC040;
            case COMPOUND   -> 0xFFFF4040;
            case DISLOCATED -> 0xFFC080FF;
            default -> 0xFFFFFFFF;
        };
        g.fill(x + 1, y + 1, x + 4, y + 3, col);
    }
}