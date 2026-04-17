package com.kiocg.world.item.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class Metallurgy {
    private static final SmeltItemFunction SMELT_ITEM_FUNCTION = new SmeltItemFunction(List.of(), true);

    private static final Registry<Enchantment> ENCHANTMENT = MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    private static final TagKey<Block> TAG_ORES = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("ores"));
    private static final Identifier KEY_METALLURGY = Identifier.fromNamespaceAndPath(Identifier.KIOCG_NAMESPACE, "metallurgy");
    private static final Holder.@Nullable Reference<Enchantment> METALLURGY;

    static {
        METALLURGY = ENCHANTMENT.get(KEY_METALLURGY).orElse(null);
    }

    public static List<ItemStack> onGetDrops(final List<ItemStack> drops, final BlockState state, final LootParams.Builder params) {
        if (METALLURGY == null) return drops;
        if (drops.isEmpty()) return drops;
        if (!state.is(TAG_ORES)) return drops;

        ItemInstance tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null) {
            int metallurgyLevel = EnchantmentHelper.getItemEnchantmentLevel(METALLURGY, tool);
            if (metallurgyLevel > 0) {
                drops.replaceAll(itemStack -> SMELT_ITEM_FUNCTION.run(itemStack, params.getLevel()));
            }
        }

        return drops;
    }
}
