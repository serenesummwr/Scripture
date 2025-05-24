package org.scripture.scripture;

import org.bukkit.plugin.java.JavaPlugin;
import org.scripture.scripture.command.ScriptureReloadCommand; // Import the new command class
import org.scripture.scripture.util.GuiUtils;

public final class Scripture extends JavaPlugin {

    @Override
    public void onEnable() {
        // Log startup
        getLogger().info("Scripture plugin is starting up!");
        // Save the default config.yml from the JAR to the plugin's data folder if it doesn't already exist.
        // This ensures that a default configuration is always available.
        saveDefaultConfig();
        // Load the configuration from config.yml. This includes any user-defined values or defaults.
        // This method is also responsible for initializing any components that depend on the configuration.
        loadPluginConfig(); // Initial load of configuration

        // Register command using Paper's recommended approach
        BookTestCommand bookTestCommand = new BookTestCommand(this);

        // Get the server's command map
        org.bukkit.command.CommandMap commandMap = getServer().getCommandMap();

        // Create a PluginCommand using reflection (since constructor is protected)
        org.bukkit.command.PluginCommand bookTestPluginCommand; // Renamed for clarity
        try {
            java.lang.reflect.Constructor<org.bukkit.command.PluginCommand> constructor =
                    org.bukkit.command.PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            constructor.setAccessible(true);
            bookTestPluginCommand = constructor.newInstance("booktest", this); // Command name "booktest"

            // Set command properties for booktest
            bookTestPluginCommand.setDescription("Open the Paper to Coin GUI");
            bookTestPluginCommand.setUsage("/<command>"); // Generic usage, specific usage in plugin.yml for subcommands
            bookTestPluginCommand.setPermission("booktest.use");
            bookTestPluginCommand.setPermissionMessage(GuiUtils.colorize("&cYou don't have permission to use this command!"));

            // Set the executor and tab completer for booktest
            bookTestPluginCommand.setExecutor(bookTestCommand);
            bookTestPluginCommand.setTabCompleter(bookTestCommand);

            // Register the booktest command
            commandMap.register("scripture", bookTestPluginCommand); // Keep "scripture" as prefix for this command for now

            getLogger().info("Successfully registered the booktest command!");
        } catch (Exception e) {
            getLogger().severe("Failed to register the booktest command: " + e.getMessage());
            e.printStackTrace();
        }

        // Register the /scripture command (which includes /scripture reload)
        // This command is defined in plugin.yml, so we retrieve it and set its executor.
        try {
            // The command 'scripture' (handling subcommands like 'reload') is defined in plugin.yml.
            // We use getCommand() to retrieve it from Bukkit's command system.
            org.bukkit.command.PluginCommand scriptureMainCommand = getCommand("scripture");
            if (scriptureMainCommand != null) {
                // Set the executor for the /scripture command to our ScriptureReloadCommand class.
                // This class will handle the logic for subcommands like /scripture reload.
                scriptureMainCommand.setExecutor(new ScriptureReloadCommand(this));
                // A TabCompleter could be added here if more subcommands are introduced later,
                // to provide suggestions for subcommands.
                // e.g., scriptureMainCommand.setTabCompleter(new ScriptureTabCompleter());
                getLogger().info("Successfully registered the main /scripture command executor!");
            } else {
                // This error indicates a mismatch between plugin.yml and the code, or an issue with Bukkit's command registration.
                getLogger().severe("Failed to get the '/scripture' command. Ensure it is defined in plugin.yml.");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to register the /scripture command executor: " + e.getMessage());
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

    /**
     * Reloads the plugin's configuration.
     * This method is typically called by a command (e.g., /scripture reload).
     * It ensures that changes made to the config.yml on disk are loaded into the plugin
     * and that relevant parts of the plugin are re-initialized with the new configuration.
     */
    public void reloadPluginConfig() {
        // Optional: Add logic here to safely disable or reset components before reloading.
        // For example, if listeners or tasks depend heavily on config values, they might
        // need to be unregistered or cancelled before loadPluginConfig() is called.
        getLogger().info("Reloading Scripture plugin configuration...");

        loadPluginConfig(); // The core logic for loading/reloading configuration

        // Optional: Add logic here to re-enable or re-initialize components based on the new config.
        // For example, re-register listeners or reschedule tasks if they were affected by the reload.
        getLogger().info("Scripture plugin configuration reloaded successfully.");
    }

    /**
     * Loads or reloads the plugin's configuration from the config.yml file.
     * This method handles reading the configuration values and should also
     * be responsible for applying these settings, such as re-initializing
     * any internal caches, settings, or components that depend on the configuration.
     * This method is called during onEnable for initial setup and by reloadPluginConfig()
     * for runtime reloading.
     */
    private void loadPluginConfig() {
        // This Bukkit method reloads the config.yml from disk, updating the plugin's internal
        // FileConfiguration object. If the file has changed, new values are loaded.
        // If the file doesn't exist (e.g., on first run after saveDefaultConfig()),
        // it loads the defaults that were just saved.
        this.reloadConfig();

        getLogger().info("Configuration loaded/reloaded from disk.");

        // Placeholder for logic to apply loaded configuration values.
        // This is where you would typically:
        // - Read values using this.getConfig().getString(), .getInt(), .getBoolean(), etc.
        // - Update internal variables or re-initialize classes that depend on these values.
        // - For example:
        //   String welcomeMessage = this.getConfig().getString("messages.welcome", "Welcome!");
        //   GuiUtils.loadColorsFromConfig(this.getConfig()); // If GuiUtils needs config values
        //   int someSetting = this.getConfig().getInt("some.setting", defaultValue);
        //   this.someInternalComponent.updateSettings(this.getConfig());
        getLogger().info("Applying configuration to plugin components (if any).");
    }
}