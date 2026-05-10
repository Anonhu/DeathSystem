package dev.yourname.deathsystem.api;

import dev.yourname.deathsystem.DeathSystemPlugin;
import org.bukkit.entity.Player;

public final class DeathSystemAPI {

    private DeathSystemAPI() {}

    public static boolean isPlayerDowned(Player player) {
        return DeathSystemPlugin.getInstance()
            .getDownedPlayerManager()
            .isDown(player);
    }

    public static void forceRevive(Player player, Player reviver, double hpPercent) {
        DeathSystemPlugin.getInstance()
            .getDownedPlayerManager()
            .revivePlayer(player, reviver, hpPercent);
    }

    public static void forceDown(Player player) {
        DeathSystemPlugin.getInstance()
            .getDownedPlayerManager()
            .downPlayer(player, null);
    }
}
