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
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Observer implements Critter {

    protected final Camera camera;
    protected final Model model;
    protected boolean givenControl = true;
    protected Vector3f predictor = new Vector3f(Float.NaN, Float.NaN, Float.NaN);

    public Observer(String modelFileName, String texName, Vector3f pos, Vector3f color, float scale) {
        this.camera = new Camera(pos);
        this.model = Model.readFromObjFile(Game.WORLD_ENTRY, modelFileName, texName);
        this.model.setPrimaryColor(color);
        this.model.setScale(scale);
        this.model.setLight(camera.getPos());
        initModelPos();
    }

    public Observer(Camera camera, Model model) {
        this.camera = camera;
        this.model = model;
        this.model.setLight(camera.getPos());
        initModelPos();
    }

    private void initModelPos() {
        model.setPos(new Vector3f(camera.getPos()));
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f temp3 = new Vector3f();
        model.setPos(model.getPos().sub(camera.getFront().mul(model.getDepth() / 2.0f, temp1), temp1));
        model.setPos(model.getPos().sub(camera.getUp().mul(model.getHeight() / 2.0f, temp2), temp2));
        model.setPos(model.getPos().sub(camera.getRight().mul(model.getWidth() / 2.0f, temp3), temp3));
        predictor = new Vector3f(camera.getPos().x, camera.getPos().y, camera.getPos().z);
    }

    public void updateModelPos() {
        model.setPos(new Vector3f(camera.getPos()));
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f temp3 = new Vector3f();
        model.setPos(model.getPos().sub(camera.getFront().mul(model.getDepth() / 2.0f, temp1), temp1));
        model.setPos(model.getPos().sub(camera.getUp().mul(model.getHeight() / 2.0f, temp2), temp2));
        model.setPos(model.getPos().sub(camera.getRight().mul(model.getWidth() / 2.0f, temp3), temp3));
        predictor = new Vector3f(camera.getPos().x, camera.getPos().y, camera.getPos().z);
    }

    @Override
    public void moveForward(float amount) {
        if (givenControl) {
            camera.moveForward(amount);
            Vector3f temp = new Vector3f();
            model.getPos().add(camera.getFront().mul(amount, temp));
        }
    }

    @Override
    public void moveBackward(float amount) {
        if (givenControl) {
            camera.moveBackward(amount);
            Vector3f temp = new Vector3f();
            model.getPos().sub(camera.getFront().mul(amount, temp));
        }
    }

    @Override
    public void moveLeft(float amount) {
        if (givenControl) {
            camera.moveLeft(amount);
            Vector3f temp = new Vector3f();
            model.getPos().sub(camera.getRight().mul(amount, temp));
        }
    }

    @Override
    public void moveRight(float amount) {
        if (givenControl) {
            camera.moveRight(amount);
            Vector3f temp = new Vector3f();
            model.getPos().add(camera.getRight().mul(amount, temp));
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
            model.setrX(-angle);
        }
    }

    @Override
    public void turnRight(float angle) {
        if (givenControl) {
            camera.turnRight(angle);
            model.setrX(angle);
        }
    }

    @Override
    public void lookAt(float mouseSensitivity, float xoffset, float yoffset) {
        if (givenControl) {
            camera.lookAt(mouseSensitivity, xoffset, yoffset);
            model.setrX(-camera.getPitch());
            model.setrY(camera.getYaw());
        }
    }

    @Override
    public boolean isBuffered() {
        return model.isBuffered();
    }

    @Override
    public void bufferAll() {
        model.bufferAll();
    }

    @Override
    public void render() {
        if (givenControl) {
            camera.render(ShaderProgram.getMainShader());
        }
//        model.render(ShaderProgram.getMainShader());
    }

    @Override
    public String toString() {
        return "Observer{" + "camera=" + camera + ", model=" + model + ", givenControl=" + givenControl + ", predictor=" + predictor + '}';
    }

    @Override
    public Camera getCamera() {
        return camera;
    }

    @Override
    public Model getModel() {
        return model;
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

}
