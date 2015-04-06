package server.net.packet.incoming;

import org.scimitarpowered.api.world.entity.character.player.Player;

import server.net.ScimitarPacket;

public interface IncomingPacketHandler {
	
	public abstract void handle(final Player player, final ScimitarPacket packet);

}
