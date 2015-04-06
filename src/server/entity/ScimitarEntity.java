package server.entity;

import org.scimitarpowered.api.world.entity.Entity;
import org.scimitarpowered.api.world.entity.EntityType;
import org.scimitarpowered.api.world.entity.Position;

import server.ScimitarEngine;

public abstract class ScimitarEntity implements Entity {
	
	private int slot = -1;
	private Entity interactingEntity;
	private boolean busy;
	private Position position = new Position((3222 + (int)Math.floor(Math.random() * 100)), (3222 + (int)Math.floor(Math.random() * 100)));
	private final Position currentRegion = new Position(0, 0, 0);
	
	@Override
	public int getIndex() {
		return slot;
	}

	@Override
	public void setIndex(int index) {
		this.slot = index;
	}
	
	@Override
	public ScimitarWorld getWorld() {
		return ScimitarEngine.getWorld();
	}

	@Override
	public Entity getInteractingEntity() {
		return interactingEntity;
	}

	@Override
	public void setInteractingEntity(Entity interactingEntity) {
		this.interactingEntity = interactingEntity;
	}
	
	@Override
	public boolean isBusy() {
		return busy;
	}
	
	@Override
	public void setBusy(boolean busy) {
		this.busy = busy;
	}
	
	/**
	 * Gets the entity's Position.
	 * 
	 * @return the position
	 */
	@Override
	public Position getPosition() {
		return position;
	}
	

	@Override
	public void setPosition(Position position) {
		this.position = position;
	}
	
	@Override
	public Position getCurrentRegion() {
		return currentRegion;
	}
	
	@Override
	public EntityType getType() { 
		return EntityType.ENTITY;
	}
	
}
