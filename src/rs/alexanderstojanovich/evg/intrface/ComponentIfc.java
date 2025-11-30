/*
 * Copyright (C) 2021 Aleksandar Stojanovic <coas91@rocketmail.com>
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

import org.joml.Vector2f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 * Interface component 2D
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public interface ComponentIfc {

    /**
     * Component width in pixels
     *
     * @return component width
     */
    public int getWidth();

    /**
     * Component height in pixels
     *
     * @return component height
     */
    public int getHeight();

    /**
     * Component position on the screen (in GL coordinates)
     *
     * @return position on the screen
     */
    public Vector2f getPos();

    /**
     * Set component position on the screen (in GL coordinates)
     *
     * @param pos must be position on the screen
     */
    public void setPos(Vector2f pos);

    /**
     * RGBA Color in OpenGL notation (scaled from 0 to 1)
     *
     * @return VEC4 RGBA Color
     */
    public Vector4f getColor();

    /**
     * Set RGBA Color in OpenGL notation (scaled from 0 to 1)
     *
     * @param color VEC4 RGBA Color
     */
    public void setColor(Vector4f color);

    /**
     * Additional scaling to standard width & height (default is 1.0)
     *
     * @return scale (decimal number)
     */
    public float getScale();

    /**
     * Is component enabled - only if enabled it is visible and rendered.
     *
     * @return is component visible
     */
    public boolean isEnabled();

    /**
     * Set Component enabled - only if enabled it is visible and rendered.
     *
     * @param enabled component visibility
     */
    public void setEnabled(boolean enabled);

    /**
     * Is component allowed to be rendered (to be drawn)
     *
     * @return is component buffered (allowed to be drawn)
     */
    public boolean isBuffered();

    /**
     * Get vertex buffer object.
     *
     * @return vertex buffer object
     */
    public int getVbo();

    /**
     * Get index buffer object.
     *
     * @return vertex buffer object
     */
    public int getIbo();

    /**
     * Render using the shader program
     *
     * @param intrface intrface to which component is rendered
     * @param shaderProgram shader program to use
     */
    public void render(Intrface intrface, ShaderProgram shaderProgram);

    /**
     * Set this component to disallow rendering unless it is buffered again
     */
    public void unbuffer();

    /**
     * Buffer vertices. The part of buffering. Indices need additionally to be
     * buffered.
     *
     * @return is buffered (if allocation memory fails it is not buffered)
     */
    public boolean bufferVertices();

    /**
     * Just update vertices without need to resize the buffer. Faster operation
     * than buffer vertices.
     *
     * @return is buffered (if allocation memory fails it is not buffered)
     */
    public boolean subBufferVertices();

    /**
     * Buffer indices. The part of buffering. Vertices need additionally to be
     * buffered.
     *
     * @return is buffered (if allocation memory fails it is not buffered)
     */
    public boolean bufferIndices();

    /**
     * Update indices. Without need to resize the buffer. Faster operation than
     * buffer indices.
     *
     * @return is buffered (if allocation memory fails it is not buffered)
     */
    public boolean subBufferIndices();

    /**
     * Buffer vertices and indices and set buffered flag accordingly.
     *
     * @param intrface intrface
     */
    public void bufferAll(Intrface intrface);

    /**
     * Buffer/update vertices & indices depending if it is first time and/or
     * vertex buffer the same size as prior.
     *
     * @param intrface intrface
     */
    public void bufferSmart(Intrface intrface);

    /**
     * Release this components. All GL buffers are deleted.
     */
    public void release();

    /**
     * Get game interface.
     *
     * @return game interface
     */
    public Intrface getIntrface();
}
