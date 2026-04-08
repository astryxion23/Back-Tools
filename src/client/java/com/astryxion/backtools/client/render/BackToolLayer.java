package com.astryxion.backtools.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.astryxion.backtools.client.core.EventHandler;
import com.astryxion.backtools.common.core.BackToolsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;

public class BackToolLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
    public BackToolLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer)
    {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch)
    {
        boolean capeWouldHideBackTools = player.isModelPartShown(PlayerModelPart.CAPE) && player.getCloakTextureLocation() != null;
        boolean allowRenderDespiteCape = BackToolsConfig.showBackToolsWithCape();
        if ((!capeWouldHideBackTools || allowRenderDespiteCape) && !player.isInvisible() && !player.isSleeping() && EventHandler.heldTools.containsKey(player))
        {
            EventHandler.HeldInfo info = EventHandler.heldTools.get(player);
            boolean enableEasterEgg = BackToolsConfig.easterEgg() && (player.getPose() == Pose.SWIMMING || player.isFallFlying() || player.getName().getString().equalsIgnoreCase("iChun"));

            poseStack.pushPose();

            float offset = !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ? 1.0F : player.isModelPartShown(PlayerModelPart.JACKET) ? 0.5F : 0F;
            boolean mainIsRight = player.getMainArm() == HumanoidArm.RIGHT;
            int tickAnim = enableEasterEgg ? player.tickCount : 0;

            getParentModel().body.translateAndRotate(poseStack);
            renderBackItems(poseStack, buffer, packedLight, player.level(), info.lastMain, info.lastOff, mainIsRight, tickAnim, partialTick, offset);

            poseStack.popPose();
        }
    }

    private static void renderBackItems(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Level level, ItemStack mainStack, ItemStack offStack, boolean mainIsRight, int ticks, float partialTick, float offset)
    {
        poseStack.translate(0F, 4F / 16F, 1.91F / 16F + (offset / 16F));
        if (!mainStack.isEmpty())
        {
            poseStack.pushPose();
            poseStack.translate(0F, 0F, 0.025F);
            boolean isShield = mainStack.getItem() instanceof ShieldItem;
            if (mainIsRight)
            {
                poseStack.scale(-1F, 1F, -1F);
            }
            if (isShield)
            {
                if (!mainIsRight)
                {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180F));
                }
                float scale = 1.5F;
                poseStack.scale(scale, scale, scale);
                if (!mainIsRight)
                {
                    poseStack.translate(-2.5F / 16F, 2F / 16F, 1.25F / 16F);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-25F));
                }
                else
                {
                    poseStack.translate(-1F / 16F, 0.25F / 16F, 1.25F / 16F);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(25F));
                }
            }

            if (!isShield)
            {
                int i = EventHandler.getToolOrientation(mainStack.getItem());
                poseStack.mulPose(Axis.ZP.rotationDegrees(-i));
                if (ticks > 0)
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees((ticks + partialTick) * 40F));
                }
            }
            Minecraft.getInstance().getItemRenderer().renderStatic(mainStack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, level, 0);
            poseStack.popPose();
        }
        if (!offStack.isEmpty())
        {
            boolean isShield = offStack.getItem() instanceof ShieldItem;
            if (!mainIsRight)
            {
                poseStack.scale(-1F, 1F, -1F);
            }
            if (isShield)
            {
                if (mainIsRight)
                {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180F));
                }
                float scale = 1.5F;
                poseStack.scale(scale, scale, scale);
                if (mainIsRight)
                {
                    poseStack.translate(-2.5F / 16F, 2F / 16F, 1.25F / 16F);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-25F));
                }
                else
                {
                    poseStack.translate(-1F / 16F, 0.25F / 16F, 1.25F / 16F);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(25F));
                }
            }
            if (!isShield)
            {
                int i = EventHandler.getToolOrientation(offStack.getItem());
                poseStack.mulPose(Axis.ZP.rotationDegrees(-i));
                if (ticks > 0)
                {
                    poseStack.mulPose(Axis.ZP.rotationDegrees((ticks + partialTick) * 40F));
                }
            }
            Minecraft.getInstance().getItemRenderer().renderStatic(offStack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, level, 0);
        }
    }
}
