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


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.scimitarpowered.api.cache.definition.ItemDefinition;
import org.scimitarpowered.api.utility.ScimitarUtility;

import server.util.ScimitarDatabaseUtility;

/**
 * 
 * @author Stuart Murphy
 *
 */
public class ScimitarItemDefinition implements ItemDefinition {
	
	private static ScimitarItemDefinition[] definitions;
	
	public static ScimitarItemDefinition getDefinition(int id) {
		return definitions[id];
	}
	
	public static void load() throws SQLException {
		System.out.println("loading item definitions");
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Connection conn = ScimitarDatabaseUtility.getConnection();
			ps = conn.prepareStatement("select id from item_definitions order by id desc limit 1");
			rs = ps.executeQuery();
			if(rs.next()) {
				definitions = new ScimitarItemDefinition[rs.getInt("id") + 1];
			}

			ps = conn.prepareStatement("select * from item_definitions");
			rs = ps.executeQuery();
			while(rs.next()) {
				ScimitarItemDefinition def = new ScimitarItemDefinition();
				def.setId(rs.getInt("id"));
				def.setName(rs.getString("name"));
				def.setExamine(rs.getString("examine"));
				def.setNoted(rs.getBoolean("noted"));
				def.setNoteable(rs.getBoolean("noteable"));
				def.setStackable(rs.getBoolean("stackable"));
				def.setParentId(rs.getInt("parent_id"));
				def.setNotedId(rs.getInt("noted_id"));
				def.setMembers(rs.getBoolean("members"));
				def.setShopValue(rs.getInt("shop_value"));
				def.setHighAlchValue(rs.getInt("high_alch_value"));
				def.setLowAlchValue(rs.getInt("low_alch_value"));
				def.setWeight(rs.getDouble("weight"));
				def.setSlot(rs.getInt("slot"));
				def.setFullMask(rs.getBoolean("full_mask"));
				def.setBonuses(ScimitarUtility.Json.getInstance().fromJson(rs.getString("bonuses"), int[].class));
				definitions[rs.getInt("id")] = def;
			}
		} finally {
			ScimitarDatabaseUtility.close(rs);
			ScimitarDatabaseUtility.close(ps);
		}
		System.out.println("loaded " + definitions.length + " item definitions");
	}
	
	private int id;
	private String name;
	private String examine;
	private boolean noted;
	private boolean noteable;
	private boolean stackable;
	private int parentId;
	private int notedId;
	private boolean members;
	private int shopValue;
	private int highAlchValue;
	private int lowAlchValue;
	private int[] bonuses;
	private double weight;
	private int slot;
	private boolean fullMask;
	
	public int getId() {
		return id;
	}
	
	private void setId(int id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	private void setName(String name) {
		this.name = name;
	}
	
	public String getExamine() {
		return examine;
	}
	
	private void setExamine(String examine) {
		this.examine = examine;
	}
	
	public boolean isNoted() {
		return noted;
	}
	
	private void setNoted(boolean noted) {
		this.noted = noted;
	}
	
	public boolean isNoteable() {
		return noteable;
	}
	
	private void setNoteable(boolean noteable) {
		this.noteable = noteable;
	}
	
	public boolean isStackable() {
		return stackable;
	}
	
	private void setStackable(boolean stackable) {
		this.stackable = stackable;
	}
	
	public int getParentId() {
		return parentId;
	}
	
	private void setParentId(int parentId) {
		this.parentId = parentId;
	}
	
	public int getNotedId() {
		return notedId;
	}
	
	private void setNotedId(int notedId) {
		this.notedId = notedId;
	}
	
	public boolean isMembers() {
		return members;
	}
	
	private void setMembers(boolean members) {
		this.members = members;
	}
	
	public int getShopValue() {
		return shopValue;
	}
	
	private void setShopValue(int shopValue) {
		this.shopValue = shopValue;
	}
	
	public int getHighAlchValue() {
		return highAlchValue;
	}
	
	private void setHighAlchValue(int highAlchValue) {
		this.highAlchValue = highAlchValue;
	}
	
	public int getLowAlchValue() {
		return lowAlchValue;
	}
	
	private void setLowAlchValue(int lowAlchValue) {
		this.lowAlchValue = lowAlchValue;
	}
	
	public double getWeight() {
		return weight;
	}
	
	private void setWeight(double weight) {
		this.weight = weight;
	}

	public int[] getBonuses() {
		return bonuses;
	}

	private void setBonuses(int[] bonuses) {
		this.bonuses = bonuses;
	}

	public int getSlot() {
		return slot;
	}

	private void setSlot(int slot) {
		this.slot = slot;
	}

	public boolean isFullMask() {
		return fullMask;
	}
	
	private void setFullMask(boolean fullMask) {
		this.fullMask = fullMask;
	}

}
