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
import org.bukkit.inventory.InventoryHolder; // Added
import org.bukkit.inventory.InventoryView;   // Added
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.scripture.scripture.holder.BookGuiHolder; // Added
import org.scripture.scripture.util.GuiUtils;

import java.util.Map;
// import java.util.UUID; // UUID may no longer be needed

public class BookGuiListener implements Listener {
    private final Scripture plugin; // plugin field is kept for BukkitRunnable

    public BookGuiListener(Scripture plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        // UUID uuid = player.getUniqueId(); // UUID no longer needed for direct tracking

        InventoryView view = e.getView();
        Inventory topInventory = view.getTopInventory();

        // This is the primary check to ensure the player is interacting with our GUI.
        if (!(topInventory.getHolder() instanceof BookGuiHolder)) {
            return;
        }
        // BookGuiHolder bookGuiHolder = (BookGuiHolder) topInventory.getHolder(); // Can be used if holder has specific methods
        Inventory gui = topInventory; // This is our custom GUI.

        ClickType click = e.getClick(); // Get click type early for aggressive check

        // Aggressive check for DOUBLE_CLICK interactions with "shop:coin"
        // This is to prevent any potential duplication or unintended merging of coins
        // when the COIN_SLOT in the GUI is active and contains a "shop:coin".
        // This check is now placed after 'gui' is established as our BookGuiHolder's inventory.
        if (click == ClickType.DOUBLE_CLICK) {
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

        InventoryAction action = e.getAction(); // Get action type
        Inventory clickedInventory = e.getClickedInventory();

        if (clickedInventory == gui) { // Click was directly in our GUI
            int slot = e.getSlot(); // This is the raw slot number in the GUI

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
            // Click was somewhere else (e.g. outside window?), or e.getClickedInventory() is null.
            // If topInventory is our GUI, we might want to cancel such events.
            // However, if e.getClickedInventory() is null, e.getSlot() might be -999 (clicked outside).
            // Bukkit often cancels these by default for custom GUIs if not handled.
            // For safety, if the interaction involves our GUI screen, cancel unknown clicks.
            if (e.getView().getTopInventory().getHolder() instanceof BookGuiHolder) {
                 // This check might be redundant due to the initial topInventory check,
                 // but ensures that if somehow we are here, and the top is our GUI, we cancel.
                if(e.getClickedInventory() == null) { // Specifically if click is outside any inventory area
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        // UUID uuid = player.getUniqueId(); // No longer needed for direct tracking

        // Check if the closed inventory is our custom GUI
        if (!(e.getInventory().getHolder() instanceof BookGuiHolder)) {
            return;
        }
        // BookGuiHolder bookGuiHolder = (BookGuiHolder) e.getInventory().getHolder();
        Inventory gui = e.getInventory(); // This is our custom GUI that was closed

        ItemStack paper = gui.getItem(GuiUtils.PAPER_SLOT);
        if (paper != null && paper.getType() == Material.PAPER) {
            player.getInventory().addItem(paper);
        }
        ItemStack coin = gui.getItem(GuiUtils.COIN_SLOT);
        if (coin != null) {
            // Ensure the coin being returned is a valid "shop:coin"
            // This is a safeguard, as only "shop:coin" should be in COIN_SLOT
            CustomStack customCoin = CustomStack.byItemStack(coin);
            if (customCoin != null && "shop:coin".equals(customCoin.getNamespacedID())) {
                player.getInventory().addItem(coin);
            } else if (customCoin == null) { // If it's not a custom item at all (e.g. vanilla item somehow placed)
                 plugin.getLogger().warning("A non-custom item was found in COIN_SLOT on GUI close for player " + player.getName() + ". Item: " + coin.getType());
                 // Optionally, decide if to return it or not. For now, returning as per original logic.
                 player.getInventory().addItem(coin);
            } else { // It's a custom item, but not "shop:coin"
                 plugin.getLogger().warning("An unexpected custom item '" + customCoin.getNamespacedID() + "' was found in COIN_SLOT on GUI close for player " + player.getName() + ".");
                 // Decide whether to return this or not. For safety, let's not return unexpected custom items.
            }
        }
        // plugin.unregisterPlayer(uuid); // Removed, no longer tracking via plugin map
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