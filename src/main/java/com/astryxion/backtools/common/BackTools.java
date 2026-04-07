package com.astryxion.backtools.common;

import com.astryxion.backtools.client.core.ClientModBusEvents;
import com.astryxion.backtools.client.core.EventHandler;
import com.astryxion.backtools.common.core.BackToolsConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;

@Mod(value = BackTools.MOD_ID, dist = { Dist.CLIENT })
public class BackTools
{
    public static final String MOD_ID = "backtools";

    public static final Logger LOGGER = LogManager.getLogger();

    public static HashMap<Class<? extends Item>, Integer> imcOrientation = new HashMap<>();
    public static HashSet<ResourceLocation> imcDisabledTools = new HashSet<>();

    public BackTools(IEventBus modEventBus, ModContainer modContainer)
    {
        modContainer.registerConfig(ModConfig.Type.CLIENT, BackToolsConfig.SPEC, BackToolsConfig.fileName());

        modEventBus.addListener(ClientModBusEvents::onAddLayers);
        modEventBus.addListener(ClientModBusEvents::onModConfigLoading);
        modEventBus.addListener(ClientModBusEvents::onModConfigReloading);

        modEventBus.addListener(this::processIMC);
        modEventBus.addListener(this::finishLoading);
    }

    @OnlyIn(Dist.CLIENT)
    private void finishLoading(FMLLoadCompleteEvent event)
    {
        EventHandler.setupConfig();
    }

    @OnlyIn(Dist.CLIENT)
    private void processIMC(InterModProcessEvent event)
    {
        event.getIMCStream(m -> m.equalsIgnoreCase("blacklist")).forEach(msg -> {
            Object o = msg.getMessageSupplier().get();
            if (o instanceof ItemStack is)
            {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(is.getItem());
                if (!is.isEmpty() && key != null && imcDisabledTools.add(key))
                {
                    LOGGER.info("IMC-{}: Disabled {}", msg.getSenderModId(), key);
                }
                else
                {
                    LOGGER.warn("IMC-{}: Unable to disable: {}", msg.getSenderModId(), is);
                }
            }
            else if (o instanceof Item item)
            {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                if (key != null && imcDisabledTools.add(key))
                {
                    LOGGER.info("IMC-{}: Disabled {}", msg.getSenderModId(), item);
                }
                else
                {
                    LOGGER.warn("IMC-{}: Unable to disable: {}", msg.getSenderModId(), item);
                }
            }
            else if (o instanceof ResourceLocation rl)
            {
                if (imcDisabledTools.add(rl))
                {
                    LOGGER.info("IMC-{}: Disabled {}", msg.getSenderModId(), rl);
                }
                else
                {
                    LOGGER.warn("IMC-{}: Unable to disable: {}", msg.getSenderModId(), rl);
                }
            }
            else if (o instanceof String s)
            {
                ResourceLocation loc = ResourceLocation.tryParse(s);
                if (loc == null)
                {
                    LOGGER.warn("IMC-{}: Invalid resource location: {}", msg.getSenderModId(), s);
                }
                else if (imcDisabledTools.add(loc))
                {
                    LOGGER.info("IMC-{}: Disabled {}", msg.getSenderModId(), s);
                }
                else
                {
                    LOGGER.warn("IMC-{}: Unable to disable: {}", msg.getSenderModId(), s);
                }
            }
        });
        event.getIMCStream(m -> m.equalsIgnoreCase("orientation")).forEach(msg -> {
            Object o = msg.getMessageSupplier().get();
            if (!(o instanceof String s))
            {
                BackTools.LOGGER.warn("IMC-{}: Passed object is not a string: {}", msg.getSenderModId(), o);
                return;
            }

            String[] split = new String[2];
            int index = s.indexOf(':');
            if (index > 0)
            {
                split[0] = s.substring(0, index);
                split[1] = s.substring(index + 1);
            }
            else
            {
                BackTools.LOGGER.warn("IMC-{}: Could not parse orientation: {}", msg.getSenderModId(), s);
                return;
            }
            try
            {
                Class<?> clz = Class.forName(split[0]);
                if (Item.class.isAssignableFrom(clz))
                {
                    @SuppressWarnings("unchecked")
                    Class<? extends Item> itemClass = (Class<? extends Item>) clz;
                    imcOrientation.put(itemClass, Integer.parseInt(split[1]));
                }
                else
                {
                    BackTools.LOGGER.warn("IMC-{}: Class does not extend Item class: {}", msg.getSenderModId(), split[0]);
                }
            }
            catch (ClassNotFoundException e)
            {
                BackTools.LOGGER.warn("IMC-{}: Could not find class to add orientation: {}", msg.getSenderModId(), split[0]);
            }
            catch (NumberFormatException e)
            {
                BackTools.LOGGER.warn("IMC-{}: Could not parse integer: {}", msg.getSenderModId(), s);
            }
        });
    }
}
