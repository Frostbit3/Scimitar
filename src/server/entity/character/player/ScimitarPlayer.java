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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.scimitarpowered.api.event.character.CharacterEntityAttackEvent;
import org.scimitarpowered.api.event.character.CharacterEntityBlockEvent;
import org.scimitarpowered.api.event.character.CharacterEntityCombatEvent;
import org.scimitarpowered.api.event.character.CharacterEntityDeathEvent;
import org.scimitarpowered.api.net.packet.outgoing.impl.SkillUpdatePacket;
import org.scimitarpowered.api.utility.ScimitarUtility;
import org.scimitarpowered.api.world.entity.Entity;
import org.scimitarpowered.api.world.entity.EntityType;
import org.scimitarpowered.api.world.entity.Position;
import org.scimitarpowered.api.world.entity.character.CharacterEntity;
import org.scimitarpowered.api.world.entity.character.Hit;
import org.scimitarpowered.api.world.entity.character.Skill;
import org.scimitarpowered.api.world.entity.character.UpdateFlags.UpdateFlag;
import org.scimitarpowered.api.world.entity.character.player.Player;
import org.scimitarpowered.api.world.entity.character.player.PlayerIO;
import org.scimitarpowered.api.world.entity.character.player.Privacy;
import org.scimitarpowered.api.world.entity.character.player.Rank;
import org.scimitarpowered.api.world.entity.item.Item;

import server.ScimitarEngine;
import server.entity.character.ScimitarCharacterEntity;
import server.entity.item.ScimitarItem;
import server.entity.item.ScimitarItemDefinition;

/**
 * Represents a logged-in player.
 * 
 * @author blakeman8192
 */
public class ScimitarPlayer extends ScimitarCharacterEntity implements Player {
	
	private int pid;
	private String username;
	private String password;
	private final ScimitarPlayerIO playerIO;
	private Rank rank;
	private int chatColor;
	private int chatEffect;
	private byte[] chatText;
	private Privacy publicChat = Privacy.ON;
	private Privacy privateChat = Privacy.ON;
	private Privacy requestChat = Privacy.ON;
	private int gender = ScimitarUtility.GENDER_MALE;
	private final int[] appearance = new int[7];
	private final int[] colors = new int[5];
	private final int[] skills = new int[22];
	private final int[] experience = new int[22];
	
	// player items
	private Item[] equipment = new ScimitarItem[ScimitarPlayerConstants.EQUIPMENT_SIZE];
	private Item[] inventory = new ScimitarItem[ScimitarPlayerConstants.INVENTORY_SIZE];
	
	// friends ignores
	private List<Long> friends = new ArrayList<Long>();
	private List<Long> ignores = new ArrayList<Long>();
	
	// Client settings
	private byte brightness = 1;
	private boolean mouseButtons;
	private boolean splitScreen = false;
	private boolean acceptAid = false;
	private boolean retaliate = false;
	private boolean chatEffects = false;
	
	// Various player update flags.
	
	private boolean needsPlacement = false;
	private boolean resetMovementQueue = false;

	/**
	 * Creates a new Player.
	 * 
	 * @param key
	 *            the SelectionKey
	 */
	public ScimitarPlayer(ScimitarPlayerIO client) {
		this.playerIO = client;
		this.rank = Rank.PLAYER;
		
		// Set the default appearance.
		getAppearance()[ScimitarUtility.APPEARANCE_SLOT_CHEST] = 18;
		getAppearance()[ScimitarUtility.APPEARANCE_SLOT_ARMS] = 26;
		getAppearance()[ScimitarUtility.APPEARANCE_SLOT_LEGS] = 36;
		getAppearance()[ScimitarUtility.APPEARANCE_SLOT_HEAD] = 0;
		getAppearance()[ScimitarUtility.APPEARANCE_SLOT_HANDS] = 33;
		getAppearance()[ScimitarUtility.APPEARANCE_SLOT_FEET] = 42;
		getAppearance()[ScimitarUtility.APPEARANCE_SLOT_BEARD] = 10;

		// Set the default colors.
		getColors()[0] = 7;
		getColors()[1] = 8;
		getColors()[2] = 9;
		getColors()[3] = 5;
		getColors()[4] = 0;

		// Set all skills to 1.
		for (int i = 0; i < skills.length; i++) {
			if (i == 3) { // Hitpoints.
				skills[i] = 10;
				experience[i] = 1154;
			} else {
				skills[i] = 1;
				experience[i] = 0;
			}
		}	
	}
	
	@Override
	public void onInstigate(CharacterEntity entity, Hit hit) {
		ScimitarEngine.getSingleton().dispatchEvent(new CharacterEntityCombatEvent(this, entity, hit));
	}

	@Override
	public void onAttack(CharacterEntity entity, Hit hit) {
		ScimitarEngine.getSingleton().dispatchEvent(new CharacterEntityAttackEvent(this, entity, hit));
	}

	@Override
	public void onBlock(CharacterEntity entity, Hit hit) {
		ScimitarEngine.getSingleton().dispatchEvent(new CharacterEntityBlockEvent(this, entity, hit));
		
	}

	@Override
	public void onDeath(CharacterEntity entity, Hit hit) {
		ScimitarEngine.getSingleton().dispatchEvent(new CharacterEntityDeathEvent(this, entity, hit));
	}
	
	@Override
	public void onRespawn(Entity entity) {
		
	}

	/**
	 * Performs processing for this player.
	 * 
	 * @throws Exception
	 */
	
	@Override
	public void tick() throws Exception {
		// If no packet for more than 5 seconds, disconnect.
		/*if (getTimeoutStopwatch().elapsed() > 5000) {
			System.out.println(this + " timed out.");
			disconnect();
			return;
		}*/
		getMovementHandler().tick();
	}

	/**
	 * Sets the skill level.
	 * 
	 * @param skillID
	 *            the skill ID
	 * @param level
	 *            the level
	 */
	public void setSkill(int skillID, int level) {
		skills[skillID] = level;
		playerIO.sendPacket(new SkillUpdatePacket(skillID, experience[skillID], skills[skillID]));
	}

	/**
	 * Adds skill experience.
	 * 
	 * @param skillID
	 *            the skill ID
	 * @param exp
	 *            the experience to add
	 */
	public void addSkillExp(int skillID, int exp) {
		experience[skillID] += exp;
		playerIO.sendPacket(new SkillUpdatePacket(skillID, experience[skillID], skills[skillID]));
	}
	
	@Override
	public void sendMessage(String message) {
		playerIO.sendMessage(message);
	}

	/**
	 * Removes skill experience.
	 * 
	 * @param skillID
	 *            the skill ID
	 * @param exp
	 *            the experience to add
	 */
	public void removeSkillExp(int skillID, int exp) {
		experience[skillID] -= exp;
		playerIO.sendPacket(new SkillUpdatePacket(skillID, experience[skillID], skills[skillID]));
	}

	/**
	 * Handles a player command.
	 * 
	 * @param keyword
	 *            the command keyword
	 * @param args
	 *            the arguments (separated by spaces)
	 */
	@Override
	public void handleCommand(String keyword, String[] args) {
		if(keyword.equals("players")) {
			int count = 0;
			for(Player player : ScimitarEngine.getWorld().getPlayers()) {
				if(player != null)
					count++;
			}
			playerIO.sendMessage("Players online: " + count);
		}
		if (keyword.equals("setlevel")) {
			this.setSkill(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		}
		if (keyword.equals("master")) {
			for (int i = 0; i < skills.length; i++) {
				skills[i] = 99;
				experience[i] = 200000000;
			}
			playerIO.sendSkills();
		}
		if (keyword.equals("noob")) {
			for (int i = 0; i < skills.length; i++) {
				skills[i] = 1;
				experience[i] = 0;
			}
			playerIO.sendSkills();
		}
		if (keyword.equals("empty")) {
			emptyInventory();
		}
		if (keyword.equals("pickup")) {
			int id = Integer.parseInt(args[0]);
			int amount = 1;
			if (args.length > 1) {
				amount = Integer.parseInt(args[1]);
			}
			addInventoryItem(new ScimitarItem(id, amount));
			playerIO.sendInventory();
		}
		if (keyword.equals("tele")) {
			int x = Integer.parseInt(args[0]);
			int y = Integer.parseInt(args[1]);
			teleport(new Position(x, y, getPosition().getZ()));
		}
		if (keyword.equals("mypos")) {
			playerIO.sendMessage("You are at: " + getPosition());
		}
	}

	/**
	 * Equips an item.
	 * 
	 * @param slot
	 *            the inventory slot
	 */
	public void equip(int slot) {
		Item item = getInventory()[slot];
		
		if(item == null) {
			return;
		}
		
		int eSlot = ((ScimitarItemDefinition)item.getDefinition()).getSlot();
		if(eSlot == -1) {
			return;
		}
		
		if(getEquipment()[eSlot] != null) {
			unequip(eSlot);
		}
		
		getInventory()[slot] = null;
		getEquipment()[eSlot] = item;
		
		playerIO.sendEquipment(eSlot, item.getIndex(), item.getAmount());
		playerIO.sendInventory();
		getUpdateFlags().flag(UpdateFlag.APPEARANCE);
	}

	/**
	 * Unequips an item.
	 * 
	 * @param slot
	 *            the equipment slot.
	 */
	public void unequip(int slot) {
		Item item = getEquipment()[slot];
		if(addInventoryItem(item)) {
			getEquipment()[slot] = null;
			playerIO.sendEquipment(slot, -1, 0);
			playerIO.sendInventory();
			getUpdateFlags().flag(UpdateFlag.APPEARANCE);
		}
	}

	/**
	 * Empties the entire inventory.
	 */
	public void emptyInventory() {
		for(int i = 0; i < ScimitarPlayerConstants.EQUIPMENT_SIZE; i++) {
			getEquipment()[i] = null;
		}
		playerIO.sendInventory();
	}

	/**
	 * Attempts to add the item (and amount) to the inventory. This method will
	 * add as many of the desired item to the inventory as possible, even if not
	 * all can be added.
	 * 
	 * @param id
	 *            the item ID
	 * @param amount
	 *            the amount of the item
	 * @return whether or not the amount of the item could be added to the
	 *         inventory
	 */
	public boolean addInventoryItem(Item item) {
		if (((ScimitarItemDefinition)item.getDefinition()).isStackable()) {
			
			// Add the item to an existing stack if there is one.
			for(int i = 0; i < ScimitarPlayerConstants.INVENTORY_SIZE; i++) {
				Item inventItem = getInventory()[i];
				if(inventItem != null && inventItem.getIndex() == item.getIndex()) {
					inventItem.incrementAmountBy(item.getAmount());
					return true;
				}
			}
			
			// No stack, try to add the item stack to an empty slot.
			for(int i = 0; i < ScimitarPlayerConstants.INVENTORY_SIZE; i++) {
				Item inventItem = getInventory()[i];
				if(inventItem == null) {
					getInventory()[i] = item;
					return true;
				}		
			}
		} else {
			// Try to add the amount of items to empty slots.
			int amountAdded = 0;
			for(int i = 0; i < ScimitarPlayerConstants.INVENTORY_SIZE && amountAdded < item.getAmount(); i++) {
				Item inventItem = getInventory()[i];
				if(inventItem == null) {
					getInventory()[i] = new ScimitarItem(item.getIndex(), 1);
					amountAdded++;
				}		
			}
			
			// Check we added all of the items
			if(amountAdded >= item.getAmount()) {
				return true;
			}
		}
		playerIO.sendMessage("You do not have enough inventory space.");
		return false;
	}

	/**
	 * Removes the desired amount of the specified item from the inventory.
	 * 
	 * @param id
	 *            the item ID
	 * @param amount
	 *            the desired amount
	 */
	public void removeInventoryItem(Item item) {
		if (((ScimitarItemDefinition)item.getDefinition()).isStackable()) {
			// Find the existing stack (if there is one).
			for(int i = 0 ; i < ScimitarPlayerConstants.INVENTORY_SIZE; i++) {
				Item inventItem = getInventory()[i];
				if(inventItem != null && inventItem.getIndex() == item.getIndex()) {
					if(item.getAmount() >= inventItem.getAmount()) {
						getInventory()[i] = null;
						break;
					}
				}
			}
		} else {
			// Remove the desired amount.
			int amountRemoved = 0;
			for (int i = 0; i < ScimitarPlayerConstants.INVENTORY_SIZE && amountRemoved < item.getAmount(); i++) {
				Item inventItem = getInventory()[i];
				if(inventItem != null && inventItem.getIndex() == item.getIndex()) {
					getInventory()[i] = null;
					amountRemoved++;
				}
			}
		}
		playerIO.sendInventory();
	}

	/**
	 * Checks if the desired amount of the item is in the inventory.
	 * 
	 * @param id
	 *            the item ID
	 * @param amount
	 *            the item amount
	 * @return whether or not the player has the desired amount of the item in
	 *         the inventory
	 */
	public boolean hasInventoryItem(Item item) {
		if (((ScimitarItemDefinition)item.getDefinition()).isStackable()) {
			// Check if an existing stack has the amount of item.
			for(int i = 0 ; i < ScimitarPlayerConstants.INVENTORY_SIZE; i++) {
				Item inventItem = getInventory()[i];
				if(inventItem != null && inventItem.getIndex() == item.getIndex()) {
					return inventItem.getAmount() >= item.getAmount();
				}
			}
		} else {
			// Check if there are the amount of items.
			int amountFound = 0;
			for(int i = 0 ; i < ScimitarPlayerConstants.INVENTORY_SIZE; i++) {
				Item inventItem = getInventory()[i];
				if(inventItem != null && inventItem.getIndex() == item.getIndex()) {
					amountFound++;
				}
			}
			return amountFound >= item.getAmount();
		}
		return false;
	}

	/**
	 * Teleports the player to the desired position.
	 * 
	 * @param position
	 *            the position
	 */
	public void teleport(Position position) {
		getMovementHandler().reset();
		getPosition().setAs(position);
		setResetMovementQueue(true);
		setNeedsPlacement(true);
		playerIO.sendMapRegion();
	}

	/**
	 * Resets the player after updating.
	 */
	@Override
	public void reset() {
		setPrimaryDirection(-1);
		setSecondaryDirection(-1);
		getUpdateFlags().reset();
		setResetMovementQueue(false);
		setNeedsPlacement(false);
	}

	/**
	 * Sets the needsPlacement boolean.
	 * 
	 * @param needsPlacement
	 */
	@Override
	public void setNeedsPlacement(boolean needsPlacement) {
		this.needsPlacement = needsPlacement;
	}

	/**
	 * Gets whether or not the player needs to be placed.
	 * 
	 * @return the needsPlacement boolean
	 */
	@Override
	public boolean needsPlacement() {
		return needsPlacement;
	}

	@Override
	public void setResetMovementQueue(boolean resetMovementQueue) {
		this.resetMovementQueue = resetMovementQueue;
	}

	@Override
	public boolean isResetMovementQueue() {
		return resetMovementQueue;
	}

	@Override
	public void setChatColor(int chatColor) {
		this.chatColor = chatColor;
	}

	@Override
	public int getChatColor() {
		return chatColor;
	}

	@Override
	public void setChatEffects(int chatEffects) {
		this.chatEffect = chatEffects;
	}

	@Override
	public int getChatEffects() {
		return chatEffect;
	}

	@Override
	public void setChatText(byte[] chatText) {
		this.chatText = chatText;
	}

	@Override
	public byte[] getChatText() {
		return chatText;
	}

	@Override
	public int[] getSkills() {
		return skills;
	}

	@Override
	public int[] getExperience() {
		return experience;
	}

	@Override
	public int[] getAppearance() {
		return appearance;
	}

	@Override
	public int[] getColors() {
		return colors;
	}

	@Override
	public void setGender(int gender) {
		this.gender = gender;
	}

	@Override
	public int getGender() {
		return gender;
	}

	public byte getBrightness() {
		return brightness;
	}

	public void setBrightness(byte brightness) {
		this.brightness = brightness;
	}

	public boolean mouseButtons() {
		return mouseButtons;
	}

	public void setMouseButtons(boolean mouseButtons) {
		this.mouseButtons = mouseButtons;
	}

	public boolean splitScreen() {
		return splitScreen;
	}

	public void setSplitScreen(boolean splitScreen) {
		this.splitScreen = splitScreen;
	}

	public boolean acceptAid() {
		return acceptAid;
	}

	public void setAcceptAid(boolean acceptAid) {
		this.acceptAid = acceptAid;
	}

	public boolean retaliate() {
		return retaliate;
	}

	public void setRetaliate(boolean retaliate) {
		this.retaliate = retaliate;
	}

	public boolean chatEffects() {
		return chatEffects;
	}

	public void setChatEffects(boolean chatEffects) {
		this.chatEffects = chatEffects;
	}

	public List<Long> getFriends() {
		return friends;
	}

	public void setFriends(List<Long> friends) {
		this.friends = friends;
	}

	public List<Long> getIgnores() {
		return ignores;
	}

	public void setIgnores(List<Long> ignores) {
		this.ignores = ignores;
	}
	
	@Override
	public Item[] getInventory() {
		return inventory;
	}
	
	@Override
	public Item[] getEquipment() {
		return equipment;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	@Override
	public PlayerIO getIO() {
		return playerIO;
	}

	@Override
	public Skill[] getSkill() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UUID getUUID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUUID(UUID uuid) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public void setPassword(String password) {
		this.password = password; 
	}

	@Override
	public Rank getRank() {
		return rank;
	}

	@Override
	public void setRank(Rank newRank) {
		this.rank = newRank;
	}

	@Override
	public Privacy getFriendChatPrivacy() {
		return privateChat;
	}

	@Override
	public Privacy getPublicChatPrivacy() {
		return publicChat;
	}

	@Override
	public Privacy getRequestPrivacy() {
		return requestChat;
	}

	@Override
	public void setFriendChatPrivacy(Privacy friendChatPrivacy) {
		this.privateChat = friendChatPrivacy;
	}

	@Override
	public void setPublicChatPrivacy(Privacy publicChatPrivacy) {
		this.publicChat = publicChatPrivacy;
	}

	@Override
	public void setRequestPrivacy(Privacy requestPrivacy) {
		this.requestChat = requestPrivacy;
	}
	
	@Override
	public EntityType getType() { 
		return EntityType.PLAYER;
	}

}
