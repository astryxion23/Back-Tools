package com.astryxion.backtools.client.core;

import com.astryxion.backtools.client.render.BackToolLayer;
import com.astryxion.backtools.common.BackTools;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class ClientModBusEvents
{
	private static final Set<PlayerRenderer> LAYERED = Collections.newSetFromMap(new WeakHashMap<>());

	private ClientModBusEvents()
	{
	}

	public static void registerPlayerLayer()
	{
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityType == EntityType.PLAYER && entityRenderer instanceof PlayerRenderer playerRenderer && LAYERED.add(playerRenderer))
			{
				registrationHelper.register(new BackToolLayer(playerRenderer));
				BackTools.LOGGER.info("Back Tools: attached BackToolLayer to a PlayerRenderer instance");
			}
		});
	}

	public static void onConfigReloaded()
	{
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (mc != null)
		{
			mc.execute(EventHandler::setupConfig);
		}
	}
}
