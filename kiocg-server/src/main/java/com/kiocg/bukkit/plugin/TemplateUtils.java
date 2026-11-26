package com.kiocg.bukkit.plugin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TemplateUtils {
    private static final Map<Material, ItemStack> UPGRADE_TEMPLATES = new EnumMap<>(Material.class);

    // 数组第 0 项是基础材质，不生成模板；后续每项以前一项为基础材质。
    private static final String[] TOOL_TIERS = {"wooden", "stone", "copper", "iron", "golden", "diamond", "netherite"};
    private static final String[] ARMOR_TIERS = {"leather", "copper", "iron", "golden", "diamond", "netherite"};
    private static final String[] HORSE_ARMOR_TIERS = {"leather", "copper", "iron", "golden", "diamond", "netherite"};
    private static final String[] NAUTILUS_ARMOR_TIERS = {"copper", "iron", "golden", "diamond", "netherite"};

    private static final String[] TOOL_TYPES = {"sword", "spear", "pickaxe", "axe", "shovel", "hoe"};
    private static final String[] ARMOR_TYPES = {"helmet", "chestplate", "leggings", "boots"};

    static {
        for (int i = 1; i < TOOL_TIERS.length; i++) {
            final String baseTier = TOOL_TIERS[i - 1];
            final String tier = TOOL_TIERS[i];
            for (final String type : TOOL_TYPES) {
                register(baseTier, tier, type);
            }
        }
        for (int i = 1; i < ARMOR_TIERS.length; i++) {
            final String baseTier = ARMOR_TIERS[i - 1];
            final String tier = ARMOR_TIERS[i];
            for (final String type : ARMOR_TYPES) {
                register(baseTier, tier, type);
            }
        }
        for (int i = 1; i < HORSE_ARMOR_TIERS.length; i++) {
            final String baseTier = HORSE_ARMOR_TIERS[i - 1];
            final String tier = HORSE_ARMOR_TIERS[i];
            register(baseTier, tier, "horse_armor");
        }
        for (int i = 1; i < NAUTILUS_ARMOR_TIERS.length; i++) {
            final String baseTier = NAUTILUS_ARMOR_TIERS[i - 1];
            final String tier = NAUTILUS_ARMOR_TIERS[i];
            register(baseTier, tier, "nautilus_armor");
        }
    }

    private TemplateUtils() {
    }

    public static ItemStack templateFor(final Material result) {
        final ItemStack template = UPGRADE_TEMPLATES.get(result);
        if (template == null) {
            throw new IllegalArgumentException("No upgrade template for " + result);
        }
        return template;
    }

    private static void register(final String baseTier, final String tier, final String type) {
        final ItemStack template = new ItemStack(Material.NETHERITE_SCRAP);
        final String displayName = tierName(tier) + typeName(type);
        final String baseName = tierName(baseTier) + typeName(type);
        final String materialName = tierMaterialName(tier);
        final int materialCount = typeMaterialCount(type);
        final String resultID = tier + "_" + type;
        setCommonTemplateMeta(template, displayName, baseName, materialName, materialCount, resultID);
        final Material result = Material.valueOf((resultID).toUpperCase(Locale.ROOT));
        UPGRADE_TEMPLATES.put(result, template);
    }

    private static String tierName(final String tier) {
        return switch (tier) {
            case "leather" -> "皮革";
            case "wooden" -> "木";
            case "stone" -> "石";
            case "copper" -> "铜";
            case "iron" -> "铁";
            case "golden" -> "金";
            case "diamond" -> "钻石";
            case "netherite" -> "下界合金";
            default -> throw new IllegalArgumentException("Unknown tier: " + tier);
        };
    }

    private static String tierMaterialName(final String tier) {
        return switch (tier) {
            case "stone" -> "圆石、黑石或深板岩圆石";
            case "copper" -> "铜锭";
            case "iron" -> "铁锭";
            case "golden" -> "金锭";
            case "diamond" -> "钻石";
            case "netherite" -> "下界合金锭";
            default -> throw new IllegalArgumentException("Unknown tier: " + tier);
        };
    }

    private static String typeName(final String type) {
        return switch (type) {
            case "sword" -> "剑";
            case "spear" -> "矛";
            case "pickaxe" -> "镐";
            case "axe" -> "斧";
            case "shovel" -> "锹";
            case "hoe" -> "锄";
            case "helmet" -> "头盔";
            case "chestplate" -> "胸甲";
            case "leggings" -> "护腿";
            case "boots" -> "靴子";
            case "horse_armor" -> "马铠";
            case "nautilus_armor" -> "鹦鹉螺铠";
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    public static int typeMaterialCount(final Material type) {
        String string = type.name().toLowerCase(Locale.ROOT);
        if (string.endsWith("_sword")) return typeMaterialCount("sword");
        if (string.endsWith("_spear")) return typeMaterialCount("spear");
        if (string.endsWith("_pickaxe")) return typeMaterialCount("pickaxe");
        if (string.endsWith("_axe")) return typeMaterialCount("axe");
        if (string.endsWith("_shovel")) return typeMaterialCount("shovel");
        if (string.endsWith("_hoe")) return typeMaterialCount("hoe");
        if (string.endsWith("_helmet")) return typeMaterialCount("helmet");
        if (string.endsWith("_chestplate")) return typeMaterialCount("chestplate");
        if (string.endsWith("_leggings")) return typeMaterialCount("leggings");
        if (string.endsWith("_boots")) return typeMaterialCount("boots");
        if (string.endsWith("_horse_armor")) return typeMaterialCount("horse_armor");
        if (string.endsWith("_nautilus_armor")) return typeMaterialCount("nautilus_armor");
        else throw new IllegalArgumentException("Unknown type: " + string);
    }

    public static int typeMaterialCount(final String type) {
        return switch (type) {
            case "sword" -> 2;
            case "spear" -> 1;
            case "pickaxe" -> 3;
            case "axe" -> 3;
            case "shovel" -> 1;
            case "hoe" -> 2;
            case "helmet" -> 5;
            case "chestplate" -> 8;
            case "leggings" -> 7;
            case "boots" -> 4;
            case "horse_armor" -> 7;
            case "nautilus_armor" -> 5;
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    private static void setCommonTemplateMeta(final ItemStack itemStack, final String displayName, final String baseName, final String materialName, final int materialCount, final String modelData) {
        itemStack.setData(DataComponentTypes.ITEM_NAME, Component.text(displayName + "升级"));
        itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Component.text("锻造模板", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("可应用于：", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text(" " + baseName, NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false),
                Component.text("所需原材料：", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text(" " + materialName + "x" + materialCount, NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false))));
        itemStack.setData(DataComponentTypes.RARITY, ItemRarity.UNCOMMON);
        itemStack.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft("netherite_upgrade_smithing_template"));
        itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addStrings(List.of("UpgradeSmithingTemplate", modelData)));
    }
}
