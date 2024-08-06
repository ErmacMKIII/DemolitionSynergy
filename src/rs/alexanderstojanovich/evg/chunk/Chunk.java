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
import java.util.Objects;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.cache.CacheModule;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.light.LightSource;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.location.TexByte;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.ModelUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Chunk implements Comparable<Chunk> { // some operations are mutually exclusive    

    // MODULATOR, DIVIDER, VISION are used in chunkCheck and for determining visible chunks
    public static final int BOUND = 256;
    public static final float VISION = 256.0f; // determines visibility
    public static final int GRID_SIZE = 4;

    public static final float STEP = 1.0f / (float) (GRID_SIZE);
    public static final int CHUNK_NUM = GRID_SIZE * GRID_SIZE;
    public static final float LENGTH = BOUND * STEP * 2.0f;

    // id of the chunk (signed)
    public final int id;

    // is a group of blocks which are prepared for instanced rendering
    // where each tuple is considered as:                
    //--------------------------MODULATOR--------DIVIDER--------VISION-------D--------E-----------------------------
    //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
    public final IList<Tuple> tupleList = new GapList<>();

    private boolean buffered = false;

    private float timeToLive = LevelContainer.STD_TTL;

    public Chunk(int id) {
        this.id = id;
    }

    @Override
    public int compareTo(Chunk o) {
        return Chunks.COMPARATOR.compare(this, o);
    }

    /**
     * Binary search of the tuple. Tuples are sorted by name ascending.
     * Complexity is logarithmic.
     *
     * @param keyTexture texture name part
     * @param keyFaceBits face bits part
     * @return Tuple if found (null if not found)
     */
    public Tuple getTuple(String keyTexture, Integer keyFaceBits) {
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
     * Gets Block from the tuple block list (duplicates may exist but in very
     * low quantity). Complexity is O(log(n)+k). Faster than method with
     * supplied Vec3f position.
     *
     * @param tuple (chunk) tuple where block might be located
     * @param pos Vector3f position of the block
     * @param blkId block unique id
     * @return block if found (null if not found)
     */
    public static Block getBlock(Tuple tuple, Vector3f pos, int blkId) {
        Integer key = blkId;

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
     * Transfer block between two tuples. Block will be transfered from tuple
     * with formFaceBits to tuple with current facebits.
     *
     * @param block block to transfer
     * @param formFaceBits face bits before
     * @param currFaceBits face bits current (after the change)
     */
    public void transfer(Block block, int formFaceBits, int currFaceBits) { // update fluids use this to transfer fluid blocks between tuples
        String texture = block.getTexName();

        Tuple srcTuple = getTuple(texture, formFaceBits);
        if (srcTuple != null) { // lazy aaah!
            srcTuple.blockList.remove(block);
            if (srcTuple.getBlockList().isEmpty()) {
                tupleList.remove(srcTuple);
            }
        }

        Tuple dstTuple = getTuple(texture, currFaceBits);
        if (dstTuple == null) {
            dstTuple = new Tuple(texture, currFaceBits);
            tupleList.add(dstTuple);
            tupleList.sort(Tuple.TUPLE_COMP);
        }
        List<Block> blockList = dstTuple.blockList;
        blockList.add(block);
        blockList.sort(Block.UNIQUE_BLOCK_CMP);

        buffered = false;
    }

    /**
     * Transfer block between two tuples. Block will be transfered from tuple
     * with formFaceBits to tuple with current facebits.
     *
     * @param blkUnits blockUnits to transfer
     */
    public void transfer(IList<TransferUnit> blkUnits) { // update fluids use this to transfer fluid blocks between tuples
        for (TransferUnit unit : blkUnits) {
            Block block = unit.block;
            String texture = block.getTexName();

            Tuple srcTuple = getTuple(texture, unit.bitsBefore);
            if (srcTuple != null) { // lazy aaah!
                srcTuple.blockList.remove(block);
                if (srcTuple.getBlockList().isEmpty()) {
                    tupleList.remove(srcTuple);
                }
            }

            Tuple dstTuple = getTuple(texture, unit.bitsAfter);
            if (dstTuple == null) {
                dstTuple = new Tuple(texture, unit.bitsAfter);
                tupleList.add(dstTuple);
                tupleList.sort(Tuple.TUPLE_COMP);
            }
            List<Block> blockList = dstTuple.blockList;
            blockList.add(block);
            blockList.sort(Block.UNIQUE_BLOCK_CMP);
        }

        buffered = false;
    }

    /**
     * Updates blocks faces of both original block and adjacent blocks.
     *
     * Used after add operation.
     *
     * @param block block to update
     */
    protected void updateForAdd(Block block) {
        // only same solidity - solid to solid or fluid to fluid is updated        
        int nbits = block.isSolid()
                ? LevelContainer.AllBlockMap.getNeighborSolidBits(block.pos)
                : LevelContainer.AllBlockMap.getNeighborFluidBits(block.pos);
        // if has neighbors (otherwise pointless)
        if (nbits != 0) {
            // retieve current neightbor bits      
            int faceBitsBefore = block.getFaceBits();
            // -------------------------------------------------------------------
            // this logic updates facebits of this block
            // & transfers it to correct tuple
            // -------------------------------------------------------------------
            block.setFaceBits(~nbits & 63);
            int faceBitsAfter = block.getFaceBits();
            if (faceBitsBefore != faceBitsAfter) {
                transfer(block, faceBitsBefore, faceBitsAfter);
            }

            IList<TransferUnit> units = new GapList<>();
            // query all neighbors and update this block and adjacent blocks accordingly
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
                    int tupleBits = adjNBits & (~mask & 63);
                    Tuple tuple = getTuple(tupleTexName, tupleBits);
                    Block adjBlock = null;
                    if (tuple != null) {
                        adjBlock = Chunk.getBlock(tuple, adjPos, blkId);
                    }
                    if (adjBlock != null) {
                        int adjFaceBitsBefore = adjBlock.getFaceBits();
                        adjBlock.setFaceBits(~adjNBits & 63);
                        int adjFaceBitsAfter = adjBlock.getFaceBits();
                        if (adjFaceBitsBefore != adjFaceBitsAfter) {
                            // if bits changed, i.e. some face(s) got disabled
                            // tranfer to correct tuple
                            units.add(new TransferUnit(adjBlock, adjFaceBitsBefore, adjFaceBitsAfter));
                        }
                    }
                }

            }

            // batch transfer
            if (!units.isEmpty()) {
                transfer(units);
            }
        }
    }

    /**
     * Updates blocks faces of both original block and adjacent blocks.
     *
     * Used after removal operation.
     *
     * @param block block to update
     */
    protected void updateForRem(Block block) {
        IList<TransferUnit> units = new GapList<>();
        // setSafeCheck adjacent blocks
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
            TexByte location = LevelContainer.AllBlockMap.getLocation(adjPos);
            int nBits = block.isSolid()
                    ? LevelContainer.AllBlockMap.getNeighborSolidBits(block.pos)
                    : LevelContainer.AllBlockMap.getNeighborFluidBits(block.pos);
            // location exists and has neighbors (otherwise pointless)
            if (location != null && nBits != 0) {
                int blkId = location.blkId;
                String tupleTexName = location.texName;
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int mask = 1 << k;
                // revert the bit that was set in LevelContainer
                //(looking for old bits i.e. current tuple)
                int tupleBits = nBits & (~mask & 63);

                Tuple tuple = getTuple(tupleTexName, tupleBits);
                Block adjBlock = null;
                if (tuple != null) {
                    adjBlock = Chunk.getBlock(tuple, adjPos, blkId);
                }
                if (adjBlock != null) {
                    int adjFaceBitsBefore = adjBlock.getFaceBits();
                    adjBlock.setFaceBits(~nBits & 63);
                    int adjFaceBitsAfter = adjBlock.getFaceBits();
                    if (adjFaceBitsBefore != adjFaceBitsAfter) {
                        // if bits changed, i.e. some face(s) got disabled
                        // tranfer to correct tuple
                        units.add(new TransferUnit(adjBlock, adjFaceBitsBefore, adjFaceBitsAfter));
                    }
                }
            }

        }

        if (!units.isEmpty()) {
            transfer(units);
        }
    }

    /**
     * Add block to the chunk.
     *
     * @param block block to add
     */
    public void addBlock(Block block) {
        String blockTexture = block.getTexName();
        int blockFaceBits = block.getFaceBits();
        Tuple tuple = getTuple(blockTexture, blockFaceBits);

        if (tuple == null) {
            tuple = new Tuple(blockTexture, blockFaceBits);
            tupleList.add(tuple);
            tupleList.sort(Tuple.TUPLE_COMP);
        }
        List<Block> blockList = tuple.getBlockList();
        blockList.add(block);
        buffered = false;
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
        updateForAdd(block);
    }

    /**
     * Remove block from the chunk.
     *
     * @param block block to remove
     */
    public void removeBlock(Block block) {
        String blockTexture = block.getTexName();
        int blockFaceBits = block.getFaceBits();
        Tuple target = getTuple(blockTexture, blockFaceBits);
        if (target != null) {
            target.getBlockList().remove(block);
            buffered = false;
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
                    DSLogger.reportError("Could not add the block!", null);
                }

            }
            updateForRem(block);
        }
    }

    // hint that stuff should be buffered again
    public void unbuffer() {
        if (!CacheModule.isCached(id)) {
            buffered = false;
        }
    }

    // renderer does this stuff prior to any rendering
    public void bufferAll() {
        if (!CacheModule.isCached(id)) {
            for (Tuple tuple : tupleList) {
                tuple.bufferAll();
            }
            buffered = true;
        }
    }

    public void animate() { // call only for fluid blocks
        for (Tuple tuple : tupleList) {
            tuple.animate();
        }
    }

    public void prepare(boolean cameraInFluid) { // call only for fluid blocks before rendering        
        for (Tuple tuple : tupleList) {
            if (!tuple.isSolid()) {
                tuple.prepare(cameraInFluid);
            }
        }
    }

    // it renders all of them instanced if they're visible
    public void render(ShaderProgram shaderProgram, LightSources lightSources) {
        if (buffered && shaderProgram != null && !tupleList.isEmpty() && timeToLive > 0.0) {
            GameObject gameObj;
            try {
                gameObj = GameObject.getInstance();
                for (Tuple tuple : tupleList) {
                    tuple.renderInstanced(shaderProgram, lightSources, gameObj.waterRenderer.texture(), gameObj.shadowRenderer.texture());
                }
            } catch (Exception ex) {
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }
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
     * Determine which chunks are visible by this chunk.If visible put into the
     * V list, otherwise put into the I list.
     *
     * @param vChnkIdList visible chunk queue
     * @param iChnkIdList invisible chunk queue
     * @param camera (Observer) camera
     *
     * @return list of changed chunks
     */
    public static boolean determineVisible(IList<Integer> vChnkIdList, IList<Integer> iChnkIdList, Camera camera) {
        final IList<Integer> before = vChnkIdList.copy();

        vChnkIdList.clear();
        iChnkIdList.clear();

        // current chunk where player is        
        int currChunkId = chunkFunc(camera.pos);
        int currCol = currChunkId % GRID_SIZE;
        int currRow = currChunkId / GRID_SIZE;

        if (!vChnkIdList.contains(currChunkId)) {
            vChnkIdList.add(currChunkId);
        }

        // rest of the chunks
        for (int chunkId = 0; chunkId < Chunk.CHUNK_NUM; chunkId++) {
            if (chunkId != currChunkId) {
                int col = chunkId % GRID_SIZE;
                int row = chunkId / GRID_SIZE;

                int deltaCol = Math.abs(currCol - col);
                int deltaRow = Math.abs(currRow - row);

                if (deltaCol <= 1 && deltaRow <= 1) {
                    vChnkIdList.add(chunkId);
                } else if (!iChnkIdList.contains(chunkId)) {
                    iChnkIdList.add(chunkId);
                }

            }
        }

        final IList<Integer> after = vChnkIdList;
        boolean changed = !Objects.equals(before, after);

        return changed;
    }

    public IList<Block> getBlockList() {
        IList<Block> result = new BigList<>();
        for (Tuple tuple : tupleList) {
            result.addAll(tuple.getBlockList());
        }
        return result;
    }

    public void clear() {
        this.tupleList.forEach(t -> t.blockList.clear());
        this.tupleList.clear();
    }

    public int getId() {
        return id;
    }

    public IList<Tuple> getTupleList() {
        return tupleList;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    public float getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(float timeToLive) {
        this.timeToLive = timeToLive;
    }

    public void decTimeToLive(float timeDec) {
        this.timeToLive -= timeDec;
        if (this.timeToLive < 0.0f) {
            this.timeToLive = 0.0f;
        }
    }

    public boolean isAlive() {
        return timeToLive > 0.0f;
    }

}
