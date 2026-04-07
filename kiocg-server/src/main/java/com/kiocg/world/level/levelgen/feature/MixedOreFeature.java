package com.kiocg.world.level.levelgen.feature;

import com.kiocg.world.level.levelgen.feature.configurations.MixedOreConfiguration;
import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class MixedOreFeature extends Feature<MixedOreConfiguration> {
    private static final List<Pair<Block, Block>> ORE_BLOCKS = new ArrayList<>();
    private static final List<Pair<Block, Block>> DEEPSLATE_ORE_BLOCKS = new ArrayList<>();

    static {
        ORE_BLOCKS.add(Pair.of(Blocks.COAL_ORE, null));
        ORE_BLOCKS.add(Pair.of(Blocks.COPPER_ORE, Blocks.RAW_COPPER_BLOCK));
        ORE_BLOCKS.add(Pair.of(Blocks.IRON_ORE, Blocks.RAW_IRON_BLOCK));
        ORE_BLOCKS.add(Pair.of(Blocks.GOLD_ORE, Blocks.RAW_GOLD_BLOCK));
        ORE_BLOCKS.add(Pair.of(Blocks.LAPIS_ORE, null));
        ORE_BLOCKS.add(Pair.of(Blocks.REDSTONE_ORE, null));
        ORE_BLOCKS.add(Pair.of(Blocks.DIAMOND_ORE, null));
        ORE_BLOCKS.add(Pair.of(Blocks.EMERALD_ORE, null));

        DEEPSLATE_ORE_BLOCKS.add(Pair.of(Blocks.DEEPSLATE_COAL_ORE, null));
        DEEPSLATE_ORE_BLOCKS.add(Pair.of(Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK));
        DEEPSLATE_ORE_BLOCKS.add(Pair.of(Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK));
        DEEPSLATE_ORE_BLOCKS.add(Pair.of(Blocks.DEEPSLATE_GOLD_ORE, Blocks.RAW_GOLD_BLOCK));
        DEEPSLATE_ORE_BLOCKS.add(Pair.of(Blocks.DEEPSLATE_LAPIS_ORE, null));
        DEEPSLATE_ORE_BLOCKS.add(Pair.of(Blocks.DEEPSLATE_REDSTONE_ORE, null));
        DEEPSLATE_ORE_BLOCKS.add(Pair.of(Blocks.DEEPSLATE_DIAMOND_ORE, null));
        DEEPSLATE_ORE_BLOCKS.add(Pair.of(Blocks.DEEPSLATE_EMERALD_ORE, null));
    }

    public MixedOreFeature(final Codec<MixedOreConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<MixedOreConfiguration> context) {
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        MixedOreConfiguration config = context.config();
        float dir = random.nextFloat() * (float) Math.PI;
        float spreadXY = config.size / 8.0F;
        int maxRadius = Mth.ceil((config.size / 16.0F * 2.0F + 1.0F) / 2.0F);
        double x0 = origin.getX() + Math.sin(dir) * spreadXY;
        double x1 = origin.getX() - Math.sin(dir) * spreadXY;
        double z0 = origin.getZ() + Math.cos(dir) * spreadXY;
        double z1 = origin.getZ() - Math.cos(dir) * spreadXY;
        int spreadY = 2;
        double y0 = origin.getY() + random.nextInt(3) - 2;
        double y1 = origin.getY() + random.nextInt(3) - 2;
        int xStart = origin.getX() - Mth.ceil(spreadXY) - maxRadius;
        int yStart = origin.getY() - 2 - maxRadius;
        int zStart = origin.getZ() - Mth.ceil(spreadXY) - maxRadius;
        int sizeXZ = 2 * (Mth.ceil(spreadXY) + maxRadius);
        int sizeY = 2 * (2 + maxRadius);

        for (int xprobe = xStart; xprobe <= xStart + sizeXZ; xprobe++) {
            for (int zprobe = zStart; zprobe <= zStart + sizeXZ; zprobe++) {
                if (yStart <= level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, xprobe, zprobe)) {
                    return this.doPlace(level, random, config, x0, x1, z0, z1, y0, y1, xStart, yStart, zStart, sizeXZ, sizeY);
                }
            }
        }

        return false;
    }

    protected boolean doPlace(
            final WorldGenLevel level,
            final RandomSource random,
            final MixedOreConfiguration config,
            final double x0,
            final double x1,
            final double z0,
            final double z1,
            final double y0,
            final double y1,
            final int xStart,
            final int yStart,
            final int zStart,
            final int sizeXZ,
            final int sizeY
    ) {
        int placed = 0;
        BitSet tested = new BitSet(sizeXZ * sizeY * sizeXZ);
        BlockPos.MutableBlockPos orePos = new BlockPos.MutableBlockPos();
        int size = config.size;
        double[] data = new double[size * 4];

        for (int i = 0; i < size; i++) {
            float step = (float) i / size;
            double xx = Mth.lerp((double) step, x0, x1);
            double yy = Mth.lerp((double) step, y0, y1);
            double zz = Mth.lerp((double) step, z0, z1);
            double ss = random.nextDouble() * size / 16.0;
            double r = ((Mth.sin((float) Math.PI * step) + 1.0F) * ss + 1.0) / 2.0;
            data[i * 4 + 0] = xx;
            data[i * 4 + 1] = yy;
            data[i * 4 + 2] = zz;
            data[i * 4 + 3] = r;
        }

        for (int i1 = 0; i1 < size - 1; i1++) {
            if (!(data[i1 * 4 + 3] <= 0.0)) {
                for (int i2 = i1 + 1; i2 < size; i2++) {
                    if (!(data[i2 * 4 + 3] <= 0.0)) {
                        double dx = data[i1 * 4 + 0] - data[i2 * 4 + 0];
                        double dy = data[i1 * 4 + 1] - data[i2 * 4 + 1];
                        double dz = data[i1 * 4 + 2] - data[i2 * 4 + 2];
                        double dr = data[i1 * 4 + 3] - data[i2 * 4 + 3];
                        if (dr * dr > dx * dx + dy * dy + dz * dz) {
                            if (dr > 0.0) {
                                data[i2 * 4 + 3] = -1.0;
                            } else {
                                data[i1 * 4 + 3] = -1.0;
                            }
                        }
                    }
                }
            }
        }

        try (BulkSectionAccess sectionGetter = new BulkSectionAccess(level)) {
            for (int i = 0; i < size; i++) {
                double r = data[i * 4 + 3];
                if (!(r < 0.0)) {
                    double xx = data[i * 4 + 0];
                    double yy = data[i * 4 + 1];
                    double zz = data[i * 4 + 2];
                    int xMin = Math.max(Mth.floor(xx - r), xStart);
                    int yMin = Math.max(Mth.floor(yy - r), yStart);
                    int zMin = Math.max(Mth.floor(zz - r), zStart);
                    int xMax = Math.max(Mth.floor(xx + r), xMin);
                    int yMax = Math.max(Mth.floor(yy + r), yMin);
                    int zMax = Math.max(Mth.floor(zz + r), zMin);

                    for (int x = xMin; x <= xMax; x++) {
                        double xd = (x + 0.5 - xx) / r;
                        if (xd * xd < 1.0) {
                            for (int y = yMin; y <= yMax; y++) {
                                double yd = (y + 0.5 - yy) / r;
                                if (xd * xd + yd * yd < 1.0) {
                                    for (int z = zMin; z <= zMax; z++) {
                                        double zd = (z + 0.5 - zz) / r;
                                        if (xd * xd + yd * yd + zd * zd < 1.0 && !level.isOutsideBuildHeight(y)) {
                                            int bitSetIndex = x - xStart + (y - yStart) * sizeXZ + (z - zStart) * sizeXZ * sizeY;
                                            if (!tested.get(bitSetIndex)) {
                                                tested.set(bitSetIndex);
                                                orePos.set(x, y, z);
                                                if (level.ensureCanWrite(orePos)) {
                                                    LevelChunkSection section = sectionGetter.getSection(orePos);
                                                    if (section != null) {
                                                        int sectionRelativeX = SectionPos.sectionRelative(x);
                                                        int sectionRelativeY = SectionPos.sectionRelative(y);
                                                        int sectionRelativeZ = SectionPos.sectionRelative(z);
                                                        BlockState blockState = section.getBlockState(sectionRelativeX, sectionRelativeY, sectionRelativeZ);

                                                        Supplier<BlockState> oreSupplier = null;
                                                        if (config.targetStates.stoneTarget.test(blockState, random)) {
                                                            oreSupplier = () -> getRandomOre(random);
                                                        } else if (config.targetStates.deepslateTarget.test(blockState, random)) {
                                                            oreSupplier = () -> getRandomDeepslateOre(random);
                                                        }
                                                        
                                                        if (oreSupplier != null) {
                                                            if (canPlaceOre(sectionGetter::getBlockState, random, config, orePos)) {
                                                                section.setBlockState(
                                                                        sectionRelativeX, sectionRelativeY, sectionRelativeZ, oreSupplier.get(), false
                                                                );
                                                                placed++;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return placed > 0;
    }

    public static boolean canPlaceOre(
            final Function<BlockPos, BlockState> blockGetter,
            final RandomSource random,
            final MixedOreConfiguration config,
            final BlockPos.MutableBlockPos orePos
    ) {
        return (shouldSkipAirCheck(random, config.discardChanceOnAirExposure) || !isAdjacentToAir(blockGetter, orePos));
    }

    public static BlockState getRandomOre(final RandomSource random) {
        return getRandomState(ORE_BLOCKS, random).defaultBlockState();
    }

    public static BlockState getRandomDeepslateOre(final RandomSource random) {
        return getRandomState(DEEPSLATE_ORE_BLOCKS, random).defaultBlockState();
    }

    private static <T> T getRandomState(final List<Pair<T, T>> list, final RandomSource random) {
        Pair<T, T> pair = list.get(random.nextInt(list.size()));
        if (pair.second() == null || random.nextInt(100) >= 10) {
            return pair.first();
        }
        return pair.second();
    }

    protected static boolean shouldSkipAirCheck(final RandomSource random, final float discardChanceOnAirExposure) {
        return discardChanceOnAirExposure <= 0.0F || !(discardChanceOnAirExposure >= 1.0F) && random.nextFloat() >= discardChanceOnAirExposure;
    }
}
