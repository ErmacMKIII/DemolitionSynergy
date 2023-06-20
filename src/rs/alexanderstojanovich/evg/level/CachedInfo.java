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
package rs.alexanderstojanovich.evg.level;

/**
 * Contains info about cached chunk. Most of the info can be read from this
 * class (instance).
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class CachedInfo {

    public final int chunkId;
    public final int chunkSize;
    public final String fileName;

    /**
     * Create new cached chunk (descriptor) info
     *
     * @param chunkId which chunk id to cache
     * @param chunkSize info about cached size
     * @param fileName file name (binary) which contains cache
     */
    public CachedInfo(int chunkId, int chunkSize, String fileName) {
        this.chunkId = chunkId;
        this.chunkSize = chunkSize;
        this.fileName = fileName;
    }

    public int getChunkId() {
        return chunkId;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public String getFileName() {
        return fileName;
    }

}
