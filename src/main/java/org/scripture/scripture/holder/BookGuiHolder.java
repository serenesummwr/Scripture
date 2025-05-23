package org.scripture.scripture.holder;

import dev.papermc.paper.inventory.CustomInventoryHolder;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class BookGuiHolder implements CustomInventoryHolder {

    private final Inventory inventory;

    public BookGuiHolder(int size, String title) {
        // Ensure this is the owner when creating the inventory
        this.inventory = Bukkit.createInventory(this, size, title);
        // Note: GUI population (glass panes, clearing slots) will be handled separately,
        // typically after creation or in the method that opens the GUI.
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    // Additional methods can be added here if the holder needs to manage specific state
    // or provide utility related to the GUI it holds. For now, it's minimal.
}
