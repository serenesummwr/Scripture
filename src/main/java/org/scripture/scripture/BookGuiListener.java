package org.scripture.scripture;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
// import org.bukkit.inventory.InventoryHolder; // Removed
// import org.bukkit.inventory.InventoryView;   // Removed
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
// import org.scripture.scripture.holder.BookGuiHolder; // Removed
import org.scripture.scripture.util.GuiUtils;

import java.util.Map;
import java.util.UUID; // Restored UUID import

public class BookGuiListener implements Listener {
    private final Scripture plugin;

    public BookGuiListener(Scripture plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        UUID uuid = player.getUniqueId(); // Restored

        if (!plugin.isUsingGui(uuid)) return; // Restored
        Inventory gui = plugin.getPlayerGui(uuid); // Restored
        if (gui == null) return; // Restored

        ClickType click = e.getClick(); // Get click type

        // Aggressive check for DOUBLE_CLICK interactions with "shop:coin"
        // This is to prevent any potential duplication or unintended merging of coins
        // when the COIN_SLOT in the GUI is active and contains a "shop:coin".
        // This check uses the 'gui' variable obtained from plugin.getPlayerGui(uuid).
        if (click == ClickType.DOUBLE_CLICK) { // This check was well-placed and is preserved
            ItemStack guiCoin = gui.getItem(GuiUtils.COIN_SLOT);
            if (guiCoin != null) {
                CustomStack customGuiCoin = CustomStack.byItemStack(guiCoin);
                if (customGuiCoin != null && "shop:coin".equals(customGuiCoin.getNamespacedID())) {
                    ItemStack currentItem = e.getCurrentItem(); // Item clicked in player inv or GUI
                    ItemStack cursorItem = e.getCursor();       // Item on cursor

                    boolean currentIsCoin = false;
                    if (currentItem != null && currentItem.getType() != Material.AIR) {
                        CustomStack customCurrent = CustomStack.byItemStack(currentItem);
                        if (customCurrent != null && "shop:coin".equals(customCurrent.getNamespacedID())) {
                            currentIsCoin = true;
                        }
                    }

                    boolean cursorIsCoin = false;
                    if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                        CustomStack customCursor = CustomStack.byItemStack(cursorItem);
                        if (customCursor != null && "shop:coin".equals(customCursor.getNamespacedID())) {
                            cursorIsCoin = true;
                        }
                    }

                    // If GUI has coin, and (clicked item is coin OR cursor has coin), cancel.
                    // This covers double-clicking a coin in player inventory to merge with GUI coin slot,
                    // or double-clicking the GUI coin slot itself.
                    if (currentIsCoin || cursorIsCoin) {
                        e.setCancelled(true);
                        return; // Immediately stop processing this event
                    }
                }
            }
        }

        InventoryAction action = e.getAction();
        Inventory clickedInventory = e.getClickedInventory(); // Keep this

        // The main logic structure for differentiating player inventory clicks vs GUI clicks.
        // This uses the 'gui' from plugin.getPlayerGui(uuid).
        if (clickedInventory != gui) { // Logic for clicks in player's inventory
            // This is the 'else if (clickedInventory == view.getBottomInventory())' block from previous version, adapted.
            // It handles shift-click paper insertion from player inventory.
            if ((action == InventoryAction.MOVE_TO_OTHER_INVENTORY && isPaper(e.getCurrentItem()))
                    || (click == ClickType.DOUBLE_CLICK && isPaper(e.getCurrentItem()))) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        handleCoinInsertion(gui); // 'gui' is the one from plugin.getPlayerGui
                    }
                }.runTaskLater(plugin, 1);
            }
            // The specific double-click coin protection that was inside 'e.getClickedInventory() != gui'
            // (the one added in fix/gui-double-click-exploit, not the aggressive one)
            // should be preserved if it was intended to be separate from the aggressive check.
            // Based on previous fixes, the aggressive check at the top is the primary one.
            // The original fix had a check here:
            // if (click == ClickType.DOUBLE_CLICK && gui.getItem(GuiUtils.COIN_SLOT) != null ...etc)
            // This is now covered by the aggressive check at the top, which is more comprehensive.
            // So, we don't need to re-add that specific one here if the aggressive one is solid.
            // The prompt mentioned "double-click protections (both the aggressive one at the top and any slot-specific ones)"
            // The aggressive one is kept. Slot-specific for COIN_SLOT double_click is also kept inside the GUI click logic.
            return; // If click is not in our GUI, and not a paper insertion, do nothing more.
        }

        // Click was in our GUI (clickedInventory == gui)
        int slot = e.getRawSlot(); // Use getRawSlot for GUI interactions if needed, or getSlot if top inventory. e.getSlot() is fine.

        // only PAPER_SLOT and COIN_SLOT interactions allowed in the GUI
        if (slot != GuiUtils.PAPER_SLOT && slot != GuiUtils.COIN_SLOT) {
            e.setCancelled(true);
            return;
        }

        if (slot == GuiUtils.PAPER_SLOT) {
                // placing paper
                if (e.getCursor() != null && isPaper(e.getCursor())) {
                    schedule(() -> handleCoinInsertion(gui), 1);
                }
                // removing paper
                else if (isPaper(e.getCurrentItem())) {
                    schedule(() -> {
                        if (gui.getItem(GuiUtils.PAPER_SLOT) == null) {
                            gui.clear(GuiUtils.COIN_SLOT);
                        }
                    }, 1);
                }
            } else { // COIN_SLOT logic (slot == GuiUtils.COIN_SLOT)
                e.setCancelled(true); // Cancel event early for all COIN_SLOT interactions

                // The aggressive double-click check at the top should have already handled
                // any problematic double-clicks. So, if click is DOUBLE_CLICK here,
                // it means it wasn't a coin-related one caught by the aggressive check,
                // but we still cancel COIN_SLOT double-clicks as per original logic.
                if (click == ClickType.DOUBLE_CLICK) {
                    return;
                }

                // ItemStack guiCoin = gui.getItem(GuiUtils.COIN_SLOT); // Already fetched by aggressive check, but can re-fetch for clarity
                ItemStack cursor = e.getCursor();

                // Block placing other items into the coin slot (already handled by e.setCancelled(true) for most cases)
                // This check is more specific for cursor interactions if any were to bypass the generic cancel.
                if (cursor != null && cursor.getType() != Material.AIR) {
                    CustomStack customCursor = CustomStack.byItemStack(cursor);
                    if (customCursor == null || !customCursor.getNamespacedID().equals("shop:coin")) {
                        player.sendMessage(GuiUtils.colorize("&cYou can only interact with coins here."));
                        return;
                    }
                }

                // Handle SHIFT-CLICK first as it's a distinct action for taking coins
                if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                    giveOneCoin(player, gui);
                    return;
                }

                // Handle LEFT and RIGHT clicks for taking coins
                if (click == ClickType.LEFT || click == ClickType.RIGHT) {
                    if (cursor == null || cursor.getType() == Material.AIR) {
                        // Player's cursor is empty, try to take one coin
                        if (consumePaperAndRefill(gui)) {
                            CustomStack csCoin = CustomStack.getInstance("shop:coin");
                            if (csCoin != null) {
                                ItemStack singleCoin = csCoin.getItemStack().clone();
                                singleCoin.setAmount(1);
                                player.setItemOnCursor(singleCoin);
                            } else {
                                plugin.getLogger().warning("Failed to get CustomStack for shop:coin even after successful paper consumption.");
                            }
                        } else {
                            player.sendMessage(GuiUtils.colorize("&cNot enough paper!"));
                        }
                    } else {
                        // Player's cursor has a "shop:coin", try to stack
                        CustomStack customCursor = CustomStack.byItemStack(cursor);
                        if (customCursor != null && customCursor.getNamespacedID().equals("shop:coin")) {
                            if (cursor.getAmount() < cursor.getMaxStackSize()) {
                                if (consumePaperAndRefill(gui)) {
                                    cursor.setAmount(cursor.getAmount() + 1);
                                } else {
                                    player.sendMessage(GuiUtils.colorize("&cNot enough paper for another coin!"));
                                }
                            } else {
                                player.sendMessage(GuiUtils.colorize("&cYour coin stack is already full!"));
                            }
                        } else {
                             // This case should be blocked by the check at the beginning of COIN_SLOT logic
                             // or by the general e.setCancelled(true) for the COIN_SLOT.
                            player.sendMessage(GuiUtils.colorize("&cPlease place your current item before taking a coin!"));
                        }
                    }
                }
            }
        } else if (clickedInventory == view.getBottomInventory()) { // Click was in player's inventory
            // handle shift-click paper insertion from player inventory
            if ((action == InventoryAction.MOVE_TO_OTHER_INVENTORY && isPaper(e.getCurrentItem()))
                    || (click == ClickType.DOUBLE_CLICK && isPaper(e.getCurrentItem()))) { // Double-click paper in player inv
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        handleCoinInsertion(gui); // gui here is topInventory (our custom GUI)
                    }
                }.runTaskLater(plugin, 1);
            }
            // The redundant double-click coin protection that was here previously is removed
            // as the aggressive check at the top of the method now handles all such cases.
        } else {
            // Clicks outside the player inventory or our GUI are not handled further up,
            // so no explicit 'else' block for e.getClickedInventory() == null is strictly needed here,
            // as the initial checks for 'gui' handle if the player is meant to be interacting with it.
            // If e.getClickedInventory() is null, it means the click was outside any inventory,
            // and the earlier checks (isUsingGui) would determine if any action is needed.
            // The original code didn't have a specific block for `e.getClickedInventory() == null`
            // after the `if (e.getClickedInventory() != gui)` block.
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        UUID uuid = player.getUniqueId(); // Restored

        if (!plugin.isUsingGui(uuid)) return; // Restored

        // Check if the closed inventory is the one we are tracking for the player
        if (e.getInventory() != plugin.getPlayerGui(uuid)) { // Restored check
            return;
        }

        Inventory closedGui = e.getInventory(); // This is our custom GUI that was closed

        ItemStack paper = closedGui.getItem(GuiUtils.PAPER_SLOT);
        if (paper != null && paper.getType() == Material.PAPER) {
            player.getInventory().addItem(paper);
        }
        ItemStack coin = closedGui.getItem(GuiUtils.COIN_SLOT);
        if (coin != null) {
            // The existing detailed check for the coin item is good and preserved.
            CustomStack customCoin = CustomStack.byItemStack(coin);
            if (customCoin != null && "shop:coin".equals(customCoin.getNamespacedID())) {
                player.getInventory().addItem(coin);
            } else if (customCoin == null) {
                 plugin.getLogger().warning("A non-custom item was found in COIN_SLOT on GUI close for player " + player.getName() + ". Item: " + coin.getType());
                 player.getInventory().addItem(coin);
            } else {
                 plugin.getLogger().warning("An unexpected custom item '" + customCoin.getNamespacedID() + "' was found in COIN_SLOT on GUI close for player " + player.getName() + ".");
                 // Not returning unexpected custom items.
            }
        }
        plugin.unregisterPlayer(uuid); // Restored
    }

    private void handleCoinInsertion(Inventory gui) {
        CustomStack cs = CustomStack.getInstance("shop:coin");
        if (cs != null) {
            gui.setItem(GuiUtils.COIN_SLOT, cs.getItemStack());
        }
    }

    private void giveOneCoin(Player player, Inventory gui) {
        // First check if we have paper to consume
        if (!hasPaperToConsume(gui)) {
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
        if (!hasInventorySpace(player, coinToGive)) {
            player.sendMessage(GuiUtils.colorize("&cInventory is full!"));
            return; // Don't consume paper if inventory is full
        }

        // Now it's safe to consume paper and give coin
        if (consumePaperAndRefill(gui)) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(coinToGive);
            if (!leftover.isEmpty()) {
                // This should not happen since we checked space above, but as a failsafe
                player.sendMessage(GuiUtils.colorize("&cUnexpected inventory issue!"));
                plugin.getLogger().warning("Inventory check passed but addItem failed in giveOneCoin for " + player.getName());
            }
        } else {
            // This should not happen since we checked hasPaperToConsume above
            player.sendMessage(GuiUtils.colorize("&cUnexpected paper consumption issue!"));
            plugin.getLogger().warning("hasPaperToConsume passed but consumePaperAndRefill failed for " + player.getName());
        }
    }

    private boolean hasPaperToConsume(Inventory gui) {
        ItemStack paper = gui.getItem(GuiUtils.PAPER_SLOT);
        return paper != null && paper.getType() == Material.PAPER && paper.getAmount() > 0;
    }

    private boolean hasInventorySpace(Player player, ItemStack item) {
        // Create a copy of the item to test
        ItemStack testItem = item.clone();

        // Try to add the item to a simulated inventory state
        // Note: This addItem can momentarily change player inventory if it fits, then we remove.
        // A more robust check might involve cloning the inventory or checking slot by slot.
        // However, this addItem/removeItem pattern is common.
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(testItem);

        boolean hasSpace = leftover.isEmpty();

        if (hasSpace) {
            // If it fit, we need to remove the temporary item.
            // This assumes addItem doesn't modify testItem if it doesn't fit.
            player.getInventory().removeItem(testItem);
        } // If it didn't fit, leftover contains the item, and it wasn't fully added.

        return hasSpace;
    }

    private boolean consumePaperAndRefill(Inventory gui) {
        ItemStack paper = gui.getItem(GuiUtils.PAPER_SLOT);
        if (paper != null && paper.getType() == Material.PAPER && paper.getAmount() > 0) {
            // Paper was consumed
            if (paper.getAmount() > 1) {
                paper.setAmount(paper.getAmount() - 1);
                handleCoinInsertion(gui); // Refills coin slot
            } else { // Last piece of paper
                gui.clear(GuiUtils.PAPER_SLOT);
                gui.clear(GuiUtils.COIN_SLOT); // Clears coin slot as last paper is used
            }
            return true; // Paper was consumed
        } else {
            // No paper, or paper item is somehow invalid
            gui.clear(GuiUtils.COIN_SLOT); // Ensure coin slot is clear if no paper
            return false; // No paper was consumed
        }
    }

    private boolean isPaper(ItemStack item) {
        return item != null && item.getType() == Material.PAPER;
    }

    private void schedule(Runnable task, int delay) {
        new BukkitRunnable() {
            @Override public void run() { task.run(); }
        }.runTaskLater(plugin, delay);
    }
}
    // Note: The original logic for COIN_SLOT had e.setCancelled(true) at the very start of its block.
    // This is preserved. The aggressive double-click check is an additional layer before specific slot logic.
    // The specific `if (click == ClickType.DOUBLE_CLICK) { return; }` within COIN_SLOT logic block
    // is also kept as it might handle non-coin double clicks if any could occur.
}
        CustomStack cs = CustomStack.getInstance("shop:coin");
        if (cs != null) {
            gui.setItem(GuiUtils.COIN_SLOT, cs.getItemStack());
        }
    }

    private void giveOneCoin(Player player, Inventory gui) {
        // First check if we have paper to consume
        if (!hasPaperToConsume(gui)) {
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
        if (!hasInventorySpace(player, coinToGive)) {
            player.sendMessage(GuiUtils.colorize("&cInventory is full!"));
            return; // Don't consume paper if inventory is full
        }

        // Now it's safe to consume paper and give coin
        if (consumePaperAndRefill(gui)) {
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

    private boolean hasPaperToConsume(Inventory gui) {
        ItemStack paper = gui.getItem(GuiUtils.PAPER_SLOT);
        return paper != null && paper.getType() == Material.PAPER && paper.getAmount() > 0;
    }

    private boolean hasInventorySpace(Player player, ItemStack item) {
        // Create a copy of the item to test
        ItemStack testItem = item.clone();

        // Try to add the item to a simulated inventory state
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(testItem);

        // If there's leftover, inventory is full
        boolean hasSpace = leftover.isEmpty();

        // Remove the test item we just added (if any was added)
        if (hasSpace) {
            player.getInventory().removeItem(testItem);
        }

        return hasSpace;
    }

    private boolean consumePaperAndRefill(Inventory gui) {
        ItemStack paper = gui.getItem(GuiUtils.PAPER_SLOT);
        if (paper != null && paper.getType() == Material.PAPER && paper.getAmount() > 0) {
            // Paper was consumed
            if (paper.getAmount() > 1) {
                paper.setAmount(paper.getAmount() - 1);
                handleCoinInsertion(gui); // Refills coin slot
            } else { // Last piece of paper
                gui.clear(GuiUtils.PAPER_SLOT);
                gui.clear(GuiUtils.COIN_SLOT); // Clears coin slot as last paper is used
            }
            return true; // Paper was consumed
        } else {
            // No paper, or paper item is somehow invalid
            gui.clear(GuiUtils.COIN_SLOT); // Ensure coin slot is clear if no paper
            return false; // No paper was consumed
        }
    }

    private boolean isPaper(ItemStack item) {
        return item != null && item.getType() == Material.PAPER;
    }

    private void schedule(Runnable task, int delay) {
        new BukkitRunnable() {
            @Override public void run() { task.run(); }
        }.runTaskLater(plugin, delay);
    }
}