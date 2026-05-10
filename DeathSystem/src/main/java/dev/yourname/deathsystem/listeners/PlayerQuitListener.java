package dev.yourname.deathsystem.listeners;

import dev.yourname.deathsystem.DeathSystemPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final DeathSystemPlugin plugin;

    public PlayerQuitListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDownedPlayerManager().isDown(player)) return;

        plugin.getDownedPlayerManager().killDowned(player);
        event.quitMessage(
            net.kyori.adventure.text.Component.text(
                "§8[§c" + player.getName() + " погиб, сбежав с поля боя§8]"
            )
        );
    }
}
