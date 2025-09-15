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

import org.joml.Vector3f;
import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.models.Block;

/**
 * World (container) of all the blocks.
 *
 * Contains all the various tuples (of textures x facebits combinations).
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Chunks {

    // single mat4Vbo is for model matrix shared amongst the vertices of the same instance    
    // single vec4Vbo is color shared amongst the vertices of the same instance    
    //--------------------------A--------B--------C-------D--------E-----------------------------
    //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
    /**
     * Tuple list (unique properties) for instanced rendering
     */
    public final IList<Tuple> tupleList = new GapList<>(Chunk.CHUNK_NUM);

    /**
     * Adds block to the chunks. Block will be added to the corresponding solid
     * chunk based on Chunk.chunkFunc
     *
     * @param block block to add
     */
    public synchronized void addBlock(Block block) {
        Chunk.addBlock(tupleList, block);
    }

    /**
     * Removes block from the chunks.Block will be located based on
     * Chunk.chunkFunc and then removed if exits.
     *
     * @param block block to remove
     */
    public synchronized void removeBlock(Block block) {
        Chunk.removeBlock(tupleList, block);
    }

    /**
     * Gets the chunk block list using chunk id.
     *
     * @param chunkId provided chunk id.
     * @return chunk (if exists non-empty) block list
     */
    public synchronized IList<Block> getBlockList(int chunkId) {
        IList<Block> blocks = new GapList<>();
        tupleList.forEach(tuple -> blocks.addAll(tuple.blockList.filter(blk -> Chunk.chunkFunc(blk.pos) == chunkId)));

        return blocks;
    }

    /**
     * Gets the chunk block list using chunk id.
     *
     * @param texName tuple texName
     * @param faceBits face bits of the tuple
     * @param chunkId provided chunk id.
     * @return null if tuple doest not exist otherwise block list from tuple
     */
    public synchronized IList<Block> getFilteredBlockList(String texName, int faceBits, int chunkId) {
        // binary search of the tuple
        Tuple tuple = Chunk.getTuple(tupleList, texName, faceBits);

        // block list filter of the tuple
        if (tuple != null) {
            Vector3f centre = Chunk.invChunkFunc(chunkId);
            return tuple.blockList.filter(blk
                    -> blk.pos.x >= centre.x - Chunk.LENGTH / 2.0f
                    && blk.pos.x < centre.x + Chunk.LENGTH / 2.0f
                    && blk.pos.z >= centre.z - Chunk.LENGTH / 2.0f
                    && blk.pos.z < centre.z + Chunk.LENGTH / 2.0f
            );
        }

        return null;
    }

    /**
     * Gets the chunk block list using chunk id.
     *
     * @param texName tuple texName
     * @param faceBits face bits of the tuple
     * @param chunkId provided chunk id.
     * @param camera observer camera (observing blocks)
     * @param degrees angle degrees of front view
     * @return null if tuple doest not exist otherwise block list from tuple
     */
    public synchronized IList<Block> getFilteredBlockList(String texName, int faceBits, int chunkId, Camera camera, float degrees) {
        // binary search of the tuple
        Tuple tuple = Chunk.getTuple(tupleList, texName, faceBits);

        // block list filter of the tuple
        if (tuple != null) {
            Vector3f centre = Chunk.invChunkFunc(chunkId);
            return tuple.blockList.filter(blk
                    -> blk.pos.x >= centre.x - Chunk.LENGTH / 2.0f
                    && blk.pos.x < centre.x + Chunk.LENGTH / 2.0f
                    && blk.pos.z >= centre.z - Chunk.LENGTH / 2.0f
                    && blk.pos.z < centre.z + Chunk.LENGTH / 2.0f
                    && camera.doesSeeEff(blk, degrees) // *New* visible blocks
            );
        }

        return null;
    }

    /**
     * Gets the chunk block list using chunk id.
     *
     * @param chunkIdList provided chunk id list
     * @return chunk (if exists non-empty) block list
     */
    public synchronized IList<Block> getBlockList(IList<Integer> chunkIdList) {
        IList<Block> blocks = new GapList<>();
        tupleList.forEach(tuple -> blocks.addAll(tuple.blockList.filter(blk -> chunkIdList.contains(Chunk.chunkFunc(blk.pos)))));

        return blocks;
    }

    /**
     * Gets the chunk block list using chunk id.
     *
     * @param texName tuple texName
     * @param faceBits face bits of the tuple
     * @param chunkIdList provided chunk id list
     * @return null if tuple doest not exist otherwise block list from tuple
     */
    public IList<Block> getFilteredBlockList(String texName, int faceBits, IList<Integer> chunkIdList) {
        // binary search of the tuple
        Tuple tuple = Chunk.getTuple(tupleList, texName, faceBits);

        // block list filter of the tuple
        if (tuple != null) {
            final IList<Block> filteredBlocks = new GapList<>();
            for (int chunkId : chunkIdList) {
                Vector3f centre = Chunk.invChunkFunc(chunkId);
                IList<Block> filtered = tuple.blockList.filter(blk
                        -> blk.pos.x >= centre.x - Chunk.LENGTH / 2.0f
                        && blk.pos.x < centre.x + Chunk.LENGTH / 2.0f
                        && blk.pos.z >= centre.z - Chunk.LENGTH / 2.0f
                        && blk.pos.z < centre.z + Chunk.LENGTH / 2.0f);
                filteredBlocks.addAll(filtered);
            }

            return filteredBlocks;
        }

        return null;
    }

    /**
     * Gets the chunk block list using chunk id.
     *
     * @param texName tuple texName
     * @param faceBits face bits of the tuple
     * @param chunkIdList provided chunk id list
     * @param camera observer camera (observing blocks)
     * @param degrees angle degrees of front view
     * @return null if tuple doest not exist otherwise block list from tuple
     */
    public IList<Block> getFilteredBlockList(String texName, int faceBits, IList<Integer> chunkIdList, Camera camera, float degrees) {
        // binary search of the tuple
        Tuple tuple = Chunk.getTuple(tupleList, texName, faceBits);

        // block list filter of the tuple
        if (tuple != null) {
            final IList<Block> filteredBlocks = new GapList<>();
            for (int chunkId : chunkIdList) {
                Vector3f centre = Chunk.invChunkFunc(chunkId);
                IList<Block> filtered = tuple.blockList.filter(blk
                        -> blk.pos.x >= centre.x - Chunk.LENGTH / 2.0f
                        && blk.pos.x < centre.x + Chunk.LENGTH / 2.0f
                        && blk.pos.z >= centre.z - Chunk.LENGTH / 2.0f
                        && blk.pos.z < centre.z + Chunk.LENGTH / 2.0f
                        && camera.doesSeeEff(blk, degrees) // *New* visible blocks
                );
                filteredBlocks.addAll(filtered);
            }

            return filteredBlocks;
        }

        return null;
    }

    /**
     * Gets the chunk block list using chunk id.
     *
     * This version (MK2) uses access to chunk id as key to provide faster
     * filtering.
     *
     * @param texName tuple texName
     * @param faceBits face bits of the tuple
     * @param chunkIdList provided chunk id list
     * @return null if tuple doest not exist otherwise block list from tuple
     */
    public IList<Block> getFilteredBlockListMK2(String texName, int faceBits, IList<Integer> chunkIdList) {
        // binary search of the tuple
        Tuple tuple = Chunk.getTuple(tupleList, texName, faceBits);

        // block list filter of the tuple
        if (tuple != null) {
            final IList<Block> filteredBlocks = new GapList<>();
            for (int chunkId : chunkIdList) {
                IList<Block> filtered = tuple.blockList.getAllByKey1(chunkId);
                filteredBlocks.addAll(filtered);
            }

            return filteredBlocks;
        }

        return null;
    }

    /**
     * Gets the chunk block list using chunk id.
     *
     * This version (MK2) uses access to chunk id as key to provide faster
     * filtering.
     *
     * @param texName tuple texName
     * @param faceBits face bits of the tuple
     * @param chunkIdList provided chunk id list
     * @param camera observer camera (observing blocks)
     * @param degrees angle degrees of front view
     * @return null if tuple doest not exist otherwise block list from tuple
     */
    public IList<Block> getFilteredBlockListMK2(String texName, int faceBits, IList<Integer> chunkIdList, Camera camera, float degrees) {
        // binary search of the tuple
        Tuple tuple = Chunk.getTuple(tupleList, texName, faceBits);

        // block list filter of the tuple
        if (tuple != null) {
            final IList<Block> filteredBlocks = new GapList<>();
            for (int chunkId : chunkIdList) {
                IList<Block> filtered = tuple.blockList
                        .getAllByKey1(chunkId)
                        .filter(blk -> camera.doesSeeEff(blk, degrees)); // *New* visible blocks
                filteredBlocks.addAll(filtered);
            }

            return filteredBlocks;
        }

        return null;
    }

    // all blocks from all the chunks in one big list
    /**
     * Get all block from all the chunks. Whole world of blocks.
     *
     * @return total list of blocks
     *
     */
    public IList<Block> getTotalList() {
        IList<Block> blocks = new BigList<>();
        tupleList.forEach(tuple -> blocks.addAll(tuple.blockList));

        return blocks;
    }

    public String printInfo() { // for debugging purposes
        StringBuilder sb = new StringBuilder();
//        sb.append("CHUNKS\n");
//        sb.append("CHUNKS TOTAL SIZE = ").append(CacheModule.totalSize(this)).append("\n");
//        sb.append("DETAILED INFO\n");
//        for (int id = 0; id < Chunk.CHUNK_NUM; id++) {
//            boolean cached = CacheModule.isCached(id);
//            chunk = CacheModule.isCached(id);
//
//            sb.append("id = ").append(id)
//                    .append(" | size = ").append((!cached && chunk != null) ? CacheModule.loadedSize(chunk) : CacheModule.cachedSize(id))
//                    .append(" | cached = ").append(cached)
//                    .append("\n");
//        }
//        sb.append("------------------------------------------------------------");
//        DSLogger.reportDebug(sb.toString(), null);

        return sb.toString();
    }

    public void clear() {
        this.tupleList.forEach(t -> t.blockList.clear());
        this.tupleList.clear();
    }

    public IList<Tuple> getTupleList() {
        return tupleList;
    }

}
