package dev.yourname.deathsystem.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class ParticleUtils {

    public static void playReviveEffect(Location location) {
        var world = location.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.HEART, location.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.HAPPY_VILLAGER, location, 10, 0.5, 0.5, 0.5, 0.1);
        world.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    public static void playBleedEffect(Location location) {
        var world = location.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.DAMAGE_INDICATOR, location.add(0, 0.5, 0),
            3, 0.2, 0.2, 0.2, 0.01);
    }
}
