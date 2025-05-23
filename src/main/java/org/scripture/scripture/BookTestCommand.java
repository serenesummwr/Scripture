package org.scripture.scripture;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.scripture.scripture.util.GuiUtils;

import java.util.ArrayList;
import java.util.List;

public class BookTestCommand implements CommandExecutor, TabCompleter {
    private final Scripture plugin;

    public BookTestCommand(Scripture plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GuiUtils.colorize("&cOnly players can use this command!"));
            return true;
        }
        if (!player.hasPermission("booktest.use")) {
            player.sendMessage(GuiUtils.colorize("&cYou don't have permission to use this command!"));
            return true;
        }

        // เรียกเมธอดใหม่ที่ตรงกับ GuiUtils.java
        GuiUtils.openPaperToCoinGui(plugin, player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}