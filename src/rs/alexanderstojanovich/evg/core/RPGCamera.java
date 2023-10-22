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

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class RPGCamera extends Camera {

    private final Model target;
    private final float distanceFromTarget = 10.0f;

    public RPGCamera(Model target) {
        this.target = target;
    }

    public RPGCamera(Model target, Vector3f pos) {
        super(pos);
        this.target = target;
    }

    public RPGCamera(Model target, Vector3f pos, Vector3f front, Vector3f up, Vector3f right) {
        super(pos, front, up, right);
        this.target = target;
    }

    private float angleAroundTarget() {
        return target.getrY();
    }

    private float horizontalDistance() {
        return this.distanceFromTarget * org.joml.Math.sin(angleAroundTarget());
    }

    private float verticalDistance() {
        return this.distanceFromTarget * org.joml.Math.cos(angleAroundTarget());
    }

    protected void calcCameraPos() {
        pos.x = this.target.pos.x + horizontalDistance() * org.joml.Math.sin(yaw + angleAroundTarget());
        pos.y = this.target.pos.y + verticalDistance();
        pos.z = this.target.pos.z + horizontalDistance() * org.joml.Math.cos(yaw + angleAroundTarget());
    }

    private void calcViewMatrix() {
        calcCameraPos();
        updateCameraVectors();

        Vector3f temp = new Vector3f();
        viewMatrix.setLookAt(pos, pos.sub(front, temp), up);
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

        front.x = (float) (org.joml.Math.cos(org.joml.Math.PI + (yaw + angleAroundTarget())) * org.joml.Math.cos(this.pitch));
        front.y = (float) org.joml.Math.sin(this.pitch);
        front.z = (float) (-org.joml.Math.sin(org.joml.Math.PI + (yaw + angleAroundTarget())) * org.joml.Math.cos(this.pitch));
        calcViewMatrix();
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
        front.x = (float) (org.joml.Math.cos(org.joml.Math.PI + (this.yaw + angleAroundTarget())) * org.joml.Math.cos(this.pitch));
        front.y = (float) org.joml.Math.sin(this.pitch);
        front.z = (float) (-org.joml.Math.sin(org.joml.Math.PI + (this.yaw + angleAroundTarget())) * org.joml.Math.cos(this.pitch));
        calcViewMatrix();
    }

    public Model getTarget() {
        return target;
    }

    public float getDistanceFromTarget() {
        return distanceFromTarget;
    }

}
