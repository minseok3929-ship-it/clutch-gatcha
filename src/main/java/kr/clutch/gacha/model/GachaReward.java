package kr.clutch.gacha.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public record GachaReward(
        String id,
        RewardType type,
        RewardGrade grade,
        int weight,
        String displayName,
        double amount,
        Material material,
        int itemAmount,
        String command,
        String titleName,
        String titleColor,
        ItemStack itemStack
) {
    public ItemStack rewardItem() {
        if (itemStack != null) {
            return itemStack.clone();
        }
        return new ItemStack(material, Math.max(1, itemAmount));
    }
}
