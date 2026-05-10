package kr.clutch.gacha.service;

import kr.clutch.gacha.config.GachaConfig;
import kr.clutch.gacha.model.GachaReward;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class GachaService {
    private final RewardService rewardService;
    private GachaConfig config;

    public GachaService(GachaConfig config, RewardService rewardService) {
        this.config = config;
        this.rewardService = rewardService;
    }

    public void updateConfig(GachaConfig config) {
        this.config = config;
    }

    public boolean canRoll(Player player) {
        if (totalWeight(config.rewards()) <= 0) {
            player.sendMessage(config.prefix() + "§c지급 가능한 가챠 보상이 없습니다.");
            return false;
        }
        return true;
    }

    public boolean roll(Player player) {
        GachaReward reward = weighted(config.rewards());
        if (reward == null) {
            player.sendMessage(config.prefix() + "§c지급 가능한 가챠 보상이 없습니다.");
            return false;
        }
        player.sendTitle(config.raw("animation.successTitle", "§8CLUTCH"), config.raw("animation.successSubtitle", "§f가챠 보상을 획득했습니다!"), 10, 50, 20);
        rewardService.giveReward(player, reward);
        return true;
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
}
