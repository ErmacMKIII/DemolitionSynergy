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
import rs.alexanderstojanovich.evg.main.Game;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Coa
 */
public class PerspectiveRenderer {

    private final static Matrix4f PROJECTION_MATRIX = new Matrix4f();

    private static void perspective(float fov, int width, int height, float zNear, float zFar, ShaderProgram shaderProgram) {
        // LH is for OpenGL way, it's required..
        PROJECTION_MATRIX.setPerspectiveLH(fov, (float) width / (float) height, zNear, zFar);
        FloatBuffer fb = BufferUtils.createFloatBuffer(4 * 4);
        PROJECTION_MATRIX.get(fb);
        int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), "projectionMatrix");
        GL20.glUniformMatrix4fv(uniformLocation, false, fb);
    }

    public static void updatePerspective(Window myWindow) {
        for (ShaderProgram shaderProgram : ShaderProgram.getSHADER_PROGRAMS()) {
            shaderProgram.bind();
            perspective((float) (Math.PI / 2.0f), myWindow.getWidth(), myWindow.getHeight(), Game.EPSILON, 1.0f / Game.EPSILON, shaderProgram);
            ShaderProgram.unbind();
        }
    }

    public static Matrix4f getPROJECTION_MATRIX() {
        return PROJECTION_MATRIX;
    }

}
