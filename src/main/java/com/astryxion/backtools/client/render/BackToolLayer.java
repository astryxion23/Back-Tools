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
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;

public class BackToolLayer extends RenderLayer<PlayerRenderState, PlayerModel>
{
    public BackToolLayer(RenderLayerParent<PlayerRenderState, PlayerModel> renderer)
    {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, PlayerRenderState state, float limbSwing, float limbSwingAmount)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
        {
            return;
        }
        Entity entity = mc.level.getEntity(state.id);
        if (!(entity instanceof AbstractClientPlayer player))
        {
            return;
        }

        boolean capeWouldHideBackTools = state.showCape && state.skin != null && state.skin.capeTexture() != null;
        boolean allowRenderDespiteCape = BackToolsConfig.SHOW_BACK_TOOLS_WITH_CAPE.get();
        if ((!capeWouldHideBackTools || allowRenderDespiteCape) && !state.isInvisible && !state.hasPose(Pose.SLEEPING) && EventHandler.heldTools.containsKey(player))
        {
            EventHandler.HeldInfo info = EventHandler.heldTools.get(player);
            boolean enableEasterEgg = BackToolsConfig.EASTER_EGG.get() && (state.pose == Pose.SWIMMING || state.isFallFlying || (state.name != null && state.name.equalsIgnoreCase("iChun")));

            poseStack.pushPose();

            float offset = !state.chestItem.isEmpty() ? 1.0F : state.showJacket ? 0.5F : 0F;
            boolean mainIsRight = state.mainArm == HumanoidArm.RIGHT;
            int tickAnim = enableEasterEgg ? player.tickCount : 0;
            float partialTick = state.partialTick;

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
