/*
 * Copyright (C) 2022 Alexander Stojanovich <coas91@rocketmail.com>
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
import rs.alexanderstojanovich.evg.level.LightSources;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class ModelCritter implements Critter {

    protected Vector3f predictor = new Vector3f(Float.NaN, Float.NaN, Float.NaN);

    protected Vector3f front = Camera.Z_AXIS;
    protected Vector3f up = Camera.Y_AXIS;
    protected Vector3f right = Camera.Y_AXIS;

    protected float yaw = (float) (-Math.PI / 2.0); // sideways look angle
    protected float pitch = (float) (-Math.PI); // up and down look angle

    protected boolean givenControl = false;

    protected final Model model;

    public ModelCritter(Model model) {
        this.model = model;
        this.predictor = new Vector3f(model.pos);
    }

    private void updateDirectionVectors() {
        Vector3f temp1 = new Vector3f();
        front = front.normalize(temp1);
        Vector3f temp2 = new Vector3f();
        right = Camera.Y_AXIS.cross(front, temp2).normalize(temp2);
        Vector3f temp3 = new Vector3f();
        up = front.cross(right, temp3).normalize(temp3);
    }

    @Override
    public void moveForward(float amount) {
        if (givenControl) {
            Vector3f temp1 = new Vector3f();
            Vector3f temp2 = new Vector3f();
            model.pos = model.pos.add(front.mul(amount, temp1), temp2);
        }
    }

    @Override
    public void moveBackward(float amount) {
        if (givenControl) {
            Vector3f temp1 = new Vector3f();
            Vector3f temp2 = new Vector3f();
            model.pos = model.pos.sub(front.mul(amount, temp1), temp2);
        }
    }

    @Override
    public void moveLeft(float amount) {
        if (givenControl) {
            Vector3f temp1 = new Vector3f();
            Vector3f temp2 = new Vector3f();
            model.pos = model.pos.sub(right.mul(amount, temp1), temp2);
        }
    }

    @Override
    public void moveRight(float amount) {
        if (givenControl) {
            Vector3f temp1 = new Vector3f();
            Vector3f temp2 = new Vector3f();
            model.pos = model.pos.add(right.mul(amount, temp1), temp2);
        }
    }

    @Override
    public void movePredictorForward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = model.pos.add(front.mul(amount, temp1), temp2);
    }

    @Override
    public void movePredictorBackward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = model.pos.sub(front.mul(amount, temp1), temp2);
    }

    @Override
    public void movePredictorLeft(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = model.pos.sub(right.mul(amount, temp1), temp2);
    }

    @Override
    public void movePredictorRight(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = model.pos.add(right.mul(amount, temp1), temp2);
    }

    @Override
    public void turnLeft(float angle) {
        if (givenControl) {
            model.setrX(-angle);
        }
    }

    @Override
    public void turnRight(float angle) {
        if (givenControl) {
            model.setrX(-angle);
        }
    }

    @Override
    public void lookAtOffset(float sensitivity, float xoffset, float yoffset) {
        if (givenControl) {
            yaw += sensitivity * xoffset;
            while (yaw >= 2.0 * Math.PI) {
                yaw -= 2.0 * Math.PI;
            }
            pitch += sensitivity * yoffset;
            if (pitch > Math.PI / 2.1) {
                pitch = (float) (Math.PI / 2.1);
            }
            if (pitch < -Math.PI / 2.1) {
                pitch = (float) (-Math.PI / 2.1);
            }

            front.x = (float) (Math.cos(yaw) * Math.cos(pitch));
            front.y = (float) Math.sin(pitch);
            front.z = (float) (-Math.sin(yaw) * Math.cos(pitch));

            updateDirectionVectors();
        }
    }

    @Override
    public void lookAtAngle(float yaw, float pitch) {
        if (givenControl) {
            this.yaw = yaw;
            this.pitch = pitch;
            front.x = (float) (Math.cos(this.yaw) * Math.cos(this.pitch));
            front.y = (float) Math.sin(this.pitch);
            front.z = (float) (-Math.sin(this.yaw) * Math.cos(this.pitch));

            updateDirectionVectors();
        }
    }

    @Override
    public void render(LightSources lightSrc, ShaderProgram shaderProgram) {
        if (!model.isBuffered()) {
            model.bufferAll();
        }
        model.render(lightSrc, shaderProgram);
    }

    @Override
    public boolean isBuffered() {
        return model.isBuffered();
    }

    @Override
    public void bufferAll() {
        model.bufferAll();
    }

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

    public Vector3f getFront() {
        return front;
    }

    public Vector3f getUp() {
        return up;
    }

    public Vector3f getRight() {
        return right;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPredictor(Vector3f predictor) {
        this.predictor = predictor;
    }

    public void setFront(Vector3f front) {
        this.front = front;
    }

    public void setUp(Vector3f up) {
        this.up = up;
    }

    public void setRight(Vector3f right) {
        this.right = right;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setPosition(Vector3f position) {
        predictor = new Vector3f(position);
        model.setPos(new Vector3f(position));
    }

    @Override
    public Vector3f getPosition() {
        return model.pos;
    }

}
