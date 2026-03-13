package org.slimecraft.bedrock.annotation.plugin;

import org.slimecraft.bedrock.dependency.LoadOrder;
import org.slimecraft.bedrock.dependency.LoadStage;

/**
 * Marks a dependency for a plugin.
 */
public @interface Dependency {
    /**
     * The name of the dependency.
     */
    String value();

    /**
     * The {@link LoadStage} in which a dependency should be loaded.
     */
    LoadStage loadStage() default LoadStage.SERVER;

    /**
     * The {@link LoadOrder} of when the dependency should be loaded.
     */
    LoadOrder loadOrder() default LoadOrder.BEFORE;

    /**
     * If the dependency is required for the plugin to function.
     */
    boolean required() default false;

    /**
     * Whether the dependency's classpath should be joined.
     */
    boolean joinClasspath() default true;
}
