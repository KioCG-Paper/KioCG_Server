package com.kiocg.bukkit.plugin;

import com.destroystokyo.paper.MaterialSetTag;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;

import java.util.*;

public final class ModifyRecipes {
    private ModifyRecipes() {
        throw new AssertionError("no instances");
    }

    public static void apply(final Plugin plugin) {
        final List<NamespacedKey> recipesToRemove = new ArrayList<>();
        final List<Recipe> recipesToAdd = new ArrayList<>();

        final List<FurnaceRecipe> furnaceRecipes = new ArrayList<>();
        final List<SmokingRecipe> smokingRecipes = new ArrayList<>();
        final List<BlastingRecipe> blastingRecipes = new ArrayList<>();
        final List<StonecuttingRecipe> stonecuttingRecipes = new ArrayList<>();
        final List<CraftingRecipe> craftingRecipes = new ArrayList<>();
        final List<SmithingTransformRecipe> smithingTransformRecipes = new ArrayList<>();

        Bukkit.recipeIterator().forEachRemaining(recipe -> {
            if (recipe instanceof FurnaceRecipe furnaceRecipe) {
                if (isVanillaRecipe(furnaceRecipe)) {
                    furnaceRecipes.add(furnaceRecipe);
                }
            } else if (recipe instanceof SmokingRecipe smokingRecipe) {
                if (isVanillaRecipe(smokingRecipe)) {
                    smokingRecipes.add(smokingRecipe);
                }
            } else if (recipe instanceof BlastingRecipe blastingRecipe) {
                if (isVanillaRecipe(blastingRecipe)) {
                    blastingRecipes.add(blastingRecipe);
                }
            } else if (recipe instanceof StonecuttingRecipe stonecuttingRecipe) {
                if (isVanillaRecipe(stonecuttingRecipe)) {
                    stonecuttingRecipes.add(stonecuttingRecipe);
                }
            } else if (recipe instanceof CraftingRecipe craftingRecipe) {
                if (isVanillaRecipe(craftingRecipe)) {
                    craftingRecipes.add(craftingRecipe);
                }
            } else if (recipe instanceof SmithingTransformRecipe smithingTransformRecipe) {
                if (isVanillaRecipe(smithingTransformRecipe)) {
                    smithingTransformRecipes.add(smithingTransformRecipe);
                }
            }
        });

        collectFurnaceLikeRemovals(plugin, furnaceRecipes, smokingRecipes, blastingRecipes, recipesToRemove);
        collectStonecuttingCraftingRemovals(plugin, stonecuttingRecipes, recipesToRemove);
        collectSmithingTemplateRemovals(plugin, craftingRecipes, recipesToRemove);
        collectSmithingTransformRemovals(plugin, smithingTransformRecipes, recipesToRemove);

        collectSmithingTransformAdditions(plugin, recipesToAdd);

        Bukkit.removeRecipe(recipesToRemove);
        Bukkit.addRecipe(recipesToAdd);
    }

    private static boolean isVanillaRecipe(final Keyed recipe) {
        return recipe.getKey().getNamespace().equals(NamespacedKey.MINECRAFT);
    }

    private static void collectFurnaceLikeRemovals(
            final Plugin plugin,
            final List<FurnaceRecipe> furnaceRecipes,
            final List<SmokingRecipe> smokingRecipes,
            final List<BlastingRecipe> blastingRecipes,
            final List<NamespacedKey> recipesToRemove
    ) {
        final Set<RecipeChoice> smokingInputs = new HashSet<>();
        smokingRecipes.forEach(smokingRecipe -> smokingInputs.add(smokingRecipe.getInputChoice()));

        int furnaceCount = 0;
        for (final FurnaceRecipe furnaceRecipe : furnaceRecipes) {
            if (smokingInputs.contains(furnaceRecipe.getInputChoice())) {
                recipesToRemove.add(furnaceRecipe.getKey());
                furnaceCount++;
            }
        }

        int blastingCount = 0;
        for (final BlastingRecipe blastingRecipe : blastingRecipes) {
            recipesToRemove.add(blastingRecipe.getKey());
            blastingCount++;
        }

        log(plugin, "[烧炼配方] 移除了 %d 个熔炉配方.", furnaceCount);
        log(plugin, "[烧炼配方] 移除了 %d 个高炉配方.", blastingCount);
    }

    private static void collectStonecuttingCraftingRemovals(
            final Plugin plugin,
            final List<StonecuttingRecipe> stonecuttingRecipes,
            final List<NamespacedKey> recipesToRemove
    ) {
        // 多个切石配方可能指向同一个合成配方，先按 key 去重。
        final Set<NamespacedKey> forRemove = new HashSet<>();

        for (final StonecuttingRecipe recipe : stonecuttingRecipes) {
            final String key = recipe.getKey().getKey();
            final int index = key.indexOf("_from_");
            if (index == -1) {
                continue;
            }

            final NamespacedKey craftingKey = NamespacedKey.minecraft(key.substring(0, index));
            if (Bukkit.getRecipe(craftingKey) instanceof CraftingRecipe craftingRecipe
                    && craftingRecipe.getResult().isSimilar(recipe.getResult())) {
                forRemove.add(craftingKey);
            }
        }

        recipesToRemove.addAll(forRemove);
        log(plugin, "[合成配方] 移除了 %d 个切石合成配方.", forRemove.size());
    }

    private static void collectSmithingTemplateRemovals(
            final Plugin plugin,
            final List<CraftingRecipe> craftingRecipes,
            final List<NamespacedKey> recipesToRemove
    ) {
        int count = 0;

        for (final CraftingRecipe recipe : craftingRecipes) {
            if (recipe instanceof ComplexRecipe || !recipe.getKey().getKey().endsWith("_smithing_template")) {
                continue;
            }

            final Collection<RecipeChoice> recipeChoices;
            if (recipe instanceof ShapedRecipe shapedRecipe) {
                recipeChoices = shapedRecipe.getChoiceMap().values();
            } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                recipeChoices = shapelessRecipe.getChoiceList();
            } else {
                throw new IllegalStateException("unknown recipe type: " + recipe.getClass());
            }

            final ItemStack result = recipe.getResult();
            if (recipeChoices.stream().anyMatch(recipeChoice -> recipeChoice.test(result))) {
                recipesToRemove.add(recipe.getKey());
                count++;
            }
        }

        log(plugin, "[合成配方] 移除了 %d 个模板复制配方.", count);
    }

    private static void collectSmithingTransformRemovals(
            final Plugin plugin,
            final List<SmithingTransformRecipe> smithingTransformRecipes,
            final List<NamespacedKey> recipesToRemove
    ) {
        int count = 0;
        for (final SmithingTransformRecipe recipe : smithingTransformRecipes) {
            recipesToRemove.add(recipe.getKey());
            count++;
        }
        log(plugin, "[锻造配方] 移除了 %d 个锻造升级配方.", count);
    }

    private static void collectSmithingTransformAdditions(
            final Plugin plugin,
            final List<Recipe> recipesToAdd
    ) {
        final int before = recipesToAdd.size();

        addTransformRecipe(Material.STONE_SWORD, Material.WOODEN_SWORD, MaterialSetTag.ITEMS_STONE_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.COPPER_SWORD, Material.STONE_SWORD, MaterialSetTag.ITEMS_COPPER_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.IRON_SWORD, Material.COPPER_SWORD, MaterialSetTag.ITEMS_IRON_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.GOLDEN_SWORD, Material.IRON_SWORD, MaterialSetTag.ITEMS_GOLD_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.DIAMOND_SWORD, Material.GOLDEN_SWORD, MaterialSetTag.ITEMS_DIAMOND_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.NETHERITE_SWORD, Material.DIAMOND_SWORD, MaterialSetTag.ITEMS_NETHERITE_TOOL_MATERIALS, recipesToAdd);

        addTransformRecipe(Material.STONE_SPEAR, Material.WOODEN_SPEAR, MaterialSetTag.ITEMS_STONE_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.COPPER_SPEAR, Material.STONE_SPEAR, MaterialSetTag.ITEMS_COPPER_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.IRON_SPEAR, Material.COPPER_SPEAR, MaterialSetTag.ITEMS_IRON_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.GOLDEN_SPEAR, Material.IRON_SPEAR, MaterialSetTag.ITEMS_GOLD_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.DIAMOND_SPEAR, Material.GOLDEN_SPEAR, MaterialSetTag.ITEMS_DIAMOND_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.NETHERITE_SPEAR, Material.DIAMOND_SPEAR, MaterialSetTag.ITEMS_NETHERITE_TOOL_MATERIALS, recipesToAdd);

        addTransformRecipe(Material.STONE_PICKAXE, Material.WOODEN_PICKAXE, MaterialSetTag.ITEMS_STONE_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.COPPER_PICKAXE, Material.STONE_PICKAXE, MaterialSetTag.ITEMS_COPPER_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.IRON_PICKAXE, Material.COPPER_PICKAXE, MaterialSetTag.ITEMS_IRON_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.GOLDEN_PICKAXE, Material.IRON_PICKAXE, MaterialSetTag.ITEMS_GOLD_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.DIAMOND_PICKAXE, Material.GOLDEN_PICKAXE, MaterialSetTag.ITEMS_DIAMOND_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.NETHERITE_PICKAXE, Material.DIAMOND_PICKAXE, MaterialSetTag.ITEMS_NETHERITE_TOOL_MATERIALS, recipesToAdd);

        addTransformRecipe(Material.STONE_AXE, Material.WOODEN_AXE, MaterialSetTag.ITEMS_STONE_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.COPPER_AXE, Material.STONE_AXE, MaterialSetTag.ITEMS_COPPER_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.IRON_AXE, Material.COPPER_AXE, MaterialSetTag.ITEMS_IRON_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.GOLDEN_AXE, Material.IRON_AXE, MaterialSetTag.ITEMS_GOLD_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.DIAMOND_AXE, Material.GOLDEN_AXE, MaterialSetTag.ITEMS_DIAMOND_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.NETHERITE_AXE, Material.DIAMOND_AXE, MaterialSetTag.ITEMS_NETHERITE_TOOL_MATERIALS, recipesToAdd);

        addTransformRecipe(Material.STONE_SHOVEL, Material.WOODEN_SHOVEL, MaterialSetTag.ITEMS_STONE_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.COPPER_SHOVEL, Material.STONE_SHOVEL, MaterialSetTag.ITEMS_COPPER_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.IRON_SHOVEL, Material.COPPER_SHOVEL, MaterialSetTag.ITEMS_IRON_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.GOLDEN_SHOVEL, Material.IRON_SHOVEL, MaterialSetTag.ITEMS_GOLD_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.DIAMOND_SHOVEL, Material.GOLDEN_SHOVEL, MaterialSetTag.ITEMS_DIAMOND_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.NETHERITE_SHOVEL, Material.DIAMOND_SHOVEL, MaterialSetTag.ITEMS_NETHERITE_TOOL_MATERIALS, recipesToAdd);

        addTransformRecipe(Material.STONE_HOE, Material.WOODEN_HOE, MaterialSetTag.ITEMS_STONE_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.COPPER_HOE, Material.STONE_HOE, MaterialSetTag.ITEMS_COPPER_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.IRON_HOE, Material.COPPER_HOE, MaterialSetTag.ITEMS_IRON_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.GOLDEN_HOE, Material.IRON_HOE, MaterialSetTag.ITEMS_GOLD_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.DIAMOND_HOE, Material.GOLDEN_HOE, MaterialSetTag.ITEMS_DIAMOND_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.NETHERITE_HOE, Material.DIAMOND_HOE, MaterialSetTag.ITEMS_NETHERITE_TOOL_MATERIALS, recipesToAdd);

        addTransformRecipe(Material.COPPER_HELMET, Material.LEATHER_HELMET, MaterialSetTag.ITEMS_COPPER_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.IRON_HELMET, Material.COPPER_HELMET, MaterialSetTag.ITEMS_IRON_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.GOLDEN_HELMET, Material.IRON_HELMET, MaterialSetTag.ITEMS_GOLD_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.DIAMOND_HELMET, Material.GOLDEN_HELMET, MaterialSetTag.ITEMS_DIAMOND_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.NETHERITE_HELMET, Material.DIAMOND_HELMET, MaterialSetTag.ITEMS_NETHERITE_TOOL_MATERIALS, recipesToAdd);

        addTransformRecipe(Material.COPPER_CHESTPLATE, Material.LEATHER_CHESTPLATE, MaterialSetTag.ITEMS_COPPER_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.IRON_CHESTPLATE, Material.COPPER_CHESTPLATE, MaterialSetTag.ITEMS_IRON_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.GOLDEN_CHESTPLATE, Material.IRON_CHESTPLATE, MaterialSetTag.ITEMS_GOLD_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.DIAMOND_CHESTPLATE, Material.GOLDEN_CHESTPLATE, MaterialSetTag.ITEMS_DIAMOND_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.NETHERITE_CHESTPLATE, Material.DIAMOND_CHESTPLATE, MaterialSetTag.ITEMS_NETHERITE_TOOL_MATERIALS, recipesToAdd);

        addTransformRecipe(Material.COPPER_LEGGINGS, Material.LEATHER_LEGGINGS, MaterialSetTag.ITEMS_COPPER_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.IRON_LEGGINGS, Material.COPPER_LEGGINGS, MaterialSetTag.ITEMS_IRON_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.GOLDEN_LEGGINGS, Material.IRON_LEGGINGS, MaterialSetTag.ITEMS_GOLD_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.DIAMOND_LEGGINGS, Material.GOLDEN_LEGGINGS, MaterialSetTag.ITEMS_DIAMOND_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.NETHERITE_LEGGINGS, Material.DIAMOND_LEGGINGS, MaterialSetTag.ITEMS_NETHERITE_TOOL_MATERIALS, recipesToAdd);

        addTransformRecipe(Material.COPPER_BOOTS, Material.LEATHER_BOOTS, MaterialSetTag.ITEMS_COPPER_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.IRON_BOOTS, Material.COPPER_BOOTS, MaterialSetTag.ITEMS_IRON_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.GOLDEN_BOOTS, Material.IRON_BOOTS, MaterialSetTag.ITEMS_GOLD_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.DIAMOND_BOOTS, Material.GOLDEN_BOOTS, MaterialSetTag.ITEMS_DIAMOND_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.NETHERITE_BOOTS, Material.DIAMOND_BOOTS, MaterialSetTag.ITEMS_NETHERITE_TOOL_MATERIALS, recipesToAdd);

        addTransformRecipe(Material.COPPER_HORSE_ARMOR, Material.LEATHER_HORSE_ARMOR, MaterialSetTag.ITEMS_COPPER_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.IRON_HORSE_ARMOR, Material.COPPER_HORSE_ARMOR, MaterialSetTag.ITEMS_IRON_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.GOLDEN_HORSE_ARMOR, Material.IRON_HORSE_ARMOR, MaterialSetTag.ITEMS_GOLD_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.DIAMOND_HORSE_ARMOR, Material.GOLDEN_HORSE_ARMOR, MaterialSetTag.ITEMS_DIAMOND_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.NETHERITE_HORSE_ARMOR, Material.DIAMOND_HORSE_ARMOR, MaterialSetTag.ITEMS_NETHERITE_TOOL_MATERIALS, recipesToAdd);

        addTransformRecipe(Material.IRON_NAUTILUS_ARMOR, Material.COPPER_NAUTILUS_ARMOR, MaterialSetTag.ITEMS_IRON_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.GOLDEN_NAUTILUS_ARMOR, Material.IRON_NAUTILUS_ARMOR, MaterialSetTag.ITEMS_GOLD_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.DIAMOND_NAUTILUS_ARMOR, Material.GOLDEN_NAUTILUS_ARMOR, MaterialSetTag.ITEMS_DIAMOND_TOOL_MATERIALS, recipesToAdd);
        addTransformRecipe(Material.NETHERITE_NAUTILUS_ARMOR, Material.DIAMOND_NAUTILUS_ARMOR, MaterialSetTag.ITEMS_NETHERITE_TOOL_MATERIALS, recipesToAdd);

        final int count = recipesToAdd.size() - before;
        log(plugin, "[锻造配方] 添加了 %d 个锻造升级配方.", count);
    }

    private static void addTransformRecipe(final Material result, final Material base, final Tag<Material> addition, final List<Recipe> recipesToAdd) {
        final ItemStack template = TemplateUtils.templateFor(result);
        final NamespacedKey namespacedKey = NamespacedKey.kiocg(result.name().toLowerCase(Locale.ROOT) + "_smithing");
        recipesToAdd.add(new SmithingTransformRecipe(
                namespacedKey,
                new ItemStack(result),
                new RecipeChoice.ExactChoice(template),
                new RecipeChoice.MaterialChoice(base),
                new RecipeChoice.MaterialChoice(addition)));
    }

    private static void log(final Plugin plugin, final String format, final Object... args) {
        plugin.getComponentLogger().info(Component.text(format.formatted(args)));
    }
}
