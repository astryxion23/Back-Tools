package com.astryxion.backtools.common.core;

import com.astryxion.backtools.common.BackTools;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Same options and defaults as Forge {@code ForgeConfigSpec} client config; stored as JSON under the Fabric config directory.
 */
public final class BackToolsConfig
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static volatile Storage storage = Storage.withDefaults();
    private static Path configPath;
    private static long lastLoadedModifiedTime;

    private BackToolsConfig()
    {
    }

    public static String fileName()
    {
        return BackTools.MOD_ID + "-client.json";
    }

    public static void loadFromDisk()
    {
        configPath = FabricLoader.getInstance().getConfigDir().resolve(fileName());
        try
        {
            Files.createDirectories(configPath.getParent());
        }
        catch (IOException e)
        {
            BackTools.LOGGER.error("Could not create config directory", e);
        }
        if (!Files.isRegularFile(configPath))
        {
            storage = Storage.withDefaults();
            saveToDisk();
            touchLastModified();
            return;
        }
        try (Reader reader = Files.newBufferedReader(configPath))
        {
            Storage parsed = GSON.fromJson(reader, Storage.class);
            storage = Storage.mergeWithDefaults(parsed);
            touchLastModified();
        }
        catch (Exception e)
        {
            BackTools.LOGGER.error("Failed to load {}, using defaults", fileName(), e);
            storage = Storage.withDefaults();
            touchLastModified();
        }
    }

    public static boolean reloadIfChanged()
    {
        if (configPath == null || !Files.isRegularFile(configPath))
        {
            return false;
        }
        try
        {
            long mtime = Files.getLastModifiedTime(configPath).toMillis();
            if (mtime != lastLoadedModifiedTime)
            {
                loadFromDisk();
                return true;
            }
        }
        catch (IOException ignored)
        {
        }
        return false;
    }

    private static void saveToDisk()
    {
        if (configPath == null)
        {
            return;
        }
        try (Writer writer = Files.newBufferedWriter(configPath))
        {
            GSON.toJson(storage, writer);
        }
        catch (IOException e)
        {
            BackTools.LOGGER.error("Failed to save {}", fileName(), e);
        }
    }

    private static void touchLastModified()
    {
        if (configPath == null || !Files.isRegularFile(configPath))
        {
            lastLoadedModifiedTime = 0L;
            return;
        }
        try
        {
            lastLoadedModifiedTime = Files.getLastModifiedTime(configPath).toMillis();
        }
        catch (IOException e)
        {
            lastLoadedModifiedTime = 0L;
        }
    }

    public static List<String> enabledToolsId()
    {
        return storage.enabledToolsID;
    }

    public static List<String> enabledToolsClass()
    {
        return storage.enabledToolsClass;
    }

    public static List<String> disabledToolsId()
    {
        return storage.disabledToolsID;
    }

    public static List<String> disabledToolsClass()
    {
        return storage.disabledToolsClass;
    }

    public static List<String> toolOrientation()
    {
        return storage.toolOrientation;
    }

    public static List<String> nbtCleaner()
    {
        return storage.nbtCleaner;
    }

    public static boolean easterEgg()
    {
        return storage.easterEgg != null ? storage.easterEgg : true;
    }

    public static boolean showBackToolsWithCape()
    {
        return storage.showBackToolsWithCape != null && storage.showBackToolsWithCape;
    }

    private static final class Storage
    {
        @SerializedName("enabledToolsID")
        List<String> enabledToolsID = new ArrayList<>();

        @SerializedName("enabledToolsClass")
        List<String> enabledToolsClass = new ArrayList<>();

        @SerializedName("disabledToolsID")
        List<String> disabledToolsID = new ArrayList<>();

        @SerializedName("disabledToolsClass")
        List<String> disabledToolsClass = new ArrayList<>();

        @SerializedName("toolOrientation")
        List<String> toolOrientation = new ArrayList<>();

        @SerializedName("nbtCleaner")
        List<String> nbtCleaner = new ArrayList<>();

        @SerializedName("easterEgg")
        Boolean easterEgg;

        @SerializedName("showBackToolsWithCape")
        Boolean showBackToolsWithCape;

        static Storage withDefaults()
        {
            Storage s = new Storage();
            s.enabledToolsID = new ArrayList<>();
            s.enabledToolsClass = defaultEnabledToolsClass();
            s.disabledToolsID = defaultDisabledToolsId();
            s.disabledToolsClass = new ArrayList<>();
            s.toolOrientation = defaultToolOrientation();
            s.nbtCleaner = defaultNbtCleaner();
            s.easterEgg = Boolean.TRUE;
            s.showBackToolsWithCape = Boolean.FALSE;
            return s;
        }

        static Storage mergeWithDefaults(Storage parsed)
        {
            Storage d = withDefaults();
            if (parsed == null)
            {
                return d;
            }
            if (parsed.enabledToolsID != null)
            {
                d.enabledToolsID = new ArrayList<>(parsed.enabledToolsID);
            }
            if (parsed.enabledToolsClass != null)
            {
                d.enabledToolsClass = new ArrayList<>(parsed.enabledToolsClass);
            }
            if (parsed.disabledToolsID != null)
            {
                d.disabledToolsID = new ArrayList<>(parsed.disabledToolsID);
            }
            if (parsed.disabledToolsClass != null)
            {
                d.disabledToolsClass = new ArrayList<>(parsed.disabledToolsClass);
            }
            if (parsed.toolOrientation != null)
            {
                d.toolOrientation = new ArrayList<>(parsed.toolOrientation);
            }
            if (parsed.nbtCleaner != null)
            {
                d.nbtCleaner = new ArrayList<>(parsed.nbtCleaner);
            }
            if (parsed.easterEgg != null)
            {
                d.easterEgg = parsed.easterEgg;
            }
            if (parsed.showBackToolsWithCape != null)
            {
                d.showBackToolsWithCape = parsed.showBackToolsWithCape;
            }
            return d;
        }
    }

    private static ArrayList<String> defaultEnabledToolsClass()
    {
        ArrayList<String> list = new ArrayList<>();
        list.add(TieredItem.class.getName());
        list.add(ProjectileWeaponItem.class.getName());
        list.add(ShearsItem.class.getName());
        list.add(FishingRodItem.class.getName());
        list.add(TridentItem.class.getName());
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
        list.add(DiggerItem.class.getName() + ":180");
        list.add(HoeItem.class.getName() + ":180");
        list.add(FishingRodItem.class.getName() + ":180");
        list.add(TridentItem.class.getName() + ":180");
        list.add(ProjectileWeaponItem.class.getName() + ":90");
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
}
