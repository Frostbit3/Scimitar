package server.entity.item;

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


import org.scimitarpowered.api.world.entity.Entity;
import org.scimitarpowered.api.world.entity.EntityType;
import org.scimitarpowered.api.world.entity.item.Item;

import server.entity.ScimitarEntity;

/**
 * An implementation of a game item.
 * @author Stuart Murphy
 * @author FrostBit3
 *
 */
public class ScimitarItem extends ScimitarEntity implements Item {
	
	private int amount;
	
	public ScimitarItem(int id, int amount) {
		this.setIndex(id);
		this.amount = amount;
	}
	@Override
	public void incrementAmount() {
		if(((long)amount + 1) > Integer.MAX_VALUE) {
			return;
		}
		amount++;
	}
	@Override
	public void decrementAmount() {
		if((amount - 1) < 0) {
			return;
		}
		amount--;
	}
	@Override
	public void incrementAmountBy(int amount) {
		if(((long)this.amount + amount) > Integer.MAX_VALUE) {
			this.amount = Integer.MAX_VALUE;
		} else {
			this.amount += amount;
		}
	}
	@Override
	public void decrementAmountBy(int amount) {
		if((this.amount - amount) < 1) {
			this.amount = 1;
		} else {
			this.amount -= amount;
		}
	}
	
	@Override
	public ScimitarItemDefinition getDefinition() {
		return ScimitarItemDefinition.getDefinition(getIndex());
	}

	/**
	 * Each item has its own tick.
	 */
	@Override
	public void tick() throws Exception {

	}

	@Override
	public void reset() {

	}
	
	@Override
	public int getAmount() {
		return amount;
	}
	
	@Override
	public void setAmount(int amount) {
		this.amount = amount;
	}
	
	@Override
	public void onRespawn(Entity entity) {}
	
	@Override
	public EntityType getType() { 
		return EntityType.ITEM;
	}

}