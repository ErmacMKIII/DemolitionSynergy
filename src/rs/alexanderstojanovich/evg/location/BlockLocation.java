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

import java.util.function.Predicate;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import org.magicwerk.brownies.collections.Key1List;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.models.Block;

/**
 * Big static memory arrays of block location properties.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class BlockLocation {

    protected final TexByte[][][] locationMap = new TexByte[Chunk.BOUND][Chunk.BOUND][Chunk.BOUND];
    public final Key1List<TexByte, Integer> locationProperties = new Key1List.Builder<TexByte, Integer>()
            .withListBig(true)
            .withKey1Map(TexByte::getBlkId).withKey1Duplicates(true)
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
        locationProperties.clear();
    }

    public boolean safeCheck(int i, int j, int k) {
        return i >= 0 && i < Chunk.BOUND
                && j >= 0 && j < Chunk.BOUND
                && k >= 0 && k < Chunk.BOUND;
    }

    /**
     * Put block locationMap into the population matrix.
     *
     * @param color block color
     * @param pos block position
     * @param texname texture of the block
     * @param bits neighbor bits (for each sides) how many block of the same
     * kind adjacent.
     * @param solid is block solid (or not)
     * @param blkId unique block id (property of block)
     */
    public void putLocation(Vector4f color, Vector3f pos, String texname, int bits, boolean solid, int blkId) {
        int i = (int) ((pos.x + Chunk.BOUND) / 2.0f);
        int j = (int) ((pos.z + Chunk.BOUND) / 2.0f);
        int k = (int) ((pos.y + Chunk.BOUND) / 2.0f);

        if (!safeCheck(i, j, k)) {
            return;
        }

        TexByte loc = new TexByte(color, texname, (byte) bits, solid, blkId);
        locationMap[i][j][k] = loc;
        locationProperties.add(loc);

        population++;
    }

    /**
     * Put block locationMap into the population matrix.
     *
     * @param pos block position
     * @param texByte texture of the block w/ neighbor bits
     */
    public void putLocation(Vector3f pos, TexByte texByte) {
        int i = (int) ((pos.x + Chunk.BOUND) / 2.0f);
        int j = (int) ((pos.z + Chunk.BOUND) / 2.0f);
        int k = (int) ((pos.y + Chunk.BOUND) / 2.0f);

        if (!safeCheck(i, j, k)) {
            return;
        }

        locationMap[i][j][k] = texByte;

        locationProperties.add(texByte);

        population++;
    }

    /**
     * Get Location for given position (can be null)
     *
     * @param pos where block could be locationMap
     * @return texture w/ byte of locationMap or null if does not exist
     */
    public TexByte getLocation(Vector3f pos) {
        int i = (int) ((pos.x + Chunk.BOUND) / 2.0f);
        int j = (int) ((pos.z + Chunk.BOUND) / 2.0f);
        int k = (int) ((pos.y + Chunk.BOUND) / 2.0f);

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
        int i = (int) ((pos.x + Chunk.BOUND) / 2.0f);
        int j = (int) ((pos.z + Chunk.BOUND) / 2.0f);
        int k = (int) ((pos.y + Chunk.BOUND) / 2.0f);

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

        int i = (int) ((pos.x + Chunk.BOUND) / 2.0f);
        int j = (int) ((pos.z + Chunk.BOUND) / 2.0f);
        int k = (int) ((pos.y + Chunk.BOUND) / 2.0f);

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
    public Key1List<TexByte, Integer> getPopulatedLocationProperties() {
        return locationProperties;
    }

    /**
     * List of populated locations for given blkId.
     *
     * @param blkId blk primary (key) id
     * @return List of Vector3f of populated locationMap(s)
     */
    public IList<TexByte> getPopulatedLocationProperties(int blkId) {
        return locationProperties.getAllByKey1(blkId);
    }

    /**
     * List of populated locations with given predicated. Warning: this is
     * performance costly - So don't call it in a loop!
     *
     * @param origin position of observer
     * @param distance measured distance from origin
     *
     * @param predicate predicate
     * @return List of Vector3f of populated locationMap(s)
     */
    public IList<Vector3f> getPopulatedLocations(Predicate<TexByte> predicate, Vector3f origin, float distance) {
        // List to store populated locations
        IList<Vector3f> result = new GapList<>();

        // Calculate bounding box around player's position
        float minX = origin.x - distance;
        float maxX = origin.x + distance;
        float minY = Math.max(-Chunk.BOUND + 2.0f, origin.y - distance); // Adjust minY based on distance
        float maxY = Math.min(Chunk.BOUND - 2.0f, origin.y + distance); // Adjust maxY based on distance
        float minZ = origin.z - distance;
        float maxZ = origin.z + distance;

        // Iterate over each y, x, and z position within the bounding box
        for (float y = minY; y <= maxY; y += 2.0f) {
            for (float x = minX; x <= maxX; x += 2.0f) {
                for (float z = minZ; z <= maxZ; z += 2.0f) {
                    // Check if the position is within the specified distance from the origin
                    if (Vector3f.distance(x, y, z, origin.x, origin.y, origin.z) > distance) {
                        continue; // Skip if outside the distance
                    }

                    // Calculate chunk ID for the current position
                    int chunkId = Chunk.chunkFunc(new Vector3f(x, 0.0f, z));

                    // Check if the chunk ID is within valid range
                    if (chunkId >= 0 && chunkId < Chunk.CHUNK_NUM) {
                        // Calculate indices within the locationMap for the current position
                        int i = (int) ((x + Chunk.BOUND) / 2.0f);
                        int j = (int) ((z + Chunk.BOUND) / 2.0f);
                        int k = (int) ((y + Chunk.BOUND) / 2.0f);

                        // Check if indices are within valid range
                        if (!safeCheck(i, j, k)) {
                            continue; // Skip if indices are out of bounds
                        }

                        // Get the value at the calculated indices
                        TexByte value = locationMap[i][j][k];

                        // Check if the value exists and satisfies the predicate
                        if (value != null && predicate.test(value)) {
                            // Add the position to the result list
                            result.add(new Vector3f(x, y, z));
                        }
                    }
                }
            }
        }

        // Return the list of populated locations
        return result;
    }

    /**
     * List of populated locations with given predicated. Warning: this is
     * performance costly - So don't call it in a loop!
     *
     * @param predicate Predicate
     * @return List of Vector3f of populated locationMap(s)
     */
    public IList<Vector3f> getPopulatedLocations(Predicate<TexByte> predicate) {
        // Initialize the result list to store populated locations
        IList<Vector3f> result = new GapList<>();

        // Define the lower and upper bounds for the Y coordinate
        float lYBound = -Chunk.BOUND + 2.0f;
        float rYBound = Chunk.BOUND - 2.0f;

        // Iterate over the Y coordinates within the specified bounds
        for (float y = lYBound; y <= rYBound; y += 2.0f) {
            // Iterate over the X coordinates within the chunk bounds
            for (float x = -Chunk.BOUND + 2.0f; x <= Chunk.BOUND - 2.0f; x += 2.0f) {
                // Iterate over the Z coordinates within the chunk bounds
                for (float z = -Chunk.BOUND + 2.0f; z <= Chunk.BOUND - 2.0f; z += 2.0f) {
                    // Calculate the indices for the current position
                    int i = (int) ((x + Chunk.BOUND) / 2.0f);
                    int j = (int) ((z + Chunk.BOUND) / 2.0f);
                    int k = (int) ((y + Chunk.BOUND) / 2.0f);

                    // Check if the indices are within the bounds of the location map
                    if (!safeCheck(i, j, k)) {
                        continue; // Skip to the next iteration if out of bounds
                    }

                    // Retrieve the value from the location map at the calculated indices
                    TexByte value = locationMap[i][j][k];

                    // Check if the value is not null and satisfies the predicate
                    if (value != null && predicate.test(value)) {
                        // Create a new Vector3f object representing the current position
                        Vector3f pos = new Vector3f(x, y, z);
                        // Add the position to the result list
                        result.add(pos);
                    }
                }
            }
        }

        // Return the list of populated locations
        return result;
    }

    /**
     * List of populated locations within the given chunk. Warning: this is
     * performance costly - So don't call it in a loop!
     *
     * @param chunkId chunkId to check (block) population.
     * @return List of Vector3f of populated locationMap(s)
     */
    public IList<Vector3f> getPopulatedLocations(int chunkId) {
        IList<Vector3f> result = new GapList<>();

        // Half the chunk length for spatial calculations
        final float halfLength = Chunk.LENGTH / 2.0f;

        // Calculate the centroid position of the chunk
        Vector3f chunkCenter = Chunk.invChunkFunc(chunkId);

        // Determine bounds in grid indices
        int iMin = (int) ((chunkCenter.x - halfLength + Chunk.BOUND) / 2.0f);
        int iMax = (int) ((chunkCenter.x + halfLength + Chunk.BOUND) / 2.0f);

        int jMin = (int) ((chunkCenter.z - halfLength + Chunk.BOUND) / 2.0f);
        int jMax = (int) ((chunkCenter.z + halfLength + Chunk.BOUND) / 2.0f);

        int kMin = (int) ((-Chunk.BOUND + 2.0f + Chunk.BOUND) / 2.0f); // Map lowerYBound
        int kMax = (int) ((Chunk.BOUND - 2.0f + Chunk.BOUND) / 2.0f); // Map upperYBound

        // Iterate over grid indices
        for (int k = kMin; k <= kMax; k++) {
            for (int i = iMin; i < iMax; i++) {
                for (int j = jMin; j < jMax; j++) {
                    // Skip invalid or unsafe positions
                    if (!safeCheck(i, j, k)) {
                        continue;
                    }

                    // Check if the position is populated
                    TexByte value = locationMap[i][j][k];
                    if (value != null) {
                        // Convert grid indices back to world coordinates
                        float x = i * 2.0f - Chunk.BOUND;
                        float z = j * 2.0f - Chunk.BOUND;
                        float y = k * 2.0f - Chunk.BOUND;

                        result.add(new Vector3f(x, y, z));
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
     * @return List of Vector3f of populated locationMap(s)
     */
    public IList<Vector3f> getPopulatedLocations(int chunkId, Predicate<TexByte> predicate) {
        IList<Vector3f> result = new GapList<>();

        // Half the chunk length for spatial calculations
        final float halfLength = Chunk.LENGTH / 2.0f;

        // Calculate the centroid position of the chunk
        Vector3f chunkCenter = Chunk.invChunkFunc(chunkId);

        // Determine bounds in grid indices
        int iMin = (int) ((chunkCenter.x - halfLength + Chunk.BOUND) / 2.0f);
        int iMax = (int) ((chunkCenter.x + halfLength + Chunk.BOUND) / 2.0f);

        int jMin = (int) ((chunkCenter.z - halfLength + Chunk.BOUND) / 2.0f);
        int jMax = (int) ((chunkCenter.z + halfLength + Chunk.BOUND) / 2.0f);

        int kMin = (int) ((-Chunk.BOUND + 2.0f + Chunk.BOUND) / 2.0f); // Map lowerYBound
        int kMax = (int) ((Chunk.BOUND - 2.0f + Chunk.BOUND) / 2.0f); // Map upperYBound

        // Iterate over grid indices
        for (int k = kMin; k <= kMax; k++) {
            for (int i = iMin; i < iMax; i++) {
                for (int j = jMin; j < jMax; j++) {
                    // Skip invalid or unsafe positions
                    if (!safeCheck(i, j, k)) {
                        continue;
                    }

                    // Check if the position is populated
                    TexByte value = locationMap[i][j][k];
                    if (value != null && predicate.test(value)) {
                        // Convert grid indices back to world coordinates
                        float x = i * 2.0f - Chunk.BOUND;
                        float z = j * 2.0f - Chunk.BOUND;
                        float y = k * 2.0f - Chunk.BOUND;

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
    public boolean removeLocation(Vector3f pos) {
        int i = (int) ((pos.x + Chunk.BOUND) / 2.0f);
        int j = (int) ((pos.z + Chunk.BOUND) / 2.0f);
        int k = (int) ((pos.y + Chunk.BOUND) / 2.0f);

        if (!safeCheck(i, j, k)) {
            return false;
        }

        boolean populated = locationMap[i][j][k] != null;

        if (populated) {
            population--;
        }

        locationProperties.remove(locationMap[i][j][k]);
        locationMap[i][j][k] = null;

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
        int i0 = (int) ((oldPos.x + Chunk.BOUND) / 2.0f);
        int j0 = (int) ((oldPos.z + Chunk.BOUND) / 2.0f);
        int k0 = (int) ((oldPos.y + Chunk.BOUND) / 2.0f);

        if (safeCheck(i0, j0, k0)) {
            removeLocation(oldPos);
        } else {
            return false;
        }

        int i1 = (int) ((newPos.x + Chunk.BOUND) / 2.0f);
        int j1 = (int) ((newPos.z + Chunk.BOUND) / 2.0f);
        int k1 = (int) ((newPos.y + Chunk.BOUND) / 2.0f);

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
    public Key1List<TexByte, Integer> getLocationProperties() {
        return locationProperties;
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
