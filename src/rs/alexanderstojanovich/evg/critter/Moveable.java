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
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface Moveable {

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

    /**
     * Move camera up (towards positive Y-axis)
     *
     * @param amount
     */
    public void ascend(float amount);

    /**
     * Move camera down (towards negative Y-axis)
     *
     * @param amount
     */
    public void descend(float amount);

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

    /*
    *Get position.
     */
    public Vector3f getPos();

    /**
     * Assign position to one (moveable)
     *
     * @param pos new position
     */
    public void setPos(Vector3f pos);

    //--------------------------------------------------------------------------
    /**
     * Move camera forward (towards positive Z-axis) on XZ plane.
     *
     * @param amount amount added forward
     */
    public void moveXZForward(float amount);

    /**
     * Move camera backward (towards negative Z-axis) on XZ plane.
     *
     * @param amount amount subtracted backward
     */
    public void moveXZBackward(float amount);

    /**
     * Move camera left (towards negative X-axis) on XZ plane..
     *
     * @param amount to move left.
     */
    public void moveXZLeft(float amount);

    /**
     * Move camera left (towards positive X-axis) on XZ plane.
     *
     * @param amount to move right.
     */
    public void moveXZRight(float amount);

    /**
     * Move camera up (towards positive Y-axis) separated from XZ plane.
     *
     * @param amount
     */
    public void jumpY(float amount);

    /**
     * Move camera down (towards negative Y-axis) separated from XZ plane.
     *
     * @param amount
     */
    public void dropY(float amount);

}
