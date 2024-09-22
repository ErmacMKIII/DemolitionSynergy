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

import java.util.Collection;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.core.RPGCamera;
import rs.alexanderstojanovich.evg.light.LightSource;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.resources.Assets;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.GlobalColors;
import rs.alexanderstojanovich.evg.weapons.WeaponIfc;
import rs.alexanderstojanovich.evg.weapons.Weapons;

/**
 * Player class.
 *
 * Player is rendered from Third Person and First Person as two different modes
 * of same camera.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Player extends Critter implements Observer {

    protected boolean registered = false;

    public static enum CameraView {
        FIRST_PERSON, THIRD_PERSON
    }
    protected CameraView cameraView = CameraView.THIRD_PERSON;

    protected final RPGCamera camera;
    public final LightSource light;

    public final Configuration cfg = Configuration.getInstance();

    /**
     * Create new player for Single Player
     *
     * @param assets game assets
     * @param body player body
     */
    public Player(Assets assets, Model body) {
        super(assets, body);
        this.camera = new RPGCamera(this.body, new Vector3f(this.body.pos));
        this.light = new LightSource(this.body.pos, new Vector3f(GlobalColors.WHITE), LightSource.PLAYER_LIGHT_INTENSITY);
        this.name = cfg.getName();
        this.body.setTexName(cfg.getModel());
        this.body.setPrimaryRGBColor(cfg.getColor());
    }

    /**
     * Create new player for Single Player
     *
     * @param assets game assets
     * @param camera camera to view
     * @param light light from player
     * @param body player body
     */
    public Player(Assets assets, RPGCamera camera, LightSource light, Model body) {
        super(assets, body);
        this.camera = camera;
        this.light = light;
        this.name = cfg.getName();
        this.body.setTexName(cfg.getModel());
        this.body.setPrimaryRGBColor(cfg.getColor());
    }

    /**
     * Create new player for Multiplayer. Using client registration to server.
     * Client has submit registration to this server.
     *
     * @param assets game assets
     * @param camera camera to view
     * @param light light from player
     * @param uniqueId unique id to be assigned to player
     * @param body player body
     */
    public Player(Assets assets, RPGCamera camera, LightSource light, String uniqueId, Model body) {
        super(assets, uniqueId, body);
        this.camera = camera;
        this.light = light;
    }

    /**
     * Toggle Between 1st Person / Third Person
     */
    public void switchViewToggle() {
        if (cameraView == CameraView.FIRST_PERSON) {
            this.camera.setDistanceFromTarget(2.1f);
            this.cameraView = CameraView.THIRD_PERSON;
        } else if (cameraView == CameraView.THIRD_PERSON) {
            this.camera.setDistanceFromTarget(0f);
            this.cameraView = CameraView.FIRST_PERSON;
        }
    }

    /**
     * Render player's weapon in hands.
     *
     * @param ls Light Source (from level container or MODEL_NONE)
     * @param sp Shader Program (default is Weapon GLSL Shader)
     */
    public void renderWeaponInHand(LightSources ls, ShaderProgram sp) {
        if (cameraView == CameraView.FIRST_PERSON) {
            if (!weapon.inHands().isBuffered()) {
                weapon.inHands().bufferAll();
            }
            // render gun on the screen
            weapon.inHands().render(ls, sp);
        }
    }

    @Override
    public void render(ShaderProgram shaderProgram) {
        camera.render(shaderProgram);
    }
    
    /**
     * Switch to weapon in hands
     *
     * @param weapons all weapons instance (wraps array)
     * @param index index of (weapon) enumeration
     */
    @Override
    public void switchWeapon(Weapons weapons, int index) {
        super.switchWeapon(weapons, index);
        // set the camera target model assuming it was changed
        this.camera.setTarget(body);
    }

    /**
     * Switch to weapon in hands
     *
     * @param weapon weapon to switch to
     */
    @Override
    public void switchWeapon(WeaponIfc weapon) {
        super.switchWeapon(weapon);
        // set the camera target model assuming it was changed
        this.camera.setTarget(body);
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
    public void render(Collection<ShaderProgram> shaderPrograms) {
        camera.render(shaderPrograms);
    }

    @Override
    public void renderContour(LightSources lightSources, ShaderProgram shaderProgram) {
        camera.render(shaderProgram);
        if (cameraView == CameraView.THIRD_PERSON) {
            super.renderContour(lightSources, shaderProgram);
        }
    }

    @Override
    public void render(LightSources lightSrc, ShaderProgram shaderProgram) {
        camera.render(shaderProgram);
        if (cameraView == CameraView.THIRD_PERSON) {
            super.render(lightSrc, shaderProgram);
        }
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
    public void dropY(float amount) {
        super.dropY(amount);
        camera.dropY(amount);
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

    public CameraView getCameraView() {
        return cameraView;
    }

    public void setCameraView(CameraView cameraView) {
        this.cameraView = cameraView;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public boolean isRegistered() {
        return registered;
    }

}
