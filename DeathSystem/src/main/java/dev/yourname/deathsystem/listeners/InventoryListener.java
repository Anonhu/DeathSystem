package dev.yourname.deathsystem.listeners;

import dev.yourname.deathsystem.DeathSystemPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class InventoryListener implements Listener {

    private final DeathSystemPlugin plugin;

    public InventoryListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!plugin.getDownedPlayerManager().isDown(player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getDownedPlayerManager().isDown(player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDownedPlayerManager().isDown(player)) return;
        event.setCancelled(true);
        player.sendMessage("§c[!] §7В таком состоянии вы не можете выбрасывать предметы...");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getDownedPlayerManager().isDown(player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDownedPlayerManager().isDown(player)) return;
        event.setCancelled(true);
    }
}
