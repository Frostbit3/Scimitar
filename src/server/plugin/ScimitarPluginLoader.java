package server.plugin;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.scimitarpowered.api.plugin.BasePlugin;
import org.scimitarpowered.api.plugin.PluginWrapper;

/**
 *  Copyright (c) 2014, john01dav
	All rights reserved.
	
	Redistribution and use in source and binary forms, with or without
	modification, are permitted provided that the following conditions are met:
	    * Redistributions of source code must retain the above copyright
	      notice, this list of conditions and the following disclaimer.
	    * Redistributions in binary form must reproduce the above copyright
	      notice, this list of conditions and the following disclaimer in the
	      documentation and/or other materials provided with the distribution.
	    * Neither the name of the <organization> nor the
	      names of its contributors may be used to endorse or promote products
	      derived from this software without specific prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
	ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
	WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
	DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
	DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
	(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
	ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
	(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
 * @author john01dav
 *
 */
public class ScimitarPluginLoader {
	private ArrayList<ScimitarPluginFile> pluginFile;
	private ArrayList<PluginWrapper> pluginWrapper;
	private URLClassLoader loader;
	private File pluginsFolder;
	private Logger logger = Logger.getLogger("RS2");

	public void onServerStart() {
		pluginsFolder = new File("plugins");

		try {
			if (!pluginsFolder.isDirectory()) {
				Files.move(pluginsFolder, new File("./plugins-file"));
				pluginsFolder.delete();
			}

			if (!pluginsFolder.exists()) {
				pluginsFolder.mkdir();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}

		pluginFile = new ArrayList<ScimitarPluginFile>();
		pluginWrapper = new ArrayList<PluginWrapper>();
		ArrayList<URL> pluginURL = new ArrayList<URL>();

		logger.info("Loading plugins...");
		for (File file : pluginsFolder.listFiles()) {
			logger.info("Loading " + file.getAbsolutePath());
			if (file.getName().endsWith(".jar") || (!file.isDirectory())) {
				ScimitarPluginFile pFile = new ScimitarPluginFile(file).load();

				if (pFile.getName() == null) {
					logger.log(
							Level.INFO,
							"Plugin "
									+ file.getAbsolutePath()
									+ " does not have a name set, please add the name to plugindata.txt");
				} else if (pFile.getMainClass() == null) {
					logger.log(
							Level.INFO,
							"Plugin "
									+ file.getAbsolutePath()
									+ " does not have a mainClass set, please add the name to plugindata.txt");
				} else {
					pluginFile.add(pFile);
					pluginURL.add(pFile.getURL());
					logger.info("Loaded[" + pluginFile.size() + "] " + pFile.getName());
				}
			} else {
				logger.info("Skipping " + file.getAbsolutePath()
						+ " not a plugin.");
			}
		}
		logger.info("Plugins loaded");

		loader = new URLClassLoader(
				pluginURL.toArray(new URL[pluginURL.size()]), getClass()
						.getClassLoader());

		logger.info("Starting plugins");
		for (ScimitarPluginFile pFile : pluginFile) {
			try {
				startPlugin(pFile, loader);
			} catch (ClassNotFoundException e) {
				logger.log(
						Level.INFO,
						"Failed to load plugin " + pFile.getName()
								+ ". Can not load main class "
								+ pFile.getMainClass() + ".");
				e.printStackTrace();
			} catch (InstantiationException e) {
				logger.log(
						Level.INFO,
						"Failed to load plugin " + pFile.getName()
								+ ". Can not instantiate main class "
								+ pFile.getMainClass() + ". ("
								+ e.getClass().getCanonicalName() + ")");
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				logger.log(
						Level.INFO,
						"Failed to load plugin " + pFile.getName()
								+ ". Can not instantiate main class "
								+ pFile.getMainClass() + ". ("
								+ e.getClass().getCanonicalName() + ")");
				e.printStackTrace();
			} catch (ClassCastException e) {
				logger.log(
						Level.INFO,
						"Failed to load plugin "
								+ pFile.getName()
								+ ". Can not instantiate main class is not instanceof "
								+ BasePlugin.class.getCanonicalName() + ".");
				e.printStackTrace();
			} catch (NullPointerException e) {
				logger.log(Level.INFO,
						"Failed to load plugin " + pFile.getName()
								+ ". Could not load main class "
								+ BasePlugin.class.getCanonicalName()
								+ ". (nullpointerexception)");
				e.printStackTrace();
			}
		}
	}

	private void startPlugin(ScimitarPluginFile pFile, ClassLoader loader)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, ClassCastException, NullPointerException {
		logger.info("Starting " + pFile.getName());
		String mainClass = pFile.getMainClass();

		if (loader == null) {
			System.out.println("Classloader null?!");
		}

		Class<?> pluginClass = loader.loadClass(mainClass);
		Object instance = pluginClass.newInstance();
		BasePlugin plugin = ((BasePlugin) instance);
		PluginWrapper wrapper = new ScimitarPluginWrapper(plugin,
				pFile.getName());
		pluginWrapper.add(wrapper);

		plugin.onEnable();

		logger.info(pFile.getName() + " started");
	}

	/**
	 * Returns a list of all plugins in their wrappers
	 * 
	 * @return A list of all plugins in their wrappers
	 */
	public ArrayList<PluginWrapper> getPlugins() {
		return pluginWrapper;
	}

	public File getPluginsFolder() {
		return pluginsFolder;
	}

}
