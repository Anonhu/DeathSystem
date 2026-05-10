package dev.anonhu.deathsystem.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Хранит всё состояние одного «упавшего» игрока.
 * Живёт в DownedPlayerManager.downedPlayers (ConcurrentHashMap).
 *
 * Поля намеренно package-private / public — класс используется
 * только внутри пакета managers и дружественными менеджерами.
 */
public final class DownedState {

    // ── Ссылка на игрока ──────────────────────────────────────
    public final Player player;

    // ── Позиция падения (для логов и аннонсов) ───────────────
    public final Location downedAt;

    // ── Таймер ───────────────────────────────────────────────
    /** Сколько секунд осталось до принудительного самоподъёма */
    public volatile int timeLeft;

    // ── Кровотечение ─────────────────────────────────────────
    /** true = игрок ползёт (движение блоков), false = лежит */
    public volatile boolean isCrawling = false;

    // ── Задачи Bukkit (отменяются при подъёме / смерти) ──────
    /** Тик кровотечения — каждые 20 тиков (1 сек) */
    public BukkitTask bleedTask;

    /** Обратный отсчёт + Title/ActionBar UI */
    public BukkitTask countdownTask;

    // ── Воскрешение союзником ─────────────────────────────────
    /** Кто сейчас воскрешает (null = никто) */
    public volatile Player reviver = null;

    /** Прогресс канала воскрешения (тики) */
    public volatile int revivalProgress = 0;

    /** Задача прогресс-бара воскрешения */
    public BukkitTask revivalChannelTask = null;

    // ── Самоподъём (SNEAK) ───────────────────────────────────
    /** Сколько секунд игрок удерживает SNEAK */
    public volatile int sneakHeldSeconds = 0;

    /** Задача отсчёта удержания SNEAK */
    public BukkitTask surrenderTask = null;

    // ─────────────────────────────────────────────────────────

    public DownedState(Player player, int maxWaitSeconds) {
        this.player    = player;
        this.downedAt  = player.getLocation().clone();
        this.timeLeft  = maxWaitSeconds;
    }

    // ── Утилиты ───────────────────────────────────────────────

    /** Отменяет ВСЕ активные задачи этого состояния. */
    public void cancelAllTasks() {
        cancelTask(bleedTask);
        cancelTask(countdownTask);
        cancelTask(revivalChannelTask);
        cancelTask(surrenderTask);
    }

    private void cancelTask(BukkitTask task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /** true = кто-то уже проводит канал воскрешения */
    public boolean isBeingRevived() {
        return reviver != null;
    }

    /**
     * Вычисляет процент оставшегося времени (0.0 .. 1.0).
     * Используется в ActionBar-прогрессбаре.
     */
    public double timeProgress(int maxWaitSeconds) {
        return (double) timeLeft / maxWaitSeconds;
    }

    @Override
    public String toString() {
        return "DownedState{"
            + "player=" + player.getName()
            + ", timeLeft=" + timeLeft
            + ", isCrawling=" + isCrawling
            + ", isBeingRevived=" + isBeingRevived()
            + '}';
    }
}
