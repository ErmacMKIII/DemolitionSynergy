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
package rs.alexanderstojanovich.evg.util;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class MathUtils {

    // linear interpolation for floats
    public static float lerp(float a, float b, float alpha) {
        return (1 - alpha) * a + alpha * b;
    }

    // linear interpolation for doubles
    public static double lerp(double a, double b, double alpha) {
        return (1 - alpha) * a + alpha * b;
    }
}
