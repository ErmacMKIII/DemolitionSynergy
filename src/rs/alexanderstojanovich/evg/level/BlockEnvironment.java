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

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.chunk.Chunks;
import rs.alexanderstojanovich.evg.chunk.Tuple;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.Game;
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

    public Matrix4f lightViewMatrix = new Matrix4f();
    private final GameObject gameObject;

    public final IList<Tuple> optimizedTuples = new GapList<>();
    protected boolean optimized = false;
    protected final Chunks chunks;
    protected int texProcIndex = 0;

    protected int lastFaceBits = 0; // starting from one, cuz zero is not rendered
    public static final int NUM_OF_PASSES_MAX = Configuration.getInstance().getOptimizationPasses();

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

        optimized = true;
    }

    /**
     * Improved version of optimization for tuples from all the chunks. World is
     * being built incrementally.
     *
     * @param vqueue visible chunkId queue
     * @param camFront camera front (look at vector)
     */
    public void optimizeFast(IList<Integer> vqueue, Vector3f camFront) {
        // determine lastFaceBits mask
        final int mask = Block.getVisibleFaceBitsFast(camFront);
        boolean someRemoved = optimizedTuples.removeIf(ot -> (ot.faceBits() & mask) == 0);

        // some removals are made
        if (someRemoved) {
            optimized = false;
        }

        // determine texture type to process - split
        if (texProcIndex++ == Texture.TEX_WORLD.length - 1) {
            texProcIndex = 0;
        }

        final String tex = Texture.TEX_WORLD[texProcIndex];

        for (int j = 0; j < NUM_OF_PASSES_MAX; j++) {
            // assign last value & increment to next value with limit to 63
            final int faceBits = (++lastFaceBits) & 63;
            if ((faceBits & (mask & 63)) != 0) {
                chunks.chunkList.forEach(chnk -> { // for all chunks
                    if (vqueue.contains(chnk.id)) { // visible ones
                        // select correlated tuples
                        final IList<Tuple> selectedTuples = chnk.tupleList.filter(t -> t.texName().equals(tex) && t.faceBits() == faceBits);
                        // for each selected tuple
                        selectedTuples.forEach(st -> {
                            st.blockList.forEach(blk -> {
                                Tuple optmTuple = optimizedTuples.getIf(ot -> ot.texName().equals(tex) && ot.faceBits() == faceBits);
                                // if optimized doesn't exist
                                if (optmTuple == null) {
                                    // create new one since is needed to add blocks
                                    optmTuple = new Tuple(tex, faceBits);
                                    // add to the optimized list
                                    boolean someAdded = optimizedTuples.add(optmTuple);
                                    // some addition(s) are made
                                    if (someAdded) {
                                        optimized = false;
                                        // sort so it remains ordered
                                        optimizedTuples.sort(Tuple.TUPLE_COMP);
                                    }
                                } else {
                                    // add absent blocks
                                    boolean modified = optmTuple.blockList.addIfAbsent(blk);
                                    if (modified) {
                                        // sort so it does remains ordered
                                        optmTuple.blockList.sort(Block.UNIQUE_BLOCK_CMP);
                                        // sets to unbuffer if modified
                                        optmTuple.setBuffered(false);
                                    }
                                }
                            });
                        });
                    }
                });
            }
        }

        // if last bits is processed start from beginning next time
        if (lastFaceBits == 64) {
            lastFaceBits = 0;
        }

        // if full circle with all textures & facebits has been completed
        if (texProcIndex == 0 && lastFaceBits == 0) {
            optimized = true;
        }
    }

    /**
     * Reorder group of vertices of optimized tuples if underwater
     *
     * @param cameraInFluid is camera in fluid (checked by level container
     * externally)
     */
    public void prepare(boolean cameraInFluid) { // call only for fluid blocks before rendering
        if (!optimized) {
            return;
        }

        if (Game.getUpsTicks() >= 1.0) {
            for (Tuple tuple : optimizedTuples) {
                if (tuple.isBuffered() && !tuple.isSolid() && tuple.faceBits() > 0) {
                    tuple.prepare(cameraInFluid);
                }
            }
        }
    }

    /**
     * Animate water (call only for fluid blocks)
     */
    public void animate() { // call only for fluid blocks
        if (!optimized) {
            return;
        }

        if (Game.getUpsTicks() < 1.0) {
            for (Tuple tuple : optimizedTuples) {
                if (tuple.isBuffered() && !tuple.isSolid() && tuple.faceBits() > 0) {
                    tuple.animate();
                }
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
        for (Tuple tuple : optimizedTuples) {
            if (!tuple.isBuffered()) {
                tuple.bufferAll();
            }

            tuple.renderInstanced(shaderProgram, lightSources, waterTexture, shadowTexture);
        }
    }

    /**
     * Static render (faster)
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

        Tuple.renderInstanced(optimizedTuples, shaderProgram, lightSources, waterTexture, shadowTexture);
    }

    /**
     * Update vertices. For light definition, for instance.
     */
    public void subBufferVertices() {
        if (!optimized || optimizedTuples.isEmpty()) {
            return;
        }

        for (Tuple tuple : optimizedTuples) {
            if (tuple.isBuffered() && tuple.faceBits() > 0) {
                tuple.subBufferVertices(); // update lights
            }
        }
    }

    /**
     * Clear optimization tuples
     */
    public void clear() {
        optimizedTuples.clear();
        optimized = false;
    }

    public IList<Tuple> getOptimizedTuples() {
        return optimizedTuples;
    }

    public boolean isOptimized() {
        return optimized;
    }

    public void setOptimized(boolean optimized) {
        this.optimized = optimized;
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

}
