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

    private final GameObject gameObject;
    public final Matrix4f PROJECTION_MATRIX = new Matrix4f();
    protected FloatBuffer floatBuff = null;

    protected boolean buffered = false;

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
    private void loadPerspective(float fov, int width, int height, float zNear, float zFar) {
        // LH is for OpenGL way, it's required..
        PROJECTION_MATRIX.setPerspectiveLH(fov, (float) width / (float) height, zNear, zFar);
    }

    /**
     * Update perspective to window resolution (dimension)
     */
    public void updatePerspective() {
        loadPerspective((float) (org.joml.Math.PI / 2.0f), gameObject.WINDOW.getWidth(), gameObject.WINDOW.getHeight(), 0.05f, 24576.0f);
    }

    /**
     * Buffer and render to the projection matrix (shaders)
     */
    @Override
    public void render() {
        floatBuff = MemoryUtil.memCallocFloat(16); // 4x4
        if (floatBuff.capacity() != 0 && MemoryUtil.memAddressSafe(floatBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            return;
        }
        PROJECTION_MATRIX.get(floatBuff);
        for (ShaderProgram shaderProgram : ShaderProgram.SHADER_PROGRAMS) {
            shaderProgram.bind();
            int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), "projectionMatrix");
            GL20.glUniformMatrix4fv(uniformLocation, false, floatBuff);
            ShaderProgram.unbind();
        }
        if (floatBuff.capacity() != 0) {
            MemoryUtil.memFree(floatBuff);
        }

        buffered = true;
    }

    public FloatBuffer getFloatBuff() {
        return floatBuff;
    }

    public void setFloatBuff(FloatBuffer floatBuff) {
        this.floatBuff = floatBuff;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

}
