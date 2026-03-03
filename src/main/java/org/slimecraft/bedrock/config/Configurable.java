package org.slimecraft.bedrock.config;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface Configurable<T> {
    @NotNull Map<String, Object> toConfig(T value);

    @NotNull T fromConfig(@NotNull Map<String, Object> representation);
}
