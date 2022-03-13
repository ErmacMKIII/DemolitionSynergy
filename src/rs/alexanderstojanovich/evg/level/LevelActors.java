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
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.critter.NPC;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.critter.Player;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LevelActors {

    protected final Observer observer = new Observer(new Vector3f());
    protected final Player player = new Player(observer.getCamera(), null, Model.readFromObjFile(Game.CHARACTER_ENTRY, "Player1_Sheriff.obj", "water"));
        //(Game.CHARACTER_ENTRY, null, "Player1_Sheriff.obj", "marble", new Vector3f(10.5f, 0.0f, -3.0f), Vector3fColors.WHITE, 1.0f);
    protected final List<NPC> npcList = new GapList<>();    
    
    public void freeze() {
        getMainActor().setGivenControl(false);
        for (NPC npc : npcList) {
            npc.setGivenControl(false);
        }
    }

    public void unfreeze() {
        getMainActor().setGivenControl(true);
        for (NPC npc : npcList) {
            npc.setGivenControl(true);
        }
    }

    public void render(List<Vector3f> lightSrc, ShaderProgram shaderProgram) {        
        for (NPC npc : npcList) {
            npc.render(lightSrc, shaderProgram);
        }
                
        getMainActor().render(lightSrc, shaderProgram);
    }

    public Observer getObserver() {
        return observer;
    }

    public Critter getMainActor() {
        if (Game.getCurrentMode() == Game.Mode.SINGLE_PLAYER) {                    
            return player;
        } else if (Game.getCurrentMode() == Game.Mode.FREE
                || Game.getCurrentMode() == Game.Mode.EDITOR) {
            return observer;
        }
        return null;
    }
    
    public Player getPlayer() {
        return player;
    }

    public List<NPC> getNpcList() {
        return npcList;
    }

}
