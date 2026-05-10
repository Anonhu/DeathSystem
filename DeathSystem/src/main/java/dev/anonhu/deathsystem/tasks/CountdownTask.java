package dev.anonhu.deathsystem.tasks;

import dev.anonhu.deathsystem.DeathSystemPlugin;
import dev.anonhu.deathsystem.managers.DownedState;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Обратный отсчёт + ActionBar UI.
 * Каждую секунду уменьшает timeLeft.
 * При 0 — форсирует самоподъём через revivePlayer(null).
 */
public class CountdownTask extends BukkitRunnable {

    private final DeathSystemPlugin plugin;
    private final DownedState       state;

    public CountdownTask(DeathSystemPlugin plugin, DownedState state) {
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

        state.timeLeft--;

        // Обновляем ActionBar только если не идёт удержание SNEAK
        if (state.sneakHeldSeconds == 0) {
            sendActionBar(player);
        }

        if (state.timeLeft <= 0) {
            // Таймер вышел — принудительный самоподъём
            plugin.getDownedPlayerManager().revivePlayer(player, null, 0);
            cancel();
        }
    }

    private void sendActionBar(var player) {
        int max     = plugin.getPluginConfig().maxWaitSeconds;
        int left    = state.timeLeft;
        double hp   = player.getHealth();

        // Прогресс-бар времени
        int filled  = (int) Math.round((double) left / max * 10);
        StringBuilder timeBar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            timeBar.append(i < filled ? "\u00a7e\u2588" : "\u00a78\u2591");
        }

        // Прогресс-бар HP
        int hpFilled = (int) Math.round(hp / 20.0 * 10);
        StringBuilder hpBar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            String color = (i < 3) ? "\u00a74" : "\u00a7c"; // \u043aрасный → тёмно-красный при низком HP
            hpBar.append(i < hpFilled ? color + "\u2665" : "\u00a78\u2665");
        }

        String bar = "\u00a7c\u2665 " + hpBar
            + "  \u00a77" + left + "\u00a78/\u00a77" + max + "\u00a78с  "
            + timeBar
            + (state.isCrawling ? "  \u00a7cПолзёт!" : "");

        player.sendActionBar(Component.text(bar));
    }
}
