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
package rs.alexanderstojanovich.evg.level;

import java.util.Arrays;
import java.util.List;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Chunk implements Comparable<Chunk> { // some operations are mutually exclusive    

    // MODULATOR, DIVIDER, VISION are used in chunkCheck and for determining visible chunks
    public static final int BOUND = 512;
    public static final float VISION = 256.0f; // determines visibility
    public static final int GRID_SIZE = 4;

    public static final float STEP = 1.0f / (float) (GRID_SIZE);
    public static final int CHUNK_NUM = GRID_SIZE * GRID_SIZE;
    public static final float LENGTH = BOUND * STEP * 2.0f;

    // id of the chunk (signed)
    private final int id;

    // is a group of blocks which are prepared for instanced rendering
    // where each tuple is considered as:                
    //--------------------------MODULATOR--------DIVIDER--------VISION-------D--------E-----------------------------
    //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
    private final IList<Tuple> tupleList = new GapList<>();

    private Texture waterTexture;

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
        String key = Vector3fUtils.blockSpecsToUniqueString(tuple.isSolid(), tuple.texName(), pos);
        int left = 0;
        int right = tuple.blockList.size() - 1;
        int startIndex = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            Block candidate = tuple.blockList.get(mid);
            String candInt = Vector3fUtils.blockSpecsToUniqueString(candidate.isSolid(), candidate.getTexName(), candidate.pos);
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
            String candInt = Vector3fUtils.blockSpecsToUniqueString(candidate.isSolid(), candidate.getTexName(), candidate.pos);
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
     * Updates blocks faces of both original block and adjacent blocks.
     *
     * Used after add operation.
     *
     * @param block block to update
     */
    protected void updateForAdd(Block block) {
        // only same solidity - solid to solid or fluid to fluid is updated        
        int nbits = block.isSolid()
                ? LevelContainer.ALL_BLOCK_MAP.getNeighborSolidBits(block.pos)
                : LevelContainer.ALL_BLOCK_MAP.getNeighborFluidBits(block.pos);
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
            // query all neighbors and update this block and adjacent blocks accordingly
            for (int j = Block.LEFT; j <= Block.FRONT; j++) {
                // -------------------------------------------------------------------
                // following logic updates adjacent block 
                // if it is same solidity as this block
                // need to find tuple where it is located
                // -------------------------------------------------------------------
                Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
                TexByte location = LevelContainer.ALL_BLOCK_MAP.getLocation(adjPos);
                if (location != null) {
                    String tupleTexName = location.getTexName();
                    int adjNBits = block.isSolid()
                            ? LevelContainer.ALL_BLOCK_MAP.getNeighborSolidBits(adjPos)
                            : LevelContainer.ALL_BLOCK_MAP.getNeighborFluidBits(adjPos);
                    int k = ((j & 1) == 0 ? j + 1 : j - 1);
                    int mask = 1 << k;
                    int tupleBits = adjNBits ^ (~mask & 63);
                    Tuple tuple = getTuple(tupleTexName, tupleBits);
                    Block adjBlock = null;
                    if (tuple != null) {
                        adjBlock = Chunk.getBlock(tuple, adjPos);
                    }
                    if (adjBlock != null && adjBlock.pos.equals(adjPos)) {
                        int adjFaceBitsBefore = adjBlock.getFaceBits();
                        adjBlock.setFaceBits(~adjNBits & 63);
                        int adjFaceBitsAfter = adjBlock.getFaceBits();
                        if (adjFaceBitsBefore != adjFaceBitsAfter) {
                            // if bits changed, i.e. some face(s) got disabled
                            // tranfer to correct tuple
                            transfer(adjBlock, adjFaceBitsBefore, adjFaceBitsAfter);
                        }
                    }
                }
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
        // check adjacent blocks
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
            TexByte location = LevelContainer.ALL_BLOCK_MAP.getLocation(adjPos);
            int nBits = block.isSolid()
                    ? LevelContainer.ALL_BLOCK_MAP.getNeighborSolidBits(block.pos)
                    : LevelContainer.ALL_BLOCK_MAP.getNeighborFluidBits(block.pos);
            // location exists and has neighbors (otherwise pointless)
            if (location != null && nBits != 0) {
                String tupleTexName = location.getTexName();
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int mask = 1 << k;
                // revert the bit that was set in LevelContainer
                //(looking for old bits i.e. current tuple)
                int tupleBits = nBits ^ (~mask & 63);

                Tuple tuple = getTuple(tupleTexName, tupleBits);
                Block adjBlock = null;
                if (tuple != null) {
                    adjBlock = Chunk.getBlock(tuple, adjPos);
                }
                if (adjBlock != null) {
                    int adjFaceBitsBefore = adjBlock.getFaceBits();
                    adjBlock.setFaceBits(~nBits & 63);
                    int adjFaceBitsAfter = adjBlock.getFaceBits();
                    if (adjFaceBitsBefore != adjFaceBitsAfter) {
                        // if bits changed, i.e. some face(s) got disabled
                        // tranfer to correct tuple
                        transfer(adjBlock, adjFaceBitsBefore, adjFaceBitsAfter);
                    }
                }
            }
        }
    }

    @Deprecated
    protected void updateSolids() {
        for (Block solidBlock : getBlockList()) {
            if (GameObject.MY_WINDOW.shouldClose()) {
                break;
            }

            int faceBitsBefore = solidBlock.getFaceBits();
            TexByte pair = LevelContainer.ALL_BLOCK_MAP.getLocation(solidBlock.pos);
            if (pair != null && pair.isSolid()) {
                byte neighborBits = pair.getByteValue();
                solidBlock.setFaceBits(~neighborBits & 63);
                int faceBitsAfter = solidBlock.getFaceBits();
                if (faceBitsBefore != faceBitsAfter) { // if bits changed, i.e. some face(s) got disabled
                    transfer(solidBlock, faceBitsBefore, faceBitsAfter);
                }
            }
        }
    }

    @Deprecated
    protected void updateFluids() {
        for (Block fluidBlock : getBlockList()) {
            if (GameObject.MY_WINDOW.shouldClose()) {
                break;
            }

            int faceBitsBefore = fluidBlock.getFaceBits();
            TexByte pair = LevelContainer.ALL_BLOCK_MAP.getLocation(fluidBlock.pos);
            if (pair != null && !pair.isSolid()) {
                byte neighborBits = pair.getByteValue();
                fluidBlock.setFaceBits(~neighborBits & 63);
                int faceBitsAfter = fluidBlock.getFaceBits();
                if (faceBitsBefore != faceBitsAfter) { // if bits changed, i.e. some face(s) got disabled
                    transfer(fluidBlock, faceBitsBefore, faceBitsAfter);
                }
            }
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
        // check if it's light block
        LightSource lightSource = new LightSource(block.pos, block.getPrimaryColor(), LightSource.DEFAULT_LIGHT_INTENSITY);
        if (block.getTexName().equals("reflc")
                && !LevelContainer.LIGHT_SOURCES.getLightSrcList().contains(lightSource)) {
            LevelContainer.LIGHT_SOURCES.getLightSrcList().add(lightSource);
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
            // check if it's light block
            if (block.getTexName().equals("reflc")) {
                LevelContainer.LIGHT_SOURCES.getLightSrcList().removeIf(ls -> ls.getPos().equals(block.pos));
            }
            updateForRem(block);

            buffered = false;
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

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);
            GL20.glEnableVertexAttribArray(3);
            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            GL20.glEnableVertexAttribArray(6);
            GL20.glEnableVertexAttribArray(7);

            for (Tuple tuple : tupleList) {
                tuple.renderInstanced(shaderProgram, lightSources, waterTexture);
            }

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
            GL20.glDisableVertexAttribArray(3);
            GL20.glDisableVertexAttribArray(4);
            GL20.glDisableVertexAttribArray(5);
            GL20.glDisableVertexAttribArray(6);
            GL20.glDisableVertexAttribArray(7);
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

        // check which column of the interval
        int col = Math.round(nx * (1.0f / STEP - 1.0f));

        // check which rows of the interval
        int row = Math.round(nz * (1.0f / STEP - 1.0f));

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
     * Determine which chunks are visible by this chunk. If visible put into the
     * V list, otherwise put into the I list.
     *
     * @param vChnkIdList visible chunk queue
     * @param iChnkIdList invisible chunk queue
     * @param actorPos actor pos (self-expl)
     * @param camFront camera front (vision)
     *
     * @return list of changed chunks
     */
    public static boolean determineVisible(IList<Integer> vChnkIdList, IList<Integer> iChnkIdList, Vector3f actorPos, Vector3f camFront) {
        final Object[] before = vChnkIdList.toArray();

        vChnkIdList.clear();
        iChnkIdList.clear();

        // current chunk where player is        
        int currChunkId = chunkFunc(actorPos);
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
                /*
                Vector3f chunkPos = Chunk.invChunkFunc(chunkId);
                Vector2f result = new Vector2f();
                Vector3f temp1 = new Vector3f();
                Vector3f temp2 = new Vector3f();
                Intersectionf.intersectRayAab(actorPos, camFront, chunkPos.add(new Vector3f(LENGTH / 2.0f), temp1), chunkPos.sub(new Vector3f(LENGTH / 2.0f), temp2), result);
                boolean visible = Math.max(result.x, result.y) <= VISION;
                 */
                if (deltaCol <= 1 && deltaRow <= 1 && !vChnkIdList.contains(chunkId)) {
                    vChnkIdList.add(chunkId);
                } else if (!iChnkIdList.contains(chunkId)) {
                    iChnkIdList.add(chunkId);
                }

            }
        }

        final Object[] after = vChnkIdList.toArray();
        boolean changed = !Arrays.equals(before, after);

        return changed;
    }

    public List<Block> getBlockList() {
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

    public List<Tuple> getTupleList() {
        return tupleList;
    }

    public Texture getWaterTexture() {
        return waterTexture;
    }

    public void setWaterTexture(Texture waterTexture) {
        this.waterTexture = waterTexture;
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
