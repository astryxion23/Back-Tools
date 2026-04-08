package com.astryxion.backtools.client;

import com.astryxion.backtools.client.core.EventHandler;
import com.astryxion.backtools.client.render.BackToolLayer;
import com.astryxion.backtools.common.BackTools;
import com.astryxion.backtools.common.core.BackToolsConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

public class BackToolsClient implements ClientModInitializer
{
    private int ticksUntilConfigPoll;

    @Override
    public void onInitializeClient()
    {
        BackToolsConfig.loadFromDisk();

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.level == null)
            {
                return;
            }
            for (Player player : client.level.players())
            {
                EventHandler.onPlayerTick(player);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (--ticksUntilConfigPoll > 0)
            {
                return;
            }
            ticksUntilConfigPoll = 20;
            if (BackToolsConfig.reloadIfChanged())
            {
                client.execute(EventHandler::setupConfig);
            }
        });

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> EventHandler.onEntityLoad(entity, world));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(EventHandler::clean));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(EventHandler::clean));

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityType == EntityType.PLAYER && entityRenderer instanceof PlayerRenderer playerRenderer)
            {
                registrationHelper.register(new BackToolLayer(playerRenderer));
            }
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            BackTools.processQueuedInterModMessages();
            EventHandler.setupConfig();
        });
    }
}
