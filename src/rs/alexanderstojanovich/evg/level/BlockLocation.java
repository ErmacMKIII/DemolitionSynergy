/*
 * Copyright (C) 2022 Alexander Stojanovich <coas91@rocketmail.com>
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

import java.util.List;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.models.Chunk;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class BlockLocation {

    protected final TexByte[][][] locationMap = new TexByte[Chunk.BOUND][Chunk.BOUND][Chunk.BOUND];
    protected int population = 0;

    /**
     * Initialize locationMap with nulls
     */
    public void init() {
        for (int i = 0; i < Chunk.BOUND; i++) {
            for (int j = 0; j < Chunk.BOUND; j++) {
                for (int k = 0; k < Chunk.BOUND; k++) {
                    locationMap[i][j][k] = null;
                }
            }
        }
        population = 0;
    }

    /**
     * Put block locationMap into the population matrix.
     *
     * @param pos block position
     * @param texname texture of the block
     * @param bits neighbor bits (for each sides) how many block of the same
     * kind adjacent.
     * @param solid is block solid (or not)
     */
    public void putLocation(Vector3f pos, String texname, int bits, boolean solid) {
        int x = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int y = Math.round((pos.y + Chunk.BOUND) / 2.0f);
        int z = Math.round((pos.z + Chunk.BOUND) / 2.0f);

        locationMap[x][y][z] = new TexByte(texname, (byte) bits, solid);
        population++;
    }

    /**
     * Put block locationMap into the population matrix.
     *
     * @param pos block position
     * @param texByte texture of the block w/ neighbor bits
     */
    public void putLocation(Vector3f pos, TexByte texByte) {
        int x = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int y = Math.round((pos.y + Chunk.BOUND) / 2.0f);
        int z = Math.round((pos.z + Chunk.BOUND) / 2.0f);

        locationMap[x][y][z] = texByte;
        population++;
    }

    /**
     * Get Location for given position (can be null)
     *
     * @param pos where block could be locationMap
     * @return texture w/ byte of locationMap or null if does not exist
     */
    public TexByte getLocation(Vector3f pos) {
        int x = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int y = Math.round((pos.y + Chunk.BOUND) / 2.0f);
        int z = Math.round((pos.z + Chunk.BOUND) / 2.0f);

        return locationMap[x][y][z];
    }

    /**
     * Is Location for given position (must not be null) populated.
     *
     * @param pos where block could be locationMap
     *
     * @return condition on whether or not it's populated.
     */
    public boolean isLocationPopulated(Vector3f pos) {
        int x = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int y = Math.round((pos.y + Chunk.BOUND) / 2.0f);
        int z = Math.round((pos.z + Chunk.BOUND) / 2.0f);

        return locationMap[x][y][z] != null;
    }

    /**
     * Is Location for given position (must not be null) populated with certain
     * block type.
     *
     * @param pos where block could be locationMap
     * @param solid is block looked for solid (or not)
     *
     * @return condition on whether or not it's populated.
     */
    public boolean isLocationPopulated(Vector3f pos, boolean solid) {
        boolean result = false;

        int x = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int y = Math.round((pos.y + Chunk.BOUND) / 2.0f);
        int z = Math.round((pos.z + Chunk.BOUND) / 2.0f);

        TexByte value = locationMap[x][y][z];
        if (value != null && value.solid == solid) {
            result = true;
        }

        return result;
    }

    /**
     * List of populated locations.
     *
     * @return List of Vector3f of populated locationMap(s)
     */
    public List<Vector3f> getPopulatedLocation() {
        List<Vector3f> result = new GapList<>();
        for (int i = 0; i < Chunk.BOUND; i++) {
            for (int j = 0; i < Chunk.BOUND; j++) {
                for (int k = 0; k < Chunk.BOUND; k++) {
                    if (locationMap[i][j][k] != null) {
                        float x = 2.0f * i - Chunk.BOUND;
                        float y = 2.0f * j - Chunk.BOUND;
                        float z = 2.0f * k - Chunk.BOUND;

                        result.add(new Vector3f(x, y, z));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Remove locationMap where vector3f is located;
     *
     * @param pos block position
     *
     * @return was location previously populated
     */
    protected boolean removeLocation(Vector3f pos) {
        int x = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int y = Math.round((pos.y + Chunk.BOUND) / 2.0f);
        int z = Math.round((pos.z + Chunk.BOUND) / 2.0f);

        boolean populated = locationMap[x][y][z] != null;
        locationMap[x][y][z] = null;

        if (populated) {
            population--;
        }

        return populated;
    }

    public TexByte[][][] getLocationMap() {
        return locationMap;
    }

    public int getPopulation() {
        return population;
    }

}
