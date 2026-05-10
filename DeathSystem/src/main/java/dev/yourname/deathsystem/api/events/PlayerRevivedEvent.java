package dev.yourname.deathsystem.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerRevivedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player revived;
    private final Player reviver;
    private final ItemStack itemUsed;

    public PlayerRevivedEvent(Player revived, @Nullable Player reviver, @Nullable ItemStack itemUsed) {
        this.revived = revived;
        this.reviver = reviver;
        this.itemUsed = itemUsed;
    }

    public Player getRevived() {
        return revived;
    }

    @Nullable
    public Player getReviver() {
        return reviver;
    }

    @Nullable
    public ItemStack getItemUsed() {
        return itemUsed;
    }

    public boolean isSelfRevive() {
        return reviver == null;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
