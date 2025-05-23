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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.scripture.scripture.util.GuiUtils;

import java.util.Map;
import java.util.UUID;

public class BookGuiListener implements Listener {
    private final Scripture plugin;

    public BookGuiListener(Scripture plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        UUID uuid = player.getUniqueId();

        if (!plugin.isUsingGui(uuid)) return;
        Inventory gui = plugin.getPlayerGui(uuid);
        if (gui == null) return;

        // Aggressive check for DOUBLE_CLICK interactions with "shop:coin"
        // This is to prevent any potential duplication or unintended merging of coins
        // when the COIN_SLOT in the GUI is active and contains a "shop:coin".
        if (e.getClick() == ClickType.DOUBLE_CLICK) {
            ItemStack guiCoin = gui.getItem(GuiUtils.COIN_SLOT);
            if (guiCoin != null) {
                CustomStack customGuiCoin = CustomStack.byItemStack(guiCoin);
                if (customGuiCoin != null && "shop:coin".equals(customGuiCoin.getNamespacedID())) {
                    // GUI's COIN_SLOT has a "shop:coin". Now check player's items.
                    ItemStack currentItem = e.getCurrentItem();
                    ItemStack cursorItem = e.getCursor();

                    boolean currentIsCoin = false;
                    if (currentItem != null) {
                        CustomStack customCurrent = CustomStack.byItemStack(currentItem);
                        if (customCurrent != null && "shop:coin".equals(customCurrent.getNamespacedID())) {
                            currentIsCoin = true;
                        }
                    }

                    boolean cursorIsCoin = false;
                    if (cursorItem != null) {
                        CustomStack customCursor = CustomStack.byItemStack(cursorItem);
                        if (customCursor != null && "shop:coin".equals(customCursor.getNamespacedID())) {
                            cursorIsCoin = true;
                        }
                    }

                    if (currentIsCoin || cursorIsCoin) {
                        e.setCancelled(true);
                        return; // Immediately stop processing this event
                    }
                }
            }
        }

        int slot = e.getSlot();
        InventoryAction action = e.getAction();
        ClickType click = e.getClick();

        // handle shift-click paper insertion from player inventory
        if (e.getClickedInventory() != gui) {
            if ((action == InventoryAction.MOVE_TO_OTHER_INVENTORY && isPaper(e.getCurrentItem()))
                    || (click == ClickType.DOUBLE_CLICK && isPaper(e.getCurrentItem()))) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        handleCoinInsertion(gui);
                    }
                }.runTaskLater(plugin, 1);
            }
            // Prevent players from pulling items from the protected COIN_SLOT via double-click in their own inventory
            if (click == ClickType.DOUBLE_CLICK &&
                    gui.getItem(GuiUtils.COIN_SLOT) != null &&
                    CustomStack.byItemStack(gui.getItem(GuiUtils.COIN_SLOT)) != null &&
                    "shop:coin".equals(CustomStack.byItemStack(gui.getItem(GuiUtils.COIN_SLOT)).getNamespacedID()) &&
                    e.getCurrentItem() != null &&
                    CustomStack.byItemStack(e.getCurrentItem()) != null &&
                    "shop:coin".equals(CustomStack.byItemStack(e.getCurrentItem()).getNamespacedID())) {
                e.setCancelled(true);
            }
            return;
        }

        // only PAPER_SLOT and COIN_SLOT
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
        } else {
            // COIN_SLOT logic
            e.setCancelled(true); // Cancel event early for COIN_SLOT interactions

            if (click == ClickType.DOUBLE_CLICK) {
                // Already cancelled, but good to have explicit return
                return;
            }

            ItemStack guiCoin = gui.getItem(GuiUtils.COIN_SLOT);
            // If the coin slot is supposed to have a coin (e.g., paper is present)
            // but somehow it's null, consumePaperAndRefill might restock it or confirm it should be empty.
            // The main check for giving a coin will be based on consumePaperAndRefill's success.

            ItemStack cursor = e.getCursor();

            // Block placing other items into the coin slot
            if (cursor != null && cursor.getType() != Material.AIR) {
                CustomStack customCursor = CustomStack.byItemStack(cursor);
                if (customCursor == null || !customCursor.getNamespacedID().equals("shop:coin")) {
                    player.sendMessage(GuiUtils.colorize("&cYou can only interact with coins here."));
                    return;
                }
            }

            // Handle SHIFT-CLICK first as it's a distinct action
            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                giveOneCoin(player, gui); // This will use the updated giveOneCoin with inventory check
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
                            // This case should ideally not happen if consumePaperAndRefill succeeded
                            // and implies an issue with getting the "shop:coin" item itself.
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
                        // but as a fallback:
                        player.sendMessage(GuiUtils.colorize("&cPlease place your current item before taking a coin!"));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!plugin.isUsingGui(uuid)) return;
        Inventory guiRef = plugin.getPlayerGui(uuid);
        if (guiRef == null || e.getInventory() != guiRef) return;

        ItemStack paper = guiRef.getItem(GuiUtils.PAPER_SLOT);
        if (paper != null && paper.getType() == Material.PAPER) {
            player.getInventory().addItem(paper);
        }
        ItemStack coin = guiRef.getItem(GuiUtils.COIN_SLOT);
        if (coin != null) {
            player.getInventory().addItem(coin);
        }

        plugin.unregisterPlayer(uuid);
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