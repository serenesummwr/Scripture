package org.scripture.scripture;

import org.bukkit.plugin.java.JavaPlugin;
import org.scripture.scripture.util.GuiUtils;

public final class Scripture extends JavaPlugin {

    @Override
    public void onEnable() {
        // Log startup
        getLogger().info("Scripture plugin is starting up!");

        // Register command using Paper's recommended approach
        BookTestCommand bookTestCommand = new BookTestCommand(this);

        // Get the server's command map
        org.bukkit.command.CommandMap commandMap = getServer().getCommandMap();

        // Create a PluginCommand using reflection (since constructor is protected)
        org.bukkit.command.PluginCommand pluginCommand;
        try {
            java.lang.reflect.Constructor<org.bukkit.command.PluginCommand> constructor =
                    org.bukkit.command.PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            constructor.setAccessible(true);
            pluginCommand = constructor.newInstance("booktest", this);

            // Set command properties
            pluginCommand.setDescription("Open the Paper to Coin GUI");
            pluginCommand.setUsage("/<command>");
            pluginCommand.setPermission("booktest.use");
            pluginCommand.setPermissionMessage(GuiUtils.colorize("&cYou don't have permission to use this command!"));

            // Set the executor and tab completer
            pluginCommand.setExecutor(bookTestCommand);
            pluginCommand.setTabCompleter(bookTestCommand);

            // Register the command
            commandMap.register("scripture", pluginCommand);

            getLogger().info("Successfully registered the booktest command!");
        } catch (Exception e) {
            getLogger().severe("Failed to register the booktest command: " + e.getMessage());
            e.printStackTrace();
        }

        // Register inventory listener
        getServer().getPluginManager().registerEvents(new BookGuiListener(this), this);

        getLogger().info("Scripture plugin has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Scripture plugin has been disabled.");
    }
}