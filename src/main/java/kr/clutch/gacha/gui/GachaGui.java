package kr.clutch.gacha.gui;

import kr.clutch.gacha.config.GachaConfig;
import kr.clutch.gacha.model.GachaBox;
import kr.clutch.gacha.model.GachaReward;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GachaGui implements Listener {
    private static final int GUI_SIZE = 27;
    private static final int BOXES_PER_PAGE = 9;
    private static final int PREVIOUS_SLOT = 18;
    private static final int NEXT_SLOT = 26;

    private final RewardListGui rewardListGui = new RewardListGui();
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
        List<GachaBox> boxes = new ArrayList<>(config.boxes().values());
        int maxPage = Math.max(0, (boxes.size() - 1) / BOXES_PER_PAGE);
        int currentPage = Math.max(0, Math.min(page, maxPage));
        Holder holder = new Holder(currentPage);
        Inventory inventory = Bukkit.createInventory(holder, GUI_SIZE, config.guiTitle());
        holder.inventory = inventory;
        fillBorders(inventory);

        int start = currentPage * BOXES_PER_PAGE;
        int end = Math.min(start + BOXES_PER_PAGE, boxes.size());
        for (int index = start; index < end; index++) {
            int slot = 9 + (index - start);
            GachaBox box = boxes.get(index);
            inventory.setItem(slot, createIcon(box));
            holder.boxesBySlot.put(slot, box);
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
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() == PREVIOUS_SLOT) {
            open(player, holder.page - 1);
            return;
        }
        if (event.getRawSlot() == NEXT_SLOT) {
            open(player, holder.page + 1);
            return;
        }
        GachaBox box = holder.boxesBySlot.get(event.getRawSlot());
        if (box == null) {
            return;
        }
        rewardListGui.open(player, box);
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

    private ItemStack createIcon(GachaBox box) {
        ItemStack itemStack = new ItemStack(box.material());
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(box.displayName());
        List<String> lore = new ArrayList<>();
        lore.add("§7보상 개수: §f" + box.rewards().size());
        if (!box.rewards().isEmpty()) {
            lore.add("§7등급별 확률 weight");
            gradeWeights(box).forEach((grade, weight) -> lore.add("§8- §f" + grade + ": §eweight " + weight));
        } else {
            lore.add("§7등록된 보상이 없습니다.");
        }
        lore.add("");
        lore.add("§7가챠권을 우클릭하면 기본 상자를 뽑습니다.");
        lore.add("§e클릭 시 보상 목록 확인");
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private Map<String, Integer> gradeWeights(GachaBox box) {
        Map<String, Integer> weightByGrade = new HashMap<>();
        for (GachaReward reward : box.rewards()) {
            if (reward.weight() <= 0) {
                continue;
            }
            weightByGrade.merge(reward.grade().name(), reward.weight(), Integer::sum);
        }
        Map<String, Integer> weights = new java.util.LinkedHashMap<>();
        weightByGrade.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> kr.clutch.gacha.model.RewardGrade.valueOf(entry.getKey()).ordinal()))
                .forEach(entry -> weights.put(entry.getKey(), entry.getValue()));
        return weights;
    }

    private static final class Holder implements InventoryHolder {
        private final Map<Integer, GachaBox> boxesBySlot = new HashMap<>();
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
