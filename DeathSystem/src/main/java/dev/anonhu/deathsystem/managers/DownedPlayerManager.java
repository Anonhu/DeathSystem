package dev.anonhu.deathsystem.managers;

import dev.anonhu.deathsystem.DeathSystemPlugin;
import dev.anonhu.deathsystem.api.events.PlayerDownedEvent;
import dev.anonhu.deathsystem.api.events.PlayerRevivedEvent;
import dev.anonhu.deathsystem.config.PluginConfig;
import dev.anonhu.deathsystem.tasks.BleedTask;
import dev.anonhu.deathsystem.tasks.CountdownTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ядро плагина. Управляет всеми игроками в состоянии downed.
 *
 * <ul>
 *   <li>ConcurrentHashMap — безопасно читается из async (напр. AsyncChatEvent)</li>
 *   <li>Пишем только из sync Bukkit-потока</li>
 * </ul>
 */
public final class DownedPlayerManager {

    private final DeathSystemPlugin plugin;
    private final PluginConfig      cfg;

    /** UUID → состояние. ConcurrentHashMap — async-безопасно. */
    private final Map<UUID, DownedState> downedPlayers = new ConcurrentHashMap<>();

    public DownedPlayerManager(DeathSystemPlugin plugin) {
        this.plugin = plugin;
        this.cfg    = plugin.getPluginConfig();
    }

    // =========================================================
    //  Публичное API
    // =========================================================

    /** true = игрок сейчас находится в downed-состоянии. Безопасно из async. */
    public boolean isDown(Player player) {
        return downedPlayers.containsKey(player.getUniqueId());
    }

    /** Возвращает состояние или null. Безпасно из async. */
    public DownedState getState(Player player) {
        return downedPlayers.get(player.getUniqueId());
    }

    /** Неизменяемый список всех upavshikh (для внешних инспекторов). */
    public Collection<DownedState> getAllDowned() {
        return Collections.unmodifiableCollection(downedPlayers.values());
    }

    // =========================================================
    //  Упасть игрока
    // =========================================================

    /**
     * Переводит игрока в downed-состояние.
     * Вызывается исключительно из sync Bukkit-потока (PlayerDamageListener).
     *
     * @param player игрок, который должен упасть
     */
    public void downPlayer(Player player) {
        if (isDown(player)) return; // защита от дублирования

        // — Создаём состояние
        DownedState state = new DownedState(player, cfg.maxWaitSeconds);
        downedPlayers.put(player.getUniqueId(), state);

        // — Устанавливаем HP на 1 (не 0! — игрок должен быть живым)
        player.setHealth(1.0);

        // — Внешний вид: sneaking = поза лёжа
        player.setSneaking(true);

        // — Потион-эффекты для имитации ранения
        applyDownedEffects(player);

        // — Title с объявлением
        showDownedTitle(player);

        // — Звук падения
        player.playSound(player.getLocation(),
            Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);

        // — Запускаем задачи
        state.bleedTask     = new BleedTask(plugin, state).runTaskTimer(plugin, 20L, 20L);
        state.countdownTask = new CountdownTask(plugin, state).runTaskTimer(plugin, 20L, 20L);

        // — Оповещаем окружающих
        broadcastNearby(player, state,
            "§c" + player.getName() + " §7нуждается в помощи!");

        // — Вызываем кастомное событие API
        Bukkit.getPluginManager().callEvent(new PlayerDownedEvent(player));
    }

    // =========================================================
    //  Воскрешение игрока
    // =========================================================

    /**
     * Поднимает игрока.
     *
     * @param player    кто поднимается
     * @param reviver   кто поднял (null = самоподъём)
     * @param hpPercent сколько % HP дать (0 = самоподъём — спавн без HP)
     */
    public void revivePlayer(Player player, Player reviver, int hpPercent) {
        DownedState state = downedPlayers.remove(player.getUniqueId());
        if (state == null) return;

        state.cancelAllTasks();

        boolean isSelfRevive = (reviver == null);

        if (isSelfRevive) {
            // ─ Самоподъём: дроп вещей + телепорт на спавн
            dropInventory(player);
            Location spawn = player.getWorld().getSpawnLocation();
            player.teleport(spawn);
            applyDebuffs(player, true);
            player.sendMessage("§7Вы с трудом поднялись... но вещи остались позади.");
        } else {
            // ─ Союзник поднял
            double maxHp = getMaxHp(player);
            double hp    = PluginConfig.percentToHp(hpPercent, maxHp);
            player.setHealth(Math.max(1.0, Math.min(hp, maxHp)));
            applyDebuffs(player, false);
            player.sendMessage("§aВас поднял §f" + reviver.getName() + "§a!");
            reviver.sendMessage("§aВы подняли §f" + player.getName() + "§a!");
        }

        // ─ Снимаем все downed-эффекты
        removeDownedEffects(player);
        player.setSneaking(false);

        // ─ Звук + Title подъёма
        player.playSound(player.getLocation(),
            Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        showRevivedTitle(player, isSelfRevive);

        // ─ API-событие
        Bukkit.getPluginManager().callEvent(
            new PlayerRevivedEvent(player, reviver));
    }

    // =========================================================
    //  Смерть в downed-состоянии
    // =========================================================

    /**
     * Убивает игрока пока он лежит — природная смерть.
     * keepInventory уважается (в отличие от самоподъёма).
     */
    public void killDowned(Player player) {
        DownedState state = downedPlayers.remove(player.getUniqueId());
        if (state == null) return;

        state.cancelAllTasks();
        removeDownedEffects(player);
        player.setSneaking(false);

        // Стандартная смерть — уважает keepInventory
        player.setHealth(0.0);
    }

    /**
     * Убивает всех downed-игроков. Вызывается в onDisable().
     */
    public void killAllDowned() {
        // Снимаем снимок чтобы не смодифицировать во время итерации
        new ArrayList<>(downedPlayers.keySet()).forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) killDowned(p);
        });
    }

    // =========================================================
    //  Эффекты
    // =========================================================

    private void applyDownedEffects(Player player) {
        // BLINDNESS — пульсирующая
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.BLINDNESS,
            Integer.MAX_VALUE, 0, false, false, false));
        // SLOWNESS IV — ползание контролирует MoveListener
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS,
            Integer.MAX_VALUE, 4, false, false, false));
        // WEAKNESS — чтобы не мог атаковать
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.WEAKNESS,
            Integer.MAX_VALUE, 4, false, false, false));
    }

    private void removeDownedEffects(Player player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
    }

    private void applyDebuffs(Player player, boolean selfRevive) {
        int ticks;
        if (selfRevive) {
            ticks = PluginConfig.secondsToPotionTicks(cfg.selfReviveDurationSeconds);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS, ticks,
                cfg.selfReviveWeaknessLevel - 1,   // amplifier = level - 1
                false, true, true));
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, ticks,
                cfg.selfReviveSlownessLevel - 1,
                false, true, true));
        } else {
            ticks = PluginConfig.secondsToPotionTicks(cfg.allyReviveDurationSeconds);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS, ticks,
                cfg.allyReviveWeaknessLevel - 1,
                false, true, true));
        }
    }

    // =========================================================
    //  Утилиты
    // =========================================================

    /**
     * Дропает весь инвентарь на месте вщави gamerule keepInventory.
     * Используется только при самоподъёме — наказание.
     */
    private void dropInventory(Player player) {
        Location loc = player.getLocation();
        World world  = loc.getWorld();
        if (world == null) return;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            Item dropped = world.dropItemNaturally(loc, item);
            // Помечаем чтобы нельзя было сразу собрать обратно
            dropped.setPickupDelay(40); // 2 секунды
        }
        player.getInventory().clear();
    }

    private double getMaxHp(Player player) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }

    /**
     * Оповещает игроков в радиусе оповещения.
     * Сам упавший сообщение не получает.
     */
    private void broadcastNearby(Player downed, DownedState state, String message) {
        double radiusSq = Math.pow(cfg.localRadiusBlocks, 2);
        Location loc = state.downedAt;

        downed.getWorld().getPlayers().stream()
            .filter(p -> !p.equals(downed))
            .filter(p -> p.getWorld().equals(downed.getWorld()))
            .filter(p -> p.getLocation().distanceSquared(loc) <= radiusSq)
            .forEach(p -> p.sendMessage(message));
    }

    // =========================================================
    //  Title / UI
    // =========================================================

    private void showDownedTitle(Player player) {
        player.showTitle(Title.title(
            Component.text("Вы ранены!", NamedTextColor.RED),
            Component.text("Нуждаетесь в помощи...", NamedTextColor.GRAY),
            Title.Times.times(
                Duration.ofMillis(300),
                Duration.ofSeconds(3),
                Duration.ofMillis(500))
        ));
    }

    private void showRevivedTitle(Player player, boolean selfRevive) {
        Component sub = selfRevive
            ? Component.text("Вы сдались. Вещи выпали...", NamedTextColor.GRAY)
            : Component.text("Союзник поднял вас!", NamedTextColor.GREEN);

        player.showTitle(Title.title(
            Component.text(selfRevive ? "Поднялись" : "Спасены!",
                selfRevive ? NamedTextColor.YELLOW : NamedTextColor.GREEN),
            sub,
            Title.Times.times(
                Duration.ofMillis(200),
                Duration.ofSeconds(2),
                Duration.ofMillis(500))
        ));
    }
}
