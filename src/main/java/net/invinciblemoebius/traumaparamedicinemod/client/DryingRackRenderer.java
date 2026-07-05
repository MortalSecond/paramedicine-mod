package net.invinciblemoebius.traumaparamedicinemod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.invinciblemoebius.traumaparamedicinemod.block.entities.DryingRackBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class DryingRackRenderer implements BlockEntityRenderer<DryingRackBlockEntity>
{
    private final ItemRenderer itemRenderer;

    public DryingRackRenderer(BlockEntityRendererProvider.Context ctx)
    {
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(DryingRackBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buf, int light, int overlay)
    {
        ItemStack stack = be.getStored();
        if (stack.isEmpty())
            return;

        pose.pushPose();
        // This is over the drying bars (~y7-8 in the model)
        pose.translate(0.5, 8.0 / 16.0, 0.5);
        // Lays it flat on the rack
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(0.5f, 0.5f, 0.5f);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buf, be.getLevel(), 0);
        pose.popPose();
    }
}