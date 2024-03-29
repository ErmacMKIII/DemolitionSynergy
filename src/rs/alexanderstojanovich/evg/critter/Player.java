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
import rs.alexanderstojanovich.evg.core.RPGCamera;
import rs.alexanderstojanovich.evg.light.LightSource;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.GlobalColors;
import rs.alexanderstojanovich.evg.util.ModelUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Player extends Critter implements Observer {

//    private Model currWeapon;
    private final Camera camera;
    public final LightSource light;

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
//            weapon.setPrimaryColor(GlobalColors.WHITE);
//            weapon.setScale(6.0f);
//            weapon.setrY((float) (-Math.PI / 2.0f));
//        }
//    }
    public Player(Model body) {
        super(body);
//        this.camera = new Camera(new Vector3f(this.body.pos));
        this.camera = new RPGCamera(this.body, new Vector3f(this.body.pos));
        this.light = new LightSource(this.body.pos, GlobalColors.WHITE, LightSource.PLAYER_LIGHT_INTENSITY);
    }

//    public void switchWeapon(int num) {
//        currWeapon = WEAPONS[num - 1];
//    }
    @Override
    public void render(ShaderProgram shaderProgram) {
        camera.render(shaderProgram);
    }

    @Override
    public void lookAtOffset(float sensitivity, float xoffset, float yoffset) {
        body.setrY(body.getrY() + sensitivity * xoffset);
        camera.lookAtOffset(sensitivity, xoffset, yoffset);
        updateCameraVectors(camera.getFront());
    }

    @Override
    public void lookAtAngle(float yaw, float pitch) {
        body.setrY((body.getrY() + yaw));
        camera.lookAtAngle(yaw, pitch);
        updateCameraVectors(camera.getFront());
    }

    @Override
    public void moveForward(float amount) {
        super.moveForward(amount);
        camera.moveForward(amount);
        light.pos = body.pos;
    }

    @Override
    public void moveBackward(float amount) {
        super.moveBackward(amount);
        camera.moveBackward(amount);
        light.pos = body.pos;
    }

    @Override
    public void moveLeft(float amount) {
        super.moveLeft(amount);
        camera.moveLeft(amount);
        light.pos = body.pos;
    }

    @Override
    public void moveRight(float amount) {
        super.moveRight(amount);
        camera.moveRight(amount);
        light.pos = body.pos;
    }

    @Override
    public void descend(float amount) {
        super.descend(amount);
        camera.descend(amount);
        light.pos = body.pos;
    }

    @Override
    public void ascend(float amount) {
        super.ascend(amount);
        camera.ascend(amount);
        light.pos = body.pos;
    }

    @Override
    public Camera getCamera() {
        return camera;
    }

    @Override
    public void setPos(Vector3f pos) {
        super.setPos(pos);
        camera.pos = pos;
    }

    public LightSource getLight() {
        return light;
    }

    @Override
    public void render(ShaderProgram[] shaderPrograms) {
        camera.render(shaderPrograms);
    }

    @Override
    public void renderContour(LightSources lightSources, ShaderProgram shaderProgram) {
        camera.render(shaderProgram);
        super.renderContour(lightSources, shaderProgram);
    }

    @Override
    public void render(LightSources lightSrc, ShaderProgram shaderProgram) {
        camera.render(shaderProgram);
        super.render(lightSrc, shaderProgram);
    }

    @Override
    public void turnRight(float angle) {
        super.turnRight(angle);
        camera.turnRight(angle);
    }

    @Override
    public void turnLeft(float angle) {
        super.turnLeft(angle);
        camera.turnLeft(angle);
    }

    @Override
    public Vector3f getFront() {
        return this.camera.getFront();
    }

    @Override
    public Vector3f getUp() {
        return this.camera.getUp();
    }

    @Override
    public Vector3f getRight() {
        return this.camera.getRight();
    }

    @Override
    public void sinkY(float amount) {
        super.sinkY(amount);
        camera.sinkY(amount);
        light.pos = body.pos;
    }

    @Override
    public void jumpY(float amount) {
        super.jumpY(amount);
        camera.jumpY(amount);
        light.pos = body.pos;
    }

    @Override
    public void moveXZRight(float amount) {
        super.moveXZRight(amount);
        camera.moveXZRight(amount);
        light.pos = body.pos;
    }

    @Override
    public void moveXZLeft(float amount) {
        super.moveXZLeft(amount);
        camera.moveXZLeft(amount);
        light.pos = body.pos;
    }

    @Override
    public void moveXZBackward(float amount) {
        super.moveXZBackward(amount);
        camera.moveXZBackward(amount);
        light.pos = body.pos;
    }

    @Override
    public void moveXZForward(float amount) {
        super.moveXZForward(amount);
        camera.moveXZForward(amount);
        light.pos = body.pos;
    }

}
