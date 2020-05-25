/* 
 * Copyright (C) 2020 Alexander Stojanovich <coas91@rocketmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rs.alexanderstojanovich.evg.level;

import java.util.List;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.critter.NPC;
import rs.alexanderstojanovich.evg.critter.Player;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LevelActors {

    private final Player player = new Player(null, "icosphere.obj", "marble.png", new Vector3f(10.5f, 0.0f, -3.0f), Vector3fColors.WHITE, 0.25f);
    private final List<NPC> npcList = new GapList<>();

    public void freeze() {
        player.setGivenControl(false);
        for (NPC npc : npcList) {
            npc.setGivenControl(false);
        }
    }

    public void unfreeze() {
        player.setGivenControl(true);
        for (NPC npc : npcList) {
            npc.setGivenControl(true);
        }
    }

    public void render() {
        player.render();
        for (NPC npc : npcList) {
            npc.render();
        }
    }

    public Player getPlayer() {
        return player;
    }

    public List<NPC> getNpcList() {
        return npcList;
    }

}
