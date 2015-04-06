package server.action;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import org.scimitarpowered.api.action.Action;
import org.scimitarpowered.api.action.ActionDispatcher;
import org.scimitarpowered.api.world.entity.Entity;

public class ScimitarActionDispatcher implements ActionDispatcher {
	
	private static ScimitarActionDispatcher instance = null;
	
	private final int QUEUE_SIZE = 3;
	
	private List<Action<Entity>> actions = new ArrayList<Action<Entity>>();

	private Queue<Action<Entity>> queuedActions = new ArrayDeque<Action<Entity>>();
	
	private ScimitarActionDispatcher() {}
	
	@Override
	public void schedule(Action<Entity> action) {
//		if (actions.isEmpty() && action.isRunning()) {
//			System.out.println("queue was empty; running action.");
//			action.execute();
//			action.setRunning(false);
//		}
		synchronized(queuedActions) {
			if (queuedActions.size() != QUEUE_SIZE) {
				queuedActions.add(action);
				System.out.println("added to queue.");
			} else {
				System.out.println("queue is too full!");
				action.onDestroy();
				actions.remove(true);
				System.out.println("removed oldest action"); // remove the oldest action to make room for newest one.
				
			}
		}
	}

	@Override
	public void tick() {
		synchronized(queuedActions) {
			Action<Entity> action;
			while ((action = queuedActions.poll()) != null) {
				actions.add(action);
				System.out.println("moved to active actions...");
			}
		}
		
		for (Iterator<Action<Entity>> it = actions.iterator(); it.hasNext();) {
			Action<Entity> action = it.next();
			try {
				if (!action.tick()) {
					action.onDestroy();
					System.out.println("removing...");
					it.remove();
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	@Override
	public void destroy() {
		actions.clear();
		queuedActions.clear();
	}

	@Override
	public List<Action<Entity>> getActiveActions() {
		return actions;
	}

	@Override
	public Queue<Action<Entity>> getQueuedActions() {
		return queuedActions;
	}

	/**
	 * Returns the single instance to the action dispatcher.
	 * @return The dispatcher instance.
	 */
	public static ScimitarActionDispatcher getInstance() {
		if (instance == null) {
			instance = new ScimitarActionDispatcher();
		}
		return instance;
	}
	
}
