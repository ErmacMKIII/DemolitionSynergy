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
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.critter.NPC;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.critter.Player;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.ModelUtils;

/**
 * Define all the level observers & critters. Present in the level container.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LevelActors {

    public Camera spectator = new Camera(); // spectator is separate camera from player instance

    public static final Model PLAYER_BODY = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player.obj", "alex", true);

    public final Player player = new Player(PLAYER_BODY);

    protected final List<NPC> npcList = new GapList<>();

    public void freeze() {
//        getMainActor().setGivenControl(false);
//        for (NPC npc : npcList) {
//            npc.setGivenControl(false);
//        }
    }

    public void unfreeze() {
//        getMainActor().setGivenControl(true);
//        for (NPC npc : npcList) {
//            npc.setGivenControl(true);
//        }
    }

    public void render(LightSources lightSrc, ShaderProgram mainActorShader, ShaderProgram npcShader) {
        for (NPC npc : npcList) {
            npc.render(lightSrc, npcShader);
        }
        mainObserver().render(ShaderProgram.SHADER_PROGRAMS);

        if (Game.getCurrentMode() == Game.Mode.SINGLE_PLAYER
                || Game.getCurrentMode() == Game.Mode.MULTIPLAYER) {
            player.render(lightSrc, mainActorShader);
        }
    }

    public Observer mainObserver() {
        if (Game.getCurrentMode() == Game.Mode.SINGLE_PLAYER
                || Game.getCurrentMode() == Game.Mode.MULTIPLAYER) {
            return player;
        } else if (Game.getCurrentMode() == Game.Mode.FREE
                || Game.getCurrentMode() == Game.Mode.EDITOR) {
            return spectator;
        }
        return null;
    }

    public void configureMainObserver(Vector3f pos) {
        mainObserver().setPos(pos);
        mainObserver().getCamera().setFront(Camera.Z_AXIS);
        mainObserver().getCamera().setUp(Camera.Y_AXIS);
        mainObserver().getCamera().setRight(Camera.X_AXIS);
    }

    public void configureMainObserver(Vector3f pos, Vector3f front, Vector3f up, Vector3f right) {
        mainObserver().setPos(pos);
        mainObserver().getCamera().setFront(front);
        mainObserver().getCamera().setUp(up);
        mainObserver().getCamera().setRight(right);
    }

    public Camera mainCamera() {
        Observer mainActor = mainObserver();
        return mainActor.getCamera();
    }

    public Player getPlayer() {
        return player;
    }

    public List<NPC> getNpcList() {
        return npcList;
    }

}
