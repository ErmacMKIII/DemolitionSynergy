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
package rs.alexanderstojanovich.evg.location;

import java.util.List;
import java.util.function.Predicate;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import org.magicwerk.brownies.collections.Key1List;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.models.Block;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class BlockLocation {

    protected final TexByte[][][] locationMap = new TexByte[Chunk.BOUND][Chunk.BOUND][Chunk.BOUND];
    public final Key1List<Vector3f, Float> locations = new Key1List.Builder<Vector3f, Float>()
            .withListBig(true)
            .withKey1Map(Vector3f::y).withKey1Duplicates(true)
            .build();

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
        locations.clear();
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
     * @param blkId unique block id (property of block)
     */
    public void putLocation(Vector3f pos, String texname, int bits, boolean solid, int blkId) {
        int i = Math.round((pos.x + Chunk.BOUND) / 2.0f);
        int j = Math.round((pos.z + Chunk.BOUND) / 2.0f);
        int k = Math.round((pos.y + Chunk.BOUND) / 2.0f);

        if (!safeCheck(i, j, k)) {
            return;
        }

        locationMap[i][j][k] = new TexByte(texname, (byte) bits, solid, blkId);
        locations.add(new Vector3f(pos));

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

        locations.add(new Vector3f(pos));

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
        return locations;
    }

    /**
     * List of populated locations for given y-coordinate. Warning: this is
     * performance costly - So don't call it in a loop!
     *
     * @param y
     * @return List of Vector3f of populated locationMap(s)
     */
    public IList<Vector3f> getPopulatedLocations(float y) {
        return locations.getAllByKey1(y);
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
            List<Vector3f> xyzCoords = locations.getAllByKey1((float) y);
            if (xyzCoords != null) {
                for (Vector3f xyz : xyzCoords) {
                    int i = Math.round((xyz.x + Chunk.BOUND) / 2.0f);
                    int k = Math.round((xyz.z + Chunk.BOUND) / 2.0f);
                    TexByte value = locationMap[i][j][k];

                    if (!safeCheck(i, j, k)) {
                        continue;
                    }

                    if (value != null && predicate.test(value)) {
                        result.add(new Vector3f(xyz.x, xyz.y, xyz.z));
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
     * @param predicate predicate
     * @param y desired y-coord
     * @return List of Vector3f of populated locationMap(s)
     */
    public IList<Vector3f> getPopulatedLocations(Predicate<TexByte> predicate, float y) {
        IList<Vector3f> result = new GapList<>();

        List<Vector3f> xyzCoords = locations.getAllByKey1((float) y);
        if (xyzCoords != null) {
            for (Vector3f xyz : xyzCoords) {
                TexByte value = getLocation(xyz);

                if (value != null && predicate.test(value)) {
                    result.add(new Vector3f(xyz.x, xyz.y, xyz.z));
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
            List<Vector3f> xyzCoords = locations.getAllByKey1((float) y);
            if (xyzCoords != null) {
                for (Vector3f xyz : xyzCoords) {
                    int i = Math.round((xyz.x + Chunk.BOUND) / 2.0f);
                    int j = Math.round((xyz.y + Chunk.BOUND) / 2.0f);
                    int k = Math.round((xyz.z + Chunk.BOUND) / 2.0f);

                    if (!safeCheck(i, j, k)) {
                        continue;
                    }

                    TexByte value = locationMap[i][j][k];
                    Vector3f pos = new Vector3f(xyz.x, xyz.y, xyz.z);
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
     * @param chunkId chunkId to check (block) population.
     *
     * @param predicate predicate
     * @param y desired y-coord
     * @return List of Vector3f of populated locationMap(s)
     */
    public IList<Vector3f> getPopulatedLocations(int chunkId, Predicate<TexByte> predicate, float y) {
        IList<Vector3f> result = new GapList<>();

        List<Vector3f> xyzCoords = locations.getAllByKey1((float) y);

        if (xyzCoords != null) {
            for (Vector3f xyz : xyzCoords) {
                int i = Math.round((xyz.x + Chunk.BOUND) / 2.0f);
                int j = Math.round((xyz.y + Chunk.BOUND) / 2.0f);
                int k = Math.round((xyz.z + Chunk.BOUND) / 2.0f);

                if (!safeCheck(i, j, k)) {
                    continue;
                }

                TexByte value = locationMap[i][j][k];
                Vector3f pos = new Vector3f(xyz.x, xyz.y, xyz.z);
                if (value != null && predicate.test(value) && Chunk.chunkFunc(pos) == chunkId) {
                    result.add(pos);
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
                List<Vector3f> xyzCoords = locations.getAllByKey1((float) y);
                if (xyzCoords != null) {
                    for (Vector3f xyz : xyzCoords) {
                        int i = Math.round((xyz.x + Chunk.BOUND) / 2.0f);
                        int j = Math.round((xyz.y + Chunk.BOUND) / 2.0f);
                        int k = Math.round((xyz.z + Chunk.BOUND) / 2.0f);

                        if (!safeCheck(i, j, k)) {
                            continue;
                        }

                        TexByte value = locationMap[i][j][k];
                        Vector3f pos = new Vector3f(xyz.x, xyz.y, xyz.z);
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
    public boolean removeLocation(Vector3f pos) {
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

        locations.remove(pos);

        return populated;
    }

    /**
     * Updates new location with (pos, value) while removing old location. Slow
     * operation.
     *
     * @param oldPos oldPosition (to remove)
     * @param newPos newPosition (to update)
     * @param value value on new position
     * @return did update succeeded
     */
    public boolean updateLocation(Vector3f oldPos, Vector3f newPos, TexByte value) {
        int i0 = Math.round((oldPos.x + Chunk.BOUND) / 2.0f);
        int j0 = Math.round((oldPos.z + Chunk.BOUND) / 2.0f);
        int k0 = Math.round((oldPos.y + Chunk.BOUND) / 2.0f);

        if (safeCheck(i0, j0, k0)) {
            removeLocation(oldPos);
        } else {
            return false;
        }

        int i1 = Math.round((newPos.x + Chunk.BOUND) / 2.0f);
        int j1 = Math.round((newPos.z + Chunk.BOUND) / 2.0f);
        int k1 = Math.round((newPos.y + Chunk.BOUND) / 2.0f);

        if (safeCheck(i1, j1, k1)) {
            putLocation(newPos, value);
        } else {
            return false;
        }

        return true;
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
     * Get XYZ Locations based on Y
     *
     * @return XYZ Locations Where one Y maps into several XYZ
     */
    public Key1List<Vector3f, Float> getLocations() {
        return locations;
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
