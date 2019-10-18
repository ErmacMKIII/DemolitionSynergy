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

import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.models.Vertex;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Coa
 */
public class Camera { // is 3D looking camera

    private Vector3f pos; // is camera position in space; it's uniform
    private final Matrix4f viewMatrix = new Matrix4f(); // is view matrix as uniform
    private final ShaderProgram shaderProgram; // is shader program used to bind viewMatrix

    public static final Vector3f X_AXIS = new Vector3f(1.0f, 0.0f, 0.0f);
    public static final Vector3f Y_AXIS = new Vector3f(0.0f, 1.0f, 0.0f);
    public static final Vector3f Z_AXIS = new Vector3f(0.0f, 0.0f, 1.0f);

    // three vectors determining exact camera position aka camera vectors
    private Vector3f front;
    private Vector3f up;
    private Vector3f right;

    private float yaw = (float) (-Math.PI / 2); // sideways look angle
    private float pitch = 0.0f; // up and down look angle

    public Camera(ShaderProgram shaderProgram) {
        this.pos = new Vector3f();
        this.shaderProgram = shaderProgram;

        this.front = Z_AXIS;
        this.up = Y_AXIS;
        this.right = X_AXIS;
    }

    public Camera(Vector3f pos, ShaderProgram shaderProgram) {
        this.pos = pos;
        this.shaderProgram = shaderProgram;

        this.front = Z_AXIS;
        this.up = Y_AXIS;
        this.right = X_AXIS;
    }

    public Camera(Vector3f pos, ShaderProgram shaderProgram, Vector3f front, Vector3f up, Vector3f right) {
        this.pos = pos;
        this.shaderProgram = shaderProgram;

        this.front = front;
        this.up = up;
        this.right = right;
    }

    private void updateViewMatrix() {
        FloatBuffer fb = BufferUtils.createFloatBuffer(4 * 4);
        viewMatrix.get(fb);
        int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), "viewMatrix");
        GL20.glUniformMatrix4fv(uniformLocation, false, fb);
    }

    public void updateViewMatrix(ShaderProgram shaderProgram) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(4 * 4);
        viewMatrix.get(fb);
        int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), "viewMatrix");
        GL20.glUniformMatrix4fv(uniformLocation, false, fb);
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

    private void updateCameraPosition() {
        int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), "cameraPos");
        GL20.glUniform3f(uniformLocation, pos.x, pos.y, pos.z);
    }

    public void updateCameraPosition(ShaderProgram shaderProgram) {
        int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), "cameraPos");
        GL20.glUniform3f(uniformLocation, pos.x, pos.y, pos.z);
    }

    private void updateCameraFront() {
        int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), "cameraFront");
        GL20.glUniform3f(uniformLocation, front.x, front.y, front.z);
    }

    public void updateCameraFront(ShaderProgram shaderProgram) {
        int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), "cameraFront");
        GL20.glUniform3f(uniformLocation, front.x, front.y, front.z);
    }

    public void moveForward(float amount) {
        updateCameraVectors();
        Vector3f temp = new Vector3f();
        pos = pos.add(front.mul(amount, temp), temp);
    }

    public void moveBackward(float amount) {
        updateCameraVectors();
        Vector3f temp = new Vector3f();
        pos = pos.sub(front.mul(amount, temp), temp);
    }

    public void moveLeft(float amount) {
        updateCameraVectors();
        Vector3f temp = new Vector3f();
        pos = pos.sub(right.mul(amount, temp), temp);
    }

    public void moveRight(float amount) {
        updateCameraVectors();
        Vector3f temp = new Vector3f();
        pos = pos.add(right.mul(amount, temp), temp);
    }

    public void turnLeft(float angle) {
        lookAt((float) (yaw - angle), pitch);
    }

    public void turnRight(float angle) {
        lookAt((float) (yaw + angle), pitch);
    }

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
        updateCameraVectors();
    }

    public void lookAt(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        front.x = (float) (Math.cos(this.yaw) * Math.cos(this.pitch));
        front.y = (float) Math.sin(this.pitch);
        front.z = (float) (-Math.sin(this.yaw) * Math.cos(this.pitch));
        updateCameraVectors();
    }

    public void render() {
        calcViewMatrix();
        shaderProgram.bind();
        updateViewMatrix();
        updateCameraPosition();
        updateCameraFront();
        ShaderProgram.unbind();
    }

    public boolean intersects(Model model) {
        boolean coll = false;
        if (!model.isPassable()) {
            boolean boolX = pos.x >= model.getPos().x - model.getWidth() / 2 && pos.x <= model.getPos().x + model.getWidth() / 2;
            boolean boolY = pos.y >= model.getPos().y - model.getHeight() / 2 && pos.y <= model.getPos().y + model.getHeight() / 2;
            boolean boolZ = pos.z >= model.getPos().z - model.getDepth() / 2 && pos.z <= model.getPos().z + model.getDepth() / 2;
            coll = boolX && boolY && boolZ;
        }
        return coll;
    }

    public boolean doesSee(Model model) {
        boolean yea = false;
        for (Vertex vertex : model.getVertices()) {
            Vector3f temp = new Vector3f();
            Vector3f vx = vertex.getPos().add(model.getPos().sub(pos, temp), temp).normalize(temp);
            if (vx.dot(front) >= 0.5) {
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

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
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

}
