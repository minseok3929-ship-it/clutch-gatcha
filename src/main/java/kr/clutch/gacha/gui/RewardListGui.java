package kr.clutch.gacha.gui;

import kr.clutch.gacha.model.GachaBox;
import kr.clutch.gacha.model.GachaReward;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class RewardListGui {
    public void open(Player player, GachaBox box) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§8보상 목록: " + box.id());
        int slot = 0;
        for (GachaReward reward : box.rewards()) {
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, icon(reward));
        }
        player.openInventory(inventory);
    }

    private ItemStack icon(GachaReward reward) {
        ItemStack itemStack = reward.itemStack() == null ? new ItemStack(Material.PAPER) : reward.itemStack().clone();
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(reward.displayName());
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (!lore.isEmpty()) {
            lore.add("");
        }
        lore.add("§7보상ID: §f" + reward.id());
        lore.add("§7타입: §f" + reward.type());
        lore.add("§7등급: §f" + reward.grade());
        lore.add("§7확률 weight: §f" + reward.weight());
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
