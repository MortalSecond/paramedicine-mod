package net.invinciblemoebius.traumaparamedicinemod.ui;

import net.invinciblemoebius.traumaparamedicinemod.client.ClientCastState;
import net.invinciblemoebius.traumaparamedicinemod.client.ClientDiagnosticState;
import net.invinciblemoebius.traumaparamedicinemod.client.ModKeybinds;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.interactions.InteractionOption;
import net.invinciblemoebius.traumaparamedicinemod.interactions.NodeAction;
import net.invinciblemoebius.traumaparamedicinemod.interactions.NodeInteractionOptions;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.network.ModNetwork;
import net.invinciblemoebius.traumaparamedicinemod.network.packets.ServerboundApplyItemToNodePacket;
import net.invinciblemoebius.traumaparamedicinemod.network.packets.ServerboundInspectPacket;
import net.invinciblemoebius.traumaparamedicinemod.network.packets.ServerboundNodeActionPacket;
import net.invinciblemoebius.traumaparamedicinemod.sounds.ModSounds;
import net.invinciblemoebius.traumaparamedicinemod.sounds.RadioAmbienceSound;
import net.invinciblemoebius.traumaparamedicinemod.ui.components.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class HealthScreen extends Screen
{
    private final Player target;
    private final AnatomicalMapComponent anatomyMap = new AnatomicalMapComponent();
    private final RightPanelComponent rightPanel = new RightPanelComponent();
    private final LeftPanelComponent leftPanel = new LeftPanelComponent();
    private final ContextMenuComponent contextMenu = new ContextMenuComponent();
    private final DiagnosticsComponent diagnostics = new DiagnosticsComponent();

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

    // Drag state: which hotbar slot the cursor picked up, or -1.
    private int draggingFromSlot = -1;
    // Per-slot screen rects, populated each frame by drawSlot() so input can hit-test them.
    private final int[] slotXs = new int[9];
    private final int[] slotYs = new int[9];

    // Sounds.
    private RadioAmbienceSound radioLoop;

    // === CONSTRUCTOR ===
    public HealthScreen(Player target)
    {
        super(Component.literal("paramedicine.health_screen"));
        this.target = target;

        // Sounds.
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        sm.play(SimpleSoundInstance.forUI(ModSounds.HEALTHSCREEN_OPEN.get(), 1.0f));
        radioLoop = new RadioAmbienceSound(ModSounds.HEALTHSCREEN_SUSTAIN_MILD.get());
        sm.play(radioLoop);
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
        g.fill(centerX + PAD, hotbarAreaY, centerX + centerW - PAD, hotbarAreaY + BORDER, C_HOTBAR_DIVIDER);
        // Hotbar items.
        renderHotbarSlots(g, centerX, centerW, hotbarAreaY, hotbarAreaH);

        // Anatomy map fills the center panel between the header and the hotbar.
        g.drawString(minecraft.font, "Anatomy", centerX + PAD, PAD, C_LABEL, false);
        int mapY = PAD + 12;
        int mapX = centerX + PAD;
        int mapW = centerW - PAD * 2;
        int mapH = hotbarAreaY - mapY - PAD;

        // Left panel.
        leftPanel.render(g, minecraft.font, PAD, PAD, leftW - PAD * 2, height - PAD * 2, mouseX, mouseY);

        // Right panel fills the... well... right panel. :sob:
        target.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
        {
            anatomyMap.render(g, mapX, mapY, mapW, mapH, mouseX, mouseY, data);
            rightPanel.render(g, minecraft.font, rightX + PAD, PAD, rightW - PAD * 2, height - PAD * 2, data, anatomyMap.getSelected());
        });

        super.render(g, mouseX, mouseY, partialTicks);

        // Tooltip last so it overlays the panels.
        leftPanel.renderTooltip(g, minecraft.font, mouseX, mouseY);

        // Diagnostic gauges, top-left of the center panel under the "Anatomy" label.
        diagnostics.render(g, minecraft.font, centerX + PAD, PAD + 12);
        renderFeedbackText(g);

        // Draws the cast around the cursor.
        if (ClientCastState.isActive())
            drawCastTimer(g, mouseX, mouseY, ClientCastState.progress());

        // Dragged item ghosts under the cursor.
        if (draggingFromSlot >= 0)
        {
            ItemStack dragStack = target.getInventory().getItem(draggingFromSlot);
            if (!dragStack.isEmpty())
                g.renderItem(dragStack, mouseX - 8, mouseY - 8);
        }

        // Context menu absolutely last. It's the topmost interactive layer.
        contextMenu.render(g, minecraft.font, mouseX, mouseY, width, height);
    }

    // This is the little circle around the cursor, AKA the 'cast.'
    private void drawCastTimer(GuiGraphics g, int cx, int cy, float progress)
    {
        int radius = 12;
        int steps = 60;
        int filled = (int) (progress * steps);
        for (int i = 0; i < filled; i++)
        {
            double ang = -Math.PI / 2 + (i / (double) steps) * Math.PI * 2; // start at top, clockwise
            int px = cx + (int) Math.round(Math.cos(ang) * radius);
            int py = cy + (int) Math.round(Math.sin(ang) * radius);
            g.fill(px - 1, py - 1, px + 1, py + 1, 0xFFFFFFFF);
        }
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
        // Record the position.
        slotXs[slotIndex] = x;
        slotYs[slotIndex] = y;

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

    // Which hotbar slot, if any, the cursor is over.
    private int slotAt(double mx, double my)
    {
        for (int i = 0; i < 9; i++)
        {
            if (mx >= slotXs[i] && mx < slotXs[i] + SLOT_STRIDE
                    && my >= slotYs[i] && my < slotYs[i] + SLOT_STRIDE)
                return i;
        }
        return -1;
    }

    private List<ContextMenuComponent.MenuOption> buildNodeOptions(LimbNode node)
    {
        PlayerHealthData data = target.getCapability(PlayerHealthCapability.PLAYER_HEALTH).resolve().orElse(null);
        List<ContextMenuComponent.MenuOption> menu = new ArrayList<>();

        for (InteractionOption opt : NodeInteractionOptions.forNode(node, data))
        {
            menu.add(new ContextMenuComponent.MenuOption(opt.label(), () -> startInteraction(opt.node(), opt.action())));
        }

        return menu;
    }

    private void renderFeedbackText(GuiGraphics g)
    {
        String full = ClientDiagnosticState.getFeedbackText();
        if (full == null || full.isEmpty())
            return;

        long age = System.currentTimeMillis() - ClientDiagnosticState.getFeedbackAt();
        int msPerChar = 30;
        int streamMs = full.length() * msPerChar;
        int chars = (int) (age / msPerChar);
        String shown = chars >= full.length() ? full : full.substring(0, chars);

        // Hold 5s after the text finishes, then fade over 1s.
        long afterDone = age - streamMs;
        float alpha = afterDone > 5000 ? Math.max(0f, 1f - (afterDone - 5000) / 1000f) : 1f;
        if (alpha <= 0f)
            return;

        float scale = 1.6f;
        int tw = minecraft.font.width(shown);
        int a = (int) (alpha * 255);

        g.pose().pushPose();
        g.pose().translate(width / 2f, PAD + 2, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(minecraft.font, shown, -tw / 2, 0, (a << 24) | 0x00A8A8B0, true);
        g.pose().popPose();
    }

    // === INTERACTIONS ===

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    protected void init()
    {
        super.init();
        ModNetwork.CHANNEL.sendToServer(new ServerboundInspectPacket(target.getId()));
    }

    @Override
    public void removed()
    {
        ModNetwork.CHANNEL.sendToServer(new ServerboundInspectPacket(-1));
        super.removed();
        SoundManager sm = Minecraft.getInstance().getSoundManager();

        // Play the click off sound.
        sm.play(SimpleSoundInstance.forUI(ModSounds.HEALTHSCREEN_CLOSE.get(), 1.0f));

        // Kill the radio loop.
        if (radioLoop != null)
        {
            sm.stop(radioLoop);
            radioLoop = null;
        }
    }

    private void startInteraction(LimbNode node, NodeAction action)
    {
        PlayerHealthData self = target.getCapability(PlayerHealthCapability.PLAYER_HEALTH).resolve().orElse(null);
        long duration = NodeInteractionOptions.castDurationMs(action, self);

        // If it's instant, then it just applies the action directly.
        if (duration <= 0L)
            ModNetwork.CHANNEL.sendToServer(new ServerboundNodeActionPacket(node, action));
        // But if it's not, it calls the cursor cast to initialize.
        else
            ClientCastState.start(node, action, duration);
    }

    @Override
    public void tick()
    {
        super.tick();
        if (ClientCastState.isComplete())
        {
            ModNetwork.CHANNEL.sendToServer(new ServerboundNodeActionPacket(ClientCastState.node(), ClientCastState.action()));
            ClientCastState.cancel();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        // Escape closes normally.
        if (keyCode == 256)
        {
            // HOWEVER. If the context menu is open, then it'll close
            // the context menu first. Just QoL.
            if (contextMenu.isOpen())
            {
                contextMenu.close();
                return true;
            }

            // Pressing escape during an active cast animation cancels the action.
            if (ClientCastState.isActive())
            {
                ClientCastState.cancel();
                return true;
            }

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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        // A click anywhere cancels an in-progress cast (unless the menu's handling it).
        if (ClientCastState.isActive() && !contextMenu.isOpen())
        {
            ClientCastState.cancel();
            return true;
        }

        // Open menu eats clicks first (select a row, or close on outside-click).
        if (contextMenu.isOpen() && contextMenu.mouseClicked(mouseX, mouseY))
            return true;

        // Right-click on a node opens its context menu.
        if (button == 1)
        {
            LimbNode hit = anatomyMap.nodeAt((int) mouseX, (int) mouseY);
            if (hit != null)
            {
                List<ContextMenuComponent.MenuOption> opts = buildNodeOptions(hit);
                if (!opts.isEmpty())
                    contextMenu.open((int) mouseX, (int) mouseY, opts);

                return true;
            }
        }

        // Begin dragging an item out of the hotbar strip.
        if (button == 0)
        {
            int slot = slotAt(mouseX, mouseY);
            if (slot >= 0 && !target.getInventory().getItem(slot).isEmpty())
            {
                draggingFromSlot = slot;
                return true;
            }
        }

        // Left-click selects a node.
        if (button == 0)
        {
            LimbNode hit = anatomyMap.nodeAt((int) mouseX, (int) mouseY);
            LimbNode prev = anatomyMap.getSelected();
            anatomyMap.setSelected(hit);

            if (hit != prev)
                rightPanel.resetScroll();
            if (hit != null)
                return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (button == 0 && draggingFromSlot >= 0)
        {
            int slot = draggingFromSlot;
            draggingFromSlot = -1;

            LimbNode hit = anatomyMap.nodeAt((int) mouseX, (int) mouseY);
            if (hit != null)
                ModNetwork.CHANNEL.sendToServer(new ServerboundApplyItemToNodePacket(slot, hit));

            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (rightPanel.mouseScrolled(mouseX, mouseY, delta))
            return true;

        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
