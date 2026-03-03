package org.slimecraft.bedrock.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class LocationConfigurable implements Configurable<Location> {
    @Override
    public @NotNull Map<String, Object> toConfig(Location value) {
        return Map.of(
                "world", value.getWorld().getUID(),
                "x", value.getBlockX(),
                "y", value.getBlockY(),
                "z", value.getBlockZ(),
                "pitch", value.getPitch(),
                "yaw", value.getYaw()
        );
    }

    @Override
    public @NotNull Location fromConfig(@NotNull Map<String, Object> representation) {
        return new Location(
                Bukkit.getWorld((UUID) representation.get("world")),
                (int) representation.get("x"),
                (int) representation.get("y"),
                (int) representation.get("z"),
                (float) representation.get("pitch"),
                (float) representation.get("yaw")
        );
    }
}
