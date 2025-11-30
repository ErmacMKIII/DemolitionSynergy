/* 
 * Copyright (C) 2020 Aleksandar Stojanovic <coas91@rocketmail.com>
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
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.level.GravityEnviroment.Result;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.models.Renderable;
import rs.alexanderstojanovich.evg.resources.Assets;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.HardwareUtils;
import rs.alexanderstojanovich.evg.weapons.WeaponIfc;
import rs.alexanderstojanovich.evg.weapons.Weapons;

/**
 * Critter is class of living things. Has capabilities moving. Is collision
 * predictable. However no observation. Renders with body in some shader.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Critter implements Predictable, Moveable, Renderable {

    protected String modelClazz = Configuration.getInstance().getModel().toLowerCase();
    protected String name;
    public final String uniqueId;
    public Model body;
    protected Vector3f predictor;
    protected Vector3f front = Camera.Z_AXIS;
    protected Vector3f up = Camera.Y_AXIS;
    protected Vector3f right = Camera.X_AXIS;
    protected Result gravityResult = Result.NEUTRAL;
    protected boolean inJump = false;

    /**
     * Critter (could be Player) has nothing in hands (no-weapon)
     */
    protected WeaponIfc weapon = Weapons.NONE;

    /**
     * Game assets resources
     */
    protected final Assets assets;

    /**
     * Create new instance of the critter. If instanced in anonymous class
     * specify the camera
     *
     * @param assets game assets
     * @param body body model
     */
    public Critter(Assets assets, Model body) {
        this.assets = assets;
        this.body = body;
        this.predictor = new Vector3f(body.pos); // separate predictor from the body
        this.uniqueId = HardwareUtils.generateHardwareUUID();
    }

    /**
     * Create new instance of the critter. If instanced in anonymous class
     * specify the camera
     *
     * @param assets game assets
     * @param pos initial position of the critter
     * @param body body model
     */
    public Critter(Assets assets, Vector3f pos, Model body) {
        this.assets = assets;
        this.body = body;
        this.predictor = new Vector3f(body.pos); // separate predictor from the body
        this.uniqueId = HardwareUtils.generateHardwareUUID();
    }

    /**
     * Create new instance of the critter. If instanced in anonymous class
     * specify the camera
     *
     * @param assets game assets
     * @param uniqueId to be assigned to the critter
     * @param body model body for the critter
     */
    public Critter(Assets assets, String uniqueId, Model body) {
        this.assets = assets;
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
    }

    @Override
    public void moveBackward(float amount) {
        Vector3f temp = new Vector3f();
        Vector3f frontAmpl = front.mul(amount, temp);
        body.pos = body.pos.sub(frontAmpl, temp);
    }

    @Override
    public void moveLeft(float amount) {
        Vector3f temp = new Vector3f();
        Vector3f rightAmpl = right.mul(amount, temp);
        body.pos = body.pos.sub(rightAmpl, temp);
    }

    @Override
    public void moveRight(float amount) {
        Vector3f temp = new Vector3f();
        Vector3f rightAmpl = right.mul(amount, temp);
        body.pos = body.pos.add(rightAmpl, temp);
    }

    @Override
    public void ascend(float amount) {
        Vector3f temp = new Vector3f();
        Vector3f upAmpl = up.mul(amount, temp);
        body.pos = body.pos.add(upAmpl, temp);
    }

    @Override
    public void descend(float amount) {
        Vector3f temp = new Vector3f();
        Vector3f upAmpl = up.mul(amount, temp);
        body.pos = body.pos.sub(upAmpl, temp);
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

    /**
     * Switch body model rendered in 3rd person
     */
    public void switchBodyModel() {
        Vector4f colCopy = this.body.getPrimaryRGBAColor();
        Vector3f posCopy = this.body.pos;
        float rYCopy = this.body.getrY();

        // model class or skin (array of models for that skin)
        switch (modelClazz) {
            case "alex":
                // switch to body model having that weapon class
                if (weapon == Weapons.NONE) {
                    this.body = assets.ALEX_BODY_DEFAULT;
                } else {
                    switch (weapon.getTexName()) {
                        case Assets.W01M9:
                            this.body = assets.ALEX_BODY_1H_SG_W01M9;
                            break;
                        case Assets.W02M1:
                            this.body = assets.ALEX_BODY_1H_SG_W02M1;
                            break;
                        case Assets.W03DE:
                            this.body = assets.ALEX_BODY_1H_SG_W03DE;
                            break;
                        case Assets.W04UZ:
                            this.body = assets.ALEX_BODY_1H_SG_W04UZ;
                            break;
                        case Assets.W05M5:
                            this.body = assets.ALEX_BODY_2H_SG_W05M5;
                            break;
                        case Assets.W06P9:
                            this.body = assets.ALEX_BODY_2H_SG_W06P9;
                            break;
                        case Assets.W07AK:
                            this.body = assets.ALEX_BODY_2H_SG_W07AK;
                            break;
                        case Assets.W08M4:
                            this.body = assets.ALEX_BODY_2H_SG_W08M4;
                            break;
                        case Assets.W09G3:
                            this.body = assets.ALEX_BODY_2H_SG_W09G3;
                            break;
                        case Assets.W10M6:
                            this.body = assets.ALEX_BODY_2H_BG_W10M6;
                            break;
                        case Assets.W11MS:
                            this.body = assets.ALEX_BODY_2H_BG_W11MS;
                            break;
                        case Assets.W12W2:
                            this.body = assets.ALEX_BODY_2H_SG_W12W2;
                            break;
                        case Assets.W13B9:
                            this.body = assets.ALEX_BODY_2H_SG_W13B9;
                            break;
                        case Assets.W14R7:
                            this.body = assets.ALEX_BODY_2H_SG_W14R7;
                            break;
                        case Assets.W15DR:
                            this.body = assets.ALEX_BODY_2H_SG_W15DR;
                            break;
                        case Assets.W16M8:
                            this.body = assets.ALEX_BODY_2H_BG_W16M8;
                            break;
                    }
                }
                break;
            case "steve":
                // switch to body model having that weapon class
                if (weapon == Weapons.NONE) {
                    this.body = assets.STEVE_BODY_DEFAULT;
                } else {
                    switch (weapon.getTexName()) {
                        case Assets.W01M9:
                            this.body = assets.STEVE_BODY_1H_SG_W01M9;
                            break;
                        case Assets.W02M1:
                            this.body = assets.STEVE_BODY_1H_SG_W02M1;
                            break;
                        case Assets.W03DE:
                            this.body = assets.STEVE_BODY_1H_SG_W03DE;
                            break;
                        case Assets.W04UZ:
                            this.body = assets.STEVE_BODY_1H_SG_W04UZ;
                            break;
                        case Assets.W05M5:
                            this.body = assets.STEVE_BODY_2H_SG_W05M5;
                            break;
                        case Assets.W06P9:
                            this.body = assets.STEVE_BODY_2H_SG_W06P9;
                            break;
                        case Assets.W07AK:
                            this.body = assets.STEVE_BODY_2H_SG_W07AK;
                            break;
                        case Assets.W08M4:
                            this.body = assets.STEVE_BODY_2H_SG_W08M4;
                            break;
                        case Assets.W09G3:
                            this.body = assets.STEVE_BODY_2H_SG_W09G3;
                            break;
                        case Assets.W10M6:
                            this.body = assets.STEVE_BODY_2H_BG_W10M6;
                            break;
                        case Assets.W11MS:
                            this.body = assets.STEVE_BODY_2H_BG_W11MS;
                            break;
                        case Assets.W12W2:
                            this.body = assets.STEVE_BODY_2H_SG_W12W2;
                            break;
                        case Assets.W13B9:
                            this.body = assets.STEVE_BODY_2H_SG_W13B9;
                            break;
                        case Assets.W14R7:
                            this.body = assets.STEVE_BODY_2H_SG_W14R7;
                            break;
                        case Assets.W15DR:
                            this.body = assets.STEVE_BODY_2H_SG_W15DR;
                            break;
                        case Assets.W16M8:
                            this.body = assets.STEVE_BODY_2H_BG_W16M8;
                            break;
                    }
                }
                break;
        }

        // set the color copy
        this.body.setPrimaryRGBAColor(colCopy);
        // set the copied position VEC3
        this.body.pos.set(posCopy);
        // rotation Y-axis angle copy
        this.body.setrY(rYCopy);
    }

    /**
     * Switch to weapon in hands
     *
     * @param weapons all weapons instance (wraps array)
     * @param index index of (weapon) enumeration
     */
    public void switchWeapon(Weapons weapons, int index) {
        this.weapon = weapons.AllWeapons[index];
        this.switchBodyModel();
    }

    /**
     * Switch to weapon in hands
     *
     * @param weapon weapon to switch to
     */
    public void switchWeapon(WeaponIfc weapon) {
        this.weapon = weapon;
        this.switchBodyModel();
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
    }

    @Override
    public void moveXZBackward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f frontXZ = new Vector3f(front.x, 0.0f, front.z);
        float scale = front.length() / frontXZ.length();
        frontXZ = frontXZ.mul(amount * scale, temp1);
        body.pos = body.pos.sub(frontXZ, temp2);
    }

    @Override
    public void moveXZLeft(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f rightXZ = new Vector3f(right.x, 0.0f, right.z);
        float scale = right.length() / rightXZ.length();
        rightXZ = rightXZ.mul(amount * scale, temp1);
        body.pos = body.pos.sub(rightXZ, temp2);
    }

    @Override
    public void moveXZRight(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f rightXZ = new Vector3f(right.x, 0.0f, right.z);
        float scale = right.length() / rightXZ.length();
        rightXZ = rightXZ.mul(amount * scale, temp1);
        body.pos = body.pos.add(rightXZ, temp2);
    }

    @Override
    public void jumpY(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f upAmount = Camera.Y_AXIS.mul(amount, temp1);
        body.pos = body.pos.add(upAmount, temp2);
    }

    @Override
    public void dropY(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f upAmount = Camera.Y_AXIS.mul(amount, temp1);
        body.pos = body.pos.sub(upAmount, temp2);
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

    public Result gravityResult() {
        return gravityResult;
    }

    public void setGravityResult(Result gravityResult) {
        this.gravityResult = gravityResult;
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

    public WeaponIfc getWeapon() {
        return weapon;
    }

    public void setModelClazz(String modelClazz) {
        this.modelClazz = modelClazz;
        switchBodyModel();
    }

}
