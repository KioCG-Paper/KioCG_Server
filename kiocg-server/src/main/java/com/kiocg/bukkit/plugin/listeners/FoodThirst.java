package com.kiocg.bukkit.plugin.listeners;

import com.kiocg.bukkit.plugin.InternalPlugin;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

public class FoodThirst implements Listener {
    private static final String THIRST_ITEM_PATH = "thirst.items.";
    private static final String WATER_BOTTLE_PATH = "thirst.water_bottle";

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemConsume(final PlayerItemConsumeEvent e) {
        final ItemStack item = e.getItem();
        final String path;
        if (item.getType() == Material.POTION && isWaterBottle(item)) {
            path = WATER_BOTTLE_PATH;
        } else {
            path = THIRST_ITEM_PATH + item.getType().getKey().getKey();
        }

        final FileConfiguration config = InternalPlugin.getInstance().getConfig();
        if (config.contains(path)) {
            e.getPlayer().addThirstLevel(config.getInt(path));
        }
    }

    private static boolean isWaterBottle(final ItemStack item) {
        final PotionContents contents = item.getData(DataComponentTypes.POTION_CONTENTS);
        return contents != null && contents.potion() == PotionType.WATER;
    }
}
