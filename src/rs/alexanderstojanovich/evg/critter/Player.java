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
package rs.alexanderstojanovich.evg.critter;

import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.light.LightSource;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.ModelUtils;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Player extends Critter {

//    private Model currWeapon;
    private final Camera camera = new Camera(new Vector3f(body.pos.x, body.pos.y + body.getHeight() / 2.0f, body.pos.z), front, up, right) {
        @Override
        public void render(LightSources lightSrc, ShaderProgram shaderProgram) {
            super.render(shaderProgram);
            body.render(lightSrc, shaderProgram);
        }
    };
    public final LightSource light = new LightSource(getCamera().getPos(), Vector3fColors.WHITE, LightSource.PLAYER_LIGHT_INTENSITY);

    public static final Vector3f WEAPON_POS = new Vector3f(1.0f, -1.0f, 3.0f);

    public static final Model PISTOL = ModelUtils.readFromObjFile(Game.PLAYER_ENTRY, "W01M9.obj", "W01M9", true);
    public static final Model SUB_MACHINE_GUN = ModelUtils.readFromObjFile(Game.PLAYER_ENTRY, "W06P9.obj", "W06P9", true);
    public static final Model SHOTGUN = ModelUtils.readFromObjFile(Game.PLAYER_ENTRY, "W13B9.obj", "W13B9", true);
    public static final Model ASSAULT_RIFLE = ModelUtils.readFromObjFile(Game.PLAYER_ENTRY, "W07AK.obj", "W07AK", true);
    public static final Model MACHINE_GUN = ModelUtils.readFromObjFile(Game.PLAYER_ENTRY, "W10M6.obj", "W10M6", true);
    public static final Model SNIPER_RIFLE = ModelUtils.readFromObjFile(Game.PLAYER_ENTRY, "W16M8.obj", "W16M8", true);
    public static final Model[] WEAPONS = {PISTOL, SUB_MACHINE_GUN, SHOTGUN, ASSAULT_RIFLE, MACHINE_GUN, SNIPER_RIFLE};

//    static {
//        for (Model weapon : WEAPONS) {
//            weapon.pos = WEAPON_POS;
//            weapon.setPrimaryColor(Vector3fColors.WHITE);
//            weapon.setScale(6.0f);
//            weapon.setrY((float) (-Math.PI / 2.0f));
//        }
//    }
    public Player(Model body) {
        super(body);
    }

//    public void switchWeapon(int num) {
//        currWeapon = WEAPONS[num - 1];
//    }
    @Override
    public void render(LightSources lightSrc, ShaderProgram shaderProgram) {
        camera.render(lightSrc, shaderProgram);
    }

    @Override
    public void lookAtOffset(float sensitivity, float xoffset, float yoffset) {
        camera.lookAtOffset(sensitivity, xoffset, yoffset);
    }

    @Override
    public void lookAtAngle(float yaw, float pitch) {
        camera.lookAtAngle(yaw, pitch);
    }

    @Override
    public void moveForward(float amount) {
        light.pos = predictor = body.pos.add(front.mul(amount));
    }

    @Override
    public void moveBackward(float amount) {
        light.pos = predictor = body.pos.sub(front.mul(amount));
    }

    @Override
    public void moveLeft(float amount) {
        light.pos = predictor = body.pos.sub(right.mul(amount));
    }

    @Override
    public void moveRight(float amount) {
        light.pos = predictor = body.pos.add(right.mul(amount));
    }

    @Override
    public Camera getCamera() {
        return camera;
    }

    public LightSource getLight() {
        return light;
    }

}
