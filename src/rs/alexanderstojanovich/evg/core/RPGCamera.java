/*
 * Copyright (C) 2023 coas9
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
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class RPGCamera extends Camera {

    private final Model target;
    private final float distanceFromTarget = 10.0f;
    private float angleAroundTarget = 0.0f;
    private final float tiltAngleTarget = (float) org.joml.Math.PI / 4.0f;

    public RPGCamera(Model target) {
        super();
        this.target = target;
        initViewMatrix();
    }

    public RPGCamera(Model target, Vector3f pos) {
        super(pos);
        this.target = target;
        initViewMatrix();
    }

    public RPGCamera(Model target, Vector3f pos, Vector3f front, Vector3f up, Vector3f right) {
        super(pos, front, up, right);
        this.target = target;
        initViewMatrix();
    }

    private float horizontalDistance() {
        return this.distanceFromTarget * org.joml.Math.sin(-this.pitch + 3.0f * (float) org.joml.Math.PI / 2.0f);
    }

    private float verticalDistance() {
        return this.distanceFromTarget * org.joml.Math.cos(-this.pitch + 3.0f * (float) org.joml.Math.PI / 2.0f);
    }

    protected void calcCameraPos() {
        pos.x = target.pos.x + horizontalDistance() * org.joml.Math.sin(-(target.getrY() + angleAroundTarget)); //* org.joml.Math.sin(tiltAngleTarget);
        pos.y = target.pos.y + verticalDistance(); //* org.joml.Math.cos(tiltAngleTarget);
        pos.z = target.pos.z + horizontalDistance() * org.joml.Math.cos(-(target.getrY() + angleAroundTarget));// * org.joml.Math.sin(tiltAngleTarget);
    }

    private void initViewMatrix() {
        updateCameraVectors();
        Vector3f temp = new Vector3f();
        calcCameraPos();
        viewMatrix.setLookAt(pos, pos.sub(front, temp), up);
    }

    @Override
    protected void calcViewMatrix() {
        updateCameraVectors();
        Vector3f temp = new Vector3f();
        calcCameraPos();
        viewMatrix.setLookAt(pos, pos.sub(front, temp), up);
    }

    /**
     * Turn left specified by angle from the game.
     *
     * @param angle angle to turn left (in radians)
     */
    @Override
    public void turnLeft(float angle) {
        super.turnLeft(angle);
        target.setrY(-angle);
        this.angleAroundTarget -= angle * 0.05f;
    }

    /**
     * Turn right specified by angle from the game.
     *
     * @param angle angle to turn right (in radians)
     */
    @Override
    public void turnRight(float angle) {
        super.turnRight(angle);
        target.setrY(+angle);
        this.angleAroundTarget += angle * 0.05f;
    }

    /**
     * This method gains ability look around using yaw & pitch angles.
     *
     * @param sensitivity mouse sensitivity set ingame
     * @param xoffset offset on X-axis
     * @param yoffset offset on Y-axis
     */
    @Override
    public void lookAtOffset(float sensitivity, float xoffset, float yoffset) {
        yaw += sensitivity * xoffset;
        this.angleAroundTarget += sensitivity * xoffset / 2.0f;
        while (yaw >= 2.0 * org.joml.Math.PI) {
            yaw -= 2.0 * org.joml.Math.PI;
        }
        pitch += sensitivity * yoffset;
        if (pitch > org.joml.Math.PI / 2.1) {
            pitch = (float) (org.joml.Math.PI / 2.1);
        }
        if (pitch < -org.joml.Math.PI / 2.1) {
            pitch = (float) (-org.joml.Math.PI / 2.1);
        }

        front.x = (float) (org.joml.Math.cos(-this.yaw - this.angleAroundTarget + (float) org.joml.Math.PI) * org.joml.Math.cos(pitch));
        front.y = (float) org.joml.Math.sin(pitch);
        front.z = (float) (-org.joml.Math.sin(-this.yaw - this.angleAroundTarget + (float) org.joml.Math.PI) * org.joml.Math.cos(pitch));
    }

    /**
     * This method is used for turning around using yaw & pitch angles.
     *
     * @param yaw sideways angle
     * @param pitch up & down angle
     */
    @Override
    public void lookAtAngle(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.angleAroundTarget = yaw * 0.05f;
        front.x = (float) (org.joml.Math.cos(-this.yaw - this.angleAroundTarget + (float) org.joml.Math.PI) * org.joml.Math.cos(this.pitch));
        front.y = (float) org.joml.Math.sin(this.pitch);
        front.z = (float) (-org.joml.Math.sin(-this.yaw - this.angleAroundTarget + (float) org.joml.Math.PI) * org.joml.Math.cos(this.pitch));
    }

    /**
     * Render accross single shader
     *
     * @param shaderProgram single shader program
     */
    @Override
    public void render(ShaderProgram shaderProgram) {
        calcViewMatrix();
        shaderProgram.bind();
        updateViewMatrix(shaderProgram);
        updateCameraPosition(shaderProgram);
        updateCameraFront(shaderProgram);
        ShaderProgram.unbind();
    }

    /**
     * Render across multiple shaders
     *
     * @param shaderPrograms multiple shader programs (array)
     */
    @Override
    public void render(ShaderProgram[] shaderPrograms) {
        calcViewMatrix();
        for (ShaderProgram shaderProgram : shaderPrograms) {
            shaderProgram.bind();
            updateViewMatrix(shaderProgram);
            updateCameraPosition(shaderProgram);
            updateCameraFront(shaderProgram);
            ShaderProgram.unbind();
        }
    }

    public Model getTarget() {
        return target;
    }

    public float getDistanceFromTarget() {
        return distanceFromTarget;
    }

    public float getAngleAroundTarget() {
        return angleAroundTarget;
    }

    public float getTiltAngleTarget() {
        return tiltAngleTarget;
    }

}
