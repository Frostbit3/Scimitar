package server.entity.character.player;
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
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.scimitarpowered.api.event.character.player.PlayerLoginEvent;
import org.scimitarpowered.api.net.ISAACCipher;
import org.scimitarpowered.api.net.StreamBuffer;
import org.scimitarpowered.api.net.StreamBuffer.ByteOrder;
import org.scimitarpowered.api.net.packet.outgoing.OutgoingPacketHandler;
import org.scimitarpowered.api.net.packet.outgoing.impl.SkillUpdatePacket;
import org.scimitarpowered.api.utility.ScimitarUtility;
import org.scimitarpowered.api.world.entity.Position;
import org.scimitarpowered.api.world.entity.character.UpdateFlags.UpdateFlag;
import org.scimitarpowered.api.world.entity.character.player.Player;
import org.scimitarpowered.api.world.entity.character.player.PlayerIO;
import org.scimitarpowered.api.world.entity.character.player.Privacy;
import org.scimitarpowered.api.world.entity.item.Item;

import server.ScimitarEngine;
import server.net.ScimitarPacket;

/**
 * The class behind a Player that handles all networking-related things.
 * 
 * @author blakeman8192
 */
public class ScimitarPlayerIO implements PlayerIO {
	
	private final Channel channel;
	private final Queue<ScimitarPacket> queuedPackets = new ConcurrentLinkedQueue<ScimitarPacket>();
	
	private final Player player;
	private final ScimitarUtility.Stopwatch timeoutStopwatch = new ScimitarUtility.Stopwatch();
	private ISAACCipher encryptor;

	/**
	 * Creates a new Client.
	 * 
	 * @param key
	 *            the SelectionKey of the client
	 */
	public ScimitarPlayerIO(Channel channel) {
		this.channel = channel;
		this.player = new ScimitarPlayer(this);
	}

	/**
	 * Adds a packet to the queue
	 * @param packet
	 */
	public void queuePacket(ScimitarPacket packet) {
		queuedPackets.add(packet);
	}
	
	@Override
	public void sendPacket(OutgoingPacketHandler packet) {
		send(packet.handle(player, ScimitarEngine.getSingleton()).getBuffer());
	}
	
	/**
	 * Handles packets we have received
	 */
	public void processQueuedPackets() {
		ScimitarPacket packet = null;
		while((packet = queuedPackets.poll()) != null) {
			handlePacket(packet.getOpcode(), packet.getSize(), StreamBuffer.OutBuffer.newInBuffer(packet.getPayload()));
		}
	}
	
	/**
	 * Sends all skills to the client.
	 */
	public void sendSkills() {
		for (int i = 0; i < player.getSkills().length; i++) {
			sendPacket(new SkillUpdatePacket(i, player.getExperience()[i], player.getSkills()[i]));
		}
	}
	
//	/**
//	 * Tells the client we have logged into the friends server (spoofed)
//	 */
//	public void sendFriendsListUpdate() {
//		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(2);
//		out.writeHeader(getEncryptor(), 221);
//		out.writeByte(2);
//		send(out.getBuffer());
//	}
//
//	/**
//	 * Sends the friends list to the player, really only used when logging in
//	 */
//	public void sendFriendsList() {
//		for(long l : player.getFriends()) {
//			if(l == 0) {
//				continue;
//			}
//			byte status = 0;
//			Player plr = ScimitarEngine.getWorld().getPlayerByName(ScimitarUtility.longToName(l));
//			if(plr != null) {
//				if(plr.getFriendChatPrivacy() == Privacy.ON) {
//					status = ScimitarEngine.getSingleton().getWorldId();
//				} else if(plr.getFriendChatPrivacy() == Privacy.FRIENDS) {
//					if(plr.getIO().hasFriend(ScimitarUtility.nameToLong(player.getUsername()))) {
//						status = ScimitarEngine.getSingleton().getWorldId();
//					}
//				}
//			}
//			sendFriendUpdate(l, status);
//		}
//	}
//	
//	public boolean hasFriend(long friend) {
//		return player.getFriends().contains(friend);
//	}
//	
//	/**
//	 * Tells all your friends that your private chat status has changed.
//	 * @param status The status of the players private chat
//	 */
//	public void updateOtherFriends(Privacy status) {
//		long myName = ScimitarUtility.nameToLong(player.getUsername());
//		for(Player plr : ScimitarEngine.getWorld().getPlayers()) {
//			if(plr == null || plr == player) {
//				continue;
//			}
//			if(plr.getIO().hasFriend(myName)) {
//				byte world = 0;
//				if(status == Privacy.ON) {
//					world = ScimitarEngine.getSingleton().getWorldId();
//				} else if(status == Privacy.FRIENDS) {
//					if(player.getIO().hasFriend(ScimitarUtility.nameToLong(plr.getUsername()))) {
//						world = ScimitarEngine.getSingleton().getWorldId();
//					}
//				}
//				plr.getIO().sendFriendUpdate(myName, world);
//			}
//		}
//	}
//	
//	/**
//	 * Sends the ignore list to the client
//	 */
//	public void sendIgnoreList() {
//		if(player.getIgnores().size() == 0) {
//			return;
//		}
//		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer((player.getIgnores().size() * 8) + 3);
//		out.writeVariableShortPacketHeader(getEncryptor(), 214);
//		for(long i : player.getIgnores()) {
//			out.writeLong(i);
//		}
//		out.finishVariableShortPacketHeader();
//		send(out.getBuffer());
//	}
//
//	
//	/**
//	 * Sends a friend update to the friends list indicating which world they are on
//	 * @param name The username as a long
//	 * @param world The world the player is on
//	 */
//	public void sendFriendUpdate(long name, byte world) {
//		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(10);
//		out.writeHeader(getEncryptor(), 50);
//		out.writeLong(name);
//		out.writeByte(world);
//		send(out.getBuffer());
//	}
//	
//	/**
//	 * Sends a private message
//	 * @param name The name of the person sending the message as a long
//	 * @param rights Rights of the player
//	 * @param message The message itself
//	 */
//	public void sendPrivateMessage(long name, byte rights, byte[] message) {
//		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(15 + message.length);
//		out.writeVariablePacketHeader(getEncryptor(), 196);
//		out.writeLong(name);
//		out.writeInt(new Random().nextInt());
//		out.writeByte(rights);
//		for(int i = 0; i < message.length; i++) {
//			out.writeByte(message[i]);
//		}
//		out.finishVariablePacketHeader();
//		send(out.getBuffer());
//	}

	/**
	 * Sends all equipment.
	 */
	public void sendEquipment() {
		for (int i = 0; i < ScimitarPlayerConstants.EQUIPMENT_SIZE; i++) {
			Item item = player.getEquipment()[i];
			if(item != null) {
				sendEquipment(i, item.getIndex(), item.getAmount());
			} else {
				sendEquipment(i, -1, 0);
			}
		}
	}

	/**
	 * Sends the equipment to the client.
	 * 
	 * @param slot
	 *            the equipment slot
	 * @param itemID
	 *            the item ID
	 * @param itemAmount
	 *            the item amount
	 */
	public void sendEquipment(int slot, int itemID, int itemAmount) {
		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(32);
		out.writeVariableShortPacketHeader(getEncryptor(), 34);
		out.writeShort(1688);
		out.writeByte(slot);
		out.writeShort(itemID + 1);
		if (itemAmount > 254) {
			out.writeByte(255);
			out.writeShort(itemAmount);
		} else {
			out.writeByte(itemAmount);
		}
		out.finishVariableShortPacketHeader();
		send(out.getBuffer());
	}

	/**
	 * Sends the current full inventory.
	 */
	public void sendInventory() {
		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(256);
		out.writeVariableShortPacketHeader(getEncryptor(), 53);
		out.writeShort(3214);
		out.writeShort(ScimitarPlayerConstants.INVENTORY_SIZE);
		for (int i = 0; i < ScimitarPlayerConstants.INVENTORY_SIZE; i++) {
			Item item = player.getInventory()[i];
			if(item != null && item.getAmount() > 254) {
				out.writeByte(255);
				out.writeInt(item.getAmount(), StreamBuffer.ByteOrder.INVERSE_MIDDLE);
			} else {
				out.writeByte(item != null ? item.getAmount() : 0);
			}
			out.writeShort(item != null ? item.getIndex() + 1 : 0, StreamBuffer.ValueType.A, StreamBuffer.ByteOrder.LITTLE);
		}
		out.finishVariableShortPacketHeader();
		send(out.getBuffer());
	}

	/**
	 * Sends a message to the players chat box.
	 * 
	 * @param message
	 *            the message
	 */
	public void sendMessage(String message) {
		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(message.length() + 3);
		out.writeVariablePacketHeader(getEncryptor(), 253);
		out.writeString(message);
		out.finishVariablePacketHeader();
		send(out.getBuffer());
	}
	
	/**
	 * Sends the chat options under the chatbox
	 */
	public void sendChatOptions() {
		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(4);
		out.writeHeader(getEncryptor(), 206);
		out.writeByte(player.getPublicChatPrivacy().asByte());
		out.writeByte(player.getFriendChatPrivacy().asByte());
		out.writeByte(player.getRequestPrivacy().asByte());
		send(out.getBuffer());
	}

	/**
	 * Sends a sidebar interface.
	 * 
	 * @param menuId
	 *            the interface slot
	 * @param form
	 *            the interface ID
	 */
	public void sendSidebarInterface(int menuId, int form) {
		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(4);
		out.writeHeader(getEncryptor(), 71);
		out.writeShort(form);
		out.writeByte(menuId, StreamBuffer.ValueType.A);
		send(out.getBuffer());
	}

	/**
	 * Refreshes the map region.
	 */
	public void sendMapRegion() {
		player.getCurrentRegion().setAs(player.getPosition());
		player.setNeedsPlacement(true);
		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(5);
		out.writeHeader(getEncryptor(), 73);
		out.writeShort(player.getPosition().getRegionX() + 6, StreamBuffer.ValueType.A);
		out.writeShort(player.getPosition().getRegionY() + 6);
		send(out.getBuffer());
	}

	/**
	 * Disconnects the client.
	 */
	public void disconnect() {
		System.out.println(this + " disconnecting.");
		try {
			logout();
			channel.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Handles a clicked button.
	 * 
	 * @param buttonId
	 *            the button ID
	 */
	private void handleButton(int buttonId) {
		switch (buttonId) {
		case 9154:
			sendLogout();
			break;
		case 153:
			player.getMovementHandler().setRunToggled(true);
			break;
		case 152:
			player.getMovementHandler().setRunToggled(false);
			break;
		case 5451:
		case 5452:
			//player.setBrightness((byte) 0);
			break;
		case 6273:
		case 6157:
			//player.setBrightness((byte) 1);
			break;
		case 6275:
		case 6274:
			//player.setBrightness((byte) 2);
			break;
		case 6277:
		case 6276:
			//player.setBrightness((byte) 3);
			break;
		case 6279:
			//player.setMouseButtons(true);
			break;
		case 6278:
			//player.setMouseButtons(false);
			break;
		case 6280:
			//player.setChatEffects(true);
			break;
		case 6281:
			//player.setChatEffects(false);
			break;
		case 952:
			//player.setSplitScreen(true);
			break;
		case 953:
			//player.setSplitScreen(false);
			break;
		case 12591:
			//player.setAcceptAid(true);
			break;
		case 12590:
			//player.setAcceptAid(false);
			break;
		case 150:
			//player.setRetaliate(true);
			break;
		case 151:
			//player.setRetaliate(false);
			break;
		default:
			System.out.println("Unhandled button: " + buttonId);
			break;
		}
	}

	/**
	 * Sends a packet that tells the client to log out.
	 */
	public void sendLogout() {
		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(1);
		out.writeHeader(getEncryptor(), 109);
		send(out.getBuffer());
	}
	
	/**
	 * Sends a configuration update
	 * 
	 * @param id
	 *            The id of the config to update
	 * @param state
	 *            The state to update to
	 */
	public void sendConfig(int id, int state) {
		StreamBuffer.OutBuffer out = StreamBuffer.newOutBuffer(4);
		out.writeHeader(getEncryptor(), 36);
		out.writeShort(id, ByteOrder.LITTLE);
		out.writeByte(state);
		send(out.getBuffer());
	}

	/**
	 * Handles the current packet.
	 */
	private void handlePacket(int packetOpcode, int packetLength, StreamBuffer.InBuffer in) {
		timeoutStopwatch.reset();
		// Handle the packet.
		try {
			switch (packetOpcode) {
			case 145: // Remove item.
				int interfaceID = in.readShort(StreamBuffer.ValueType.A);
				int slot = in.readShort(StreamBuffer.ValueType.A);
				in.readShort(StreamBuffer.ValueType.A); // Item ID.
				if (interfaceID == 1688) {
					//player.unequip(slot);
				}
				break;
			case 41: // Equip item.
				in.readShort(); // Item ID.
				slot = in.readShort(StreamBuffer.ValueType.A);
				in.readShort(); // Interface ID.
				//player.equip(slot);
				break;
			case 185: // Button clicking.
				int buttonId = ScimitarUtility.hexToInt(in.readBytes(2));
				handleButton(buttonId);
				//Server.getSingleton().getEventDispatcher().dispatch(new ButtonInteractEvent(player, buttonId));
				break;
			case 4: // Player chat.
				int effects = in.readByte(false, StreamBuffer.ValueType.S);
				int color = in.readByte(false, StreamBuffer.ValueType.S);
				int chatLength = (packetLength - 2);
				byte[] text = in.readBytesReverse(chatLength, StreamBuffer.ValueType.A);
				player.setChatEffects(effects);
				player.setChatColor(color);
				player.setChatText(text);
				player.getUpdateFlags().flag(UpdateFlag.CHAT);
				break;
			case 103: // Player command.
				String command = in.readString();
				String[] split = command.split(" ");
				player.handleCommand(split[0].toLowerCase(), Arrays.copyOfRange(split, 1, split.length));
				break;
			case 248: // Movement.
			case 164: // ^
			case 98: // ^
				int length = packetLength;
				if (packetOpcode == 248) {
					length -= 14;
				}
				int steps = (length - 5) / 2;
				int[][] path = new int[steps][2];
				int firstStepX = in.readShort(StreamBuffer.ValueType.A, StreamBuffer.ByteOrder.LITTLE);
				for (int i = 0; i < steps; i++) {
					path[i][0] = in.readByte();
					path[i][1] = in.readByte();
				}
				int firstStepY = in.readShort(StreamBuffer.ByteOrder.LITTLE);

				player.getMovementHandler().reset();
				player.getMovementHandler().setRunPath(in.readByte(StreamBuffer.ValueType.C) == 1);
				player.getMovementHandler().addToPath(new Position(firstStepX, firstStepY));
				for (int i = 0; i < steps; i++) {
					path[i][0] += firstStepX;
					path[i][1] += firstStepY;
					player.getMovementHandler().addToPath(new Position(path[i][0], path[i][1]));
				}
				player.getMovementHandler().finish();
				break;
			case 95: // Chat option changing
				byte status = (byte) in.readByte();
				if (status >= 0 && status <= 3) {
					player.setPublicChatPrivacy(Privacy.values()[status]);
				}
				status = (byte) in.readByte();
				if (status >= 0 && status <= 3) {
					player.setFriendChatPrivacy(Privacy.values()[status]);
					//updateOtherFriends(Privacy.values()[status]); // TODO
				}
				status = (byte) in.readByte();
				if (status >= 0 && status <= 3) {
					player.setRequestPrivacy(Privacy.values()[status]);
				}
				break;
			case 188: // Add friend TODO
//				long friend = in.readLong();
//				if(player.getFriends().size() >= 200) {
//					sendMessage("Friends list is full.");
//					break;
//				}
//				if(player.getFriends().contains(friend)) {
//					sendMessage("That player is already on your friends list.");
//					break;
//				}
//				player.getFriends().add(friend);
//				Player plr = ScimitarEngine.getWorld().getPlayerByName(ScimitarUtility.longToName(friend));
//				byte world = 0;
//				if(plr != null) {
//					if(plr.getFriendChatPrivacy() == Privacy.ON) {
//						world = ScimitarEngine.getSingleton().getWorldId();
//					} else if(plr.getFriendChatPrivacy() == Privacy.FRIENDS) {
//						if(plr.getIO().hasFriend(ScimitarUtility.nameToLong(player.getUsername()))) {
//							world = ScimitarEngine.getSingleton().getWorldId();
//						}
//					}
//					if(plr.getFriendChatPrivacy() == Privacy.FRIENDS && plr.getIO().hasFriend(ScimitarUtility.nameToLong(player.getUsername()))) {
//						plr.getIO().sendFriendUpdate(ScimitarUtility.nameToLong(player.getUsername()), ScimitarEngine.getSingleton().getWorldId());
//					}
//				}
//				sendFriendUpdate(friend, world);
				break;
			case 215: // Remove friend TODO
//				friend = in.readLong();
//				if(!player.getFriends().contains(friend)) {
//					sendMessage("That player is not on your friends list.");
//					break;
//				}
//				player.getFriends().remove(friend);
//				if(plr.getFriendChatPrivacy() == Privacy.FRIENDS) {
//					plr = ScimitarEngine.getWorld().getPlayerByName(ScimitarUtility.longToName(friend));
//					if(plr != null) {
//						plr.getIO().sendFriendUpdate(ScimitarUtility.nameToLong(player.getUsername()), (byte)0);
//					}
//				}
				break;
			case 126: // Send pm TODO
//				friend = in.readLong();
//				if(!player.getFriends().contains(friend)) {
//					sendMessage("That player is not on your friends list.");
//					break;
//				}
//				plr = ScimitarEngine.getWorld().getPlayerByName(ScimitarUtility.longToName(friend));
//				if(plr == null) {
//					sendMessage("That player is currently offline.");
//					break;
//				}
//				int size = packetLength - 8;
//				byte[] message = in.readBytes(size);
//				plr.getIO().sendPrivateMessage(ScimitarUtility.nameToLong(player.getUsername()), player.getRank().asByte(), message);
				break;
			case 74: // Remove ignore TODO
//				long ignore = in.readLong();
//				if(!player.getIgnores().contains(ignore)) {
//					sendMessage("That player is not on your ignore list.");
//					break;
//				}
//				player.getIgnores().remove(ignore);
				break;
			case 133: // Add ignore TODO
//				ignore = in.readLong();
//				if(player.getIgnores().size() >= 100) {
//					sendMessage("Ignore list is full.");
//					break;
//				}
//				if(player.getIgnores().contains(ignore)) {
//					sendMessage("That player is already on your ignore list.");
//					break;
//				}
//				player.getIgnores().add(ignore);
				break;
			case 0: // Empty packets
			case 3:
			case 202:
			case 77:
			case 86:
			case 78:
			case 36:
			case 226:
			case 246:
			case 148:
			case 183:
			case 230:
			case 136:
			case 189:
			case 152:
			case 200:
			case 85:
			case 165:
			case 238:
			case 150:
				break;
			default:
				System.out.println(this + " unhandled packet received " + packetOpcode + " - " + packetLength);
				break;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void login() throws Exception {
		int response = ScimitarUtility.LOGIN_RESPONSE_OK;

		// Check if the player is already logged in.
		for (Player player : ScimitarEngine.getWorld().getPlayers()) {
			if (player == null) {
				continue;
			}
			if (player.getUsername().equals(player.getUsername())) {
				response = ScimitarUtility.LOGIN_RESPONSE_ACCOUNT_ONLINE;
			}
		}

		// Load the player and send the login response.
		int status = ScimitarPlayerFile.load(player);
		
		if(status == 1) {
			response = ScimitarUtility.LOGIN_RESPONSE_NEED_MEMBERS;
		} else if(status == 2) {
			response = ScimitarUtility.LOGIN_RESPONSE_INVALID_CREDENTIALS;
		}
		
		StreamBuffer.OutBuffer resp = StreamBuffer.newOutBuffer(3);
		resp.writeByte(response);
		resp.writeByte(player.getRank().asByte());
		resp.writeByte(0);
		send(resp.getBuffer());
		if (response != 2) {
			disconnect();
			return;
		}

		ScimitarEngine.getWorld().register(player);
		sendMapRegion();
		ScimitarEngine.getSingleton().dispatchEvent(new PlayerLoginEvent(player));
		sendInventory();
		sendSkills();
		sendEquipment();
		player.getUpdateFlags().flag(UpdateFlag.APPEARANCE);
		sendSidebarInterface(1, 3917);
		sendSidebarInterface(2, 638);
		sendSidebarInterface(3, 3213);
		sendSidebarInterface(4, 1644);
		sendSidebarInterface(5, 5608);
		sendSidebarInterface(6, 1151);
		sendSidebarInterface(8, 5065);
		sendSidebarInterface(9, 5715);
		sendSidebarInterface(10, 2449);
		sendSidebarInterface(11, 4445);
		sendSidebarInterface(12, 147);
		sendSidebarInterface(13, 6299);
		sendSidebarInterface(0, 2423);
//		sendConfig(166, player.getBrightness() + 1);
//		sendConfig(287, (player.splitScreen() ? 1 : 0));
//		sendConfig(170, (player.mouseButtons() ? 1 : 0));
//		sendConfig(171, (!player.chatEffects() ? 1 : 0));
//		sendConfig(427, (player.acceptAid() ? 1 : 0));
//		sendConfig(173, (player.getMovementHandler().isRunToggled() ? 1 : 0));
//		sendConfig(172, (player.retaliate() ? 0 : 1));
		sendChatOptions();
		//TODO
//		sendFriendsListUpdate();
//		sendFriendsList();
//		sendIgnoreList();
//		updateOtherFriends(player.getPrivateChat());
		
		sendMessage("Welcome to " + ScimitarEngine.getSingleton().getName());

		//System.out.println(this + " has logged in.");
	}

	public void logout() throws Exception {
//		updateOtherFriends(2); TODO
		ScimitarEngine.getWorld().unregister(player);
		System.out.println(this + " has logged out.");
		if (player.getIndex() != -1) {
			ScimitarPlayerFile.save(player);
		}
	}

	/**
	 * Sends the buffer to the socket.
	 * 
	 * @param buffer
	 *            the buffer
	 * @throws IOException
	 */
	public void send(ChannelBuffer buffer) {
		if(channel == null || !channel.isConnected()) {
			return;
		}
		channel.write(buffer);
	}
	
	@Override
	public String toString() {
		return player.getUsername() == null ? "Client(" + getHost() + ")" : "Player(" + player.getUsername() + ":" + player.getPassword() + " - " + getHost() + ")";
	}

	/**
	 * Gets the remote host of the client.
	 * 
	 * @return the host
	 */
	public String getHost() {
		if(channel == null) {
			return "unknown";
		}
		return ((InetSocketAddress) channel.getRemoteAddress()).getAddress().getHostAddress();
	}
	
	/**
	 * Sets the encryptor.
	 * 
	 * @param encryptor
	 *            the encryptor
	 */
	public void setEncryptor(ISAACCipher encryptor) {
		this.encryptor = encryptor;
	}

	/**
	 * Gets the encryptor.
	 * 
	 * @return the encryptor
	 */
	public ISAACCipher getEncryptor() {
		return encryptor;
	}

	/**
	 * Gets the Player subclass implementation of this superclass.
	 * 
	 * @return the player
	 */
	@Override
	public Player getPlayer() {
		return player;
	}

	public ScimitarUtility.Stopwatch getTimeoutStopwatch() {
		return timeoutStopwatch;
	}
	
}
