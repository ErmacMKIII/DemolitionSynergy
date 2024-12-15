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
package rs.alexanderstojanovich.evg.level;

import rs.alexanderstojanovich.evg.critter.Critter;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface GravityEnviroment {

    public static final float WATER_DENSITY = 5600f;
    public static final float GRAVITY_CONSTANT = 9.8f; // apply only if Game.upsTicks >= 1.0
    public static final float TERMINAL_VELOCITY = 100f; // apply only if Game.upsTicks >= 1.0

    /**
     * Affect Environment with gravity. Object not supported from bottom will
     * fall. Critter not supported from bottom will fall.
     *
     * @param critter critter affected by gravity
     * @param deltaTime delta time
     * @return did gravity affect
     */
    public boolean gravityDo(Critter critter, float deltaTime);

    /**
     * Affect Environment with jump against gravity. Assuming that critter will
     * not hit ceilings.
     *
     * @param critter level actor critter (player or npc)
     * @param amountY amount Y-axis multiplier (velocity)
     * @param deltaTime delta time
     * @return did critter jump
     */
    public boolean jump(Critter critter, float amountY, float deltaTime);

    /**
     * Makes the player push downwards, pressuring the bottom surface (or air),
     * towards Y-axis negative.
     *
     * @param critter The player.
     * @param amountYNeg The amount of upward movement.
     * @param deltaTime The time elapsed since the last handleInput.
     * @return did critter crouch
     */
    public boolean crouch(Critter critter, float amountYNeg, float deltaTime);

    /**
     * Get falling velocity
     *
     * @return falling velocity
     */
    public float getFallVelocity();

    /**
     * Is gravity on (manually could be turned off when player spawning)
     *
     * @return is gravity on
     */
    public boolean isGravityOn();
}
