package kr.clutch.gacha.service;

import kr.clutch.gacha.config.GachaConfig;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class TicketService {
    private final NamespacedKey ticketKey;
    private GachaConfig config;

    public TicketService(JavaPlugin plugin, GachaConfig config) {
        this.ticketKey = new NamespacedKey(plugin, "clutch_gacha_ticket");
        this.config = config;
    }

    public void updateConfig(GachaConfig config) {
        this.config = config;
    }

    public ItemStack createTicket(int amount) {
        ItemStack itemStack = new ItemStack(config.ticketMaterial(), Math.max(1, amount));
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(config.ticketDisplayName());
        meta.setLore(config.ticketLore());
        if (config.ticketCustomModelData() != null) {
            meta.setCustomModelData(config.ticketCustomModelData());
        }
        meta.getPersistentDataContainer().set(ticketKey, PersistentDataType.BYTE, (byte) 1);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public void giveTicket(Player player, int amount) {
        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(createTicket(amount));
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    public void consumeHeldTicket(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (!isTicket(itemStack)) {
            return;
        }
        int amount = itemStack.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            itemStack.setAmount(amount - 1);
            player.getInventory().setItemInMainHand(itemStack);
        }
    }

    public boolean isTicket(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        Byte marker = itemStack.getItemMeta().getPersistentDataContainer().get(ticketKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }
}
