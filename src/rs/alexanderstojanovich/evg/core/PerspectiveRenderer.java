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

import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class PerspectiveRenderer implements CoreRenderer {

    public static final float FOV = (float) (org.joml.Math.PI / 2.0f);
    public static final float NEAR_PLANE = 0.05f;
    public static final float FAR_PLANE = 12288.0f;

    private final GameObject gameObject;
    public final Matrix4f perspectiveMatrix = new Matrix4f();
    public final Matrix4f orthogonalMatrix = new Matrix4f();
    protected FloatBuffer floatBuffPerspective = null;
    protected FloatBuffer floatBuffOrthogonal = null;

    public PerspectiveRenderer(GameObject gameObject) {
        this.gameObject = gameObject;
    }

    /**
     * Load Perspective (projection matrix used in shaders)
     *
     * @param fov field of view (in radians)
     * @param width screen width
     * @param height screen height
     * @param zNear nearest point
     * @param zFar furthest point
     */
    private void loadOrthogonal(float left, float right, float bottom, float top, float zNear, float zFar) {
        // LH is for OpenGL way, it's required..
        orthogonalMatrix.setOrthoLH(left, right, bottom, top, zNear, zFar);
    }

    /**
     * Load Perspective (projection matrix used in shaders)
     *
     * @param fov field of view (in radians)
     * @param width screen width
     * @param height screen height
     * @param zNear nearest point
     * @param zFar furthest point
     */
    private void loadPerspective(float fov, int width, int height, float zNear, float zFar) {
        // LH is for OpenGL way, it's required..
        perspectiveMatrix.setPerspectiveLH(fov, (float) width / (float) height, zNear, zFar);
    }

    /**
     * Update perspective to window resolution (dimension)
     */
    public void updatePerspective() {
        loadPerspective(FOV, gameObject.WINDOW.getWidth(), gameObject.WINDOW.getHeight(), NEAR_PLANE, FAR_PLANE);
    }

    /**
     * Update orthogonal to bounding box width * height * depth. Bounding box as
     * cuboid is represented by 8 vertices.
     *
     */
    public void updateOrthogonal() {
        final ShadowBox shadowBox = gameObject.shadowRenderer.shadowBox;
        loadOrthogonal(shadowBox.minX, shadowBox.maxX, shadowBox.minY, shadowBox.maxY, shadowBox.minZ, shadowBox.maxZ);
    }

    /**
     * Initialize. Allocate memory
     */
    public void init() {
        floatBuffPerspective = MemoryUtil.memCallocFloat(16); // 4x4
        if (floatBuffPerspective.capacity() != 0 && MemoryUtil.memAddressSafe(floatBuffPerspective) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
        }
        floatBuffOrthogonal = MemoryUtil.memCallocFloat(16); // 4x4
        if (floatBuffOrthogonal.capacity() != 0 && MemoryUtil.memAddressSafe(floatBuffOrthogonal) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
        }
    }

    /**
     * Buffer and render to the projection matrix (shaders)
     */
    @Override
    public void render() {
        perspectiveMatrix.get(floatBuffPerspective);

        for (ShaderProgram shaderProgram : ShaderProgram.ACTOR_SHADERS) {
            shaderProgram.bind();
            int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), "projectionMatrix");
            GL20.glUniformMatrix4fv(uniformLocation, false, floatBuffPerspective);
            ShaderProgram.unbind();
        }

        for (ShaderProgram shaderProgram : ShaderProgram.ENVIRONMENTAL_SHADERS) {
            shaderProgram.bind();
            int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), "projectionMatrix");
            GL20.glUniformMatrix4fv(uniformLocation, false, floatBuffPerspective);
            ShaderProgram.unbind();
        }

        for (ShaderProgram shaderProgram : ShaderProgram.WATER_SHADERS) {
            shaderProgram.bind();
            int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), "projectionMatrix");
            GL20.glUniformMatrix4fv(uniformLocation, false, floatBuffPerspective);
            ShaderProgram.unbind();
        }

        orthogonalMatrix.get(floatBuffOrthogonal);

        for (ShaderProgram shaderProgram : ShaderProgram.SHADOW_SHADERS) {
            shaderProgram.bind();
            int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), "projectionMatrix");
            GL20.glUniformMatrix4fv(uniformLocation, false, floatBuffOrthogonal);
            ShaderProgram.unbind();
        }
    }

    @Override
    public void prepare() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void release() {
        if (floatBuffPerspective.capacity() != 0) {
            MemoryUtil.memFree(floatBuffPerspective);
        }
        if (floatBuffOrthogonal.capacity() != 0) {
            MemoryUtil.memFree(floatBuffOrthogonal);
        }
    }

    public FloatBuffer getFloatBuffPerspective() {
        return floatBuffPerspective;
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public Matrix4f getPerspectiveMatrix() {
        return perspectiveMatrix;
    }

    public Matrix4f getOrthogonalMatrix() {
        return orthogonalMatrix;
    }

    public FloatBuffer getFloatBuffOrthogonal() {
        return floatBuffOrthogonal;
    }

}
