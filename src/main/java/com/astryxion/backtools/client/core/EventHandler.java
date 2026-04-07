package com.astryxion.backtools.client.core;

import com.astryxion.backtools.common.BackTools;
import com.astryxion.backtools.common.core.BackToolsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = BackTools.MOD_ID, value = Dist.CLIENT)
public class EventHandler
{
    public static WeakHashMap<AbstractClientPlayer, HeldInfo> heldTools = new WeakHashMap<>();

    public static HashSet<Pattern> enabledToolsID = new HashSet<>();
    public static HashSet<Class<? extends Item>> enabledToolsClass = new HashSet<>();
    public static HashSet<Pattern> disabledToolsID = new HashSet<>();
    public static HashSet<Class<? extends Item>> disabledToolsClass = new HashSet<>();
    public static HashMap<Class<? extends Item>, Integer> toolOrientations = new HashMap<>();

    public static void setupConfig()
    {
        enabledToolsID.clear();
        enabledToolsClass.clear();
        for (String s : BackToolsConfig.ENABLED_TOOLS_ID.get())
        {
            enabledToolsID.add(Pattern.compile(s));
        }
        for (String s : BackToolsConfig.ENABLED_TOOLS_CLASS.get())
        {
            try
            {
                Class<?> clz = Class.forName(s);
                if (Item.class.isAssignableFrom(clz))
                {
                    @SuppressWarnings("unchecked")
                    Class<? extends Item> itemClass = (Class<? extends Item>) clz;
                    enabledToolsClass.add(itemClass);
                }
                else
                {
                    BackTools.LOGGER.warn("Class {} does not extend Item class", clz);
                }
            }
            catch (ClassNotFoundException e)
            {
                BackTools.LOGGER.warn("Cannot find class {}", s);
            }
        }

        disabledToolsID.clear();
        disabledToolsClass.clear();
        for (String s : BackToolsConfig.DISABLED_TOOLS_ID.get())
        {
            disabledToolsID.add(Pattern.compile(s));
        }
        for (String s : BackToolsConfig.DISABLED_TOOLS_CLASS.get())
        {
            try
            {
                Class<?> clz = Class.forName(s);
                if (Item.class.isAssignableFrom(clz))
                {
                    @SuppressWarnings("unchecked")
                    Class<? extends Item> itemClass = (Class<? extends Item>) clz;
                    disabledToolsClass.add(itemClass);
                }
                else
                {
                    BackTools.LOGGER.warn("Class {} does not extend Item class", clz);
                }
            }
            catch (ClassNotFoundException e)
            {
                BackTools.LOGGER.warn("Cannot find class {}", s);
            }
        }

        toolOrientations.clear();
        for (String s : BackToolsConfig.TOOL_ORIENTATION.get())
        {
            String[] split = new String[2];
            int index = s.indexOf(':');
            if (index > 0)
            {
                split[0] = s.substring(0, index);
                split[1] = s.substring(index + 1);
            }
            else
            {
                BackTools.LOGGER.warn("Could not parse orientation: {}", s);
                continue;
            }
            try
            {
                Class<?> clz = Class.forName(split[0]);
                if (Item.class.isAssignableFrom(clz))
                {
                    @SuppressWarnings("unchecked")
                    Class<? extends Item> itemClass = (Class<? extends Item>) clz;
                    toolOrientations.put(itemClass, Integer.parseInt(split[1]));
                }
                else
                {
                    BackTools.LOGGER.warn("Class does not extend Item class: {}", split[0]);
                }
            }
            catch (ClassNotFoundException e)
            {
                BackTools.LOGGER.warn("Could not find class to add orientation: {}", split[0]);
            }
            catch (NumberFormatException e)
            {
                BackTools.LOGGER.warn("Could not parse integer: {}", s);
            }
        }
        toolOrientations.putAll(BackTools.imcOrientation);
    }

    public static Integer getToolOrientation(Item item)
    {
        return getToolOrientation(item.getClass());
    }

    @SuppressWarnings("unchecked")
    public static Integer getToolOrientation(Class<?> clz)
    {
        if (Item.class.equals(clz))
        {
            return 0;
        }
        Class<? extends Item> key = (Class<? extends Item>) clz;
        if (!toolOrientations.containsKey(key))
        {
            toolOrientations.put(key, getToolOrientation(clz.getSuperclass()));
        }
        return toolOrientations.get(key);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START && event.player.level().isClientSide)
        {
            AbstractClientPlayer player = (AbstractClientPlayer) event.player;
            if (!player.isAlive())
            {
                heldTools.remove(player);
            }
            else
            {
                HeldInfo info = heldTools.computeIfAbsent(player, v -> new HeldInfo());
                info.tick(player.getMainHandItem().copy(), player.getOffhandItem().copy());
            }
        }
    }

    @SubscribeEvent
    public static void onItemSpawnEvent(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide && event.getEntity() instanceof ItemEntity item)
        {
            List<Player> ents = event.getLevel().getEntitiesOfClass(Player.class, item.getBoundingBox().inflate(1.0D, 1.0D, 1.0D), k -> true);
            ents.forEach((e) -> {
                if (e instanceof AbstractClientPlayer acp)
                {
                    heldTools.computeIfPresent(acp, (k, v) -> {
                        v.itemEntity = item;
                        return v;
                    });
                }
            });
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event)
    {
        if (event.getLevel() instanceof ClientLevel)
        {
            Minecraft.getInstance().execute(EventHandler::clean);
        }
    }

    @SubscribeEvent
    public static void onLoggedOutEvent(ClientPlayerNetworkEvent.LoggingOut event)
    {
        Minecraft.getInstance().execute(EventHandler::clean);
    }

    public static void clean()
    {
        heldTools.clear();
    }

    public static boolean isItemTool(Item item)
    {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null)
        {
            return false;
        }
        String idStr = key.toString();

        for (Pattern p : disabledToolsID)
        {
            Matcher m = p.matcher(idStr);
            if (m.matches())
            {
                return false;
            }
        }

        for (Class<? extends Item> clz : disabledToolsClass)
        {
            if (clz.isInstance(item))
            {
                return false;
            }
        }

        for (Pattern p : enabledToolsID)
        {
            Matcher m = p.matcher(idStr);
            if (m.matches())
            {
                return true;
            }
        }

        for (Class<? extends Item> clz : enabledToolsClass)
        {
            if (clz.isInstance(item))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean areItemStacksEqualToolsIgnoreDamage(@Nonnull ItemStack stackA, @Nonnull ItemStack stackB)
    {
        if (stackA.isEmpty() || stackB.isEmpty() || stackA.hasTag() && !stackB.hasTag() || !stackA.hasTag() && stackB.hasTag() || stackA.getItem() != stackB.getItem())
        {
            return false;
        }
        else if (stackA.hasTag() && stackB.hasTag())
        {
            CompoundTag tagA = stackA.getTag().copy();
            CompoundTag tagB = stackB.getTag().copy();
            for (String s : BackToolsConfig.NBT_CLEANER.get())
            {
                tagA.remove(s);
                tagB.remove(s);
            }

            return tagA.equals(tagB) && stackA.areCapsCompatible(stackB);
        }
        else
        {
            return stackA.areCapsCompatible(stackB);
        }
    }

    public static class HeldInfo
    {
        public ItemEntity itemEntity = null;

        public ItemStack lastMain = ItemStack.EMPTY;
        public ItemStack lastOff = ItemStack.EMPTY;

        public ItemStack toolMain = ItemStack.EMPTY;
        public ItemStack toolOff = ItemStack.EMPTY;

        public void tick(ItemStack main, ItemStack off)
        {
            if (itemEntity != null && !itemEntity.getItem().isEmpty())
            {
                checkItem(itemEntity);

                itemEntity = null;
                return;
            }

            if (areItemStacksEqualToolsIgnoreDamage(main, lastMain) || areItemStacksEqualToolsIgnoreDamage(off, lastMain))
            {
                lastMain = ItemStack.EMPTY;
            }

            if (areItemStacksEqualToolsIgnoreDamage(main, lastOff) || areItemStacksEqualToolsIgnoreDamage(off, lastOff))
            {
                lastOff = ItemStack.EMPTY;
            }

            if (!toolMain.isEmpty() && !areItemStacksEqualToolsIgnoreDamage(main, toolMain) && !areItemStacksEqualToolsIgnoreDamage(off, toolMain))
            {
                lastMain = toolMain;
                toolMain = ItemStack.EMPTY;
            }

            if (!toolOff.isEmpty() && !areItemStacksEqualToolsIgnoreDamage(main, toolOff) && !areItemStacksEqualToolsIgnoreDamage(off, toolOff))
            {
                lastOff = toolOff;
                toolOff = ItemStack.EMPTY;
            }

            if (isItemTool(main.getItem()))
            {
                toolMain = main;
                if (areItemStacksEqualToolsIgnoreDamage(toolMain, toolOff))
                {
                    toolOff = ItemStack.EMPTY;
                }
            }

            if (isItemTool(off.getItem()))
            {
                toolOff = off;
                if (areItemStacksEqualToolsIgnoreDamage(toolOff, toolMain))
                {
                    toolMain = ItemStack.EMPTY;
                }
            }
        }

        public void checkItem(ItemEntity item)
        {
            if (areItemStacksEqualToolsIgnoreDamage(item.getItem(), lastMain))
            {
                lastMain = ItemStack.EMPTY;
            }

            if (areItemStacksEqualToolsIgnoreDamage(item.getItem(), toolMain))
            {
                toolMain = ItemStack.EMPTY;
            }

            if (areItemStacksEqualToolsIgnoreDamage(item.getItem(), lastOff))
            {
                lastOff = ItemStack.EMPTY;
            }

            if (areItemStacksEqualToolsIgnoreDamage(item.getItem(), toolOff))
            {
                toolOff = ItemStack.EMPTY;
            }
        }
    }
}
