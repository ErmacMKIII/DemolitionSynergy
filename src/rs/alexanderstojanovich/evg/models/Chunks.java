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
import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.level.CacheModule;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.level.LightSource;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Pair;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Chunks {

    private final boolean solid;
    // single mat4Vbo is for model matrix shared amongst the vertices of the same instance    
    // single vec4Vbo is color shared amongst the vertices of the same instance    
    //--------------------------A--------B--------C-------D--------E-----------------------------
    //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
    private final List<Chunk> chunkList = new GapList<>();

    public Chunks(boolean solid) {
        this.solid = solid;
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
            if (GameObject.getInstance().MY_WINDOW.shouldClose()) {
                break;
            }

            int faceBitsBefore = solidBlock.getFaceBits();
            Pair<String, Byte> pair = LevelContainer.ALL_SOLID_MAP.get(solidBlock.pos);
            if (pair != null) {
                byte neighborBits = pair.getValue();
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
            if (GameObject.getInstance().MY_WINDOW.shouldClose()) {
                break;
            }

            int faceBitsBefore = fluidBlock.getFaceBits();
            Pair<String, Byte> pair = LevelContainer.ALL_FLUID_MAP.get(fluidBlock.pos);
            if (pair != null) {
                byte neighborBits = pair.getValue();
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
    private void updateSolidForAdd(Block block) {
        int faceBitsBefore = block.getFaceBits();
        Pair<String, Byte> pair = LevelContainer.ALL_SOLID_MAP.get(block.pos);
        int chunkId = Chunk.chunkFunc(block.pos);
        Chunk chunk = getChunk(chunkId);
        if (pair != null && chunk != null) {
            byte neighborBits = pair.getValue();
            block.setFaceBits(~neighborBits & 63);
            int faceBitsAfter = block.getFaceBits();
            if (faceBitsBefore != faceBitsAfter) {
                // if bits changed, i.e. some face(s) got disabled
                // tranfer to correct tuple
                chunk.transfer(block, faceBitsBefore, faceBitsAfter);
            } else {
                // check adjacent blocks
                for (int j = Block.LEFT; j <= Block.FRONT; j++) {
                    Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
                    int adjChunkId = Chunk.chunkFunc(adjPos);
                    Chunk adjChunk = getChunk(adjChunkId);
                    Pair<String, Byte> adjPair = LevelContainer.ALL_SOLID_MAP.get(adjPos);
                    if (adjPair != null && adjChunk != null) {
                        String tupleTexName = adjPair.getKey();
                        byte adjNBits = adjPair.getValue();
                        int k = ((j & 1) == 0 ? j + 1 : j - 1);
                        int mask = 1 << k;
                        // revert the bit that was set in LevelContainer
                        //(looking for old bits i.e. current tuple)
                        int tupleBits = adjNBits ^ (~mask & 63);

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
        }
    }

    /**
     * Updates blocks faces of both original block and adjacent blocks. Block
     * must be solid.
     *
     * Used after removal operation.
     *
     * @param block block to update
     */
    private void updateSolidForRem(Block block) {
        // check adjacent blocks
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
            Pair<String, Byte> adjPair = LevelContainer.ALL_SOLID_MAP.get(adjPos);
            if (adjPair != null) {
                String tupleTexName = adjPair.getKey();
                byte adjNBits = adjPair.getValue();
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
     * Updates blocks faces of both original block and adjacent blocks. Block
     * must be fluid. Removal is used only if block is being removed.
     *
     * @param block block to update
     */
    private void updateFluidForAdd(Block block) {
        int faceBitsBefore = block.getFaceBits();
        Pair<String, Byte> pair = LevelContainer.ALL_FLUID_MAP.get(block.pos);
        int chunkId = Chunk.chunkFunc(block.pos);
        Chunk chunk = getChunk(chunkId);
        if (pair != null && chunk != null) {
            byte neighborBits = pair.getValue();
            block.setFaceBits(~neighborBits & 63);
            int faceBitsAfter = block.getFaceBits();
            if (faceBitsBefore != faceBitsAfter) {
                // if bits changed, i.e. some face(s) got disabled
                // tranfer to correct tuple
                chunk.transfer(block, faceBitsBefore, faceBitsAfter);
            } else {
                // check adjacent blocks
                for (int j = Block.LEFT; j <= Block.FRONT; j++) {
                    Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
                    int adjChunkId = Chunk.chunkFunc(adjPos);
                    Chunk adjChunk = getChunk(adjChunkId);
                    Pair<String, Byte> adjPair = LevelContainer.ALL_FLUID_MAP.get(adjPos);
                    if (adjPair != null && adjChunk != null) {
                        String tupleTexName = adjPair.getKey();
                        byte adjNBits = adjPair.getValue();
                        int k = ((j & 1) == 0 ? j + 1 : j - 1);
                        int mask = 1 << k;
                        // revert the bit that was set in LevelContainer
                        //(looking for old bits i.e. current tuple)
                        int tupleBits = adjNBits ^ (~mask & 63);

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
        }
    }

    /**
     * Updates blocks faces of both original block and adjacent blocks. Block
     * must be solid.
     *
     * Used after removal operation.
     *
     * @param block block to update
     *
     */
    private void updateFluidForRem(Block block) {
        // check adjacent blocks
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            Vector3f adjPos = Block.getAdjacentPos(block.pos, j);
            Pair<String, Byte> adjPair = LevelContainer.ALL_FLUID_MAP.get(adjPos);
            if (adjPair != null) {
                String tupleTexName = adjPair.getKey();
                byte adjNBits = adjPair.getValue();
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
     * Adds block to the chunks. Block will be added to the corresponding solid
     * chunk based on Chunk.chunkFunc
     *
     * @param block block to add
     * @param useLevelContainer update level container environment map (for
     * adjacency)
     */
    public void addBlock(Block block, boolean useLevelContainer) {
        //----------------------------------------------------------------------
        int chunkId = Chunk.chunkFunc(block.pos);
        Chunk chunk = getChunk(chunkId);

        if (chunk == null) {
            chunk = new Chunk(chunkId, block.solid);
            chunkList.add(chunk);
            chunkList.sort(COMPARATOR);
        }

        chunk.addBlock(block, useLevelContainer);
        if (block.solid) {
            updateSolidForAdd(block);
        } else {
            updateFluidForAdd(block);
        }
    }

    /**
     * Removes block from the chunks. Block will be located based on
     * Chunk.chunkFunc and then removed if exits.
     *
     * @param block block to remove
     * @param useLevelContainer update level container environment map (for
     * adjacency)
     */
    public void removeBlock(Block block, boolean useLevelContainer) {
        int chunkId = Chunk.chunkFunc(block.pos);
        Chunk chunk = getChunk(chunkId);

        if (chunk != null) { // if chunk exists already                            
            chunk.removeBlock(block, useLevelContainer);

            if (block.solid) {
                updateSolidForRem(block);
            } else {
                updateFluidForRem(block);
            }

            // if chunk is empty (with no tuples) -> remove it
            if (chunk.getTupleList().isEmpty()) {
                chunkList.remove(chunk);
            }
        }
    }

    /**
     * Gets the chunk using
     *
     * @param chunkId
     * @return
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

    public void animate() { // call only for fluid blocks
        for (Chunk chunk : getChunkList()) {
            if (!CacheModule.isCached(chunk.getId(), solid) && chunk.isBuffered()) {
                chunk.animate();
            }
        }
    }

    public void prepare(boolean cameraInFluid) { // call only for fluid blocks before rendering        
        for (Chunk chunk : chunkList) {
            chunk.prepare(cameraInFluid);
        }
    }

    // for each instanced rendering
    public void render(ShaderProgram shaderProgram, List<LightSource> lightSrc) {
        for (Chunk chunk : chunkList) {
            if (!chunk.isBuffered()) {
                chunk.bufferAll();
            }
            chunk.render(shaderProgram, lightSrc);
        }
    }

    public void render(Queue<Integer> queue, ShaderProgram shaderProgram, List<LightSource> lightSrc) {
        for (int chunkId : queue) {
            Chunk chunk = getChunk(chunkId);
            if (chunk != null) {
                if (!chunk.isBuffered()) {
                    chunk.bufferAll();
                }
                chunk.render(shaderProgram, lightSrc);
            }
        }
    }

    // all blocks from all the chunks in one big list
    public List<Block> getTotalList() {
        List<Block> result = new BigList<>();
        for (int id = 0; id < Chunk.CHUNK_NUM; id++) {
            if (!CacheModule.isCached(id, solid)) {
                Chunk chunk = getChunk(id);
                if (chunk != null) {
                    result.addAll(chunk.getBlockList());
                }
            }
        }
        return result;
    }

    public void printInfo() { // for debugging purposes
        StringBuilder sb = new StringBuilder();
        sb.append("CHUNKS\n");
        sb.append("CHUNKS TOTAL SIZE = ").append(CacheModule.totalSize(this, solid)).append("\n");
        sb.append("DETAILED INFO\n");
        for (int id = 0; id < Chunk.CHUNK_NUM; id++) {
            boolean cached = CacheModule.isCached(id, solid);
            Chunk chunk = null;
            if (!cached) {
                chunk = getChunk(id);
            }

            sb.append("id = ").append(id)
                    .append(" | solid = ").append(solid)
                    .append(" | size = ").append((!cached && chunk != null) ? CacheModule.loadedSize(chunk) : CacheModule.cachedSize(id, solid))
                    .append(" | timeToLive = ").append((chunk != null) ? String.format("%.1f", chunk.getTimeToLive()) : 0.0f)
                    .append(" | buffered = ").append((chunk != null) ? chunk.isBuffered() : false)
                    .append(" | cached = ").append(cached)
                    .append("\n");
        }
        sb.append("------------------------------------------------------------");
        DSLogger.reportInfo(sb.toString(), null);
    }

    public List<Chunk> getChunkList() {
        return chunkList;
    }

    public boolean isSolid() {
        return solid;
    }

}
