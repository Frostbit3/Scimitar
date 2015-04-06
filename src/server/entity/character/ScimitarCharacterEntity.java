package server.entity.character;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.scimitarpowered.api.world.entity.EntityType;
import org.scimitarpowered.api.world.entity.character.CharacterEntity;
import org.scimitarpowered.api.world.entity.character.MovementHandler;
import org.scimitarpowered.api.world.entity.character.UpdateFlags;
import org.scimitarpowered.api.world.entity.character.npc.NPC;
import org.scimitarpowered.api.world.entity.character.player.Player;

import server.entity.ScimitarEntity;

public abstract class ScimitarCharacterEntity extends ScimitarEntity implements CharacterEntity {

	private final MovementHandler movementHandler = new ScimitarMovementHandler(this);
	private int primaryDirection = -1;
	private int secondaryDirection = -1;
	private final Map<Integer, Player> players = new HashMap<Integer, Player>();
	private final List<NPC> npcs = new LinkedList<NPC>();
	private final UpdateFlags updateFlags = new UpdateFlags();

	@Override
	public MovementHandler getMovementHandler() {
		return movementHandler;
	}

	@Override
	public int getPrimaryDirection() {
		return primaryDirection;
	}

	@Override
	public void setPrimaryDirection(int primaryDirection) {
		this.primaryDirection = primaryDirection;
	}

	@Override
	public int getSecondaryDirection() {
		return secondaryDirection;
	}

	@Override
	public void setSecondaryDirection(int secondaryDirection) {
		this.secondaryDirection = secondaryDirection;
	}

	@Override
	public Map<Integer, Player> getPlayers() {
		return players;
	}

	@Override
	public List<NPC> getNpcs() {
		return npcs;
	}

	@Override
	public UpdateFlags getUpdateFlags() {
		return updateFlags;
	}
	
	@Override
	public EntityType getType() { 
		return EntityType.CHARACTER;
	}

}
