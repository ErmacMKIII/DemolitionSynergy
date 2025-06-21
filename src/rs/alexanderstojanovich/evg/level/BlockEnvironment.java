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

import java.util.HashMap;
import java.util.Map;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.chunk.Chunks;
import rs.alexanderstojanovich.evg.chunk.Tuple;
import rs.alexanderstojanovich.evg.chunk.TupleBufferObject;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.main.GameRenderer;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.resources.Assets;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 * Module with blocks from all the chunks. Effectively ready for rendering after
 * optimization.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class BlockEnvironment {

    public static final Configuration cfg = Configuration.getInstance();

    public static final int LIGHT_MASK = 0x01;
    public static final int WATER_MASK = 0x02;
    public static final int SHADOW_MASK = 0x04;

    public final GameObject gameObject;

    /**
     * Working tuples (from update)
     */
    protected volatile IList<Tuple> workingTuples = new GapList<>();
    /**
     * Optimizes tuples (from render)
     */
    protected volatile IList<Tuple> optimizedTuples = new GapList<>();
    /**
     * Lookup table for faster tuple access (avoiding multiple filters)
     */
    protected final Map<String, Map<Integer, Tuple>> tupleLookup = new HashMap<>();

    /**
     * Some tlist based on split facebits(0-63) into Max Iterations of Game
     * Renderer
     */
    protected final IList<IList<Integer>> sometIList = splitFaceBits(64, GameRenderer.NUM_OF_PASSES_MAX);

    /**
     * Modified tuples (from update/render). Meaning from update they are
     * modified and need to be pushed to render.
     */
//    public final IList<Tuple> modifiedTuples = new GapList<>();
    protected volatile boolean optimizing = false;
    protected final Chunks chunks;

    protected int lastFaceBits = 0; // starting from one, cuz zero is not rendered
    public static final int NUM_OF_PASSES_MAX = Configuration.getInstance().getOptimizationPasses();
    public final IList<String> modifiedWorkingTupleNames = new GapList<>();

    /**
     * Contains all batched buffer(s). As one.
     */
    public final TupleBufferObject tupleBuffObj = new TupleBufferObject(optimizedTuples);

    public BlockEnvironment(GameObject gameObject, Chunks chunks) {
        this.gameObject = gameObject;
        this.chunks = chunks;
    }

    /**
     * Optimization for tuples from all the chunks. The world is built
     * incrementally and consists of two passes. Also includes modifications to
     * work with Tuple Buffer Object (TBO). Modified tuples are pushed to the
     * optimized stream. (Chat GPT)
     *
     * @param vqueue visible chunkId queue
     * @param camera in-game camera
     */
    public void updateNoptimize(IList<Integer> vqueue, Camera camera) {
        optimizing = true;

        // Determine lastFaceBits mask
        final int mask0 = Block.getVisibleFaceBitsFast(camera.getFront(), LevelContainer.actorInFluid ? 0f : 45f);
        workingTuples.removeIf(ot -> (ot.faceBits() & mask0) == 0);
        int lastFaceBitsCopy = lastFaceBits;

        // Create a lookup table for faster tuple access (avoiding multiple filters)
        if (lastFaceBits == 0) {
            pull();
            tupleLookup.clear();
            for (Tuple t : workingTuples) {
                tupleLookup
                        .computeIfAbsent(t.texName(), k -> new HashMap<>())
                        .put(t.faceBits(), t);
            }
        }

        for (String tex : Assets.TEX_WORLD) {
            for (int j = 0; j < NUM_OF_PASSES_MAX; j++) {
                final int faceBits = (++lastFaceBitsCopy) & 63;
                if ((faceBits & (mask0 & 63)) != 0) {
                    // PASS 1: Fetch or Create Tuple
                    Tuple optmTuple = tupleLookup
                            .computeIfAbsent(tex, k -> new HashMap<>())
                            .computeIfAbsent(faceBits, fb -> {
                                Tuple newTuple = new Tuple(tex, fb);
                                workingTuples.add(newTuple);
                                return newTuple;
                            });

                    // PASS 2: Process Chunks and Fill Tuples
                    final IList<Block> selectedBlockList = chunks.getFilteredBlockList(tex, faceBits, vqueue);
                    if (selectedBlockList != null) {
                        boolean modified = optmTuple.blockList.addAll(
                                selectedBlockList.filter(blk -> blk.getTexName().equals(tex) && blk.getFaceBits() == faceBits
                                && camera.doesSeeEff(blk, 30f) && !optmTuple.blockList.contains(blk))
                        );

                        if (modified) {
                            modifiedWorkingTupleNames.addIfAbsent(optmTuple.getName());
                        }
                    }
                }
            }
        }

        lastFaceBits += NUM_OF_PASSES_MAX;
        lastFaceBits &= 63;

        if (lastFaceBits == 0) {
            workingTuples.sort(Tuple.TUPLE_COMP);

            // Only process modified tuples
            workingTuples
                    .filter(wt -> modifiedWorkingTupleNames.contains(wt.getName()))
                    .forEach(wt -> {
                        wt.blockList.sort(Block.UNIQUE_BLOCK_CMP);
                        wt.setBuffered(false);
                    });

            // Remove empty tuples
            workingTuples.removeIf(wt -> wt.blockList.isEmpty());

            modifiedWorkingTupleNames.clear();
            swap();
        }

        optimizing = false;
    }

    /**
     * Reorder group of vertices of optimized tuples if underwater
     *
     * @param cameraInFluid is camera in fluid (checked by level container
     * externally)
     */
    public void prepare(boolean cameraInFluid) { // call only for fluid blocks before rendering
        if (optimizing || optimizedTuples.isEmpty()) {
            return;
        }

        for (Tuple tuple : optimizedTuples.filter(ot -> ot.isBuffered() && !ot.isSolid() && ot.faceBits() != 0)) {
            tuple.prepare(cameraInFluid);
        }
    }

    public static IList<IList<Integer>> splitFaceBits(int totalBits, int numPasses) {
        IList<IList<Integer>> passes = new GapList<>();

        // Calculate base size and remainder
        int baseSize = totalBits / numPasses; // 6
        int remainder = totalBits % numPasses; // 4

        // Split the face bits into passes
        int currentBit = 0;
        for (int i = 0; i < numPasses; i++) {
            int passSize = baseSize + (i < remainder ? 1 : 0); // Add 1 if this pass gets an extra bit
            IList<Integer> pass = new GapList<>();
            for (int j = 0; j < passSize; j++) {
                pass.add(currentBit++);
            }
            passes.add(pass);
        }
        return passes;
    }

    /**
     * Reorder group of vertices of optimized tuples if underwater
     *
     * Still very heavy operation.
     *
     * @param camFront camera front (used for ray-trace)
     * @param cameraInFluid is camera in fluid (checked by level container
     * externally)
     */
    public void prepare(Vector3f camFront, boolean cameraInFluid) { // call only for fluid blocks before rendering
        if (optimizing || optimizedTuples.isEmpty()) {
            return;
        }

        for (Tuple tuple : optimizedTuples.filter(ot -> ot.isBuffered() && !ot.isSolid()
                && sometIList.get(GameRenderer.getFps() & (GameRenderer.NUM_OF_PASSES_MAX - 1)).contains(ot.faceBits()))) {
            tuple.prepare(camFront, cameraInFluid);
        }
    }

    /**
     * Animate water (call only for fluid blocks)
     */
    public void animate() { // call only for fluid blocks
        if (optimizing || optimizedTuples.isEmpty()) {
            return;
        }

        for (Tuple tuple : optimizedTuples.filter(ot -> ot.isBuffered() && !ot.isSolid() && ot.texName().equals("water") && ot.faceBits() > 0)) {
            tuple.animate();
        }
    }

    /**
     * Standard render (slow)
     *
     * @param shaderProgram voxel shader
     * @param renderFlag what is renderered
     */
    public void render(ShaderProgram shaderProgram, int renderFlag) {
        if (optimizing || optimizedTuples.isEmpty()) {
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
        if (optimizing || optimizedTuples.isEmpty()) {
            return;
        }

        final boolean renderLights = (renderFlag & LIGHT_MASK) != 0;
        final boolean renderWater = (renderFlag & WATER_MASK) != 0;
        final boolean renderShadow = (renderFlag & SHADOW_MASK) != 0;

        final LightSources lightSources = (renderLights) ? gameObject.levelContainer.lightSources : LightSources.NONE;
        final Texture waterTexture = (renderWater) ? gameObject.waterRenderer.getFrameBuffer().getTexture() : Texture.EMPTY;
        final Texture shadowTexture = (renderShadow) ? gameObject.shadowRenderer.getFrameBuffer().getTexture() : Texture.EMPTY;

        optimizedTuples.filter(ot -> !ot.isBuffered() && ot.faceBits() > 0).forEach(ot -> ot.bufferAll());
        Tuple.renderInstanced(
                optimizedTuples.filter(ot -> ot.isBuffered() && ot.faceBits() > 0),
                shaderProgram, lightSources, gameObject.GameAssets.WORLD, waterTexture, shadowTexture
        );
    }

    /**
     * Push changes. Push working tuples to optimizing tuples. By coping each
     * optimizing to working.
     */
    public void push() {
        optimizedTuples = workingTuples.copy();
    }

    /**
     * Swap working tuples with optimized tuples. What was built by optimization
     * could be rendered (drawn). Called from Game Renderer.
     */
    public void swap() {
        IList<Tuple> temp = optimizedTuples;
        optimizedTuples = workingTuples;
        workingTuples = temp;
    }

    /**
     * Pull from recent. Pull optimized tuples to working tuples. By coping each
     * working to optimized.
     */
    public void pull() {
        workingTuples.addAll(optimizedTuples.filter(ot -> !workingTuples.contains(ot)));
        workingTuples.sort(Tuple.TUPLE_COMP);
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

        if (!tupleBuffObj.isBuffered()) {
            tupleBuffObj.bufferBatchAll();
        }

        Tuple.renderInstanced(
                optimizedTuples, tupleBuffObj,
                shaderProgram, lightSources, gameObject.GameAssets.WORLD, waterTexture, shadowTexture
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
        if (optimizedTuples.isEmpty()) {
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

    public boolean isOptimizing() {
        return optimizing;
    }

    public Chunks getChunks() {
        return chunks;
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

    public IList<String> getModifiedWorkingTupleNames() {
        return modifiedWorkingTupleNames;
    }

    public Map<String, Map<Integer, Tuple>> getTupleLookup() {
        return tupleLookup;
    }

    public IList<IList<Integer>> getSometIList() {
        return sometIList;
    }

}
