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
package rs.alexanderstojanovich.evg.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.chunk.Chunks;
import rs.alexanderstojanovich.evg.chunk.Tuple;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.VectorFloatUtils;

/**
 * Cache Module is used for caching chunks to not overweight Game Renderer.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class CacheModule {

    public static final int TEX_LEN = 5; // 5 B
    public static final int VEC3_LEN = 12; // 12 B
    public static final int VEC4_LEN = 16; // 16 B
    public static final int BOOL_LEN = 1; // 1 B

    public static final int BLOCK_SIZE = 34;

    public static final int DEFAULT_BUFFER_SIZE = 32768; // 32 KB
    private static final byte[] MEMORY = new byte[0x1000000]; // 16 MB
    private static int pos = 0;
    private final LevelContainer levelContainer;
    public static final IList<CachedInfo> CACHED_CHUNKS = new GapList<>();
    public static final int BLOCKS_PER_RUN = Configuration.getInstance().getBlocksPerRun();

    public CacheModule(LevelContainer levelContainer) {
        this.levelContainer = levelContainer;
    }

    /**
     * Size of the chunk (in blocks) when is loaded.
     *
     * @param id chunk id
     *
     * @return loaded size of the chunk
     */
    public int loadedSize(int id) { // for debugging purposes
        int size = 0;
        if (!CacheModule.isCached(id)) {
            Chunk chunk = this.levelContainer.chunks.getChunk(id);
            if (chunk != null) {
                for (Tuple tuple : chunk.getTupleList()) {
                    size += tuple.getBlockList().size();
                }
            }
        }
        return size;
    }

    /**
     * Size of the chunk (in blocks) when is loaded.
     *
     * @param chunk chunk itself
     *
     * @return loaded size of that chunk
     */
    public static int loadedSize(Chunk chunk) {
        int size = 0;
        if (!CacheModule.isCached(chunk.getId())) {
            for (Tuple tuple : chunk.getTupleList()) {
                size += tuple.getBlockList().size();
            }
        }
        return size;
    }

    /**
     * Size of the chunk in blocks when is cached.
     *
     * @param id chunk id
     *
     * @return loaded size of the chunk
     */
    public static int cachedSize(int id) { // for debugging purposes
        int size = 0;
        if (CacheModule.isCached(id)) {
            CachedInfo info = CACHED_CHUNKS.getIf(ci -> ci.chunkId == id);
            if (info != null) {
                size = info.blockSize;
            }
        }
        return size;
    }

    // total loaded + cached size
    public int totalSize() {
        int result = 0;
        for (int id = 0; id < Chunk.CHUNK_NUM; id++) {
            Chunk chunk;
            if (CacheModule.isCached(id)) {
                result += CacheModule.cachedSize(id);
            } else {
                chunk = levelContainer.chunks.getChunk(id);
                if (chunk != null) {
                    result += loadedSize(id);
                }
            }
        }
        return result;
    }

    // total loaded + cached size
    public static int totalSize(Chunks chunks) {
        int result = 0;
        for (int id = 0; id < Chunk.CHUNK_NUM; id++) {
            Chunk chunk;
            if (CacheModule.isCached(id)) {
                result += CacheModule.cachedSize(id);
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
            bos = new BufferedOutputStream(new FileOutputStream(file), DEFAULT_BUFFER_SIZE);
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
     *
     */
    private void loadDiskToMem(String filename, int length) {
        File file = new File(filename);
        if (file.exists()) {
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE);
                bis.read(MEMORY, 0, length);
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

        }
    }

    private static String getFileName(int id) {
        return Game.CACHE + File.separator + "chnk" + (id < 0 ? "m" + (-id) : id) + ".cache";
    }

    /**
     * Save chunk to Disk.
     *
     * @param id chunk id
     * @return is operation performed (chunks modified)
     */
    public boolean saveToDisk(int id) {
        boolean op = false;
        if (!CacheModule.isCached(id)) {
            List<Block> blocks = null;
            // DETERMINING WHICH CHUNK
            Chunk chunk = this.levelContainer.chunks.getChunk(id);
            // REMOVE OPREATIONS
            if (chunk != null) {
                chunk.unbuffer();

                blocks = chunk.getBlockList();

                // better than tuples clear (otherwise much slower to load)
                // this indicates that add with no transfer on fluid blocks will be used!
                for (Tuple tuple : chunk.tupleList) {
                    tuple.blockList.clear();
                }
                chunk.tupleList.clear();
                this.levelContainer.chunks.getChunkList().remove(chunk);
            }
            // SAVE OPERATIONS
            if (blocks != null) {
                pos = 0;
                MEMORY[pos++] = (byte) id;
                MEMORY[pos++] = (byte) blocks.size();
                MEMORY[pos++] = (byte) (blocks.size() >> 8);
                for (Block block : blocks) {
//                    TexByte location = LevelContainer.AllBlockMap.getLocation(block.pos);
//                    if (location != null) {
//                        LevelContainer.AllBlockMap.removeLocation(block.pos);
//                    }

                    byte[] texName = block.getTexName().getBytes();
                    System.arraycopy(texName, 0, MEMORY, pos, TEX_LEN);
                    pos += TEX_LEN;
                    VectorFloatUtils.vec3fToByteArray(block.pos, MEMORY, pos);
                    pos += VEC3_LEN;
                    Vector4f primCol = block.getPrimaryRGBAColor();
                    VectorFloatUtils.vec4fToByteArray(primCol, MEMORY, pos);
                    pos += VEC4_LEN;
                    if (block.isSolid()) {
                        MEMORY[pos] = (byte) 0xFF;
                    } else {
                        MEMORY[pos] = (byte) 0x00;
                    }
                    pos++;
                }
                final int cachedSize = pos;

                File cacheDir = new File(Game.CACHE);
                if (!cacheDir.exists()) {
                    cacheDir.mkdir();
                }

                String fileName = getFileName(id);
                saveMemToDisk(fileName);
//                DSLogger.reportInfo("pos=" + pos, null);

                // ADD TO CACHED
                CACHED_CHUNKS.add(new CachedInfo(id, blocks.size(), cachedSize, fileName));
                DSLogger.reportDebug("ChunkId=" + id + " cached to " + fileName, null);
                op = true;
            }
        }

        return op;
    }

    /**
     * Load chunk from Disk.
     *
     * @param id chunk id
     * @return is operation performed (chunks modified)
     */
    public boolean loadFromDisk(int id) {
        boolean op = false;
        // IF ITS NOT CACHED TO DISK
        if (CacheModule.isCached(id)) {
            // LOAD INTO MEMORY
            String fileName = getFileName(id);
            CachedInfo cachedInfo = CACHED_CHUNKS.getIf(ci -> ci.chunkId == id);
            int remainingSize = cachedInfo.cachedSize - cachedInfo.readBytes;
            loadDiskToMem(fileName, remainingSize);
            int len = 0;
            if (remainingSize == cachedInfo.cachedSize) {
                pos = 1;
                // INIT BLOCK ARRAY
                len = ((MEMORY[pos + 1] & 0xFF) << 8) | (MEMORY[pos] & 0xFF);
                pos += 2;
                cachedInfo.readBytes += 3;
            } else {
                pos = cachedInfo.readBytes;
                len = cachedInfo.blockSize - cachedInfo.readBlocks;
            }
            // READ BLOCK ARRAY                                              
            int runlen = Math.min(len, BLOCKS_PER_RUN);
            for (int i = 0; i < runlen; i++) {
                char[] texNameArr = new char[TEX_LEN];
                for (int k = 0; k < texNameArr.length; k++) {
                    texNameArr[k] = (char) MEMORY[pos++];
                }
                String texName = String.valueOf(texNameArr);

                Vector3f blockPos = VectorFloatUtils.vec3fFromByteArray(MEMORY, pos);
                pos += VEC3_LEN;

                Vector4f blockCol = VectorFloatUtils.vec4fFromByteArray(MEMORY, pos);
                pos += VEC4_LEN;

                boolean solid = MEMORY[pos] != (byte) 0x00;
                pos++;
                Block block = new Block(texName, blockPos, blockCol, solid);
                // PUT ALL BLOCK WHERE THEY BELONG TO                
                levelContainer.chunks.addBlock(block);

                cachedInfo.readBytes += BLOCK_SIZE;
                cachedInfo.readBlocks++;
            }

            // REMOVE FROM CACHED
            if (cachedInfo.cachedSize - cachedInfo.readBytes == 0) {
                CACHED_CHUNKS.removeIf(ci -> ci.chunkId == id);
                DSLogger.reportDebug("ChunkId=" + id + " restored from " + fileName, null);
                File file = new File(fileName);
                if (file.exists()) {
                    file.delete();
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

        CACHED_CHUNKS.clear();
    }

    /**
     * Is cached.
     *
     * @param chunkId chunk id
     * @return is chunk cached or not cached (loaded)
     */
    public static boolean isCached(int chunkId) {
        return CACHED_CHUNKS.containsIf(ci -> ci.chunkId == chunkId);
    }

    public static int getPos() {
        return pos;
    }

    public LevelContainer getLevelContainer() {
        return levelContainer;
    }

    public static IList<CachedInfo> getCACHED_CHUNKS() {
        return CACHED_CHUNKS;
    }

}
