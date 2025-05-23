/*
 * File: GuiUtils.java
 * Utility class for GUI-related operations, with COIN_SLOT added
 */
package org.scripture.scripture.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.scripture.scripture.Scripture; // Restored Scripture import
// import org.scripture.scripture.holder.BookGuiHolder; // Removed BookGuiHolder import

public class GuiUtils {
    // Constants for GUI
    public static final int GUI_SIZE    = 27;
    public static final String GUI_TITLE = "Paper to Coin";
    public static final int PAPER_SLOT  = 11;
    public static final int COIN_SLOT   = 15;  // Added for shop:coin

    /**
     * Opens the Paper-to-Coin GUI
     */
    public static void openPaperToCoinGui(Scripture plugin, Player player) { // Scripture plugin parameter restored
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE); // Restored inventory creation

        // Fill with glass panes
        ItemStack pane = createGuiItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < GUI_SIZE; i++) {
            gui.setItem(i, pane); // Fill all slots with panes
        }
        // Clear paper and coin slots
        gui.clear(PAPER_SLOT); // Clear specific slots
        gui.clear(COIN_SLOT);  // Clear specific slots

        plugin.registerPlayerGui(player.getUniqueId(), gui); // Restored registration line
        player.openInventory(gui);
    }

    /**
     * Creates a GUI item with the specified material and name
     */
    public static ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Colorizes a string
     */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}