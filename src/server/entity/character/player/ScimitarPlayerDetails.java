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

import java.util.List;

import org.scimitarpowered.api.world.entity.character.player.Privacy;

public class ScimitarPlayerDetails {
	
	public int gender;
	public int[] apperance;
	public int[] colors;
	public byte brightness;
	public boolean mouseButtons;
	public boolean splitScreen;
	public boolean acceptAid;
	public boolean chatEffects;
	public Privacy publicChat;
	public Privacy privateChat;
	public Privacy tradeCompete;
	public boolean running;
	public List<Long> friends;
	public List<Long> ignores;
	
	public ScimitarPlayerDetails() {
		
	}
	
	public ScimitarPlayerDetails(ScimitarPlayer player) {
		gender = player.getGender();
		apperance = player.getAppearance();
		colors = player.getColors();
		brightness = player.getBrightness();
		mouseButtons = player.mouseButtons();
		splitScreen = player.splitScreen();
		acceptAid = player.acceptAid();
		chatEffects = player.chatEffects();
		publicChat = player.getPublicChatPrivacy();
		privateChat = player.getFriendChatPrivacy();
		tradeCompete = player.getRequestPrivacy();
		running = player.getMovementHandler().isRunToggled();
		friends = player.getFriends();
		ignores = player.getIgnores();
	}

	public void populatePlayer(ScimitarPlayer player) {
		player.setGender(gender);
		player.setBrightness(brightness);
		player.setMouseButtons(mouseButtons);
		player.setSplitScreen(splitScreen);
		player.setAcceptAid(acceptAid);
		player.setChatEffects(chatEffects);
		player.setPublicChatPrivacy(publicChat);
		player.setFriendChatPrivacy(privateChat);
		player.setRequestPrivacy(tradeCompete);
		player.getMovementHandler().setRunToggled(running);
		player.setFriends(friends);
		player.setIgnores(ignores);
	}
	
}
