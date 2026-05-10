package dev.anonhu.deathsystem;

import dev.anonhu.deathsystem.config.PluginConfig;
import dev.anonhu.deathsystem.listeners.*;
import dev.anonhu.deathsystem.managers.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Главный класс плагина.
 * Связывает все менеджеры и листенеры.
 */
public final class DeathSystemPlugin extends JavaPlugin {

    private PluginConfig        pluginConfig;
    private DownedPlayerManager downedPlayerManager;
    private CustomItemManager   customItemManager;
    private RevivalManager      revivalManager;
    private PlayerMoveListener  moveListener;

    // =========================================================
    //  Жизненный цикл
    // =========================================================

    @Override
    public void onEnable() {
        // 1. Конфиг
        pluginConfig = new PluginConfig(this);

        // 2. Менеджеры
        downedPlayerManager = new DownedPlayerManager(this);
        customItemManager   = new CustomItemManager(this);
        revivalManager      = new RevivalManager(this);

        // 3. Листенеры
        registerListeners();

        // 4. PlaceholderAPI (мягкая зависимость)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            // new DeathSystemPlaceholders(this).register();
            getLogger().info("PlaceholderAPI found — integration ready (uncomment when class added).");
        }

        getLogger().info("╔═════════════════════════╗");
        getLogger().info("║   DeathSystem  v" + getDescription().getVersion() + "   ║");
        getLogger().info("╚═════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (downedPlayerManager != null) {
            downedPlayerManager.killAllDowned();
        }
        getLogger().info("DeathSystem выключен. Все downed-игроки убиты.");
    }

    // =========================================================
    //  Команда /ds reload
    // =========================================================

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String[] args) {
        if (!command.getName().equalsIgnoreCase("deathsystem")) return false;
        if (!sender.hasPermission("deathsystem.admin")) {
            sender.sendMessage("\u00a7cНедостаточно прав.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            pluginConfig = new PluginConfig(this);
            sender.sendMessage("\u00a7aDeathSystem: конфиг перезагружен.");
            getLogger().info("Config reloaded by " + sender.getName());
        } else {
            sender.sendMessage("\u00a77Использование: /ds reload");
        }
        return true;
    }

    // =========================================================
    //  Регистрация листенеров
    // =========================================================

    private void registerListeners() {
        var pm = getServer().getPluginManager();

        moveListener = new PlayerMoveListener(this);

        pm.registerEvents(new PlayerDamageListener(this), this);
        pm.registerEvents(moveListener,                   this);
        pm.registerEvents(new InventoryListener(this),    this);
        pm.registerEvents(new CommandListener(this),      this);
        pm.registerEvents(new PlayerQuitListener(this),   this);
        pm.registerEvents(new ChatListener(this),         this);
        pm.registerEvents(revivalManager,                 this); // RevivalManager тоже Listener
    }

    // =========================================================
    //  Геттеры
    // =========================================================

    public PluginConfig        getPluginConfig()        { return pluginConfig; }
    public DownedPlayerManager getDownedPlayerManager() { return downedPlayerManager; }
    public CustomItemManager   getCustomItemManager()   { return customItemManager; }
    public RevivalManager      getRevivalManager()      { return revivalManager; }
    public PlayerMoveListener  getMoveListener()        { return moveListener; }
}
