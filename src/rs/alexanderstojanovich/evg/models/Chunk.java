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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Chunk { // some operations are mutually exclusive    

    // MODULATOR, DIVIDER, VISION are used in chunkCheck and for determining visible chunks
    public static final int ABS_BOUND = Math.round(LevelContainer.SKYBOX_WIDTH / 2.0f); // modulator
    public static final int DIVIDER = 16; // divider -> number of chunks is calculated as (2 * MODULATOR + 1) / DIVIDER
    public static final int CHUNKS_NUM = 2 * (ABS_BOUND / DIVIDER) + 1;

    public static final float VISION = 100.0f; // determines visibility

    // id of the chunk (signed)
    private final int id;
    private final boolean solid;

    // is a group of blocks which are prepared for instanced rendering
    // where each tuple is considered as:                
    //--------------------------MODULATOR--------DIVIDER--------VISION-------D--------E-----------------------------
    //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
    private final Set<Tuple> tupleSet = new HashSet<>();

    private Texture waterTexture;

    private boolean buffered = false;

    private static final byte[] MEMORY = new byte[0x100000];
    private static int pos = 0;
    private boolean cached = false;

    private double timeToLive = 0.0;

    public Chunk(int id, boolean solid) {
        this.id = id;
        this.solid = solid;
    }

    private Tuple getTuple(String keyTexture, Integer keyFaceBits) {
        Tuple result = null;
        for (Tuple tuple : tupleSet) {
            if (tuple.getTexName().equals(keyTexture)
                    && tuple.getFaceEnBits() == keyFaceBits) {
                result = tuple;
                break;
            }
        }
        return result;
    }

    public void transfer(Block fluidBlock, int formFaceBits, int currFaceBits) { // update fluids use this to transfer fluid blocks between tuples
        String fluidTexture = fluidBlock.texName;

        Tuple srcTuple = getTuple(fluidTexture, formFaceBits);
        if (srcTuple != null) { // lazy aaah!
            srcTuple.getBlocks().getBlockList().remove(fluidBlock);
            if (srcTuple.getBlocks().getBlockList().isEmpty()) {
                tupleSet.remove(srcTuple);
            }
        }

        Tuple dstTuple = getTuple(fluidTexture, currFaceBits);
        if (dstTuple == null) {
            dstTuple = new Tuple(fluidTexture, currFaceBits);
            tupleSet.add(dstTuple);
        }
        dstTuple.getBlocks().getBlockList().add(fluidBlock);
        dstTuple.getBlocks().getBlockList().sort(Block.Y_AXIS_COMP);

        buffered = false;
    }

    // for internal use, for load disk method (does not add to LevelContainer)
    private void addBlock(Block block) {
        String blockTexture = block.texName;
        int blockFaceBits = block.getFaceBits();
        Tuple tuple = getTuple(blockTexture, blockFaceBits);

        if (tuple == null) {
            tuple = new Tuple(blockTexture, blockFaceBits);
            tupleSet.add(tuple);
        }

        tuple.getBlocks().getBlockList().add(block);
        tuple.getBlocks().getBlockList().sort(Block.Y_AXIS_COMP);

        buffered = false;
    }

    public void addBlock(Block block, boolean useLevelContainer) {
        if (useLevelContainer) {
            LevelContainer.putBlock(block);
        }

        String blockTexture = block.texName;
        int blockFaceBits = block.getFaceBits();
        Tuple tuple = getTuple(blockTexture, blockFaceBits);

        if (tuple == null) {
            tuple = new Tuple(blockTexture, blockFaceBits);
            tupleSet.add(tuple);
        }

        tuple.getBlocks().getBlockList().add(block);
        tuple.getBlocks().getBlockList().sort(Block.Y_AXIS_COMP);

        buffered = false;
    }

    public void removeBlock(Block block, boolean useLevelContainer) {
        if (useLevelContainer) {
            LevelContainer.removeBlock(block);
        }

        String blockTexture = block.texName;
        int blockFaceBits = block.getFaceBits();
        Tuple target = getTuple(blockTexture, blockFaceBits);
        if (target != null) {
            target.getBlocks().getBlockList().remove(block);
            buffered = false;
            // if tuple has no blocks -> remove it
            if (target.getBlocks().getBlockList().isEmpty()) {
                tupleSet.remove(target);
            }
        }
    }

    // hint that stuff should be buffered again
    public void unbuffer() {
        if (!cached) {
            buffered = false;
        }
    }

    // renderer does this stuff prior to any rendering
    public void bufferAll() {
        if (!cached) {
            for (Tuple tuple : tupleSet) {
                tuple.buffer();
            }
            buffered = true;
        }
    }

    public void animate() { // call only for fluid blocks
        for (Tuple tuple : tupleSet) {
            tuple.getBlocks().animate();
        }
    }

    public void prepare() { // call only for fluid blocks before rendering        
        for (Tuple tuple : tupleSet) {
            Blocks blocks = tuple.getBlocks();
            blocks.prepare();
        }
    }

    // set camera in fluid for underwater effects (call only for fluid)
    public void setCameraInFluid(boolean cameraInFluid) {
        for (Tuple tuple : tupleSet) {
            tuple.getBlocks().setCameraInFluid(cameraInFluid);
        }
    }

    // it renders all of them instanced if they're visible
    public void render(ShaderProgram shaderProgram, Vector3f lightSrc) {
        if (buffered && shaderProgram != null && !tupleSet.isEmpty() && timeToLive > 0.0) {
            Texture.enable();

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);
            GL20.glEnableVertexAttribArray(3);
            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            GL20.glEnableVertexAttribArray(6);
            GL20.glEnableVertexAttribArray(7);

            for (Tuple tuple : tupleSet) {
                tuple.render(shaderProgram, solid, lightSrc, waterTexture);
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
        if (!cached) {
            //--------------------------MODULATOR--------DIVIDER--------VISION-------D--------E-----------------------------
            //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
            for (Tuple tuple : tupleSet) {
                tuple.release();
            }
            buffered = false;
        }
    }

    // determine chunk (where am I)
    public static int chunkFunc(Vector3f pos) {
        return Math.round(((pos.x + pos.y + pos.z) / (3.0f * DIVIDER)));
    }

    // determine if chunk is visible
    public static int chunkFunc(Vector3f actorPos, Vector3f actorFront) {
        float x = VISION * actorFront.x + actorPos.x;
        float y = VISION * actorFront.y + actorPos.y;
        float z = VISION * actorFront.z + actorPos.z;

        return Math.round(((x + y + z) / (3.0f * DIVIDER)));
    }

    // determine where chunk position might be based on the chunkId
    public static Vector3f chunkInverFunc(int chunkId) {
        float component = chunkId * DIVIDER;
        return new Vector3f(component, component, component);
    }

    // determine which chunks are visible by this chunk
    public static void determineVisible(Set<Integer> visibleSet, Vector3f actorPos, Vector3f actorFront) {
        final int val = CHUNKS_NUM / 2 - 1;
        // current chunk where player is
        int cid = chunkFunc(actorPos);
        Vector3f temp = new Vector3f();
        // this is for other chunks
        for (int id = -val; id <= val; id++) {
            Vector3f chunkPos = chunkInverFunc(id);
            float product = chunkPos.sub(actorPos, temp).normalize(temp).dot(actorFront);
            float distance = chunkPos.distance(actorPos);
            if (id == cid && distance <= VISION
                    || id != cid && distance <= VISION && product >= 0.5f) {
                visibleSet.add(id);
            } else {
                visibleSet.remove(id);
            }
        }
    }

    public int size() { // for debugging purposes
        int size = 0;
        if (cached) {
            size = (pos + 1 - 3) / 29;
        } else {
            for (Tuple tuple : tupleSet) {
                size += tuple.getBlocks().getBlockList().size();
            }
        }
        return size;
    }

    public List<Block> getList() {
        List<Block> result = new GapList<>();
        for (Tuple tuple : tupleSet) {
            result.addAll(tuple.getBlocks().getBlockList());
        }
        return result;
    }

    private synchronized void saveMemToDisk(String filename) {
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

    private synchronized void loadDiskToMem(String filename) {
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

    public void saveToDisk() {
        if (!cached) {
            List<Block> blocks = getList();
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
            for (Tuple tuple : tupleSet) {
                tuple.getBlocks().getBlockList().clear();
            }

            File cacheDir = new File(Game.CACHE);
            if (!cacheDir.exists()) {
                cacheDir.mkdir();
            }

            saveMemToDisk(getFileName());

            tupleSet.clear();

            cached = true;
        }
    }

    public void loadFromDisk() {
        if (cached) {
            long time0 = System.nanoTime();
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
                addBlock(block);
            }
            cached = false;
            long time1 = System.nanoTime();
            System.out.println("Chunk " + id + " solid = " + solid + " time = " + (time1 - time0) / 1E6D);
        }
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

    public int getId() {
        return id;
    }

    public boolean isSolid() {
        return solid;
    }

    public Set<Tuple> getTupleSet() {
        return tupleSet;
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

    public byte[] getMemory() {
        return MEMORY;
    }

    public int getPos() {
        return pos;
    }

    public boolean isCached() {
        return cached;
    }

    public double getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(double timeToLive) {
        this.timeToLive = timeToLive;
    }

    public void decTimeToLive() {
        if (timeToLive > 0.0) {
            timeToLive--;
        } else {
            timeToLive = 0.0;
        }
    }

    public boolean isAlive() {
        return timeToLive > 0.0;
    }

}
