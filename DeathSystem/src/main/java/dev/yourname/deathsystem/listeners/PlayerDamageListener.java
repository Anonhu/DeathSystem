package dev.yourname.deathsystem.listeners;

import dev.yourname.deathsystem.DeathSystemPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class PlayerDamageListener implements Listener {

    private final DeathSystemPlugin plugin;

    public PlayerDamageListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var downedManager = plugin.getDownedPlayerManager();

        if (downedManager.isDown(player)) {
            event.setCancelled(true);
            return;
        }

        if (player.isInvulnerable()) return;

        double resultHp = player.getHealth() - event.getFinalDamage();
        if (resultHp > 0) return;

        event.setCancelled(true);

        Player killer = null;
        if (event instanceof EntityDamageByEntityEvent byEntityEvent) {
            if (byEntityEvent.getDamager() instanceof Player k) killer = k;
        }

        final Player finalKiller = killer;
        plugin.getServer().getScheduler().runTask(plugin, () ->
            downedManager.downPlayer(player, finalKiller)
        );
    }
}
