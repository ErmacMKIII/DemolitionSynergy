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

import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.level.CacheModule;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.level.LightSources;
import rs.alexanderstojanovich.evg.level.TexByte;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
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
    private final List<Chunk> chunkList = new GapList<>(Chunk.CHUNK_NUM);

    protected final IList<Tuple> optimizedTuples = new GapList<>(63);
    protected boolean optimized = false;

    public Chunks() {

    }

    public static final Comparator<Chunk> COMPARATOR = new Comparator<Chunk>() {
        @Override
        public int compare(Chunk o1, Chunk o2) {
            if (o1.getId() > o2.getId()) {
                return 1;
            } else if (o1.getId() == o2.getId()) {
                return 0;
            } else {
                return -1;
            }
        }
    };

    @Deprecated
    public void updateSolids() {
        for (Block solidBlock : getTotalList()) {
            if (GameObject.MY_WINDOW.shouldClose()) {
                break;
            }

            int faceBitsBefore = solidBlock.getFaceBits();
            TexByte pair = LevelContainer.ALL_BLOCK_MAP.getLocation(solidBlock.pos);
            if (pair != null && pair.isSolid()) {
                byte neighborBits = pair.getByteValue();
                solidBlock.setFaceBits(~neighborBits & 63);
                int faceBitsAfter = solidBlock.getFaceBits();
                if (faceBitsBefore != faceBitsAfter) { // if bits changed, i.e. some face(s) got disabled
                    int chunkId = Chunk.chunkFunc(solidBlock.getPos());
                    Chunk solidChunk = getChunk(chunkId);
                    if (solidChunk != null) {
                        solidChunk.transfer(solidBlock, faceBitsBefore, faceBitsAfter);
                    }
                }
            }
        }
    }

    @Deprecated
    public void updateFluids() {
        for (Block fluidBlock : getTotalList()) {
            if (GameObject.MY_WINDOW.shouldClose()) {
                break;
            }

            int faceBitsBefore = fluidBlock.getFaceBits();
            TexByte pair = LevelContainer.ALL_BLOCK_MAP.getLocation(fluidBlock.pos);
            if (pair != null && !pair.isSolid()) {
                byte neighborBits = pair.getByteValue();
                fluidBlock.setFaceBits(~neighborBits & 63);
                int faceBitsAfter = fluidBlock.getFaceBits();
                if (faceBitsBefore != faceBitsAfter) { // if bits changed, i.e. some face(s) got disabled
                    int chunkId = Chunk.chunkFunc(fluidBlock.getPos());
                    Chunk fluidChunk = getChunk(chunkId);
                    if (fluidChunk != null) {
                        fluidChunk.transfer(fluidBlock, faceBitsBefore, faceBitsAfter);
                    }
                }
            }
        }
    }

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
        int neighborBits = block.solid
                ? LevelContainer.ALL_BLOCK_MAP.getNeighborSolidBits(block.pos)
                : LevelContainer.ALL_BLOCK_MAP.getNeighborFluidBits(block.pos);
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
                TexByte location = LevelContainer.ALL_BLOCK_MAP.getLocation(adjPos);
                if (location != null) {
                    String tupleTexName = location.getTexName();
                    int adjNBits = block.solid
                            ? LevelContainer.ALL_BLOCK_MAP.getNeighborSolidBits(adjPos)
                            : LevelContainer.ALL_BLOCK_MAP.getNeighborFluidBits(adjPos);
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
                        adjBlock = Chunk.getBlock(tuple, adjPos);
                    }
                    if (adjBlock != null && adjBlock.pos.equals(adjPos)) {
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

    private void updateForRem(Block block) {
        // check adjacent blocks
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
            int nBits = block.solid
                    ? LevelContainer.ALL_BLOCK_MAP.getNeighborSolidBits(block.pos)
                    : LevelContainer.ALL_BLOCK_MAP.getNeighborFluidBits(block.pos);
            TexByte location = LevelContainer.ALL_BLOCK_MAP.getLocation(adjPos);
            // location exists and has neighbors (otherwise pointless)
            if (location != null && nBits != 0) {
                String tupleTexName = location.getTexName();
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
                    adjBlock = Chunk.getBlock(tuple, adjPos);
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
     * Adds block to the chunks.Block will be added to the corresponding solid
     * chunk based on Chunk.chunkFunc
     *
     * @param block block to add
     */
    public void addBlock(Block block) {
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
    public void removeBlock(Block block) {
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

    public synchronized void animate() { // call only for fluid blocks
        if (!optimized) {
            return;
        }

        for (Tuple tuple : optimizedTuples) {
            if (tuple.isBuffered()) {
                tuple.animate();
            }
        }
    }

    public synchronized void prepare(boolean cameraInFluid) { // call only for fluid blocks before rendering                
        for (Chunk chunk : chunkList) {
            chunk.prepare(cameraInFluid);
        }
    }

    public synchronized void prepareOptimized(boolean cameraInFluid) { // call only for fluid blocks before rendering
        if (!optimized) {
            return;
        }

        for (Tuple tuple : optimizedTuples) {
            if (!tuple.isSolid()) {
                tuple.prepare(cameraInFluid);
            }
        }
    }

    public synchronized void optimize(Queue<Integer> queue) {
        optimizedTuples.clear();
        int faceBits = 1; // starting from one, cuz zero is not rendered               
        while (faceBits <= 63) {
            for (String tex : Texture.TEX_WORLD) {
                Tuple optmTuple = null;
                for (int chunkId : queue) {
                    Chunk chunk = getChunk(chunkId);
                    if (chunk != null) {
                        Tuple tuple = chunk.getTuple(tex, faceBits);
                        if (tuple != null) {
                            if (optmTuple == null) {
                                optmTuple = new Tuple(tex, faceBits);
                            }
                            optmTuple.blockList.addAll(tuple.blockList);
                        }
                    }
                }

                if (optmTuple != null) {
                    optimizedTuples.add(optmTuple);
                    optimizedTuples.sort(Tuple.TUPLE_COMP);
                }
            }
            faceBits++;
        }

        optimized = true;
    }

    public synchronized void optimize(Queue<Integer> queue, Vector3f camFront) {
        optimizedTuples.clear();
        int faceBits = 1; // starting from one, cuz zero is not rendered               
        final int mask = Block.getVisibleFaceBits(camFront);
        while (faceBits <= 63) {
            if ((faceBits & (mask & 63)) != 0) {
                for (String tex : Texture.TEX_WORLD) {
                    Tuple optmTuple = null;
                    for (int chunkId : queue) {
                        Chunk chunk = getChunk(chunkId);
                        if (chunk != null) {
                            Tuple tuple = chunk.getTuple(tex, faceBits);
                            if (tuple != null) {
                                if (optmTuple == null) {
                                    optmTuple = new Tuple(tex, faceBits);
                                }
                                optmTuple.blockList.addAll(tuple.blockList);
                            }
                        }
                    }

                    if (optmTuple != null) {
                        optimizedTuples.add(optmTuple);
                        optimizedTuples.sort(Tuple.TUPLE_COMP);
                    }
                }
            }
            faceBits++;
        }

        optimized = true;
    }

    public synchronized void optimizeSuper(Queue<Integer> vQueue, Vector3f camFront) {
        final int mask = Block.getVisibleFaceBits(camFront);
        for (int faceBits = 1; faceBits <= 63; faceBits++) {
            final int faceBitsCopy = faceBits;
            if ((faceBitsCopy & (mask & 63)) != 0) {
                for (String tex : Texture.TEX_WORLD) {
                    for (int chunkId : vQueue) {
                        Chunk chunk = getChunk(chunkId);
                        if (chunk != null) {
                            Tuple tuple = chunk.getTuple(tex, faceBits);
                            if (tuple != null) {
                                Tuple optmTuple = optimizedTuples.getIf(ot -> ot.texName().equals(tex) && ot.faceBits() == faceBitsCopy);
                                if (optmTuple == null) {
                                    optmTuple = new Tuple(tex, faceBitsCopy);
                                    optimizedTuples.add(optmTuple);
                                    optimizedTuples.sort(Tuple.TUPLE_COMP);
                                } else {
                                    optmTuple.buffered = false;
                                }
                                final Tuple optmTupleCopy = optmTuple;
                                tuple.blockList.forEach(blk -> {
                                    optmTupleCopy.blockList.addIfAbsent(blk);
                                });
                            }
                        }
                    }
                }
            } else {
                optimizedTuples.removeIf(ot -> ot.faceBits() == faceBitsCopy);
            }
        }

        optimized = true;
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

    public synchronized void render(Queue<Integer> queue, ShaderProgram shaderProgram, LightSources lightSources) {
        if (!optimized || optimizedTuples.isEmpty()) {
            return;
        }

        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);
        GL20.glEnableVertexAttribArray(3);
        GL20.glEnableVertexAttribArray(4);
        GL20.glEnableVertexAttribArray(5);
        GL20.glEnableVertexAttribArray(6);
        GL20.glEnableVertexAttribArray(7);

        for (Tuple tuple : optimizedTuples) {
            if (!tuple.isBuffered()) {
                tuple.bufferAll();
            }
            tuple.renderInstanced(shaderProgram, lightSources, GameObject.getWaterRenderer().getFrameBuffer().getTexture());
        }

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
        GL20.glDisableVertexAttribArray(3);
        GL20.glDisableVertexAttribArray(4);
        GL20.glDisableVertexAttribArray(5);
        GL20.glDisableVertexAttribArray(6);
        GL20.glDisableVertexAttribArray(7);
    }

    // all blocks from all the chunks in one big list
    public List<Block> getTotalList() {
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
        DSLogger.reportInfo(sb.toString(), null);

        return sb.toString();
    }

    public List<Chunk> getChunkList() {
        return chunkList;
    }

    public boolean isOptimized() {
        return optimized;
    }

    public void setOptimized(boolean optimized) {
        this.optimized = optimized;
    }

    public List<Tuple> getOptimizedTuples() {
        return optimizedTuples;
    }

}
