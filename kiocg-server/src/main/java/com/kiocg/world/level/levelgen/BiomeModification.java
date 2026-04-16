package com.kiocg.world.level.levelgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.*;

public class BiomeModification {
    public final PlacedFeatureModifications placedFeatureModifications = new PlacedFeatureModifications();
    private final RegistryAccess registryAccess;

    public BiomeModification(RegistryAccess registryAccess) {
        this.registryAccess = registryAccess;
        addDefaultModifications();
    }

    private void addDefaultModifications() {
        final Registry<PlacedFeature> lookup = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
        final Holder.Reference<PlacedFeature> oreDiamond = lookup.getOrThrow(OrePlacements.ORE_DIAMOND);
        final Holder.Reference<PlacedFeature> oreEmerald = lookup.getOrThrow(OrePlacements.ORE_EMERALD);
        final Holder.Reference<PlacedFeature> mixedOre = lookup.getOrThrow(PlacementUtils.createKey("kiocg_mixed_ore"));

        final Registry<Biome> biomeRegistry = registryAccess.lookupOrThrow(Registries.BIOME);
        for (Biome biome : biomeRegistry) {
            final List<HolderSet<PlacedFeature>> features = biome.getGenerationSettings().features();
            if (features.size() > GenerationStep.Decoration.UNDERGROUND_ORES.ordinal()) {
                final HolderSet<PlacedFeature> holders = features.get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());
                if (holders.contains(oreDiamond)) {
                    if (!holders.contains(oreEmerald)) {
                        placedFeatureModifications.modify(biome, GenerationStep.Decoration.UNDERGROUND_ORES, oreEmerald);
                    }
                    placedFeatureModifications.modify(biome, GenerationStep.Decoration.UNDERGROUND_ORES, mixedOre);
                }
            }
        }
    }

    public void applyModifications() {
        placedFeatureModifications.apply();
    }

    public static class PlacedFeatureModifications {
        private final Map<Biome, List<PlacedFeatureModification>> modifications = new HashMap<>();

        public void modify(final Biome biome, final GenerationStep.Decoration decoration, final Holder<PlacedFeature> placedFeature) {
            modifications.computeIfAbsent(biome, _ -> new ArrayList<>()).add(new PlacedFeatureModification(decoration, placedFeature));
        }

        private void apply() {
            modifications.forEach((biome, placedFeatureModifications) -> {
                final BiomeGenerationSettings generationSettings = biome.getGenerationSettings();
                final Iterable<Holder<ConfiguredWorldCarver<?>>> carvers = generationSettings.getCarvers();
                final List<HolderSet<PlacedFeature>> features = generationSettings.features();

                final BiomeGenerationSettings.PlainBuilder builder = new BiomeGenerationSettings.PlainBuilder();
                for (Holder<ConfiguredWorldCarver<?>> carver : carvers) {
                    builder.addCarver(carver);
                }
                for (int i = 0; i < features.size(); i++) {
                    for (Holder<PlacedFeature> holder : features.get(i)) {
                        builder.addFeature(i, holder);
                    }
                }

                placedFeatureModifications.forEach(modification -> builder.addFeature(modification.generationStep, modification.holder));
                biome.generationSettings = builder.build();
            });
        }

        private record PlacedFeatureModification(GenerationStep.Decoration generationStep, Holder<PlacedFeature> holder) {
        }
    }
}
