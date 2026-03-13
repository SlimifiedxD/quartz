package org.slimecraft.bedrock.annotation.plugin.dependency;

/**
 * Represents the order in which a dependency is loaded.
 */
public enum LoadOrder {
    /**
     * The dependency will be loaded before your plugin.
     */
    BEFORE,
    /**
     * The dependency will be loaded after your plugin.
     */
    AFTER,
    /**
     * The dependency will have undefined ordering behaviour; it may be loaded before or after your plugin.
     */
    OMIT
}
