package kr.clutch.gacha.model;

import org.bukkit.Material;

import java.util.List;

public record GachaBox(
        String id,
        int slot,
        String displayName,
        Material material,
        List<String> lore,
        List<GachaReward> rewards
) {
}
