package server.event;
import org.scimitarpowered.api.event.Event;
import org.scimitarpowered.api.event.EventDispatcher;
import org.scimitarpowered.api.event.EventSubscriber;

import com.google.common.eventbus.EventBus;

/**
 * A wrapper for {@link EventBus} which dispatches {@link Event}s and provides a
 * way for {@link EventSubscriber}s to be registered and unregistered.
 * 
 * @author Ryley Kimmel <ryley.kimmel@live.com>
 */
public final class ScimitarEventDispatcher implements EventDispatcher {

    /**
     * The default event bus, used to register and deregister event subscribers
     * as well as dispatch events.
     */
    private final EventBus eventBus = new EventBus();

    /**
     * Registers the specified event subscriber.
     * 
     * @param subscriber The event subscriber to register.
     * @param E The type of event attached to the event subscriber.
     */
    public <E extends Event> void register(EventSubscriber<E> subscriber) {
    	eventBus.register(subscriber);
    }

    /**
     * Unregisters the specified event subscriber.
     * 
     * @param subscriber The event subscriber to unregister.
     * @param E The type of event attached to the event subscriber.
     */
    public <E extends Event> void deregister(EventSubscriber<E> subscriber) {
    	eventBus.unregister(subscriber);
    }

    /**
     * Dispatches the specified event, notifying its event subscriber.
     * 
     * @param event The event to dispatch.
     */
    public void dispatch(Event event) {
    	eventBus.post(event);
    }

}