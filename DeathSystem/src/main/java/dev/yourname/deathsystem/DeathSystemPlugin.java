package dev.yourname.deathsystem;

import dev.yourname.deathsystem.api.DeathSystemPlaceholders;
import dev.yourname.deathsystem.config.PluginConfig;
import dev.yourname.deathsystem.listeners.*;
import dev.yourname.deathsystem.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathSystemPlugin extends JavaPlugin {

    private static DeathSystemPlugin instance;
    private PluginConfig pluginConfig;
    private DownedPlayerManager downedPlayerManager;
    private RevivalManager revivalManager;
    private CustomItemManager customItemManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.pluginConfig = new PluginConfig(this);
        this.customItemManager = new CustomItemManager(this);
        this.downedPlayerManager = new DownedPlayerManager(this);
        this.revivalManager = new RevivalManager(this);

        customItemManager.registerRecipes();
        registerListeners();
        registerCommands();
        registerPlaceholders();

        getLogger().info("DeathSystem v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        downedPlayerManager.killAllDowned();
        getLogger().info("DeathSystem disabled.");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();

        pm.registerEvents(new PlayerDamageListener(this), this);

        var moveListener = new PlayerMoveListener(this);
        pm.registerEvents(moveListener, this);
        downedPlayerManager.setMoveListener(moveListener);

        pm.registerEvents(new PlayerInteractListener(this), this);
        pm.registerEvents(new InventoryListener(this), this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new CommandListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
    }

    private void registerCommands() {
        getCommand("deathsystem").setExecutor((sender, cmd, label, args) -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("deathsystem.admin")) {
                    sender.sendMessage("§cНет прав!");
                    return true;
                }
                reloadConfig();
                pluginConfig.reload();
                sender.sendMessage("§aКонфиг перезагружен!");
            }
            return true;
        });
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DeathSystemPlaceholders(this).register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }
    }

    public static DeathSystemPlugin getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public DownedPlayerManager getDownedPlayerManager() {
        return downedPlayerManager;
    }

    public RevivalManager getRevivalManager() {
        return revivalManager;
    }

    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }
}
