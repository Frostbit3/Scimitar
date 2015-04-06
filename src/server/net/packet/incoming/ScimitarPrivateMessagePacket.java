package server.net.packet.incoming;

import org.scimitarpowered.api.event.character.player.ReceivePrivateMessageEvent;
import org.scimitarpowered.api.net.packet.outgoing.impl.PrivateMessagePacket;
import org.scimitarpowered.api.utility.ScimitarUtility;
import org.scimitarpowered.api.world.entity.character.player.Player;

import server.ScimitarEngine;
import server.net.ScimitarPacket;

public class ScimitarPrivateMessagePacket implements IncomingPacketHandler {

	@Override
	public void handle(Player player, ScimitarPacket packet) {
		long friendNameAsLong = packet.getPayload().readLong();
		Player friend = ScimitarEngine.getWorld().getPlayerByName(
				ScimitarUtility.longToName(friendNameAsLong));
		int size = packet.getSize() - 8; // TODO: figure out what '8' is.
		byte[] message = packet.getPayload().readBytes(size).array();
		
		final String unpackedMessage = ScimitarUtility.textUnpack(message, message.length);
		ScimitarEngine.getSingleton().dispatchEvent(
				new ReceivePrivateMessageEvent(player,
						friend.getUsername(), unpackedMessage));
		player.getIO().sendPacket(new PrivateMessagePacket(
				player.getUsername(), player.getRank(), unpackedMessage));
	}

}
