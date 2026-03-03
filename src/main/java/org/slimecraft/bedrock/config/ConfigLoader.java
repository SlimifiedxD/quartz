package org.slimecraft.bedrock.config;

public interface ConfigLoader {
    <T> T load(Class<T> clazz);

    <T> void save(T config);

    static ConfigLoader yaml() {
        return YamlConfigLoader.instance();
    }
}
