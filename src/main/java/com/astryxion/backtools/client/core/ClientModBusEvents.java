package com.astryxion.backtools.client.core;

import com.astryxion.backtools.client.render.BackToolLayer;
import com.astryxion.backtools.common.BackTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class ClientModBusEvents
{
    private ClientModBusEvents()
    {
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event)
    {
        // 1.21.x uses render-state layers (PlayerRenderState), not the pre-1.21.2 render(..., Player, limbSwing...) override.
        Set<PlayerRenderer> layered = Collections.newSetFromMap(new IdentityHashMap<>());
        int added = 0;
        for (PlayerSkin.Model model : event.getSkins())
        {
            EntityRenderer<? extends Player, ?> renderer = event.getSkin(model);
            if (renderer == null)
            {
                continue;
            }
            if (renderer instanceof PlayerRenderer playerRenderer && layered.add(playerRenderer))
            {
                playerRenderer.addLayer(new BackToolLayer(playerRenderer));
                added++;
            }
        }
        if (event.getEntityTypes().contains(EntityType.PLAYER))
        {
            Object playerRendererObj = event.getRenderer(EntityType.PLAYER);
            if (playerRendererObj instanceof PlayerRenderer playerRenderer && layered.add(playerRenderer))
            {
                playerRenderer.addLayer(new BackToolLayer(playerRenderer));
                added++;
            }
        }
        if (added > 0)
        {
            BackTools.LOGGER.info("Back Tools: attached BackToolLayer to {} PlayerRenderer instance(s)", added);
        }
        else
        {
            BackTools.LOGGER.warn("Back Tools: AddLayers found no PlayerRenderer to attach BackToolLayer — in-world player may not show back items.");
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
