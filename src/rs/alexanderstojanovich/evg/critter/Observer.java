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

import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 * Capabilities moving & observing. Can move in 3D space and observe.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface Observer {

    /**
     * Move camera forward (towards positive Z-axis).
     *
     * @param amount amount added forward
     */
    public void moveForward(float amount);

    /**
     * Move camera backward (towards negative Z-axis).
     *
     * @param amount amount subtracted backward
     */
    public void moveBackward(float amount);

    /**
     * Move camera left (towards negative X-axis).
     *
     * @param amount to move left.
     */
    public void moveLeft(float amount);

    /**
     * Move camera left (towards positive X-axis).
     *
     * @param amount to move right.
     */
    public void moveRight(float amount);

    //--------------------------------------------------------------------------
    /**
     * Turn this critter left side for given angle. To turn critter has to have
     * give control (set to true).
     *
     * @param angle radian angle to turn critter to the left.
     */
    public void turnLeft(float angle);

    /**
     * Turn this critter right side for given angle. To turn critter has to have
     * give control (set to true).
     *
     * @param angle radian angle to turn critter to the right.
     */
    public void turnRight(float angle);

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
     * Render this critter. To render needs to be buffered.
     *
     * @param lightSrc list of light sources
     * @param shaderProgram shader program used for rendering
     */
    public void render(LightSources lightSrc, ShaderProgram shaderProgram);

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
    public Vector3f getPos();

    /**
     * Set observer VEC3 position
     *
     * @param pos new obsserver position
     */
    public void setPos(Vector3f pos);
}
