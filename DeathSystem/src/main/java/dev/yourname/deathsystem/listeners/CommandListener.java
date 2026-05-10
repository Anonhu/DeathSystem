package dev.yourname.deathsystem.listeners;

import dev.yourname.deathsystem.DeathSystemPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Set;

public class CommandListener implements Listener {

    private final DeathSystemPlugin plugin;
    private static final Set<String> ALLOWED_COMMANDS = Set.of(
        "/list", "/who", "/online"
    );

    public CommandListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDownedPlayerManager().isDown(player)) return;
        if (player.hasPermission("deathsystem.bypass")) return;

        String cmd = event.getMessage().toLowerCase().split(" ")[0];
        if (ALLOWED_COMMANDS.contains(cmd)) return;

        event.setCancelled(true);
        player.sendMessage("§c[!] §7Вы не можете использовать команды в таком состоянии...");
    }
}
