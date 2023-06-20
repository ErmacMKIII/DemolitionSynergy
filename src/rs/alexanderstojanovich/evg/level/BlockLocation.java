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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.models.Block;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class BlockLocation {

    protected final TexByte[][][] locationMap = new TexByte[Chunk.BOUND][Chunk.BOUND][Chunk.BOUND];
    protected final Map<Float, List<Vector2f>> planes = new LinkedHashMap<>();

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
        planes.clear();
    }

    public boolean safeCheck(int i, int j, int k) {
        return i >= 0 && i < Chunk.BOUND
                && j >= 0 && j < Chunk.BOUND
                && k >= 0 && k < Chunk.BOUND;
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
        int i = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int j = Math.round((pos.z + Chunk.BOUND) / 2.0f);
        int k = Math.round((pos.y + Chunk.BOUND) / 2.0f);

        if (!safeCheck(i, j, k)) {
            return;
        }

        locationMap[i][j][k] = new TexByte(texname, (byte) bits, solid);

        List<Vector2f> yCoords = planes.get(pos.y);
        if (yCoords == null) {
            yCoords = new GapList<>();
        }
        yCoords.add(new Vector2f(pos.x, pos.z));
        planes.put(pos.y, yCoords);

        population++;
    }

    /**
     * Put block locationMap into the population matrix.
     *
     * @param pos block position
     * @param texByte texture of the block w/ neighbor bits
     */
    public void putLocation(Vector3f pos, TexByte texByte) {
        int i = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int j = Math.round((pos.z + Chunk.BOUND) / 2.0f);
        int k = Math.round((pos.y + Chunk.BOUND) / 2.0f);

        if (!safeCheck(i, j, k)) {
            return;
        }

        locationMap[i][j][k] = texByte;

        List<Vector2f> yCoords = planes.get(pos.y);
        if (yCoords == null) {
            yCoords = new GapList<>();
        }
        yCoords.add(new Vector2f(pos.x, pos.z));
        planes.put(pos.y, yCoords);

        population++;
    }

    /**
     * Get Location for given position (can be null)
     *
     * @param pos where block could be locationMap
     * @return texture w/ byte of locationMap or null if does not exist
     */
    public TexByte getLocation(Vector3f pos) {
        int i = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int j = Math.round((pos.z + Chunk.BOUND) / 2.0f);
        int k = Math.round((pos.y + Chunk.BOUND) / 2.0f);

        if (!safeCheck(i, j, k)) {
            return null;
        }

        return locationMap[i][j][k];
    }

    /**
     * Is Location for given position (must not be null) populated.
     *
     * @param pos where block could be locationMap
     *
     * @return condition on whether or not it's populated.
     */
    public boolean isLocationPopulated(Vector3f pos) {
        int i = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int j = Math.round((pos.z + Chunk.BOUND) / 2.0f);
        int k = Math.round((pos.y + Chunk.BOUND) / 2.0f);

        return safeCheck(i, j, k) && locationMap[i][j][k] != null;
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

        int i = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int j = Math.round((pos.z + Chunk.BOUND) / 2.0f);
        int k = Math.round((pos.y + Chunk.BOUND) / 2.0f);

        if (!safeCheck(i, j, k)) {
            return false;
        }

        TexByte value = locationMap[i][j][k];
        if (value != null && value.solid == solid) {
            result = true;
        }

        return result;
    }

    /**
     * List of populated locations. Warning: this is performance costly - So
     * don't call it in a loop!
     *
     * @return List of Vector3f of populated locationMap(s)
     */
    public IList<Vector3f> getPopulatedLocations() {
        IList<Vector3f> result = new GapList<>();

        for (int j = 0; j < Chunk.BOUND; j++) {
            float y = 2.0f * j - Chunk.BOUND;
            for (Vector2f xz : planes.get(y)) {
                result.add(new Vector3f(xz.x, y, xz.y));
            }
        }

        return result;
    }

    /**
     * List of populated locations with given predicated. Warning: this is
     * performance costly - So don't call it in a loop!
     *
     * @param predicate predicate
     * @return List of Vector3f of populated locationMap(s)
     */
    public IList<Vector3f> getPopulatedLocations(Predicate<TexByte> predicate) {
        IList<Vector3f> result = new GapList<>();

        for (int j = 0; j < Chunk.BOUND; j++) {
            float y = 2.0f * j - Chunk.BOUND;
            for (Vector2f xz : planes.get(y)) {
                int i = Math.round((xz.x + Chunk.BOUND) / 2.0f);
                int k = Math.round((xz.y + Chunk.BOUND) / 2.0f);
                TexByte value = locationMap[i][j][k];

                if (!safeCheck(i, j, k)) {
                    continue;
                }

                if (value != null && predicate.test(value)) {
                    result.add(new Vector3f(xz.x, y, xz.y));
                }
            }
        }

        return result;
    }

    /**
     * List of populated locations with given predicated. Warning: this is
     * performance costly - So don't call it in a loop!
     *
     * @param chunkId chunkId to check (block) population.
     *
     * @param predicate predicate
     * @return List of Vector3f of populated locationMap(s)
     */
    public IList<Vector3f> getPopulatedLocations(int chunkId, Predicate<TexByte> predicate) {
        IList<Vector3f> result = new GapList<>();

        float lYBound = -Chunk.BOUND + 2.0f;
        float rYBound = Chunk.BOUND - 2.0f;

        for (float y = lYBound; y <= rYBound; y += 2.0f) {
            List<Vector2f> yCoords = planes.get((float) y);
            if (yCoords != null) {
                for (Vector2f xz : yCoords) {
                    int i = Math.round((xz.x + Chunk.BOUND) / 2.0f);
                    int j = Math.round((y + Chunk.BOUND) / 2.0f);
                    int k = Math.round((xz.y + Chunk.BOUND) / 2.0f);

                    if (!safeCheck(i, j, k)) {
                        continue;
                    }

                    TexByte value = locationMap[i][j][k];
                    Vector3f pos = new Vector3f(xz.x, y, xz.y);
                    if (value != null && predicate.test(value) && Chunk.chunkFunc(pos) == chunkId) {
                        result.add(pos);
                    }
                }
            }
        }

        return result;
    }

    /**
     * List of populated locations with given predicated. Warning: this is
     * performance costly - So don't call it in a loop!
     *
     * @param chunkIdList chunkId list to check (block) population.
     *
     * @param predicate predicate
     * @return List of Vector3f of populated locationMap(s)
     */
    public IList<Vector3f> getPopulatedLocations(IList<Integer> chunkIdList, Predicate<TexByte> predicate) {
        IList<Vector3f> result = new GapList<>();
        for (int chunkId : chunkIdList) {
            float lYBound = -Chunk.BOUND + 2.0f;
            float rYBound = Chunk.BOUND - 2.0f;

            for (float y = lYBound; y <= rYBound; y += 2.0f) {
                List<Vector2f> yCoords = planes.get((float) y);
                if (yCoords != null) {
                    for (Vector2f xz : yCoords) {
                        int i = Math.round((xz.x + Chunk.BOUND) / 2.0f);
                        int j = Math.round((y + Chunk.BOUND) / 2.0f);
                        int k = Math.round((xz.y + Chunk.BOUND) / 2.0f);

                        if (!safeCheck(i, j, k)) {
                            continue;
                        }

                        TexByte value = locationMap[i][j][k];
                        Vector3f pos = new Vector3f(xz.x, y, xz.y);
                        if (value != null && predicate.test(value) && Chunk.chunkFunc(pos) == chunkId) {
                            result.add(pos);
                        }
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
        int i = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int j = Math.round((pos.z + Chunk.BOUND) / 2.0f);
        int k = Math.round((pos.y + Chunk.BOUND) / 2.0f);

        if (!safeCheck(i, j, k)) {
            return false;
        }

        boolean populated = locationMap[i][j][k] != null;
        locationMap[i][j][k] = null;

        if (populated) {
            population--;
        }

        List<Vector2f> yCoords = planes.get((float) pos.y);
        if (yCoords != null) {
            yCoords.remove(new Vector2f(pos.x, pos.z));
            if (yCoords.isEmpty()) {
                planes.remove(pos.y);
            }
        }

        return populated;
    }

    /**
     * Get Location Map of all the blocks
     *
     * @return location map [i,j,k] => (x,z,y)
     */
    public TexByte[][][] getLocationMap() {
        return locationMap;
    }

    /**
     * Population (number of blocks).
     *
     * @return Block number - population.
     */
    public int getPopulation() {
        return population;
    }

    /**
     * Get XZ PLanes based on Y
     *
     * @return XZ Planes Where one Y maps into several XZ
     */
    public Map<Float, List<Vector2f>> getPlanes() {
        return planes;
    }

    // used in static Level container to get compressed positioned sets    
    public int getNeighborSolidBits(Vector3f pos) {
        int bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) { // j - face number
            Vector3f adjPos = Block.getAdjacentPos(pos, j);
            TexByte location = this.getLocation(adjPos);
            if (location != null && location.isSolid()) {
                int mask = 1 << j;
                bits |= mask;
            }
        }
        return bits;
    }

    // used in static Level container to get compressed positioned sets    
    public int getNeighborFluidBits(Vector3f pos) {
        int bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) { // j - face number
            Vector3f adjPos = Block.getAdjacentPos(pos, j);
            TexByte location = this.getLocation(adjPos);
            if (location != null && !location.isSolid()) {
                int mask = 1 << j;
                bits |= mask;
            }
        }
        return bits;
    }

}
