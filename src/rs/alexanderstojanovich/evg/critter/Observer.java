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

import java.util.List;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.level.LightSource;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Observer implements Critter {

    protected final Camera camera;
    protected boolean givenControl = false;
    protected Vector3f predictor = new Vector3f(Float.NaN, Float.NaN, Float.NaN);

    public Observer(Vector3f pos) {
        this.camera = new Camera(pos);
    }

    @Override
    public void moveForward(float amount) {
        if (givenControl) {
            camera.moveForward(amount);
        }
    }

    @Override
    public void moveBackward(float amount) {
        if (givenControl) {
            camera.moveBackward(amount);
        }
    }

    @Override
    public void moveLeft(float amount) {
        if (givenControl) {
            camera.moveLeft(amount);
        }
    }

    @Override
    public void moveRight(float amount) {
        if (givenControl) {
            camera.moveRight(amount);
        }
    }

    //--------------------------------------------------------------------------
    @Override
    public void movePredictorForward(float amount) {
        Vector3f temp1 = new Vector3f();
        predictor = camera.getPos().add(camera.getFront().mul(amount, temp1), temp1);
    }

    @Override
    public void movePredictorBackward(float amount) {
        Vector3f temp1 = new Vector3f();
        predictor = camera.getPos().sub(camera.getFront().mul(amount, temp1), temp1);
    }

    @Override
    public void movePredictorLeft(float amount) {
        Vector3f temp1 = new Vector3f();
        predictor = camera.getPos().sub(camera.getRight().mul(amount, temp1), temp1);
    }

    @Override
    public void movePredictorRight(float amount) {
        Vector3f temp1 = new Vector3f();
        predictor = camera.getPos().add(camera.getRight().mul(amount, temp1), temp1);
    }

    //--------------------------------------------------------------------------
    @Override
    public void turnLeft(float angle) {
        if (givenControl) {
            camera.turnLeft(angle);
        }
    }

    @Override
    public void turnRight(float angle) {
        if (givenControl) {
            camera.turnRight(angle);
        }
    }

    @Override
    public void lookAtAngle(float yaw, float pitch) {
        if (givenControl) {
            camera.lookAt(yaw, pitch);
        }
    }

    @Override
    public void lookAtOffset(float mouseSensitivity, float xoffset, float yoffset) {
        if (givenControl) {
            camera.lookAt(mouseSensitivity, xoffset, yoffset);
        }
    }

    @Override
    public boolean isBuffered() {
        return true;
    }

    @Override
    public void bufferAll() {

    }

    @Override
    public void render(List<LightSource> lightSrc, ShaderProgram shaderProgram) {
        if (givenControl) {
            camera.render(shaderProgram);
        }
    }

    @Override
    public String toString() {
        return "Observer{" + "camera=" + camera + ", givenControl=" + givenControl + ", predictor=" + predictor + '}';
    }

    @Override
    public boolean isGivenControl() {
        return givenControl;
    }

    @Override
    public void setGivenControl(boolean givenControl) {
        this.givenControl = givenControl;
    }

    @Override
    public Vector3f getPredictor() {
        return predictor;
    }

    public Camera getCamera() {
        return camera;
    }

}
