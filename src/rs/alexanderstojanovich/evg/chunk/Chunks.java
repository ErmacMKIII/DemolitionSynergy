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

import java.util.Comparator;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.cache.CacheModule;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.location.TexByte;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Chunks {

    // single mat4Vbo is for model matrix shared amongst the vertices of the same instance    
    // single vec4Vbo is color shared amongst the vertices of the same instance    
    //--------------------------A--------B--------C-------D--------E-----------------------------
    //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
    public final IList<Chunk> chunkList = new GapList<>(Chunk.CHUNK_NUM);

    public static final Comparator<Chunk> COMPARATOR = (Chunk o1, Chunk o2) -> {
        if (o1.getId() > o2.getId()) {
            return 1;
        } else if (o1.getId() == o2.getId()) {
            return 0;
        } else {
            return -1;
        }
    };

    /**
     * Updates blocks faces of both original block and adjacent blocks. Block
     * must be solid.
     *
     * Used after add operation.
     *
     * @param block block to update
     */
    protected void updateForAdd(Block block) {
        // only same solidity - solid to solid or fluid to fluid is updated        
        int neighborBits = block.isSolid()
                ? LevelContainer.AllBlockMap.getNeighborSolidBits(block.pos)
                : LevelContainer.AllBlockMap.getNeighborFluidBits(block.pos);
        if (neighborBits != 0) {
            // retieve current neightbor bits      
            int faceBitsBefore = block.getFaceBits();
            // -------------------------------------------------------------------
            // this logic updates facebits of this block
            // & transfers it to correct tuple 
            // -------------------------------------------------------------------                    
            block.setFaceBits(~neighborBits & 63);
            int faceBitsAfter = block.getFaceBits();
            if (faceBitsBefore != faceBitsAfter) {
                int chunkId = Chunk.chunkFunc(block.pos);
                Chunk chunk = getChunk(chunkId);
                if (chunk != null) {
                    chunk.transfer(block, faceBitsBefore, faceBitsAfter);
                }
            }
            // query all neighbors and update this block and adjacent blocks accordingly
            for (int j = Block.LEFT; j <= Block.FRONT; j++) {
                // -------------------------------------------------------------------
                // following logic updates adjacent block 
                // if it is same solidity as this block
                // need to find tuple where it is located
                // -------------------------------------------------------------------
                Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
                TexByte location = LevelContainer.AllBlockMap.getLocation(adjPos);
                if (location != null) {
                    int blkId = location.blkId;
                    String tupleTexName = location.texName;
                    int adjNBits = block.isSolid()
                            ? LevelContainer.AllBlockMap.getNeighborSolidBits(adjPos)
                            : LevelContainer.AllBlockMap.getNeighborFluidBits(adjPos);
                    int k = ((j & 1) == 0 ? j + 1 : j - 1);
                    int mask = 1 << k;
                    // revert the bit that was set in LevelContainer
                    //(looking for old bits i.e. current tuple)
                    int tupleBits = adjNBits ^ (~mask & 63);

                    int adjChunkId = Chunk.chunkFunc(adjPos);
                    Chunk adjChunk = getChunk(adjChunkId);
                    if (adjChunk != null) {
                        Tuple tuple = adjChunk.getTuple(tupleTexName, tupleBits);
                        if (tuple != null) {
                            Block adjBlock = Chunk.getBlock(tuple, adjPos, blkId);

                            if (adjBlock != null) {
                                int adjFaceBitsBefore = adjBlock.getFaceBits();
                                adjBlock.setFaceBits(~adjNBits & 63);
                                int adjFaceBitsAfter = adjBlock.getFaceBits();
                                if (adjFaceBitsBefore != adjFaceBitsAfter) {
                                    // if bits changed, i.e. some face(s) got disabled
                                    // tranfer to correct tuple
                                    adjChunk.transfer(adjBlock, adjFaceBitsBefore, adjFaceBitsAfter);
                                }
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
     * Used after rem operation.
     *
     * @param block block to update
     */
    private void updateForRem(Block block) {
        // setSafeCheck adjacent blocks
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
            int nBits = block.isSolid()
                    ? LevelContainer.AllBlockMap.getNeighborSolidBits(block.pos)
                    : LevelContainer.AllBlockMap.getNeighborFluidBits(block.pos);
            TexByte location = LevelContainer.AllBlockMap.getLocation(adjPos);
            // location exists and has neighbors (otherwise pointless)
            if (location != null && nBits != 0) {
                int blkId = location.blkId;
                String tupleTexName = location.texName;
                byte adjNBits = location.getByteValue();
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int mask = 1 << k;
                // revert the bit that was set in LevelContainer
                //(looking for old bits i.e. current tuple)
                int tupleBits = adjNBits ^ (~mask & 63);

                int adjChunkId = Chunk.chunkFunc(adjPos);
                Chunk adjChunk = getChunk(adjChunkId);

                Tuple tuple = adjChunk.getTuple(tupleTexName, tupleBits);
                Block adjBlock = null;

                if (tuple != null) {
                    adjBlock = Chunk.getBlock(tuple, adjPos, blkId);
                }
                if (adjBlock != null) {
                    int adjFaceBitsBefore = adjBlock.getFaceBits();
                    adjBlock.setFaceBits(~adjNBits & 63);
                    int adjFaceBitsAfter = adjBlock.getFaceBits();
                    if (adjFaceBitsBefore != adjFaceBitsAfter) {
                        // if bits changed, i.e. some face(s) got disabled
                        // tranfer to correct tuple
                        adjChunk.transfer(adjBlock, adjFaceBitsBefore, adjFaceBitsAfter);
                    }
                }
            }
        }
    }

    /**
     * Adds block to the chunks. Block will be added to the corresponding solid
     * chunk based on Chunk.chunkFunc
     *
     * @param block block to add
     */
    public synchronized void addBlock(Block block) {
        //----------------------------------------------------------------------
        int chunkId = Chunk.chunkFunc(block.pos);
        Chunk chunk = getChunk(chunkId);

        if (chunk == null) {
            chunk = new Chunk(chunkId);
            chunkList.add(chunk);
            chunkList.sort(COMPARATOR);
        }

        chunk.addBlock(block);
        updateForAdd(block);
    }

    /**
     * Removes block from the chunks.Block will be located based on
     * Chunk.chunkFunc and then removed if exits.
     *
     * @param block block to remove
     */
    public synchronized void removeBlock(Block block) {
        int chunkId = Chunk.chunkFunc(block.pos);
        Chunk chunk = getChunk(chunkId);

        if (chunk != null) { // if chunk exists already                            
            chunk.removeBlock(block);
            updateForRem(block);
            // if chunk is empty (with no tuples) -> remove it
            if (chunk.getTupleList().isEmpty()) {
                chunkList.remove(chunk);
            }
        }

    }

    /**
     * Gets the chunk using chunk id. Uses binary search. Complexity is
     * algorithmic.
     *
     * @param chunkId provided chunk id.
     * @return chunk (if exists)
     */
    public Chunk getChunk(int chunkId) {
        int left = 0;
        int right = chunkList.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            Chunk candidate = chunkList.get(mid);
            if (candidate.getId() == chunkId) {
                return candidate;
            } else if (candidate.getId() < chunkId) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return null;
    }

    /**
     * Reorder group of vertices if underwater
     *
     * @param cameraInFluid is camera in fluid (checked by level container
     * externally)
     */
    public void prepare(boolean cameraInFluid) { // call only for fluid blocks before rendering                
        for (Chunk chunk : chunkList) {
            chunk.prepare(cameraInFluid);
        }
    }

    // for each instanced rendering
    public void render(ShaderProgram shaderProgram, LightSources lightSources) {
        for (Chunk chunk : chunkList) {
            if (!chunk.isBuffered()) {
                chunk.bufferAll();
            }
            chunk.render(shaderProgram, lightSources);
        }
    }

    // all blocks from all the chunks in one big list
    public IList<Block> getTotalList() {
        IList<Block> result = new BigList<>();
        for (int id = 0; id < Chunk.CHUNK_NUM; id++) {
            if (!CacheModule.isCached(id)) {
                Chunk chunk = getChunk(id);
                if (chunk != null) {
                    result.addAll(chunk.getBlockList());
                }
            }
        }
        return result;
    }

    public String printInfo() { // for debugging purposes
        StringBuilder sb = new StringBuilder();
        sb.append("CHUNKS\n");
        sb.append("CHUNKS TOTAL SIZE = ").append(CacheModule.totalSize(this)).append("\n");
        sb.append("DETAILED INFO\n");
        for (int id = 0; id < Chunk.CHUNK_NUM; id++) {
            boolean cached = CacheModule.isCached(id);
            Chunk chunk = null;
            if (!cached) {
                chunk = getChunk(id);
            }

            sb.append("id = ").append(id)
                    .append(" | size = ").append((!cached && chunk != null) ? CacheModule.loadedSize(chunk) : CacheModule.cachedSize(id))
                    .append(" | timeToLive = ").append((chunk != null) ? String.format("%.1f", chunk.getTimeToLive()) : 0.0f)
                    .append(" | buffered = ").append((chunk != null) ? chunk.isBuffered() : false)
                    .append(" | cached = ").append(cached)
                    .append("\n");
        }
        sb.append("------------------------------------------------------------");
        DSLogger.reportDebug(sb.toString(), null);

        return sb.toString();
    }

    public void clear() {
        this.chunkList.forEach(c -> c.clear());
    }

    public IList<Chunk> getChunkList() {
        return chunkList;
    }

}
