package kr.clutch.gacha.command;

import kr.clutch.gacha.ClutchGachaPlugin;
import kr.clutch.gacha.config.GachaConfig;
import kr.clutch.gacha.gui.GachaGui;
import kr.clutch.gacha.model.RewardGrade;
import kr.clutch.gacha.service.TicketService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GachaCommand implements CommandExecutor, TabCompleter, Listener {
    private final ClutchGachaPlugin plugin;
    private final GachaGui gui;
    private final TicketService ticketService;
    private final Map<UUID, ItemStack> pendingRewards = new ConcurrentHashMap<>();
    private GachaConfig config;

    public GachaCommand(ClutchGachaPlugin plugin, GachaConfig config, GachaGui gui, TicketService ticketService) {
        this.plugin = plugin;
        this.config = config;
        this.gui = gui;
        this.ticketService = ticketService;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void updateConfig(GachaConfig config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("가챠권")) {
            return handleTicket(sender, args);
        }
        if (args.length > 0) {
            return switch (args[0]) {
                case "리로드" -> handleReload(sender);
                case "상품추가" -> handleAddReward(sender, args);
                case "상품목록" -> handleRewardList(sender, args);
                case "상품삭제" -> handleDeleteReward(sender, args);
                default -> handleOpen(sender);
            };
        }
        return handleOpen(sender);
    }

    private boolean handleOpen(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.prefix() + "§c플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!player.hasPermission("clutch.gacha.use")) {
            player.sendMessage(config.prefix() + "§c권한이 없습니다.");
            return true;
        }
        gui.open(player);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return true;
        }
        plugin.reloadPluginConfig();
        sender.sendMessage(plugin.config().message("reloaded", "§aClutchGacha 설정을 리로드했습니다."));
        return true;
    }

    private boolean handleAddReward(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.prefix() + "§c플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(config.prefix() + "§c사용법: /가챠 상품추가");
            return true;
        }
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            sender.sendMessage(config.prefix() + "§c보상으로 저장할 아이템을 손에 들어주세요.");
            return true;
        }
        pendingRewards.put(player.getUniqueId(), itemStack.clone());
        player.closeInventory();
        player.sendMessage(config.prefix() + "§e채팅으로 등급과 weight를 입력하세요. 예: LEGENDARY 1");
        return true;
    }

    private boolean handleRewardList(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(config.prefix() + "§c사용법: /가챠 상품목록");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.prefix() + "§c플레이어만 사용할 수 있습니다.");
            return true;
        }
        gui.open(player);
        return true;
    }

    private boolean handleDeleteReward(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(config.prefix() + "§c사용법: /가챠 상품삭제 <보상ID>");
            return true;
        }
        String rewardId = args[1];
        if (config.rewards().stream().noneMatch(reward -> reward.id().equals(rewardId))) {
            sender.sendMessage(config.prefix() + "§c삭제할 보상을 찾을 수 없습니다.");
            return true;
        }
        boolean deleted = plugin.storedGachaRepository().deleteReward(rewardId);
        if (!deleted) {
            sender.sendMessage(config.prefix() + "§c삭제할 보상을 찾을 수 없습니다.");
            return true;
        }
        plugin.reloadStoredGacha();
        sender.sendMessage(config.prefix() + "§a보상을 삭제했습니다.");
        return true;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        ItemStack pending = pendingRewards.remove(event.getPlayer().getUniqueId());
        if (pending == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> finishAddReward(event.getPlayer(), pending, message));
    }

    private void finishAddReward(Player player, ItemStack pending, String message) {
        String[] parts = message.trim().split("\\s+");
        if (parts.length != 2) {
            player.sendMessage(config.prefix() + "§c취소되었습니다.");
            return;
        }
        RewardGrade grade;
        int weight;
        try {
            grade = RewardGrade.valueOf(parts[0].toUpperCase(Locale.ROOT));
            weight = Integer.parseInt(parts[1]);
        } catch (IllegalArgumentException exception) {
            player.sendMessage(config.prefix() + "§c취소되었습니다.");
            return;
        }
        if (weight <= 0) {
            player.sendMessage(config.prefix() + "§c취소되었습니다.");
            return;
        }
        String displayName = itemDisplayName(pending);
        plugin.storedGachaRepository().addItemReward(pending, grade, weight, displayName);
        plugin.reloadStoredGacha();
        player.sendMessage(config.prefix() + "§a상품을 추가했습니다. §7(" + grade + ", weight " + weight + ")");
    }

    private boolean handleTicket(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length != 3 || !args[0].equalsIgnoreCase("지급")) {
            sender.sendMessage(config.prefix() + "§c사용법: /가챠권 지급 <닉네임> <개수>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(config.prefix() + "§c접속 중인 플레이어를 찾을 수 없습니다.");
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(config.prefix() + "§c개수는 숫자여야 합니다.");
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage(config.prefix() + "§c개수는 1 이상이어야 합니다.");
            return true;
        }
        ticketService.giveTicket(target, amount);
        sender.sendMessage(config.message("ticketGiven", "§a%player%님에게 가챠권 %amount%개를 지급했습니다.")
                .replace("%player%", target.getName())
                .replace("%amount%", String.valueOf(amount)));
        return true;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("clutch.gacha.admin")) {
            return true;
        }
        sender.sendMessage(config.prefix() + "§c권한이 없습니다.");
        return false;
    }

    private String itemDisplayName(ItemStack itemStack) {
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            return itemStack.getItemMeta().getDisplayName();
        }
        return itemStack.getType().name();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equals("가챠") && sender.hasPermission("clutch.gacha.admin")) {
            if (args.length == 1) {
                completions.addAll(List.of("리로드", "상품추가", "상품목록", "상품삭제"));
            } else if (args.length == 2 && args[0].equals("상품삭제")) {
                config.rewards().forEach(reward -> completions.add(reward.id()));
            }
        }
        if (command.getName().equals("가챠권")) {
            if (args.length == 1) {
                completions.add("지급");
            } else if (args.length == 2) {
                Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
            }
        }
        return completions.stream().filter(value -> args.length == 0 || value.startsWith(args[args.length - 1])).toList();
    }
}
