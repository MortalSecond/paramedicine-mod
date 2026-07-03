package net.invinciblemoebius.traumaparamedicinemod.overlays;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.invinciblemoebius.traumaparamedicinemod.client.ModKeybinds;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.network.DeathCause;
import net.invinciblemoebius.traumaparamedicinemod.network.ModNetwork;
import net.invinciblemoebius.traumaparamedicinemod.network.packets.ServerboundDeathPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.joml.Matrix4f;

public class GiveUpOverlay
{
    private static final long HOLD_DURATION_MS = 2000L;
    private static long holdStartMs = 0L;
    private static boolean sent = false;

    public static void clientTick()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            reset();
            return;
        }

        boolean holding = eligible() && mc.screen == null && ModKeybinds.GIVE_UP.isDown();
        if (!holding)
        {
            reset();
            return;
        }

        if (holdStartMs == 0L)
            holdStartMs = System.currentTimeMillis();

        if (!sent && progress() >= 1f)
        {
            ModNetwork.CHANNEL.sendToServer(new ServerboundDeathPacket(DeathCause.GIVE_UP));
            sent = true;
        }
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight)
    {
        if (!eligible())
            return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Component text = Component.translatable("hud.paramedicine.give_up",
                ModKeybinds.GIVE_UP.getTranslatedKeyMessage());

        int margin = 8;
        int textW = font.width(text);
        int textX = screenWidth - margin - textW;
        int textY = margin + 2;

        g.drawString(font, text, textX, textY, 0xFFE0E0E0, true);
    }

    private static boolean eligible()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return false;

        return mc.player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).map(PlayerHealthData::canGiveUp).orElse(false);
    }

    private static float progress()
    {
        if (holdStartMs == 0L)
            return 0f;

        return Math.min(1f, (System.currentTimeMillis() - holdStartMs) / (float) HOLD_DURATION_MS);
    }

    private static void reset()
    {
        holdStartMs = 0L;
        sent = false;
    }
}