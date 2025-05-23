package org.scripture.scripture.gui;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.scripture.scripture.Scripture;
import org.scripture.scripture.util.GuiUtils;

import java.util.Map;

/**
 * Custom InventoryHolder for the Paper-to-Coin conversion GUI
 */
public class PaperToCoinHolder implements InventoryHolder {
    private final Scripture plugin;
    private final Player player;
    private final Inventory inventory;

    // Slot constants
    public static final int PAPER_SLOT = 2;
    public static final int COIN_SLOT = 29;
    private static final int GUI_SIZE = 45;
    private static final String GUI_TITLE = "";

    public PaperToCoinHolder(@NotNull Scripture plugin, @NotNull Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, net.kyori.adventure.text.Component.text(GUI_TITLE));
        setupGui();
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }



    /**
     * Sets up the initial GUI layout
     */
    private void setupGui() {
        inventory.clear(PAPER_SLOT);
        inventory.clear(COIN_SLOT);
    }

    /**
     * Handles coin insertion when paper is placed
     */
    public void handleCoinInsertion() {
        CustomStack cs = CustomStack.getInstance("shop:coin");
        if (cs != null) {
            inventory.setItem(COIN_SLOT, cs.getItemStack());
        }
    }

    /**
     * Gives one coin to the player if they have paper to consume
     */
    public void giveOneCoin() {
        // First check if we have paper to consume
        if (!hasPaperToConsume()) {
            player.sendMessage(GuiUtils.colorize("&cNot enough paper to get a coin!"));
            return;
        }

        // Get the coin item that we would give
        CustomStack cs = CustomStack.getInstance("shop:coin");
        if (cs == null) {
            plugin.getLogger().warning("Failed to get CustomStack for shop:coin in giveOneCoin.");
            return;
        }

        ItemStack coinToGive = cs.getItemStack().clone();
        coinToGive.setAmount(1);

        // Check if inventory has space BEFORE consuming paper
        if (!hasInventorySpace(coinToGive)) {
            player.sendMessage(GuiUtils.colorize("&cInventory is full!"));
            return;
        }

        // Now it's safe to consume paper and give coin
        if (consumePaperAndRefill()) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(coinToGive);
            if (!leftover.isEmpty()) {
                // This should not happen since we checked space above, but as a failsafe
                player.sendMessage(GuiUtils.colorize("&cUnexpected inventory issue!"));
                plugin.getLogger().warning("Inventory check passed but addItem failed in giveOneCoin");
            }
        } else {
            // This should not happen since we checked hasPaperToConsume above
            player.sendMessage(GuiUtils.colorize("&cUnexpected paper consumption issue!"));
            plugin.getLogger().warning("hasPaperToConsume passed but consumePaperAndRefill failed");
        }
    }

    /**
     * Consumes one paper and handles coin slot refilling
     */
    public boolean consumePaperAndRefill() {
        ItemStack paper = inventory.getItem(PAPER_SLOT);
        if (paper != null && paper.getType() == Material.PAPER && paper.getAmount() > 0) {
            if (paper.getAmount() > 1) {
                paper.setAmount(paper.getAmount() - 1);
                handleCoinInsertion(); // Refills coin slot
            } else {
                // Last piece of paper
                inventory.clear(PAPER_SLOT);
                inventory.clear(COIN_SLOT); // Clear coin slot as last paper is used
            }
            return true;
        } else {
            // No paper, or paper item is somehow invalid
            inventory.clear(COIN_SLOT); // Ensure coin slot is clear if no paper
            return false;
        }
    }

    /**
     * Checks if there's paper available to consume
     */
    public boolean hasPaperToConsume() {
        ItemStack paper = inventory.getItem(PAPER_SLOT);
        return paper != null && paper.getType() == Material.PAPER && paper.getAmount() > 0;
    }

    /**
     * Checks if the player's inventory has space for the given item
     */
    private boolean hasInventorySpace(ItemStack item) {
        ItemStack testItem = item.clone();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(testItem);
        boolean hasSpace = leftover.isEmpty();

        // Remove the test item we just added (if any was added)
        if (hasSpace) {
            player.getInventory().removeItem(testItem);
        }

        return hasSpace;
    }

    /**
     * Handles GUI cleanup when closed
     */
    public void handleClose() {
        // Return paper to player
        ItemStack paper = inventory.getItem(PAPER_SLOT);
        if (paper != null && paper.getType() == Material.PAPER) {
            player.getInventory().addItem(paper);
        }

        // Return coins to player
        ItemStack coin = inventory.getItem(COIN_SLOT);
        if (coin != null) {
            player.getInventory().addItem(coin);
        }
    }

    /**
     * Checks if an item is paper
     */
    public static boolean isPaper(ItemStack item) {
        return item != null && item.getType() == Material.PAPER;
    }

    /**
     * Opens this GUI for the player
     */
    public void open() {
        player.openInventory(inventory);
    }
}