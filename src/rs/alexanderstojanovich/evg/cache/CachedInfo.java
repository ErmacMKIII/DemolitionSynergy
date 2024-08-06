/*
 * Copyright (C) 2023 Alexander Stojanovich <coas91@rocketmail.com>
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

/**
 * Contains info about cached chunk. Most of the info can be read from this
 * class (instance).
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class CachedInfo {

    public final int chunkId;
    public final int blockSize;
    public final int cachedSize;
    public final String fileName;
    protected int readBytes = 0;
    protected int readBlocks = 0;

    /**
     * Create new cached chunk (descriptor) info
     *
     * @param chunkId which chunk id to cache
     * @param blockSize
     * @param cachedSize info about cached size
     * @param fileName file name (binary) which contains cache
     */
    public CachedInfo(int chunkId, int blockSize, int cachedSize, String fileName) {
        this.chunkId = chunkId;
        this.blockSize = blockSize;
        this.cachedSize = cachedSize;
        this.fileName = fileName;
    }

    public int getChunkId() {
        return chunkId;
    }

    public int getCachedSize() {
        return cachedSize;
    }

    public String getFileName() {
        return fileName;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getReadBytes() {
        return readBytes;
    }

    public void setReadBytes(int readBytes) {
        this.readBytes = readBytes;
    }

    public int getReadBlocks() {
        return readBlocks;
    }

    public void setReadBlocks(int readBlocks) {
        this.readBlocks = readBlocks;
    }

}
