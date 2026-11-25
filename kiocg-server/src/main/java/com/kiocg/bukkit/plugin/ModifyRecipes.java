package com.kiocg.bukkit.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
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
            }
        });

        collectFurnaceLikeRemovals(plugin, furnaceRecipes, smokingRecipes, blastingRecipes, recipesToRemove);
        collectStonecuttingCraftingRemovals(plugin, stonecuttingRecipes, recipesToRemove);
        collectSmithingTemplateRemovals(plugin, craftingRecipes, recipesToRemove);

        // TODO: 升级配方（UpgradeRecipe）等新增配方
        // recipesToAdd.add(...);

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

    private static void log(final Plugin plugin, final String format, final Object... args) {
        plugin.getComponentLogger().info(Component.text(format.formatted(args)));
    }
}
