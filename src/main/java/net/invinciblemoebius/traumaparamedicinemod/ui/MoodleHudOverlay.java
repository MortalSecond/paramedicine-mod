package net.invinciblemoebius.traumaparamedicinemod.ui;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.status.Condition;
import net.invinciblemoebius.traumaparamedicinemod.status.ConditionSeverity;
import net.invinciblemoebius.traumaparamedicinemod.status.PlayerStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MoodleHudOverlay
{
    // LAYOUT
    private static final int MOODLE_SIZE = 20; // Diameter in px.
    private static final int MOODLE_PADDING = 4; // Space between moodles in px.
    private static final int BOTTOM_MARGIN = 10; // Distance from the bottom of the screen, in px.
    private static final int GLOW_BORDER = 3; // Border from CRITICAL_GLOW
    // STATE
    private static final Map<Condition, MoodleAnimState> animStates = new LinkedHashMap<>();
    private static Set<Condition> lastActive = new HashSet<>();
    private static final Map<Condition, ConditionSeverity> lastSeverity = new HashMap<>();

    // === METHODS ===

    // Registers the HUD into the game's GUI.
    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event)
    {
        event.registerAboveAll("moodle_bar", MoodleHudOverlay::render);
    }

    public static void clientTick()
    {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Build current status from client-side capability.
        mc.player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
        {
            PlayerStatus status = PlayerStatus.buildSelf(data);
            Set<Condition> currentActive = status.getVisibleConditions();

            // New conditions create an AnimState
            for (Condition condition : currentActive)
            {
                if (!animStates.containsKey(condition))
                    animStates.put(condition, new MoodleAnimState());
            }

            // Worsened conditions shake.
            for (Condition condition : currentActive)
            {
                ConditionSeverity prev = lastSeverity.get(condition);
                if (prev != null && condition.severity.ordinal() > prev.ordinal())
                    animStates.get(condition).triggerShake();
                lastSeverity.put(condition, condition.severity);
            }

            // Fade out on removal.
            for (Condition condition : lastActive)
            {
                if (!currentActive.contains(condition) && animStates.containsKey(condition))
                {
                    animStates.get(condition).dying = true;
                    lastSeverity.remove(condition);
                }
            }

            lastActive = new HashSet<>(currentActive);
        });

        // Advance all anim states and remove the fully faded.
        animStates.entrySet().removeIf(entry -> entry.getValue().tick());
    }

    // Render.
    public static void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight)
    {
        if (animStates.isEmpty()) return;

        // Sort visible (non-dying) conditions by severity descending.
        List<Condition> sorted = animStates.keySet().stream()
                .filter(condition -> !animStates.get(condition).dying || animStates.get(condition).alpha > 0f)
                .sorted(Comparator.comparingInt((Condition condition) -> condition.severity.ordinal()).reversed())
                .toList();

        int totalWidth = sorted.size() * (MOODLE_SIZE + MOODLE_PADDING) - MOODLE_PADDING;
        int startX = (screenWidth - totalWidth) / 2;
        int y = screenHeight - MOODLE_SIZE - BOTTOM_MARGIN;

        for (int i = 0; i < sorted.size(); i++)
        {
            Condition condition = sorted.get(i);
            MoodleAnimState animState = animStates.get(condition);
            MoodleDefinition def = MoodleDefinition.get(condition);

            int baseColor = MoodleDefinition.severityColor(condition.severity);
            int x = startX + i * (MOODLE_SIZE + MOODLE_PADDING) + (int) animState.xOffset;
            int drawY = y + (int) animState.shakeOffset;

            // Fade out alpha masking. Or compositing. Whatever.
            int alpha255 = (int) (animState.alpha * 255);
            int colorWithAlpha = applyAlpha(baseColor, alpha255);

            // Glow border for CRITICAL_GLOW
            if (condition.severity == ConditionSeverity.CRITICAL_GLOW)
            {
                int glowAlpha = (int)(animState.glowAlpha() * alpha255);
                int glowColor = applyAlpha(0xFFFF2244, glowAlpha);
                graphics.fill(
                        x - GLOW_BORDER, drawY - GLOW_BORDER,
                        x + MOODLE_SIZE + GLOW_BORDER, drawY + MOODLE_SIZE + GLOW_BORDER,
                        glowColor
                );
            }

            // Circle background.
            drawSquare(graphics, x, drawY, MOODLE_SIZE, colorWithAlpha);

            // Icon character
            String iconStr = String.valueOf(def.icon);
            int textColor = applyAlpha(0xFFFFFFFF, alpha255);
            int textX = x + MOODLE_SIZE / 2 - Minecraft.getInstance().font.width(iconStr) / 2;
            int textY = drawY + MOODLE_SIZE / 2 - 4;
            graphics.drawString(Minecraft.getInstance().font, iconStr, textX, textY, textColor, false);
        }
    }

    // === HELPER METHODS ===

    // Draws a square.
    private static void drawSquare(GuiGraphics graphics, int x, int y, int size, int color)
    {
        graphics.fill(x, y, x + size, y + size, color);
    }

    // Replaces the alpha channel of an ARGB color with a 255 value.
    private static int applyAlpha(int argb, int alpha255)
    {
        return (argb & 0x00FFFFFF) | (alpha255 << 24);
    }
}
