package dev.anonhu.deathsystem.listeners;

import dev.anonhu.deathsystem.DeathSystemPlugin;
import dev.anonhu.deathsystem.managers.DownedState;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Управляет движением и ползанием в downed-состоянии,
 * а также самоподъёмом через удержание SNEAK.
 */
public class PlayerMoveListener implements Listener {

    private final DeathSystemPlugin plugin;

    public PlayerMoveListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    //  Движение / ползание
    // =========================================================

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        DownedState state = plugin.getDownedPlayerManager().getState(player);
        if (state == null) return;

        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;

        // Запрещаем прыжок
        if (to.getY() > from.getY() + 0.05) {
            event.setCancelled(true);
            return;
        }

        boolean movedBlock = from.getBlockX() != to.getBlockX()
                          || from.getBlockZ() != to.getBlockZ();

        if (movedBlock && !state.isCrawling) {
            state.isCrawling = true;
            applyCrawlSpeed(player);
        } else if (!movedBlock && state.isCrawling) {
            state.isCrawling = false;
            resetSpeed(player);
        }
    }

    // =========================================================
    //  SNEAK → самоподъём
    // =========================================================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        DownedState state = plugin.getDownedPlayerManager().getState(player);
        if (state == null) return;

        if (event.isSneaking()) {
            startSurrenderCountdown(player, state);
        } else {
            cancelSurrenderCountdown(state);
            player.sendActionBar(Component.empty());
        }
    }

    // =========================================================
    //  Самоподъём: внутренняя логика
    // =========================================================

    private void startSurrenderCountdown(Player player, DownedState state) {
        if (state.surrenderTask != null) return; // уже идёт

        state.sneakHeldSeconds = 0;
        int maxSeconds = plugin.getPluginConfig().surrenderHoldSeconds;

        state.surrenderTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()
                        || !plugin.getDownedPlayerManager().isDown(player)
                        || !player.isSneaking()) {
                    cancelSurrenderCountdown(state);
                    player.sendActionBar(Component.empty());
                    return;
                }

                state.sneakHeldSeconds++;
                sendSurrenderBar(player, state.sneakHeldSeconds, maxSeconds);

                // Нарастающий звук
                float pitch = 0.5f + (state.sneakHeldSeconds * 0.25f);
                player.playSound(player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, pitch);

                if (state.sneakHeldSeconds >= maxSeconds) {
                    triggerSelfRevive(player, state);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void cancelSurrenderCountdown(DownedState state) {
        if (state.surrenderTask != null && !state.surrenderTask.isCancelled()) {
            state.surrenderTask.cancel();
        }
        state.surrenderTask     = null;
        state.sneakHeldSeconds  = 0;
    }

    private void triggerSelfRevive(Player player, DownedState state) {
        cancelSurrenderCountdown(state);

        // Вспышка частиц
        player.getWorld().spawnParticle(
            Particle.EXPLOSION,
            player.getLocation().add(0, 0.5, 0),
            8, 0.3, 0.3, 0.3, 0.01);
        player.playSound(player.getLocation(),
            Sound.ENTITY_PLAYER_HURT, 0.5f, 0.6f);

        // Небольшая задержка перед телепортом — эффект успевает сыграть
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.getDownedPlayerManager().isDown(player)) {
                plugin.getDownedPlayerManager().revivePlayer(player, null, 0);
            }
        }, 10L);
    }

    private void sendSurrenderBar(Player player, int current, int max) {
        int filled = (int) Math.round((double) current / max * 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "\u00a7c\u2588" : "\u00a78\u2591");
        }
        player.sendActionBar(Component.text(
            "\u00a77Сдаётесь... " + bar
            + " \u00a7c" + current + "\u00a78/\u00a7c" + max + " сек"
            + "  \u00a78(\u00a77отпустите SNEAK чтобы отменить\u00a78)"
        ));
    }

    // =========================================================
    //  Скорость
    // =========================================================

    private void applyCrawlSpeed(Player player) {
        var attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.setBaseValue(0.1 * plugin.getPluginConfig().crawlSpeedModifier);
    }

    private void resetSpeed(Player player) {
        var attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null) attr.setBaseValue(0.1);
    }

    /** Вызывается из DownedPlayerManager при подъёме/смерти. */
    public void cleanup(Player player) {
        DownedState state = plugin.getDownedPlayerManager().getState(player);
        if (state != null) cancelSurrenderCountdown(state);
        resetSpeed(player);
    }
}
