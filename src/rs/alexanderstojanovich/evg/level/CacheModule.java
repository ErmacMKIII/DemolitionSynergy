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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Chunk;
import rs.alexanderstojanovich.evg.models.Chunks;
import rs.alexanderstojanovich.evg.models.Tuple;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class CacheModule {

    private static final byte[] MEMORY = new byte[0x1000000]; // 16 MB
    private static int pos = 0;
    private final LevelContainer levelContainer;

    public CacheModule(LevelContainer levelContainer) {
        this.levelContainer = levelContainer;
    }

    /**
     * Size of the chunk when is loaded.
     *
     * @param id chunk id
     * @param solid is chunk solid or fluid
     *
     * @return loaded size of the chunk
     */
    public int loadedSize(int id, boolean solid) { // for debugging purposes
        int size = 0;
        if (!CacheModule.isCached(id, solid)) {
            Chunk chunk = (solid)
                    ? this.levelContainer.solidChunks.getChunk(id)
                    : this.levelContainer.fluidChunks.getChunk(id);
            if (chunk != null) {
                for (Tuple tuple : chunk.getTupleList()) {
                    size += tuple.getBlockList().size();
                }
            }
        }
        return size;
    }

    /**
     * Size of the chunk when is loaded.
     *
     * @param chunk chunk itself
     *
     * @return loaded size of that chunk
     */
    public static int loadedSize(Chunk chunk) {
        int size = 0;
        if (!CacheModule.isCached(chunk.getId(), chunk.isSolid())) {
            for (Tuple tuple : chunk.getTupleList()) {
                size += tuple.getBlockList().size();
            }
        }
        return size;
    }

    /**
     * Size of the chunk when is cached.
     *
     * @param id chunk id
     * @param solid is chunk solid or fluid
     *
     * @return loaded size of the chunk
     */
    public static int cachedSize(int id, boolean solid) { // for debugging purposes
        int size = 0;
        if (CacheModule.isCached(id, solid)) {
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

    // total loaded + cached size
    public int totalSize(boolean solid) {
        int result = 0;
        for (int id = 0; id < Chunk.CHUNK_NUM; id++) {
            Chunk chunk;
            if (CacheModule.isCached(id, solid)) {
                result += CacheModule.cachedSize(id, solid);
            } else {
                chunk = (solid)
                        ? levelContainer.solidChunks.getChunk(id)
                        : levelContainer.fluidChunks.getChunk(id);
                if (chunk != null) {
                    result += loadedSize(id, solid);
                }
            }
        }
        return result;
    }

    // total loaded + cached size
    public static int totalSize(Chunks chunks, boolean solid) {
        int result = 0;
        for (int id = 0; id < Chunk.CHUNK_NUM; id++) {
            Chunk chunk;
            if (CacheModule.isCached(id, solid)) {
                result += CacheModule.cachedSize(id, solid);
            } else {
                chunk = chunks.getChunk(id);
                if (chunk != null) {
                    result += loadedSize(chunk);
                }
            }
        }
        return result;
    }

    //--------------------------------------------------------------------------
    /**
     * Save Memory Buffer to Disk (SSD or HardDrive)
     *
     * @param filename filename of cached file to save
     */
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

    /**
     * Load Memory Buffer from Disk (SSD or HardDrive)
     *
     * @param filename filename of cached file to load
     */
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

    private static String getFileName(int id, boolean solid) {
        return Game.CACHE + File.separator + (solid ? "s" : "f") + "chnk" + (id < 0 ? "m" + (-id) : id) + ".cache";
    }

    /**
     * Save chunk to Disk.
     *
     * @param id chunk id
     * @param solid is chunk solid or fluid
     * @return is operation performed (chunks modified)
     */
    public boolean saveToDisk(int id, boolean solid) {
        boolean op = false;
        if (!CacheModule.isCached(id, solid)) {
            List<Block> blocks = null;
            // DETERMINING WHICH CHUNK
            Chunk chunk = (solid)
                    ? this.levelContainer.solidChunks.getChunk(id)
                    : this.levelContainer.fluidChunks.getChunk(id);
            // REMOVE OPREATIONS
            if (chunk != null) {
                chunk.unbuffer();

                blocks = chunk.getBlockList();

                // better than tuples clear (otherwise much slower to load)
                // this indicates that add with no transfer on fluid blocks will be used!
                for (Tuple tuple : chunk.getTupleList()) {
                    tuple.getBlockList().clear();
                }
                chunk.getTupleList().clear();

                if (solid) {
                    this.levelContainer.solidChunks.getChunkList().remove(chunk);
                } else {
                    this.levelContainer.fluidChunks.getChunkList().remove(chunk);
                }
            }
            // SAVE OPERATIONS
            if (blocks != null) {
                pos = 0;
                MEMORY[pos++] = (byte) id;
                MEMORY[pos++] = (byte) blocks.size();
                MEMORY[pos++] = (byte) (blocks.size() >> 8);
                for (Block block : blocks) {
                    byte[] texName = block.getTexName().getBytes();
                    System.arraycopy(texName, 0, MEMORY, pos, 5);
                    pos += 5;
                    byte[] somePos = Vector3fUtils.vec3fToByteArray(block.getPos());
                    System.arraycopy(somePos, 0, MEMORY, pos, somePos.length);
                    pos += somePos.length;
                    Vector3f primCol = block.getPrimaryColor();
                    byte[] someCol = Vector3fUtils.vec3fToByteArray(primCol);
                    System.arraycopy(someCol, 0, MEMORY, pos, someCol.length);
                    pos += someCol.length;
                }

                File cacheDir = new File(Game.CACHE);
                if (!cacheDir.exists()) {
                    cacheDir.mkdir();
                }

                saveMemToDisk(getFileName(id, solid));
                op = true;
            }
        }

        return op;
    }

    /**
     * Load chunk from Disk.
     *
     * @param id chunk id
     * @param solid is chunk solid or fluid
     * @return is operation performed (chunks modified)
     */
    public boolean loadFromDisk(int id, boolean solid) {
        boolean op = false;
        // IF ITS NOT CACHED TO DISK
        if (CacheModule.isCached(id, solid)) {
            // LOAD INTO MEMORY
            loadDiskToMem(getFileName(id, solid));
            pos = 1;
            // INIT BLOCK ARRAY
            int len = ((MEMORY[pos + 1] & 0xFF) << 8) | (MEMORY[pos] & 0xFF);
            Block[] blocks = new Block[len];
            pos += 2;
            // READ BLOCK ARRAY
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
                blocks[i] = block;
            }

            // PUT ALL BLOCK WHERE THEY BELONG TO
            if (solid) {
                for (Block block : blocks) {
                    levelContainer.solidChunks.addBlock(block, true);
                }
            } else {
                for (Block block : blocks) {
                    levelContainer.fluidChunks.addBlock(block, true);
                }
            }
            op = true;
        }

        return op;
    }

    /**
     * Delete all the cache files.
     */
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

    /**
     * Is cached.
     *
     * @param chunkId chunk id
     * @param solid is chunk solid or fluid
     * @return is chunk cached or not cached (loaded)
     */
    public static boolean isCached(int chunkId, boolean solid) {
        File file = new File(getFileName(chunkId, solid));
        return file.exists();
    }
}
