package kr.clutch.gacha.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;

public record GachaReward(
        String id,
        RewardType type,
        RewardGrade grade,
        double weight,
        String displayName,
        double amount,
        Material material,
        int itemAmount,
        String command,
        String titleName,
        String titleColor,
        ItemStack itemStack
) {
    public String effectiveDisplayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        if (itemStack != null && itemStack.hasItemMeta()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
        }
        Material fallbackMaterial = itemStack == null ? material : itemStack.getType();
        return prettyMaterialName(fallbackMaterial);
    }

    public ItemStack rewardItem() {
        if (itemStack != null) {
            return itemStack.clone();
        }
        return new ItemStack(material, Math.max(1, itemAmount));
    }

    private String prettyMaterialName(Material material) {
        if (material == null) {
            return id;
        }
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }
}
