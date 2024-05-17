/*
 * Copyright (C) 2024 Alexander Stojanovich <coas91@rocketmail.com>
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

import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.chunk.Chunks;
import rs.alexanderstojanovich.evg.chunk.Tuple;
import rs.alexanderstojanovich.evg.chunk.TupleBufferObject;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 * Module with blocks from all the chunks. Effectively ready for rendering after
 * optimization.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class BlockEnvironment {

    public static final int LIGHT_MASK = 0x01;
    public static final int WATER_MASK = 0x02;
    public static final int SHADOW_MASK = 0x04;

    private final GameObject gameObject;

    /**
     * Working tuples (from update)
     */
    protected volatile IList<Tuple> workingTuples = new GapList<>();
    /**
     * Optimizes tuples (from render)
     */
    protected volatile IList<Tuple> optimizedTuples = new GapList<>();

    /**
     * Modified tuples (from update/render). Meaning from update they are
     * modified and need to be pushed to render.
     */
//    public final IList<Tuple> modifiedTuples = new GapList<>();
    protected boolean optimizing = false;
    protected boolean fullyOptimized = false;
    protected final Chunks chunks;
    protected int texProcIndex = 0;

    protected int lastFaceBits = 0; // starting from one, cuz zero is not rendered
    public static final int NUM_OF_PASSES_MAX = Configuration.getInstance().getOptimizationPasses();

    /**
     * Contains all batched buffer(s). As one.
     */
    public final TupleBufferObject tupleBuffObj = new TupleBufferObject(optimizedTuples);

    public BlockEnvironment(GameObject gameObject, Chunks chunks) {
        this.gameObject = gameObject;
        this.chunks = chunks;
    }

    /**
     * Basic version of optimization for tuples from all the chunks. (Deprecated
     * as clear/new operations are used constantly)
     *
     * @param queue visible chunkId queue
     */
    @Deprecated
    public void optimize(IList<Integer> queue) {
        optimizedTuples.clear();
        int faceBits = 1; // starting from one, cuz zero is not rendered               
        while (faceBits <= 63) {
            for (String tex : Texture.TEX_WORLD) {
                Tuple optmTuple = null;
                for (int chunkId : queue) {
                    Chunk chunk = chunks.getChunk(chunkId);
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

        fullyOptimized = true;
    }

    /**
     * Improved version of optimization for tuples from all the chunks. World is
     * being built incrementally. Consist of two passes.
     *
     * @param vqueue visible chunkId queue
     * @param camera ingame camera
     */
    public void optimizeFast(IList<Integer> vqueue, Camera camera) {
        // determine lastFaceBits mask
        final int mask0 = Block.getVisibleFaceBitsFast(camera.getFront(), LevelContainer.actorInFluid ? 0f : 45f);
        boolean someRemoved = optimizedTuples.removeIf(ot -> (ot.faceBits() & mask0) == 0);

        // some removals are made
        if (someRemoved) {
            fullyOptimized = false;
        }

        // determine texture type to process - split
        if (texProcIndex++ == Texture.TEX_WORLD.length - 1) {
            texProcIndex = 0;
        }

        final String tex = Texture.TEX_WORLD[texProcIndex];

        // PASS 1 : CREATE TUPLES
        int lastFaceBitsCopy = lastFaceBits;
        for (int j = 0; j < NUM_OF_PASSES_MAX; j++) {
            // assign last value & increment to next value with limit to 63
            final int faceBits = (++lastFaceBitsCopy) & 63;
            if ((faceBits & (mask0 & 63)) != 0) {
                final Tuple optmTuple = optimizedTuples.getIf(ot -> ot.texName().equals(tex) && ot.faceBits() == faceBits);
                if (optmTuple == null) {
                    optimizedTuples.add(new Tuple(tex, faceBits));
                    // sort so it remains ordered
                    optimizedTuples.sort(Tuple.TUPLE_COMP);
                }
            }
        }

        // PASS 2 : FILL TUPLES
        lastFaceBitsCopy = lastFaceBits;
        for (int j = 0; j < NUM_OF_PASSES_MAX; j++) {
            // assign last value & increment to next value with limit to 63
            final int faceBits = (++lastFaceBitsCopy) & 63;
            if ((faceBits & (mask0 & 63)) != 0) {
                chunks.chunkList.forEach(chnk -> { // for all chunks
                    if (vqueue.contains(chnk.id)) { // visible ones && not cached!
                        // select correlated tuples
                        final IList<Tuple> selectedTuples = chnk.tupleList.filter(t -> t.texName().equals(tex) && t.faceBits() == faceBits);
                        // for each selected tuple
                        selectedTuples.forEach(st -> {
                            final Tuple optmTuple = optimizedTuples.getIf(ot -> ot.texName().equals(tex) && ot.faceBits() == faceBits);
                            boolean modified = false;
                            // if fullyOptimized doesn't exist
                            for (Block blk : st.blockList) {
                                // take into consideration if could be seen by camera (impr. method)
                                if (camera.doesSeeEff(blk)) {
                                    // add absent blocks
                                    modified |= optmTuple.blockList.addIfAbsent(blk);
                                }
                            }
                            if (modified) {
                                // sort so it does remains ordered
                                optmTuple.blockList.sort(Block.UNIQUE_BLOCK_CMP);
                                // sets TBO to unbuffer if modified
                                optmTuple.setBuffered(false);
                            }
                        });
                    }
                });
            }
        }

        // move forward (with increment)
        lastFaceBits += NUM_OF_PASSES_MAX;

        // Remove empty optimization tuples
        optimizedTuples.removeIf(ot -> ot.blockList.isEmpty());

        // if last bits is processed start from beginning next time
        if (lastFaceBits == 64) {
            lastFaceBits = 0;
        }

        // if full circle with all textures & facebits has been completed
        if (texProcIndex == 0 && lastFaceBits == 0) {
            fullyOptimized = true;
        }

    }

    /**
     * Improved version of optimization for tuples from all the chunks. World is
     * being built incrementally. Consist of two passes. Also consists of
     * modifications to make it work with Tuple Buffer Object (TBO). On
     * modification modified tuples are pushed to the optimized.
     *
     * @param vqueue visible chunkId queue
     * @param camera ingame camera
     */
    public void optimizeByControl(IList<Integer> vqueue, Camera camera) {
        optimizing = true;
        pull(); // pull from optimized stream

        // determine lastFaceBits mask
        final int mask0 = Block.getVisibleFaceBitsFast(camera.getFront(), LevelContainer.actorInFluid ? 0f : 45f);
        workingTuples.removeIf(ot -> (ot.faceBits() & mask0) == 0);

        final String tex = Texture.TEX_WORLD[texProcIndex];

        // PASS 1 : CREATE TUPLES
        int lastFaceBitsCopy = lastFaceBits;
        for (int j = 0; j < NUM_OF_PASSES_MAX; j++) {
            // assign last value & increment to next value with limit to 63
            final int faceBits = (++lastFaceBitsCopy) & 63;
            if ((faceBits & (mask0 & 63)) != 0) {
                final Tuple optmTuple = workingTuples.getIf(ot -> ot.texName().equals(tex) && ot.faceBits() == faceBits);
                if (optmTuple == null) {
                    workingTuples.add(new Tuple(tex, faceBits));
                }
            }
        }

        // PASS 2 : FILL TUPLES
        lastFaceBitsCopy = lastFaceBits;
        for (int j = 0; j < NUM_OF_PASSES_MAX; j++) {
            // assign last value & increment to next value with limit to 63
            final int faceBits = (++lastFaceBitsCopy) & 63;
            if ((faceBits & (mask0 & 63)) != 0) {
                chunks.chunkList.forEach(chnk -> { // for all chunks
                    if (vqueue.contains(chnk.id) && Chunk.doesSeeChunk(chnk.id, camera)) { // visible ones && not cached!                        
                        // select correlated tuples
                        final IList<Tuple> selectedTuples = chnk.tupleList.filter(t -> t.texName().equals(tex) && t.faceBits() == faceBits);
                        // for each selected tuple
                        selectedTuples.forEach(st -> {
                            // get tuple from PASS 1
                            final Tuple workTuple = workingTuples.getIf(ot -> ot.texName().equals(tex) && ot.faceBits() == faceBits);
                            if (workTuple != null) {
                                boolean modified = false;
                                // if fullyOptimized doesn't exist
                                for (Block blk : st.blockList) {
                                    // take into consideration if could be seen by camera (impr. method)                                
                                    if (camera.doesSeeEff(blk)) {
                                        // add absent blocks                                    
                                        modified |= workTuple.blockList.addIfAbsent(blk);
                                    }
                                }
                                if (modified) {
                                    // single modification means no fullyOptimized
                                    fullyOptimized = false;
                                    // sort so it does remains ordered
                                    workTuple.blockList.sort(Block.UNIQUE_BLOCK_CMP);
                                    // unbuffer working tuple
                                    workTuple.setBuffered(false);
                                }
                            }
                        });
                    }
                });
            }
        }

        // move forward (with increment)
        lastFaceBits += NUM_OF_PASSES_MAX;

        // determine texture type to process - split
        if (texProcIndex++ == Texture.TEX_WORLD.length - 1) {
            texProcIndex = 0;
        }

        // Remove empty optimization tuples
        workingTuples.removeIf(ot -> ot.blockList.isEmpty());

        // if last bits is processed start from beginning next time
        if (lastFaceBits == 64) {
            lastFaceBits = 0;
        }

        // if full circle with all textures & facebits has been completed
        if (texProcIndex == 0 && lastFaceBits == 0) {
            workingTuples.sort(Tuple.TUPLE_COMP);
            fullyOptimized = true; // it is (fully) fullyOptimized  
        }

        optimizing = false;
    }

    /**
     * Reorder group of vertices of fullyOptimized tuples if underwater
     *
     * @param cameraInFluid is camera in fluid (checked by level container
     * externally)
     */
    public void prepare(boolean cameraInFluid) { // call only for fluid blocks before rendering
        if (optimizedTuples.isEmpty()) {
            return;
        }

        for (Tuple tuple : optimizedTuples.filter(ot -> ot.isBuffered() && !ot.isSolid() && ot.faceBits() != 0)) {
            tuple.prepare(cameraInFluid);
        }
    }

    /**
     * Reorder group of vertices of fullyOptimized tuples if underwater
     *
     * @param camFront camera front (used for ray-trace)
     * @param cameraInFluid is camera in fluid (checked by level container
     * externally)
     */
    public void prepare(Vector3f camFront, boolean cameraInFluid) { // call only for fluid blocks before rendering
        if (optimizedTuples.isEmpty()) {
            return;
        }

        for (Tuple tuple : optimizedTuples.filter(ot -> ot.isBuffered() && !ot.isSolid() && ot.faceBits() != 0)) {
            tuple.prepare(camFront, cameraInFluid);
        }
    }

    /**
     * Animate water (call only for fluid blocks)
     */
    public void animate() { // call only for fluid blocks
        if (optimizedTuples.isEmpty()) {
            return;
        }

        for (Tuple tuple : optimizedTuples.filter(ot -> ot.isBuffered() && !ot.isSolid() && ot.faceBits() > 0)) {
            if (tuple.isBuffered() && !tuple.isSolid() && tuple.faceBits() > 0) {
                tuple.animate();
            }
        }
    }

    /**
     * Standard render (slow)
     *
     * @param shaderProgram voxel shader
     * @param renderFlag what is renderered
     */
    public void render(ShaderProgram shaderProgram, int renderFlag) {
        if (optimizedTuples.isEmpty()) {
            return;
        }

        final boolean renderLights = (renderFlag & LIGHT_MASK) != 0;
        final boolean renderWater = (renderFlag & WATER_MASK) != 0;
        final boolean renderShadow = (renderFlag & SHADOW_MASK) != 0;

        final LightSources lightSources = (renderLights) ? gameObject.levelContainer.lightSources : LightSources.NONE;
        final Texture waterTexture = (renderWater) ? gameObject.waterRenderer.getFrameBuffer().getTexture() : Texture.EMPTY;
        final Texture shadowTexture = (renderShadow) ? gameObject.shadowRenderer.getFrameBuffer().getTexture() : Texture.EMPTY;
        for (Tuple tuple : optimizedTuples.filter(ot -> ot.faceBits() > 0)) {
            if (!tuple.isBuffered()) {
                tuple.bufferAll();
            }

            tuple.renderInstanced(shaderProgram, lightSources, waterTexture, shadowTexture);
        }
    }

    /**
     * Static render (faster). Batched & Instanced rendering is being used.
     *
     * @param shaderProgram voxel shader
     * @param renderFlag what is renderered
     */
    public void renderStatic(ShaderProgram shaderProgram, int renderFlag) {
        if (optimizedTuples.isEmpty()) {
            return;
        }

        final boolean renderLights = (renderFlag & LIGHT_MASK) != 0;
        final boolean renderWater = (renderFlag & WATER_MASK) != 0;
        final boolean renderShadow = (renderFlag & SHADOW_MASK) != 0;

        final LightSources lightSources = (renderLights) ? gameObject.levelContainer.lightSources : LightSources.NONE;
        final Texture waterTexture = (renderWater) ? gameObject.waterRenderer.getFrameBuffer().getTexture() : Texture.EMPTY;
        final Texture shadowTexture = (renderShadow) ? gameObject.shadowRenderer.getFrameBuffer().getTexture() : Texture.EMPTY;

        Tuple.renderInstanced(
                optimizedTuples.filter(ot -> ot.faceBits() > 0),
                shaderProgram, lightSources, waterTexture, shadowTexture
        );
    }

    /**
     * Push changes. Push working tuples to optimizing tuples. By coping each
     * optimizing to working.
     */
    public synchronized void push() {
        optimizedTuples = workingTuples.copy();
    }

    /**
     * Swap working tuples with optimized tuples. What was built by optimization
     * could be rendered (drawn). Called from Game Renderer.
     */
    public synchronized void swap() {
        IList<Tuple> temp = optimizedTuples;
        optimizedTuples = workingTuples;
        workingTuples = temp;
    }

    /**
     * Pull from recent. Pull optimized tuples to working tuples. By coping each
     * working to optimized.
     */
    public synchronized void pull() {
        workingTuples.addAll(optimizedTuples.filter(ot -> !workingTuples.contains(ot)));
    }

    /**
     * Static render (faster). Batched & Instanced rendering is being used.
     * Experimental.
     *
     * @param shaderProgram voxel shader
     * @param renderFlag what is renderered
     */
    @Deprecated
    public void renderStaticTBO(ShaderProgram shaderProgram, int renderFlag) {
        if (optimizedTuples.isEmpty()) {
            return;
        }

        final boolean renderLights = (renderFlag & LIGHT_MASK) != 0;
        final boolean renderWater = (renderFlag & WATER_MASK) != 0;
        final boolean renderShadow = (renderFlag & SHADOW_MASK) != 0;

        final LightSources lightSources = (renderLights) ? gameObject.levelContainer.lightSources : LightSources.NONE;
        final Texture waterTexture = (renderWater) ? gameObject.waterRenderer.getFrameBuffer().getTexture() : Texture.EMPTY;
        final Texture shadowTexture = (renderShadow) ? gameObject.shadowRenderer.getFrameBuffer().getTexture() : Texture.EMPTY;

        if (fullyOptimized && !tupleBuffObj.isBuffered()) {
            tupleBuffObj.bufferBatchAll();
        }

        Tuple.renderInstanced(
                optimizedTuples, tupleBuffObj,
                shaderProgram, lightSources, waterTexture, shadowTexture
        );
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public TupleBufferObject getTupleBuffObj() {
        return tupleBuffObj;
    }

    /**
     * Update vertices. For light definition, for instance.
     */
    public void subBufferVertices() {
        if (!fullyOptimized || optimizedTuples.isEmpty()) {
            return;
        }

        for (Tuple tuple : optimizedTuples) {
            if (tuple.isBuffered() && tuple.faceBits() > 0) {
                tuple.subBufferVertices(); // update lights
            }
        }
    }

    /**
     * Clear working & optimization tuples
     */
    public void clear() {
        workingTuples.clear();
        optimizedTuples.clear();
        fullyOptimized = false;
    }

    /**
     * Delete all the resources
     */
    public void release() {
        optimizedTuples.forEach(t -> t.release());
    }

    public IList<Tuple> getOptimizedTuples() {
        return optimizedTuples;
    }

    public boolean isFullyOptimized() {
        return fullyOptimized;
    }

    public void setFullyOptimized(boolean fullyOptimized) {
        this.fullyOptimized = fullyOptimized;
    }

    public boolean isOptimizing() {
        return optimizing;
    }

    public Chunks getChunks() {
        return chunks;
    }

    public int getTexProcIndex() {
        return texProcIndex;
    }

    public int getBitPos() {
        return lastFaceBits;
    }

    public int getLastFaceBits() {
        return lastFaceBits;
    }

    public IList<Tuple> getWorkingTuples() {
        return workingTuples;
    }

}
