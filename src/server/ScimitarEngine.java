package server;
/*
 * This file is part of RuneSource.
 *
 * RuneSource is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RuneSource is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RuneSource.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.scimitarpowered.api.Engine;
import org.scimitarpowered.api.action.Action;
import org.scimitarpowered.api.event.Event;
import org.scimitarpowered.api.event.EventSubscriber;
import org.scimitarpowered.api.utility.ScimitarUtility;
import org.scimitarpowered.api.world.entity.Entity;

import server.action.ScimitarActionDispatcher;
import server.entity.ScimitarWorld;
import server.event.ScimitarEventDispatcher;
import server.net.PipelineFactory;
import server.plugin.ScimitarPluginLoader;

/**
 * The main core of RuneSource.
 * 
 * @author blakeman8192
 */
public class ScimitarEngine implements Runnable, Engine {

	private static ScimitarEngine singleton;
	private static final Properties props = new Properties();
	private final ScimitarEventDispatcher eventDispatcher = new ScimitarEventDispatcher();
	private static ScimitarWorld world;
	
	private final String host;
	private final int port;
	private final int cycleRate;
	private final byte worldId;
	private boolean sleeping;
	private final String name;
	private boolean shutdown = false;

	private ScimitarPluginLoader pluginLoader;

	private InetSocketAddress address;
	private ScimitarUtility.Stopwatch cycleTimer;

	/**
	 * Creates a new Server.
	 * 
	 * @param host
	 *            the host
	 * @param port
	 *            the port
	 * @param cycleRate
	 *            the cycle rate
	 */
	private ScimitarEngine(String name, String host, int port, int cycleRate, byte worldId) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.cycleRate = cycleRate;
		this.worldId = worldId;
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		/*try {
			props.load(new FileInputStream("settings.props"));
		} catch(Exception e) {
			e.printStackTrace();
		}*/
		
		setSingleton(new ScimitarEngine("Scimitar", "127.0.0.1", 43594, 600, (byte) 1));
		setWorld(new ScimitarWorld());
		new Thread(getSingleton()).start();
	}

	@Override
	public void run() {
		try {

			address = new InetSocketAddress(host, port);
			System.out.println("Starting RuneSource on " + address + "...");

			pluginLoader = new ScimitarPluginLoader();
			pluginLoader.onServerStart();

			// Load configuration.
			//ItemDefinition.load();

			// Start up and get a'rollin!
			startup();
			System.out.println("Online!");
			while (!shutdown) {
				tick();
				sleep();
			}
			shutdown();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public void shutdown() {
		// perform shutdown tasks here.
		System.out.println("Shutting down...");
		ScimitarActionDispatcher.getInstance().destroy();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public String getIP() {
		return host;
	}

	@Override
	public void startup() throws IOException {
		// Initialize netty and begin listening for new clients
		ServerBootstrap serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
		serverBootstrap.setPipelineFactory(new PipelineFactory());
		serverBootstrap.bind(address);

		// Finally, initialize whatever else we need.
		cycleTimer = new ScimitarUtility.Stopwatch();
	}

	@Override
	public void tick() {
		// Next, perform game processing.
		try {
			world.process();
			ScimitarActionDispatcher.getInstance().tick();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void sleep() throws InterruptedException {
		long sleepTime = cycleRate - cycleTimer.elapsed();
		//System.out.println("players: " + PlayerHandler.playerAmount() + " tick time:" + (600 - sleepTime));
		if (sleepTime > 0) {
			sleeping = true;
			Thread.sleep(sleepTime);
		} else {
			// The server has reached maximum load, players may now lag.
			System.out.println("[WARNING]: Server load: " + (100 + (Math.abs(sleepTime) / (cycleRate / 100))) + "%!");
		}
		sleeping = false;
		cycleTimer.reset();
	}
	
	private static void setWorld(final ScimitarWorld world) {
		if (ScimitarEngine.world != null) {
			throw new IllegalStateException("World already exists!");
		}
		ScimitarEngine.world = world;
	}
	
	public static ScimitarWorld getWorld() {
		return world;
	}
	
	/**
	 * Sets the server singleton object.
	 * 
	 * @param singleton
	 *            the singleton
	 */
	private static void setSingleton(ScimitarEngine singleton) {
		if (ScimitarEngine.singleton != null) {
			throw new IllegalStateException("Singleton already set!");
		}
		ScimitarEngine.singleton = singleton;
	}

	/**
	 * Gets the server singleton object.
	 * 
	 * @return the singleton
	 */
	public static ScimitarEngine getSingleton() {
		return singleton;
	}

	/**
	 * Gets a setting from our properties file
	 * @param key
	 * @return
	 */
	public static String getSetting(String key) {
		String value = props.getProperty(key);
		if(value == null) {
			throw new IllegalStateException("no such setting " + key);
		}
		return value;
	}

	public byte getWorldId() {
		return worldId;
	}
	
	public ScimitarPluginLoader getPluginLoader() {
		return pluginLoader;
	}

	@Override
	public boolean isSleeping() {
		return sleeping;
	}

	@Override
	public <E extends Event> void registerEvent(
			EventSubscriber<E> subscriber) {
		eventDispatcher.register(subscriber);
	}

	@Override
	public <E extends Event> void deregisterEvent(
			EventSubscriber<E> subscriber) {
		eventDispatcher.deregister(subscriber);
	}

	@Override
	public void dispatchEvent(Event event) {
		eventDispatcher.dispatch(event);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void dispatchAction(Action<?> action) {
		ScimitarActionDispatcher.getInstance().schedule((Action<Entity>) action);
	}
	
}
