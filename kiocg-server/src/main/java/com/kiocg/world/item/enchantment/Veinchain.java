package com.kiocg.world.item.enchantment;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class Veinchain {
    private static final Registry<Enchantment> ENCHANTMENT = MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    private static final TagKey<Block> TAG_ORES = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("ores"));
    private static final Identifier KEY_VEINCHAIN = Identifier.fromNamespaceAndPath(Identifier.KIOCG_NAMESPACE, "veinchain");
    private static final Holder.@Nullable Reference<Enchantment> VEINCHAIN;
    private static final Direction[] ALL_DIRECTIONS = Direction.values();

    static {
        VEINCHAIN = ENCHANTMENT.get(KEY_VEINCHAIN).orElse(null);
    }

    private final Level level;
    private final int maxDistance;
    private final ItemStack itemStack;

    // 使用 long 编码坐标，避免 BlockPos 分配
    private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    private final LongOpenHashSet visited = new LongOpenHashSet();

    // 记录从起点到每个位置的步数
    private int nextDistance = 1;
    private final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

    public Veinchain(final Level level, final BlockPos startPos, final int maxDistance, final ItemStack itemStack) {
        Preconditions.checkArgument(maxDistance > 0, "maxDistance must be positive");
        this.level = level;
        this.maxDistance = maxDistance;
        this.itemStack = itemStack;

        long startLong = startPos.asLong();
        queue.enqueue(startLong);
        visited.add(startLong);
    }

    public static Veinchain onDestroyBlock(final Level level, final BlockPos pos, final BlockState state, final ItemStack itemStack) {
        if (VEINCHAIN == null) return null;
        if (!state.is(TAG_ORES)) return null;

        int veinchainLevel = itemStack.getEnchantments().getLevel(VEINCHAIN);
        if (veinchainLevel <= 0) return null;

        return new Veinchain(level, pos, veinchainLevel, itemStack);
    }

    public boolean tick(final ServerPlayer player, final ItemStack heldItem) {
        boolean blockDestroyed = false;
        if (!queue.isEmpty() && itemStack == heldItem) { // 防止手中物品更改
            int layerSize = queue.size();
            for (int i = 0; i < layerSize; i++) {
                long currentLong = queue.dequeueLong();
                // 将 long 解码回坐标，填充到 mutable 中
                mutable.set(currentLong);

                for (Direction direction : ALL_DIRECTIONS) {
                    mutable.move(direction);
                    long neighborLong = mutable.asLong();
                    if (visited.add(neighborLong)) {
                        BlockState neighborState = level.getBlockStateIfLoadedAndInBounds(mutable);
                        if (neighborState != null && neighborState.is(TAG_ORES)
                                && (!neighborState.requiresCorrectToolForDrops() || heldItem.isCorrectToolForDrops(neighborState))) {
                            BlockPos blockPos = mutable.immutable();
                            if (player.gameMode.destroyBlock(blockPos, false)) {
                                sendDestroyParticles(player, blockPos, neighborState);
                                blockDestroyed = true;
                            }
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
        return blockDestroyed;
    }

    private void sendDestroyParticles(final ServerPlayer player, final BlockPos pos, final BlockState state) {
        if (player.level() == this.level) {
            double xd = pos.getX() - player.getX();
            double yd = pos.getY() - player.getY();
            double zd = pos.getZ() - player.getZ();
            if (xd * xd + yd * yd + zd * zd < 64.0 * 64.0) {
                ClientboundLevelEventPacket packet = new ClientboundLevelEventPacket(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state), false);
                player.connection.send(packet);
            }
        }
    }
}
