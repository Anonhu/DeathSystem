package dev.yourname.deathsystem.tasks;

import dev.yourname.deathsystem.DeathSystemPlugin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BleedTask extends BukkitRunnable {

    private final DeathSystemPlugin plugin;
    private final Player player;

    public BleedTask(DeathSystemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public void run() {
        if (!player.isOnline() || !plugin.getDownedPlayerManager().isDown(player)) {
            this.cancel();
            return;
        }

        var state = plugin.getDownedPlayerManager().getState(player);
        var cfg = plugin.getPluginConfig();

        plugin.getDownedPlayerManager().refreshDownedPose(player);

        double bleedRate = state.isCrawling ? cfg.bleedRateCrawling : cfg.bleedRateIdle;
        double newHp = player.getHealth() - bleedRate;

        if (newHp <= 0) {
            this.cancel();
            plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getDownedPlayerManager().killDowned(player)
            );
        } else {
            player.setHealth(newHp);
            player.getWorld().spawnParticle(
                org.bukkit.Particle.DAMAGE_INDICATOR,
                player.getLocation().add(0, 0.5, 0),
                3, 0.2, 0.2, 0.2, 0.01
            );
        }
    }
}
