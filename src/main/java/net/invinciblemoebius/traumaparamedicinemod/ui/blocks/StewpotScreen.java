package net.invinciblemoebius.traumaparamedicinemod.ui.blocks;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.menu.designs.StewpotMenu;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceColor;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StewpotScreen extends AbstractContainerScreen<StewpotMenu>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(ParamedicineMod.MOD_ID, "textures/gui/menu/stewpot_ui.png");
    // Total size of the UI PNG texture.
    private static final int TEX_W = 176;
    private static final int TEX_H = 180;
    // Pot interior window, where fluid shows.
    private static final int POT_X = 67, POT_Y = 49, POT_W = 57, POT_H = 41;
    // Temperature gauge strip.
    private static final int GAUGE_X = 48, GAUGE_Y = 36, GAUGE_W = 5, GAUGE_H = 61;
    // Color of the gauge cover.
    private static final int PANEL_GREY = 0xFFC9C9C9;

    public StewpotScreen(StewpotMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = TEX_W;
        this.imageHeight = TEX_H;
    }

    // So, let me explain.
    // The pot thing in the middle isn't actually "filled" as per its silhouette,
    // rather, it's a transparent socket cut into the UI itself. The way the fluid
    // shows is by putting a black square two layers behind the UI (the "backdrop"),
    // and then putting more squares atop eachother one layer behind the UI. That way,
    // the pot is colored black when not filled, but colored with a multitude of
    // colors depending on what is inside the pot.
    // The temperature gauge works inversely (because MC doesn't have native gradients).
    // It's already pre-rendered on the PNG, so instead there's a UI-colored square
    // on top of it that shrinks relative to the temperature of the pot.
    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos, y = this.topPos;

        // Black backdrop and fill in the pot window first...
        int potX = x + POT_X, potY = y + POT_Y;
        g.fill(potX, potY, potX + POT_W, potY + POT_H, 0xFF000000);

        FluidMixture contents = menu.getContents();
        if (!contents.isEmpty())
        {
            float cap = menu.getCapacity();
            List<Map.Entry<SubstanceType, Float>> comps = new ArrayList<>(contents.getComponents().entrySet());
            comps.sort((a, b) -> Float.compare(b.getValue(), a.getValue())); // bulk solvent at the bottom

            int bottom = potY + POT_H;
            for (Map.Entry<SubstanceType, Float> e : comps)
            {
                int h = Math.round((e.getValue() / cap) * POT_H);
                if (h <= 0)
                    continue;

                int top = Math.max(potY, bottom - h);
                g.fill(potX, top, potX + POT_W, bottom, SubstanceColor.get(e.getKey()));
                bottom = top;
            }
        }

        // ...then the panel on top. The transparent pot window reveals the fill.
        g.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        // Temperature gauge cover. Grey block over the gradient, shrinking as it heats.
        float t = Mth.clamp(menu.getTemperature(), 0f, 1f);
        int coverH = Math.round((1f - t) * GAUGE_H);
        if (coverH > 0)
            g.fill(x + GAUGE_X, y + GAUGE_Y, x + GAUGE_X + GAUGE_W, y + GAUGE_Y + coverH, PANEL_GREY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY)
    {
        // Dynamic panel descriptions.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        int potX = this.leftPos + POT_X, potY = this.topPos + POT_Y;
        if (mouseX >= potX && mouseX < potX + POT_W && mouseY >= potY && mouseY < potY + POT_H)
        {
            FluidMixture c = menu.getContents();
            List<Component> lines = new ArrayList<>();
            if (c.isEmpty())
                lines.add(Component.literal("Empty"));
            else
            {
                for (Map.Entry<SubstanceType, Float> e : c.getComponents().entrySet())
                    lines.add(Component.literal(String.format("%s: %.0f mL", e.getKey(), e.getValue())));
                lines.add(Component.literal(String.format("Total: %.0f / %.0f mL", c.totalVolume(), menu.getCapacity())));
            }
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }

        this.renderTooltip(g, mouseX, mouseY);
    }
}