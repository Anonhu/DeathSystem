package dev.yourname.deathsystem.api;

import dev.yourname.deathsystem.DeathSystemPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeathSystemPlaceholders extends PlaceholderExpansion {

    private final DeathSystemPlugin plugin;

    public DeathSystemPlaceholders(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "deathsystem";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty()
            ? "YourName"
            : plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        var mgr = plugin.getDownedPlayerManager();

        return switch (params) {
            case "is_downed" -> String.valueOf(mgr.isDown(player));
            case "time_left" -> {
                var state = mgr.getState(player);
                yield state != null ? String.valueOf(state.timeLeft) : "";
            }
            case "hp" -> mgr.isDown(player)
                ? String.format("%.1f", player.getHealth())
                : "";
            default -> null;
        };
    }
}
