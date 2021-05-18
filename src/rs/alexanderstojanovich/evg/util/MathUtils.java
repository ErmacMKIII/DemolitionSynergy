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

import org.joml.SimplexNoise;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class MathUtils {

    // linear interpolation for floats
    public static float lerp(float a, float b, float alpha) {
        return (1.0f - alpha) * a + alpha * b;
    }

    // linear interpolation for doubles
    public static double lerp(double a, double b, double alpha) {
        return (1.0 - alpha) * a + alpha * b;
    }

    // Taylor series approximation
    public static float expm1(float x) {
        return x * (1.0f + 0.5f * x * (1.0f + x * (1.0f + 0.25f * x) / 3.0f));
    }

    // Taylor series approximation
    public static double expm1(double x) {
        return x * (1.0 + 0.5 * x * (1.0 + x * (1.0 + 0.25 * x) / 3.0));
    }

    /**
     * Generate noise using given number of iterations (or octaves)
     *
     * @param numOfOctaves iterations num
     * @param x x-coord
     * @param y y-coord
     * @param persistence amplitude multiplier
     * @param scale scale
     * @param low minimum output
     * @param high maximum output
     * @param lacunarity frequency multiplier
     * @return noise
     */
    public static float noise(int numOfOctaves, float x, float y, float persistence, float scale, float low, float high, float lacunarity) {
        float maxAmp = 0.0f;
        float amp = 1.0f;
        float freq = scale;
        float noise = 0.0f;

        // add successively smaller, higher-frequency terms
        for (int i = 0; i < numOfOctaves; i++) {
            noise += SimplexNoise.noise(x * freq, y * freq) * amp;
            maxAmp += amp;
            amp *= persistence;
            freq *= lacunarity;
        }
        // take the average value of the iterations
        noise /= maxAmp;

        // normalize the result
        noise = noise * (high - low) / 2.0f + (high + low) / 2.0f;

        return noise;
    }

}
