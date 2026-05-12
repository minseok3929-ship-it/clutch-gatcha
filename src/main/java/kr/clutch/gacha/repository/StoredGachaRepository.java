package kr.clutch.gacha.repository;

import kr.clutch.gacha.model.GachaReward;
import kr.clutch.gacha.model.RewardGrade;
import kr.clutch.gacha.model.RewardType;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class StoredGachaRepository {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;
    private List<GachaReward> rewards = new ArrayList<>();
    private Set<String> deletedRewardIds = new HashSet<>();

    public StoredGachaRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rewards.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("데이터 폴더를 생성하지 못했습니다.");
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        deletedRewardIds = new HashSet<>(yaml.getStringList("deletedRewards"));
        rewards = new ArrayList<>();
        for (Map<?, ?> rawReward : yaml.getMapList("gacha.rewards")) {
            GachaReward reward = parseReward(rawReward);
            if (reward != null) {
                rewards.add(reward);
            }
        }
    }

    public List<GachaReward> rewards() {
        return Collections.unmodifiableList(rewards);
    }

    public Set<String> deletedRewardIds() {
        return Collections.unmodifiableSet(deletedRewardIds);
    }

    public GachaReward addItemReward(ItemStack originalItem, RewardGrade grade, double weight, String displayName) {
        String rewardId = "item_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        GachaReward reward = new GachaReward(
                rewardId,
                RewardType.ITEM,
                grade,
                weight,
                displayName,
                0D,
                originalItem.getType(),
                originalItem.getAmount(),
                "",
                "",
                "white",
                originalItem.clone()
        );
        List<Map<String, Object>> rewards = new ArrayList<>();
        for (Map<?, ?> raw : yaml.getMapList("gacha.rewards")) {
            Map<String, Object> copy = new LinkedHashMap<>();
            raw.forEach((key, value) -> copy.put(String.valueOf(key), value));
            rewards.add(copy);
        }
        Map<String, Object> serialized = new LinkedHashMap<>();
        serialized.put("id", rewardId);
        serialized.put("type", RewardType.ITEM.name());
        serialized.put("grade", grade.name());
        serialized.put("weight", weight);
        serialized.put("displayName", displayName);
        serialized.put("item", serializeItem(originalItem));
        rewards.add(serialized);
        yaml.set("gacha.rewards", rewards);
        saveAndReload();
        return reward;
    }

    public boolean deleteReward(String rewardId) {
        List<Map<String, Object>> rewards = new ArrayList<>();
        boolean removed = false;
        for (Map<?, ?> raw : yaml.getMapList("gacha.rewards")) {
            if (rewardId.equals(String.valueOf(raw.get("id")))) {
                removed = true;
                continue;
            }
            Map<String, Object> copy = new LinkedHashMap<>();
            raw.forEach((key, value) -> copy.put(String.valueOf(key), value));
            rewards.add(copy);
        }
        if (!removed) {
            deletedRewardIds.add(rewardId);
            yaml.set("deletedRewards", new ArrayList<>(deletedRewardIds));
            saveAndReload();
            return true;
        }
        yaml.set("gacha.rewards", rewards);
        saveAndReload();
        return true;
    }

    private GachaReward parseReward(Map<?, ?> rawReward) {
        String id = string(rawReward, "id", "reward");
        ItemStack itemStack = null;
        String serialized = string(rawReward, "item", "");
        if (!serialized.isBlank()) {
            itemStack = deserializeItem(serialized);
        }
        Material material = itemStack == null ? material(string(rawReward, "material", "STONE"), Material.STONE) : itemStack.getType();
        int itemAmount = itemStack == null ? Math.max(1, integer(rawReward, "itemAmount", integer(rawReward, "amount", 1))) : itemStack.getAmount();
        return new GachaReward(
                id,
                enumValue(RewardType.class, string(rawReward, "type", "ITEM"), RewardType.ITEM),
                enumValue(RewardGrade.class, string(rawReward, "grade", "COMMON"), RewardGrade.COMMON),
                Math.max(0D, decimal(rawReward, "weight", 1D)),
                string(rawReward, "displayName", ""),
                decimal(rawReward, "amount", 0D),
                material,
                itemAmount,
                string(rawReward, "command", ""),
                string(rawReward, "title", string(rawReward, "titleName", "")),
                string(rawReward, "color", string(rawReward, "titleColor", "white")),
                itemStack
        );
    }

    private void saveAndReload() {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "rewards.yml 저장에 실패했습니다.", exception);
        }
        load();
    }

    private String serializeItem(ItemStack itemStack) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(); BukkitObjectOutputStream objectStream = new BukkitObjectOutputStream(byteStream)) {
            objectStream.writeObject(itemStack.clone());
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("아이템 직렬화에 실패했습니다.", exception);
        }
    }

    private ItemStack deserializeItem(String value) {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(Base64.getDecoder().decode(value)); BukkitObjectInputStream objectStream = new BukkitObjectInputStream(byteStream)) {
            Object object = objectStream.readObject();
            return object instanceof ItemStack itemStack ? itemStack : null;
        } catch (IOException | ClassNotFoundException | IllegalArgumentException exception) {
            plugin.getLogger().log(Level.WARNING, "저장된 아이템을 읽지 못했습니다.", exception);
            return null;
        }
    }

    private static String string(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static int integer(Map<?, ?> map, String key, int fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double decimal(Map<?, ?> map, String key, double fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Material material(String value, Material fallback) {
        if (value == null) {
            return fallback;
        }
        Material material = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
