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

import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class VectorFloatUtils {

    /**
     * Convert VEC3 to byte array.
     *
     * @param vector float3(x,y,z) vector
     * @return byte array of that vector
     */
    public static byte[] vec3fToByteArray(Vector3f vector) {
        byte[] buffer = new byte[12];
        int x = Float.floatToIntBits(vector.x);
        buffer[0] = (byte) (x);
        buffer[1] = (byte) (x >> 8);
        buffer[2] = (byte) (x >> 16);
        buffer[3] = (byte) (x >> 24);

        int y = Float.floatToIntBits(vector.y);
        buffer[4] = (byte) (y);
        buffer[5] = (byte) (y >> 8);
        buffer[6] = (byte) (y >> 16);
        buffer[7] = (byte) (y >> 24);

        int z = Float.floatToIntBits(vector.z);
        buffer[8] = (byte) (z);
        buffer[9] = (byte) (z >> 8);
        buffer[10] = (byte) (z >> 16);
        buffer[11] = (byte) (z >> 24);

        return buffer;
    }

    /**
     * Convert VEC3 to byte array.
     *
     * @param vector float3(x,y,z) vector
     * @param outBuffer array where to put vector
     * @param pos write position
     */
    public static void vec3fToByteArray(Vector3f vector, byte[] outBuffer, int pos) {
        int x = Float.floatToIntBits(vector.x);
        outBuffer[pos] = (byte) (x);
        outBuffer[pos + 1] = (byte) (x >> 8);
        outBuffer[pos + 2] = (byte) (x >> 16);
        outBuffer[pos + 3] = (byte) (x >> 24);

        int y = Float.floatToIntBits(vector.y);
        outBuffer[pos + 4] = (byte) (y);
        outBuffer[pos + 5] = (byte) (y >> 8);
        outBuffer[pos + 6] = (byte) (y >> 16);
        outBuffer[pos + 7] = (byte) (y >> 24);

        int z = Float.floatToIntBits(vector.z);
        outBuffer[pos + 8] = (byte) (z);
        outBuffer[pos + 9] = (byte) (z >> 8);
        outBuffer[pos + 10] = (byte) (z >> 16);
        outBuffer[pos + 11] = (byte) (z >> 24);
    }

    /**
     * Convert VEC4 to byte array.
     *
     * @param vector float4(x,y,z,w) vector
     * @return byte array of that vector
     */
    public static byte[] vec4fToByteArray(Vector4f vector) {
        byte[] buffer = new byte[16];
        int x = Float.floatToIntBits(vector.x);
        buffer[0] = (byte) (x);
        buffer[1] = (byte) (x >> 8);
        buffer[2] = (byte) (x >> 16);
        buffer[3] = (byte) (x >> 24);

        int y = Float.floatToIntBits(vector.y);
        buffer[4] = (byte) (y);
        buffer[5] = (byte) (y >> 8);
        buffer[6] = (byte) (y >> 16);
        buffer[7] = (byte) (y >> 24);

        int z = Float.floatToIntBits(vector.z);
        buffer[8] = (byte) (z);
        buffer[9] = (byte) (z >> 8);
        buffer[10] = (byte) (z >> 16);
        buffer[11] = (byte) (z >> 24);

        int w = Float.floatToIntBits(vector.w);
        buffer[12] = (byte) (w);
        buffer[13] = (byte) (w >> 8);
        buffer[14] = (byte) (w >> 16);
        buffer[15] = (byte) (w >> 24);

        return buffer;
    }

    /**
     * Convert VEC4 to byte array.
     *
     * @param vector float4(x,y,z,w) vector
     * @param outBuffer array where to put float4 vector
     * @param pos write position
     * @return byte array of that vector
     */
    public static byte[] vec4fToByteArray(Vector4f vector, byte[] outBuffer, int pos) {
        int x = Float.floatToIntBits(vector.x);
        outBuffer[pos + 0] = (byte) (x);
        outBuffer[pos + 1] = (byte) (x >> 8);
        outBuffer[pos + 2] = (byte) (x >> 16);
        outBuffer[pos + 3] = (byte) (x >> 24);

        int y = Float.floatToIntBits(vector.y);
        outBuffer[pos + 4] = (byte) (y);
        outBuffer[pos + 5] = (byte) (y >> 8);
        outBuffer[pos + 6] = (byte) (y >> 16);
        outBuffer[pos + 7] = (byte) (y >> 24);

        int z = Float.floatToIntBits(vector.z);
        outBuffer[pos + 8] = (byte) (z);
        outBuffer[pos + 9] = (byte) (z >> 8);
        outBuffer[pos + 10] = (byte) (z >> 16);
        outBuffer[pos + 11] = (byte) (z >> 24);

        int w = Float.floatToIntBits(vector.w);
        outBuffer[pos + 12] = (byte) (w);
        outBuffer[pos + 13] = (byte) (w >> 8);
        outBuffer[pos + 14] = (byte) (w >> 16);
        outBuffer[pos + 15] = (byte) (w >> 24);

        return outBuffer;
    }

    /**
     * Convert VEC3 from byte array.
     *
     * @param array byte array of that vector;
     * @return float3(x,y,z) vector
     */
    public static Vector3f vec3fFromByteArray(byte[] array) {
        int valx = (array[3] & 0xFF) << 24 | (array[2] & 0xFF) << 16 | (array[1] & 0xFF) << 8 | (array[0] & 0xFF);
        float x = Float.intBitsToFloat(valx);

        int valy = (array[7] & 0xFF) << 24 | (array[6] & 0xFF) << 16 | (array[5] & 0xFF) << 8 | (array[4] & 0xFF);
        float y = Float.intBitsToFloat(valy);

        int valz = (array[11] & 0xFF) << 24 | (array[10] & 0xFF) << 16 | (array[9] & 0xFF) << 8 | (array[8] & 0xFF);
        float z = Float.intBitsToFloat(valz);

        return new Vector3f(x, y, z);
    }

    /**
     * Convert VEC3 from byte array.
     *
     * @param array byte array of that vector;
     * @param pos read position
     * @return float3(x,y,z) vector
     */
    public static Vector3f vec3fFromByteArray(byte[] array, int pos) {
        int valx = (array[pos + 3] & 0xFF) << 24 | (array[pos + 2] & 0xFF) << 16 | (array[pos + 1] & 0xFF) << 8 | (array[pos + 0] & 0xFF);
        float x = Float.intBitsToFloat(valx);

        int valy = (array[pos + 7] & 0xFF) << 24 | (array[pos + 6] & 0xFF) << 16 | (array[pos + 5] & 0xFF) << 8 | (array[pos + 4] & 0xFF);
        float y = Float.intBitsToFloat(valy);

        int valz = (array[pos + 11] & 0xFF) << 24 | (array[pos + 10] & 0xFF) << 16 | (array[pos + 9] & 0xFF) << 8 | (array[pos + 8] & 0xFF);
        float z = Float.intBitsToFloat(valz);

        return new Vector3f(x, y, z);
    }

    /**
     * Convert VEC4 from byte array.
     *
     * @param array byte array of that vector;
     * @return float4(x,y,z,w) vector
     */
    public static Vector4f vec4fFromByteArray(byte[] array) {
        int valx = (array[3] & 0xFF) << 24 | (array[2] & 0xFF) << 16 | (array[1] & 0xFF) << 8 | (array[0] & 0xFF);
        float x = Float.intBitsToFloat(valx);

        int valy = (array[7] & 0xFF) << 24 | (array[6] & 0xFF) << 16 | (array[5] & 0xFF) << 8 | (array[4] & 0xFF);
        float y = Float.intBitsToFloat(valy);

        int valz = (array[11] & 0xFF) << 24 | (array[10] & 0xFF) << 16 | (array[9] & 0xFF) << 8 | (array[8] & 0xFF);
        float z = Float.intBitsToFloat(valz);

        int valw = (array[15] & 0xFF) << 24 | (array[14] & 0xFF) << 16 | (array[13] & 0xFF) << 8 | (array[12] & 0xFF);
        float w = Float.intBitsToFloat(valw);

        return new Vector4f(x, y, z, w);
    }

    /**
     * Convert VEC4 from byte array.
     *
     * @param array byte array of that vector;
     * @param pos read position
     * @return float4(x,y,z,w) vector
     */
    public static Vector4f vec4fFromByteArray(byte[] array, int pos) {
        int valx = (array[pos + 3] & 0xFF) << 24 | (array[pos + 2] & 0xFF) << 16 | (array[pos + 1] & 0xFF) << 8 | (array[pos + 0] & 0xFF);
        float x = Float.intBitsToFloat(valx);

        int valy = (array[pos + 7] & 0xFF) << 24 | (array[pos + 6] & 0xFF) << 16 | (array[pos + 5] & 0xFF) << 8 | (array[pos + 4] & 0xFF);
        float y = Float.intBitsToFloat(valy);

        int valz = (array[pos + 11] & 0xFF) << 24 | (array[pos + 10] & 0xFF) << 16 | (array[pos + 9] & 0xFF) << 8 | (array[pos + 8] & 0xFF);
        float z = Float.intBitsToFloat(valz);

        int valw = (array[pos + 15] & 0xFF) << 24 | (array[pos + 14] & 0xFF) << 16 | (array[pos + 13] & 0xFF) << 8 | (array[pos + 12] & 0xFF);
        float w = Float.intBitsToFloat(valw);

        return new Vector4f(x, y, z, w);
    }

}
