package net.invinciblemoebius.traumaparamedicinemod.ui;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.menu.DressingStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class DressingStationScreen extends AbstractContainerScreen<DressingStationMenu>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(ParamedicineMod.MOD_ID, "textures/gui/menu/dressing_station_ui.png");
    private static final int TEX_W = 176, TEX_H = 180;

    private static final int BOX_X = 41, BOX_Y = 8, BOX_W = 88, BOX_H = 66;
    private static final int BTN_X = 76, BTN_Y = 81, BTN_W = 20, BTN_H = 18;
    private static final int INK = 0xFFE8E4D8;

    public DressingStationScreen(DressingStationMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title);
        this.imageWidth = TEX_W;
        this.imageHeight = TEX_H;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY)
    {
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        // Make-button state overlay.
        int bx = x + BTN_X, by = y + BTN_Y;
        if (!menu.canMake())
            g.fill(bx, by, bx + BTN_W, by + BTN_H, 0x80202020);
        else if (mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H)
            g.fill(bx, by, bx + BTN_W, by + BTN_H, 0x40FFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY)
    {
        // Qualitative summary, written into the black box.
        List<String> bands = menu.getSummary();
        int maxLines = (BOX_H - 6) / 10;
        for (int i = 0; i < bands.size() && i < maxLines; i++)
            g.drawString(font, bands.get(i), BOX_X + 4, BOX_Y + 4 + i * 10, INK, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn)
    {
        if (btn == 0 && menu.canMake())
        {
            int bx = leftPos + BTN_X, by = topPos + BTN_Y;
            if (mx >= bx && mx < bx + BTN_W && my >= by && my < by + BTN_H)
            {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DressingStationMenu.BUTTON_MAKE);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}