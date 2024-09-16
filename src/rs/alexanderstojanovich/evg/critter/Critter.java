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

import java.util.UUID;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.models.Renderable;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.HardwareUtils;
import rs.alexanderstojanovich.evg.weapons.WeaponIfc;
import rs.alexanderstojanovich.evg.weapons.Weapons;

/**
 * Critter is class of living things. Has capabilities moving. Is collision
 * predictable. However no observation. Renders with body in some shader.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Critter implements Predictable, Moveable, Renderable {

    protected String name;
    public final String uniqueId;
    public final Model body;
    protected Vector3f predictor;
    protected Vector3f front = Camera.Z_AXIS;
    protected Vector3f up = Camera.Y_AXIS;
    protected Vector3f right = Camera.X_AXIS;
    protected boolean underGravity = false;
    protected boolean inJump = false;

    /**
     * Weapon model on character body with the weapon
     */
    protected Model charBodyWeaponModel = Model.MODEL_NONE;

    /**
     * Critter (could be Player) has nothing in hands (no-weapon)
     */
    protected WeaponIfc weapon = Weapons.NONE;

    /**
     * Create new instance of the critter. If instanced in anonymous class
     * specify the camera
     *
     * @param body body model
     */
    public Critter(Model body) {
        this.body = body;
        this.predictor = new Vector3f(body.pos); // separate predictor from the body
        this.uniqueId = HardwareUtils.generateHardwareUUID();
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
        this.uniqueId = HardwareUtils.generateHardwareUUID();
    }

    /**
     * Create new instance of the critter. If instanced in anonymous class
     * specify the camera
     *
     * @param uniqueId to be assigned to the critter
     * @param body model body for the critter
     */
    public Critter(String uniqueId, Model body) {
        this.uniqueId = uniqueId;
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
        Vector3f frontAmpl = front.mul(amount, temp);
        body.pos = body.pos.add(frontAmpl, temp);
//        charBodyWeaponModel.pos = charBodyWeaponModel.pos.add(frontAmpl, temp);
    }

    @Override
    public void moveBackward(float amount) {
        Vector3f temp = new Vector3f();
        Vector3f frontAmpl = front.mul(amount, temp);
        body.pos = body.pos.sub(frontAmpl, temp);
//        charBodyWeaponModel.pos = charBodyWeaponModel.pos.sub(frontAmpl, temp);
    }

    @Override
    public void moveLeft(float amount) {
        Vector3f temp = new Vector3f();
        Vector3f rightAmpl = right.mul(amount, temp);
        body.pos = body.pos.sub(rightAmpl, temp);
//        charBodyWeaponModel.pos = charBodyWeaponModel.pos.sub(rightAmpl, temp);
    }

    @Override
    public void moveRight(float amount) {
        Vector3f temp = new Vector3f();
        Vector3f rightAmpl = right.mul(amount, temp);
        body.pos = body.pos.add(rightAmpl, temp);
//        charBodyWeaponModel.pos = charBodyWeaponModel.pos.add(rightAmpl, temp);
    }

    @Override
    public void ascend(float amount) {
        Vector3f temp = new Vector3f();
        Vector3f upAmpl = up.mul(amount, temp);
        body.pos = body.pos.add(upAmpl, temp);
//        charBodyWeaponModel.pos = charBodyWeaponModel.pos.add(upAmpl, temp);
    }

    @Override
    public void descend(float amount) {
        Vector3f temp = new Vector3f();
        Vector3f upAmpl = up.mul(amount, temp);
        body.pos = body.pos.sub(upAmpl, temp);
//        charBodyWeaponModel.pos = charBodyWeaponModel.pos.sub(upAmpl, temp);
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
        // weapon which critte
        if (weapon != Weapons.NONE) {
            if (!charBodyWeaponModel.isBuffered()) {
                charBodyWeaponModel.bufferAll();
            }
            charBodyWeaponModel.render(lightSrc, shaderProgram);
        }
    }

    @Override
    public void renderContour(LightSources lightSources, ShaderProgram shaderProgram) {
        if (!body.isBuffered()) {
            body.bufferAll();
        }
        body.renderContour(lightSources, shaderProgram);
        if (weapon != Weapons.NONE) {
            if (!charBodyWeaponModel.isBuffered()) {
                charBodyWeaponModel.bufferAll();
            }
            charBodyWeaponModel.renderContour(lightSources, shaderProgram);
        }
    }

    /**
     * Switch to weapon in hands
     *
     * @param weapons all weapons instance (wraps array)
     * @param index index of (weapon) enumeration
     */
    public void switchWeapon(Weapons weapons, int index) {
        this.weapon = weapons.AllWeapons[index];
        this.charBodyWeaponModel = this.weapon.deriveBodyModel(this);
    }

    /**
     * Switch to weapon in hands
     *
     * @param weapon weapon to switch to
     */
    public void switchWeapon(WeaponIfc weapon) {
//        this.weapon = weapon;
//        this.charBodyWeaponModel = this.weapon.deriveBodyModel(this);
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

    @Override
    public void moveXZForward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f frontXZ = new Vector3f(front.x, 0.0f, front.z);
        float scale = front.length() / frontXZ.length();
        frontXZ = frontXZ.mul(amount * scale, temp1);
        body.pos = body.pos.add(frontXZ, temp2);
//        charBodyWeaponModel.pos = charBodyWeaponModel.pos.add(frontXZ, temp);
    }

    @Override
    public void moveXZBackward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f frontXZ = new Vector3f(front.x, 0.0f, front.z);
        float scale = front.length() / frontXZ.length();
        frontXZ = frontXZ.mul(amount * scale, temp1);
        body.pos = body.pos.sub(frontXZ, temp2);
//        charBodyWeaponModel.pos = charBodyWeaponModel.pos.sub(frontXZ, temp);
    }

    @Override
    public void moveXZLeft(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f rightXZ = new Vector3f(right.x, 0.0f, right.z);
        float scale = right.length() / rightXZ.length();
        rightXZ = rightXZ.mul(amount * scale, temp1);
        body.pos = body.pos.sub(rightXZ, temp2);
//        charBodyWeaponModel.pos = charBodyWeaponModel.pos.sub(rightXZ, temp);
    }

    @Override
    public void moveXZRight(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f rightXZ = new Vector3f(right.x, 0.0f, right.z);
        float scale = right.length() / rightXZ.length();
        rightXZ = rightXZ.mul(amount * scale, temp1);
        body.pos = body.pos.add(rightXZ, temp2);
//        charBodyWeaponModel.pos = charBodyWeaponModel.pos.add(rightXZ, temp);
    }

    @Override
    public void jumpY(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f upAmount = Camera.Y_AXIS.mul(amount, temp1);
        body.pos = body.pos.add(upAmount, temp2);
//        charBodyWeaponModel.pos = charBodyWeaponModel.pos.add(upAmount, temp);
    }

    @Override
    public void dropY(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f upAmount = Camera.Y_AXIS.mul(amount, temp1);
        body.pos = body.pos.sub(upAmount, temp2);
//        charBodyWeaponModel.pos = charBodyWeaponModel.pos.sub(upAmount, temp);
    }

    @Override
    public void movePredictorXZForward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f frontXZ = new Vector3f(front.x, 0.0f, front.z);
        float scale = front.length() / frontXZ.length();
        frontXZ = frontXZ.mul(amount * scale, temp1);
        predictor = body.pos.add(frontXZ, temp2);
    }

    @Override
    public void movePredictorXZBackward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f frontXZ = new Vector3f(front.x, 0.0f, front.z);
        float scale = front.length() / frontXZ.length();
        frontXZ = frontXZ.mul(amount * scale, temp1);
        predictor = body.pos.sub(frontXZ, temp2);
    }

    @Override
    public void movePredictorXZLeft(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f rightXZ = new Vector3f(right.x, 0.0f, right.z);
        float scale = right.length() / rightXZ.length();
        rightXZ = rightXZ.mul(amount * scale, temp1);
        predictor = body.pos.sub(rightXZ, temp2);
    }

    @Override
    public void movePredictorXZRight(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f rightXZ = new Vector3f(right.x, 0.0f, right.z);
        float scale = right.length() / rightXZ.length();
        rightXZ = rightXZ.mul(amount * scale, temp1);
        predictor = body.pos.add(rightXZ, temp2);
    }

    @Override
    public void movePredictorYUp(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f upAmount = Camera.Y_AXIS.mul(amount, temp1);
        predictor = body.pos.add(upAmount, temp2);
    }

    @Override
    public void movePredictorYDown(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f upAmount = Camera.Y_AXIS.mul(amount, temp1);
        predictor = body.pos.sub(upAmount, temp2);
    }

    /**
     * Set XYZ Body (Model) Rotation
     *
     * @param newFront new front vec3f
     */
    public void setRotationXYZ(Vector3f newFront) {
        final float newYaw = org.joml.Math.atan2(-newFront.z, newFront.x);
//        float newPitch = -org.joml.Math.atan2(newFront.y, org.joml.Math.sqrt(newFront.x * newFront.x + newFront.z * newFront.z));        
        this.body.setrY(-3.0f * (float) (org.joml.Math.PI) / 2.0f + newYaw);
        updateCameraVectors(newFront);
    }

    public boolean isUnderGravity() {
        return underGravity;
    }

    public void setUnderGravity(boolean underGravity) {
        this.underGravity = underGravity;
    }

    public boolean isInJump() {
        return inJump;
    }

    public void setIsInJump(boolean isJump) {
        this.inJump = isJump;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUniqueId() {
        return uniqueId;
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

    public Model getCharBodyWeaponModel() {
        return charBodyWeaponModel;
    }

    public WeaponIfc getWeapon() {
        return weapon;
    }

}
