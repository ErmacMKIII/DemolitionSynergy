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
package rs.alexanderstojanovich.evg.core;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.models.Vertex;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 * Represents 3D, first person looking camera. Yaw (sideways rotation) and Pitch
 * (looking up and down) is available. Uses Euler angles instead of Quaternions.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Camera { // is 3D looking camera

    private Vector3f pos; // is camera position in space; it's uniform
    private final Matrix4f viewMatrix = new Matrix4f(); // is view matrix as uniform

    public static final Vector3f X_AXIS = new Vector3f(1.0f, 0.0f, 0.0f);
    public static final Vector3f Y_AXIS = new Vector3f(0.0f, 1.0f, 0.0f);
    public static final Vector3f Z_AXIS = new Vector3f(0.0f, 0.0f, 1.0f);

    // three vectors determining exact camera position aka camera vectors
    private Vector3f front = Z_AXIS;
    private Vector3f up = Y_AXIS;
    private Vector3f right = X_AXIS;

    private float yaw = (float) (-Math.PI / 2.0); // sideways look angle
    private float pitch = (float) (-Math.PI); // up and down look angle

    public Camera() {
        this.pos = new Vector3f();

        this.front = Z_AXIS;
        this.up = Y_AXIS;
        this.right = X_AXIS;
        calcViewMatrix();
    }

    public Camera(Vector3f pos) {
        this.pos = pos;

        this.front = Z_AXIS;
        this.up = Y_AXIS;
        this.right = X_AXIS;
        calcViewMatrix();
    }

    public Camera(Vector3f pos, Vector3f front, Vector3f up, Vector3f right) {
        this.pos = pos;

        this.front = front;
        this.up = up;
        this.right = right;
        calcViewMatrix();
    }

    public void updateViewMatrix(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(viewMatrix, "viewMatrix");
    }

    private void updateCameraVectors() {
        Vector3f temp1 = new Vector3f();
        front = front.normalize(temp1);
        Vector3f temp2 = new Vector3f();
        right = Y_AXIS.cross(front, temp2).normalize(temp2);
        Vector3f temp3 = new Vector3f();
        up = front.cross(right, temp3).normalize(temp3);
    }

    private void calcViewMatrix() {
        updateCameraVectors();
        Vector3f temp = new Vector3f();
        viewMatrix.setLookAt(pos, pos.sub(front, temp), up);
    }

    public void calcViewMatrixPub() { // public version of force calc
        updateCameraVectors();
        Vector3f temp = new Vector3f();
        viewMatrix.setLookAt(pos, pos.sub(front, temp), up);
    }

    public void updateCameraPosition(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(pos, "cameraPos");
    }

    public void updateCameraFront(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(front, "cameraFront");
    }

    /**
     * Move camera forward (towards positive Z-axis).
     *
     * @param amount amount added forward
     */
    public void moveForward(float amount) {
        Vector3f temp = new Vector3f();
        pos = pos.add(front.mul(amount, temp), temp);
        calcViewMatrix();
    }

    /**
     * Move camera backward (towards negative Z-axis).
     *
     * @param amount amount subtracted backward
     */
    public void moveBackward(float amount) {
        Vector3f temp = new Vector3f();
        pos = pos.sub(front.mul(amount, temp), temp);
        calcViewMatrix();
    }

    /**
     * Move camera left (towards negative X-axis).
     *
     * @param amount to move left.
     */
    public void moveLeft(float amount) {
        Vector3f temp = new Vector3f();
        pos = pos.sub(right.mul(amount, temp), temp);
        calcViewMatrix();
    }

    /**
     * Move camera left (towards positive X-axis).
     *
     * @param amount to move right.
     */
    public void moveRight(float amount) {
        Vector3f temp = new Vector3f();
        pos = pos.add(right.mul(amount, temp), temp);
        calcViewMatrix();
    }

    /**
     * Turn left specified by angle from the game.
     *
     * @param angle angle to turn left (in radians)
     */
    public void turnLeft(float angle) {
        lookAt((float) (yaw - angle), pitch);
    }

    /**
     * Turn right specified by angle from the game.
     *
     * @param angle angle to turn right (in radians)
     */
    public void turnRight(float angle) {
        lookAt((float) (yaw + angle), pitch);
    }

    /**
     * This method gains ability look around using yaw & pitch angles.
     *
     * @param sensitivity mouse sensitivity set ingame
     * @param xoffset offset on X-axis
     * @param yoffset offset on Y-axis
     */
    public void lookAt(float sensitivity, float xoffset, float yoffset) {
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
        calcViewMatrix();
    }

    /**
     * This method is used for turning around using yaw & pitch angles.
     *
     * @param yaw sideways angle
     * @param pitch up & down angle
     */
    public void lookAt(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        front.x = (float) (Math.cos(this.yaw) * Math.cos(this.pitch));
        front.y = (float) Math.sin(this.pitch);
        front.z = (float) (-Math.sin(this.yaw) * Math.cos(this.pitch));
        calcViewMatrix();
    }

    public void render(ShaderProgram shaderProgram) {
        shaderProgram.bind();
        updateViewMatrix(shaderProgram);
        updateCameraPosition(shaderProgram);
        updateCameraFront(shaderProgram);
        ShaderProgram.unbind();
    }

    public boolean intersects(Model model) {
        boolean coll = false;
        if (model.isSolid()) {
            boolean boolX = pos.x >= model.getPos().x - model.getWidth() / 2.0f && pos.x <= model.getPos().x + model.getWidth() / 2.0f;
            boolean boolY = pos.y >= model.getPos().y - model.getHeight() / 2.0f && pos.y <= model.getPos().y + model.getHeight() / 2.0f;
            boolean boolZ = pos.z >= model.getPos().z - model.getDepth() / 2.0f && pos.z <= model.getPos().z + model.getDepth() / 2.0f;
            coll = boolX && boolY && boolZ;
        }
        return coll;
    }

    public boolean doesSee(Model model) {
        boolean yea = false;
        for (Vertex vertex : model.getVertices()) {
            Vector3f temp = new Vector3f();
            Vector3f vx = vertex.getPos().add(model.getPos().sub(pos, temp), temp).normalize(temp);
            if (vx.dot(front) >= 0.25f) {
                yea = true;
                break;
            }
        }
        return yea;
    }

    @Override
    public String toString() {
        return "Camera{" + "pos=" + pos + ", front=" + front + ", up=" + up + ", right=" + right + '}';
    }

    public Vector3f getPos() {
        return pos;
    }

    public void setPos(Vector3f pos) {
        this.pos = pos;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
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

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
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

}
