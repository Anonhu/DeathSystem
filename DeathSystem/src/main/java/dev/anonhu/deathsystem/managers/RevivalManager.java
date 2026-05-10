package dev.anonhu.deathsystem.managers;

import dev.anonhu.deathsystem.DeathSystemPlugin;
import dev.anonhu.deathsystem.config.PluginConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Управляет процессом воскрешения союзником.
 *
 * Триггер: правая кнопка мыши по упавшему игроку с предметом в руке.
 * Канал: спаситель стоит рядом N секунд — прогресс-бар в ActionBar обоих игроков.
 * При движении спасителя — прогресс сбрасывается.
 */
public class RevivalManager implements Listener {

    private final DeathSystemPlugin plugin;
    private final PluginConfig      cfg;

    public RevivalManager(DeathSystemPlugin plugin) {
        this.plugin = plugin;
        this.cfg    = plugin.getPluginConfig();
    }

    // =========================================================
    //  Триггер: ПКМ по упавшему
    // =========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        // Только главная рука
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player downed)) return;

        Player reviver = event.getPlayer();

        var mgr   = plugin.getDownedPlayerManager();
        var state = mgr.getState(downed);
        if (state == null) return;                    // цель не лежит
        if (state.isBeingRevived()) return;           // уже кто-то спасает
        if (reviver.equals(downed)) return;           // нельзя спасать самого себя

        // Дистанция
        if (reviver.getLocation().distance(downed.getLocation()) > cfg.radiusBlocks) return;

        // Предмет в руке
        ItemStack item = reviver.getInventory().getItemInMainHand();
        RevivalItem revivalItem = getRevivalItem(item);
        if (revivalItem == null) return; // неподходящий предмет

        event.setCancelled(true);

        if (revivalItem.channelSeconds == 0) {
            // Мгновенный подъём (дефибриллятор)
            consumeItem(reviver, item, revivalItem);
            mgr.revivePlayer(downed, reviver, revivalItem.hpPercent);
        } else {
            // Начинаем канал
            startRevivalChannel(reviver, downed, state, revivalItem, item);
        }
    }

    // =========================================================
    //  Канал воскрешения
    // =========================================================

    private void startRevivalChannel(Player reviver, Player downed,
                                     DownedState state, RevivalItem revivalItem,
                                     ItemStack usedItem) {
        state.reviver         = reviver;
        state.revivalProgress = 0;
        int totalTicks = revivalItem.channelSeconds * 20;

        reviver.sendMessage("\u00a7eНачали подъём... Не двигайтесь!");
        downed.sendMessage("\u00a7aВас начали поднимать!");

        state.revivalChannelTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Проверки живучести
                if (!reviver.isOnline() || !downed.isOnline()
                        || !plugin.getDownedPlayerManager().isDown(downed)) {
                    cancelChannel(state, reviver, false);
                    cancel();
                    return;
                }

                // Дистанция
                if (reviver.getLocation().distance(downed.getLocation()) > cfg.radiusBlocks) {
                    cancelChannel(state, reviver, true);
                    cancel();
                    return;
                }

                state.revivalProgress += 2; // +2 тика за каждые 2 тика

                // UI
                sendRevivalBar(reviver, downed, state.revivalProgress, totalTicks);

                // Частицы
                downed.getWorld().spawnParticle(
                    Particle.HEART,
                    downed.getLocation().add(0, 1.2, 0),
                    2, 0.3, 0.1, 0.3, 0);

                if (state.revivalProgress >= totalTicks) {
                    // Канал завершён!
                    consumeItem(reviver, usedItem, revivalItem);
                    plugin.getDownedPlayerManager()
                        .revivePlayer(downed, reviver, revivalItem.hpPercent);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 2L, 2L); // тик каждые 2 тика = ~10 обновлений/сек
    }

    /** Прерывает канал если спаситель отошёл. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onReviverMove(PlayerMoveEvent event) {
        Player reviver = event.getPlayer();

        // Ищем упавшего которого этот игрок спасает
        plugin.getDownedPlayerManager().getAllDowned().stream()
            .filter(s -> reviver.equals(s.reviver))
            .findFirst()
            .ifPresent(state -> {
                var from = event.getFrom();
                var to   = event.getTo();
                if (to == null) return;

                boolean moved = from.getBlockX() != to.getBlockX()
                             || from.getBlockZ() != to.getBlockZ()
                             || from.getBlockY() != to.getBlockY();
                if (moved) {
                    cancelChannel(state, reviver, true);
                }
            });
    }

    // =========================================================
    //  Внутренние утилиты
    // =========================================================

    private void cancelChannel(DownedState state, Player reviver, boolean notify) {
        if (state.revivalChannelTask != null && !state.revivalChannelTask.isCancelled()) {
            state.revivalChannelTask.cancel();
        }
        state.revivalChannelTask = null;
        state.reviver            = null;
        state.revivalProgress    = 0;

        if (notify && reviver != null && reviver.isOnline()) {
            reviver.sendMessage("\u00a7cПодъём прерван! Не двигайтесь во время помощи.");
            reviver.sendActionBar(Component.empty());
            reviver.playSound(reviver.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.4f);
        }
    }

    private void sendRevivalBar(Player reviver, Player downed, int progress, int total) {
        int filled = (int) Math.round((double) progress / total * 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "\u00a7a\u2588" : "\u00a78\u2591");
        }
        String text = "\u00a7eПодъём " + downed.getName() + "... " + bar;
        reviver.sendActionBar(Component.text(text));
        downed.sendActionBar(Component.text("\u00a7aПодъём... " + bar));
    }

    /**
     * Уменьшает количество предмета на 1.
     * Для дефибриллятора уменьшает заряд (через NBT/PersistentData — см. CustomItemManager).
     */
    private void consumeItem(Player reviver, ItemStack item, RevivalItem revivalItem) {
        if (revivalItem.consumable) {
            item.setAmount(item.getAmount() - 1);
        }
        // Для дефибриллятора заряд уменьшает CustomItemManager
        if (revivalItem == RevivalItem.DEFIBRILLATOR) {
            plugin.getCustomItemManager().decrementDefibrillatorCharge(reviver);
        }
    }

    // =========================================================
    //  Определение предмета
    // =========================================================

    private RevivalItem getRevivalItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        var cim = plugin.getCustomItemManager();

        // Дефибриллятор
        if (cim.isDefibrillator(item) && cim.hasCharge(item)) {
            return RevivalItem.DEFIBRILLATOR;
        }
        // Бинт
        if (cim.isBandage(item)) return RevivalItem.BANDAGE;

        // Стандартные ванильные предметы
        return switch (item.getType()) {
            case ENCHANTED_GOLDEN_APPLE -> RevivalItem.ENCHANTED_APPLE;
            case SPLASH_POTION          -> isSplashHealingPotion(item)
                                            ? RevivalItem.HEALING_POTION : null;
            default -> null;
        };
    }

    private boolean isSplashHealingPotion(ItemStack item) {
        if (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta meta)) return false;
        var type = meta.getBasePotionType();
        return type != null && (type == org.bukkit.potion.PotionType.HEALING
                             || type == org.bukkit.potion.PotionType.STRONG_HEALING);
    }

    // =========================================================
    //  Варианты предметов
    // =========================================================

    private enum RevivalItem {
        ENCHANTED_APPLE (50, 5,  true,  false),
        HEALING_POTION  (20, 5,  true,  false),
        BANDAGE         (5,  3,  true,  false),
        DEFIBRILLATOR   (30, 0,  false, true);  // не расходуется целиком

        final int     hpPercent;
        final int     channelSeconds;
        final boolean consumable;    // уменьшает item.amount
        final boolean hasCharges;    // уменьшает PersistentData-заряд

        RevivalItem(int hpPercent, int channelSeconds,
                    boolean consumable, boolean hasCharges) {
            this.hpPercent      = hpPercent;
            this.channelSeconds = channelSeconds;
            this.consumable     = consumable;
            this.hasCharges     = hasCharges;
        }
    }
}
