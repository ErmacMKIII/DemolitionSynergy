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
package rs.alexanderstojanovich.evg.models;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Pair;
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Chunk implements Comparable<Chunk> { // some operations are mutually exclusive    

    // MODULATOR, DIVIDER, VISION are used in chunkCheck and for determining visible chunks
    public static final int BOUND = 1000;
    public static final float VISION = 250.0f; // determines visibility
    private static final int GRID_SIZE = 3;

    public static final float STEP = 1.0f / (float) (GRID_SIZE);
    public static final int CHUNK_NUM = GRID_SIZE * GRID_SIZE;
    public static final float LENGTH = BOUND * STEP;

    // id of the chunk (signed)
    private final int id;
    private final boolean solid;

    // is a group of blocks which are prepared for instanced rendering
    // where each tuple is considered as:                
    //--------------------------MODULATOR--------DIVIDER--------VISION-------D--------E-----------------------------
    //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
    private final List<Tuple> tupleList = new GapList<>();

    private Texture waterTexture;

    private boolean buffered = false;

    private static final byte[] MEMORY = new byte[0x1000000]; // 16 MB
    private static int pos = 0;

    private float timeToLive = 0.0f;

    public Chunk(int id, boolean solid) {
        this.id = id;
        this.solid = solid;
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
    private Tuple getTuple(String keyTexture, Integer keyFaceBits) {
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
     * Transfer block between two tuples. Block will be transfered from tuple
     * with formFaceBits to tuple with current facebits.
     *
     * @param block block to transfer
     * @param formFaceBits face bits before
     * @param currFaceBits face bits current (after the change)
     */
    public void transfer(Block block, int formFaceBits, int currFaceBits) { // update fluids use this to transfer fluid blocks between tuples
        String texture = block.texName;

        Tuple srcTuple = getTuple(texture, formFaceBits);
        if (srcTuple != null) { // lazy aaah!
            srcTuple.getBlockList().remove(block);
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
        List<Block> blockList = dstTuple.getBlockList();
        blockList.add(block);
        blockList.sort(Block.Y_AXIS_COMP);

        buffered = false;
    }

    /**
     * Updates blocks faces of both original block and adjacent blocks. Block
     * must be solid.
     *
     * Used after add operation.
     *
     * @param block block to update
     */
    private void updateSolidForAdd(Block block) {
        int faceBitsBefore = block.getFaceBits();
        Pair<String, Byte> pair = LevelContainer.ALL_SOLID_MAP.get(block.pos);
        if (pair != null) {
            byte neighborBits = pair.getValue();
            block.setFaceBits(~neighborBits & 63);
            int faceBitsAfter = block.getFaceBits();
            if (faceBitsBefore != faceBitsAfter) {
                // if bits changed, i.e. some face(s) got disabled
                // tranfer to correct tuple
                transfer(block, faceBitsBefore, faceBitsAfter);
                // check adjacent blocks
                for (int j = Block.LEFT; j <= Block.FRONT; j++) {
                    Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
                    Pair<String, Byte> adjPair = LevelContainer.ALL_SOLID_MAP.get(adjPos);
                    if (adjPair != null) {
                        String tupleTexName = adjPair.getKey();
                        byte adjNBits = adjPair.getValue();
                        int k = ((j & 1) == 0 ? j + 1 : j - 1);
                        int mask = 1 << k;
                        // revert the bit that was set in LevelContainer
                        //(looking for old bits i.e. current tuple)
                        int tupleBits = adjNBits ^ (~mask & 63);

                        Tuple tuple = getTuple(tupleTexName, tupleBits);
                        Block adjBlock = null;
                        if (tuple != null) {
                            for (Block blk : tuple.blockList) {
                                if (blk.pos.equals(adjPos)) {
                                    adjBlock = blk;
                                    break;
                                }
                            }
                        }
                        if (adjBlock != null) {
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

    }

    /**
     * Updates blocks faces of both original block and adjacent blocks. Block
     * must be solid.
     *
     * Used after removal operation.
     *
     * @param block block to update
     */
    private void updateSolidForRem(Block block) {
        // check adjacent blocks
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
            Pair<String, Byte> adjPair = LevelContainer.ALL_SOLID_MAP.get(adjPos);
            if (adjPair != null) {
                String tupleTexName = adjPair.getKey();
                byte adjNBits = adjPair.getValue();
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int mask = 1 << k;
                // revert the bit that was set in LevelContainer
                //(looking for old bits i.e. current tuple)
                int tupleBits = adjNBits ^ (~mask & 63);

                Tuple tuple = getTuple(tupleTexName, tupleBits);
                Block adjBlock = null;
                if (tuple != null) {
                    for (Block blk : tuple.blockList) {
                        if (blk.pos.equals(adjPos)) {
                            adjBlock = blk;
                            break;
                        }
                    }
                }
                if (adjBlock != null) {
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

    /**
     * Updates blocks faces of both original block and adjacent blocks. Block
     * must be solid.
     *
     * Used after removal operation.
     *
     * @param block block to update
     */
    private void updateFluidForRem(Block block) {
        // check adjacent blocks
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
            Pair<String, Byte> adjPair = LevelContainer.ALL_FLUID_MAP.get(adjPos);
            if (adjPair != null) {
                String tupleTexName = adjPair.getKey();
                byte adjNBits = adjPair.getValue();
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int mask = 1 << k;
                // revert the bit that was set in LevelContainer
                //(looking for old bits i.e. current tuple)
                int tupleBits = adjNBits ^ (~mask & 63);

                Tuple tuple = getTuple(tupleTexName, tupleBits);
                Block adjBlock = null;
                if (tuple != null) {
                    for (Block blk : tuple.blockList) {
                        if (blk.pos.equals(adjPos)) {
                            adjBlock = blk;
                            break;
                        }
                    }
                }
                if (adjBlock != null) {
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

    @Deprecated
    public void updateSolids() {
        for (Block solidBlock : getBlockList()) {
            if (GameObject.getInstance().MY_WINDOW.shouldClose()) {
                break;
            }

            int faceBitsBefore = solidBlock.getFaceBits();
            Pair<String, Byte> pair = LevelContainer.ALL_SOLID_MAP.get(solidBlock.pos);
            if (pair != null) {
                byte neighborBits = pair.getValue();
                solidBlock.setFaceBits(~neighborBits & 63);
                int faceBitsAfter = solidBlock.getFaceBits();
                if (faceBitsBefore != faceBitsAfter) { // if bits changed, i.e. some face(s) got disabled
                    transfer(solidBlock, faceBitsBefore, faceBitsAfter);
                }
            }
        }
    }

    /**
     * Updates blocks faces of both original block and adjacent blocks. Block
     * must be fluid. Removal is used only if block is being removed.
     *
     * @param block block to update
     */
    private void updateFluidForAdd(Block block) {
        int faceBitsBefore = block.getFaceBits();
        Pair<String, Byte> pair = LevelContainer.ALL_FLUID_MAP.get(block.pos);
        if (pair != null) {
            byte neighborBits = pair.getValue();
            block.setFaceBits(~neighborBits & 63);
            int faceBitsAfter = block.getFaceBits();
            if (faceBitsBefore != faceBitsAfter) {
                // if bits changed, i.e. some face(s) got disabled
                // tranfer to correct tuple
                transfer(block, faceBitsBefore, faceBitsAfter);
                // check adjacent blocks
                for (int j = Block.LEFT; j <= Block.FRONT; j++) {
                    Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
                    Pair<String, Byte> adjPair = LevelContainer.ALL_FLUID_MAP.get(adjPos);
                    if (adjPair != null) {
                        String tupleTexName = adjPair.getKey();
                        byte adjNBits = adjPair.getValue();
                        int k = ((j & 1) == 0 ? j + 1 : j - 1);
                        int mask = 1 << k;
                        // revert the bit that was set in LevelContainer
                        //(looking for old bits i.e. current tuple)
                        int tupleBits = adjNBits ^ (~mask & 63);

                        Tuple tuple = getTuple(tupleTexName, tupleBits);
                        Block adjBlock = null;
                        if (tuple != null) {
                            for (Block blk : tuple.blockList) {
                                if (blk.pos.equals(adjPos)) {
                                    adjBlock = blk;
                                    break;
                                }
                            }
                        }
                        if (adjBlock != null) {
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

    }

    @Deprecated
    public void updateFluids() {
        for (Block fluidBlock : getBlockList()) {
            if (GameObject.getInstance().MY_WINDOW.shouldClose()) {
                break;
            }

            int faceBitsBefore = fluidBlock.getFaceBits();
            Pair<String, Byte> pair = LevelContainer.ALL_FLUID_MAP.get(fluidBlock.pos);
            if (pair != null) {
                byte neighborBits = pair.getValue();
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
     * @param useLevelContainer update level container environment map (for
     * adjacency)
     */
    public void addBlock(Block block, boolean useLevelContainer) {
        String blockTexture = block.texName;
        int blockFaceBits = block.getFaceBits();
        Tuple tuple = getTuple(blockTexture, blockFaceBits);

        if (tuple == null) {
            tuple = new Tuple(blockTexture, blockFaceBits);
            tupleList.add(tuple);
            tupleList.sort(Tuple.TUPLE_COMP);
        }

        List<Block> blockList = tuple.getBlockList();
        blockList.add(block);
        blockList.sort(Block.Y_AXIS_COMP);

        if (useLevelContainer) {
            // level container also set neighbor bits
            LevelContainer.putBlock(block);
            // update original block with neighbor blocks
            if (solid) {
                // check if it's light block
                if (block.getTexName().equals("reflc")
                        && !LevelContainer.LIGHT_SRC.contains(block.pos)) {
                    LevelContainer.LIGHT_SRC.add(new Vector3f(block.pos));
                }
                updateSolidForAdd(block);
            } else {
                updateFluidForAdd(block);
            }
        }

        buffered = false;
    }

    /**
     * Remove block from the chunk.
     *
     * @param block block to remove
     * @param useLevelContainer update level container environment map (for
     * adjacency)
     */
    public void removeBlock(Block block, boolean useLevelContainer) {
        String blockTexture = block.texName;
        int blockFaceBits = block.getFaceBits();
        Tuple target = getTuple(blockTexture, blockFaceBits);
        if (target != null) {
            target.getBlockList().remove(block);
            buffered = false;
            // if tuple has no blocks -> remove it
            if (target.getBlockList().isEmpty()) {
                tupleList.remove(target);
            }

            if (useLevelContainer) {
                // level container also set neighbor bits
                LevelContainer.removeBlock(block);
                // update original block with neighbor blocks
                if (solid) {
                    // check if it's light block
                    if (block.getTexName().equals("reflc")
                            && LevelContainer.LIGHT_SRC.contains(block.pos)) {
                        LevelContainer.LIGHT_SRC.remove(new Vector3f(block.pos));
                    }
                    updateSolidForRem(block);
                } else {
                    updateFluidForRem(block);
                }
            }
        }
    }

    // hint that stuff should be buffered again
    public void unbuffer() {
        if (!Chunk.isCached(id, solid)) {
            buffered = false;
        }
    }

    // renderer does this stuff prior to any rendering
    public void bufferAll() {
        if (!Chunk.isCached(id, solid)) {
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
            tuple.prepare(cameraInFluid);
        }
    }

    // it renders all of them instanced if they're visible
    public void render(ShaderProgram shaderProgram, List<Vector3f> lightSrc) {
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
                tuple.renderInstanced(shaderProgram, solid, lightSrc, waterTexture);
            }

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
            GL20.glDisableVertexAttribArray(3);
            GL20.glDisableVertexAttribArray(4);
            GL20.glDisableVertexAttribArray(5);
            GL20.glDisableVertexAttribArray(6);
            GL20.glDisableVertexAttribArray(7);

            Texture.disable();
        }
    }

    // deallocates Chunk from graphic card
    @Deprecated
    public void release() {
        if (!Chunk.isCached(id, solid)) {
            //--------------------------MODULATOR--------DIVIDER--------VISION-------D--------E-----------------------------
            //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
            for (Tuple tuple : tupleList) {
                tuple.release();
            }
            buffered = false;
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

    // determine which chunks are visible by this chunk
    public static void determineVisible(Queue<Pair<Integer, Float>> visibleQueue,
            Queue<Pair<Integer, Float>> invisibleQueue, Vector3f actorPos, Vector3f actorFront) {
        // current chunk where player is
        int currChunkId = chunkFunc(actorPos);
        visibleQueue.offer(new Pair<>(currChunkId, 0.0f));
        for (int id = 0; id < Chunk.CHUNK_NUM; id++) {
            if (id != currChunkId) {
                Vector3f chunkPos = invChunkFunc(id);
                float distance1 = chunkPos.distance(actorPos);
                if (distance1 <= VISION) {
                    visibleQueue.offer(new Pair<>(id, distance1));
                } else {
                    invisibleQueue.offer(new Pair<>(id, distance1));
                }
            }
        }
    }

    public int loadedSize() { // for debugging purposes
        int size = 0;
        if (!Chunk.isCached(id, solid)) {
            for (Tuple tuple : tupleList) {
                size += tuple.getBlockList().size();
            }
        }
        return size;
    }

    public static int cachedSize(int id, boolean solid) { // for debugging purposes
        int size = 0;
        if (Chunk.isCached(id, solid)) {
            try {
                FileInputStream fos = new FileInputStream(getFileName(id, solid));
                byte[] bytes = new byte[3];
                fos.read(bytes, 0, 3);
                size = ((bytes[2] & 0xFF) << 8) | (bytes[1] & 0xFF);
            } catch (FileNotFoundException ex) {
                DSLogger.reportError(ex.getMessage(), ex);
            } catch (IOException ex) {
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }
        return size;
    }

    public List<Block> getBlockList() {
        List<Block> result = new BigList<>();
        for (Tuple tuple : tupleList) {
            result.addAll(tuple.getBlockList());
        }
        return result;
    }

    private void saveMemToDisk(String filename) {
        BufferedOutputStream bos = null;
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(MEMORY, 0, pos);
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
        if (bos != null) {
            try {
                bos.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        }
    }

    private void loadDiskToMem(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(file));
                bis.read(MEMORY);
            } catch (FileNotFoundException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ex) {
                    DSLogger.reportFatalError(ex.getMessage(), ex);
                }
            }
            file.delete();
        }
    }

    private String getFileName() {
        return Game.CACHE + File.separator + (solid ? "s" : "f") + "chnk" + (id < 0 ? "m" + (-id) : id) + ".cache";
    }

    private static String getFileName(int id, boolean solid) {
        return Game.CACHE + File.separator + (solid ? "s" : "f") + "chnk" + (id < 0 ? "m" + (-id) : id) + ".cache";
    }

    public void saveToDisk() {
        if (!Chunk.isCached(id, solid)) {
            List<Block> blocks = getBlockList();
            pos = 0;
            MEMORY[pos++] = (byte) id;
            MEMORY[pos++] = (byte) blocks.size();
            MEMORY[pos++] = (byte) (blocks.size() >> 8);
            for (Block block : blocks) {
                byte[] texName = block.texName.getBytes();
                System.arraycopy(texName, 0, MEMORY, pos, 5);
                pos += 5;
                byte[] solidPos = Vector3fUtils.vec3fToByteArray(block.getPos());
                System.arraycopy(solidPos, 0, MEMORY, pos, solidPos.length);
                pos += solidPos.length;
                Vector3f primCol = block.getPrimaryColor();
                byte[] solidCol = Vector3fUtils.vec3fToByteArray(primCol);
                System.arraycopy(solidCol, 0, MEMORY, pos, solidCol.length);
                pos += solidCol.length;
            }

            // better than tuples clear (otherwise much slower to load)
            // this indicates that add with no transfer on fluid blocks will be used!
            for (Tuple tuple : tupleList) {
                tuple.getBlockList().clear();
            }

            File cacheDir = new File(Game.CACHE);
            if (!cacheDir.exists()) {
                cacheDir.mkdir();
            }

            saveMemToDisk(getFileName());

            tupleList.clear();
        }
    }

    public void loadFromDisk() {
        if (Chunk.isCached(id, solid)) {
            loadDiskToMem(getFileName());
            pos = 1;
            int len = ((MEMORY[pos + 1] & 0xFF) << 8) | (MEMORY[pos] & 0xFF);
            pos += 2;
            for (int i = 0; i < len; i++) {
                char[] texNameArr = new char[5];
                for (int k = 0; k < texNameArr.length; k++) {
                    texNameArr[k] = (char) MEMORY[pos++];
                }
                String texName = String.valueOf(texNameArr);

                byte[] blockPosArr = new byte[12];
                System.arraycopy(MEMORY, pos, blockPosArr, 0, blockPosArr.length);
                Vector3f blockPos = Vector3fUtils.vec3fFromByteArray(blockPosArr);
                pos += blockPosArr.length;

                byte[] blockPosCol = new byte[12];
                System.arraycopy(MEMORY, pos, blockPosCol, 0, blockPosCol.length);
                Vector3f blockCol = Vector3fUtils.vec3fFromByteArray(blockPosCol);
                pos += blockPosCol.length;

                Block block = new Block(texName, blockPos, blockCol, solid);
                addBlock(block, true);
            }
        }
    }

    public static Chunk loadFromDisk(int chunkId, boolean solid) {
        Chunk chunk = new Chunk(chunkId, solid);
        chunk.loadFromDisk();
        return chunk;
    }

    public static void deleteCache() {
        // deleting cache
        File cache = new File(Game.CACHE);
        if (cache.exists()) {
            for (File file : cache.listFiles()) {
                file.delete(); // deleting all chunk files
            }
            cache.delete();
        }
    }

    public static boolean isCached(int chunkId, boolean solid) {
        File file = new File(getFileName(chunkId, solid));
        return file.exists();
    }

    public int getId() {
        return id;
    }

    public boolean isSolid() {
        return solid;
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

    public static byte[] getMEMORY() {
        return MEMORY;
    }

    public int getPos() {
        return pos;
    }

    public float getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(float timeToLive) {
        this.timeToLive = timeToLive;
    }

    public void decTimeToLive(float timeDec) {
        if (timeDec > 0.0f) {
            this.timeToLive -= timeDec;
        } else {
            this.timeToLive = 0.0f;
        }
    }

    public boolean isAlive() {
        return timeToLive > 0.0f;
    }

}
