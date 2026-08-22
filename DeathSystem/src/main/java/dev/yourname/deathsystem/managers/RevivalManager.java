package dev.yourname.deathsystem.managers;

import dev.yourname.deathsystem.DeathSystemPlugin;
import dev.yourname.deathsystem.tasks.RevivalChannelTask;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RevivalManager {

    private final DeathSystemPlugin plugin;
    private final Map<UUID, RevivalChannelTask> activeRevivals = new HashMap<>();
    private final Map<UUID, ItemStack> lastReviveItems = new HashMap<>();

    public RevivalManager(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public void startRevival(Player reviver, Player target, ItemStack item,
                              int hpPercent, int channelSeconds) {
        cancelRevival(reviver);
        lastReviveItems.put(reviver.getUniqueId(), item.clone());

        if (channelSeconds == 0) {
            completeRevival(reviver, target, hpPercent, item);
            return;
        }

        BossBar bossBar = plugin.getServer().createBossBar(
            "§aПодъём: §f" + target.getName(),
            BarColor.GREEN,
            BarStyle.SOLID
        );
        bossBar.addPlayer(reviver);
        bossBar.setProgress(0.0);

        RevivalChannelTask task = new RevivalChannelTask(
            plugin, reviver, target, hpPercent, channelSeconds, bossBar,
            () -> {
                activeRevivals.remove(reviver.getUniqueId());
                completeRevival(reviver, target, hpPercent, item);
            },
            () -> {
                activeRevivals.remove(reviver.getUniqueId());
            }
        );

        activeRevivals.put(reviver.getUniqueId(), task);
        task.runTaskTimer(plugin, 0L, 5L);

        reviver.sendMessage("§eНачинаем подъём §f" + target.getName() + "§e... Не двигайтесь!");
        target.sendMessage("§e" + reviver.getName() + " §7пытается вас поднять...");
    }

    public void cancelRevival(Player reviver) {
        RevivalChannelTask task = activeRevivals.remove(reviver.getUniqueId());
        if (task != null) task.cancel();
    }

    public boolean isReviving(Player reviver) {
        return activeRevivals.containsKey(reviver.getUniqueId());
    }

    public ItemStack getLastReviveItem(Player reviver) {
        return lastReviveItems.get(reviver.getUniqueId());
    }

    public void clearLastReviveItem(Player reviver) {
        lastReviveItems.remove(reviver.getUniqueId());
    }

    private void completeRevival(Player reviver, Player target, int hpPercent, ItemStack item) {
        consumeItem(reviver, item);
        plugin.getDownedPlayerManager().revivePlayer(target, reviver, hpPercent);
    }

    private void consumeItem(Player reviver, ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) return;

        var pdc = meta.getPersistentDataContainer();
        var typeKey = new org.bukkit.NamespacedKey(plugin, "item_type");
        String type = pdc.get(typeKey, org.bukkit.persistence.PersistentDataType.STRING);

        if ("defibrillator".equals(type)) {
            var usesKey = new org.bukkit.NamespacedKey(plugin, "use_count");
            int uses = pdc.getOrDefault(usesKey, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
            uses++;

            if (uses >= plugin.getPluginConfig().defibrillatorMaxUses) {
                reviver.getInventory().getItemInMainHand().setAmount(0);
                reviver.sendMessage("§8[Дефибриллятор сломался после последнего использования]");
            } else {
                pdc.set(usesKey, org.bukkit.persistence.PersistentDataType.INTEGER, uses);
                item.setItemMeta(meta);
                plugin.getCustomItemManager().updateDefibrillatorLore(
                    reviver.getInventory().getItemInMainHand(), uses
                );
            }
        } else {
            ItemStack hand = reviver.getInventory().getItemInMainHand();
            if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
            else reviver.getInventory().setItemInMainHand(null);
        }
    }
}
