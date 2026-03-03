package org.slimecraft.bedrock.config;

public interface ConfigLoader {
    <T> T load(Class<T> clazz);

    <T> void save(T config);

    <T> void register(Configurable<T> configurable);

    static ConfigLoader yaml() {
        return YamlConfigLoader.instance();
    }
}
