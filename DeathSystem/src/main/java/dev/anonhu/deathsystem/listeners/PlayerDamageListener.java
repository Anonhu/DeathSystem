package dev.anonhu.deathsystem.listeners;

import dev.anonhu.deathsystem.DeathSystemPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Перехватывает смерть игрока — перенаправляет в downed-состояние.
 * HIGHEST — чтобы быть последними и не конфликтовать с EssentialsX и др.
 */
public class PlayerDamageListener implements Listener {

    private final DeathSystemPlugin plugin;

    public PlayerDamageListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var mgr = plugin.getDownedPlayerManager();

        // — Игрок уже лежит: блокируем все уроны — кровотечение управляет BleedTask
        if (mgr.isDown(player)) {
            event.setCancelled(true);
            return;
        }

        // — Игрок в god mode / invulnerable — пропускаем
        if (player.isInvulnerable()) return;

        // — bypass-пермиссия — операторы могут умирать обычно
        if (player.hasPermission("deathsystem.bypass")) return;

        // — Урон убьёт игрока?
        double finalHp = player.getHealth() - event.getFinalDamage();
        if (finalHp > 0) return; // ещё живой

        // — Отменяем смерть и переводим в downed
        event.setCancelled(true);
        mgr.downPlayer(player);
    }
}
