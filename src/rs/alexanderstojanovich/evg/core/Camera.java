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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.models.Mesh;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.models.Vertex;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 * Represents 3D, first person abstract looking camera. Yaw (sideways rotation)
 * and Pitch (looking up and down) is available. Uses Euler angles instead of
 * Quaternions.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Camera implements Observer { // is 3D looking camera

    public Vector3f pos; // is camera position in space; it's uniform
    public final Matrix4f viewMatrix = new Matrix4f().zero(); // is view matrix as uniform

    public static final Vector3f X_AXIS = new Vector3f(1.0f, 0.0f, 0.0f);
    public static final Vector3f Y_AXIS = new Vector3f(0.0f, 1.0f, 0.0f);
    public static final Vector3f Z_AXIS = new Vector3f(0.0f, 0.0f, 1.0f);

    public static final Vector3f XNEG_AXIS = new Vector3f(-1.0f, 0.0f, 0.0f);
    public static final Vector3f YNEG_AXIS = new Vector3f(0.0f, -1.0f, 0.0f);
    public static final Vector3f ZNEG_AXIS = new Vector3f(0.0f, 0.0f, -1.0f);

    // three vectors determining exact camera position aka camera vectors
    protected Vector3f front = Z_AXIS;
    protected Vector3f up = Y_AXIS;
    protected Vector3f right = X_AXIS;

    protected float yaw = (float) (-org.joml.Math.PI) / 2.0f; // sideways look angle
    protected float pitch = (float) (-org.joml.Math.PI); // up and down look angle

    public Camera() {
        this.pos = new Vector3f();

        this.front = Z_AXIS;
        this.up = Y_AXIS;
        this.right = X_AXIS;
        initViewMatrix();
    }

    public Camera(Vector3f pos) {
        this.pos = pos;

        this.front = Z_AXIS;
        this.up = Y_AXIS;
        this.right = X_AXIS;
        initViewMatrix();
    }

    public Camera(Vector3f pos, Vector3f front, Vector3f up, Vector3f right) {
        this.pos = pos;

        this.front = front;
        this.up = up;
        this.right = right;
        initViewMatrix();
    }

    public void updateViewMatrix(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(viewMatrix, "viewMatrix");
    }

    protected void updateCameraVectors() {
        Vector3f temp1 = new Vector3f();
        front = front.normalize(temp1);
        Vector3f temp2 = new Vector3f();
        right = Y_AXIS.cross(front, temp2).normalize(temp2);
        Vector3f temp3 = new Vector3f();
        up = front.cross(right, temp3).normalize(temp3);
    }

    private void initViewMatrix() {
        updateCameraVectors();
        Vector3f temp = new Vector3f();
        viewMatrix.setLookAt(pos, pos.sub(front, temp), up);
    }

    protected void calcViewMatrix() {
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
    @Override
    public void moveForward(float amount) {
        Vector3f temp = new Vector3f();
        pos = pos.add(front.mul(amount, temp), temp);
    }

    /**
     * Move camera backward (towards negative Z-axis).
     *
     * @param amount amount subtracted backward
     */
    @Override
    public void moveBackward(float amount) {
        Vector3f temp = new Vector3f();
        pos = pos.sub(front.mul(amount, temp), temp);
    }

    /**
     * Move camera left (towards negative X-axis).
     *
     * @param amount to move left.
     */
    @Override
    public void moveLeft(float amount) {
        Vector3f temp = new Vector3f();
        pos = pos.sub(right.mul(amount, temp), temp);
    }

    /**
     * Move camera left (towards positive X-axis).
     *
     * @param amount to move right.
     */
    @Override
    public void moveRight(float amount) {
        Vector3f temp = new Vector3f();
        pos = pos.add(right.mul(amount, temp), temp);
    }

    /**
     * Move camera up (towards positive Y-axis)
     *
     * @param amount to move up.
     */
    @Override
    public void ascend(float amount) {
        Vector3f temp = new Vector3f();
        pos = pos.add(up.mul(amount, temp), temp);
    }

    /**
     * Move camera down (towards negative Y-axis)
     *
     * @param amount to move down.
     */
    @Override
    public void descend(float amount) {
        Vector3f temp = new Vector3f();
        pos = pos.sub(up.mul(amount, temp), temp);
    }

    /**
     * Turn left specified by angle from the game.
     *
     * @param angle angle to turn left (in radians)
     */
    @Override
    public void turnLeft(float angle) {
        lookAtAngle((float) (yaw - angle), pitch);
    }

    /**
     * Turn right specified by angle from the game.
     *
     * @param angle angle to turn right (in radians)
     */
    @Override
    public void turnRight(float angle) {
        lookAtAngle((float) (yaw + angle), pitch);
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
        pitch += sensitivity * yoffset;

        if (pitch > org.joml.Math.PI / 2.1) {
            pitch = (float) (org.joml.Math.PI / 2.1);
        }
        if (pitch < -org.joml.Math.PI / 2.1) {
            pitch = (float) (-org.joml.Math.PI / 2.1);
        }

        front.x = (float) (org.joml.Math.cos(yaw) * org.joml.Math.cos(pitch));
        front.y = (float) org.joml.Math.sin(pitch);
        front.z = (float) (-org.joml.Math.sin(yaw) * org.joml.Math.cos(pitch));
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
        front.x = (float) (org.joml.Math.cos(this.yaw) * org.joml.Math.cos(this.pitch));
        front.y = (float) org.joml.Math.sin(this.pitch);
        front.z = (float) (-org.joml.Math.sin(this.yaw) * org.joml.Math.cos(this.pitch));
    }

    /**
     * Render across single shader
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
    public void render(Collection<ShaderProgram> shaderPrograms) {
        calcViewMatrix();
        for (ShaderProgram shaderProgram : shaderPrograms) {
            shaderProgram.bind();
            updateViewMatrix(shaderProgram);
            updateCameraPosition(shaderProgram);
            updateCameraFront(shaderProgram);
            ShaderProgram.unbind();
        }
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

    /**
     * Small function to determine if camera does see model from its position
     * and front vector (Legacy.)
     *
     * @param model observation model
     *
     * @return wether or not camera does see model
     */
    public boolean doesSee(Model model) {
        boolean yea = false;
        Vector3f camFrontNeg = new Vector3f(-front.x, -front.y, -front.z);
        for (Vertex vertex : model.meshes.getFirst().vertices) {
            Vector3f temp = new Vector3f();
            Vector3f vx = vertex.getPos().add(model.pos.sub(pos, temp), temp).normalize(temp);
            if (vx.dot(camFrontNeg) <= 0.25f) {
                yea = true;
                break;
            }
        }
        return yea;
    }

    /**
     * Efficient small function to determine if camera does see model from its
     * position and front vector. Efficiency comes from removing duplicate
     * vertices first. (Before check.)
     *
     * @param model observation model
     *
     * @return wether or not camera does see model
     */
    public boolean doesSeeEff(Model model) {
        final float cosine = (float) Math.cos(Math.toRadians(45f));
        final Vector3f camFrontNeg = new Vector3f(-front.x, -front.y, -front.z);

        // Reusable vectors to avoid object creation in loop
        final Vector3f directionToVertex = new Vector3f();
        final Vector3f modelToCam = new Vector3f();

        // Calculate model-to-camera vector once
        model.pos.sub(pos, modelToCam);

        // Use direct array access instead of streams for better performance
        final Mesh firstMesh = model.meshes.getFirst();
        final Vertex[] vertices = firstMesh.vertices.toArray(Vertex[]::new);
        final int vertexCount = vertices.length;

        // Use Set for deduplication with better performance characteristics
        final Set<Vector3f> seenPositions = new HashSet<>(vertexCount);

        for (int i = 0; i < vertexCount; i++) {
            Vector3f vertexPos = vertices[i].getPos();

            // Skip duplicate positions
            if (!seenPositions.add(vertexPos)) {
                continue;
            }

            // Calculate direction: vertexPos + (model.pos - pos) then normalize
            directionToVertex.set(vertexPos).add(modelToCam).normalize();

            if (directionToVertex.dot(camFrontNeg) <= cosine) {
                return true; // Early return when found
            }
        }

        return false;
    }

    /**
     * Optimized function to determine if camera sees model from its position
     * and front vector. Uses efficient vector operations, avoids object
     * creation in loops, and provides early exit.
     *
     * @param model observation model
     * @param degrees angle degrees of front view
     * @return whether or not the camera does see the model
     */
    public boolean doesSeeEff(Model model, float degrees) {
        final float cosine = (float) Math.cos(Math.toRadians(degrees));
        final Vector3f camFrontNeg = new Vector3f(-front.x, -front.y, -front.z);

        // Reusable vectors to avoid object creation in loop
        final Vector3f directionToVertex = new Vector3f();
        final Vector3f modelToCam = new Vector3f();

        // Calculate model-to-camera vector once
        model.pos.sub(pos, modelToCam);

        // Use direct array access instead of streams for better performance
        final Mesh firstMesh = model.meshes.getFirst();
        final Vertex[] vertices = firstMesh.vertices.toArray(Vertex[]::new);
        final int vertexCount = vertices.length;

        // Use Set for deduplication with better performance characteristics
        final Set<Vector3f> seenPositions = new HashSet<>(vertexCount);

        for (int i = 0; i < vertexCount; i++) {
            Vector3f vertexPos = vertices[i].getPos();

            // Skip duplicate positions
            if (!seenPositions.add(vertexPos)) {
                continue;
            }

            // Calculate direction: vertexPos + (model.pos - pos) then normalize
            directionToVertex.set(vertexPos).add(modelToCam).normalize();

            if (directionToVertex.dot(camFrontNeg) <= cosine) {
                return true; // Early return when found
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "Camera{" + "pos=" + pos + ", front=" + front + ", up=" + up + ", right=" + right + '}';
    }

    @Override
    public Vector3f getPos() {
        return pos;
    }

    @Override
    public void setPos(Vector3f pos) {
        this.pos = pos;
        calcViewMatrix();
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    @Override
    public Vector3f getFront() {
        return front;
    }

    @Override
    public Vector3f getUp() {
        return up;
    }

    @Override
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

    @Override
    public Camera getCamera() {
        return this;
    }

    @Override
    public void moveXZForward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f frontXZ = new Vector3f(front.x, 0.0f, front.z);
        float scale = front.length() / frontXZ.length();
        frontXZ = frontXZ.mul(amount * scale, temp1);
        this.pos = this.pos.add(frontXZ, temp2);
    }

    @Override
    public void moveXZBackward(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f frontXZ = new Vector3f(front.x, 0.0f, front.z);
        float scale = front.length() / frontXZ.length();
        frontXZ = frontXZ.mul(amount * scale, temp1);
        this.pos = this.pos.sub(frontXZ, temp2);
    }

    @Override
    public void moveXZLeft(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f rightXZ = new Vector3f(right.x, 0.0f, right.z);
        float scale = right.length() / rightXZ.length();
        rightXZ = rightXZ.mul(amount * scale, temp1);
        this.pos = this.pos.sub(rightXZ, temp2);
    }

    @Override
    public void moveXZRight(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f rightXZ = new Vector3f(right.x, 0.0f, right.z);
        float scale = right.length() / rightXZ.length();
        rightXZ = rightXZ.mul(amount * scale, temp1);
        this.pos = this.pos.add(rightXZ, temp2);
    }

    @Override
    public void jumpY(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f upAmount = Camera.Y_AXIS.mul(amount, temp1);
        this.pos = this.pos.add(upAmount, temp2);
    }

    @Override
    public void dropY(float amount) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f upAmount = Camera.Y_AXIS.mul(amount, temp1);
        this.pos = this.pos.sub(upAmount, temp2);
    }

}
