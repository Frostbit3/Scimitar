package server.entity.character.npc;

import org.scimitarpowered.api.world.entity.Entity;
import org.scimitarpowered.api.world.entity.EntityType;
import org.scimitarpowered.api.world.entity.character.CharacterEntity;
import org.scimitarpowered.api.world.entity.character.Hit;
import org.scimitarpowered.api.world.entity.character.Skill;
import org.scimitarpowered.api.world.entity.character.npc.NPC;

import server.entity.character.ScimitarCharacterEntity;

/**
 * A non-player-character. Extends Player so that we can share the many
 * attributes.
 * 
 * @author blakeman8192
 */
public class ScimitarNPC extends ScimitarCharacterEntity implements NPC {

	/** The NPC ID. */
	private int id;

	/** Whether or not the NPC is visible. */
	private boolean isVisible = true;

	/**
	 * Creates a new Npc.
	 * 
	 * @param npcId
	 *            the NPC ID
	 */
	public ScimitarNPC(int npcId) {
		this.setNpcId(npcId);
	}

	@Override
	public void tick() {
		// NPC-specific processing.
		getMovementHandler().tick();
	}

	@Override
	public void reset() {
		// TODO: Any other NPC resetting that isn't in Player.
	}

	/**
	 * Sets the NPC ID.
	 * 
	 * @param npcId
	 *            the npcId
	 */
	public void setNpcId(int npcId) {
		this.id = npcId;
	}

	/**
	 * Gets the NPC ID.
	 * 
	 * @return the npcId
	 */
	public int getNpcId() {
		return id;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}

	public boolean isVisible() {
		return isVisible;
	}

	@Override
	public void onInstigate(CharacterEntity entity, Hit hit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAttack(CharacterEntity entity, Hit hit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBlock(CharacterEntity entity, Hit hit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeath(CharacterEntity entity, Hit hit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Skill[] getSkill() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onRespawn(Entity entity) {
		
	}
	
	@Override
	public EntityType getType() { 
		return EntityType.NPC;
	}

}
