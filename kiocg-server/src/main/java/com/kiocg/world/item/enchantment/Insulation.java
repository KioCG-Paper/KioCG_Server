package com.kiocg.world.item.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

public class Insulation {
    private static final Registry<Enchantment> ENCHANTMENT = MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    private static final Identifier KEY_INSULATION = Identifier.fromNamespaceAndPath(Identifier.KIOCG_NAMESPACE, "insulation");
    private static final Holder.@Nullable Reference<Enchantment> INSULATION;

    static {
        INSULATION = ENCHANTMENT.get(KEY_INSULATION).orElse(null);
    }

    public static void update(final ServerPlayer player, final EquipmentSlot slot, final ItemStack itemStack) {
        if (INSULATION == null) return;

        int insulationLevel = itemStack.getEnchantments().getLevel(INSULATION);
        if (insulationLevel > 0) {
            player.insulationLevels.put(slot, insulationLevel);
        } else {
            player.insulationLevels.removeInt(slot);
        }
    }
}
