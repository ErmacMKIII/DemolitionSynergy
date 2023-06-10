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
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class PerspectiveRenderer {

    public static final Matrix4f PROJECTION_MATRIX = new Matrix4f();
    protected static FloatBuffer floatBuff = MemoryUtil.memCallocFloat(4 * 4);

    protected static boolean buffered = false;

    private static void loadPerspective(float fov, int width, int height, float zNear, float zFar) {
        // LH is for OpenGL way, it's required..
        PROJECTION_MATRIX.setPerspectiveLH(fov, (float) width / (float) height, zNear, zFar);
        PROJECTION_MATRIX.get(floatBuff);
    }

    public static void updatePerspective(Window myWindow) {
        loadPerspective((float) (Math.PI / 2.0f), myWindow.getWidth(), myWindow.getHeight(), 0.05f, 20000.0f);
    }

    public static void bufferAll() {
        floatBuff = MemoryUtil.memCallocFloat(16);
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

    public static FloatBuffer getFloatBuff() {
        return floatBuff;
    }

    public static void setFloatBuff(FloatBuffer floatBuff) {
        PerspectiveRenderer.floatBuff = floatBuff;
    }

    public static boolean isBuffered() {
        return buffered;
    }

    public static void setBuffered(boolean buffered) {
        PerspectiveRenderer.buffered = buffered;
    }

}
