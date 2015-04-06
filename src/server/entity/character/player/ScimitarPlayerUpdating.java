package server.entity.character.player;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.scimitarpowered.api.net.StreamBuffer;
import org.scimitarpowered.api.utility.ScimitarUtility;
import org.scimitarpowered.api.world.entity.Position;
import org.scimitarpowered.api.world.entity.character.UpdateFlags.UpdateFlag;
import org.scimitarpowered.api.world.entity.character.player.Player;
import org.scimitarpowered.api.world.entity.item.Item;

import server.ScimitarEngine;
import server.entity.item.ScimitarItemDefinition;

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

/**
 * Provides static utility methods for updating players.
 * 
 * @author blakeman8192
 */
public final class ScimitarPlayerUpdating {

	/**
	 * Updates the player.
	 * 
	 * @param player
	 *            the player
	 */
	public static void update(Player player) {
		// XXX: The buffer sizes may need to be tuned.
		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(10240);
		StreamBuffer.OutBuffer block = StreamBuffer.newOutBuffer(10000);

		// Initialize the update packet.
		out.writeVariableShortPacketHeader(player.getIO().getEncryptor(), 81);
		out.setAccessType(StreamBuffer.AccessType.BIT_ACCESS);

		// Update this player.
		ScimitarPlayerUpdating.updateLocalPlayerMovement(player, out);
		if (player.getUpdateFlags().isUpdateRequired()) {
			ScimitarPlayerUpdating.updateState(player, block, false, true);
		}

		// Update other local players.
		out.writeBits(8, player.getPlayers().size());
		
		Iterator<Entry<Integer, Player>> it = player.getPlayers().entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<Integer, Player> pairs = (Map.Entry<Integer, Player>)it.next();
			if (pairs.getValue().getPosition().isViewableFrom(player.getPosition())) {
				ScimitarPlayerUpdating.updateOtherPlayerMovement(pairs.getValue(), out);
				if (pairs.getValue().getUpdateFlags().isUpdateRequired()) {
					ScimitarPlayerUpdating.updateState(pairs.getValue(), block, false, false);
				}
			} else {
				out.writeBit(true);
				out.writeBits(2, 3);
				it.remove();
			}
		}
		
		// Update the local player list.
		for (int i = 0; i < ScimitarEngine.getWorld().getPlayers().length; i++) {
			if (player.getPlayers().size() >= 255) {
				// Player limit has been reached.
				break;
			}
			Player other = ScimitarEngine.getWorld().getPlayers()[i];
			if (other == null || other == player) {
				continue;
			}
			if (!player.getPlayers().containsKey(i) && other.getPosition().isViewableFrom(player.getPosition())) {
				player.getPlayers().put(i, other);
				ScimitarPlayerUpdating.addPlayer(out, player, other);
				ScimitarPlayerUpdating.updateState(other, block, true, false);
			}
		}

		// Append the attributes block to the main packet.
		if (block.getBuffer().writerIndex() > 0) {
			out.writeBits(11, 2047);
			out.setAccessType(StreamBuffer.AccessType.BYTE_ACCESS);
			out.writeBytes(block.getBuffer());
		} else {
			out.setAccessType(StreamBuffer.AccessType.BYTE_ACCESS);
		}

		// Finish the packet and send it.
		out.finishVariableShortPacketHeader();
		((ScimitarPlayerIO) player.getIO()).send(out.getBuffer());
	}

	/**
	 * Appends the state of a player's chat to a buffer.
	 * 
	 * @param player
	 *            the player
	 * @param out
	 *            the buffer
	 */
	public static void appendChat(Player player, StreamBuffer.OutBuffer out) {
		out.writeShort(((player.getChatColor() & 0xff) << 8) + (player.getChatEffects() & 0xff), StreamBuffer.ByteOrder.LITTLE);
		out.writeByte(player.getRank().asByte());
		out.writeByte(player.getChatText().length, StreamBuffer.ValueType.C);
		out.writeBytesReverse(player.getChatText());
	}

	/**
	 * Appends the state of a player's appearance to a buffer.
	 * 
	 * @param player
	 *            the player
	 * @param out
	 *            the buffer
	 */
	public static void appendAppearance(Player player, StreamBuffer.OutBuffer out) {
		StreamBuffer.OutBuffer block = StreamBuffer.newOutBuffer(128);

		block.writeByte(0); // Gender
		block.writeByte(0); // Skull icon

		// Hat.
		Item helm = player.getEquipment()[ScimitarUtility.EQUIPMENT_SLOT_HEAD];
		if (helm != null) {
			block.writeShort(0x200 + helm.getIndex());
		} else {
			block.writeByte(0);
		}

		// Cape.
		Item item = player.getEquipment()[ScimitarUtility.EQUIPMENT_SLOT_CAPE];
		if (item != null) {
			block.writeShort(0x200 + item.getIndex());
		} else {
			block.writeByte(0);
		}

		// Amulet.
		item = player.getEquipment()[ScimitarUtility.EQUIPMENT_SLOT_AMULET];
		if (item != null) {
			block.writeShort(0x200 + item.getIndex());
		} else {
			block.writeByte(0);
		}

		// Weapon.
		item = player.getEquipment()[ScimitarUtility.EQUIPMENT_SLOT_WEAPON];
		if (item != null) {
			block.writeShort(0x200 + item.getIndex());
		} else {
			block.writeByte(0);
		}

		// Chest.
		Item chest = player.getEquipment()[ScimitarUtility.EQUIPMENT_SLOT_CHEST];
		if (chest != null) {
			block.writeShort(0x200 + chest.getIndex());
		} else {
			block.writeShort(0x100 + player.getAppearance()[ScimitarUtility.APPEARANCE_SLOT_CHEST]);
		}

		// Shield.
		item = player.getEquipment()[ScimitarUtility.EQUIPMENT_SLOT_CHEST];
		if (item != null) {
			block.writeShort(0x200 + item.getIndex());
		} else {
			block.writeByte(0);
		}
		
		// Arms
		if(chest != null && ((ScimitarItemDefinition) chest.getDefinition()).isFullMask()) {
			block.writeShort(0x200 + chest.getIndex());
		} else {
			block.writeShort(0x100 + player.getAppearance()[ScimitarUtility.APPEARANCE_SLOT_ARMS]);
		}

		// Legs.
		item = player.getEquipment()[ScimitarUtility.EQUIPMENT_SLOT_LEGS];
		if (item != null) {
			block.writeShort(0x200 + item.getIndex());
		} else {
			block.writeShort(0x100 + player.getAppearance()[ScimitarUtility.APPEARANCE_SLOT_LEGS]);
		}

		// Head (with a hat already on).
		if (helm != null && ((ScimitarItemDefinition) helm.getDefinition()).isFullMask()) {
			block.writeByte(0);
		} else {
			block.writeShort(0x100 + player.getAppearance()[ScimitarUtility.APPEARANCE_SLOT_HEAD]);
		}

		// Hands.
		item = player.getEquipment()[ScimitarUtility.EQUIPMENT_SLOT_HANDS];
		if (item != null) {
			block.writeShort(0x200 + item.getIndex());
		} else {
			block.writeShort(0x100 + player.getAppearance()[ScimitarUtility.APPEARANCE_SLOT_HANDS]);
		}

		// Feet.
		item = player.getEquipment()[ScimitarUtility.EQUIPMENT_SLOT_FEET];
		if (item != null) {
			block.writeShort(0x200 + item.getIndex());
		} else {
			block.writeShort(0x100 + player.getAppearance()[ScimitarUtility.APPEARANCE_SLOT_FEET]);
		}

		// Beard.
		item = player.getEquipment()[ScimitarUtility.EQUIPMENT_SLOT_HEAD];
		if (player.getGender() == ScimitarUtility.GENDER_MALE && (helm == null || (helm != null && !((ScimitarItemDefinition) helm.getDefinition()).isFullMask()))) {
			block.writeShort(0x100 + player.getAppearance()[ScimitarUtility.APPEARANCE_SLOT_BEARD]);
		} else {
			block.writeByte(0);
		}

		// Player colors
		block.writeByte(player.getColors()[0]);
		block.writeByte(player.getColors()[1]);
		block.writeByte(player.getColors()[2]);
		block.writeByte(player.getColors()[3]);
		block.writeByte(player.getColors()[4]);

		// Movement animations
		block.writeShort(0x328); // stand
		block.writeShort(0x337); // stand turn
		block.writeShort(0x333); // walk
		block.writeShort(0x334); // turn 180
		block.writeShort(0x335); // turn 90 cw
		block.writeShort(0x336); // turn 90 ccw
		block.writeShort(0x338); // run

		block.writeLong(ScimitarUtility.nameToLong(player.getUsername()));
		block.writeByte(3); // Combat level.
		block.writeShort(0); // Total level.

		// Append the block length and the block to the packet.
		out.writeByte(block.getBuffer().writerIndex(), StreamBuffer.ValueType.C);
		out.writeBytes(block.getBuffer());
	}

	/**
	 * Adds a player to the local player list of another player.
	 * 
	 * @param out
	 *            the packet to write to
	 * @param player
	 *            the host player
	 * @param other
	 *            the player being added
	 */
	public static void addPlayer(StreamBuffer.OutBuffer out, Player player, Player other) {
		out.writeBits(11, other.getIndex()); // Server slot.
		out.writeBit(true); // Yes, an update is required.
		out.writeBit(true); // Discard walking queue(?)

		// Write the relative position.
		Position delta = ScimitarUtility.delta(player.getPosition(), other.getPosition());
		out.writeBits(5, delta.getY());
		out.writeBits(5, delta.getX());
	}

	/**
	 * Updates movement for this local player. The difference between this
	 * method and the other player method is that this will make use of sector
	 * 2,3 to place the player in a specific position while sector 2,3 is not
	 * present in updating of other players (it simply flags local list removal
	 * instead).
	 * 
	 * @param player
	 * @param out
	 */
	public static void updateLocalPlayerMovement(Player player, StreamBuffer.OutBuffer out) {
		boolean updateRequired = player.getUpdateFlags().isUpdateRequired();
		if (player.needsPlacement()) { // Do they need placement?
			out.writeBit(true); // Yes, there is an update.
			int posX = player.getPosition().getLocalX(player.getCurrentRegion());
			int posY = player.getPosition().getLocalY(player.getCurrentRegion());
			appendPlacement(out, posX, posY, player.getPosition().getZ(), player.isResetMovementQueue(), updateRequired);
			player.setNeedsPlacement(false);
		} else { // No placement update, check for movement.
			int pDir = player.getPrimaryDirection();
			int sDir = player.getSecondaryDirection();
			if (pDir != -1) { // If they moved.
				out.writeBit(true); // Yes, there is an update.
				if (sDir != -1) { // If they ran.
					appendRun(out, pDir, sDir, updateRequired);
				} else { // Movement but no running - they walked.
					appendWalk(out, pDir, updateRequired);
				}
			} else { // No movement.
				if (updateRequired) { // Does the state need to be updated?
					out.writeBit(true); // Yes, there is an update.
					appendStand(out);
				} else { // No update whatsoever.
					out.writeBit(false);
				}
			}
		}
	}

	/**
	 * Updates the movement of a player for another player (does not make use of
	 * sector 2,3).
	 * 
	 * @param player
	 *            the player
	 * @param out
	 *            the packet
	 */
	public static void updateOtherPlayerMovement(Player player, StreamBuffer.OutBuffer out) {
		boolean updateRequired = player.getUpdateFlags().isUpdateRequired();
		int pDir = player.getPrimaryDirection();
		int sDir = player.getSecondaryDirection();
		if (pDir != -1) { // If they moved.
			out.writeBit(true); // Yes, there is an update.
			if (sDir != -1) { // If they ran.
				appendRun(out, pDir, sDir, updateRequired);
			} else { // Movement but no running - they walked.
				appendWalk(out, pDir, updateRequired);
			}
		} else { // No movement.
			if (updateRequired) { // Does the state need to be updated?
				out.writeBit(true); // Yes, there is an update.
				appendStand(out);
			} else { // No update whatsoever.
				out.writeBit(false);
			}
		}
	}

	/**
	 * Updates the state of a player.
	 * 
	 * @param player
	 *            the player
	 * @param block
	 *            the block
	 */
	public static void updateState(Player player, StreamBuffer.OutBuffer block, boolean forceAppearance, boolean noChat) {

		// First we must prepare the mask.
		int mask = 0x0;
		if (player.getUpdateFlags().get(UpdateFlag.CHAT) && !noChat) {
			mask |= 0x80;
		}
		if (player.getUpdateFlags().get(UpdateFlag.APPEARANCE) || forceAppearance) {
			mask |= 0x10;
		}

		// Now, we write the actual mask.
		if (mask >= 0x100) {
			mask |= 0x40;
			block.writeShort(mask, StreamBuffer.ByteOrder.LITTLE);
		} else {
			block.writeByte(mask);
		}

		// Finally, we append the attributes blocks.
		// Graphics
		// Animation
		// Forced chat
		if (player.getUpdateFlags().get(UpdateFlag.CHAT) && !noChat) {
			appendChat(player, block);
		}
		// Face entity
		// Appearance
		if (player.getUpdateFlags().get(UpdateFlag.APPEARANCE) || forceAppearance) {
			appendAppearance(player, block);
		}
		// Face coordinates
		// Primary hit
		// Secondary hit
	}

	/**
	 * Appends the stand version of the movement section of the update packet
	 * (sector 2,0). Appending this (instead of just a zero bit) automatically
	 * assumes that there is a required attribute update afterwards.
	 * 
	 * @param out
	 *            the buffer to append to
	 */
	public static void appendStand(StreamBuffer.OutBuffer out) {
		out.writeBits(2, 0); // 0 - no movement.
	}

	/**
	 * Appends the walk version of the movement section of the update packet
	 * (sector 2,1).
	 * 
	 * @param out
	 *            the buffer to append to
	 * @param direction
	 *            the walking direction
	 * @param attributesUpdate
	 *            whether or not a player attributes update is required
	 */
	public static void appendWalk(StreamBuffer.OutBuffer out, int direction, boolean attributesUpdate) {
		out.writeBits(2, 1); // 1 - walking.

		// Append the actual sector.
		out.writeBits(3, direction);
		out.writeBit(attributesUpdate);
	}

	/**
	 * Appends the walk version of the movement section of the update packet
	 * (sector 2,2).
	 * 
	 * @param out
	 *            the buffer to append to
	 * @param direction
	 *            the walking direction
	 * @param direction2
	 *            the running direction
	 * @param attributesUpdate
	 *            whether or not a player attributes update is required
	 */
	public static void appendRun(StreamBuffer.OutBuffer out, int direction, int direction2, boolean attributesUpdate) {
		out.writeBits(2, 2); // 2 - running.

		// Append the actual sector.
		out.writeBits(3, direction);
		out.writeBits(3, direction2);
		out.writeBit(attributesUpdate);
	}

	/**
	 * Appends the player placement version of the movement section of the
	 * update packet (sector 2,3). Note that by others this was previously
	 * called the "teleport update".
	 * 
	 * @param out
	 *            the buffer to append to
	 * @param localX
	 *            the local X coordinate
	 * @param localY
	 *            the local Y coordinate
	 * @param z
	 *            the Z coordinate
	 * @param discardMovementQueue
	 *            whether or not the client should discard the movement queue
	 * @param attributesUpdate
	 *            whether or not a plater attributes update is required
	 */
	public static void appendPlacement(StreamBuffer.OutBuffer out, int localX, int localY, int z, boolean discardMovementQueue, boolean attributesUpdate) {
		out.writeBits(2, 3); // 3 - placement.

		// Append the actual sector.
		out.writeBits(2, z);
		out.writeBit(discardMovementQueue);
		out.writeBit(attributesUpdate);
		out.writeBits(7, localY);
		out.writeBits(7, localX);
	}

}
