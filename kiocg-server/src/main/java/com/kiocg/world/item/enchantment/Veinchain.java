package com.kiocg.world.item.enchantment;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Veinchain {
    private static final Registry<Enchantment> ENCHANTMENT = MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    private static final TagKey<Block> TAG_ORES = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("ores"));
    private static final Identifier KEY_VEINCHAIN = Identifier.fromNamespaceAndPath(Identifier.KIOCG_NAMESPACE, "veinchain");
    private static final Holder.@Nullable Reference<Enchantment> VEINCHAIN;

    static {
        VEINCHAIN = ENCHANTMENT.get(KEY_VEINCHAIN).orElse(null);
    }

    public static void onDestroyBlock(final Level level, final BlockPos pos, final BlockState state, final ServerPlayer player, final ItemStack itemStack) {
        if (VEINCHAIN == null) return;
        if (!state.is(TAG_ORES)) return;

        int veinchainLevel = itemStack.getEnchantments().getLevel(VEINCHAIN);
        if (veinchainLevel > 0) {
            List<BlockPos> connectedOres = getConnectedOres(level, pos, veinchainLevel, itemStack);
            connectedOres.forEach(blockPos -> player.gameMode.destroyBlock(blockPos, false));
        }
    }

    private static List<BlockPos> getConnectedOres(final Level level, final BlockPos startPos, final int maxDistance, final ItemStack itemStack) {
        List<BlockPos> result = new ArrayList<>();
        if (maxDistance <= 0) return result; // 处理 maxDistance=0 的情况

        // 使用 long 编码坐标，避免 BlockPos 分配
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        LongOpenHashSet visited = new LongOpenHashSet();

        long startLong = startPos.asLong();
        queue.enqueue(startLong);
        visited.add(startLong);

        // 记录从起点到每个位置的步数
        int nextDistance = 1;
        Direction[] directions = Direction.values();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        Object2BooleanMap<Block> correctToolForDrops = new Object2BooleanOpenHashMap<>();

        while (!queue.isEmpty()) {
            int layerSize = queue.size();
            for (int i = 0; i < layerSize; i++) {
                long currentLong = queue.dequeueLong();
                // 将 long 解码回坐标，填充到 mutable 中
                mutable.set(currentLong);

                for (Direction direction : directions) {
                    mutable.move(direction);
                    long neighborLong = mutable.asLong();
                    if (visited.add(neighborLong)) {
                        BlockState neighborState = level.getBlockStateIfLoadedAndInBounds(mutable);
                        if (neighborState != null && neighborState.is(TAG_ORES)
                                && correctToolForDrops.computeIfAbsent(neighborState.getBlock(), _ -> !neighborState.requiresCorrectToolForDrops() || itemStack.isCorrectToolForDrops(neighborState))) {
                            result.add(mutable.immutable());
                            if (nextDistance < maxDistance) {
                                queue.enqueue(neighborLong);
                            }
                        }
                    }
                    mutable.move(direction.getOpposite()); // 恢复位置
                }
            }
            nextDistance++;
        }

        return result;
    }
}
