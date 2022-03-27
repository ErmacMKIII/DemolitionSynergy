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

import java.util.List;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 * Critter is interface of living things. Critters are considered no model
 * Observer and Model Critters. Model Critter is Player and NPCs. Only player
 * has camera.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface Critter { // interface for Observer, Player and NPC

    /**
     * Move this critter in front direction for amount. Also to move critter has
     * to have give control (set to true).
     *
     * @param amount amount to move this critter
     */
    public void moveForward(float amount);

    /**
     * Move this critter in back direction for amount. Also to move critter has
     * to have give control (set to true).
     *
     * @param amount amount to move this critter
     */
    public void moveBackward(float amount);

    /**
     * Move this critter in left direction for amount. Also to move critter has
     * to have give control (set to true).
     *
     * @param amount amount to move this critter
     */
    public void moveLeft(float amount);

    /**
     * Move this critter in right direction for amount. Also to move critter has
     * to have give control (set to true).
     *
     * @param amount amount to move this critter
     */
    public void moveRight(float amount);

    //--------------------------------------------------------------------------
    /**
     * Move this critter prediction forward (but critter stays). Used for
     * collision testing.
     *
     * @param amount amount to move this critter predictor
     */
    public void movePredictorForward(float amount);

    /**
     * Move this critter prediction backward (but critter stays). Used for
     * collision testing.
     *
     * @param amount amount to move this critter predictor
     */
    public void movePredictorBackward(float amount);

    /**
     * Move this critter prediction left (but critter stays). Used for collision
     * testing.
     *
     * @param amount amount to move this critter predictor
     */
    public void movePredictorLeft(float amount);

    /**
     * Move this critter prediction right (but critter stays). Used for
     * collision testing.
     *
     * @param amount amount to move this critter predictor
     */
    public void movePredictorRight(float amount);

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
    public void render(List<Vector3f> lightSrc, ShaderProgram shaderProgram);

    //--------------------------------------------------------------------------
    /**
     * Checks if rendering is allowed (buffered into the memory).
     *
     * @return buffered flag.
     */
    public boolean isBuffered();

    /**
     * Buffer this critter, allowing it to be rendered.
     */
    public void bufferAll();

    //--------------------------------------------------------------------------
    @Override
    public String toString();

    /**
     * Is it given control to perform movement or turning.
     *
     * @return given control flag
     */
    public boolean isGivenControl();

    /**
     * Set given control flag to perform movement or turning.
     *
     * @param givenControl desired given control flag (true - allows, false -
     * disallows)
     */
    public void setGivenControl(boolean givenControl);

    /**
     * Gets predictor used for collision testing.
     *
     * @return critter predictor
     */
    public Vector3f getPredictor();

}
