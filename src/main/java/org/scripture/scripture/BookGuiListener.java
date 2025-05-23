package org.scripture.scripture;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.scripture.scripture.gui.PaperToCoinHolder;
import org.scripture.scripture.util.GuiUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BookGuiListener implements Listener {
    private final Scripture plugin;
    // Track which players have textured GUIs open
    private final Map<UUID, PaperToCoinHolder> activeTexturedGuis = new HashMap<>();

    public BookGuiListener(Scripture plugin) {
        this.plugin = plugin;
    }

    /**
     * Register a textured GUI for a player
     */
    public void registerTexturedGui(Player player, PaperToCoinHolder holder) {
        activeTexturedGuis.put(player.getUniqueId(), holder);
    }

    /**
     * Unregister a textured GUI for a player
     */
    public void unregisterTexturedGui(Player player) {
        activeTexturedGuis.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // Check if this is our custom GUI
        PaperToCoinHolder holder = getHolderFromEvent(e);
        if (holder == null) {
            return;
        }

        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Aggressive check for DOUBLE_CLICK interactions with "shop:coin"
        if (e.getClick() == ClickType.DOUBLE_CLICK) {
            ItemStack guiCoin = holder.getInventory().getItem(PaperToCoinHolder.COIN_SLOT);
            if (guiCoin != null) {
                CustomStack customGuiCoin = CustomStack.byItemStack(guiCoin);
                if (customGuiCoin != null && "shop:coin".equals(customGuiCoin.getNamespacedID())) {
                    // GUI's COIN_SLOT has a "shop:coin". Now check player's items.
                    ItemStack currentItem = e.getCurrentItem();
                    ItemStack cursorItem = e.getCursor();

                    boolean currentIsCoin = isShopCoin(currentItem);
                    boolean cursorIsCoin = isShopCoin(cursorItem);

                    if (currentIsCoin || cursorIsCoin) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }

        int slot = e.getSlot();
        InventoryAction action = e.getAction();
        ClickType click = e.getClick();

        // Handle shift-click paper insertion from player inventory
        if (e.getClickedInventory() != holder.getInventory()) {
            if ((action == InventoryAction.MOVE_TO_OTHER_INVENTORY && PaperToCoinHolder.isPaper(e.getCurrentItem()))
                    || (click == ClickType.DOUBLE_CLICK && PaperToCoinHolder.isPaper(e.getCurrentItem()))) {
                scheduleTask(() -> holder.handleCoinInsertion());
            }

            // Prevent players from pulling items from the protected COIN_SLOT via double-click
            if (click == ClickType.DOUBLE_CLICK &&
                    isShopCoin(holder.getInventory().getItem(PaperToCoinHolder.COIN_SLOT)) &&
                    isShopCoin(e.getCurrentItem())) {
                e.setCancelled(true);
            }
            return;
        }

        // Only allow interaction with PAPER_SLOT and COIN_SLOT
        if (slot != PaperToCoinHolder.PAPER_SLOT && slot != PaperToCoinHolder.COIN_SLOT) {
            e.setCancelled(true);
            return;
        }

        if (slot == PaperToCoinHolder.PAPER_SLOT) {
            handlePaperSlotClick(e, holder);
        } else {
            handleCoinSlotClick(e, holder, player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        PaperToCoinHolder holder = getHolderFromCloseEvent(e);
        if (holder == null) {
            return;
        }

        if (!(e.getPlayer() instanceof Player)) {
            return;
        }

        holder.handleClose();
    }

    /**
     * Gets the PaperToCoinHolder from an InventoryClickEvent
     */
    private PaperToCoinHolder getHolderFromEvent(InventoryClickEvent e) {
        // First, check if it's directly our holder
        if (e.getInventory().getHolder() instanceof PaperToCoinHolder) {
            return (PaperToCoinHolder) e.getInventory().getHolder();
        }

        // Check if it's a TexturedInventoryWrapper
        if (e.getInventory().getHolder() instanceof TexturedInventoryWrapper) {
            // Look up the holder from our registry
            if (e.getWhoClicked() instanceof Player player) {
                return activeTexturedGuis.get(player.getUniqueId());
            }
        }

        return null;
    }

    /**
     * Gets the PaperToCoinHolder from an InventoryCloseEvent
     */
    private PaperToCoinHolder getHolderFromCloseEvent(InventoryCloseEvent e) {
        // Similar logic to getHolderFromEvent
        if (e.getInventory().getHolder() instanceof PaperToCoinHolder) {
            return (PaperToCoinHolder) e.getInventory().getHolder();
        }

        // Handle TexturedInventoryWrapper case
        if (e.getInventory().getHolder() instanceof TexturedInventoryWrapper) {
            if (e.getPlayer() instanceof Player player) {
                return activeTexturedGuis.get(player.getUniqueId());
            }
        }

        return null;
    }

    private void handlePaperSlotClick(InventoryClickEvent e, PaperToCoinHolder holder) {
        // Placing paper
        if (PaperToCoinHolder.isPaper(e.getCursor())) {
            scheduleTask(holder::handleCoinInsertion);
        }
        // Removing paper
        else if (PaperToCoinHolder.isPaper(e.getCurrentItem())) {
            scheduleTask(() -> {
                if (holder.getInventory().getItem(PaperToCoinHolder.PAPER_SLOT) == null) {
                    holder.getInventory().clear(PaperToCoinHolder.COIN_SLOT);
                }
            });
        }
    }

    private void handleCoinSlotClick(InventoryClickEvent e, PaperToCoinHolder holder, Player player) {
        // Cancel event early for COIN_SLOT interactions
        e.setCancelled(true);

        if (e.getClick() == ClickType.DOUBLE_CLICK) {
            return;
        }

        ItemStack cursor = e.getCursor();

        // Block placing other items into the coin slot
        if (cursor.getType() != Material.AIR) {
            if (!isShopCoin(cursor)) {
                player.sendMessage(GuiUtils.colorize("&cYou can only interact with coins here."));
                return;
            }
        }

        // Handle SHIFT-CLICK
        if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT) {
            holder.giveOneCoin();
            return;
        }

        // Handle LEFT and RIGHT clicks for taking coins
        if (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.RIGHT) {
            if (cursor.getType() == Material.AIR) {
                // Player's cursor is empty, try to take one coin
                if (holder.consumePaperAndRefill()) {
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
            } else if (isShopCoin(cursor)) {
                // Player's cursor has a "shop:coin", try to stack
                if (cursor.getAmount() < cursor.getMaxStackSize()) {
                    if (holder.consumePaperAndRefill()) {
                        cursor.setAmount(cursor.getAmount() + 1);
                    } else {
                        player.sendMessage(GuiUtils.colorize("&cNot enough paper for another coin!"));
                    }
                } else {
                    player.sendMessage(GuiUtils.colorize("&cYour coin stack is already full!"));
                }
            } else {
                player.sendMessage(GuiUtils.colorize("&cPlease place your current item before taking a coin!"));
            }
        }
    }

    /**
     * Checks if an ItemStack is a shop:coin
     */
    private boolean isShopCoin(ItemStack item) {
        if (item == null) return false;
        CustomStack custom = CustomStack.byItemStack(item);
        return custom != null && "shop:coin".equals(custom.getNamespacedID());
    }

    /**
     * Schedules a task to run one tick later
     */
    private void scheduleTask(Runnable task) {
        new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTaskLater(plugin, 1);
    }
}
