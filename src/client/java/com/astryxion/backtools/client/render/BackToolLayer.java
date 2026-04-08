package com.astryxion.backtools.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.astryxion.backtools.client.core.EventHandler;
import com.astryxion.backtools.common.core.BackToolsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;

public class BackToolLayer extends RenderLayer<AvatarRenderState, PlayerModel>
{
	public BackToolLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer)
	{
		super(renderer);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, AvatarRenderState state, float limbSwing, float limbSwingAmount)
	{
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
		{
			return;
		}

		AbstractClientPlayer player = resolvePlayerForRender(mc, state);
		if (player == null)
		{
			return;
		}

		if (skipBackToolsForCape(state, player))
		{
			return;
		}

		if (!state.isInvisible && state.pose != Pose.SLEEPING && EventHandler.heldTools.containsKey(player))
		{
			EventHandler.HeldInfo info = EventHandler.heldTools.get(player);
			String scoreName = state.scoreText != null ? state.scoreText.getString() : null;
			boolean enableEasterEgg = BackToolsConfig.EASTER_EGG.get() && (state.pose == Pose.SWIMMING || state.isFallFlying || (scoreName != null && scoreName.equalsIgnoreCase("iChun")));

			poseStack.pushPose();

			float offset = !state.chestEquipment.isEmpty() ? 1.0F : state.showJacket ? 0.5F : 0F;
			boolean mainIsRight = state.mainArm == HumanoidArm.RIGHT;
			int tickAnim = enableEasterEgg ? player.tickCount : 0;
			float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

			getParentModel().body.translateAndRotate(poseStack);
			renderBackItems(poseStack, collector, packedLight, player, info.lastMain, info.lastOff, mainIsRight, tickAnim, partialTick, offset, state.outlineColor);

			poseStack.popPose();
		}
	}

	/**
	 * Match vanilla {@code AvatarRenderer.extractRenderState}: {@link AvatarRenderState#id} is {@link Player#getId()}.
	 * Only resolve by id / profile name — never by position (can pick the wrong player or miss when coords diverge).
	 */
	private static AbstractClientPlayer resolvePlayerForRender(Minecraft mc, AvatarRenderState state)
	{
		Level level = mc.level;
		Entity entity = level.getEntity(state.id);
		if (entity instanceof AbstractClientPlayer acp)
		{
			return acp;
		}

		LocalPlayer local = mc.player;
		if (local != null && local.level() == level)
		{
			if (local.getId() == state.id)
			{
				return local;
			}
			String scoreName = state.scoreText != null ? state.scoreText.getString() : null;
			if (scoreName != null && scoreName.equals(local.getGameProfile().name()))
			{
				return local;
			}
		}

		for (Player p : level.players())
		{
			if (p.getId() == state.id && p instanceof AbstractClientPlayer acp)
			{
				return acp;
			}
		}

		String scoreName = state.scoreText != null ? state.scoreText.getString() : null;
		if (scoreName != null && !scoreName.isEmpty())
		{
			for (Player p : level.players())
			{
				if (p instanceof AbstractClientPlayer acp)
				{
					if (scoreName.equals(acp.getGameProfile().name()) || scoreName.equals(acp.getName().getString()))
					{
						return acp;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Classic Back Tools: no back items when a cape would show. Default config = skip. Set {@code showBackToolsWithCape}
	 * to override. Uses the same test as src 1.21.4 on {@link AvatarRenderState}, plus the live player when 1.21.x
	 * state does not match what you see in third person.
	 */
	private static boolean skipBackToolsForCape(AvatarRenderState state, AbstractClientPlayer player)
	{
		if (BackToolsConfig.SHOW_BACK_TOOLS_WITH_CAPE.get())
		{
			return false;
		}
		boolean like124 = state.showCape && state.skin != null && state.skin.cape() != null;
		var skin = player.getSkin();
		boolean fromPlayer = player.isModelPartShown(PlayerModelPart.CAPE) && skin != null && skin.cape() != null;
		return like124 || fromPlayer;
	}

	private static void renderBackItems(
		PoseStack poseStack,
		SubmitNodeCollector collector,
		int packedLight,
		LivingEntity renderEntity,
		ItemStack mainStack,
		ItemStack offStack,
		boolean mainIsRight,
		int ticks,
		float partialTick,
		float offset,
		int outlineColor
	)
	{
		Minecraft mc = Minecraft.getInstance();
		ItemModelResolver resolver = mc.getItemModelResolver();
		ItemStackRenderState itemRenderState = new ItemStackRenderState();

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
			resolver.updateForLiving(itemRenderState, mainStack, ItemDisplayContext.FIXED, renderEntity);
			itemRenderState.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, outlineColor);
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
			resolver.updateForLiving(itemRenderState, offStack, ItemDisplayContext.FIXED, renderEntity);
			itemRenderState.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, outlineColor);
		}
	}
}
