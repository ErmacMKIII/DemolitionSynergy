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

import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Coa
 */
public class Critter {

    private Camera camera;
    private Model model;
    private boolean givenControl = true;

    public Critter(String modelFileName, String textureFileName, ShaderProgram shaderProgram, Vector3f pos, Vector4f color, float scale) {
        this.camera = new Camera(pos, shaderProgram);
        this.model = new Model(modelFileName, textureFileName, shaderProgram);
        this.model.setPrimaryColor(color);
        this.model.setScale(scale);
        this.model.setLight(camera.getPos());        
        initModelPos();
    }

    public Critter(Camera camera, Model model) {
        this.camera = camera;
        this.model = model;
    }

    private void initModelPos() {
        model.getPos().x = camera.getPos().x;
        model.getPos().y = camera.getPos().y;
        model.getPos().z = camera.getPos().z;
        model.setPos(model.getPos().sub(camera.getUp().mul(model.getHeight() / 2.0f)));
    }

    public void moveForward(float amount) {        
        if (givenControl) {         
            camera.moveForward(amount);
            model.setPos(model.getPos().add(camera.getFront().mul(amount)));
        }
    }

    public void moveBackward(float amount) {
        if (givenControl) {
            camera.moveBackward(amount);
            model.setPos(model.getPos().sub(camera.getFront().mul(amount)));
        }
    }

    public void moveLeft(float amount) {
        if (givenControl) {
            camera.moveLeft(amount);
            model.setPos(model.getPos().sub(camera.getRight().mul(amount)));
        }
    }

    public void moveRight(float amount) {
        if (givenControl) {
            camera.moveRight(amount);
            model.setPos(model.getPos().add(camera.getRight().mul(amount)));
        }
    }

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

    public void render() {
        if (givenControl) {
            camera.render();
        }
        /*else {
            model.render();
        }*/
    }

    @Override
    public String toString() {
        return "Critter{" + "camera=" + camera + ", model=" + model + '}';
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public boolean isGivenControl() {
        return givenControl;
    }

    public void setGivenControl(boolean givenControl) {
        this.givenControl = givenControl;
    }

}
