package dev.yourname.deathsystem.listeners;

import dev.yourname.deathsystem.DeathSystemPlugin;
import dev.yourname.deathsystem.utils.ChatDistorter;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final DeathSystemPlugin plugin;

    public ChatListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getDownedPlayerManager().isDown(player)) return;
        if (!plugin.getPluginConfig().chatDistortionEnabled) return;

        String original = PlainTextComponentSerializer.plainText()
            .serialize(event.originalMessage());

        int every = plugin.getPluginConfig().chatDistortEveryNChars;
        String distorted = ChatDistorter.distort(original, every);

        event.message(Component.text("§7*" + distorted + "*"));
    }
}
