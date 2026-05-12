package kr.clutch.gacha;

import kr.clutch.gacha.command.GachaCommand;
import kr.clutch.gacha.config.GachaConfig;
import kr.clutch.gacha.gui.GachaGui;
import kr.clutch.gacha.listener.TicketInteractListener;
import kr.clutch.gacha.repository.StoredGachaRepository;
import kr.clutch.gacha.service.GachaService;
import kr.clutch.gacha.service.RewardService;
import kr.clutch.gacha.service.TicketService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClutchGachaPlugin extends JavaPlugin {
    private GachaConfig config;
    private StoredGachaRepository storedGachaRepository;
    private TicketService ticketService;
    private RewardService rewardService;
    private GachaService gachaService;
    private GachaGui gachaGui;
    private GachaCommand gachaCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        storedGachaRepository = new StoredGachaRepository(this);
        storedGachaRepository.load();
        config = GachaConfig.load(getConfig(), storedGachaRepository.rewards(), storedGachaRepository.deletedRewardIds());

        ticketService = new TicketService(this, config);
        rewardService = new RewardService(this, config);
        gachaService = new GachaService(this, config, rewardService);
        gachaGui = new GachaGui(this, config);
        gachaCommand = new GachaCommand(this, config, gachaGui, ticketService);
        getServer().getPluginManager().registerEvents(new TicketInteractListener(ticketService, gachaService), this);

        registerCommand("가챠");
        registerCommand("가챠권");
        getLogger().info("ClutchGacha 활성화 완료");
    }

    @Override
    public void onDisable() {
    }

    public void reloadPluginConfig() {
        reloadConfig();
        storedGachaRepository.load();
        config = GachaConfig.load(getConfig(), storedGachaRepository.rewards(), storedGachaRepository.deletedRewardIds());
        ticketService.updateConfig(config);
        rewardService.updateConfig(config);
        gachaService.updateConfig(config);
        gachaGui.updateConfig(config);
        gachaCommand.updateConfig(config);
    }

    public void reloadStoredGacha() {
        config = GachaConfig.load(getConfig(), storedGachaRepository.rewards(), storedGachaRepository.deletedRewardIds());
        ticketService.updateConfig(config);
        rewardService.updateConfig(config);
        gachaService.updateConfig(config);
        gachaGui.updateConfig(config);
        gachaCommand.updateConfig(config);
    }

    public StoredGachaRepository storedGachaRepository() {
        return storedGachaRepository;
    }

    public GachaConfig config() {
        return config;
    }

    private void registerCommand(String name) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("plugin.yml에 명령어가 없습니다: " + name);
            return;
        }
        command.setExecutor(gachaCommand);
        command.setTabCompleter(gachaCommand);
    }
}
