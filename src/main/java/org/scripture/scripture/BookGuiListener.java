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

            if (click == ClickType.DOUBLE_CLICK) {
                e.setCancelled(true);
                return;
            }

            ItemStack guiCoin = gui.getItem(GuiUtils.COIN_SLOT);
            if (guiCoin == null) {
                e.setCancelled(true);
                return;
            }

            ItemStack cursor = e.getCursor();
            // merge same coins
            if (cursor != null && cursor.getType() != Material.AIR) {
                CustomStack cs = CustomStack.byItemStack(cursor);
                if (cs != null && cs.getNamespacedID().equals("shop:coin")) {
                    e.setCancelled(true);
                    int current = cursor.getAmount();
                    if (current < cursor.getMaxStackSize()) {
                        cursor.setAmount(current + 1);
                        schedule(() -> consumePaperAndRefill(gui), 1);
                    } else {
                        player.sendMessage(GuiUtils.colorize("&cYour coin stack is already full!"));
                    }
                    return;
                }
            }

            // block other items
            if (cursor != null && cursor.getType() != Material.AIR) {
                e.setCancelled(true);
                player.sendMessage(GuiUtils.colorize("&cPlease place your current item before taking a coin!"));
                return;
            }

            // SHIFT-CLICK
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
                giveOneCoin(player, gui);
            }
            // NORMAL CLICK
            else if (action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_ONE) {
                e.setCancelled(true);
                CustomStack cs = CustomStack.getInstance("shop:coin");
                if (cs != null) {
                    ItemStack single = cs.getItemStack().clone();
                    single.setAmount(1);
                    player.setItemOnCursor(single);
                    schedule(() -> consumePaperAndRefill(gui), 1);
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
        ItemStack coin = gui.getItem(GuiUtils.COIN_SLOT).clone();
        coin.setAmount(1);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(coin);
        if (!leftover.isEmpty()) {
            player.sendMessage(GuiUtils.colorize("&cInventory is full!"));
        } else {
            consumePaperAndRefill(gui);
        }
    }

    private void consumePaperAndRefill(Inventory gui) {
        ItemStack paper = gui.getItem(GuiUtils.PAPER_SLOT);
        if (paper != null && paper.getType() == Material.PAPER) {
            if (paper.getAmount() > 1) {
                paper.setAmount(paper.getAmount() - 1);
                handleCoinInsertion(gui);
            } else {
                gui.clear(GuiUtils.PAPER_SLOT);
                gui.clear(GuiUtils.COIN_SLOT);
            }
        } else {
            gui.clear(GuiUtils.COIN_SLOT);
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