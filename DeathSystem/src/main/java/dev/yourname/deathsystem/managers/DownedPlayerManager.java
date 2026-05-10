package dev.yourname.deathsystem.managers;

import dev.yourname.deathsystem.DeathSystemPlugin;
import dev.yourname.deathsystem.api.events.PlayerDownedEvent;
import dev.yourname.deathsystem.api.events.PlayerRevivedEvent;
import dev.yourname.deathsystem.listeners.PlayerMoveListener;
import dev.yourname.deathsystem.tasks.BleedTask;
import dev.yourname.deathsystem.tasks.CountdownTask;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DownedPlayerManager {

    private final DeathSystemPlugin plugin;
    private final Map<UUID, DownedState> downedPlayers = new ConcurrentHashMap<>();
    private PlayerMoveListener moveListener;

    public DownedPlayerManager(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public void setMoveListener(PlayerMoveListener moveListener) {
        this.moveListener = moveListener;
    }

    public void downPlayer(Player player, Player killer) {
        if (isDown(player)) return;

        PlayerDownedEvent event = new PlayerDownedEvent(player, killer);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        DownedState state = new DownedState(player.getLocation());
        downedPlayers.put(player.getUniqueId(), state);

        player.setHealth(20.0);
        applyDownedEffects(player);
        player.setSneaking(true);

        state.bleedTask = new BleedTask(plugin, player);
        state.bleedTask.runTaskTimer(plugin, 0L, 20L);

        state.countdownTask = new CountdownTask(plugin, player);
        state.countdownTask.runTaskTimer(plugin, 0L, 20L);

        announceDown(player);

        plugin.getLogger().info(player.getName() + " is now downed.");
    }

    public void revivePlayer(Player player, Player reviver, double hpPercent) {
        DownedState state = downedPlayers.get(player.getUniqueId());
        if (state == null) return;

        cancelTasks(state);
        downedPlayers.remove(player.getUniqueId());

        removeDownedEffects(player);
        player.setSneaking(false);

        if (moveListener != null) {
            moveListener.cleanup(player);
        }

        double maxHp = Objects.requireNonNull(
            player.getAttribute(Attribute.MAX_HEALTH)
        ).getValue();
        double targetHp = maxHp * (hpPercent / 100.0);
        player.setHealth(Math.max(1.0, targetHp));

        boolean selfRevive = (reviver == null);
        applyRevivalDebuffs(player, selfRevive);

        if (selfRevive) {
            dropInventory(player);

            Location spawnLoc = player.getBedSpawnLocation() != null
                ? player.getBedSpawnLocation()
                : player.getWorld().getSpawnLocation();

            player.teleport(spawnLoc);
            player.sendMessage("§7Вы с трудом поднялись... но вещи остались позади.");
        } else {
            reviver.sendMessage("§a✔ Вы спасли " + player.getName() + "!");
            player.sendMessage("§aВас поднял " + reviver.getName() + "!");

            plugin.getServer().getScheduler().runTask(plugin, () ->
                dev.yourname.deathsystem.utils.ParticleUtils.playReviveEffect(player.getLocation())
            );
        }

        PlayerRevivedEvent event = new PlayerRevivedEvent(player, reviver,
            selfRevive ? null : plugin.getRevivalManager().getLastReviveItem(reviver));
        plugin.getServer().getPluginManager().callEvent(event);
    }

    public void killDowned(Player player) {
        DownedState state = downedPlayers.get(player.getUniqueId());
        if (state == null) return;

        cancelTasks(state);
        downedPlayers.remove(player.getUniqueId());

        removeDownedEffects(player);
        player.setSneaking(false);

        if (moveListener != null) {
            moveListener.cleanup(player);
        }

        player.setHealth(0.0);
    }

    public void killAllDowned() {
        new ArrayList<>(downedPlayers.keySet()).forEach(uuid -> {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) killDowned(player);
        });
    }

    public boolean isDown(Player player) {
        return downedPlayers.containsKey(player.getUniqueId());
    }

    public DownedState getState(Player player) {
        return downedPlayers.get(player.getUniqueId());
    }

    private void applyDownedEffects(Player player) {
        int duration = 999999;
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 3, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 3, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 128, false, false));
    }

    private void removeDownedEffects(Player player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }

    private void applyRevivalDebuffs(Player player, boolean selfRevive) {
        var cfg = plugin.getPluginConfig();
        int ticks;
        if (selfRevive) {
            ticks = cfg.selfReviveDurationSeconds * 20;
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,
                ticks, cfg.selfReviveWeaknessLevel - 1, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                ticks, cfg.selfReviveSlownessLevel - 1, false, true));
        } else {
            ticks = cfg.allyReviveDurationSeconds * 20;
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,
                ticks, cfg.allyReviveWeaknessLevel - 1, false, true));
        }
    }

    private void dropInventory(Player player) {
        Location loc = player.getLocation();
        Arrays.stream(player.getInventory().getContents())
            .filter(Objects::nonNull)
            .forEach(item -> loc.getWorld().dropItemNaturally(loc, item));
        player.getInventory().clear();
    }

    private void announceDown(Player player) {
        Location loc = player.getLocation();
        String msg = String.format("§c⚠ [%s] §7ранен и нуждается в помощи! §8(X: %d, Z: %d)",
            player.getName(),
            loc.getBlockX(),
            loc.getBlockZ()
        );

        int radius = plugin.getPluginConfig().localAnnouncementRadius;
        player.getWorld().getPlayers().stream()
            .filter(p -> p.getLocation().distance(loc) <= radius)
            .filter(p -> !p.equals(player))
            .forEach(p -> p.sendMessage(msg));
    }

    private void cancelTasks(DownedState state) {
        if (state.bleedTask != null) state.bleedTask.cancel();
        if (state.countdownTask != null) state.countdownTask.cancel();
    }
}
