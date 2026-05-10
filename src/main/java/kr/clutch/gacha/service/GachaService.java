package kr.clutch.gacha.service;

import kr.clutch.gacha.config.GachaConfig;
import kr.clutch.gacha.model.GachaReward;
import kr.clutch.gacha.model.RewardGrade;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GachaService {
    private static final int GUI_SIZE = 27;
    private static final int WIN_SLOT = 13;
    private static final int TOTAL_ANIMATION_TICKS = 60;

    private final JavaPlugin plugin;
    private final RewardService rewardService;
    private final Set<UUID> rollingPlayers = new HashSet<>();
    private GachaConfig config;

    public GachaService(JavaPlugin plugin, GachaConfig config, RewardService rewardService) {
        this.plugin = plugin;
        this.config = config;
        this.rewardService = rewardService;
    }

    public void updateConfig(GachaConfig config) {
        this.config = config;
    }

    public boolean isRolling(Player player) {
        return rollingPlayers.contains(player.getUniqueId());
    }

    public boolean canRoll(Player player) {
        if (isRolling(player)) {
            player.sendMessage(config.prefix() + "§c이미 가챠가 진행 중입니다.");
            return false;
        }
        if (totalWeight(config.rewards()) <= 0) {
            player.sendMessage(config.prefix() + "§c지급 가능한 가챠 보상이 없습니다.");
            return false;
        }
        return true;
    }

    public boolean startRoulette(Player player) {
        GachaReward finalReward = weighted(config.rewards());
        if (finalReward == null) {
            player.sendMessage(config.prefix() + "§c지급 가능한 가챠 보상이 없습니다.");
            return false;
        }
        rollingPlayers.add(player.getUniqueId());
        RouletteHolder holder = new RouletteHolder();
        Inventory inventory = Bukkit.createInventory(holder, GUI_SIZE, "§8CLUTCH §7가챠");
        holder.inventory = inventory;
        decorate(inventory);
        player.openInventory(inventory);
        runRoulette(player, inventory, finalReward, 0);
        return true;
    }

    private void runRoulette(Player player, Inventory inventory, GachaReward finalReward, int elapsedTicks) {
        if (elapsedTicks >= TOTAL_ANIMATION_TICKS) {
            finishRoulette(player, inventory, finalReward);
            return;
        }
        spin(inventory, elapsedTicks);
        playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT);
        int delay = nextDelay(elapsedTicks);
        new BukkitRunnable() {
            @Override
            public void run() {
                runRoulette(player, inventory, finalReward, elapsedTicks + delay);
            }
        }.runTaskLater(plugin, delay);
    }

    private void finishRoulette(Player player, Inventory inventory, GachaReward finalReward) {
        inventory.setItem(WIN_SLOT, displayItem(finalReward));
        player.sendTitle(config.raw("animation.successTitle", "§8CLUTCH"), config.raw("animation.successSubtitle", "§f가챠 보상을 획득했습니다!"), 10, 50, 20);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
        if (finalReward.grade().isAtLeast(RewardGrade.LEGENDARY)) {
            playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE);
        }
        rewardService.giveReward(player, finalReward);
        new BukkitRunnable() {
            @Override
            public void run() {
                rollingPlayers.remove(player.getUniqueId());
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof RouletteHolder) {
                    player.closeInventory();
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    private void spin(Inventory inventory, int elapsedTicks) {
        List<GachaReward> rewards = displayRewards();
        for (int slot = 9; slot <= 17; slot++) {
            GachaReward reward = rewards.get(ThreadLocalRandom.current().nextInt(rewards.size()));
            inventory.setItem(slot, displayItem(reward));
        }
        ItemStack marker = new ItemStack(elapsedTicks > 42 ? Material.YELLOW_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = marker.getItemMeta();
        meta.setDisplayName("§e당첨 위치");
        marker.setItemMeta(meta);
        inventory.setItem(4, marker);
        inventory.setItem(22, marker);
    }

    private List<GachaReward> displayRewards() {
        List<GachaReward> rewards = config.rewards().stream().filter(reward -> reward.weight() > 0).toList();
        return rewards.isEmpty() ? new ArrayList<>(config.rewards()) : rewards;
    }

    private int nextDelay(int elapsedTicks) {
        if (elapsedTicks < 24) {
            return 2;
        }
        if (elapsedTicks < 44) {
            return 4;
        }
        if (elapsedTicks < 54) {
            return 6;
        }
        return 8;
    }

    private void decorate(Inventory inventory) {
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

    private ItemStack displayItem(GachaReward reward) {
        ItemStack itemStack = reward.itemStack() == null ? new ItemStack(reward.material(), Math.max(1, reward.itemAmount())) : reward.itemStack().clone();
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(reward.displayName());
        List<String> lore = new ArrayList<>();
        lore.add("§7등급: §f" + reward.grade());
        lore.add("§7weight: §f" + reward.weight());
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private GachaReward weighted(List<GachaReward> rewards) {
        int totalWeight = totalWeight(rewards);
        if (totalWeight <= 0) {
            return null;
        }
        int cursor = ThreadLocalRandom.current().nextInt(totalWeight);
        for (GachaReward reward : rewards) {
            if (reward.weight() <= 0) {
                continue;
            }
            cursor -= reward.weight();
            if (cursor < 0) {
                return reward;
            }
        }
        return null;
    }

    private int totalWeight(List<GachaReward> rewards) {
        return rewards.stream().mapToInt(GachaReward::weight).filter(weight -> weight > 0).sum();
    }

    private void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1F, 1F);
    }

    public static final class RouletteHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
