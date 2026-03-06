/*
 * Copyright (C) 2020 Aleksandar Stojanovic <coas91@rocketmail.com>
 * ...
 */
package rs.alexanderstojanovich.evg.chunk;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.models.Block;
import static rs.alexanderstojanovich.evg.models.Block.getRayTraceMultiFaceFast;
import rs.alexanderstojanovich.evg.models.Vertex;
import rs.alexanderstojanovich.evg.resources.Assets;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.texture.TextureArray;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.ModelUtils;

/**
 * List of world blocks with same face enabled bits and same texture layer.
 * Designated for instanced rendering with texture array (sampler2DArray).
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Tuple extends Series {

    public static final int VEC2_SIZE = 2;
    public static final int VEC3_SIZE = 3;
    public static final int VEC4_SIZE = 4;
    public static final int MAT4_SIZE = 16;

    /**
     * Maps texture name -> layer index in the GL_TEXTURE_2D_ARRAY.
     * Populated once at startup from Assets.TEX_WORLD ordering.
     */
    public static final Map<String, Integer> TEX_LAYER_MAP = new HashMap<>();

    /**
     * Layer index for "water" texture (used for isSolid check).
     * Set during TEX_LAYER_MAP initialization.
     */
    public static int WATER_LAYER_INDEX = -1;

    static {
        // Build layer map from Assets.TEX_WORLD ordering
        // Assets.TEX_WORLD must be ordered consistently (e.g. ["grass","stone","water",...])
        String[] texWorld = Assets.TEX_WORLD;
        for (int i = 0; i < texWorld.length; i++) {
            TEX_LAYER_MAP.put(texWorld[i], i);
            if (texWorld[i].equals("water")) {
                WATER_LAYER_INDEX = i;
            }
        }
    }

    protected int vec4Vbo = 0; // color
    protected static FloatBuffer vec4FloatColorBuff = null;

    protected int mat4Vbo = 0; // model matrix [col0, col1, col2, col3]
    protected static FloatBuffer mat4FloatModelBuff = null;

    protected final String name;

    protected final int facesNum;
    protected final int faceEnBits;

    /**
     * Layer index into GL_TEXTURE_2D_ARRAY. Encoded as UV.z per vertex.
     */
    protected final int layerIndex;

    /**
     * Tuple comparator sorting tuples by (String) name.
     */
    public static final Comparator<Tuple> TUPLE_COMP = (Tuple o1, Tuple o2) -> o1.getName().compareTo(o2.getName());
    /**
     * Unique string name for this tuple, derived from texture name and face enabled bits.
     * Used for hashing and equality checks.
     */
    public final String texName;

    /**
     * Construct new tuple by definition texName x face-enabled-bits.
     * Layer index is resolved from TEX_LAYER_MAP.
     *
     * @param texName    texture name
     * @param faceEnBits face enabled bits
     */
    public Tuple(String texName, int faceEnBits) {
        this.texName = texName;
        this.faceEnBits = faceEnBits;
        this.layerIndex = TEX_LAYER_MAP.getOrDefault(texName, 0);
        this.name = String.format("%s%02d", texName, faceEnBits);

        int numberOfOnes = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            if ((faceEnBits & (1 << j)) != 0) {
                numberOfOnes++;
            }
        }

        this.facesNum = numberOfOnes;
        this.verticesNum = 4 * numberOfOnes;
        this.indicesNum = 6 * numberOfOnes;
    }

    /**
     * Copy constructor. Creates a new tuple with the same properties and a copy of the block list.
     *
     * @param original to copy properties from
     */
    public Tuple(Tuple original) {
        this.texName = original.texName;
        this.faceEnBits = original.faceEnBits;
        this.layerIndex = original.layerIndex;
        this.name = String.format("%s%02d", original.texName(), original.faceBits());
        this.facesNum = original.facesNum;
        this.verticesNum = original.verticesNum;
        this.indicesNum = original.indicesNum;

        this.blockList.clear();
        this.blockList.addAll(original.blockList);
    }

    /**
     * Gets Block from the tuple block list. Complexity is O(log(n)+k).
     *
     * @param pos Vector3f position of the block
     * @return block if found (null if not found)
     */
    public Block getBlock(Vector3f pos) {
        String key = ModelUtils.blockSpecsToUniqueString(this.texName(), pos);

        int left = 0;
        int right = this.blockList.size() - 1;
        int startIndex = -1;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            int cmp = Block.UNIQUE_BLOCK_CMP.compare(blockList.get(mid), blockList.get(mid)); // placeholder
            String midKey = ModelUtils.blockSpecsToUniqueString(this.texName(), blockList.get(mid).getPos());
            int c = midKey.compareTo(key);
            if (c < 0) left = mid + 1;
            else if (c > 0) right = mid - 1;
            else { startIndex = mid; right = mid - 1; }
        }

        left = 0; right = this.blockList.size() - 1;
        int endIndex = -1;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            String midKey = ModelUtils.blockSpecsToUniqueString(this.texName(), blockList.get(mid).getPos());
            int c = midKey.compareTo(key);
            if (c < 0) left = mid + 1;
            else if (c > 0) right = mid - 1;
            else { endIndex = mid; left = mid + 1; }
        }

        if (startIndex != -1 && endIndex != -1) {
            for (int i = startIndex; i <= endIndex; i++) {
                Block b = blockList.get(i);
                if (b.getPos().equals(pos)) return b;
            }
        }

        return null;
    }

    /**
     * Gets Block from the tuple block list by id. Complexity is O(log(n)+k).
     *
     * @param pos   Vector3f position of the block
     * @param blkId block unique id
     * @return block if found (null if not found)
     */
    public Block getBlock(Vector3f pos, String blkId) {
        String key = blkId;

        int left = 0;
        int right = this.blockList.size() - 1;
        int startIndex = -1;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            String midKey = ModelUtils.blockSpecsToUniqueString(this.texName(), blockList.get(mid).getPos());
            int c = midKey.compareTo(key);
            if (c < 0) left = mid + 1;
            else if (c > 0) right = mid - 1;
            else { startIndex = mid; right = mid - 1; }
        }

        left = 0; right = this.blockList.size() - 1;
        int endIndex = -1;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            String midKey = ModelUtils.blockSpecsToUniqueString(this.texName(), blockList.get(mid).getPos());
            int c = midKey.compareTo(key);
            if (c < 0) left = mid + 1;
            else if (c > 0) right = mid - 1;
            else { endIndex = mid; left = mid + 1; }
        }

        if (startIndex != -1 && endIndex != -1) {
            for (int i = startIndex; i <= endIndex; i++) {
                Block b = blockList.get(i);
                if (b.getPos().equals(pos)) return b;
            }
        }

        return null;
    }

    /**
     * Buffer vertices with vec3 UV (u, v, layerIndex).
     * UV layout (location=2) is now vec3: xy=atlas UV, z=texture array layer.
     * Vertex.SIZE must be updated to 9 floats (pos3 + normal3 + uv3).
     */
    /**
     * Buffer vertices with vec3 UV (u, v, layerIndex).
     * UV layout (location=2) is now vec3: xy=atlas UV, z=texture array layer.
     * Writes 9 floats per vertex: pos(3) + normal(3) + uv(2) + layer(1).
     */
    @Override
    public boolean bufferVertices() {
        final int VERTEX_FLOATS = 9; // pos3 + normal3 + uv2 + layer1
        int someSize = blockList.size() * verticesNum * VERTEX_FLOATS;

        if (bigFloatBuff == null || bigFloatBuff.capacity() == 0) {
            bigFloatBuff = MemoryUtil.memCallocFloat(someSize);
        } else if (bigFloatBuff.capacity() < someSize) {
            bigFloatBuff = MemoryUtil.memRealloc(bigFloatBuff, someSize);
        }

        bigFloatBuff.position(0);
        bigFloatBuff.limit(someSize);

        if (bigFloatBuff.capacity() != 0 && MemoryUtil.memAddressSafe(bigFloatBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        float layer = (float) layerIndex;

        for (Block block : blockList) {
            for (Vertex vertex : block.getVertices()) {
                if (vertex.isEnabled()) {
                    bigFloatBuff.put(vertex.getPos().x)
                            .put(vertex.getPos().y)
                            .put(vertex.getPos().z)
                            .put(vertex.getNormal().x)
                            .put(vertex.getNormal().y)
                            .put(vertex.getNormal().z)
                            .put(vertex.getUv().x)
                            .put(vertex.getUv().y)
                            .put(layer);   // UV.z = texture array layer index
                }
            }
        }

        if (bigFloatBuff.position() != 0) {
            bigFloatBuff.flip();
        }

        if (bigVbo == 0) {
            bigVbo = GL15.glGenBuffers();
        }

        // Each vertex now has 9 floats: pos(3) + normal(3) + uv(2) + layer(1) = 36 bytes per vertex
        final int STRIDE = Vertex.EXTENDED_SIZE * 4; // 36 bytes

        if (bigFloatBuff.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bigFloatBuff, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, STRIDE, 0);  // pos
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, STRIDE, 12); // normal
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, STRIDE, 24); // uv vec3 (xy + layer)

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

    /**
     * SubBuffer vertex data prior rendering. And after at least one vertex
     * data buffering.
     *
     * @return if vertex data was successfully buffered
     */
    @Override
    public boolean subBufferVertices() {
        if (bigVbo == 0) {
            DSLogger.reportError("Vertex array object or vertex buffer object is zero!", null);
            throw new RuntimeException("Vertex array object or vertex buffer object is zero!");
        }

        final int VERTEX_FLOATS = 9; // pos3 + normal3 + uv2 + layer1
        int someSize = blockList.size() * verticesNum * VERTEX_FLOATS;

        if (bigFloatBuff == null || bigFloatBuff.capacity() == 0) {
            bigFloatBuff = MemoryUtil.memCallocFloat(someSize);
        } else if (bigFloatBuff.capacity() < someSize) {
            bigFloatBuff = MemoryUtil.memRealloc(bigFloatBuff, someSize);
        }

        bigFloatBuff.position(0);
        bigFloatBuff.limit(someSize);

        if (bigFloatBuff.capacity() != 0 && MemoryUtil.memAddressSafe(bigFloatBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        float layer = (float) layerIndex;

        for (Block block : blockList) {
            for (Vertex vertex : block.getVertices()) {
                if (vertex.isEnabled()) {
                    bigFloatBuff.put(vertex.getPos().x)
                            .put(vertex.getPos().y)
                            .put(vertex.getPos().z)
                            .put(vertex.getNormal().x)
                            .put(vertex.getNormal().y)
                            .put(vertex.getNormal().z)
                            .put(vertex.getUv().x)
                            .put(vertex.getUv().y)
                            .put(layer); // UV.z = texture array layer index
                }
            }
        }

        if (bigFloatBuff.position() != 0) {
            bigFloatBuff.flip();
        }

        final int STRIDE = Vertex.EXTENDED_SIZE * 4; // 36 bytes

        if (bigFloatBuff.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, bigFloatBuff);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, STRIDE, 0);  // pos    (vec3)
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, STRIDE, 12); // normal (vec3)
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, STRIDE, 24); // uv     (vec3: u,v,layer)

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

    /**
     * Buffer VEC4 colors - instanced rendering
     *
     * @return buffered success
     */
    protected boolean bufferColors() {
        int someSize = blockList.size() * VEC4_SIZE;
        if (vec4FloatColorBuff == null || vec4FloatColorBuff.capacity() == 0) {
            vec4FloatColorBuff = MemoryUtil.memAllocFloat(someSize);
        } else if (vec4FloatColorBuff.capacity() < someSize) {
            MemoryUtil.memFree(vec4FloatColorBuff);
            vec4FloatColorBuff = MemoryUtil.memAllocFloat(someSize);
        }
        vec4FloatColorBuff.position(0);
        vec4FloatColorBuff.limit(someSize);

        if (vec4FloatColorBuff.capacity() != 0 && MemoryUtil.memAddressSafe(vec4FloatColorBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            return false;
        }

        for (Block block : blockList) {
            Vector4f col = block.getPrimaryRGBAColor();
            vec4FloatColorBuff.put(col.x).put(col.y).put(col.z).put(col.w);
        }

        if (vec4FloatColorBuff.position() != 0) {
            vec4FloatColorBuff.flip();
        }

        if (vec4Vbo == 0) {
            vec4Vbo = GL15.glGenBuffers();
        }

        if (vec4FloatColorBuff.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vec4Vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vec4FloatColorBuff, GL15.GL_DYNAMIC_DRAW);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

    /**
     * Buffer Model MAT4 [col0, col1, col2, col3]
     *
     * @return buffered success
     */
    protected boolean bufferModelMatrices() {
        int someSize = blockList.size() * MAT4_SIZE;
        if (mat4FloatModelBuff == null || mat4FloatModelBuff.capacity() == 0) {
            mat4FloatModelBuff = MemoryUtil.memAllocFloat(someSize);
        } else if (mat4FloatModelBuff.capacity() < someSize) {
            MemoryUtil.memFree(mat4FloatModelBuff);
            mat4FloatModelBuff = MemoryUtil.memAllocFloat(someSize);
        }
        mat4FloatModelBuff.position(0);
        mat4FloatModelBuff.limit(someSize);

        for (Block block : blockList) {
            Matrix4f mat = block.calcModelMatrix();
            mat4FloatModelBuff
                    .put(mat.m00()).put(mat.m01()).put(mat.m02()).put(mat.m03())
                    .put(mat.m10()).put(mat.m11()).put(mat.m12()).put(mat.m13())
                    .put(mat.m20()).put(mat.m21()).put(mat.m22()).put(mat.m23())
                    .put(mat.m30()).put(mat.m31()).put(mat.m32()).put(mat.m33());
        }

        if (mat4FloatModelBuff.position() != 0) {
            mat4FloatModelBuff.flip();
        }

        if (mat4Vbo == 0) {
            mat4Vbo = GL15.glGenBuffers();
        }

        if (mat4FloatModelBuff.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mat4Vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mat4FloatModelBuff, GL15.GL_DYNAMIC_DRAW);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

    @Override
    public void bufferAll() {
        buffered = bufferVertices() && bufferColors() && bufferModelMatrices() && bufferIndices();
    }

    @Override
    public void animate() {
        if (!buffered || blockList.isEmpty()) {
            return;
        }

        IList<Block> filterBlks = blockList.filter(blk -> !blk.isSolid() && blk.getFaceBits() != 0);
        for (Block block : filterBlks) {
            if (!block.isSolid()) {
                block.getMeshes().getFirst().triangSwap();
            }
        }
        subBufferVertices();
    }

    @Override
    public void prepare(boolean cameraInFluid) {
        if (!buffered || blockList.isEmpty() || isSolid()) {
            return;
        }

        for (Block block : blockList.filter(blk -> !blk.isSolid() && blk.getFaceBits() != 0 && cameraInFluid ^ blk.isVerticesReversed())) {
            block.reverseFaceVertexOrder();
        }

        subBufferVertices();
    }

    /**
     * Heavy operation to make underwater ambient if camera in fluid.
     *
     * @param camFront      camera front vec3
     * @param cameraInFluid boolean condition if camera is in fluid
     */
    public void prepare(Vector3f camFront, boolean cameraInFluid) {
        if (!buffered || blockList.isEmpty() || isSolid()) {
            return;
        }

        boolean verticesReversedGlobl = !blockList.filter(blk -> !blk.isSolid() && blk.getFaceBits() != 0 && blk.isVerticesReversed()).isEmpty();
        final float degrees = (cameraInFluid || verticesReversedGlobl) ? 0f : 45f;
        IList<Integer> faces = getRayTraceMultiFaceFast(camFront, degrees);

        if (!faces.isEmpty()) {
            IList<Block> filterBlks = blockList.filter(blk -> !blk.isSolid() && blk.getFaceBits() != 0 && cameraInFluid ^ blk.isVerticesReversed());
            for (Block blk : filterBlks) {
                blk.reverseFaceVertexOrder();
            }

            if (!filterBlks.isEmpty()) {
                subBufferVertices();
            }
        }
    }

    /**
     * Render this tuple using instanced rendering.
     * No per-tuple texture bind — texture array is bound externally.
     * UV location 2 is now vec3 (u, v, layerIndex).
     *
     * @param shaderProgram shader program
     * @param lightSources  light sources
     * @param waterTexture  water reflection texture
     * @param shadowTexture shadow map texture
     */
    public void renderInstanced(ShaderProgram shaderProgram, LightSources lightSources,
                                Texture waterTexture, Texture shadowTexture) {
        if (buffered && !blockList.isEmpty() && faceEnBits > 0) {
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);
            GL20.glEnableVertexAttribArray(3);
            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            GL20.glEnableVertexAttribArray(6);
            GL20.glEnableVertexAttribArray(7);

            shaderProgram.bind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.EXTENDED_SIZE * 4, 0);  // pos  (vec3)
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.EXTENDED_SIZE * 4, 12); // normal (vec3)
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, Vertex.EXTENDED_SIZE * 4, 24); // uv (vec3: u,v,layer)

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vec4Vbo);
            GL20.glVertexAttribPointer(3, VEC4_SIZE, GL11.GL_FLOAT, false, VEC4_SIZE * 4, 0);
            GL33.glVertexAttribDivisor(3, 1);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mat4Vbo);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 0);
            GL20.glVertexAttribPointer(5, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 16);
            GL20.glVertexAttribPointer(6, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 32);
            GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 48);

            GL33.glVertexAttribDivisor(4, 1);
            GL33.glVertexAttribDivisor(5, 1);
            GL33.glVertexAttribDivisor(6, 1);
            GL33.glVertexAttribDivisor(7, 1);

            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "normal");
            shaderProgram.bindAttribute(2, "uv");      // now vec3 in shader
            shaderProgram.bindAttribute(3, "color");
            shaderProgram.bindAttribute(4, "column0");
            shaderProgram.bindAttribute(5, "column1");
            shaderProgram.bindAttribute(6, "column2");
            shaderProgram.bindAttribute(7, "column3");

            lightSources.updateLightsInShaderIfModified(shaderProgram);

            // NOTE: modelTexture0 (sampler2DArray) is bound ONCE externally
            // before iterating tuples — no per-tuple texture bind needed.

            if (waterTexture != null && waterTexture != Texture.EMPTY) {
                shaderProgram.updateUniform(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), "modelColor1");
                waterTexture.bind(1, shaderProgram, "modelTexture1");
            }

            if (shadowTexture != null && shadowTexture != Texture.EMPTY) {
                shaderProgram.updateUniform(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), "modelColor2");
                shadowTexture.bind(2, shaderProgram, "modelTexture2");
            }

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GL32.glDrawElementsInstancedBaseVertex(
                    GL11.GL_TRIANGLES,
                    indicesNum,
                    GL11.GL_UNSIGNED_INT,
                    0,
                    blockList.size(),
                    0
            );Texture.unbind(0);
            Texture.unbind(1);
            Texture.unbind(2);

            ShaderProgram.unbind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
            GL20.glDisableVertexAttribArray(3);
            GL20.glDisableVertexAttribArray(4);
            GL20.glDisableVertexAttribArray(5);
            GL20.glDisableVertexAttribArray(6);
            GL20.glDisableVertexAttribArray(7);
        }
    }

    /**
     * Render all tuples in a single pass. Texture array (sampler2DArray) bound
     * once outside the per-tuple loop. UV.z carries the layer index per vertex.
     *
     * @param tuples        list of tuples to render
     * @param shaderProgram shader program
     * @param lightSources  light sources
     * @param blocksTexture texture array (GL_TEXTURE_2D_ARRAY)
     * @param waterTexture  water reflection texture
     * @param shadowTexture shadow map texture
     */
    public static void renderInstanced(IList<Tuple> tuples, ShaderProgram shaderProgram,
                                       LightSources lightSources, TextureArray blocksTexture,
                                       Texture waterTexture, Texture shadowTexture) {
        /**
         * Early exit if no tuples to render. Avoids unnecessary state changes and shader binds.
         */
        if (tuples.isEmpty()) {
            return;
        }

        /**
         * Buffer all tuples if not already buffered. This is a heavy operation and should ideally be done once per chunk update.
         * Tuples are expected to be reused across multiple frames without modification, so buffering is done once and retained.
         */
        if (!blocksTexture.isBuffered()) {
            blocksTexture.bufferAll();
        }

        /**
         * Buffer all tuples if not already buffered. This is a heavy operation and should ideally be done once per chunk update.
         * Tuples are expected to be reused across multiple frames without modification, so buffering is done once and retained.
         */
        for (Tuple tuple : tuples) {
            if (!tuple.isBuffered()) {
                tuple.bufferAll();
            }
        }

        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);
        GL20.glEnableVertexAttribArray(3);
        GL20.glEnableVertexAttribArray(4);
        GL20.glEnableVertexAttribArray(5);
        GL20.glEnableVertexAttribArray(6);
        GL20.glEnableVertexAttribArray(7);

        shaderProgram.bind();

        shaderProgram.bindAttribute(0, "pos");
        shaderProgram.bindAttribute(1, "normal");
        shaderProgram.bindAttribute(2, "uv");      // vec3 in shader
        shaderProgram.bindAttribute(3, "color");
        shaderProgram.bindAttribute(4, "column0");
        shaderProgram.bindAttribute(5, "column1");
        shaderProgram.bindAttribute(6, "column2");
        shaderProgram.bindAttribute(7, "column3");

        lightSources.updateLightsInShaderIfModified(shaderProgram);

        // Bind texture array ONCE for all tuples — layer selected per vertex via UV.z
        if (blocksTexture != null) {
            blocksTexture.bind(0, shaderProgram, "modelTexture0");
        }

        if (waterTexture != null && waterTexture != Texture.EMPTY) {
            shaderProgram.updateUniform(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), "modelColor1");
            waterTexture.bind(1, shaderProgram, "modelTexture1");
        }

        if (shadowTexture != null && shadowTexture != Texture.EMPTY) {
            shaderProgram.updateUniform(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), "modelColor2");
            shadowTexture.bind(2, shaderProgram, "modelTexture2");
        }

        for (Tuple tuple : tuples) {
            if (tuple.blockList.isEmpty() || tuple.faceBits() <= 0) {
                continue;
            }

            // Per-tuple: only bind geometry buffers (no texture switch!)
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tuple.bigVbo);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.EXTENDED_SIZE * 4, 0);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.EXTENDED_SIZE * 4, 12);
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, Vertex.EXTENDED_SIZE * 4, 24); // vec3 UV

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tuple.vec4Vbo);
            GL20.glVertexAttribPointer(3, VEC4_SIZE, GL11.GL_FLOAT, false, VEC4_SIZE * 4, 0);
            GL33.glVertexAttribDivisor(3, 1);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tuple.mat4Vbo);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 0);
            GL20.glVertexAttribPointer(5, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 16);
            GL20.glVertexAttribPointer(6, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 32);
            GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 48);

            GL33.glVertexAttribDivisor(4, 1);
            GL33.glVertexAttribDivisor(5, 1);
            GL33.glVertexAttribDivisor(6, 1);
            GL33.glVertexAttribDivisor(7, 1);

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, tuple.ibo);
            GL32.glDrawElementsInstancedBaseVertex(
                    GL11.GL_TRIANGLES,
                    tuple.indicesNum,
                    GL11.GL_UNSIGNED_INT,
                    0,
                    tuple.blockList.size(),
                    0
            );
        }

        Texture.unbind(0);
        Texture.unbind(1);
        Texture.unbind(2);

        ShaderProgram.unbind();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
        GL20.glDisableVertexAttribArray(3);
        GL20.glDisableVertexAttribArray(4);
        GL20.glDisableVertexAttribArray(5);
        GL20.glDisableVertexAttribArray(6);
        GL20.glDisableVertexAttribArray(7);

        TextureArray.unbind();
    }

    @Override
    public void release() {
        super.release();

        if (mat4Vbo != 0) {
            GL15.glDeleteBuffers(mat4Vbo);
        }
        if (vec4Vbo != 0) {
            GL15.glDeleteBuffers(vec4Vbo);
        }

        if (vec4FloatColorBuff != null && vec4FloatColorBuff.capacity() != 0) {
            MemoryUtil.memFree(vec4FloatColorBuff);
            vec4FloatColorBuff = null;
        }

        if (mat4FloatModelBuff != null && mat4FloatModelBuff.capacity() != 0) {
            MemoryUtil.memFree(mat4FloatModelBuff);
            mat4FloatModelBuff = null;
        }

        buffered = false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + this.indicesNum;
        hash = 97 * hash + this.verticesNum;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Tuple other = (Tuple) obj;
        if (this.indicesNum != other.indicesNum) return false;
        if (this.verticesNum != other.verticesNum) return false;
        return Objects.equals(this.name, other.name);
    }

    /**
     * Returns the texture name associated with this tuple.
     */
    public String texName() {
       return texName;
    }

    /**
     * Returns the face enabled bits for this tuple.
     */
    public int faceBits() {
        return faceEnBits;
    }

    /**
     * Returns true if this tuple is solid (not water/fluid).
     * Uses layerIndex comparison instead of string comparison.
     */
    public boolean isSolid() {
        return layerIndex != WATER_LAYER_INDEX;
    }

    public int getLayerIndex() {
        return layerIndex;
    }

    public int getVec4Vbo() {
        return vec4Vbo;
    }

    public int getMat4Vbo() {
        return mat4Vbo;
    }

    public String getName() {
        return name;
    }

    public int getIbo() {
        return ibo;
    }

    public FloatBuffer getVec4FloatColorBuff() {
        return vec4FloatColorBuff;
    }

    public FloatBuffer getMat4FloatModelBuff() {
        return mat4FloatModelBuff;
    }

    public IntBuffer getIntBuff() {
        return intBuff;
    }

    @Override
    public String toString() {
        return "Tuple{" + "name=" + name + ", layer=" + layerIndex + '}';
    }

    public int getVerticesNum() {
        return verticesNum;
    }

    public int getFacesNum() {
        return facesNum;
    }
}
