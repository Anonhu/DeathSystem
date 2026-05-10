package dev.anonhu.deathsystem.listeners;

import dev.anonhu.deathsystem.DeathSystemPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Блокирует команды для downed-игроков.
 * Позволяет только приватные сообщения (чат идёт через ChatListener).
 */
public class CommandListener implements Listener {

    private final DeathSystemPlugin plugin;

    public CommandListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getDownedPlayerManager().isDown(event.getPlayer())) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(
            "\u00a7cВы не можете использовать команды, пока лежите!"
        );
    }
}
