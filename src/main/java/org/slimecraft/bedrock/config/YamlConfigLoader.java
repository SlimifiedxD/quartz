package org.slimecraft.bedrock.config;

import org.slimecraft.bedrock.annotation.config.Configuration;
import org.slimecraft.bedrock.annotation.config.ConfigurationValue;
import org.slimecraft.bedrock.internal.Bedrock;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class YamlConfigLoader implements ConfigLoader {
    private YamlConfigLoader() {}

    protected static YamlConfigLoader instance() {
        return Singleton.INSTANCE;
    }

    private static final class Singleton {
        public static final YamlConfigLoader INSTANCE = new YamlConfigLoader();
    }

    public <T> T load(Class<T> clazz) {
        if (!clazz.isAnnotationPresent(Configuration.class)) {
            throw new IllegalArgumentException(clazz.getName() + " is not annotated with @Configuration!");
        }
        try {
            final T config = clazz.getConstructor().newInstance();
            final File file = new File(Bedrock.bedrock().getPlugin().getDataPath().resolve(clazz.getDeclaredAnnotation(Configuration.class).value()) + ".yml");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                configureFileFromYmlConfig(config, file);
            } else {
                configureYmlConfigFromFile(config, file);
            }

            return config;
        } catch (RuntimeException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Something went wrong whilst parsing the config!");
        }
    }

    public <T> void save(T config) {
        throw new IllegalArgumentException("Not yet implemented!");
    }

    private <T> void configureYmlConfigFromFile(T config, File file) throws IOException, IllegalAccessException {
        final Yaml yaml = new Yaml();
        Map<String, Object> contents;
        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            contents = yaml.load(reader);
        }
        if (contents == null) return;
        modifyMapForDotNotation(contents);
        for (Field field : config.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(ConfigurationValue.class)) continue;
            final String configKey = field.getDeclaredAnnotation(ConfigurationValue.class).value();
            field.set(config, contents.get(configKey));
        }
    }

    private void modifyMapForDotNotation(Map<String, Object> data) {
        final List<String> keys = new ArrayList<>(data.keySet());

        keys.forEach(key -> {
            if (!key.contains(".")) return;
            final Object value = data.remove(key);
            final String[] parts = key.split("\\.");

            Map<String, Object> current = data;
            for (int i = 0; i < parts.length; i++) {
                final String part = parts[i];

                if (i == parts.length - 1) {
                    current.put(part, value);
                } else {
                    current = (Map<String, Object>) current.computeIfAbsent(part, k -> new LinkedHashMap<>());
                }
            }
        });
    }

    private <T> void configureFileFromYmlConfig(T config, File file) throws IllegalAccessException, IOException {
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        final Yaml yaml = new Yaml(dumperOptions);
        final Map<String, Object> contents = new LinkedHashMap<>();
        for (Field field : config.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(ConfigurationValue.class)) continue;
            final String configKey = field.getDeclaredAnnotation(ConfigurationValue.class).value();

            contents.put(configKey, field.get(config));
        }
        modifyMapForDotNotation(contents);
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            yaml.dump(contents, writer);
        }
    }
}
