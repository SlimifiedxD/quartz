package org.slimecraft.bedrock.event;

import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.ApiStatus;
import org.slimecraft.bedrock.internal.Bedrock;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class EventNode {
    private final Key identifier;
    private final List<EventListener<?>> listeners;
    private final List<EventNode> children;

    private static final class SingletonHelper {
        private static final EventNode GLOBAL_NODE = new EventNode(Key.key("bedrock", "global"), new ArrayList<>(), new ArrayList<>());

        static {
            /**
             * This is a quick and dirty hack to initialize bedrock because #getPlugin must be called somewhere.
             */
            Bedrock.bedrock().getPlugin();
        }
    }

    public static EventNode global() {
        return SingletonHelper.GLOBAL_NODE;
    }

    public EventNode(Key identifier, List<EventListener<?>> listeners, List<EventNode> children) {
        this.identifier = identifier;
        this.listeners = listeners;
        this.children = children;
    }

    public EventNode(Key identifier) {
        this(identifier, new ArrayList<>(), new ArrayList<>());
    }

    public <T> void fire(T event) {
        fireEventNodeRecursive(event);
    }

    public <T> void addListener(EventListener<T> listener) {
        if (Bedrock.bedrock().getLazyEvents().add(listener.getEventType())) {
            if (!Event.class.isAssignableFrom(listener.getEventType())) return;
            Bukkit.getServer().getPluginManager().registerEvent((Class<? extends Event>) listener.getEventType(),
                    Bedrock.bedrock().getBukkitListener(),
                    EventPriority.NORMAL,
                    (bukkitListener, event) -> {
                        if (event.getClass() != listener.getEventType()) return; // don't fire subclasses of events
                        EventNode.global().fire(event);
                    }, Bedrock.bedrock().getPlugin());
        }
        listeners.add(listener);
    }

    /**
     * Use {@link EventListener#of(Class)} instead.
     */
    @ApiStatus.Obsolete
    public <T> void addListener(Class<T> eventType, Consumer<T> action) {
        final EventListener<T> listener = new EventListener<>(eventType);
        listener.addHandler(action);
        addListener(listener);
    }

    /**
     * Use {@link EventListener#of(Class)} instead.
     */
    @ApiStatus.Obsolete
    public <T> void addListener(Class<T> eventType, Consumer<T> action, Predicate<T> predicate) {
        final EventListener<T> listener = new EventListener<>(eventType);
        listener.addHandler(action, Filter.of(predicate));
        addListener(listener);
    }

    public <T> void addListener(EventListenerBuilder<T> builder) {
        addListener(builder.build());
    }

    public void addChild(EventNode child) {
        this.children.add(child);
    }

    public void attachTo(EventNode parent) {
        parent.addChild(this);
    }

    public Key getIdentifier() {
        return identifier;
    }

    public List<EventNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public List<EventListener<?>> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    private <T> void fireEventNodeRecursive(T event) {
        fireEventNode(event);
        // TODO: fix recursion so it doesnt give a stack overflow
        //getChildren().forEach(child -> fireEventNodeRecursive(event));
    }

    private <T> void fireEventNode(T event) {
        getListeners().forEach(listener -> {
            if (!listener.getEventType().isAssignableFrom(event.getClass())) {
                return;
            }
            final var typedListener = (EventListener<T>) listener;
            for (var handler : typedListener.getHandlers()) {
                final var optFilter = handler.getFilter();
                if (optFilter.isPresent()) {
                    final var filter = optFilter.get();
                    if (!filter.getPredicate().test(event)) {
                        filter.getOrElse().ifPresent(orElseHandler -> {
                            orElseHandler.accept(event);
                        });
                        continue;
                    }
                }
                handler.getConsumer().accept(event);
            }
        });
    }
}
