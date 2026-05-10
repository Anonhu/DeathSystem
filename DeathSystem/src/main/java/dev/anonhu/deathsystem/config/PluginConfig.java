package dev.anonhu.deathsystem.config;

import dev.anonhu.deathsystem.DeathSystemPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Читает config.yml один раз при старте / перезагрузке.
 * Все поля публичны и финальны — доступ без геттеров.
 */
public final class PluginConfig {

    // ── Downed state ─────────────────────────────────────────
    public final int    maxWaitSeconds;
    public final double bleedRateIdle;
    public final double bleedRateCrawling;
    public final double crawlSpeedModifier;

    // ── Revival ───────────────────────────────────────────────
    public final int radiusBlocks;
    public final int surrenderHoldSeconds;

    public final int    enchantedAppleHpPercent;
    public final int    enchantedAppleChannelSeconds;

    public final int    healingPotionHpPercent;
    public final int    healingPotionChannelSeconds;

    public final int    bandageHpPercent;
    public final int    bandageChannelSeconds;

    public final int    defibrillatorHpPercent;
    public final int    defibrillatorChannelSeconds;

    // ── Debuffs after revival ─────────────────────────────────
    public final int selfReviveWeaknessLevel;
    public final int selfReviveSlownessLevel;
    public final int selfReviveDurationSeconds;

    public final int allyReviveWeaknessLevel;
    public final int allyReviveDurationSeconds;

    // ── Chat distortion ───────────────────────────────────────
    public final boolean chatDistortionEnabled;
    public final int     chatDistortEveryNChars;

    // ── Announcements ─────────────────────────────────────────
    public final int     localRadiusBlocks;

    // ── Custom items ──────────────────────────────────────────
    public final int bandageModelData;
    public final int defibrillatorModelData;
    public final int defibrillatorMaxUses;

    // ─────────────────────────────────────────────────────────

    public PluginConfig(DeathSystemPlugin plugin) {
        // Убеждаемся что defaults из jar загружены
        plugin.saveDefaultConfig();
        FileConfiguration cfg = plugin.getConfig();

        // Downed state
        maxWaitSeconds      = cfg.getInt("downed-state.max-wait-seconds",    60);
        bleedRateIdle       = cfg.getDouble("downed-state.bleed-rate-idle",   0.5);
        bleedRateCrawling   = cfg.getDouble("downed-state.bleed-rate-crawling", 1.5);
        crawlSpeedModifier  = cfg.getDouble("downed-state.crawl-speed-modifier", 0.15);

        // Revival
        radiusBlocks            = cfg.getInt("revival.radius-blocks",           2);
        surrenderHoldSeconds    = cfg.getInt("revival.surrender-hold-seconds",  3);

        enchantedAppleHpPercent      = cfg.getInt("revival.enchanted-apple.hp-percent",      50);
        enchantedAppleChannelSeconds = cfg.getInt("revival.enchanted-apple.channel-seconds",  5);

        healingPotionHpPercent      = cfg.getInt("revival.healing-potion.hp-percent",      20);
        healingPotionChannelSeconds = cfg.getInt("revival.healing-potion.channel-seconds",  5);

        bandageHpPercent      = cfg.getInt("revival.bandage.hp-percent",      5);
        bandageChannelSeconds = cfg.getInt("revival.bandage.channel-seconds", 3);

        defibrillatorHpPercent      = cfg.getInt("revival.defibrillator.hp-percent",      30);
        defibrillatorChannelSeconds = cfg.getInt("revival.defibrillator.channel-seconds",  0);

        // Debuffs
        selfReviveWeaknessLevel   = cfg.getInt("debuffs-after-revival.self-revive.weakness-level",  2);
        selfReviveSlownessLevel   = cfg.getInt("debuffs-after-revival.self-revive.slowness-level",  1);
        selfReviveDurationSeconds = cfg.getInt("debuffs-after-revival.self-revive.duration-seconds", 60);

        allyReviveWeaknessLevel   = cfg.getInt("debuffs-after-revival.ally-revive.weakness-level",  1);
        allyReviveDurationSeconds = cfg.getInt("debuffs-after-revival.ally-revive.duration-seconds", 30);

        // Chat distortion
        chatDistortionEnabled  = cfg.getBoolean("chat-distortion.enabled",             true);
        chatDistortEveryNChars = cfg.getInt("chat-distortion.distort-every-n-chars",   4);

        // Announcements
        localRadiusBlocks = cfg.getInt("announcements.local-radius-blocks", 50);

        // Custom items
        bandageModelData        = cfg.getInt("custom-items.bandage.custom-model-data",         1001);
        defibrillatorModelData  = cfg.getInt("custom-items.defibrillator.custom-model-data",   1002);
        defibrillatorMaxUses    = cfg.getInt("custom-items.defibrillator.max-uses",               3);
    }

    // ── Хелперы ───────────────────────────────────────────────

    /**
     * Возвращает HP для подъёма в абсолютном значении (0..maxHp).
     * @param percent  процент HP из конфига (0-100)
     * @param maxHp    максимальный HP игрока (обычно 20.0)
     */
    public static double percentToHp(int percent, double maxHp) {
        return maxHp * (percent / 100.0);
    }

    /**
     * Конвертирует секунды в тики Bukkit (1 сек = 20 тиков).
     */
    public static long secondsToTicks(int seconds) {
        return seconds * 20L;
    }

    /**
     * Конвертирует секунды в тики для PotionEffect (1 сек = 20 тиков).
     */
    public static int secondsToPotionTicks(int seconds) {
        return seconds * 20;
    }
}
