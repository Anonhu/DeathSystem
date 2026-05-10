package dev.yourname.deathsystem.listeners;

import dev.yourname.deathsystem.DeathSystemPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveListener implements Listener {

    private final DeathSystemPlugin plugin;
    private final Map<UUID, Integer> sneakSeconds = new HashMap<>();
    private final Map<UUID, BukkitRunnable> surrenderTasks = new HashMap<>();
    private static final int SURRENDER_SECONDS = 3;

    public PlayerMoveListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDownedPlayerManager().isDown(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        boolean movedBlock = from.getBlockX() != to.getBlockX()
                          || from.getBlockZ() != to.getBlockZ();

        if (to.getY() > from.getY() + 0.05) {
            event.setCancelled(true);
            return;
        }

        var state = plugin.getDownedPlayerManager().getState(player);
        if (state == null) return;

        if (movedBlock) {
            if (!state.isCrawling) {
                state.isCrawling = true;
                applyCrawlSpeed(player);
            }
        } else {
            if (state.isCrawling) {
                state.isCrawling = false;
                resetSpeed(player);
            }
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDownedPlayerManager().isDown(player)) return;

        if (event.isSneaking()) {
            startSurrenderCountdown(player);
        } else {
            cancelSurrenderCountdown(player);
            clearSneakProgress(player);
        }
    }

    private void startSurrenderCountdown(Player player) {
        if (surrenderTasks.containsKey(player.getUniqueId())) return;

        int holdSeconds = plugin.getPluginConfig().surrenderHoldSeconds;
        sneakSeconds.put(player.getUniqueId(), 0);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()
                        || !plugin.getDownedPlayerManager().isDown(player)
                        || !player.isSneaking()) {
                    cancelSurrenderCountdown(player);
                    clearSneakProgress(player);
                    return;
                }

                int secs = sneakSeconds.merge(player.getUniqueId(), 1, Integer::sum);
                int holdSeconds = plugin.getPluginConfig().surrenderHoldSeconds;

                sendSurrenderBar(player, secs, holdSeconds);

                float pitch = 0.5f + (secs * 0.25f);
                player.playSound(player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, pitch);

                if (secs >= holdSeconds) {
                    cancelSurrenderCountdown(player);
                    triggerSelfRevive(player);
                }
            }
        };

        surrenderTasks.put(player.getUniqueId(), task);
        task.runTaskTimer(plugin, 20L, 20L);
    }

    private void triggerSelfRevive(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(
            Particle.EXPLOSION,
            loc.clone().add(0, 0.5, 0),
            8, 0.3, 0.3, 0.3, 0.01
        );

        player.playSound(loc, Sound.ENTITY_PLAYER_HURT, 0.5f, 0.6f);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.getDownedPlayerManager().isDown(player)) {
                plugin.getDownedPlayerManager().revivePlayer(player, null, 0);
            }
        }, 10L);
    }

    public void cleanup(Player player) {
        cancelSurrenderCountdown(player);
        clearSneakProgress(player);
        resetSpeed(player);
    }

    private void cancelSurrenderCountdown(Player player) {
        BukkitRunnable task = surrenderTasks.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) task.cancel();
    }

    private void clearSneakProgress(Player player) {
        sneakSeconds.remove(player.getUniqueId());
        player.sendActionBar(Component.empty());
    }

    private void sendSurrenderBar(Player player, int current, int holdSeconds) {
        int filled = (int) Math.round((double) current / holdSeconds * 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "§c█" : "§8░");
        }
        player.sendActionBar(Component.text(
            "§7Сдаётесь... " + bar + " §c" + current + "§8/§c" + holdSeconds + " сек"
            + "  §8(§7отпустите §8SNEAK §7чтобы отменить§8)"
        ));
    }

    private void applyCrawlSpeed(Player player) {
        var attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        double base = 0.1;
        double crawl = base * plugin.getPluginConfig().crawlSpeedModifier;
        attr.setBaseValue(crawl);
    }

    private void resetSpeed(Player player) {
        var attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null) attr.setBaseValue(0.1);
    }
}
