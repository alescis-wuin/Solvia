package fr.seynax.solvia.desktop.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import fr.seynax.solvia.desktop.ui.DesktopEventBus.EventType;

class DesktopEventBusTest {

    @Test
    void publishesSubscribedEvents() {
        DesktopEventBus eventBus = new DesktopEventBus();
        AtomicInteger accountEvents = new AtomicInteger();
        AtomicInteger portfolioEvents = new AtomicInteger();

        eventBus.subscribe(EventType.ACCOUNTS_CHANGED, accountEvents::incrementAndGet);
        eventBus.subscribe(EventType.PORTFOLIO_DATA_CHANGED, portfolioEvents::incrementAndGet);

        eventBus.publish(EventType.ACCOUNTS_CHANGED);
        eventBus.publish(EventType.PORTFOLIO_DATA_CHANGED);
        eventBus.publish(EventType.PORTFOLIO_DATA_CHANGED);

        assertEquals(1, accountEvents.get());
        assertEquals(2, portfolioEvents.get());
    }

    @Test
    void closesSubscription() {
        DesktopEventBus eventBus = new DesktopEventBus();
        AtomicInteger events = new AtomicInteger();

        DesktopEventBus.Subscription subscription = eventBus.subscribe(EventType.PORTFOLIO_DATA_CHANGED, events::incrementAndGet);
        eventBus.publish(EventType.PORTFOLIO_DATA_CHANGED);
        subscription.close();
        eventBus.publish(EventType.PORTFOLIO_DATA_CHANGED);

        assertEquals(1, events.get());
    }
}
