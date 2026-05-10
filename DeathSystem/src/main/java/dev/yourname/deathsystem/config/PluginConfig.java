package dev.yourname.deathsystem.config;

import dev.yourname.deathsystem.DeathSystemPlugin;

public class PluginConfig {

    private final DeathSystemPlugin plugin;

    public int maxWaitSeconds;
    public double bleedRateIdle;
    public double bleedRateCrawling;
    public double crawlSpeedModifier;

    public int revivalRadiusBlocks;
    public int enchantedAppleHpPercent;
    public int enchantedAppleChannelSeconds;
    public int healingPotionHpPercent;
    public int healingPotionChannelSeconds;
    public int bandageHpPercent;
    public int bandageChannelSeconds;
    public int defibrillatorHpPercent;
    public int defibrillatorChannelSeconds;

    public int selfReviveWeaknessLevel;
    public int selfReviveSlownessLevel;
    public int selfReviveDurationSeconds;
    public int allyReviveWeaknessLevel;
    public int allyReviveDurationSeconds;

    public boolean chatDistortionEnabled;
    public int chatDistortEveryNChars;

    public int localAnnouncementRadius;
    public boolean dynmapMarkerEnabled;

    public int bandageModelData;
    public int defibrillatorModelData;
    public int defibrillatorMaxUses;

    public int surrenderHoldSeconds;

    public PluginConfig(DeathSystemPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        var cfg = plugin.getConfig();

        maxWaitSeconds = cfg.getInt("downed-state.max-wait-seconds", 60);
        bleedRateIdle = cfg.getDouble("downed-state.bleed-rate-idle", 0.5);
        bleedRateCrawling = cfg.getDouble("downed-state.bleed-rate-crawling", 1.5);
        crawlSpeedModifier = cfg.getDouble("downed-state.crawl-speed-modifier", 0.15);

        revivalRadiusBlocks = cfg.getInt("revival.radius-blocks", 2);
        enchantedAppleHpPercent = cfg.getInt("revival.enchanted-apple.hp-percent", 50);
        enchantedAppleChannelSeconds = cfg.getInt("revival.enchanted-apple.channel-seconds", 5);
        healingPotionHpPercent = cfg.getInt("revival.healing-potion.hp-percent", 20);
        healingPotionChannelSeconds = cfg.getInt("revival.healing-potion.channel-seconds", 5);
        bandageHpPercent = cfg.getInt("revival.bandage.hp-percent", 5);
        bandageChannelSeconds = cfg.getInt("revival.bandage.channel-seconds", 3);
        defibrillatorHpPercent = cfg.getInt("revival.defibrillator.hp-percent", 30);
        defibrillatorChannelSeconds = cfg.getInt("revival.defibrillator.channel-seconds", 0);

        selfReviveWeaknessLevel = cfg.getInt("debuffs-after-revival.self-revive.weakness-level", 2);
        selfReviveSlownessLevel = cfg.getInt("debuffs-after-revival.self-revive.slowness-level", 1);
        selfReviveDurationSeconds = cfg.getInt("debuffs-after-revival.self-revive.duration-seconds", 60);
        allyReviveWeaknessLevel = cfg.getInt("debuffs-after-revival.ally-revive.weakness-level", 1);
        allyReviveDurationSeconds = cfg.getInt("debuffs-after-revival.ally-revive.duration-seconds", 30);

        chatDistortionEnabled = cfg.getBoolean("chat-distortion.enabled", true);
        chatDistortEveryNChars = cfg.getInt("chat-distortion.distort-every-n-chars", 4);

        localAnnouncementRadius = cfg.getInt("announcements.local-radius-blocks", 50);
        dynmapMarkerEnabled = cfg.getBoolean("announcements.dynmap-marker", false);

        bandageModelData = cfg.getInt("custom-items.bandage.custom-model-data", 1001);
        defibrillatorModelData = cfg.getInt("custom-items.defibrillator.custom-model-data", 1002);
        defibrillatorMaxUses = cfg.getInt("custom-items.defibrillator.max-uses", 3);

        surrenderHoldSeconds = cfg.getInt("revival.surrender-hold-seconds", 3);
    }
}
