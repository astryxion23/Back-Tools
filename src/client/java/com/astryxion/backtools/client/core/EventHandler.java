package com.astryxion.backtools.client.core;

import com.astryxion.backtools.common.BackTools;
import com.astryxion.backtools.common.core.BackToolsConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        for (String s : BackToolsConfig.enabledToolsId())
        {
            enabledToolsID.add(Pattern.compile(s));
        }
        for (String s : BackToolsConfig.enabledToolsClass())
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
        for (String s : BackToolsConfig.disabledToolsId())
        {
            disabledToolsID.add(Pattern.compile(s));
        }
        for (String s : BackToolsConfig.disabledToolsClass())
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
        for (String s : BackToolsConfig.toolOrientation())
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

    public static void onPlayerTick(Player player)
    {
        if (!(player instanceof AbstractClientPlayer acp))
        {
            return;
        }
        if (!acp.level().isClientSide())
        {
            return;
        }
        if (!acp.isAlive())
        {
            heldTools.remove(acp);
        }
        else
        {
            HeldInfo info = heldTools.computeIfAbsent(acp, v -> new HeldInfo());
            info.tick(acp.getMainHandItem().copy(), acp.getOffhandItem().copy());
        }
    }

    public static void onEntityLoad(Entity entity, Level level)
    {
        if (!level.isClientSide() || !(entity instanceof ItemEntity item))
        {
            return;
        }
        List<Player> ents = level.getEntitiesOfClass(Player.class, item.getBoundingBox().inflate(1.0D, 1.0D, 1.0D), k -> true);
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

    public static void clean()
    {
        heldTools.clear();
    }

    public static boolean isItemTool(Item item)
    {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
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

    public static boolean areItemStacksEqualToolsIgnoreDamage(@NotNull ItemStack stackA, @NotNull ItemStack stackB)
    {
        CustomData customA = stackA.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CustomData customB = stackB.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        boolean hasA = !customA.isEmpty();
        boolean hasB = !customB.isEmpty();

        if (stackA.isEmpty() || stackB.isEmpty() || hasA && !hasB || !hasA && hasB || stackA.getItem() != stackB.getItem())
        {
            return false;
        }
        else if (hasA && hasB)
        {
            CompoundTag tagA = customA.copyTag();
            CompoundTag tagB = customB.copyTag();
            for (String s : BackToolsConfig.nbtCleaner())
            {
                tagA.remove(s);
                tagB.remove(s);
            }

            return tagA.equals(tagB);
        }
        else
        {
            return true;
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
