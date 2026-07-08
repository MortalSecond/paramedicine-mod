package net.invinciblemoebius.traumaparamedicinemod.ui.components;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.AppliedDressing;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.invinciblemoebius.traumaparamedicinemod.wound.Dressing;
import net.invinciblemoebius.traumaparamedicinemod.wound.DressingType;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundStage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The right-hand info panel. Two modes:
//   selectedNode == null: whole-body OVERVIEW (wound tally, dressings, substances)
//   selectedNode != null: that node's per-wound DETAIL
public class RightPanelComponent
{
    private static final int LINE_H = 11;
    private static final int HEADER_H = 16;
    // MC hour (24000-tick day / 24)
    private static final int TICKS_PER_HOUR = 1000;

    private static final int C_HEADER = 0xFF3A6BFF;
    private static final int C_NODENAME = 0xFFE03030;
    private static final int C_WHITE = 0xFFFFFFFF;
    private static final int C_GOLD = 0xFFD8A53A;
    private static final int C_RED = 0xFFC83232;
    private static final int C_DIM = 0xFFB0B0B0;
    private static final int C_GOOD = 0xFF7AC77A;

    private int scrollOffset = 0;
    private int contentHeight = 0;
    // x1,y1(after header),x2,y2 for scroll hit-testing
    private int[] viewport = new int[4];

    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, PlayerHealthData data, LimbNode selectedNode)
    {
        int bodyTop = y + HEADER_H;
        viewport = new int[]{x, bodyTop, x + w, y + h};

        // Header (sticky, outside the scissor).
        if (selectedNode == null)
            g.drawString(font, "Overview", x, y, C_HEADER, false);
        else
            g.drawString(font, prettyNode(selectedNode), x, y, C_NODENAME, false);

        g.enableScissor(x, bodyTop, x + w, y + h);
        int cy = bodyTop - scrollOffset;
        cy = (selectedNode == null) ? renderOverview(g, font, x, cy, data) : renderNodeDetail(g, font, x, cy, data.getLimb(selectedNode));
        g.disableScissor();

        contentHeight = (cy + scrollOffset) - bodyTop;
        drawScrollbar(g, x + w - 2, bodyTop, y + h);
    }

    // === OVERVIEW MODE ===
    private int renderOverview(GuiGraphics g, Font font, int x, int cy, PlayerHealthData data)
    {
        // Tally wounds across the whole body.
        Map<String, int[]> tally = new LinkedHashMap<>();

        for (LimbNode node : LimbNode.values())
            for (Wound wound : data.getLimb(node).getWounds())
            {
                String label = sizeBucket(wound.getSize()) + " " + pretty(wound.getType().name());
                int[] entry = tally.computeIfAbsent(label, k -> new int[]{0, severityColor(wound)});
                entry[0]++;
            }

        if (tally.isEmpty())
            cy = line(g, font, x, cy, "No detected wounds.", C_GOOD, 0);
        else
            for (Map.Entry<String, int[]> e : tally.entrySet())
                cy = line(g, font, x, cy, e.getValue()[0] + "x " + e.getKey(), e.getValue()[1], 0);

        cy += 6;

        // Dressings.
        // Dressings (per node, may cover several wounds each).
        List<AppliedDressing> allDressings = new ArrayList<>();
        for (LimbNode node : LimbNode.values())
            allDressings.addAll(data.getLimb(node).getDressings());

        if (!allDressings.isEmpty())
        {
            cy = line(g, font, x, cy, allDressings.size() + "x Dressings", C_HEADER, 0);
            Map<String, Integer> dress = new LinkedHashMap<>();
            for (AppliedDressing appliedDressing : allDressings)
            {
                String name = (appliedDressing.getDressing().isOverdue() ? "Old " : "") + dressingLabel(appliedDressing.getDressing());
                dress.merge(name, 1, Integer::sum);
            }
            for (Map.Entry<String, Integer> entry : dress.entrySet())
                cy = line(g, font, x, cy, entry.getValue() + "x " + entry.getKey(),
                        entry.getKey().startsWith("Old") ? C_GOLD : C_WHITE, 1);
            cy += 6;
        }

        // Systemic substances.
        for (SubstanceType substance : data.getClientActiveSubstances())
            cy = line(g, font, x, cy, "Recently injected " + pretty(substance.name()), C_HEADER, 0);

        return cy;
    }

    // === NODE DETAIL MODE ===
    private int renderNodeDetail(GuiGraphics g, Font font, int x, int cy, LimbData limb)
    {
        // Node-level condition tags.
        List<String> tags = new ArrayList<>();

        if (limb.getEffectivePain() > 0.05f)
            tags.add("Pain");
        if (limb.getLastNetBleedRateML() > 0.01f)
            tags.add("Bleeding");
        if (!limb.isCirculatingProximally())
            tags.add("No Circulation");
        if (!tags.isEmpty())
            cy = line(g, font, x, cy, "- " + String.join("   - ", tags), C_DIM, 0);

        cy += 4;

        // Wounds, worst first.
        List<Wound> wounds = new ArrayList<>(limb.getWounds());
        wounds.sort((a, b) -> Float.compare(severityScore(b), severityScore(a)));

        if (wounds.isEmpty())
            cy = line(g, font, x, cy, "No detected wounds.", C_GOOD, 0);

        for (Wound wound : wounds)
        {
            cy = line(g, font, x, cy, sizeBucket(wound.getSize()) + " " + pretty(wound.getType().name()), severityColor(wound), 0);
            cy = line(g, font, x, cy, stageText(wound), C_DIM, 1);

            if (wound.hasBeenIrrigated())
                cy = line(g, font, x, cy, "+ Irrigated", C_GOOD, 1);
            // Node dressings (each covers one or more wounds).
            for (AppliedDressing ad : limb.getDressings())
            {
                int hours = (int) (ad.getDressing().getAgeTicks() / TICKS_PER_HOUR);
                String name = (ad.getDressing().isOverdue() ? "Old " : "") + dressingLabel(ad.getDressing());
                cy = line(g, font, x, cy, "+ " + name + " (covers " + ad.getCoveredWoundIds().size() + ") [" + hours + "h]",
                        ad.getDressing().isOverdue() ? C_GOLD : C_GOOD, 0);
            }
            if (!limb.getDressings().isEmpty())
                cy += 4;

            String cont = contaminationText(wound.getContamination());

            if (cont != null)
                cy = line(g, font, x, cy, "- " + cont, C_RED, 1);
            if (wound.getInfectionLevel() >= 0.30f)
                cy = line(g, font, x, cy, "- Infected", C_RED, 1);
            cy += 5;
        }
        return cy;
    }

    // === SCROLL ===
    public boolean mouseScrolled(double mx, double my, double delta)
    {
        if (mx < viewport[0] || mx > viewport[2] || my < viewport[1] || my > viewport[3])
            return false;

        int max = Math.max(0, contentHeight - (viewport[3] - viewport[1]));
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) (delta * LINE_H * 2)));

        return true;
    }

    public void resetScroll() { scrollOffset = 0; }

    private void drawScrollbar(GuiGraphics g, int x, int top, int bottom)
    {
        int viewH = bottom - top;
        if (contentHeight <= viewH)
            return;

        int trackH = viewH;
        int thumbH = Math.max(12, (int) (trackH * (viewH / (float) contentHeight)));
        int max = contentHeight - viewH;
        int thumbY = top + (max == 0 ? 0 : (int) ((trackH - thumbH) * (scrollOffset / (float) max)));
        g.fill(x, top, x + 2, bottom, 0x40FFFFFF);
        g.fill(x, thumbY, x + 2, thumbY + thumbH, 0xFFC83232);
    }

    private int line(GuiGraphics g, Font font, int x, int cy, String text, int color, int indent)
    {
        // Cheap cull.
        if (cy + LINE_H >= viewport[1] && cy <= viewport[3])
            g.drawString(font, text, x + indent * 8, cy, color, false);

        return cy + LINE_H;
    }

    // === FORMAT HELPERS ===
    private static String sizeBucket(float s)
    {
        if (s < 0.20f)
            return "Minor";
        if (s < 0.40f)
            return "Small";
        if (s < 0.60f)
            return "Medium";
        if (s < 0.80f)
            return "Large";

        return "Massive";
    }

    private static String stageText(Wound wound)
    {
        String phase = switch (wound.getStage())
        {
            case FRESH -> "Fresh";
            case INFLAMED -> "Inflamed";
            case SCABBING -> wound.getStageProgress() > 0.5f ? "Mostly scabbed" : "Scabbing over";
            case SCARRING -> "Scarring";
            case HEALED -> "Healed";
        };

        if (wound.getStage() == WoundStage.FRESH)
        {
            String trend = switch (wound.hemostasisTrend())
            {
                case BLEEDING -> "bleeding";
                case CLOTTING -> "clotting";
                case CLOTTED -> "clotted";
            };
            return phase + " - " + trend;
        }
        return phase;
    }

    private static String contaminationText(float c)
    {
        if (c < 0.15f)
            return null;
        if (c < 0.40f)
            return "Mildly contaminated";
        if (c < 0.70f)
            return "Contaminated";

        return "Heavily contaminated";
    }

    private static String dressingLabel(Dressing d)
    {
        if (d == null)
            return "Dressing";
        if (d.getHemostatic() >= 0.5f)
            return "Hemostatic Dressing";
        if (d.getOcclusion()  >= 0.6f)
            return "Occlusive Seal";
        if (d.getAntiseptic() >= 0.5f)
            return "Antiseptic Dressing";
        if (d.getCleanliness() >= 0.75f)
            return "Clean Dressing";
        if (d.getCleanliness() <  0.30f)
            return "Dirty Dressing";

        return "Dressing";
    }

    private static float severityScore(Wound wound)
    {
        return wound.getDepth().ordinal() * 0.15f + wound.getSize();
    }

    private static int severityColor(Wound wound)
    {
        float severity = severityScore(wound);

        if (severity < 0.5f)
            return C_WHITE;
        if (severity < 1.0f)
            return C_GOLD;

        return C_RED;
    }

    private static String prettyNode(LimbNode n)
    {
        return pretty(n.name());
    }

    // Small helper method. Turns the screaming ENUM NAME into a prettier title-cased version of itself.
    private static String pretty(String enumName)
    {
        String[] parts = enumName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();

        for (String part : parts)
        {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }

        return sb.toString();
    }
}