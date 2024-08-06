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
package rs.alexanderstojanovich.evg.critter;

import java.util.Collection;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 * Capabilities observing. Can move in 3D space and observe.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface Observer extends Moveable {

    /**
     * Look for xoffset, yoffset using Euler angles.Requires given control (set
     * to true)
     *
     * @param sensitivity mouse sensitivity - multiplier
     * @param xoffset X-axis offset
     * @param yoffset Y-axis offset
     */
    public void lookAtOffset(float sensitivity, float xoffset, float yoffset);

    /**
     * Look at exactly yaw & pitch angle using Euler angles.Requires given
     * control (set to true)
     *
     * @param yaw sideways angle
     * @param pitch up & down angle
     */
    public void lookAtAngle(float yaw, float pitch);

    /**
     * Render this critter.
     *
     * @param shaderProgram shader program used for rendering
     */
    public void render(ShaderProgram shaderProgram);

    /**
     * Render across multiple shaders
     *
     * @param shaderPrograms multiple shader programs (array)
     */
    public void render(Collection<ShaderProgram> shaderPrograms);

    //--------------------------------------------------------------------------    
    /**
     * Get Camera using to observe
     *
     * @return camera using to observer
     */
    public Camera getCamera();

    /**
     * Observer VEC3 position
     *
     * @return observer position
     */
    @Override
    public Vector3f getPos();

    /**
     * Set observer VEC3 position
     *
     * @param pos new obsserver position
     */
    @Override
    public void setPos(Vector3f pos);

    /**
     * Get Z-Positive axis vector
     *
     * @return Z-Axis look at vector
     */
    public Vector3f getFront();

    /**
     * Get Y-Positive axis vector
     *
     * @return Y-Axis look at vector
     */
    public Vector3f getUp();

    /**
     * Get X-Positive axis vector
     *
     * @return X-Axis look at vector
     */
    public Vector3f getRight();
}
