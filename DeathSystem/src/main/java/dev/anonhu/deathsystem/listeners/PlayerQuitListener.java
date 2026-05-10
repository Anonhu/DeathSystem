package dev.anonhu.deathsystem.listeners;

import dev.anonhu.deathsystem.DeathSystemPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Если downed-игрок выходит — полная смерть + дроп вещей.
 * killDowned() уважает keepInventory — здесь мы дропаем ручно перед удалением.
 */
public class PlayerQuitListener implements Listener {

    private final DeathSystemPlugin plugin;

    public PlayerQuitListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        var mgr    = plugin.getDownedPlayerManager();

        if (!mgr.isDown(player)) return;

        // Дропаем вещи вручную (даже если keepInventory=true)
        var loc   = player.getLocation();
        var world = loc.getWorld();
        if (world != null) {
            for (var item : player.getInventory().getContents()) {
                if (item != null) world.dropItemNaturally(loc, item);
            }
            player.getInventory().clear();
        }

        // Стандартная очистка состояния
        mgr.killDowned(player);
    }
}
