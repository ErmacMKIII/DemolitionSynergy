/*
 * Copyright (C) 2023 coas91@rocketmail.com
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

/**
 * Everything that implements this interface is collision predictable.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface Predictable {

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

    /**
     * Move this critter prediction up (ascending, but critter stays)
     *
     * @param amount amount to move this critter predictor
     */
    public void movePredictorUp(float amount);

    /**
     * Move this critter prediction up (ascending, but critter stays)
     *
     * @param amount amount to move this critter predictor
     */
    public void movePredictorDown(float amount);

    /**
     * Move this critter prediction forward (but critter stays). Used for
     * collision testing. On XZ plane..
     *
     * @param amount amount to move this critter predictor
     */
    public void movePredictorXZForward(float amount);

    /**
     * Move this critter prediction backward (but critter stays). Used for
     * collision testing. On XZ plane..
     *
     * @param amount amount to move this critter predictor
     */
    public void movePredictorXZBackward(float amount);

    /**
     * Move this critter prediction left (but critter stays). Used for collision
     * testing. On XZ plane..
     *
     * @param amount amount to move this critter predictor
     */
    public void movePredictorXZLeft(float amount);

    /**
     * Move this critter prediction right (but critter stays). Used for
     * collision testing. On XZ plane..
     *
     * @param amount amount to move this critter predictor
     */
    public void movePredictorXZRight(float amount);

    /**
     * Move this critter prediction up (ascending, but critter stays). Separated
     * from XZ plane.
     *
     * @param amount amount to move this critter predictor
     */
    public void movePredictorYUp(float amount);

    /**
     * Move this critter prediction up (ascending, but critter stays). Separated
     * from XZ plane.
     *
     * @param amount amount to move this critter predictor
     */
    public void movePredictorYDown(float amount);

    /**
     * Get collision prediction vector (predictor)
     *
     * @return VEC3 Prediction vector
     */
    public Vector3f getPredictor();

    /**
     * Set collision prediction vector (predictor)
     *
     * @param pos new predictor vector
     */
    public void setPredictor(Vector3f pos);

}
