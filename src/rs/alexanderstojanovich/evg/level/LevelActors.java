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

import java.util.Arrays;
import java.util.List;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.critter.NPC;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.critter.Player;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.net.PlayerInfo;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.ModelUtils;

/**
 * Define all the level observers & critters. Present in the level container.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LevelActors {

    /**
     * Spectator is separate camera from player instance
     */
    public final Observer spectator = new Camera(); // spectator is separate camera from player instance

    public static final Model PLAYER_BODY = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player.obj", "alex", true);
    /**
     * Main player (Single Player & Multiplayer)
     */
    public final Player player = new Player(new Model(PLAYER_BODY));
    /**
     * Non-playable characters. Handled by client (SinglePlayer) or server host
     * (MultiPlyer).
     */
    public final List<NPC> npcList = new GapList<>();

    /**
     * Other players (Multiplayer)
     */
    public final IList<Critter> otherPlayers = new GapList<>();

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
        // Other players is used in Multiplayer
        for (Critter otherPlayer : otherPlayers) {
            otherPlayer.render(lightSrc, npcShader);
        }
        // Npc list is for now empty
        for (NPC npc : npcList) {
            npc.render(lightSrc, npcShader);
        }
        if ((mainActor() == player)) {
            player.render(lightSrc, mainActorShader);
        } else if (mainActor() == spectator) {
            spectator.render(mainActorShader);
        }

    }

    public Observer mainActor() {
        if (Game.getCurrentMode() == Game.Mode.SINGLE_PLAYER
                || Game.getCurrentMode() == Game.Mode.MULTIPLAYER_HOST || Game.getCurrentMode() == Game.Mode.MULTIPLAYER_JOIN) {
            return player;
        } else if (Game.getCurrentMode() == Game.Mode.FREE
                || Game.getCurrentMode() == Game.Mode.EDITOR) {
            return spectator;
        }
        return null;
    }

    public void configureMainObserver(Vector3f pos) {
        mainActor().setPos(pos);
        mainActor().getCamera().setFront(Camera.Z_AXIS);
        mainActor().getCamera().setUp(Camera.Y_AXIS);
        mainActor().getCamera().setRight(Camera.X_AXIS);
    }

    public void configureMainObserver(Vector3f pos, Vector3f front, Vector3f up, Vector3f right) {
        mainActor().setPos(pos);
        mainActor().getCamera().setFront(front);
        mainActor().getCamera().setUp(up);
        mainActor().getCamera().setRight(right);
    }

    public Camera mainCamera() {
        Observer mainActor = mainActor();
        return mainActor.getCamera();
    }

    public Player getPlayer() {
        return player;
    }

    public List<NPC> getNpcList() {
        return npcList;
    }

    public void configOtherPlayers(PlayerInfo[] playerInfo) {
        Arrays.asList(playerInfo).forEach(pi -> {
            if (!pi.uniqueId.equals(player.uniqueId)) {
                Critter op = new Critter(pi.uniqueId, new Model(LevelActors.PLAYER_BODY));
                op.setName(pi.name);
                op.body.setPrimaryRGBAColor(pi.color);
                op.body.setTexName(pi.texModel);
                otherPlayers.add(op);
            }
        });
    }

}
