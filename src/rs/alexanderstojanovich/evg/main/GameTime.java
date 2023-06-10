/*
 * Copyright (C) 2023 coas9
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
package rs.alexanderstojanovich.evg.main;

import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.level.LevelContainer;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameTime {

    public static float PI = (float) Math.PI;

    public static final Vector3f Y_AXIS = new Vector3f(0.0f, 1.0f, 0.0f);
    public static final Vector3f Y_AXIS_NEG = new Vector3f(0.0f, -1.0f, 0.0f);

    protected final int hours;
    protected final int minutes;
    protected final int seconds;
    protected final float time;

    /**
     * Get current ingame time fields in HH:mm:ss (in 24 hour format).
     *
     * @return GameTime
     */
    public static GameTime Now() {
        final float sunAngleRadians = -PI / 2.0f + org.joml.Math.atan2(LevelContainer.SUN.pos.y, LevelContainer.SUN.pos.x);

        final float time = 12.0f + 24.0f * (float) org.joml.Math.toDegrees(sunAngleRadians) / 360.0f;

        return new GameTime(time);
    }

    public GameTime(float time) {
        this.time = time;
        this.hours = Math.floorMod((int) time, 24);
        this.minutes = Math.floorMod((int) (time * 60f), 60);
        this.seconds = Math.floorMod((int) (time * 3600f), 60);
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public float getTime() {
        return time;
    }

}
