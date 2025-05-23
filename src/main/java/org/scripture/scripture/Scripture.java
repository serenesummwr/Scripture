package org.scripture.scripture;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.Inventory; // Restored
import org.scripture.scripture.util.GuiUtils;

import java.util.Map;                // Restored
import java.util.HashMap;            // Restored
import java.util.Set;                // Restored
import java.util.HashSet;            // Restored
import java.util.Collections;        // Restored
import java.util.UUID;               // Restored

public final class Scripture extends JavaPlugin {
    private final Map<UUID, Inventory> playerGui = new HashMap<>(); // Restored
    private final Set<UUID> using = new HashSet<>(); // Restored

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
            pluginCommand.setDescription("Open the Paper to Book GUI");
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
        // Clean up resources
        playerGui.clear(); // Restored
        using.clear(); // Restored

        getLogger().info("Scripture plugin has been disabled.");
    }

    /**
     * Registers a player's GUI in the tracking system
     *
     * @param uuid The player's UUID
     * @param inventory The inventory to register
     */
    public void registerPlayerGui(UUID uuid, Inventory inventory) {
        playerGui.put(uuid, inventory);
        using.add(uuid);
    }

    /**
     * Unregisters a player from the GUI tracking system
     *
     * @param uuid The player's UUID
     */
    public void unregisterPlayer(UUID uuid) {
        playerGui.remove(uuid);
        using.remove(uuid);
    }

    /**
     * Checks if a player is using the GUI
     *
     * @param uuid The player's UUID
     * @return True if the player is using the GUI
     */
    public boolean isUsingGui(UUID uuid) {
        return using.contains(uuid);
    }

    /**
     * Gets the GUI for a player
     *
     * @param uuid The player's UUID
     * @return The player's GUI, or null if not found
     */
    public Inventory getPlayerGui(UUID uuid) {
        return playerGui.get(uuid);
    }

    // Legacy methods for backward compatibility
    @Deprecated
    public Map<UUID, Inventory> getPlayerGui() { return Collections.unmodifiableMap(playerGui); }
    @Deprecated
    public Set<UUID> getUsing() { return Collections.unmodifiableSet(using); }
}