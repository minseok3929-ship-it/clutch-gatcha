package kr.clutch.gacha.service;

import kr.clutch.gacha.config.GachaConfig;
import kr.clutch.gacha.model.GachaReward;
import kr.clutch.gacha.model.RewardGrade;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;

public final class RewardService {
    private final JavaPlugin plugin;
    private GachaConfig config;

    public RewardService(JavaPlugin plugin, GachaConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updateConfig(GachaConfig config) {
        this.config = config;
    }

    public void giveReward(Player player, GachaReward reward) {
        switch (reward.type()) {
            case MONEY -> giveMoney(player, reward.amount());
            case ITEM -> giveItem(player, reward.rewardItem());
            case TITLE -> giveTitle(player, reward);
            case COMMAND -> runCommand(player, reward.command());
        }
        player.sendMessage(config.message("reward", "§a%reward%§f 보상을 획득했습니다.").replace("%reward%", reward.displayName()));
        playSound(player, config.raw("animation.rewardSound", "ENTITY_PLAYER_LEVELUP"));
        if (reward.grade().isAtLeast(RewardGrade.LEGENDARY)) {
            announceRare(player, reward);
        }
    }

    private void giveMoney(Player player, double amount) {
        if (tryVaultDeposit(player, amount)) {
            return;
        }
        String command = config.moneyCommand();
        if (command == null || command.isBlank()) {
            plugin.getLogger().warning("MONEY 보상을 지급할 경제 플러그인 또는 money.command 설정이 없습니다.");
            return;
        }
        runCommand(player, command.replace("%amount%", formatAmount(amount)));
    }

    private boolean tryVaultDeposit(Player player, double amount) {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        try {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Class economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(economyClass);
            if (provider == null) {
                return false;
            }
            Object economy = provider.getProvider();
            Method deposit = economyClass.getMethod("depositPlayer", Player.class, double.class);
            deposit.invoke(economy, player, amount);
            return true;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "Vault 경제 지급에 실패해 money.command로 대체합니다.", exception);
            return false;
        }
    }

    private void giveItem(Player player, ItemStack itemStack) {
        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private void giveTitle(Player player, GachaReward reward) {
        String title = reward.titleName().isBlank() ? reward.displayName() : reward.titleName();
        runCommand(player, "칭호 지급 %player% " + title + " " + reward.titleColor());
    }

    private void runCommand(Player player, String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
    }

    private void announceRare(Player player, GachaReward reward) {
        Bukkit.broadcastMessage(config.message("broadcastRare", "§6%player%님이 %reward%를 획득했습니다!")
                .replace("%player%", player.getName())
                .replace("%reward%", reward.displayName()));
    }

    private void playSound(Player player, String soundName) {
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 1F, 1F);
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("알 수 없는 사운드입니다: " + soundName);
        }
    }

    private String formatAmount(double amount) {
        return amount == Math.rint(amount) ? String.valueOf((long) amount) : String.valueOf(amount);
    }
}
