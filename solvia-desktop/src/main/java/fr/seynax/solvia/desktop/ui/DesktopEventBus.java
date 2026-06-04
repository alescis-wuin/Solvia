package fr.seynax.solvia.desktop.ui;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DesktopEventBus {

    public enum EventType {
        ACCOUNTS_CHANGED,
        PORTFOLIO_DATA_CHANGED
    }

    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    private final Map<EventType, CopyOnWriteArrayList<Runnable>> subscribers = new EnumMap<>(EventType.class);

    public DesktopEventBus() {
        for (EventType type : EventType.values()) {
            subscribers.put(type, new CopyOnWriteArrayList<>());
        }
    }

    public Subscription subscribe(EventType type, Runnable listener) {
        if (type == null || listener == null) {
            return () -> { };
        }
        List<Runnable> listeners = subscribers.get(type);
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void publish(EventType type) {
        if (type == null) {
            return;
        }
        for (Runnable listener : subscribers.get(type)) {
            listener.run();
        }
    }
}
