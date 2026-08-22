package dev.yourname.deathsystem.tasks;

import dev.yourname.deathsystem.DeathSystemPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Objects;

public class CountdownTask extends BukkitRunnable {

    private static final LegacyComponentSerializer LEGACY =
        LegacyComponentSerializer.legacySection();

    private final DeathSystemPlugin plugin;
    private final Player player;
    private final double maxHp;
    private int secondsLeft;

    public CountdownTask(DeathSystemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.secondsLeft = plugin.getPluginConfig().maxWaitSeconds;

        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        this.maxHp = attr != null ? Math.max(1.0, attr.getValue()) : 20.0;
    }

    @Override
    public void run() {
        if (!player.isOnline() || !plugin.getDownedPlayerManager().isDown(player)) {
            this.cancel();
            return;
        }

        var state = plugin.getDownedPlayerManager().getState(player);
        if (state == null) {
            this.cancel();
            return;
        }
        state.timeLeft = secondsLeft;

        String color = secondsLeft > 30 ? "§e" : (secondsLeft > 10 ? "§6" : "§c");
        int holdSeconds = plugin.getPluginConfig().surrenderHoldSeconds;

        Title title = Title.title(
            LEGACY.deserialize("§c☠ Вы ранены!"),
            LEGACY.deserialize(color + secondsLeft + " сек §8| §7Зажмите §fSNEAK §7на "
                + holdSeconds + " сек для сдачи"),
            Title.Times.times(
                Duration.ofMillis(0),
                Duration.ofMillis(1200),
                Duration.ofMillis(300)
            )
        );
        player.showTitle(title);

        double hpPercent = (player.getHealth() / maxHp) * 100;
        String bar = buildHpBar(hpPercent);
        player.sendActionBar(LEGACY.deserialize(
            "§4❤ " + bar + " §c" + String.format("%.1f", player.getHealth()) + " HP"
        ));

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
