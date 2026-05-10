package kr.clutch.gacha.config;

import kr.clutch.gacha.model.GachaBox;
import kr.clutch.gacha.model.GachaReward;
import kr.clutch.gacha.model.RewardGrade;
import kr.clutch.gacha.model.RewardType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class GachaConfig {
    private final String prefix;
    private final String moneyCommand;
    private final String defaultBox;
    private final Material ticketMaterial;
    private final Integer ticketCustomModelData;
    private final String ticketDisplayName;
    private final List<String> ticketLore;
    private final String guiTitle;
    private final int guiSize;
    private final Map<String, GachaBox> boxes;
    private final FileConfiguration config;

    private GachaConfig(FileConfiguration config, Map<String, GachaBox> boxes) {
        this.config = config;
        this.prefix = config.getString("prefix", "§8[CLUTCH] ");
        this.moneyCommand = config.getString("money.command", "");
        this.defaultBox = config.getString("defaultBox", "normal");
        this.ticketMaterial = material(config.getString("ticket.material"), Material.PAPER);
        this.ticketCustomModelData = config.isInt("ticket.customModelData") ? config.getInt("ticket.customModelData") : null;
        this.ticketDisplayName = config.getString("ticket.displayName", "§6가챠권");
        this.ticketLore = config.getStringList("ticket.lore").isEmpty()
                ? List.of("§7우클릭으로 사용할 수 있습니다.")
                : config.getStringList("ticket.lore");
        this.guiTitle = config.getString("gui.title", "§8CLUTCH 가챠");
        this.guiSize = 27;
        this.boxes = Collections.unmodifiableMap(boxes);
    }

    public static GachaConfig load(FileConfiguration config) {
        return load(config, Map.of());
    }

    public static GachaConfig load(FileConfiguration config, Map<String, GachaBox> extraBoxes) {
        return load(config, extraBoxes, Set.of(), Map.of());
    }

    public static GachaConfig load(FileConfiguration config, Map<String, GachaBox> extraBoxes, Set<String> deletedBoxIds) {
        return load(config, extraBoxes, deletedBoxIds, Map.of());
    }

    public static GachaConfig load(FileConfiguration config, Map<String, GachaBox> extraBoxes, Set<String> deletedBoxIds, Map<String, Set<String>> deletedRewardIds) {
        Map<String, GachaBox> boxes = new LinkedHashMap<>();
        ConfigurationSection boxSection = config.getConfigurationSection("boxes");
        if (boxSection != null) {
            for (String boxId : boxSection.getKeys(false)) {
                ConfigurationSection section = boxSection.getConfigurationSection(boxId);
                if (section != null && !deletedBoxIds.contains(boxId)) {
                    boxes.put(boxId, parseBox(boxId, section, deletedRewardIds.getOrDefault(boxId, Set.of())));
                }
            }
        }
        for (Map.Entry<String, GachaBox> entry : extraBoxes.entrySet()) {
            GachaBox base = boxes.get(entry.getKey());
            if (base == null) {
                boxes.put(entry.getKey(), entry.getValue());
                continue;
            }
            List<GachaReward> mergedRewards = new ArrayList<>(base.rewards());
            mergedRewards.addAll(entry.getValue().rewards());
            boxes.put(entry.getKey(), new GachaBox(
                    base.id(),
                    base.slot(),
                    base.displayName(),
                    base.material(),
                    base.lore(),
                    Collections.unmodifiableList(mergedRewards)
            ));
        }
        return new GachaConfig(config, boxes);
    }

    private static GachaBox parseBox(String id, ConfigurationSection section, Set<String> deletedRewardIds) {
        List<GachaReward> rewards = new ArrayList<>();
        for (Map<?, ?> rawReward : section.getMapList("rewards")) {
            String rewardId = string(rawReward, "id", "reward");
            if (!deletedRewardIds.contains(rewardId)) {
                rewards.add(parseReward(rawReward));
            }
        }
        return new GachaBox(
                id,
                section.getInt("slot", 0),
                section.getString("displayName", id),
                material(section.getString("material"), Material.CHEST),
                section.getStringList("lore"),
                Collections.unmodifiableList(rewards)
        );
    }

    private static GachaReward parseReward(Map<?, ?> rawReward) {
        String id = string(rawReward, "id", "reward");
        return new GachaReward(
                id,
                enumValue(RewardType.class, string(rawReward, "type", "ITEM"), RewardType.ITEM),
                enumValue(RewardGrade.class, string(rawReward, "grade", "COMMON"), RewardGrade.COMMON),
                Math.max(0, integer(rawReward, "weight", 1)),
                string(rawReward, "displayName", id),
                decimal(rawReward, "amount", 0D),
                material(string(rawReward, "material", "STONE"), Material.STONE),
                Math.max(1, integer(rawReward, "itemAmount", integer(rawReward, "amount", 1))),
                string(rawReward, "command", ""),
                string(rawReward, "title", string(rawReward, "titleName", "")),
                string(rawReward, "color", string(rawReward, "titleColor", "white")),
                null
        );
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

    private static int normalizeGuiSize(int configured) {
        int size = Math.max(9, Math.min(54, configured));
        return size % 9 == 0 ? size : ((size / 9) + 1) * 9;
    }

    public String message(String path, String fallback) {
        return prefix + config.getString("messages." + path, fallback);
    }

    public String raw(String path, String fallback) {
        return config.getString(path, fallback);
    }

    public String prefix() {
        return prefix;
    }

    public String moneyCommand() {
        return moneyCommand;
    }

    public String defaultBox() {
        return defaultBox;
    }

    public Material ticketMaterial() {
        return ticketMaterial;
    }

    public Integer ticketCustomModelData() {
        return ticketCustomModelData;
    }

    public String ticketDisplayName() {
        return ticketDisplayName;
    }

    public List<String> ticketLore() {
        return ticketLore;
    }

    public String guiTitle() {
        return guiTitle;
    }

    public int guiSize() {
        return guiSize;
    }

    public Map<String, GachaBox> boxes() {
        return boxes;
    }
}
