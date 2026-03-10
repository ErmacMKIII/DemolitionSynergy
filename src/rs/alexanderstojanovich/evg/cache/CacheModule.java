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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
 * Thread-safe: uses thread-local buffers and concurrent registry.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class CacheModule {

    // Data layout per block:
    // texName: fixed-length string (TEX_LEN bytes)
    // position: 3 floats (VEC3_LEN bytes)
    //  color: 4 floats (VEC4_LEN bytes)
    //  solid flag: 1 byte (BOOL_LEN)

    public static final int TEX_LEN = 5;   // 5 B
    public static final int VEC3_LEN = 12; // 12 B
    public static final int VEC4_LEN = 16; // 16 B
    public static final int BOOL_LEN = 1;  // 1 B

    // Layout per block: TEX_LEN + VEC3_LEN + VEC4_LEN + BOOL_LEN = 34 B
    public static final int BLOCK_SIZE = TEX_LEN + VEC3_LEN + VEC4_LEN + BOOL_LEN; // 34 B

    // Header layout: INT(blockCount=4) = 4 B
    /**
     * Total per-block size on disk: 4 B (header) + N * 34 B (blocks)
     * Example: 100 blocks = 4 + 100*34 = 3404 B
     */
    public static final int HEADER_SIZE = Integer.BYTES; // 4 B

    public static final int DEFAULT_BUFFER_SIZE = 65536; // 64 KB

    /** Maximum chunk size in bytes that can be cached (must fit in MEMORY_SIZE).
     * For example, with BLOCK_SIZE=34 B, 16 MB can hold ~488,281 blocks.
     */
    public static final int MEMORY_SIZE = 0x1000000;     // 16 MB

    /**
     * Thread-local direct ByteBuffer — each thread gets its own buffer,
     * eliminating concurrent access issues entirely.
     */
    private static final ThreadLocal<ByteBuffer> THREAD_LOCAL_MEMORY = ThreadLocal.withInitial(()
            -> MemoryUtil.memCalloc(MEMORY_SIZE)
    );

    /**
     * Thread-safe registry of currently cached chunks.
     * Key = chunkId, Value = CachedInfo
     */
    public static final ConcurrentHashMap<Integer, CachedInfo> CACHED_CHUNKS_MAP = new ConcurrentHashMap<>();

    /**
     * Per-chunk file write lock — prevents two threads from writing/reading
     * the same chunk file simultaneously.
     */
    private static final ConcurrentHashMap<Integer, ReentrantReadWriteLock> CHUNK_LOCKS = new ConcurrentHashMap<>();

    private final LevelContainer levelContainer;
    public static final int BLOCKS_PER_RUN = Configuration.getInstance().getBlocksPerRun();

    public CacheModule(LevelContainer levelContainer) {
        this.levelContainer = levelContainer;
    }

    /**
     * Get or create a per-chunk read/write lock.
     */
    private static ReentrantReadWriteLock getLockForChunk(int id) {
        return CHUNK_LOCKS.computeIfAbsent(id, k -> new ReentrantReadWriteLock());
    }

    /**
     * Get the thread-local MEMORY buffer (cleared and ready for use).
     */
    private static ByteBuffer getMemory() {
        ByteBuffer mem = THREAD_LOCAL_MEMORY.get();
        mem.clear();
        return mem;
    }

    // -------------------------------------------------------------------------
    /**
     * Size of the chunk (in blocks) when loaded (not cached).
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
     */
    public static int cachedSize(int id) {
        CachedInfo info = CACHED_CHUNKS_MAP.get(id);
        return (info != null) ? info.blockSize : 0;
    }

    /**
     * Return total loaded + cached block count.
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

    // -------------------------------------------------------------------------
    /**
     * Write the given buffer to disk via NIO FileChannel (fast bulk write).
     * The buffer must already be flipped (limit = data size, position = 0).
     *
     * @param filename target cache file path
     * @param buffer   data buffer to write (already flipped)
     */
    private void saveMemToDisk(String filename, ByteBuffer buffer) {
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
             FileChannel fc = raf.getChannel()) {
            while (buffer.hasRemaining()) {
                fc.write(buffer);
            }fc.force(false); // flush OS page cache to disk
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
    }

    /**
     * Read disk file into the given buffer via NIO FileChannel (fast bulk read).
     * Prepares buffer for reading (flipped) after completion.
     *
     * @param filename source cache file path
     * @param length   max number of bytes to read
     * @param buffer   target buffer (will be cleared first)
     */
    private void loadDiskToMem(String filename, int length, ByteBuffer buffer) {
        File file = new File(filename);
        if (!file.exists()) {
            buffer.flip();
            return;
        }
        buffer.clear();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel fc = raf.getChannel()) {
            buffer.limit(Math.min(length, MEMORY_SIZE));
            while (buffer.hasRemaining()) {
                int read = fc.read(buffer);
                if (read == -1) break;
            }
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            buffer.flip(); // prepare for reading
        }
    }

    private static String getFileName(int id) {
        return Game.CACHE + File.separator + "chnk" + (id < 0 ? "m" + (-id) : id) + ".cache";
    }

    /**
     * Save chunk to disk using NIO bulk I/O.
     * Thread-safe: acquires write lock for the chunk id.
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
            cacheDir.mkdirs();
        }

        final int blockCount = blocks.size();
        final int requiredBytes = HEADER_SIZE + blockCount * BLOCK_SIZE;
        if (requiredBytes > MEMORY_SIZE) {
            DSLogger.reportError("Chunk " + id + " too large to cache (" + requiredBytes + " B)", null);
            return false;
        }

        ReentrantReadWriteLock lock = getLockForChunk(id);
        lock.writeLock().lock();
        try {
            // Double-check after acquiring lock
            if (CacheModule.isCached(id)) {
                return false;
            }

            ByteBuffer memory = getMemory();

            // --- Write header ---
            memory.putInt(blockCount);

            // --- Write blocks in bulk ---
            for (Block blk : blocks) {
                // texName: exactly TEX_LEN bytes
                byte[] texBytes = blk.texName.getBytes();
                memory.put(texBytes, 0, Math.min(texBytes.length, TEX_LEN));
                for (int p = texBytes.length; p < TEX_LEN; p++) {
                    memory.put((byte) 0);
                }
                // position (12 B)
                memory.put(VectorFloatUtils.vec3fToByteArray(blk.pos));
                // color (16 B)
                memory.put(VectorFloatUtils.vec4fToByteArray(blk.getPrimaryRGBAColor()));
                // solid flag (1 B)
                memory.put(blk.isSolid() ? (byte) 0xFF : (byte) 0x00);
            }

            memory.flip(); // prepare for writing
            final int cachedSize = memory.limit();

            String fileName = getFileName(id);
            saveMemToDisk(fileName, memory);

            // Remove blocks from tuples
            final IList<Block> blockSnapshot = blocks;
            levelContainer.chunks.tupleList.forEach(t -> t.blockList.removeIf(blockSnapshot::contains));

            CACHED_CHUNKS_MAP.put(id, new CachedInfo(id, blockCount, cachedSize, fileName));
            DSLogger.reportDebug("ChunkId=" + id + " cached to " + fileName
                    + " (" + blockCount + " blocks, " + cachedSize + " B)", null);

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Load chunk from disk using NIO bulk I/O.
     * Supports partial loading controlled by BLOCKS_PER_RUN.
     * Thread-safe: acquires read lock for file access, write lock for registry update.
     *
     * @param id chunk id
     * @return true if any blocks were loaded, false if not cached
     */
    public boolean loadFromDisk(int id) {
        if (!CacheModule.isCached(id)) {
            return false;
        }

        ReentrantReadWriteLock lock = getLockForChunk(id);
        lock.writeLock().lock();
        try {
            // Re-check under lock
            CachedInfo cachedInfo = CACHED_CHUNKS_MAP.get(id);
            if (cachedInfo == null) {
                return false;
            }

            String fileName = getFileName(id);

            // Check if nothing remains to read
            int blocksRemaining = cachedInfo.blockSize - cachedInfo.readBlocks;
            if (blocksRemaining <= 0) {
                finalizeCacheEntry(id, cachedInfo, fileName);
                return false;
            }

            // Determine run size for this call
            int runLen = Math.min(blocksRemaining, BLOCKS_PER_RUN);

            // Calculate byte offset: header + already-read blocks
            int byteOffset = HEADER_SIZE + cachedInfo.readBlocks * BLOCK_SIZE;
            int bytesToRead = runLen * BLOCK_SIZE;
            int totalNeeded = byteOffset + bytesToRead;

            ByteBuffer memory = getMemory();
            loadDiskToMem(fileName, totalNeeded, memory);

            if (memory.limit() < totalNeeded) {
                DSLogger.reportError("Cache file " + fileName + " is shorter than expected"
                        + " (needed=" + totalNeeded + ", got=" + memory.limit() + ")", null);
                return false;
            }

            // Seek to the block data offset
            memory.position(byteOffset);

            // --- Read blocks in bulk ---
            for (int i = 0; i < runLen; i++) {
                // texName (TEX_LEN bytes)
                byte[] texBytes = new byte[TEX_LEN];
                memory.get(texBytes);
                String texName = new String(texBytes).trim();

                // position (12 B)
                byte[] vec3fPosBytes = new byte[VEC3_LEN];
                memory.get(vec3fPosBytes);
                Vector3f blockPos = VectorFloatUtils.vec3fFromByteArray(vec3fPosBytes);

                // color (16 B)
                byte[] vec4fColBytes = new byte[VEC4_LEN];
                memory.get(vec4fColBytes);
                Vector4f blockCol = VectorFloatUtils.vec4fFromByteArray(vec4fColBytes);

                // solid flag (1 B)
                boolean solid = memory.get() != (byte) 0x00;

                Block block = new Block(texName, blockPos, blockCol, solid);
                levelContainer.chunks.addBlock(block);

                cachedInfo.readBytes += BLOCK_SIZE;
                cachedInfo.readBlocks++;
            }

            // If all blocks have been read, finalize
            if (cachedInfo.readBlocks >= cachedInfo.blockSize) {
                finalizeCacheEntry(id, cachedInfo, fileName);
            }

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove cache entry from registry and delete the cache file.
     */
    private void finalizeCacheEntry(int id, CachedInfo cachedInfo, String fileName) {
        CACHED_CHUNKS_MAP.remove(id);
        CHUNK_LOCKS.remove(id);
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
        CACHED_CHUNKS_MAP.clear();
        CHUNK_LOCKS.clear();
    }

    /**
     * Release thread-local native MEMORY buffer for the current thread.
     * Call this when the owning thread is shutting down.
     */
    public static void releaseThreadLocalMemory() {
        ByteBuffer buf = THREAD_LOCAL_MEMORY.get();
        if (buf != null) {
            MemoryUtil.memFree(buf);
            THREAD_LOCAL_MEMORY.remove();
        }
    }

    /**
     * Check whether a chunk is currently cached to disk.
     */
    public static boolean isCached(int chunkId) {
        return CACHED_CHUNKS_MAP.containsKey(chunkId);
    }

    /**
     * Legacy list accessor — returns a snapshot GapList of cached infos.
     * Prefer using CACHED_CHUNKS_MAP directly.
     */
    public static IList<CachedInfo> CACHED_CHUNKS() {
        IList<CachedInfo> list = new GapList<>();
        list.addAll(CACHED_CHUNKS_MAP.values());
        return list;
    }

    public LevelContainer getLevelContainer() {
        return levelContainer;
    }
}
