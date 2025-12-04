/* 
 * Copyright (C) 2020 Aleksandar Stojanovic <coas91@rocketmail.com>
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
 * Humble Math Utilities. Towards Fast Approximation. Taken from various
 * sources.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class MathUtils {

    public static final float PI = (float) org.joml.Math.PI;

    public static float smoothstep(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    public static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    // linear interpolation for floats
    public static float lerp(float a, float b, float alpha) {
        return (1.0f - alpha) * a + alpha * b;
    }

    /**
     * Calculate linear interpolation for two doubles
     *
     * @param a first argument
     * @param b second argument
     * @param alpha
     * @return
     */
    public static double lerp(double a, double b, double alpha) {
        return (1.0 - alpha) * a + alpha * b;
    }

    /**
     * C macro that does a good job at exploiting the IEEE 754 floating-point
     * representation to calculate e^x. Source:
     * https://martin.ankerl.com/2007/02/11/optimized-exponential-functions-for-java/
     *
     * @param x argument
     * @return e^x.
     */
    public static double exp(double x) {
        final long tmp = (long) (org.joml.Math.fma(1512775, x, 1072632447));
        return Double.longBitsToDouble(tmp << 32);
    }

    /**
     * Approximation of natural logarithm. Approximation is not very good. Gets
     * worse the larger the values are. Source:
     * https://martin.ankerl.com/2007/02/11/optimized-exponential-functions-for-java/
     *
     * @param x argument
     * @return log(x) for base e.
     */
    public static double log(double x) {
        return 6 * (x - 1) / (x + 1 + 4 * (org.joml.Math.sqrt(x)));
    }

    /**
     * Return approximate value of a^b. Source:
     * https://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/
     *
     * @param a base
     * @param b exponent
     * @return a^b
     */
    public static double pow(double a, double b) {
        final int x = (int) (Double.doubleToLongBits(a) >> 32);
        final int y = (int) (org.joml.Math.fma(b, x - 1072632447, 1072632447));
        return Double.longBitsToDouble(((long) y) << 32);
    }

    /**
     * Calculate linear polynomial with given constants.
     *
     * @param linear
     * @param cnst
     * @param x
     * @return polynomial value
     */
    public static float polynomial(float linear, float cnst, float x) {
        return (linear * x + cnst);
    }

    /**
     * Calculate quadratic polynomial with given constants.
     *
     * @param quad
     * @param linear
     * @param cnst
     * @param x
     * @return polynomial value
     */
    public static float polynomial(float quad, float linear, float cnst, float x) {
        return (quad * x * x + linear * x + cnst);
    }

    /**
     * Calculate cubic polynomial with given constants.
     *
     * @param cubic
     * @param quad
     * @param linear
     * @param cnst
     * @param x
     * @return polynomial value
     */
    public static float polynomial(float cubic, float quad, float linear, float cnst, float x) {
        return (cubic * x * x * x + quad * x * x + linear * x + cnst);
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
    public static float noise2(int numOfOctaves, float x, float y, float persistence, float scale, float low, float high, float lacunarity) {
        float amp = 1.0f;
        float freq = scale;
        float noise = 0.0f;

        // add successively smaller, higher-frequency terms
        for (int i = 0; i < numOfOctaves; i++) {
            noise = Math.fma(SimplexNoise.noise(x * freq, y * freq), amp, noise);
            amp *= persistence;
            freq *= lacunarity;
        }

        // put outside the loop
        float maxAmp = (1.0f - (float) MathUtils.pow(persistence, numOfOctaves)) / (1.0f - persistence);

        // take the average value of the iterations
        noise /= maxAmp;

        // normalize the result
        noise = Math.fma(noise, high - low, high + low) / 2.0f;

        return noise;
    }

    /**
     * Generate noise using given number of iterations (or octaves)
     *
     * @param numOfOctaves iterations num
     * @param x x-coord
     * @param y y-coord
     * @param persistence amplitude multiplier
     * @param low minimum output
     * @param high maximum output
     * @param frequencies noise args
     * @param amplitudes noise amplitude multiplier
     * @return noise
     */
    public static float noise2(int numOfOctaves, float x, float y, float persistence, float low, float high, float[] frequencies, float[] amplitudes) {
        float noise = 0.0f;

        // add successively smaller, higher-frequency terms
        for (int i = 0; i < numOfOctaves; i++) {
            noise = Math.fma(SimplexNoise.noise(x * frequencies[i], y * frequencies[i]), amplitudes[i], noise);
        }

        // put outside the loop
        float maxAmp = (1.0f - (float) MathUtils.pow(persistence, numOfOctaves)) / (1.0f - persistence);

        // take the average value of the iterations
        noise /= maxAmp;

        // normalize the result
        noise = Math.fma(noise, high - low, high + low) / 2.0f;

        return noise;
    }

    /**
     * Generate noise using given number of iterations (or octaves)
     *
     * @param numOfOctaves iterations num
     * @param x x-coord
     * @param y y-coord
     * @param z y-coord
     * @param persistence amplitude multiplier
     * @param scale scale
     * @param low minimum output
     * @param high maximum output
     * @param lacunarity frequency multiplier
     * @return noise
     */
    public static float noise3(int numOfOctaves, float x, float y, float z, float persistence, float scale, float low, float high, float lacunarity) {
        float amp = 1.0f;
        float freq = scale;
        float noise = 0.0f;

        // add successively smaller, higher-frequency terms
        for (int i = 0; i < numOfOctaves; i++) {
            noise = Math.fma(SimplexNoise.noise(x * freq, y * freq, z * freq), amp, noise);
            amp *= persistence;
            freq *= lacunarity;
        }

        // put outside the loop
        float maxAmp = (1.0f - (float) MathUtils.pow(persistence, numOfOctaves)) / (1.0f - persistence);

        // take the average value of the iterations
        noise /= maxAmp;

        // normalize the result
        noise = Math.fma(noise, high - low, high + low) / 2.0f;

        return noise;
    }

    /**
     * Generate noise using given number of iterations (or octaves)
     *
     * @param numOfOctaves iterations num
     * @param x x-coord
     * @param y y-coord
     * @param z y-coord
     * @param persistence amplitude multiplier
     * @param low minimum output
     * @param high maximum output
     * @param frequencies noise args
     * @param amplitudes noise amplitude multiplier
     * @return noise
     */
    public static float noise3(int numOfOctaves, float x, float y, float z, float persistence, float low, float high, float[] frequencies, float[] amplitudes) {
        float noise = 0.0f;

        // add successively smaller, higher-frequency terms
        for (int i = 0; i < numOfOctaves; i++) {
            noise = Math.fma(SimplexNoise.noise(x * frequencies[i], y * frequencies[i], z * frequencies[i]), amplitudes[i], noise);
        }

        // put outside the loop
        float maxAmp = (1.0f - (float) MathUtils.pow(persistence, numOfOctaves)) / (1.0f - persistence);

        // take the average value of the iterations
        noise /= maxAmp;

        // normalize the result
        noise = Math.fma(noise, high - low, high + low) / 2.0f;

        return noise;
    }

    /**
     * Arc-cosine (cos^-1) from radian angle x Error value of +/- 15 degrees.
     *
     * @param x in [-1, 1]
     * @return angle [0, Pi]
     */
    public static float acos(float x) {
        float negate = Math.signum(x);
        x = Math.abs(x);
        float ret = -0.0187293f;
        ret = ret * x;
        ret = ret + 0.0742610f;
        ret = ret * x;
        ret = ret - 0.2121144f;
        ret = ret * x;
        ret = ret + 1.5707288f;
        ret = (float) (ret * org.joml.Math.sqrt(1.0f - x));
        ret = ret - 2.0f * negate * ret;
        return negate * 3.14159265358979f + ret;
    }

    /**
     * Compute fast inverse square root (Quake 3 engine).
     *
     * @param x float argument
     * @return approximation of inverse square root.
     */
    public static float invSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
//        x *= (1.5f - xhalf * x * x);
        x *= org.joml.Math.fma(-xhalf, x * x, 1.5);

        return x;
    }

    /**
     * Compute fast inverse square root (Quake 3 engine).
     *
     * @param x double argument
     * @return approximation of inverse square root.
     */
    public static double invSqrt(double x) {
        double xhalf = 0.5d * x;
        long i = Double.doubleToLongBits(x);
        i = 0x5fe6ec85e7de30daL - (i >> 1);
        x = Double.longBitsToDouble(i);
//        x *= (1.5d - xhalf * x * x);
        x *= org.joml.Math.fma(-xhalf, x * x, 1.5d);

        return x;
    }

    /**
     * Convert angle to degrees from radians
     *
     * @param angleRadians angle in radians
     * @return angle degrees
     */
    public static float toDegrees(float angleRadians) {
        return 180.0f * angleRadians / PI;
    }

}
