package com.astryxion.backtools.common;

import com.astryxion.backtools.client.core.EventHandler;
import com.astryxion.backtools.common.core.BackToolsConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;

@Mod(BackTools.MOD_ID)
public class BackTools
{
    public static final String MOD_ID = "backtools";
    public static final String MOD_NAME = "Back Tools";

    public static final Logger LOGGER = LogManager.getLogger();

    public static HashMap<Class<? extends Item>, Integer> imcOrientation = new HashMap<>();
    public static HashSet<ResourceLocation> imcDisabledTools = new HashSet<>();

    public BackTools()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext modLoadingContext = ModLoadingContext.get();

        modLoadingContext.registerConfig(ModConfig.Type.CLIENT, BackToolsConfig.SPEC);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener(this::processIMC);
            modEventBus.addListener(this::finishLoading);
        });
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> LOGGER.log(Level.ERROR, "You are loading " + MOD_NAME + " on a server. " + MOD_NAME + " is a client only mod!"));

        modLoadingContext.registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
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
                ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(is.getItem());
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
                ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
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
