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
package rs.alexanderstojanovich.evg.chunk;

import java.util.List;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.light.LightSource;
import rs.alexanderstojanovich.evg.location.TexByte;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.ModelUtils;

/**
 * Chunk is virtual.
 *
 * It does not actually exists (anymore). It is part of the world blocks based
 * on location VEC3 pos.
 *
 * Contains various util functions.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Chunk { // some operations are mutually exclusive    

    // MODULATOR, DIVIDER, VISION are used in chunkCheck and for determining visible chunks
    public static final int BOUND = 256;
    public static final float VISION = 256.0f; // determines visibility
    public static final int GRID_SIZE = 4;

    public static final float STEP = 1.0f / (float) (GRID_SIZE);
    public static final int CHUNK_NUM = GRID_SIZE * GRID_SIZE;
    public static final float LENGTH = BOUND * STEP * 2.0f;

    // is a group of blocks which are prepared for instanced rendering
    // where each tuple is considered as:                
    //--------------------------MODULATOR--------DIVIDER--------VISION-------D--------E-----------------------------
    //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
    /**
     * Binary search of the tuple. Tuples are sorted by name ascending.
     * Complexity is logarithmic.
     *
     * @param tupleList provided tuple list
     * @param keyTexture texture name part
     * @param keyFaceBits face bits part
     * @return Tuple if found (null if not found)
     */
    public static Tuple getTuple(List<Tuple> tupleList, String keyTexture, Integer keyFaceBits) {
        String keyName = String.format("%s%02d", keyTexture, keyFaceBits);
        int left = 0;
        int right = tupleList.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            Tuple candidate = tupleList.get(mid);
            int res = candidate.getName().compareTo(keyName);
            if (res < 0) {
                left = mid + 1;
            } else if (res == 0) {
                return candidate;
            } else {
                right = mid - 1;
            }
        }
        return null;
    }

    /**
     * Gets Block from the tuple block list (duplicates may exist but in very
     * low quantity). Complexity is O(log(n)+k).
     *
     * @param tuple (chunk) tuple where block might be located
     * @param pos Vector3f position of the block
     * @return block if found (null if not found)
     */
    public static Block getBlock(Tuple tuple, Vector3f pos) {
        Integer key = ModelUtils.blockSpecsToUniqueInt(tuple.isSolid(), tuple.texName(), tuple.faceBits(), pos);
        int left = 0;
        int right = tuple.blockList.size() - 1;
        int startIndex = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            Block candidate = tuple.blockList.get(mid);
            Integer candInt = candidate.getId();
            int res = candInt.compareTo(key);
            if (res < 0) {
                left = mid + 1;
            } else if (res == 0) {
                startIndex = mid;
                right = mid - 1;
            } else {
                right = mid - 1;
            }
        }

        left = 0;
        right = tuple.blockList.size() - 1;
        int endIndex = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            Block candidate = tuple.blockList.get(mid);
            Integer candInt = candidate.getId();
            int res = candInt.compareTo(key);
            if (res < 0) {
                left = mid + 1;
            } else if (res == 0) {
                endIndex = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        if (startIndex != -1 && endIndex != -1) {
            for (int i = startIndex; i <= endIndex; i++) {
                Block blk = tuple.blockList.get(i);
                if (blk.pos.equals(pos)) {
                    return blk;
                }
            }
        }

        return null;
    }

    /**
     * Retrieves a block from the tuple's block list based on its unique ID and
     * position. Optimized with binary search and filtering for duplicates.
     * Complexity is O(log(n) + k), where k is the number of blocks with the
     * same unique ID.
     *
     * @param tuple the tuple (chunk) containing the block list
     * @param pos the position of the block as a Vector3f
     * @param blkId the unique ID of the block
     * @return the matching block if found, or null if not found
     */
    public static Block getBlock(Tuple tuple, Vector3f pos, int blkId) {
        // Binary search for the first occurrence of blkId
        int left = 0;
        int right = tuple.blockList.size() - 1;
        int firstIndex = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            Block candidate = tuple.blockList.get(mid);
            int comparison = Integer.compare(candidate.getId(), blkId);

            if (comparison < 0) {
                left = mid + 1;
            } else if (comparison > 0) {
                right = mid - 1;
            } else {
                firstIndex = mid;
                right = mid - 1; // Continue searching to the left
            }
        }

        // If no block with the given ID exists, return null
        if (firstIndex == -1) {
            return null;
        }

        // Collect all blocks with the matching blkId
        IList<Block> matchingBlocks = new GapList<>();
        for (int i = firstIndex; i < tuple.blockList.size(); i++) {
            Block candidate = tuple.blockList.get(i);
            if (candidate.getId() == blkId) {
                matchingBlocks.add(candidate);
            } else {
                break; // Exit loop as IDs are sorted
            }
        }

        // Check for a block with the matching position
        for (Block blk : matchingBlocks) {
            if (blk.pos.equals(pos)) {
                return blk;
            }
        }

        return null; // No matching block found
    }

    /**
     * Retrieves a block from the tuple's block list based on its unique ID.
     * Uses binary search to find the block efficiently. Complexity is
     * O(log(n)).
     *
     * @param tuple the tuple (chunk) containing the block list
     * @param blkId the unique ID of the block
     * @return the first matching block if found, or null if not found
     */
    public static Block getBlock(Tuple tuple, int blkId) {
        // Perform binary search to locate the block with the given ID
        int left = 0;
        int right = tuple.blockList.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            Block candidate = tuple.blockList.get(mid);
            int comparison = Integer.compare(candidate.getId(), blkId);

            if (comparison < 0) {
                left = mid + 1;
            } else if (comparison > 0) {
                right = mid - 1;
            } else {
                return candidate; // Found a match
            }
        }

        // No block with the given ID was found
        return null;
    }

    /**
     * Transfer block between two tuples. Block will be transfered from tuple
     * with formFaceBits to tuple with current facebits.
     *
     * @param tupleList provided tuple list
     * @param block block to transfer
     * @param formFaceBits face bits before
     * @param currFaceBits face bits current (after the change)
     */
    public static void transfer(IList<Tuple> tupleList, Block block, int formFaceBits, int currFaceBits) { // update fluids use this to transfer fluid blocks between tuples
        String texture = block.getTexName();

        Tuple srcTuple = getTuple(tupleList, texture, formFaceBits);
        if (srcTuple != null) { // lazy aaah!
            srcTuple.blockList.remove(block);
            if (srcTuple.getBlockList().isEmpty()) {
                tupleList.remove(srcTuple);
            }
        }

        Tuple dstTuple = getTuple(tupleList, texture, currFaceBits);
        if (dstTuple == null) {
            dstTuple = new Tuple(texture, currFaceBits);
            tupleList.add(dstTuple);
            tupleList.sort(Tuple.TUPLE_COMP);
        }
        List<Block> blockList = dstTuple.blockList;
        blockList.add(block);
        blockList.sort(Block.UNIQUE_BLOCK_CMP);
    }

    /**
     * Transfer block between two tuples. Block will be transfered from tuple
     * with formFaceBits to tuple with current facebits.
     *
     * @param tupleList provided tuple list
     * @param blkUnits blockUnits to transfer
     */
    public static void transfer(IList<Tuple> tupleList, IList<TransferUnit> blkUnits) { // update fluids use this to transfer fluid blocks between tuples
        for (TransferUnit unit : blkUnits) {
            Block block = unit.block;
            String texture = block.getTexName();

            Tuple srcTuple = getTuple(tupleList, texture, unit.bitsBefore);
            if (srcTuple != null) { // lazy aaah!
                srcTuple.blockList.remove(block);
                if (srcTuple.getBlockList().isEmpty()) {
                    tupleList.remove(srcTuple);
                }
            }

            Tuple dstTuple = getTuple(tupleList, texture, unit.bitsAfter);
            if (dstTuple == null) {
                dstTuple = new Tuple(texture, unit.bitsAfter);
                tupleList.add(dstTuple);
                tupleList.sort(Tuple.TUPLE_COMP);
            }
            List<Block> blockList = dstTuple.blockList;
            blockList.add(block);
            blockList.sort(Block.UNIQUE_BLOCK_CMP);
        }
    }

    /**
     * Updates blocks faces of both original block and adjacent blocks. Block
     * must be solid.
     *
     * Used after add operation.
     *
     * @param tupleList provided tuple list
     * @param block block to update
     */
    protected static void updateForAdd(IList<Tuple> tupleList, Block block) {
        // only same solidity - solid to solid or fluid to fluid is updated        
        int neighborBits = block.isSolid()
                ? LevelContainer.AllBlockMap.getNeighborSolidBits(block.pos)
                : LevelContainer.AllBlockMap.getNeighborFluidBits(block.pos);
        if (neighborBits != 0) {
            // retieve current neightbor bits      
            int faceBitsBefore = block.getFaceBits();
            // -------------------------------------------------------------------
            // this logic updates facebits of this block
            // & transfers it to correct tuple 
            // -------------------------------------------------------------------                    
            block.setFaceBits(~neighborBits & 63);
            int faceBitsAfter = block.getFaceBits();
            if (faceBitsBefore != faceBitsAfter) {
                Chunk.transfer(tupleList, block, faceBitsBefore, faceBitsAfter);
            }
            // query all neighbors and update this block and adjacent blocks accordingly
            // tranfer units
            IList<TransferUnit> blkUnits = new GapList<>();
            for (int j = Block.LEFT; j <= Block.FRONT; j++) {
                // -------------------------------------------------------------------
                // following logic updates adjacent block 
                // if it is same solidity as this block
                // need to find tuple where it is located
                // -------------------------------------------------------------------
                Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
                TexByte location = LevelContainer.AllBlockMap.getLocation(adjPos);
                if (location != null) {
                    int blkId = location.blkId;
                    String tupleTexName = location.texName;
                    int adjNBits = block.isSolid()
                            ? LevelContainer.AllBlockMap.getNeighborSolidBits(adjPos)
                            : LevelContainer.AllBlockMap.getNeighborFluidBits(adjPos);
                    int k = ((j & 1) == 0 ? j + 1 : j - 1);
                    int mask = 1 << k;
                    // revert the bit that was set in LevelContainer
                    //(looking for old bits i.e. current tuple)
                    int tupleBits = adjNBits ^ (~mask & 63);

                    Tuple tuple = Chunk.getTuple(tupleList, tupleTexName, tupleBits);
                    if (tuple != null) {
                        Block adjBlock = Chunk.getBlock(tuple, blkId);

                        if (adjBlock != null) {
                            int adjFaceBitsBefore = adjBlock.getFaceBits();
                            adjBlock.setFaceBits(~adjNBits & 63);
                            int adjFaceBitsAfter = adjBlock.getFaceBits();
                            if (adjFaceBitsBefore != adjFaceBitsAfter) {
                                // if bits changed, i.e. some face(s) got disabled
                                // tranfer to correct tuple
                                blkUnits.add(new TransferUnit(adjBlock, adjFaceBitsBefore, adjFaceBitsAfter));
                            }
                        }
                    }
                }
            }

            if (!blkUnits.isEmpty()) {
                Chunk.transfer(tupleList, blkUnits);
            }
        }
    }

    /**
     * Updates blocks faces of both original block and adjacent blocks. Block
     * must be solid.
     *
     * Used after rem operation.
     *
     * @param tupleList provided tuple list
     * @param block block to update
     */
    protected static void updateForRem(IList<Tuple> tupleList, Block block) {
        // setSafeCheck adjacent blocks
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
            int nBits = block.isSolid()
                    ? LevelContainer.AllBlockMap.getNeighborSolidBits(block.pos)
                    : LevelContainer.AllBlockMap.getNeighborFluidBits(block.pos);
            TexByte location = LevelContainer.AllBlockMap.getLocation(adjPos);
            // location exists and has neighbors (otherwise pointless)
            if (location != null && nBits != 0) {
                int blkId = location.blkId;
                String tupleTexName = location.texName;
                byte adjNBits = location.getByteValue();
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int mask = 1 << k;
                // revert the bit that was set in LevelContainer
                //(looking for old bits i.e. current tuple)
                int tupleBits = adjNBits ^ (~mask & 63);

                Tuple tuple = Chunk.getTuple(tupleList, tupleTexName, tupleBits);
                Block adjBlock = null;

                if (tuple != null) {
                    adjBlock = Chunk.getBlock(tuple, blkId);
                }

                if (adjBlock != null) {
                    int adjFaceBitsBefore = adjBlock.getFaceBits();
                    adjBlock.setFaceBits(~adjNBits & 63);
                    int adjFaceBitsAfter = adjBlock.getFaceBits();
                    if (adjFaceBitsBefore != adjFaceBitsAfter) {
                        // if bits changed, i.e. some face(s) got disabled
                        // tranfer to correct tuple
                        Chunk.transfer(tupleList, adjBlock, adjFaceBitsBefore, adjFaceBitsAfter);
                    }
                }
            }
        }
    }

    /**
     * Add block to the chunks.
     *
     * @param tupleList chunks tuple list
     * @param block block to add
     */
    public static void addBlock(IList<Tuple> tupleList, Block block) {
        String blockTexture = block.getTexName();
        int blockFaceBits = block.getFaceBits();
        Tuple tuple = getTuple(tupleList, blockTexture, blockFaceBits);

        if (tuple == null) {
            tuple = new Tuple(blockTexture, blockFaceBits);
            tupleList.add(tuple);
            tupleList.sort(Tuple.TUPLE_COMP);
        }
        IList<Block> blockList = tuple.blockList;
        blockList.add(block);
        blockList.sort(Block.UNIQUE_BLOCK_CMP);

        // level container also set neighbor bits
        LevelContainer.putBlock(block);
        // update original block with neighbor blocks
        // setSafeCheck if it's light block        
        if (block.getTexName().equals("reflc")) { // if first add
            GameObject gameObject;
            try {
                gameObject = GameObject.getInstance();
                LightSource lightSource = new LightSource(block.pos, block.getPrimaryRGBColor(), LightSource.DEFAULT_LIGHT_INTENSITY);
                gameObject.levelContainer.lightSources.addLight(lightSource);
                gameObject.levelContainer.lightSources.setModified(block.pos, true);
            } catch (Exception ex) {
                DSLogger.reportError("Could not add the block!", null);
            }
        }
        updateForAdd(tupleList, block);
    }

    /**
     * Remove block from the chunk.
     *
     * @param tupleList chunks tuple list
     * @param block block to remove
     */
    public static void removeBlock(IList<Tuple> tupleList, Block block) {
        String blockTexture = block.getTexName();
        int blockFaceBits = block.getFaceBits();
        Tuple target = getTuple(tupleList, blockTexture, blockFaceBits);
        if (target != null) {
            target.getBlockList().remove(block);
            // if tuple has no blocks -> remove it
            if (target.getBlockList().isEmpty()) {
                tupleList.remove(target);
            }

            // level container also set neighbor bits
            LevelContainer.removeBlock(block);
            // update original block with neighbor blocks
            // setSafeCheck if it's light block
            if (block.getTexName().equals("reflc")) {
                GameObject gameObject;
                try {
                    gameObject = GameObject.getInstance();
                    gameObject.levelContainer.lightSources.removeLight(block.pos);
                } catch (Exception ex) {
                    DSLogger.reportError("Could not remove the block!", null);
                }

            }
            updateForRem(tupleList, block);
        }
    }

    /**
     * Gets the chunk block list using chunk id.
     *
     * @param tupleList chunks tuple list
     * @param chunkId provided chunk id.
     * @return chunk (if exists non-empty) block list
     */
    public static IList<Block> getBlockList(int chunkId, IList<Tuple> tupleList) {
        IList<Block> blocks = new GapList<>();
        tupleList.forEach(tuple -> blocks.addAll(tuple.blockList.filter(blk -> Chunk.chunkFunc(blk.pos) == chunkId)));

        return blocks;
    }

    /**
     * Gets the chunk block list using chunk id.
     *
     * @param tupleList chunks tuple list
     * @param chunkIdList provided chunk id list
     * @return chunk (if exists non-empty) block list
     */
    public static IList<Block> getBlockList(IList<Integer> chunkIdList, IList<Tuple> tupleList) {
        IList<Block> blocks = new GapList<>();
        tupleList.forEach(tuple -> blocks.addAll(tuple.blockList.filter(blk -> chunkIdList.contains(Chunk.chunkFunc(blk.pos)))));

        return blocks;
    }

    /**
     * Calculate chunk based on position.
     *
     * @param pos position of the thing (critter or object)
     * @return chunk number (grid size based)
     */
    public static int chunkFunc(Vector3f pos) {
        // normalized x & z
        float nx = (pos.x + BOUND) / (float) (BOUND << 1);
        float nz = (pos.z + BOUND) / (float) (BOUND << 1);

        // setSafeCheck which column of the interval
        int col = (int) (nx * (1.0f / STEP - 1.0f));

        // setSafeCheck which rows of the interval
        int row = (int) (nz * (1.0f / STEP - 1.0f));

        // determining chunk id -> row(z) & col(x)
        int cid = row * GRID_SIZE + col;

        return cid;
    }

    /**
     * Calculate position centroid based on the chunk Id
     *
     * @param chunkId chunk number
     *
     * @return chunk middle position
     */
    public static Vector3f invChunkFunc(int chunkId) {
        // determining row(z) & col(x)
        int col = chunkId % GRID_SIZE;
        int row = chunkId / GRID_SIZE;

        // calculating middle normalized
        // col * STEP + STEP / 2.0f;
        // row * STEP + STEP / 2.0f;
        float nx = STEP * (col + 0.5f);
        float nz = STEP * (row + 0.5f);

        float x = nx * (BOUND << 1) - BOUND;
        float z = nz * (BOUND << 1) - BOUND;

        return new Vector3f(x, 0.0f, z);
    }

    /**
     * Does camera intersects this chunk
     *
     * @param chunkId chunk number
     * @param camera (obsever) camera
     * @return intersection bool value
     */
    public static boolean intersectsVisionFunc(int chunkId, Camera camera) {
        final Vector3f chunkPos = invChunkFunc(chunkId);
        final GameObject gameObject;
        try {
            gameObject = GameObject.getInstance();

            Matrix4f perspectiveMatrix = gameObject.perspectiveRenderer.perspectiveMatrix;
            Matrix4f viewMatrix = camera.viewMatrix;
            Matrix4f temp = new Matrix4f();
            Matrix4f pv = perspectiveMatrix.mul(viewMatrix, temp);
            FrustumIntersection frustumIntersection = new org.joml.FrustumIntersection(pv);

            Vector3f temp1 = new Vector3f();
            Vector3f min = chunkPos.sub(LENGTH / 2.0f, VISION * 8.0f, LENGTH / 2.0f, temp1);
            Vector3f temp2 = new Vector3f();
            Vector3f max = chunkPos.add(LENGTH / 2.0f, VISION * 8.0f, LENGTH / 2.0f, temp2);

            return frustumIntersection.intersectAab(min, max) != FrustumIntersection.OUTSIDE;
        } catch (Exception ex) {
            DSLogger.reportInfo(ex.getMessage(), ex);
        }

        return false;
    }

    /**
     * Does camera sees chunk. Useful.
     *
     * @param chunkId chunk id (number)
     * @param camera main camera (or other camera)
     * @param angleDegrees angle degrees
     * @return boolean condition see (or not see)
     */
    public static boolean doesSeeChunk(int chunkId, Camera camera, float angleDegrees) {
        final Vector3f chunkPos = invChunkFunc(chunkId);
        final Vector2f chunkPosXZ = new Vector2f(chunkPos.x, chunkPos.z);
        final Vector2f camPosXZ = new Vector2f(camera.pos.x, camera.pos.z);
        final Vector2f camFrontXZNeg = new Vector2f(-camera.getFront().x, -camera.getFront().z);
        final float cosine = org.joml.Math.cos(org.joml.Math.toRadians(angleDegrees));
        boolean yea = false;

        // Now iterate and perform calculations
        OUTER:
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Vector2f temp = new Vector2f();
                Vector2f tst = new Vector2f(x, z).add(chunkPosXZ.sub(camPosXZ, temp), temp).normalize();
                if (tst.dot(camFrontXZNeg) <= cosine) {
                    yea |= true;
                    break OUTER;
                }
            }
        }

        return yea;
    }

    /**
     * Determine which chunks are visible by this chunk. If visible put into the
     * V list, otherwise put into the I list.
     *
     * @param vChnkIdList visible chunk queue
     * @param iChnkIdList invisible chunk queue
     * @param camera (Observer) camera
     *
     * @return list of changed chunks
     */
    public static boolean determineVisible(IList<Integer> vChnkIdList, IList<Integer> iChnkIdList, Camera camera) {
        boolean changed = false;
        // current chunk where player is        
        int currChunkId = chunkFunc(camera.pos);
        int currCol = currChunkId % GRID_SIZE;
        int currRow = currChunkId / GRID_SIZE;

        vChnkIdList.addIfAbsent(currChunkId);

        // rest of the chunks
        for (int chunkId = 0; chunkId < Chunk.CHUNK_NUM; chunkId++) {
            if (chunkId != currChunkId) {
                int col = chunkId % GRID_SIZE;
                int row = chunkId / GRID_SIZE;

                int deltaCol = Math.abs(currCol - col);
                int deltaRow = Math.abs(currRow - row);

                if (deltaCol <= 1 && deltaRow <= 1) {
                    changed |= vChnkIdList.addIfAbsent(chunkId);
                } else if (!iChnkIdList.contains(chunkId)) {
                    changed |= iChnkIdList.addIfAbsent(chunkId);
                }

            }
        }

        return changed;
    }

}
