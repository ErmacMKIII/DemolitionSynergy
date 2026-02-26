/*
 * Copyright (C) 2022 Aleksandar Stojanovic <coas91@rocketmail.com>
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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.VectorFloatUtils;

/**
 * Cache Module is used for caching chunks to not overweight Game Renderer.
 * Uses NIO FileChannel for fast bulk I/O operations.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class CacheModule {

    public static final int TEX_LEN = 5;   // 5 B
    public static final int VEC3_LEN = 12; // 12 B
    public static final int VEC4_LEN = 16; // 16 B
    public static final int BOOL_LEN = 1;  // 1 B

    // Layout per block: TEX_LEN + VEC3_LEN + VEC4_LEN + BOOL_LEN = 34 B
    public static final int BLOCK_SIZE = TEX_LEN + VEC3_LEN + VEC4_LEN + BOOL_LEN; // 34 B

    // Header layout: INT(blockCount=4) = 4 B
    public static final int HEADER_SIZE = Integer.BYTES; // 4 B

    public static final int DEFAULT_BUFFER_SIZE = 65536; // 64 KB
    public static final int MEMORY_SIZE = 0x1000000;     // 16 MB

    // Per-instance direct ByteBuffer — avoids shared-state concurrency issues
    private final ByteBuffer MEMORY = MemoryUtil.memCalloc(MEMORY_SIZE);

    private final LevelContainer levelContainer;
    public static final IList<CachedInfo> CACHED_CHUNKS = new GapList<>();
    public static final int BLOCKS_PER_RUN = Configuration.getInstance().getBlocksPerRun();

    public CacheModule(LevelContainer levelContainer) {
        this.levelContainer = levelContainer;
    }

    /**
     * Size of the chunk (in blocks) when loaded (not cached).
     *
     * @param id chunk id
     * @return loaded size of the chunk
     */
    public int loadedSize(int id) {
        int size = 0;
        if (!CacheModule.isCached(id)) {
            IList<Block> blkList = levelContainer.chunks.getBlockList(id);
            size += blkList.size();
        }
        return size;
    }

    /**
     * Size of the chunk in blocks when cached.
     *
     * @param id chunk id
     * @return cached size of the chunk (block count)
     */
    public static int cachedSize(int id) {
        if (CacheModule.isCached(id)) {
            CachedInfo info = CACHED_CHUNKS.getIf(ci -> ci.chunkId == id);
            if (info != null) {
                return info.blockSize;
            }
        }
        return 0;
    }

    /**
     * Return total loaded + cached block count.
     *
     * @return total (loaded + cached) size
     */
    public int totalSize() {
        int result = 0;
        for (int id = 0; id < Chunk.CHUNK_NUM; id++) {
            if (CacheModule.isCached(id)) {
                result += CacheModule.cachedSize(id);
            } else {
                result += levelContainer.chunks.getBlockList(id).size();
            }
        }
        return result;
    }

    //--------------------------------------------------------------------------
    /**
     * Write MEMORY buffer to disk via NIO FileChannel (fast bulk write).
     *
     * @param filename target cache file path
     */
    private void saveMemToDisk(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
             FileChannel fc = raf.getChannel()) {
            // MEMORY is already flipped (limit = data size, position = 0)
            while (MEMORY.hasRemaining()) {
                fc.write(MEMORY);
            }
            fc.force(false); // flush OS page cache to disk
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            MEMORY.clear();
        }
    }

    /**
     * Read disk file into MEMORY buffer via NIO FileChannel (fast bulk read).
     *
     * @param filename source cache file path
     * @param length   number of bytes to read
     */
    private void loadDiskToMem(String filename, int length) {
        File file = new File(filename);
        if (!file.exists()) {
            return;
        }
        MEMORY.clear();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel fc = raf.getChannel()) {
            // Limit how many bytes we pull into MEMORY
            MEMORY.limit(Math.min(length, MEMORY_SIZE));
            while (MEMORY.hasRemaining()) {
                int read = fc.read(MEMORY);
                if (read == -1) break;
            }
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            MEMORY.flip(); // prepare for reading
        }
    }

    private static String getFileName(int id) {
        return Game.CACHE + File.separator + "chnk" + (id < 0 ? "m" + (-id) : id) + ".cache";
    }

    /**
     * Save chunk to disk using NIO bulk I/O.
     *
     * @param id chunk id
     * @return true if chunk was saved, false if already cached or empty
     */
    public boolean saveToDisk(int id) {
        if (CacheModule.isCached(id)) {
            return false;
        }

        IList<Block> blocks = levelContainer.chunks.getBlockList(id);
        if (blocks.isEmpty()) {
            return false;
        }

        // Ensure cache directory exists
        File cacheDir = new File(Game.CACHE);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }

        final int blockCount = blocks.size();

        // Pre-calculate required buffer size: header + blocks
        final int requiredBytes = HEADER_SIZE + blockCount * BLOCK_SIZE;
        if (requiredBytes > MEMORY_SIZE) {
            DSLogger.reportError("Chunk " + id + " too large to cache (" + requiredBytes + " B)", null);
            return false;
        }

        MEMORY.clear();

        // --- Write header ---
        MEMORY.putInt(blockCount);

        // --- Write blocks in bulk ---
        for (Block blk : blocks) {
            // texName: exactly TEX_LEN bytes
            byte[] texBytes = blk.texName.getBytes();
            MEMORY.put(texBytes, 0, Math.min(texBytes.length, TEX_LEN));
            // pad if shorter than TEX_LEN
            for (int p = texBytes.length; p < TEX_LEN; p++) {
                MEMORY.put((byte) 0);
            }
            // position (12 B)
            MEMORY.put(VectorFloatUtils.vec3fToByteArray(blk.pos));
            // color (16 B)
            MEMORY.put(VectorFloatUtils.vec4fToByteArray(blk.getPrimaryRGBAColor()));
            // solid flag (1 B)
            MEMORY.put(blk.isSolid() ? (byte) 0xFF : (byte) 0x00);
        }

        MEMORY.flip(); // prepare for writing
        final int cachedSize = MEMORY.limit();

        String fileName = getFileName(id);
        saveMemToDisk(fileName);

        // Remove blocks from tuples efficiently using a Set-based removal
        final IList<Block> blockSnapshot = blocks;
        levelContainer.chunks.tupleList.forEach(t -> t.blockList.removeIf(blockSnapshot::contains));

        CACHED_CHUNKS.add(new CachedInfo(id, blockCount, cachedSize, fileName));
        DSLogger.reportDebug("ChunkId=" + id + " cached to " + fileName + " (" + blockCount + " blocks, " + cachedSize + " B)", null);

        return true;
    }

    /**
     * Load chunk from disk using NIO bulk I/O.
     * Supports partial loading controlled by BLOCKS_PER_RUN.
     *
     * @param id chunk id
     * @return true if any blocks were loaded, false if not cached
     */
    public boolean loadFromDisk(int id) {
        if (!CacheModule.isCached(id)) {
            return false;
        }

        String fileName = getFileName(id);
        CachedInfo cachedInfo = CACHED_CHUNKS.getIf(ci -> ci.chunkId == id);
        if (cachedInfo == null) {
            return false;
        }

        // Calculate how many blocks remain to read
        int blocksRemaining = cachedInfo.blockSize - cachedInfo.readBlocks;
        if (blocksRemaining <= 0) {
            // Nothing left — clean up
            finalizeCacheEntry(id, cachedInfo, fileName);
            return false;
        }

        // Determine run size for this call
        int runLen = Math.min(blocksRemaining, BLOCKS_PER_RUN);

        // Calculate byte offset for this run: header + already-read blocks
        int byteOffset = HEADER_SIZE + cachedInfo.readBlocks * BLOCK_SIZE;
        int bytesToRead = runLen * BLOCK_SIZE;

        // Load only the needed slice from disk
        loadDiskToMem(fileName, byteOffset + bytesToRead);

        if (MEMORY.limit() < byteOffset + bytesToRead) {
            DSLogger.reportError("Cache file " + fileName + " is shorter than expected", null);
            return false;
        }

        // Seek to the block data offset
        MEMORY.position(byteOffset);

        // --- Read blocks in bulk ---
        for (int i = 0; i < runLen; i++) {
            // texName (TEX_LEN bytes)
            byte[] texBytes = new byte[TEX_LEN];
            MEMORY.get(texBytes);
            String texName = new String(texBytes).trim();

            // position (12 B)
            byte[] vec3fPosBytes = new byte[VEC3_LEN];
            MEMORY.get(vec3fPosBytes);
            Vector3f blockPos = VectorFloatUtils.vec3fFromByteArray(vec3fPosBytes);

            // color (16 B) — fix: was never read from MEMORY in original code!
            byte[] vec4fColBytes = new byte[VEC4_LEN];
            MEMORY.get(vec4fColBytes);
            Vector4f blockCol = VectorFloatUtils.vec4fFromByteArray(vec4fColBytes);

            // solid flag (1 B)
            boolean solid = MEMORY.get() != (byte) 0x00;

            Block block = new Block(texName, blockPos, blockCol, solid);
            levelContainer.chunks.addBlock(block);

            cachedInfo.readBytes += BLOCK_SIZE;
            cachedInfo.readBlocks++;
        }

        MEMORY.clear();

        // If all blocks have been read, remove from cache registry and delete file
        if (cachedInfo.readBlocks >= cachedInfo.blockSize) {
            finalizeCacheEntry(id, cachedInfo, fileName);
        }

        return true;
    }

    /**
     * Remove cache entry from registry and delete the cache file.
     */
    private void finalizeCacheEntry(int id, CachedInfo cachedInfo, String fileName) {
        CACHED_CHUNKS.removeIf(ci -> ci.chunkId == id);
        DSLogger.reportDebug("ChunkId=" + id + " fully restored from " + fileName
                + " (" + cachedInfo.blockSize + " blocks)", null);
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Delete all cache files and clear registry.
     */
    public static void deleteCache() {
        File cache = new File(Game.CACHE);
        if (cache.exists()) {
            File[] files = cache.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            cache.delete();
        }
        CACHED_CHUNKS.clear();
    }

    /**
     * Release native MEMORY buffer.
     */
    public void release() {
        MemoryUtil.memFree(MEMORY);
    }

    /**
     * Check whether a chunk is currently cached to disk.
     *
     * @param chunkId chunk id
     * @return true if cached, false if loaded in memory
     */
    public static boolean isCached(int chunkId) {
        return CACHED_CHUNKS.containsIf(ci -> ci.chunkId == chunkId);
    }

    public LevelContainer getLevelContainer() {
        return levelContainer;
    }

    public static IList<CachedInfo> getCACHED_CHUNKS() {
        return CACHED_CHUNKS;
    }
}
