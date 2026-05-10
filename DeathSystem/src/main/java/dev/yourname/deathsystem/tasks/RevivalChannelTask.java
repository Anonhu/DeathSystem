package dev.yourname.deathsystem.tasks;

import dev.yourname.deathsystem.DeathSystemPlugin;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RevivalChannelTask extends BukkitRunnable {

    private final DeathSystemPlugin plugin;
    private final Player reviver;
    private final Player target;
    private final int hpPercent;
    private final int channelTicks;
    private final BossBar bossBar;
    private final Runnable onSuccess;
    private final Runnable onCancel;

    private int ticksPassed = 0;
    private Location reviverStartLoc;

    public RevivalChannelTask(DeathSystemPlugin plugin, Player reviver, Player target,
                               int hpPercent, int channelSeconds, BossBar bossBar,
                               Runnable onSuccess, Runnable onCancel) {
        this.plugin = plugin;
        this.reviver = reviver;
        this.target = target;
        this.hpPercent = hpPercent;
        this.channelTicks = channelSeconds * 20;
        this.bossBar = bossBar;
        this.onSuccess = onSuccess;
        this.onCancel = onCancel;
        this.reviverStartLoc = reviver.getLocation().clone();
    }

    @Override
    public void run() {
        if (!reviver.isOnline() || !target.isOnline()
                || !plugin.getDownedPlayerManager().isDown(target)) {
            abort();
            return;
        }

        double dist = reviver.getLocation().distance(target.getLocation());
        if (dist > plugin.getPluginConfig().revivalRadiusBlocks + 0.5) {
            reviver.sendMessage("§cВы отошли слишком далеко! Прогресс сброшен.");
            abort();
            return;
        }

        Location cur = reviver.getLocation();
        if (cur.getBlockX() != reviverStartLoc.getBlockX()
         || cur.getBlockZ() != reviverStartLoc.getBlockZ()) {
            reviver.sendMessage("§cВы пошевелились! Прогресс сброшен.");
            abort();
            return;
        }

        ticksPassed += 5;
        double progress = (double) ticksPassed / channelTicks;

        bossBar.setProgress(Math.min(1.0, progress));
        bossBar.setTitle("§aПодъём §f" + target.getName() + " §7" +
            (int)(progress * 100) + "%");

        if (ticksPassed >= channelTicks) {
            bossBar.removeAll();
            cancel();
            onSuccess.run();
        }
    }

    private void abort() {
        bossBar.removeAll();
        cancel();
        onCancel.run();
    }
}
