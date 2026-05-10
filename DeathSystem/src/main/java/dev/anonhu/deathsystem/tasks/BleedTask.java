package dev.anonhu.deathsystem.tasks;

import dev.anonhu.deathsystem.DeathSystemPlugin;
import dev.anonhu.deathsystem.managers.DownedState;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Тик кровотечения — каждую секунду.
 * Если HP достигает 0 — вызывает killDowned().
 */
public class BleedTask extends BukkitRunnable {

    private final DeathSystemPlugin plugin;
    private final DownedState       state;

    public BleedTask(DeathSystemPlugin plugin, DownedState state) {
        this.plugin = plugin;
        this.state  = state;
    }

    @Override
    public void run() {
        var player = state.player;
        if (!player.isOnline()
                || !plugin.getDownedPlayerManager().isDown(player)) {
            cancel();
            return;
        }

        double rate = state.isCrawling
            ? plugin.getPluginConfig().bleedRateCrawling
            : plugin.getPluginConfig().bleedRateIdle;

        double newHp = player.getHealth() - rate;

        if (newHp <= 0) {
            // HP до нуля — полная смерть
            plugin.getDownedPlayerManager().killDowned(player);
            cancel();
            return;
        }

        player.setHealth(newHp);

        // Частицы крови
        player.getWorld().spawnParticle(
            Particle.DAMAGE_INDICATOR,
            player.getLocation().add(0, 0.5, 0),
            3, 0.2, 0.1, 0.2, 0.01
        );

        // Звук сердцебиения при низком HP
        if (newHp < 5.0) {
            player.playSound(player.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.5f);
        }
    }
}
