package dev.yourname.deathsystem.tasks;

import dev.yourname.deathsystem.DeathSystemPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;

public class CountdownTask extends BukkitRunnable {

    private final DeathSystemPlugin plugin;
    private final Player player;
    private int secondsLeft;

    public CountdownTask(DeathSystemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.secondsLeft = plugin.getPluginConfig().maxWaitSeconds;
    }

    @Override
    public void run() {
        if (!player.isOnline() || !plugin.getDownedPlayerManager().isDown(player)) {
            this.cancel();
            return;
        }

        var state = plugin.getDownedPlayerManager().getState(player);
        state.timeLeft = secondsLeft;

        String color = secondsLeft > 30 ? "§e" : (secondsLeft > 10 ? "§6" : "§c");

        Title title = Title.title(
            Component.text("§c☠ Вы ранены!"),
            Component.text(color + secondsLeft + " сек §8| §7Зажмите §fSNEAK §73 сек для сдачи"),
            Title.Times.times(
                Duration.ofMillis(0),
                Duration.ofMillis(1500),
                Duration.ofMillis(500)
            )
        );
        player.showTitle(title);

        double hpPercent = (player.getHealth() / 20.0) * 100;
        String bar = buildHpBar(hpPercent);
        player.sendActionBar(Component.text("§4❤ " + bar + " §c" + String.format("%.1f", player.getHealth()) + " HP"));

        secondsLeft--;

        if (secondsLeft < 0) {
            this.cancel();
            plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getDownedPlayerManager().revivePlayer(player, null, 0)
            );
        }
    }

    private String buildHpBar(double percent) {
        int filled = (int) Math.round(percent / 10.0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? "§c█" : "§8░");
        }
        return sb.toString();
    }
}
