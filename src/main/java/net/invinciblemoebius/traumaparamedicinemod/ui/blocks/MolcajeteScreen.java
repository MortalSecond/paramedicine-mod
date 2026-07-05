package net.invinciblemoebius.traumaparamedicinemod.ui.blocks;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.menu.designs.MolcajeteMenu;
import net.invinciblemoebius.traumaparamedicinemod.substance.PowderMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceColor;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MolcajeteScreen extends AbstractContainerScreen<MolcajeteMenu>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(ParamedicineMod.MOD_ID, "textures/gui/menu/molcajete_ui.png");
    private static final int TEX_W = 176, TEX_H = 180;
    // Silhouette window.
    private static final int POWDER_X = 71, POWDER_Y = 43, POWDER_W = 32, POWDER_H = 49;
    // Buttons.
    private static final int GRIND_X = 22, GRIND_Y = 55;
    private static final int EXPORT_X = 114, EXPORT_Y = 55;
    private static final int BTN_W = 21, BTN_H = 19;

    public MolcajeteScreen(MolcajeteMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title);
        this.imageWidth = TEX_W;
        this.imageHeight = TEX_H;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY)
    {
        int x = leftPos, y = topPos;

        // Black backdrop and stacked powder fill behind the transparent window.
        int px = x + POWDER_X, py = y + POWDER_Y;
        g.fill(px, py, px + POWDER_W, py + POWDER_H, 0xFF000000);

        PowderMixture contents = menu.getContents();
        if (!contents.isEmpty())
        {
            float cap = menu.getCapacity();
            List<Map.Entry<SubstanceType, Float>> components = new ArrayList<>(contents.getComponents().entrySet());
            components.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
            int bottom = py + POWDER_H;
            for (Map.Entry<SubstanceType, Float> entry : components)
            {
                int h = Math.round((entry.getValue() / cap) * POWDER_H);
                if (h <= 0)
                    continue;

                int top = Math.max(py, bottom - h);
                g.fill(px, top, px + POWDER_W, bottom, SubstanceColor.get(entry.getKey()));
                bottom = top;
            }
        }

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        // Button state overlays (drawn over the pre-made button art).
        drawButtonState(g, GRIND_X, GRIND_Y, menu.canGrind(), mouseX, mouseY);
        drawButtonState(g, EXPORT_X, EXPORT_Y, menu.canExport(), mouseX, mouseY);
    }

    private void drawButtonState(GuiGraphics g, int bx, int by, boolean enabled, int mouseX, int mouseY)
    {
        int x = leftPos + bx, y = topPos + by;
        if (!enabled)
            // Darken on disabled.
            g.fill(x, y, x + BTN_W, y + BTN_H, 0x80202020);
        else if (mouseX >= x && mouseX < x + BTN_W && mouseY >= y && mouseY < y + BTN_H)
            // Highlight on hover.
            g.fill(x, y, x + BTN_W, y + BTN_H, 0x40FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn)
    {
        if (btn == 0)
        {
            if (menu.canGrind() && inButton(mx, my, GRIND_X, GRIND_Y))
            {
                press(MolcajeteMenu.BUTTON_GRIND);
                return true;
            }
            if (menu.canExport() && inButton(mx, my, EXPORT_X, EXPORT_Y))
            {
                press(MolcajeteMenu.BUTTON_EXPORT);
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private boolean inButton(double mx, double my, int bx, int by)
    {
        int x = leftPos + bx, y = topPos + by;
        return mx >= x && mx < x + BTN_W && my >= y && my < y + BTN_H;
    }

    private void press(int id)
    {
        this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        int px = leftPos + POWDER_X, py = topPos + POWDER_Y;
        if (mouseX >= px && mouseX < px + POWDER_W && mouseY >= py && mouseY < py + POWDER_H)
        {
            PowderMixture content = menu.getContents();
            List<Component> lines = new ArrayList<>();
            if (content.isEmpty())
                lines.add(Component.literal("Empty"));
            else
            {
                for (Map.Entry<SubstanceType, Float> entry : content.getComponents().entrySet())
                    lines.add(Component.literal(String.format("%s: %.1f mg", entry.getKey(), entry.getValue())));
                lines.add(Component.literal(String.format("Total: %.1f / %.0f mg", content.totalMass(), menu.getCapacity())));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
        renderTooltip(g, mouseX, mouseY);
    }
}