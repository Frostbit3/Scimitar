package server.entity.character.npc;
import java.util.Iterator;

import org.scimitarpowered.api.net.StreamBuffer;
import org.scimitarpowered.api.utility.ScimitarUtility;
import org.scimitarpowered.api.world.entity.Position;
import org.scimitarpowered.api.world.entity.character.npc.NPC;
import org.scimitarpowered.api.world.entity.character.player.Player;

import server.ScimitarEngine;
import server.entity.character.player.ScimitarPlayerIO;

/**
 * Provides static utility methods for updating NPCs.
 * 
 * @author blakeman8192
 */
public class ScimitarNPCUpdating {

	/**
	 * Updates all NPCs for the argued Player.
	 * 
	 * @param player
	 *            the player
	 */
	public static void update(Player player) {
		// XXX: The buffer sizes may need to be tuned.
		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(2048);
		StreamBuffer.OutBuffer block = StreamBuffer.newOutBuffer(1024);

		// Initialize the update packet.
		out.writeVariableShortPacketHeader(player.getIO().getEncryptor(), 65);
		out.setAccessType(StreamBuffer.AccessType.BIT_ACCESS);

		// Update the NPCs in the local list.
		out.writeBits(8, player.getNpcs().size());
		
		for (Iterator<NPC> i = player.getNpcs().iterator(); i.hasNext();) {
			NPC npc = i.next();
			if (npc.getPosition().isViewableFrom(player.getPosition()) && npc.isVisible()) {
				ScimitarNPCUpdating.updateNpcMovement(out, npc);
				if (npc.getUpdateFlags().isUpdateRequired()) {
					ScimitarNPCUpdating.updateState(block, npc);
				}
			} else {
				// Remove the NPC from the local list.
				out.writeBit(true);
				out.writeBits(2, 3);
				i.remove();
			}
		}

		// Update the local NPC list itself.
		for (int i = 0; i < ScimitarEngine.getWorld().getNPCs().length; i++) {
			NPC npc = ScimitarEngine.getWorld().getNPCs()[i];
			if (npc == null || player.getNpcs().contains(npc) || !npc.isVisible()) {
				continue;
			}
			if (npc.getPosition().isViewableFrom(player.getPosition())) {
				player.getNpcs().add(npc);
				addNpc(out, player, npc);
				if (npc.getUpdateFlags().isUpdateRequired()) {
					ScimitarNPCUpdating.updateState(block, npc);
				}
			}
		}

		// Append the update block to the packet if need be.
		if (block.getBuffer().writerIndex() > 0) {
			out.writeBits(14, 16383);
			out.setAccessType(StreamBuffer.AccessType.BYTE_ACCESS);
			out.writeBytes(block.getBuffer());
		} else {
			out.setAccessType(StreamBuffer.AccessType.BYTE_ACCESS);
		}

		// Ship the packet out to the client.
		out.finishVariableShortPacketHeader();
		((ScimitarPlayerIO) player.getIO()).send(out.getBuffer());
	}

	/**
	 * Adds the NPC to the clientside local list.
	 * 
	 * @param out
	 *            The buffer to write to
	 * @param player
	 *            The player
	 * @param npc
	 *            The NPC being added
	 */
	private static void addNpc(StreamBuffer.OutBuffer out, Player player, NPC npc) {
		out.writeBits(14, npc.getIndex());
		Position delta = ScimitarUtility.delta(player.getPosition(), npc.getPosition());
		out.writeBits(5, delta.getY());
		out.writeBits(5, delta.getX());
		out.writeBits(1, 0);
		out.writeBits(12, npc.getNpcId());
		out.writeBits(1, npc.getUpdateFlags().isUpdateRequired() ? 1 : 0);
	}

	/**
	 * Updates the movement of a NPC for this cycle.
	 * 
	 * @param out
	 *            The buffer to write to
	 * @param npc
	 *            The NPC to update
	 */
	private static void updateNpcMovement(StreamBuffer.OutBuffer out, NPC npc) {
		if (npc.getPrimaryDirection() == -1) {
			if (npc.getUpdateFlags().isUpdateRequired()) {
				out.writeBit(true);
				out.writeBits(2, 0);
			} else {
				out.writeBit(false);
			}
		} else {
			out.writeBit(true);
			out.writeBits(2, 1);
			out.writeBits(3, npc.getPrimaryDirection());
			out.writeBit(true);
		}
	}

	/**
	 * Updates the state of the NPC to the given update block.
	 * 
	 * @param block
	 *            The update block to append to
	 * @param npc
	 *            The NPC to update
	 */
	private static void updateState(StreamBuffer.OutBuffer block, NPC npc) {
		int mask = 0x0;
		
		
		block.writeByte(mask);
	}

}
