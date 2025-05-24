package org.scripture.scripture.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.scripture.scripture.Scripture; // Import the main plugin class

/**
 * Handles the `/scripture reload` command, allowing administrators
 * to reload the plugin's configuration without restarting the server.
 */
public class ScriptureReloadCommand implements CommandExecutor {

    private final Scripture plugin;

    /**
     * Constructs the ScriptureReloadCommand.
     * @param plugin An instance of the main Scripture plugin class, used to access plugin functionalities like config reloading.
     */
    public ScriptureReloadCommand(Scripture plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("scripture.reload")) {
                sender.sendMessage(org.bukkit.ChatColor.RED + "You do not have permission to reload the Scripture plugin.");
                return true;
            }

            try {
                plugin.reloadPluginConfig(); // Calls the method in the main plugin class to perform the reload.
                sender.sendMessage(org.bukkit.ChatColor.GREEN + "Scripture plugin configuration reloaded successfully.");
            } catch (Exception e) {
                sender.sendMessage(org.bukkit.ChatColor.RED + "Failed to reload Scripture plugin configuration. Check console for errors.");
                plugin.getLogger().severe("Error during plugin reload: " + e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        // If not the 'reload' subcommand, or no subcommand is provided.
        // Returning false will show the usage message from plugin.yml if defined for the base command.
        return false;
    }
}
