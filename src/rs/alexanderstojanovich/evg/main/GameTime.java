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

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class GameTime {

    protected static final Configuration cfg = Configuration.getInstance();
    public static double PI = (double) org.joml.Math.PI;

    public static final Vector3f Y_AXIS = new Vector3f(0.0f, 1.0f, 0.0f);
    public static final Vector3f Y_AXIS_NEG = new Vector3f(0.0f, -1.0f, 0.0f);

    protected final int hours;
    protected final int minutes;
    protected final int seconds;
    protected final double time;
    protected int days = 0;

    /**
     * Get current ingame time fields in HH:mm:ss (in 24 hour format).
     *
     * @return GameTime
     */
    public static GameTime Now() {
        final double gtm = cfg.getGameTimeMultiplier();
        final double dt = gtm * (double) (Game.gameTicks * Game.TICK_TIME / 16.0);
        final int days = (int) (1.0f + dt / (2.0f * PI));

        final double sin = org.joml.Math.sin(dt);
        final double cos = org.joml.Math.cosFromSin(sin, dt);

        double x = cos - sin;
        double y = sin + cos;

        final double internalAngle = org.joml.Math.atan2(y, x); // replacing sun angle with internal angle
        final double time = 21.0f + 24.0f * (double) org.joml.Math.toDegrees(internalAngle) / 360.0f;

        return new GameTime(days, time);
    }

    public GameTime(int days, double time) {
        this.days = days;
        this.time = time;

        this.hours = Math.floorMod((int) time, 24);
        this.minutes = Math.floorMod((int) (time * 60f), 60);
        this.seconds = Math.floorMod((int) (time * 3600f), 60);
    }

    public Configuration getCfg() {
        return cfg;
    }

    /**
     * Get Elapsed days since the start of the game.
     *
     * @return ingame time days
     */
    public int getDays() {
        return days;
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

    public double getTime() {
        return time;
    }

}
