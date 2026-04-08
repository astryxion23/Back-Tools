package com.astryxion.backtools.client.core;

import com.astryxion.backtools.common.BackTools;
import com.astryxion.backtools.common.core.BackToolsConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EventHandler
{
	private static final String LEGACY_PICKAXE_CLASS = "net.minecraft.world.item.PickaxeItem";
	private static final String LEGACY_SWORD_CLASS = "net.minecraft.world.item.SwordItem";
	private static final String LEGACY_DIGGER_CLASS = "net.minecraft.world.item.DiggerItem";
	private static final String LEGACY_AXE_CLASS = "net.minecraft.world.item.AxeItem";
	private static final String LEGACY_SHOVEL_CLASS = "net.minecraft.world.item.ShovelItem";
	private static final String LEGACY_HOE_CLASS = "net.minecraft.world.item.HoeItem";
	private static final String LEGACY_MACE_CLASS = "net.minecraft.world.item.MaceItem";
	private static final String LEGACY_PROJECTILE_WEAPON_CLASS = "net.minecraft.world.item.ProjectileWeaponItem";
	private static final String LEGACY_SHEARS_CLASS = "net.minecraft.world.item.ShearsItem";
	private static final String LEGACY_FISHING_ROD_CLASS = "net.minecraft.world.item.FishingRodItem";
	private static final String LEGACY_TRIDENT_CLASS = "net.minecraft.world.item.TridentItem";

	private static final Identifier SHEARS_ITEM_ID = Identifier.withDefaultNamespace("shears");

	public static WeakHashMap<AbstractClientPlayer, HeldInfo> heldTools = new WeakHashMap<>();

	public static HashSet<Pattern> enabledToolsID = new HashSet<>();
	public static HashSet<Class<? extends Item>> enabledToolsClass = new HashSet<>();
	/** 1.21.5+: many tool types are plain {@link Item}s with registry tags; legacy config still names removed *Item subclasses. */
	public static HashSet<TagKey<Item>> enabledToolTags = new HashSet<>();
	/** When no item tag exists (e.g. shears), match by registry id. */
	public static HashSet<Identifier> enabledToolItemIds = new HashSet<>();
	public static HashSet<Pattern> disabledToolsID = new HashSet<>();
	public static HashSet<Class<? extends Item>> disabledToolsClass = new HashSet<>();
	public static HashMap<Class<? extends Item>, Integer> toolOrientations = new HashMap<>();
	/** Orientations for items that no longer use distinct tool subclasses (same legacy names as {@link #enabledToolTags}). */
	public static HashMap<TagKey<Item>, Integer> toolTagOrientations = new HashMap<>();
	/** Per-item orientations when only a registry id applies (e.g. legacy {@code ShearsItem} in config). */
	public static HashMap<Identifier, Integer> toolItemOrientations = new HashMap<>();

	private EventHandler()
	{
	}

	public static void registerFabricEvents()
	{
		ClientTickEvents.END_CLIENT_TICK.register(EventHandler::onEndClientTick);
		ClientEntityEvents.ENTITY_LOAD.register(EventHandler::onEntityLoadClient);
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> client.execute(EventHandler::clean));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(EventHandler::clean));
	}

	private static void onEndClientTick(Minecraft client)
	{
		if (client.level == null)
		{
			return;
		}
		for (Player player : client.level.players())
		{
			if (!player.level().isClientSide())
			{
				continue;
			}
			if (!(player instanceof AbstractClientPlayer acp))
			{
				continue;
			}
			if (!acp.isAlive())
			{
				heldTools.remove(acp);
			}
			else
			{
				HeldInfo info = heldTools.computeIfAbsent(acp, v -> new HeldInfo());
				info.tick(acp.getMainHandItem().copy(), acp.getOffhandItem().copy(), acp.level().registryAccess());
			}
		}
	}

	private static void onEntityLoadClient(net.minecraft.world.entity.Entity entity, ClientLevel world)
	{
		if (!(entity instanceof ItemEntity item))
		{
			return;
		}
		List<Player> ents = world.getEntitiesOfClass(Player.class, item.getBoundingBox().inflate(1.0D, 1.0D, 1.0D), k -> true);
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

	public static void setupConfig()
	{
		enabledToolsID.clear();
		enabledToolsClass.clear();
		enabledToolTags.clear();
		enabledToolItemIds.clear();
		for (String s : BackToolsConfig.ENABLED_TOOLS_ID.get())
		{
			enabledToolsID.add(Pattern.compile(s));
		}
		for (String s : BackToolsConfig.ENABLED_TOOLS_CLASS.get())
		{
			addEnabledToolClassFromConfigString(s);
		}

		if (enabledToolsID.isEmpty() && enabledToolsClass.isEmpty() && enabledToolTags.isEmpty() && enabledToolItemIds.isEmpty())
		{
			BackTools.LOGGER.warn("enabledToolsID and enabledToolsClass are both empty in config; restoring built-in tool classes.");
			for (String s : BackToolsConfig.builtinEnabledToolClassNames())
			{
				addEnabledToolClassFromConfigString(s);
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
				Class<?> clz = Class.forName(s, false, EventHandler.class.getClassLoader());
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
		toolTagOrientations.clear();
		toolItemOrientations.clear();
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
			int angle;
			try
			{
				angle = Integer.parseInt(split[1]);
			}
			catch (NumberFormatException e)
			{
				BackTools.LOGGER.warn("Could not parse integer: {}", s);
				continue;
			}
			try
			{
				Class<?> clz = Class.forName(split[0], false, EventHandler.class.getClassLoader());
				if (Item.class.isAssignableFrom(clz))
				{
					@SuppressWarnings("unchecked")
					Class<? extends Item> itemClass = (Class<? extends Item>) clz;
					toolOrientations.put(itemClass, angle);
				}
				else
				{
					BackTools.LOGGER.warn("Class does not extend Item class: {}", split[0]);
				}
			}
			catch (ClassNotFoundException e)
			{
				if (LEGACY_PICKAXE_CLASS.equals(split[0]))
				{
					toolTagOrientations.put(ItemTags.PICKAXES, angle);
				}
				else if (LEGACY_DIGGER_CLASS.equals(split[0]))
				{
					toolTagOrientations.put(ItemTags.PICKAXES, angle);
					toolTagOrientations.put(ItemTags.SHOVELS, angle);
				}
				else if (LEGACY_SWORD_CLASS.equals(split[0]))
				{
					toolTagOrientations.put(ItemTags.SWORDS, angle);
				}
				else if (LEGACY_AXE_CLASS.equals(split[0]))
				{
					toolTagOrientations.put(ItemTags.AXES, angle);
				}
				else if (LEGACY_SHOVEL_CLASS.equals(split[0]))
				{
					toolTagOrientations.put(ItemTags.SHOVELS, angle);
				}
				else if (LEGACY_HOE_CLASS.equals(split[0]))
				{
					toolTagOrientations.put(ItemTags.HOES, angle);
				}
				else if (LEGACY_FISHING_ROD_CLASS.equals(split[0]))
				{
					toolTagOrientations.put(ItemTags.FISHING_ENCHANTABLE, angle);
				}
				else if (LEGACY_TRIDENT_CLASS.equals(split[0]))
				{
					toolTagOrientations.put(ItemTags.TRIDENT_ENCHANTABLE, angle);
				}
				else if (LEGACY_PROJECTILE_WEAPON_CLASS.equals(split[0]))
				{
					toolTagOrientations.put(ItemTags.BOW_ENCHANTABLE, angle);
					toolTagOrientations.put(ItemTags.CROSSBOW_ENCHANTABLE, angle);
				}
				else if (LEGACY_MACE_CLASS.equals(split[0]))
				{
					toolTagOrientations.put(ItemTags.MACE_ENCHANTABLE, angle);
				}
				else if (LEGACY_SHEARS_CLASS.equals(split[0]))
				{
					toolItemOrientations.put(SHEARS_ITEM_ID, angle);
				}
				else
				{
					BackTools.LOGGER.warn("Could not find class to add orientation: {}", split[0]);
				}
			}
		}
		toolOrientations.putAll(BackTools.imcOrientation);
	}

	private static void addEnabledToolClassFromConfigString(String s)
	{
		try
		{
			Class<?> clz = Class.forName(s, false, EventHandler.class.getClassLoader());
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
			if (LEGACY_PICKAXE_CLASS.equals(s))
			{
				enabledToolTags.add(ItemTags.PICKAXES);
			}
			else if (LEGACY_SWORD_CLASS.equals(s))
			{
				enabledToolTags.add(ItemTags.SWORDS);
			}
			else if (LEGACY_AXE_CLASS.equals(s))
			{
				enabledToolTags.add(ItemTags.AXES);
			}
			else if (LEGACY_SHOVEL_CLASS.equals(s))
			{
				enabledToolTags.add(ItemTags.SHOVELS);
			}
			else if (LEGACY_HOE_CLASS.equals(s))
			{
				enabledToolTags.add(ItemTags.HOES);
			}
			else if (LEGACY_MACE_CLASS.equals(s))
			{
				enabledToolTags.add(ItemTags.MACE_ENCHANTABLE);
			}
			else if (LEGACY_PROJECTILE_WEAPON_CLASS.equals(s))
			{
				enabledToolTags.add(ItemTags.BOW_ENCHANTABLE);
				enabledToolTags.add(ItemTags.CROSSBOW_ENCHANTABLE);
			}
			else if (LEGACY_FISHING_ROD_CLASS.equals(s))
			{
				enabledToolTags.add(ItemTags.FISHING_ENCHANTABLE);
			}
			else if (LEGACY_TRIDENT_CLASS.equals(s))
			{
				enabledToolTags.add(ItemTags.TRIDENT_ENCHANTABLE);
			}
			else if (LEGACY_SHEARS_CLASS.equals(s))
			{
				enabledToolItemIds.add(SHEARS_ITEM_ID);
			}
			else
			{
				BackTools.LOGGER.warn("Cannot find class {}", s);
			}
		}
	}

	public static Integer getToolOrientation(Item item)
	{
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		if (id != null && toolItemOrientations.containsKey(id))
		{
			return toolItemOrientations.get(id);
		}
		var holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
		for (var e : toolTagOrientations.entrySet())
		{
			if (holder.is(e.getKey()))
			{
				return e.getValue();
			}
		}
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

	public static void clean()
	{
		heldTools.clear();
	}

	public static boolean isItemTool(Item item)
	{
		Identifier key = BuiltInRegistries.ITEM.getKey(item);
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

		if (enabledToolItemIds.contains(key))
		{
			return true;
		}

		var holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
		for (TagKey<Item> tag : enabledToolTags)
		{
			if (holder.is(tag))
			{
				return true;
			}
		}
		return false;
	}

	public static boolean areItemStacksEqualToolsIgnoreDamage(@NotNull ItemStack stackA, @NotNull ItemStack stackB, HolderLookup.Provider registries)
	{
		if (stackA.isEmpty() || stackB.isEmpty() || stackA.getItem() != stackB.getItem())
		{
			return false;
		}
		boolean patchA = !stackA.getComponentsPatch().isEmpty();
		boolean patchB = !stackB.getComponentsPatch().isEmpty();
		if (patchA != patchB)
		{
			return false;
		}
		if (patchA && patchB)
		{
			CompoundTag tagA = ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stackA)
					.resultOrPartial(msg -> BackTools.LOGGER.warn("{}", msg))
					.map(t -> t instanceof CompoundTag c ? c.copy() : new CompoundTag())
					.orElseGet(CompoundTag::new);
			CompoundTag tagB = ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stackB)
					.resultOrPartial(msg -> BackTools.LOGGER.warn("{}", msg))
					.map(t -> t instanceof CompoundTag c ? c.copy() : new CompoundTag())
					.orElseGet(CompoundTag::new);
			for (String s : BackToolsConfig.NBT_CLEANER.get())
			{
				tagA.remove(s);
				tagB.remove(s);
			}

			return tagA.equals(tagB) && ItemStack.isSameItemSameComponents(stackA, stackB);
		}
		else
		{
			return ItemStack.isSameItemSameComponents(stackA, stackB);
		}
	}

	public static class HeldInfo
	{
		public ItemEntity itemEntity = null;

		public ItemStack lastMain = ItemStack.EMPTY;
		public ItemStack lastOff = ItemStack.EMPTY;

		public ItemStack toolMain = ItemStack.EMPTY;
		public ItemStack toolOff = ItemStack.EMPTY;

		public void tick(ItemStack main, ItemStack off, HolderLookup.Provider registries)
		{
			if (itemEntity != null && !itemEntity.getItem().isEmpty())
			{
				checkItem(itemEntity, registries);

				itemEntity = null;
				return;
			}

			if (areItemStacksEqualToolsIgnoreDamage(main, lastMain, registries) || areItemStacksEqualToolsIgnoreDamage(off, lastMain, registries))
			{
				lastMain = ItemStack.EMPTY;
			}

			if (areItemStacksEqualToolsIgnoreDamage(main, lastOff, registries) || areItemStacksEqualToolsIgnoreDamage(off, lastOff, registries))
			{
				lastOff = ItemStack.EMPTY;
			}

			if (!toolMain.isEmpty() && !areItemStacksEqualToolsIgnoreDamage(main, toolMain, registries) && !areItemStacksEqualToolsIgnoreDamage(off, toolMain, registries))
			{
				lastMain = toolMain;
				toolMain = ItemStack.EMPTY;
			}

			if (!toolOff.isEmpty() && !areItemStacksEqualToolsIgnoreDamage(main, toolOff, registries) && !areItemStacksEqualToolsIgnoreDamage(off, toolOff, registries))
			{
				lastOff = toolOff;
				toolOff = ItemStack.EMPTY;
			}

			if (isItemTool(main.getItem()))
			{
				toolMain = main;
				if (areItemStacksEqualToolsIgnoreDamage(toolMain, toolOff, registries))
				{
					toolOff = ItemStack.EMPTY;
				}
			}

			if (isItemTool(off.getItem()))
			{
				toolOff = off;
				if (areItemStacksEqualToolsIgnoreDamage(toolOff, toolMain, registries))
				{
					toolMain = ItemStack.EMPTY;
				}
			}
		}

		public void checkItem(ItemEntity item, HolderLookup.Provider registries)
		{
			if (areItemStacksEqualToolsIgnoreDamage(item.getItem(), lastMain, registries))
			{
				lastMain = ItemStack.EMPTY;
			}

			if (areItemStacksEqualToolsIgnoreDamage(item.getItem(), toolMain, registries))
			{
				toolMain = ItemStack.EMPTY;
			}

			if (areItemStacksEqualToolsIgnoreDamage(item.getItem(), lastOff, registries))
			{
				lastOff = ItemStack.EMPTY;
			}

			if (areItemStacksEqualToolsIgnoreDamage(item.getItem(), toolOff, registries))
			{
				toolOff = ItemStack.EMPTY;
			}
		}
	}
}
