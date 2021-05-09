/*
 * Copyright (C) 2021 Alexander Stojanovich <coas91@rocketmail.com>
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
package rs.alexanderstojanovich.evg.intrface;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface ComponentIfc {

    public int getWidth();

    public void setWidth(int width);

    public int getHeight();

    public void setHeight(int height);

    public Vector2f getPos();

    public void setPos(Vector2f pos);

    public Vector3f getColor();

    public void setColor(Vector3f color);

    public Matrix4f getModelMatrix();

    public float getScale();

    public boolean isEnabled();

    public void setEnabled(boolean enabled);

    public boolean isBuffered();

    public FloatBuffer getFloatBuffer();

    public IntBuffer getIntBuffer();

    public int getVbo();

    public int getIbo();

    public void render(ShaderProgram shaderProgram);

    public void unbuffer();

    public void bufferVertices();

    public void updateVertices();

    public void bufferIndices();

    public void bufferAll();

    public void bufferSmart();

}
