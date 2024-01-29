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
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.models.Renderable;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 * Critter is class of living things. Has capabilities moving. Is collision
 * predictable. However no observation. Renders with body in some shader.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Critter implements Predictable, Moveable, Renderable {

    public final Model body;
    protected Vector3f predictor;
    protected Vector3f front = Camera.Z_AXIS;
    protected Vector3f up = Camera.Y_AXIS;
    protected Vector3f right = Camera.X_AXIS;

    /**
     * Create new instance of the critter. If instanced in anonymous class
     * specify the camera
     *
     * @param body body model
     */
    public Critter(Model body) {
        this.body = body;
        this.predictor = new Vector3f(body.pos); // separate predictor from the body
    }

    /**
     * Create new instance of the critter. If instanced in anonymous class
     * specify the camera
     *
     * @param pos initial position of the critter
     * @param body body model
     */
    public Critter(Vector3f pos, Model body) {
        this.body = body;
        this.predictor = new Vector3f(body.pos); // separate predictor from the body
    }

    @Override
    public void movePredictorForward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = body.pos.add(front.mul(amount, temp1), temp2);
    }

    @Override
    public void movePredictorBackward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = body.pos.sub(front.mul(amount, temp1), temp2);
    }

    @Override
    public void movePredictorLeft(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = body.pos.sub(right.mul(amount, temp1), temp2);
    }

    @Override
    public void movePredictorRight(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = body.pos.add(right.mul(amount, temp1), temp2);
    }

    @Override
    public void movePredictorUp(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = body.pos.add(up.mul(amount, temp1), temp2);
    }

    @Override
    public void movePredictorDown(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        predictor = body.pos.sub(up.mul(amount, temp1), temp2);
    }

    @Override
    public Vector3f getPredictor() {
        return predictor;
    }

    @Override
    public void moveForward(float amount) {
        Vector3f temp = new Vector3f();
        body.pos = body.pos.add(front.mul(amount, temp), temp);
    }

    @Override
    public void moveBackward(float amount) {
        Vector3f temp = new Vector3f();
        body.pos = body.pos.sub(front.mul(amount, temp), temp);
    }

    @Override
    public void moveLeft(float amount) {
        Vector3f temp = new Vector3f();
        body.pos = body.pos.sub(right.mul(amount, temp), temp);
    }

    @Override
    public void moveRight(float amount) {
        Vector3f temp = new Vector3f();
        body.pos = body.pos.add(right.mul(amount, temp), temp);
    }

    @Override
    public void ascend(float amount) {
        Vector3f temp = new Vector3f();
        body.pos = body.pos.add(up.mul(amount, temp), temp);
    }

    @Override
    public void descend(float amount) {
        Vector3f temp = new Vector3f();
        body.pos = body.pos.sub(up.mul(amount, temp), temp);
    }

    protected void updateCameraVectors() {
        Vector3f temp1 = new Vector3f();
        front = front.normalize(temp1);
        Vector3f temp2 = new Vector3f();
        right = Camera.Y_AXIS.cross(front, temp2).normalize(temp2);
        Vector3f temp3 = new Vector3f();
        up = front.cross(right, temp3).normalize(temp3);
    }

    protected void updateCameraVectors(Vector3f front) {
        this.front = front;
        Vector3f temp1 = new Vector3f();
        front = front.normalize(temp1);
        Vector3f temp2 = new Vector3f();
        right = Camera.Y_AXIS.cross(front, temp2).normalize(temp2);
        Vector3f temp3 = new Vector3f();
        up = front.cross(right, temp3).normalize(temp3);
    }

    @Override
    public void turnLeft(float angle) {
        body.setrY(body.getrY() - angle);
        Vector3f temp = new Vector3f();
        front = front.rotateY(-angle, temp);
        updateCameraVectors();
    }

    @Override
    public void turnRight(float angle) {
        body.setrY(body.getrY() + angle);
        Vector3f temp = new Vector3f();
        front = front.rotateY(angle, temp);
        updateCameraVectors();
    }

    @Override
    public void render(LightSources lightSrc, ShaderProgram shaderProgram) {
        if (!body.isBuffered()) {
            body.bufferAll();
        }
        body.render(lightSrc, shaderProgram);
    }

    @Override
    public void renderContour(LightSources lightSources, ShaderProgram shaderProgram) {
        if (!body.isBuffered()) {
            body.bufferAll();
        }
        body.renderContour(lightSources, shaderProgram);
    }

    @Override
    public Vector3f getPos() {
        return body.pos;
    }

    @Override
    public void setPos(Vector3f pos) {
        body.pos = pos;
        predictor = new Vector3f(pos);
    }

    @Override
    public void setPredictor(Vector3f predictor) {
        this.predictor = predictor;
    }

    public Model getBody() {
        return body;
    }

}
