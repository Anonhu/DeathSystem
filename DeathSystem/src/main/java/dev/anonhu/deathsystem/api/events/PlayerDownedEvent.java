package dev.anonhu.deathsystem.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Вызывается когда игрок переходит в downed-состояние. */
public class PlayerDownedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    public PlayerDownedEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() { return player; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()          { return HANDLERS; }
}
