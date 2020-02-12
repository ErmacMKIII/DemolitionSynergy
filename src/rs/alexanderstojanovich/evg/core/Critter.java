/*
 * Copyright (C) 2019 Coa
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
package rs.alexanderstojanovich.evg.core;

import rs.alexanderstojanovich.evg.texture.Texture;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Coa
 */
public class Critter {

    private final Camera camera;
    private final Model model;
    private boolean givenControl = true;
    private Vector3f predictor = new Vector3f(Float.NaN, Float.NaN, Float.NaN);
    private final Model predModel = new Model(false, "icosphere.obj");

    public Critter(String modelFileName, Texture texture, Vector3f pos, Vector4f color, float scale) {
        this.camera = new Camera(pos);
        this.model = new Model(false, modelFileName, texture);
        this.model.setPrimaryColor(color);
        this.model.setScale(scale);
        this.model.setLight(camera.getPos());
        updateModelPos();
    }

    public Critter(Camera camera, Model model) {
        this.camera = camera;
        this.model = model;
        updateModelPos();
    }

    private void updateModelPos() {
        model.getPos().x = camera.getPos().x;
        model.getPos().y = camera.getPos().y;
        model.getPos().z = camera.getPos().z;
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f temp3 = new Vector3f();
        model.setPos(model.getPos().sub(camera.getFront().mul(model.getDepth() / 2.0f, temp1), temp1));
        model.setPos(model.getPos().sub(camera.getUp().mul(model.getHeight() / 2.0f, temp2), temp2));
        model.setPos(model.getPos().sub(camera.getRight().mul(model.getWidth() / 2.0f, temp3), temp3));
        predictor = new Vector3f(camera.getPos().x, camera.getPos().y, camera.getPos().z);
        predModel.setScale(model.getScale());
    }

    public void moveForward(float amount) {
        if (givenControl) {
            camera.moveForward(amount);
            updateModelPos();
        }
    }

    public void moveBackward(float amount) {
        if (givenControl) {
            camera.moveBackward(amount);
            updateModelPos();
        }
    }

    public void moveLeft(float amount) {
        if (givenControl) {
            camera.moveLeft(amount);
            updateModelPos();
        }
    }

    public void moveRight(float amount) {
        if (givenControl) {
            camera.moveRight(amount);
            updateModelPos();
        }
    }

    //--------------------------------------------------------------------------
    public void movePredictorForward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = camera.getPos().add(camera.getFront().mul(amount, temp1), temp1);
        predModel.setPos(model.getPos().add(camera.getFront().mul(amount, temp2), temp2));
    }

    public void movePredictorBackward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = camera.getPos().sub(camera.getFront().mul(amount, temp1), temp1);
        predModel.setPos(model.getPos().sub(camera.getFront().mul(amount, temp2), temp2));
    }

    public void movePredictorLeft(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = camera.getPos().sub(camera.getRight().mul(amount, temp1), temp1);
        predModel.setPos(model.getPos().sub(camera.getRight().mul(amount, temp2), temp2));
    }

    public void movePredictorRight(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = camera.getPos().add(camera.getRight().mul(amount, temp1), temp1);
        predModel.setPos(model.getPos().add(camera.getRight().mul(amount, temp2), temp2));
    }

    //--------------------------------------------------------------------------
    public void turnLeft(float angle) {
        if (givenControl) {
            camera.turnLeft(angle);
            model.setrX(-angle);
        }
    }

    public void turnRight(float angle) {
        if (givenControl) {
            camera.turnRight(angle);
            model.setrX(angle);
        }
    }

    public void lookAt(float mouseSensitivity, float xoffset, float yoffset) {
        if (givenControl) {
            camera.lookAt(mouseSensitivity, xoffset, yoffset);
            model.setrX(-camera.getPitch());
            model.setrY(camera.getYaw());
        }
    }

    public void render(ShaderProgram shaderProgram) {
        if (givenControl) {
            camera.render(shaderProgram);
        }
//        model.render();
    }

    @Override
    public String toString() {
        return "Critter{" + "camera=" + camera + ", model=" + model + '}';
    }

    public Camera getCamera() {
        return camera;
    }

    public Model getModel() {
        return model;
    }

    public boolean isGivenControl() {
        return givenControl;
    }

    public void setGivenControl(boolean givenControl) {
        this.givenControl = givenControl;
    }

    public Vector3f getPredictor() {
        return predictor;
    }

    public Model getPredModel() {
        return predModel;
    }

}
