package dev.yourname.deathsystem.managers;

import dev.yourname.deathsystem.tasks.BleedTask;
import dev.yourname.deathsystem.tasks.CountdownTask;
import org.bukkit.Location;

public class DownedState {

    public final Location downedLocation;
    public BleedTask bleedTask;
    public CountdownTask countdownTask;
    public boolean isCrawling = false;
    public int timeLeft;

    public DownedState(Location location) {
        this.downedLocation = location.clone();
    }
}
