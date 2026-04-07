package com.astryxion.backtools.client.core;

import com.astryxion.backtools.client.render.BackToolLayer;
import com.astryxion.backtools.common.BackTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class ClientModBusEvents
{
    private ClientModBusEvents()
    {
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event)
    {
        for (PlayerSkin.Model model : event.getSkins())
        {
            EntityRenderer<? extends Player, ?> renderer = event.getSkin(model);
            if (renderer instanceof PlayerRenderer playerRenderer)
            {
                playerRenderer.addLayer(new BackToolLayer(playerRenderer));
            }
        }
    }

    public static void onModConfigLoading(ModConfigEvent.Loading event)
    {
        onModConfig(event);
    }

    public static void onModConfigReloading(ModConfigEvent.Reloading event)
    {
        onModConfig(event);
    }

    private static void onModConfig(ModConfigEvent event)
    {
        if (!BackTools.MOD_ID.equals(event.getConfig().getModId()))
        {
            return;
        }
        if (event.getConfig().getType() != ModConfig.Type.CLIENT)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null)
        {
            mc.execute(EventHandler::setupConfig);
        }
    }
}
