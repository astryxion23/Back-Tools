package com.astryxion.backtools.client.core;

import com.astryxion.backtools.client.render.BackToolLayer;
import com.astryxion.backtools.common.BackTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = BackTools.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModBusEvents
{
    private ClientModBusEvents()
    {
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event)
    {
        for (String skinName : event.getSkins())
        {
            EntityRenderer<? extends Player> renderer = event.getPlayerSkin(skinName);
            if (renderer instanceof PlayerRenderer playerRenderer)
            {
                playerRenderer.addLayer(new BackToolLayer(playerRenderer));
            }
        }
    }

    @SubscribeEvent
    public static void onModConfig(ModConfigEvent event)
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
