package kr.clutch.gacha.gui;

import kr.clutch.gacha.config.GachaConfig;
import kr.clutch.gacha.model.GachaReward;
import kr.clutch.gacha.service.GachaService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class GachaGui implements Listener {
    private static final int GUI_SIZE = 27;
    private static final int REWARDS_PER_PAGE = 9;
    private static final int PREVIOUS_SLOT = 18;
    private static final int NEXT_SLOT = 26;

    private GachaConfig config;

    public GachaGui(JavaPlugin plugin, GachaConfig config) {
        this.config = config;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void updateConfig(GachaConfig config) {
        this.config = config;
    }

    public void open(Player player) {
        open(player, 0);
    }

    private void open(Player player, int page) {
        List<GachaReward> rewards = config.rewards();
        int maxPage = Math.max(0, (rewards.size() - 1) / REWARDS_PER_PAGE);
        int currentPage = Math.max(0, Math.min(page, maxPage));
        Holder holder = new Holder(currentPage);
        Inventory inventory = Bukkit.createInventory(holder, GUI_SIZE, config.guiTitle());
        holder.inventory = inventory;
        fillBorders(inventory);

        int start = currentPage * REWARDS_PER_PAGE;
        int end = Math.min(start + REWARDS_PER_PAGE, rewards.size());
        for (int index = start; index < end; index++) {
            inventory.setItem(9 + (index - start), createIcon(rewards.get(index)));
        }
        if (currentPage > 0) {
            inventory.setItem(PREVIOUS_SLOT, navigationIcon("§e이전 페이지"));
        }
        if (currentPage < maxPage) {
            inventory.setItem(NEXT_SLOT, navigationIcon("§e다음 페이지"));
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isGachaGui(event.getView().getTitle(), event.getInventory().getHolder())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryHolder inventoryHolder = event.getInventory().getHolder();
        if (!(inventoryHolder instanceof Holder holder)) {
            return;
        }
        if (event.getRawSlot() == PREVIOUS_SLOT) {
            open(player, holder.page - 1);
            return;
        }
        if (event.getRawSlot() == NEXT_SLOT) {
            open(player, holder.page + 1);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isGachaGui(event.getView().getTitle(), event.getInventory().getHolder())) {
            event.setCancelled(true);
        }
    }

    private boolean isGachaGui(String title, InventoryHolder holder) {
        return holder instanceof Holder || holder instanceof GachaService.RouletteHolder || title.equals(config.guiTitle()) || title.equals("§8CLUTCH §7가챠");
    }

    private void fillBorders(Inventory inventory) {
        ItemStack pane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        for (int slot = 0; slot <= 8; slot++) {
            inventory.setItem(slot, pane);
        }
        for (int slot = 18; slot <= 26; slot++) {
            inventory.setItem(slot, pane);
        }
    }

    private ItemStack navigationIcon(String name) {
        ItemStack itemStack = new ItemStack(Material.ARROW);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(name);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private ItemStack createIcon(GachaReward reward) {
        ItemStack itemStack = reward.itemStack() == null ? new ItemStack(reward.material(), Math.max(1, reward.itemAmount())) : reward.itemStack().clone();
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(reward.displayName());
        List<String> lore = new ArrayList<>();
        lore.add("§7등급: §f" + reward.grade());
        lore.add("§7weight: §f" + reward.weight());
        lore.add("§7설명: §f가챠권 우클릭 시 획득 가능한 보상입니다.");
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private static final class Holder implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        private Holder(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
