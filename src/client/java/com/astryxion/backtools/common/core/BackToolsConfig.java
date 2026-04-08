package com.astryxion.backtools.common.core;

import com.astryxion.backtools.common.BackTools;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client config for Back Tools. Class names are string literals so missing types on newer Minecraft versions only
 * affect {@link Class#forName} at config load, not static init.
 */
public final class BackToolsConfig
{
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private static volatile ConfigData configData = ConfigData.withDefaults();
	private static final AtomicReference<WatchService> watchServiceRef = new AtomicReference<>();
	private static volatile Thread watchThread;

	public static final StringListConfigValue ENABLED_TOOLS_ID = () -> configData.enabledToolsID;

	public static final StringListConfigValue ENABLED_TOOLS_CLASS = () -> configData.enabledToolsClass;

	public static final StringListConfigValue DISABLED_TOOLS_ID = () -> configData.disabledToolsID;

	public static final StringListConfigValue DISABLED_TOOLS_CLASS = () -> configData.disabledToolsClass;

	public static final StringListConfigValue TOOL_ORIENTATION = () -> configData.toolOrientation;

	public static final StringListConfigValue NBT_CLEANER = () -> configData.nbtCleaner;

	public static final BooleanConfigValue EASTER_EGG = () -> configData.easterEgg;

	public static final BooleanConfigValue SHOW_BACK_TOOLS_WITH_CAPE = () -> configData.showBackToolsWithCape;

	private BackToolsConfig()
	{
	}

	public static String fileName()
	{
		return BackTools.MOD_ID + "-client.json";
	}

	/** Used when the config file clears all enabled tool entries so the mod still recognizes vanilla tools. */
	public static List<String> builtinEnabledToolClassNames()
	{
		return List.copyOf(defaultEnabledToolsClass());
	}

	public static void bootstrap()
	{
		loadFromDisk();
	}

	public static void startConfigFileWatcher()
	{
		if (watchThread != null)
		{
			return;
		}
		try
		{
			WatchService ws = FabricLoader.getInstance().getConfigDir().getFileSystem().newWatchService();
			FabricLoader.getInstance().getConfigDir().register(ws, StandardWatchEventKinds.ENTRY_MODIFY);
			watchServiceRef.set(ws);
			Thread t = new Thread(BackToolsConfig::watchLoop, "backtools-config-watch");
			t.setDaemon(true);
			t.start();
			watchThread = t;
		}
		catch (IOException e)
		{
			LOGGER.warn("Back Tools: could not watch config directory for reloads", e);
		}
	}

	private static void watchLoop()
	{
		WatchService ws = watchServiceRef.get();
		if (ws == null)
		{
			return;
		}
		Path configFile = getConfigPath().getFileName();
		while (!Thread.currentThread().isInterrupted())
		{
			WatchKey key;
			try
			{
				key = ws.take();
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				break;
			}
			boolean ours = false;
			for (var evt : key.pollEvents())
			{
				Object ctx = evt.context();
				if (ctx instanceof Path p && p.getFileName().equals(configFile))
				{
					ours = true;
					break;
				}
			}
			key.reset();
			if (ours)
			{
				try
				{
					Thread.sleep(300L);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					break;
				}
				loadFromDisk();
				com.astryxion.backtools.client.core.ClientModBusEvents.onConfigReloaded();
			}
		}
	}

	private static Path getConfigPath()
	{
		return FabricLoader.getInstance().getConfigDir().resolve(fileName());
	}

	public static synchronized void loadFromDisk()
	{
		Path path = getConfigPath();
		ConfigData loaded;
		try
		{
			if (Files.isRegularFile(path))
			{
				try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
				{
					JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
					loaded = GSON.fromJson(root, ConfigData.class);
					if (loaded == null)
					{
						loaded = ConfigData.withDefaults();
					}
					else
					{
						ConfigData defaults = ConfigData.withDefaults();
						loaded.fillDefaultsFrom(defaults);
						if (!root.has("easterEgg"))
						{
							loaded.easterEgg = defaults.easterEgg;
						}
						if (!root.has("showBackToolsWithCape"))
						{
							loaded.showBackToolsWithCape = defaults.showBackToolsWithCape;
						}
					}
				}
			}
			else
			{
				loaded = ConfigData.withDefaults();
				saveToDisk(loaded);
			}
		}
		catch (IOException | JsonParseException e)
		{
			LOGGER.warn("Back Tools: failed to read {}, using defaults", path, e);
			loaded = ConfigData.withDefaults();
		}
		configData = loaded;
	}

	private static void saveToDisk(ConfigData data) throws IOException
	{
		Path path = getConfigPath();
		Files.createDirectories(path.getParent());
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
		{
			GSON.toJson(data, writer);
		}
	}

	private static ArrayList<String> defaultEnabledToolsClass()
	{
		ArrayList<String> list = new ArrayList<>();
		list.add("net.minecraft.world.item.PickaxeItem");
		list.add("net.minecraft.world.item.SwordItem");
		list.add("net.minecraft.world.item.MaceItem");
		list.add("net.minecraft.world.item.AxeItem");
		list.add("net.minecraft.world.item.ShovelItem");
		list.add("net.minecraft.world.item.HoeItem");
		list.add("net.minecraft.world.item.ProjectileWeaponItem");
		list.add("net.minecraft.world.item.ShearsItem");
		list.add("net.minecraft.world.item.FishingRodItem");
		list.add("net.minecraft.world.item.TridentItem");
		return list;
	}

	private static ArrayList<String> defaultDisabledToolsId()
	{
		ArrayList<String> list = new ArrayList<>();
		list.add("minecraft:shield");
		return list;
	}

	private static ArrayList<String> defaultToolOrientation()
	{
		ArrayList<String> list = new ArrayList<>();
		list.add("net.minecraft.world.item.PickaxeItem:180");
		list.add("net.minecraft.world.item.ShovelItem:180");
		list.add("net.minecraft.world.item.AxeItem:180");
		list.add("net.minecraft.world.item.HoeItem:180");
		list.add("net.minecraft.world.item.FishingRodItem:180");
		list.add("net.minecraft.world.item.TridentItem:180");
		list.add("net.minecraft.world.item.ProjectileWeaponItem:90");
		return list;
	}

	private static ArrayList<String> defaultNbtCleaner()
	{
		ArrayList<String> list = new ArrayList<>();
		list.add("Damage");
		list.add("Charged");
		list.add("ChargedProjectiles");
		return list;
	}

	@FunctionalInterface
	public interface StringListConfigValue
	{
		List<String> get();
	}

	@FunctionalInterface
	public interface BooleanConfigValue
	{
		boolean get();
	}

	private static final class ConfigData
	{
		List<String> enabledToolsID;
		List<String> enabledToolsClass;
		List<String> disabledToolsID;
		List<String> disabledToolsClass;
		List<String> toolOrientation;
		List<String> nbtCleaner;
		boolean easterEgg;
		boolean showBackToolsWithCape;

		static ConfigData withDefaults()
		{
			ConfigData d = new ConfigData();
			d.enabledToolsID = new ArrayList<>();
			d.enabledToolsClass = new ArrayList<>(defaultEnabledToolsClass());
			d.disabledToolsID = new ArrayList<>(defaultDisabledToolsId());
			d.disabledToolsClass = new ArrayList<>();
			d.toolOrientation = new ArrayList<>(defaultToolOrientation());
			d.nbtCleaner = new ArrayList<>(defaultNbtCleaner());
			d.easterEgg = true;
			d.showBackToolsWithCape = false;
			return d;
		}

		void fillDefaultsFrom(ConfigData defaults)
		{
			if (enabledToolsID == null)
			{
				enabledToolsID = new ArrayList<>(defaults.enabledToolsID);
			}
			if (enabledToolsClass == null)
			{
				enabledToolsClass = new ArrayList<>(defaults.enabledToolsClass);
			}
			if (disabledToolsID == null)
			{
				disabledToolsID = new ArrayList<>(defaults.disabledToolsID);
			}
			if (disabledToolsClass == null)
			{
				disabledToolsClass = new ArrayList<>(defaults.disabledToolsClass);
			}
			if (toolOrientation == null)
			{
				toolOrientation = new ArrayList<>(defaults.toolOrientation);
			}
			if (nbtCleaner == null)
			{
				nbtCleaner = new ArrayList<>(defaults.nbtCleaner);
			}
		}
	}
}
