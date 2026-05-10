package dev.anonhu.deathsystem.listeners;

import dev.anonhu.deathsystem.DeathSystemPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Random;

/**
 * Искажает чат downed-игроков.
 * AsyncChatEvent — async, поэтому только читаем из ConcurrentHashMap (безопасно).
 */
public class ChatListener implements Listener {

    private static final char[] NOISE_CHARS =
        { '.', ',', '*', '+', '!', '?', '~', '#', '@', '%' };

    private final DeathSystemPlugin plugin;
    private final Random            rng = new Random();

    public ChatListener(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        var cfg = plugin.getPluginConfig();
        if (!cfg.chatDistortionEnabled) return;
        if (!plugin.getDownedPlayerManager().isDown(event.getPlayer())) return;

        // Извлекаем чистый текст сообщения
        String original = PlainTextComponentSerializer.plainText()
            .serialize(event.message());

        String distorted = distort(original, cfg.chatDistortEveryNChars);

        // Заменяем сообщение
        event.message(Component.text(distorted));
    }

    private String distort(String input, int everyN) {
        StringBuilder sb = new StringBuilder(input.length() + input.length() / everyN);
        for (int i = 0; i < input.length(); i++) {
            sb.append(input.charAt(i));
            // После каждого N-го символа вставляем шум
            if ((i + 1) % everyN == 0) {
                sb.append(NOISE_CHARS[rng.nextInt(NOISE_CHARS.length)]);
            }
        }
        return sb.toString();
    }
}
