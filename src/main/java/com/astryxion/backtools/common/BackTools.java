package com.astryxion.backtools.common;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class BackTools implements ModInitializer
{
    public static final String MOD_ID = "backtools";
    public static final String MOD_NAME = "Back Tools";

    public static final Logger LOGGER = LogManager.getLogger();

    public static final HashMap<Class<? extends Item>, Integer> imcOrientation = new HashMap<>();
    public static final HashSet<ResourceLocation> imcDisabledTools = new HashSet<>();

    private static final ConcurrentLinkedQueue<PendingImc> IMC_QUEUE = new ConcurrentLinkedQueue<>();

    public record PendingImc(String senderModId, String method, Supplier<Object> supplier)
    {
    }

    public static void submitInterModComms(String senderModId, String method, Supplier<Object> supplier)
    {
        IMC_QUEUE.add(new PendingImc(senderModId, method, supplier));
    }

    public static void processQueuedInterModMessages()
    {
        List<PendingImc> drained = new ArrayList<>();
        PendingImc m;
        while ((m = IMC_QUEUE.poll()) != null)
        {
            drained.add(m);
        }

        drained.stream().filter(msg -> msg.method().equalsIgnoreCase("blacklist")).forEach(msg -> {
            Object o = msg.supplier().get();
            if (o instanceof ItemStack is)
            {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(is.getItem());
                if (!is.isEmpty() && key != null && imcDisabledTools.add(key))
                {
                    LOGGER.info("IMC-{}: Disabled {}", msg.senderModId(), key);
                }
                else
                {
                    LOGGER.warn("IMC-{}: Unable to disable: {}", msg.senderModId(), is);
                }
            }
            else if (o instanceof Item item)
            {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                if (key != null && imcDisabledTools.add(key))
                {
                    LOGGER.info("IMC-{}: Disabled {}", msg.senderModId(), item);
                }
                else
                {
                    LOGGER.warn("IMC-{}: Unable to disable: {}", msg.senderModId(), item);
                }
            }
            else if (o instanceof ResourceLocation rl)
            {
                if (imcDisabledTools.add(rl))
                {
                    LOGGER.info("IMC-{}: Disabled {}", msg.senderModId(), rl);
                }
                else
                {
                    LOGGER.warn("IMC-{}: Unable to disable: {}", msg.senderModId(), rl);
                }
            }
            else if (o instanceof String s)
            {
                ResourceLocation loc = ResourceLocation.tryParse(s);
                if (loc == null)
                {
                    LOGGER.warn("IMC-{}: Invalid resource location: {}", msg.senderModId(), s);
                }
                else if (imcDisabledTools.add(loc))
                {
                    LOGGER.info("IMC-{}: Disabled {}", msg.senderModId(), s);
                }
                else
                {
                    LOGGER.warn("IMC-{}: Unable to disable: {}", msg.senderModId(), s);
                }
            }
        });
        drained.stream().filter(msg -> msg.method().equalsIgnoreCase("orientation")).forEach(msg -> {
            Object o = msg.supplier().get();
            if (!(o instanceof String s))
            {
                BackTools.LOGGER.warn("IMC-{}: Passed object is not a string: {}", msg.senderModId(), o);
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
                BackTools.LOGGER.warn("IMC-{}: Could not parse orientation: {}", msg.senderModId(), s);
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
                    BackTools.LOGGER.warn("IMC-{}: Class does not extend Item class: {}", msg.senderModId(), split[0]);
                }
            }
            catch (ClassNotFoundException e)
            {
                BackTools.LOGGER.warn("IMC-{}: Could not find class to add orientation: {}", msg.senderModId(), split[0]);
            }
            catch (NumberFormatException e)
            {
                BackTools.LOGGER.warn("IMC-{}: Could not parse integer: {}", msg.senderModId(), s);
            }
        });
    }

    @Override
    public void onInitialize()
    {
    }
}
