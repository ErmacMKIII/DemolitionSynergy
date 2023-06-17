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
import static rs.alexanderstojanovich.evg.core.Camera.X_AXIS;
import static rs.alexanderstojanovich.evg.core.Camera.Y_AXIS;
import static rs.alexanderstojanovich.evg.core.Camera.Z_AXIS;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 * Critter is interface of living things. Has generic observation. Is collision
 * predicatable.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public abstract class Critter implements Predictable, Observer {

    public final Model body;
    protected Vector3f predictor = new Vector3f(Float.NaN, Float.NaN, Float.NaN);

    // three vectors determining exact camera position aka camera vectors
    protected Vector3f front = Z_AXIS;
    protected Vector3f up = Y_AXIS;
    protected Vector3f right = X_AXIS;

    public Critter(Model body) {
        this.body = body;
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
    public Vector3f getPredictor() {
        return predictor;
    }

    @Override
    public void moveForward(float amount) {
        predictor = body.pos.add(front.mul(amount));
    }

    @Override
    public void moveBackward(float amount) {
        predictor = body.pos.sub(front.mul(amount));
    }

    @Override
    public void moveLeft(float amount) {
        predictor = body.pos.sub(right.mul(amount));
    }

    @Override
    public void moveRight(float amount) {
        predictor = body.pos.add(right.mul(amount));
    }

    @Override
    public void turnLeft(float angle) {
        body.setrX(-angle);
    }

    @Override
    public void turnRight(float angle) {
        body.setrX(-angle);
    }

    @Override
    public void render(LightSources lightSrc, ShaderProgram shaderProgram) {
        body.render(lightSrc, shaderProgram);
    }

    @Override
    public Camera getCamera() {
        return null;
    }

    @Override
    public Vector3f getPos() {
        return body.pos;
    }

    public void setPos(Vector3f pos) {
        predictor = body.pos = new Vector3f(pos);
    }
}
