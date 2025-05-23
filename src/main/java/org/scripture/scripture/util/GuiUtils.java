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
// Scripture import removed as it's no longer needed
import org.scripture.scripture.holder.BookGuiHolder; // Added BookGuiHolder import

public class GuiUtils {
    // Constants for GUI
    public static final int GUI_SIZE    = 27;
    public static final String GUI_TITLE = "Paper to Coin";
    public static final int PAPER_SLOT  = 11;
    public static final int COIN_SLOT   = 15;  // Added for shop:coin

    /**
     * Opens the Paper-to-Coin GUI
     */
    public static void openPaperToCoinGui(Player player) { // Scripture plugin parameter removed
        BookGuiHolder holder = new BookGuiHolder(GUI_SIZE, GUI_TITLE); // Create GUI with BookGuiHolder
        Inventory gui = holder.getInventory(); // Get the inventory from the holder

        // Fill with glass panes, avoiding PAPER_SLOT and COIN_SLOT
        ItemStack pane = createGuiItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < GUI_SIZE; i++) {
            if (i != PAPER_SLOT && i != COIN_SLOT) { // Avoid overwriting slots
                gui.setItem(i, pane);
            }
        }
        // Ensure paper and coin slots are initially empty
        gui.setItem(PAPER_SLOT, null); // Explicitly set to null
        gui.setItem(COIN_SLOT, null);  // Explicitly set to null

        player.openInventory(gui);
        // plugin.registerPlayerGui line removed
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