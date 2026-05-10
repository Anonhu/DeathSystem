package dev.anonhu.deathsystem.listeners;

import dev.anonhu.deathsystem.DeathSystemPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Блокирует все действия с инвентарём в downed-состоянии.
 */
public class InventoryListener implements Listener {

    private final DeathSystemPlugin plugin;

    public InventoryListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    /** Клики в инвентаре */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && plugin.getDownedPlayerManager().isDown(player)) {
            event.setCancelled(true);
        }
    }

    /** Взаимодействие с миром / блоками */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (plugin.getDownedPlayerManager().isDown(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /** Бросить предмет */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getDownedPlayerManager().isDown(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /** Смена слота хотбара */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        if (plugin.getDownedPlayerManager().isDown(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /** Перекладывание в руку (F) */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (plugin.getDownedPlayerManager().isDown(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
