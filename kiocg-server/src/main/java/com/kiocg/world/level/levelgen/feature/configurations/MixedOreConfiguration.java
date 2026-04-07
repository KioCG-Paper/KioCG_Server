package com.kiocg.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public class MixedOreConfiguration implements FeatureConfiguration {
    public static final Codec<MixedOreConfiguration> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                            MixedOreConfiguration.TargetBlockState.CODEC.fieldOf("targets").forGetter(c -> c.targetStates),
                            Codec.intRange(0, 64).fieldOf("size").forGetter(c -> c.size),
                            Codec.floatRange(0.0F, 1.0F).fieldOf("discard_chance_on_air_exposure").forGetter(c -> c.discardChanceOnAirExposure)
                    )
                    .apply(i, MixedOreConfiguration::new)
    );
    public final MixedOreConfiguration.TargetBlockState targetStates;
    public final int size;
    public final float discardChanceOnAirExposure;

    public MixedOreConfiguration(final MixedOreConfiguration.TargetBlockState targetBlockStates, final int size, final float discardChanceOnAirExposure) {
        this.size = size;
        this.targetStates = targetBlockStates;
        this.discardChanceOnAirExposure = discardChanceOnAirExposure;
    }

    public static class TargetBlockState {
        public static final Codec<MixedOreConfiguration.TargetBlockState> CODEC = RecordCodecBuilder.create(
                i -> i.group(RuleTest.CODEC.fieldOf("stone_target").forGetter(c -> c.stoneTarget), RuleTest.CODEC.fieldOf("deepslate_target").forGetter(c -> c.deepslateTarget))
                        .apply(i, MixedOreConfiguration.TargetBlockState::new)
        );
        public final RuleTest stoneTarget;
        public final RuleTest deepslateTarget;

        private TargetBlockState(final RuleTest stoneTarget, final RuleTest deepslateTarget) {
            this.stoneTarget = stoneTarget;
            this.deepslateTarget = deepslateTarget;
        }
    }
}
