package server.entity;
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


import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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



import org.scimitarpowered.api.world.World;
import org.scimitarpowered.api.world.entity.character.npc.NPC;
import org.scimitarpowered.api.world.entity.character.player.Player;

import server.entity.character.npc.ScimitarNPC;
import server.entity.character.npc.ScimitarNPCUpdating;
import server.entity.character.player.ScimitarPlayer;
import server.entity.character.player.ScimitarPlayerIO;
import server.entity.character.player.ScimitarPlayerUpdating;

/**
 * Handles all logged in players.
 * 
 * @author blakeman8192
 */
public class ScimitarWorld implements World {

	/** All registered players. */
	private final Player[] players = new Player[2048];

	/** All registered NPCs. */
	private final NPC[] npcs = new NPC[8192];

	/**
	 * Login queue
	 */
	private final Queue<Player> queuedLogins = new ConcurrentLinkedQueue<Player>();

	/**
	 * Performs the processing of all players.
	 * 
	 * @throws Exception
	 */
	public void process() throws Exception {

		/**
		 * Process logins
		 */
		Player plr = null;
		while ((plr = queuedLogins.poll()) != null) {
			plr.getIO().login();
		}

		/**
		 * Process packets for each players
		 */
		for (int i = 0; i < players.length; i++) {
			Player player = players[i];
			if (player == null) {
				continue;
			}
			try {
				((ScimitarPlayerIO) player.getIO()).processQueuedPackets();
			} catch (Exception ex) {
				ex.printStackTrace();
				player.getIO().disconnect();
			}
		}

		// Perform any logic processing for players.
		for (int i = 0; i < players.length; i++) {
			Player player = players[i];
			if (player == null) {
				continue;
			}
			try {
				player.tick();
			} catch (Exception ex) {
				ex.printStackTrace();
				player.getIO().disconnect();
			}
		}

		// Perform any logic processing for NPCs.
		for (int i = 0; i < npcs.length; i++) {
			NPC npc = npcs[i];
			if (npc == null) {
				continue;
			}
			try {
				npc.tick();
			} catch (Exception ex) {
				ex.printStackTrace();
				unregister(npc);
			}
		}

		// Update all players.
		for (int i = 0; i < players.length; i++) {
			Player player = players[i];
			if (player == null) {
				continue;
			}
			try {
				ScimitarPlayerUpdating.update(player);
				ScimitarNPCUpdating.update(player);
			} catch (Exception ex) {
				ex.printStackTrace();
				player.getIO().disconnect();
			}
		}

		// Reset all players after cycle.
		for (int i = 0; i < players.length; i++) {
			Player player = players[i];
			if (player == null) {
				continue;
			}
			try {
				player.reset();
			} catch (Exception ex) {
				ex.printStackTrace();
				player.getIO().disconnect();
			}
		}

		// Reset all NPCs after cycle.
		for (int i = 0; i < npcs.length; i++) {
			NPC npc = npcs[i];
			if (npc == null) {
				continue;
			}
			try {
				npc.reset();
			} catch (Exception ex) {
				ex.printStackTrace();
				unregister(npc);
			}
		}
	}

	/**
	 * Registers a player for processing.
	 * 
	 * @param player
	 *            the player
	 */
	@Override
	public void register(Player player) {
		for (int i = 1; i < players.length; i++) {
			if (players[i] == null) {
				players[i] = (ScimitarPlayer) player;
				player.setIndex(i);
				return;
			}
		}
		throw new IllegalStateException("Server is full!");
	}

	/**
	 * Registers an NPC for processing.
	 * 
	 * @param npc
	 *            the npc
	 */
	@Override
	public void register(NPC npc) {
		for (int i = 1; i < npcs.length; i++) {
			if (npcs[i] == null) {
				npcs[i] = (ScimitarNPC) npc;
				npc.setIndex(i);
				return;
			}
		}
		throw new IllegalStateException("Server is full!");
	}

	/**
	 * Unregisters a player from processing.
	 * 
	 * @param player
	 *            the player
	 */
	@Override
	public void unregister(Player player) {
		if (player.getIndex() == -1) {
			return;
		}
		players[player.getIndex()] = null;
	}

	/**
	 * Unregisters an NPC from processing.
	 * 
	 * @param npc
	 *            the npc
	 */
	@Override
	public void unregister(NPC npc) {
		if (npc.getIndex() == -1) {
			return;
		}
		npcs[npc.getIndex()] = null;
	}

	/**
	 * Gets the amount of players that are online.
	 * 
	 * @return the amount of online players
	 */
	@Override
	public int getPlayerCount() {
		int amount = 0;
		for (int i = 1; i < players.length; i++) {
			if (players[i] != null) {
				amount++;
			}
		}
		return amount;
	}

	/**
	 * Gets the amoutn of NPCs that are online.
	 * 
	 * @return the amount of online NPCs
	 */
	@Override
	public int getNPCCount() {
		int amount = 0;
		for (int i = 1; i < npcs.length; i++) {
			if (npcs[i] != null) {
				amount++;
			}
		}
		return amount;
	}
	
	@Override
	public int getEntityCount() {
		return 0;
	}
	
	@Override
	public int getItemCount() {
		return 0;
	}
	
	@Override
	public int getCharacterEntityCount() {
		return 0;
	}

	/**
	 * Gets all registered players.
	 * 
	 * @return the players
	 */
	@Override
	public Player[] getPlayers() {
		return players;
	}

	/**
	 * Gets all registered NPCs.
	 * 
	 * @return the npcs
	 */
	@Override
	public NPC[] getNPCs() {
		return npcs;
	}

	/**
	 * Queues a successful login
	 * 
	 * @param player
	 */
	public void queueLogin(Player player) {
		queuedLogins.add(player);
	}

	/**
	 * Gets a players instance by username
	 * 
	 * @param name
	 *            The username
	 * @return The player instance
	 */
	@Override
	public Player getPlayerByName(String name) {
		for (Player player : players) {
			if (player == null)
				continue;
			if (player.getUsername().equalsIgnoreCase(name))
				return player;
		}
		return null;
	}

}
