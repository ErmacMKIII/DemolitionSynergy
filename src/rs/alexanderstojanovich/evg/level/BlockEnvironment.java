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
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class BlockEnvironment {

    /**
     * Shared configuration instance for game settings
     */
    public static final Configuration cfg = Configuration.getInstance();

    public static final int LIGHT_MASK = 0x01;
    public static final int WATER_MASK = 0x02;
    public static final int SHADOW_MASK = 0x04;

    public static final int NUM_OF_PASSES_MAX = Configuration.getInstance().getOptimizationPasses();
    /**
     * Bitmask for face bits (6 bits = 64 possible combinations)
     */
    private static final int FACE_BITS_MASK = 0x3F;

    /**
     * Flag indicating optimization is in progress. Volatile ensures proper
     * visibility across threads.
     */
    protected volatile boolean optimizing = false;

    /**
     * Flag indicating tuples need GPU buffer updates. Set when tuples are
     * modified and cleared after finalization.
     */
    protected boolean needsRebuffer = false;

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

    public final GameObject gameObject;

    /**
     * Optimizes tuples (from render, finalized form)
     */
    protected final IList<Tuple> optimizedTuples = new GapList<>();

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
    protected final Chunks chunks;

    protected int lastFaceBits = 0; // starting from one, cuz zero is not rendered

    /**
     * Contains all batched buffer(s). As one.
     */
    public final @Deprecated
    TupleBufferObject tupleBuffObj = null;

    public BlockEnvironment(GameObject gameObject, Chunks chunks) {
        this.gameObject = gameObject;
        this.chunks = chunks;
    }

    /**
     * Optimizes the block environment by processing visible chunks. Performs
     * incremental optimization across multiple frames, modifying tuples
     * directly in the optimized list rather than using transient copies.
     *
     * @param vqueue list of visible chunk IDs to process
     * @param camera the game camera for visibility determination
     */
    public void optimizeTuples(IList<Integer> vqueue, Camera camera) {
        optimizing = true;
        try {
            // Determine which faces are visible based on camera
            final int faceMask = calculateFaceMask(camera);

            // Rebuild lookup if starting new cycle and inconsistencies exist
            if (lastFaceBits == 0) {
                ensureLookupConsistency();
            }

            // Process all visible chunks for current face bits
            processVisibleChunks(vqueue, camera, faceMask);

            // Advance face bits counter for next pass
            lastFaceBits = (lastFaceBits + cfg.getOptimizationPasses()) & FACE_BITS_MASK;

            // Finalize if completed full cycle and changes occurred
            if (lastFaceBits == 0 && needsRebuffer) {
                finalizeOptimization();
//                DSLogger.reportInfo("size=" + optimizedTuples.size(), null);
            }
        } finally {
            optimizing = false;
        }
    }

    /**
     * Calculates which block faces are visible based on camera orientation.
     *
     * @param camera the game camera to check against
     * @return bitmask of currently visible faces
     */
    private int calculateFaceMask(Camera camera) {
        // Adjust angle based on whether actor is in fluid
        final float angleAdjust = LevelContainer.actorInFluid ? 0f : 45f;
        return Block.getVisibleFaceBitsFast(camera.getFront(), angleAdjust) & FACE_BITS_MASK;
    }

    /**
     * Ensures the lookup table accurately reflects the optimized tuples. Only
     * rebuilds if inconsistencies are detected to avoid unnecessary work.
     */
    private void ensureLookupConsistency() {
        synchronized (optimizedTuples) {
            // Check if all lookup tuples exist in optimized list
            boolean consistent = tupleLookup.values().stream()
                    .flatMap(m -> m.values().stream())
                    .allMatch(t -> optimizedTuples.contains(t));

            if (!consistent || tupleLookup.isEmpty()) {
                tupleLookup.clear();
                for (Tuple t : optimizedTuples) {
                    tupleLookup.computeIfAbsent(t.texName(), k -> new HashMap<>())
                            .put(t.faceBits(), t);
                }
            }
        }
    }

    /**
     * Processes all visible chunks for the current optimization pass.
     *
     * @param vqueue visible chunk IDs to process
     * @param camera game camera for visibility checks
     * @param faceMask bitmask of currently visible faces
     */
    private void processVisibleChunks(IList<Integer> vqueue, Camera camera, int faceMask) {
        int currentFaceBits = lastFaceBits;

        // Process each texture type in the world
        for (String tex : Assets.TEX_WORLD) {
            // Get or create face bit mapping for this texture
            Map<Integer, Tuple> faceBitMap = tupleLookup.computeIfAbsent(tex, k -> new HashMap<>());

            // Process configured number of face bits in this pass
            for (int i = 0; i < cfg.getOptimizationPasses(); i++) {
                currentFaceBits = (currentFaceBits + 1) & FACE_BITS_MASK;

                // Only process if these face bits are currently visible
                if ((currentFaceBits & faceMask) != 0) {
                    processFaceBitGroup(vqueue, camera, tex, faceBitMap, currentFaceBits);
                }
            }
        }
    }

    /**
     * Processes a specific face bit group for a texture across visible chunks.
     *
     * @param vqueue visible chunk IDs
     * @param camera game camera
     * @param tex current texture being processed
     * @param faceBitMap map of face bits to tuples for this texture
     * @param faceBits current face bits being processed
     */
    private void processFaceBitGroup(IList<Integer> vqueue, Camera camera,
            String tex, Map<Integer, Tuple> faceBitMap,
            int faceBits) {
        Tuple tuple;
        synchronized (optimizedTuples) {
            // Get or create tuple for this texture/facebits combination
            tuple = faceBitMap.computeIfAbsent(faceBits, fb -> {
                Tuple newTuple = new Tuple(tex, fb);
                optimizedTuples.add(newTuple);
                needsRebuffer = true;
                return newTuple;
            });
        }

        // Process blocks from visible chunks
        boolean modified = processChunkBlocks(vqueue, camera, tuple);

        if (modified) {
            tuple.setBuffered(false);
            needsRebuffer = true;
        }
    }

    /**
     * Processes blocks from visible chunks for a specific tuple.
     *
     * @param vqueue visible chunk IDs
     * @param camera game camera
     * @param tuple the tuple being updated
     * @return true if the tuple was modified, false otherwise
     */
    private boolean processChunkBlocks(IList<Integer> vqueue, Camera camera, Tuple tuple) {
        boolean modified = false;

        final float angleDegrees = 30f;

        // Clear entire tuple
        tuple.blockList.clear();

        // Filter each visible chunk for relevant & visible blocks
        IList<Block> filterBlks = chunks.getFilteredBlockList(
                tuple.texName(), tuple.faceBits(), vqueue,
                camera, angleDegrees
        );
        if (filterBlks != null && !filterBlks.isEmpty()) {
            modified |= tuple.blockList.addAll(filterBlks);
        }

        // Sort if modifications occurred
        if (modified) {
            tuple.blockList.sort(Block.UNIQUE_BLOCK_CMP);
        }

        return modified;
    }

    /**
     * Finalizes the optimization process after completing a full cycle. Removes
     * empty tuples, sorts the final list, and prepares for rendering.
     */
    private void finalizeOptimization() {
        synchronized (optimizedTuples) {
            // Clean up any empty tuples that were created
            optimizedTuples.removeIf(t -> t.blockList.isEmpty());

            // Ensure consistent ordering for rendering
            optimizedTuples.sort(Tuple.TUPLE_COMP);
        }
        // Reset flag until next changes occur
        needsRebuffer = false;
    }

    /**
     * Reorder group of vertices of optimized tuples if underwater
     *
     * @param cameraInFluid is camera in fluid (checked by level container
     * externally)
     */
    public void prepare(boolean cameraInFluid) { // call only for fluid blocks before rendering
        synchronized (optimizedTuples) {
            if (optimizing || optimizedTuples.isEmpty()) {
                return;
            }
        }

        IList<Tuple> filtered = optimizedTuples.filter(ot -> ot.isBuffered() && !ot.isSolid() && ot.faceBits() != 0);
        for (Tuple tuple : filtered) {
            tuple.prepare(cameraInFluid);
        }
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
        synchronized (optimizedTuples) {
            if (optimizing || optimizedTuples.isEmpty()) {
                return;
            }
        }

        IList<Tuple> filtered = optimizedTuples.filter(ot -> ot.isBuffered() && !ot.isSolid() && sometIList.get(GameRenderer.getFps() & (GameRenderer.NUM_OF_PASSES_MAX - 1)).contains(ot.faceBits()));

        for (Tuple tuple : filtered) {
            tuple.prepare(camFront, cameraInFluid);
        }
    }

    /**
     * Animate water (call only for fluid blocks)
     */
    public void animate() { // call only for fluid blocks
        synchronized (optimizedTuples) {
            if (optimizing || optimizedTuples.isEmpty()) {
                return;
            }
        }

        IList<Tuple> filtered;
        synchronized (optimizedTuples) {
            filtered = optimizedTuples.filter(ot -> ot.isBuffered() && !ot.isSolid() && ot.texName().equals("water") && ot.faceBits() > 0);
        }
        for (Tuple tuple : filtered) {
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
        synchronized (optimizedTuples) {
            if (optimizing || optimizedTuples.isEmpty()) {
                return;
            }
        }

        final boolean renderLights = (renderFlag & LIGHT_MASK) != 0;
        final boolean renderWater = (renderFlag & WATER_MASK) != 0;
        final boolean renderShadow = (renderFlag & SHADOW_MASK) != 0;

        final LightSources lightSources = (renderLights) ? gameObject.levelContainer.lightSources : LightSources.NONE;
        final Texture waterTexture = (renderWater) ? gameObject.waterRenderer.getFrameBuffer().getTexture() : Texture.EMPTY;
        final Texture shadowTexture = (renderShadow) ? gameObject.shadowRenderer.getFrameBuffer().getTexture() : Texture.EMPTY;

        IList<Tuple> filtered;
        synchronized (optimizedTuples) {
            filtered = optimizedTuples.filter(ot -> ot.faceBits() > 0);
        }

        for (Tuple tuple : filtered) {
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
        synchronized (optimizedTuples) {
            if (optimizing || optimizedTuples.isEmpty()) {
                return;
            }
        }

        final boolean renderLights = (renderFlag & LIGHT_MASK) != 0;
        final boolean renderWater = (renderFlag & WATER_MASK) != 0;
        final boolean renderShadow = (renderFlag & SHADOW_MASK) != 0;

        final LightSources lightSources = (renderLights) ? gameObject.levelContainer.lightSources : LightSources.NONE;
        final Texture waterTexture = (renderWater) ? gameObject.waterRenderer.getFrameBuffer().getTexture() : Texture.EMPTY;
        final Texture shadowTexture = (renderShadow) ? gameObject.shadowRenderer.getFrameBuffer().getTexture() : Texture.EMPTY;

        IList<Tuple> filtered;
        synchronized (optimizedTuples) {
            optimizedTuples.filter(ot -> !ot.isBuffered() && ot.faceBits() > 0).forEach(ot -> ot.bufferAll());
            filtered = optimizedTuples.filter(ot -> ot.isBuffered() && ot.faceBits() > 0);
        }
        Tuple.renderInstanced(
                filtered,
                shaderProgram, lightSources, gameObject.GameAssets.WORLD,
                waterTexture, shadowTexture
        );
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
        synchronized (optimizedTuples) {
            if (optimizedTuples.isEmpty()) {
                return;
            }
        }

        IList<Tuple> filtered;
        synchronized (optimizedTuples) {
            filtered = optimizedTuples.filter(tuple -> tuple.isBuffered() && tuple.faceBits() > 0);
        }

        for (Tuple tuple : filtered) {
            tuple.subBufferVertices(); // update lights
        }
    }

    /**
     * Delete all the resources
     */
    public synchronized void release() {
        optimizedTuples.forEach(t -> t.release());
    }

    /**
     * Clear optimization tuples
     */
    public void clear() {
        tupleLookup.clear();
        synchronized (optimizedTuples) {
            optimizedTuples.clear();
        }
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

    public Map<String, Map<Integer, Tuple>> getTupleLookup() {
        return tupleLookup;
    }

    public IList<IList<Integer>> getSometIList() {
        return sometIList;
    }

}
