package org.scripture.scripture.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.scripture.scripture.Scripture;
import org.scripture.scripture.gui.PaperToCoinHolder;

public class GuiUtils {

    /**
     * Opens the Paper-to-Coin GUI using the custom InventoryHolder
     */
    public static void openPaperToCoinGui(Scripture plugin, Player player) {
        PaperToCoinHolder holder = new PaperToCoinHolder(plugin, player);
        holder.open();
    }

    /**
     * Colorizes a string using ChatColor
     */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}