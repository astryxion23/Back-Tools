package com.astryxion.backtools.common.core;

import com.google.common.collect.Lists;
import com.astryxion.backtools.common.BackTools;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Local replacement for the former iChunUtil-backed config. Default lists match the original Back Tools 1.16.5 mod,
 * adapted only where Minecraft class names changed (e.g. ToolItem → DiggerItem, ShootableItem → ProjectileWeaponItem).
 */
public final class BackToolsConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENABLED_TOOLS_ID = BUILDER
            .comment("Enabled tools by resource location pattern (regex).")
            .defineListAllowEmpty("enabledToolsID", Lists.newArrayList(), o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENABLED_TOOLS_CLASS = BUILDER
            .comment("Enabled tools by fully qualified class name (must extend Item).")
            .defineListAllowEmpty("enabledToolsClass", defaultEnabledToolsClass(), o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_TOOLS_ID = BUILDER
            .comment("Disabled tools by resource location pattern (regex).")
            .defineListAllowEmpty("disabledToolsID", defaultDisabledToolsId(), o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLED_TOOLS_CLASS = BUILDER
            .comment("Disabled tools by fully qualified class name.")
            .defineListAllowEmpty("disabledToolsClass", Lists.newArrayList(), o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TOOL_ORIENTATION = BUILDER
            .comment("Per-class Z rotation in degrees, formatted as \"fully.qualified.ClassName:degrees\".")
            .defineListAllowEmpty("toolOrientation", defaultToolOrientation(), o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> NBT_CLEANER = BUILDER
            .comment("NBT keys stripped when comparing tool stacks (same as original).")
            .defineListAllowEmpty("nbtCleaner", defaultNbtCleaner(), o -> o instanceof String);

    public static final ModConfigSpec.BooleanValue EASTER_EGG = BUILDER
            .comment("When true, enables the swimming / elytra / name-based rotation easter egg (former iChunUtil client flag, default on).")
            .define("easterEgg", true);

    public static final ModConfigSpec.BooleanValue SHOW_BACK_TOOLS_WITH_CAPE = BUILDER
            .comment("Vanilla Back Tools hid back items when the cape skin part was on and a cloak texture existed (e.g. Mojang cape). Set true to show back tools anyway.")
            .define("showBackToolsWithCape", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private BackToolsConfig()
    {
    }

    public static String fileName()
    {
        return BackTools.MOD_ID + "-client.toml";
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
