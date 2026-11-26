package com.kiocg.bukkit.plugin.listeners;

import com.kiocg.bukkit.plugin.TemplateUtils;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ListIterator;

public class GenerateTemplate implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onLootGenerate(final LootGenerateEvent e) {
        final ListIterator<ItemStack> iterator = e.getLoot().listIterator();
        while (iterator.hasNext()) {
            final ItemStack itemStack = iterator.next();
            if (itemStack == null) {
                continue;
            }

            if (itemStack.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
                iterator.set(TemplateUtils.getRandomTemplate());
            }
        }
    }
}
