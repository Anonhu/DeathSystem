package dev.anonhu.deathsystem.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

/** Вызывается когда игрок поднят. */
public class PlayerRevivedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    /** null = самоподъём */
    @Nullable
    private final Player reviver;

    public PlayerRevivedEvent(Player player, @Nullable Player reviver) {
        this.player  = player;
        this.reviver = reviver;
    }

    public Player getPlayer()            { return player; }
    @Nullable public Player getReviver() { return reviver; }
    public boolean isSelfRevive()        { return reviver == null; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()          { return HANDLERS; }
}
