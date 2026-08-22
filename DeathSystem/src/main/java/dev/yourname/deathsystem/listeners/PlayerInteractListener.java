package dev.yourname.deathsystem.listeners;

import dev.yourname.deathsystem.DeathSystemPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

public class PlayerInteractListener implements Listener {

    private final DeathSystemPlugin plugin;

    public PlayerInteractListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Событие файрится для каждой руки — обрабатываем только главную
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        if (!(event.getRightClicked() instanceof Player target)) return;

        Player reviver = event.getPlayer();
        if (!plugin.getDownedPlayerManager().isDown(target)) return;
        if (reviver.equals(target)) return;

        event.setCancelled(true);

        ItemStack item = reviver.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;

        var cfg = plugin.getPluginConfig();

        if (isEnchantedApple(item)) {
            plugin.getRevivalManager().startRevival(reviver, target, item,
                cfg.enchantedAppleHpPercent,
                cfg.enchantedAppleChannelSeconds);
        } else if (isHealingPotion(item)) {
            plugin.getRevivalManager().startRevival(reviver, target, item,
                cfg.healingPotionHpPercent,
                cfg.healingPotionChannelSeconds);
        } else if (isBandage(item)) {
            plugin.getRevivalManager().startRevival(reviver, target, item,
                cfg.bandageHpPercent,
                cfg.bandageChannelSeconds);
        } else if (isDefibrillator(item)) {
            if (!hasCharges(item)) {
                reviver.sendMessage("§cДефибриллятор разряжен!");
                return;
            }
            plugin.getRevivalManager().startRevival(reviver, target, item,
                cfg.defibrillatorHpPercent,
                cfg.defibrillatorChannelSeconds);
        }
    }

    private boolean isEnchantedApple(ItemStack item) {
        return item.getType() == Material.ENCHANTED_GOLDEN_APPLE;
    }

    private boolean isHealingPotion(ItemStack item) {
        if (item.getType() != Material.POTION) return false;
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;
        return meta.getBasePotionType() == PotionType.HEALING
            || meta.getBasePotionType() == PotionType.STRONG_HEALING;
    }

    private boolean isBandage(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        var key = new org.bukkit.NamespacedKey(plugin, "item_type");
        String type = pdc.get(key, PersistentDataType.STRING);
        return "bandage".equals(type);
    }

    private boolean isDefibrillator(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        var key = new org.bukkit.NamespacedKey(plugin, "item_type");
        String type = pdc.get(key, PersistentDataType.STRING);
        return "defibrillator".equals(type);
    }

    private boolean hasCharges(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        var key = new org.bukkit.NamespacedKey(plugin, "use_count");
        Integer uses = pdc.get(key, PersistentDataType.INTEGER);
        int maxUses = plugin.getPluginConfig().defibrillatorMaxUses;
        return uses == null || uses < maxUses;
    }
}
