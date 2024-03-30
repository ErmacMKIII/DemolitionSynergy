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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.ModelUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Tuple extends Series {
    // tuple is distinct rendering object for instanced rendering
    // all blocks in the tuple have the same properties, 
    // like model matrices, color and texture name, and enabled faces in 6-bit represenation
    // iboMap is not used here

    public static final int VEC2_SIZE = 2;
    public static final int VEC3_SIZE = 3;
    public static final int VEC4_SIZE = 4;
    public static final int MAT4_SIZE = 16;

    protected int vec4Vbo = 0; // color
    protected static FloatBuffer vec4FloatColorBuff = null;

    protected int lightVbo = 0; // light color
    protected static FloatBuffer lightFloatColorBuff = null;

    protected int mat4Vbo = 0; // model matrix [col0, col1, col2, col3]
    protected static FloatBuffer mat4FloatModelBuff = null;

    protected final String name;

    protected boolean needUpdateLights = false;

    public static final Comparator<Tuple> TUPLE_COMP = new Comparator<Tuple>() {
        @Override
        public int compare(Tuple o1, Tuple o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    /**
     * Construct new tuple by definition texName x face-enabled-bits
     *
     * @param texName texture name
     * @param faceEnBits face enabled bits
     */
    public Tuple(String texName, int faceEnBits) {
        this.name = String.format("%s%02d", texName, faceEnBits);

        int numberOfOnes = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            if ((faceEnBits & mask) != 0) {
                numberOfOnes++;
            }
        }
        this.verticesNum = 4 * numberOfOnes; // affects buffering of vertices
        this.indicesNum = 6 * numberOfOnes; // affect buffering of indices
    }

    /**
     * Gets Block from the tuple block list (duplicates may exist but in very
     * low quantity). Complexity is O(log(n)+k).
     *
     * @param pos Vector3f position of the block
     * @return block if found (null if not found)
     */
    public Block getBlock(Vector3f pos) {
        Integer key = ModelUtils.blockSpecsToUniqueInt(isSolid(), this.texName(), this.faceBits(), pos);

        int left = 0;
        int right = this.blockList.size() - 1;
        int startIndex = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            Block candidate = this.blockList.get(mid);
            Integer candInt = candidate.getId();
            int res = candInt.compareTo(key);
            if (res < 0) {
                left = mid + 1;
            } else if (res == 0) {
                startIndex = mid;
                right = mid - 1;
            } else {
                right = mid - 1;
            }
        }

        left = 0;
        right = this.blockList.size() - 1;
        int endIndex = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            Block candidate = this.blockList.get(mid);
            Integer candInt = candidate.getId();
            int res = candInt.compareTo(key);
            if (res < 0) {
                left = mid + 1;
            } else if (res == 0) {
                endIndex = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        if (startIndex != -1 && endIndex != -1) {
            for (int i = startIndex; i <= endIndex; i++) {
                Block blk = this.blockList.get(i);
                if (blk.pos.equals(pos)) {
                    return blk;
                }
            }
        }

        return null;
    }

    /**
     * Buffer VEC3 colors - instanced rendering
     *
     * @return buffered success
     */
    protected boolean bufferColors() { // buffering colors
        if (vao == 0) {
            DSLogger.reportError("Vertex array object is zero!", null);
            throw new RuntimeException("Vertex array object is zero!");
        }

        int someSize = blockList.size() * VEC4_SIZE;
        if (vec4FloatColorBuff == null) {
            vec4FloatColorBuff = MemoryUtil.memCallocFloat(someSize);
        } else if (vec4FloatColorBuff.capacity() < someSize) {
            vec4FloatColorBuff = MemoryUtil.memRealloc(vec4FloatColorBuff, someSize);
        }
        vec4FloatColorBuff.position(0);
        vec4FloatColorBuff.limit(someSize);

        if (vec4FloatColorBuff.capacity() != 0 && MemoryUtil.memAddressSafe(vec4FloatColorBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        for (Block block : blockList) {
            Vector4f color = block.getPrimaryRGBAColor();
            vec4FloatColorBuff.put(color.x);
            vec4FloatColorBuff.put(color.y);
            vec4FloatColorBuff.put(color.z);
            vec4FloatColorBuff.put(color.w);
        }
        if (vec4FloatColorBuff.position() != 0) {
            vec4FloatColorBuff.flip();
        }

        if (vec4Vbo == 0) {
            vec4Vbo = GL15.glGenBuffers();
        }

        if (vec4FloatColorBuff.capacity() != 0) {
            GL30.glBindVertexArray(vao); //**
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vec4Vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vec4FloatColorBuff, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(3, VEC4_SIZE, GL11.GL_FLOAT, false, VEC4_SIZE * 4, 0); // this is for color
            GL33.glVertexAttribDivisor(3, 1);
            GL20.glDisableVertexAttribArray(3);

            GL20.glDisableVertexAttribArray(3);
            GL30.glBindVertexArray(0); //**
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

//    /**
//     * Buffer VEC3 light colors - instanced rendering
//     *
//     * @return buffered success
//     */
//    protected boolean bufferLightColors() { // buffering colors
//        if (vao == 0) {
//            DSLogger.reportError("Vertex array object is zero!", null);
//            throw new RuntimeException("Vertex array object is zero!");
//        }
//
//        int someSize = blockList.size() * VEC4_SIZE;
//        if (lightFloatColorBuff == null) {
//            lightFloatColorBuff = MemoryUtil.memCallocFloat(someSize);
//        } else if (lightFloatColorBuff.capacity() < someSize) {
//            lightFloatColorBuff = MemoryUtil.memRealloc(lightFloatColorBuff, someSize);
//        }
//        lightFloatColorBuff.position(0);
//        lightFloatColorBuff.limit(someSize);
//
//        if (lightFloatColorBuff.capacity() != 0 && MemoryUtil.memAddressSafe(lightFloatColorBuff) == MemoryUtil.NULL) {
//            DSLogger.reportError("Could not allocate memory address!", null);
//            throw new RuntimeException("Could not allocate memory address!");
//        }
//
//        for (Block block : blockList) {
//            Vector4f lightCol = block.getMapLightColor();
//
//            lightFloatColorBuff.put(lightCol.x);
//            lightFloatColorBuff.put(lightCol.y);
//            lightFloatColorBuff.put(lightCol.z);
//            lightFloatColorBuff.put(lightCol.w);
//        }
//        if (lightFloatColorBuff.position() != 0) {
//            lightFloatColorBuff.flip();
//        }
//
//        if (lightVbo == 0) {
//            lightVbo = GL15.glGenBuffers();
//        }
//
//        if (lightFloatColorBuff.capacity() != 0) {
//            GL30.glBindVertexArray(vao); //**
//            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lightVbo);
//            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, lightFloatColorBuff, GL15.GL_STATIC_DRAW);
//
//            GL20.glEnableVertexAttribArray(4);
//            GL20.glVertexAttribPointer(4, VEC4_SIZE, GL11.GL_FLOAT, false, VEC4_SIZE * 4, 0); // this is for light color
//            GL33.glVertexAttribDivisor(4, 1);
//            GL20.glDisableVertexAttribArray(4);
//
//            GL30.glBindVertexArray(0); //**
//        }
//        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//
//        return true;
//    }
//
//    /**
//     * Buffer VEC3 light colors - instanced rendering
//     *
//     * @return buffered success
//     */
//    protected boolean updateLightColors() {
//        if (vao == 0 || lightVbo == 0) {
//            DSLogger.reportError("Vertex array object or light color buffer object is zero!", null);
//            throw new RuntimeException("Vertex array object or light color buffer object is zero!");
//        }
//
//        int someSize = blockList.size() * VEC4_SIZE;
//        if (lightFloatColorBuff == null) {
//            lightFloatColorBuff = MemoryUtil.memCallocFloat(someSize);
//        } else if (lightFloatColorBuff.capacity() < someSize) {
//            lightFloatColorBuff = MemoryUtil.memRealloc(lightFloatColorBuff, someSize);
//        }
//        lightFloatColorBuff.position(0);
//        lightFloatColorBuff.limit(someSize);
//
//        if (lightFloatColorBuff == null || MemoryUtil.memAddressSafe(lightFloatColorBuff) == MemoryUtil.NULL) {
//            DSLogger.reportError("Could not allocate memory address!", null);
//            throw new RuntimeException("Could not allocate memory address!");
//        }
//
//        for (Block block : blockList) {
//            Vector4f lightCol = block.getMapLightColor();
//            lightFloatColorBuff.put(lightCol.x).put(lightCol.y).put(lightCol.z).put(lightCol.w);
//        }
//
//        lightFloatColorBuff.flip();
//
//        GL30.glBindVertexArray(vao);
//        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lightVbo);
//        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, lightFloatColorBuff);
//        GL20.glEnableVertexAttribArray(4);
//        GL20.glVertexAttribPointer(4, VEC4_SIZE, GL11.GL_FLOAT, false, VEC4_SIZE * 4, 0);
//        GL33.glVertexAttribDivisor(4, 1);
//        GL20.glDisableVertexAttribArray(4);
//        GL30.glBindVertexArray(0);
//
//        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//
//        return true;
//    }
    /**
     * Buffer Model MAT4[col0, col1, col2, col3]
     *
     * @return buffered success
     */
    protected boolean bufferModelMatrices() { // buffering model matrices
        if (vao == 0) {
            DSLogger.reportError("Vertex array object is zero!", null);
            throw new RuntimeException("Vertex array object is zero!");
        }

        int someSize = blockList.size() * MAT4_SIZE;
        if (mat4FloatModelBuff == null) {
            mat4FloatModelBuff = MemoryUtil.memCallocFloat(someSize);
        } else if (mat4FloatModelBuff.capacity() < someSize) {
            mat4FloatModelBuff = MemoryUtil.memRealloc(mat4FloatModelBuff, someSize);
        }
        mat4FloatModelBuff.position(0);
        mat4FloatModelBuff.limit(someSize);

        for (Block block : blockList) {
            Vector4f[] vectArr = new Vector4f[4];
            for (int i = 0; i < 4; i++) {
                vectArr[i] = new Vector4f();
                Matrix4f modelMatrix = block.calcModelMatrix();
                modelMatrix.getColumn(i, vectArr[i]);
                mat4FloatModelBuff.put(vectArr[i].x);
                mat4FloatModelBuff.put(vectArr[i].y);
                mat4FloatModelBuff.put(vectArr[i].z);
                mat4FloatModelBuff.put(vectArr[i].w);
            }
        }

        if (mat4FloatModelBuff.position() != 0) {
            mat4FloatModelBuff.flip();
        }

        if (mat4Vbo == 0) {
            mat4Vbo = GL15.glGenBuffers();
        }

        if (mat4FloatModelBuff.capacity() != 0) {
            GL30.glBindVertexArray(vao); //**
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mat4Vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mat4FloatModelBuff, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            GL20.glEnableVertexAttribArray(6);
            GL20.glEnableVertexAttribArray(7);

            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 0); // this is for column0
            GL20.glVertexAttribPointer(5, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 16); // this is for column1
            GL20.glVertexAttribPointer(6, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 32); // this is for column2
            GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 48); // this is for column3                                     

            GL33.glVertexAttribDivisor(4, 1);
            GL33.glVertexAttribDivisor(5, 1);
            GL33.glVertexAttribDivisor(6, 1);
            GL33.glVertexAttribDivisor(7, 1);

            GL20.glDisableVertexAttribArray(4);
            GL20.glDisableVertexAttribArray(5);
            GL20.glDisableVertexAttribArray(6);
            GL20.glDisableVertexAttribArray(7);
            GL30.glBindVertexArray(0); //**
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

    // renderer does this stuff prior to any rendering
    @Override
    public void bufferAll() {
        buffered = bufferVertices() && bufferColors() && bufferModelMatrices() & bufferIndices();
        needUpdateLights = false;
    }

    @Override
    public void animate() { // call only for fluid blocks
        if (!buffered || blockList.isEmpty() || isSolid()) {
            return;
        }

        for (Block block : blockList) {
            if (!block.isSolid()) {
                block.getMeshes().getFirst().triangSwap();
            }
        }

        subBufferVertices();
    }

    @Override
    public void prepare(boolean cameraInFluid) { // call only for fluid blocks before rendering                      
        if (!buffered || blockList.isEmpty() || isSolid()) {
            return;
        }

        for (Block block : blockList) {
            if (!block.isSolid() && cameraInFluid ^ block.isVerticesReversed()) {
                block.reverseFaceVertexOrder();
            }
        }

        subBufferVertices();
    }

    public void renderInstanced(ShaderProgram shaderProgram, LightSources lightSources, Texture waterTexture, Texture shadowTexture) {
        // if tuple has any blocks to be rendered and
        // if face bits are greater than zero, i.e. tuple has something to be rendered
        String texName = name.substring(0, 5);
        int faceEnBits = Integer.parseInt(name.substring(5));
        if (buffered && !blockList.isEmpty() && faceEnBits > 0) {
            shaderProgram.bind();
            GL30.glBindVertexArray(vao);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);
            GL20.glEnableVertexAttribArray(3);
            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            GL20.glEnableVertexAttribArray(6);
            GL20.glEnableVertexAttribArray(7);

            /**
             * layout (location = 0) in vec3 pos; layout (location = 1) in vec3
             * normal; layout (location = 2) in vec2 uv; layout (location = 3)
             * in vec3 color; layout (location = 4) in vec4 column0; layout
             * (location = 5) in vec4 column1; layout (location = 6) in vec4
             * column2; layout (location = 7) in vec4 column3;
             */
            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "normal");
            shaderProgram.bindAttribute(2, "uv");

            shaderProgram.bindAttribute(3, "color");

            shaderProgram.bindAttribute(4, "column0");
            shaderProgram.bindAttribute(5, "column1");
            shaderProgram.bindAttribute(6, "column2");
            shaderProgram.bindAttribute(7, "column3");

            // -- Lights            
            lightSources.updateLightsInShaderIfModified(shaderProgram);
            // --

            Texture blocksTexture = Texture.getOrDefault(texName);
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
            // zero matrix if shadows are disabled
            // PASS 2
//            shaderProgram.updateUniform(lightSources.lightSpaceMatrix, "lightSpaceMatrix");

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GL32.glDrawElementsInstancedBaseVertex(
                    GL11.GL_TRIANGLES,
                    indicesNum,
                    GL11.GL_UNSIGNED_INT,
                    0,
                    blockList.size(),
                    0
            );

            Texture.unbind(0);
            Texture.unbind(1);
            Texture.unbind(2);

            ShaderProgram.unbind();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
            GL20.glDisableVertexAttribArray(3);
            GL20.glDisableVertexAttribArray(4);
            GL20.glDisableVertexAttribArray(5);
            GL20.glDisableVertexAttribArray(6);
            GL20.glDisableVertexAttribArray(7);

            GL30.glBindVertexArray(0);
        }
    }

    public static void renderInstanced(IList<Tuple> tuples, ShaderProgram shaderProgram, LightSources lightSources, Texture waterTexture, Texture shadowTexture) {
        if (!tuples.isEmpty()) {
            shaderProgram.bind();

            /**
             * layout (location = 0) in vec3 pos; layout (location = 1) in vec3
             * normal; layout (location = 2) in vec2 uv; layout (location = 3)
             * in vec3 color; layout (location = 4) in vec4 column0; layout
             * (location = 5) in vec4 column1; layout (location = 6) in vec4
             * column2; layout (location = 7) in vec4 column3;
             */
            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "normal");
            shaderProgram.bindAttribute(2, "uv");

            shaderProgram.bindAttribute(3, "color");

            shaderProgram.bindAttribute(4, "column0");
            shaderProgram.bindAttribute(5, "column1");
            shaderProgram.bindAttribute(6, "column2");
            shaderProgram.bindAttribute(7, "column3");

            // -- Lights            
            lightSources.updateLightsInShaderIfModified(shaderProgram);
            // --
            Texture blocksTexture = Texture.WORLD;
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
            // zero matrix if shadows are disabled
            // PASS 2
//            shaderProgram.updateUniform(lightSources.lightSpaceMatrix, "lightSpaceMatrix");
//            DSLogger.reportInfo(lightSources.lightSpaceMatrix.toString(), null);
            for (Tuple tuple : tuples) {
                if (!tuple.isBuffered()) {
                    tuple.bufferAll();
                }

                if (!tuple.blockList.isEmpty() && tuple.faceBits() > 0) {
                    GL30.glBindVertexArray(tuple.vao);

                    GL20.glEnableVertexAttribArray(0);
                    GL20.glEnableVertexAttribArray(1);
                    GL20.glEnableVertexAttribArray(2);
                    GL20.glEnableVertexAttribArray(3);
                    GL20.glEnableVertexAttribArray(4);
                    GL20.glEnableVertexAttribArray(5);
                    GL20.glEnableVertexAttribArray(6);
                    GL20.glEnableVertexAttribArray(7);

                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, tuple.ibo);
                    GL32.glDrawElementsInstancedBaseVertex(
                            GL11.GL_TRIANGLES,
                            tuple.indicesNum,
                            GL11.GL_UNSIGNED_INT,
                            0,
                            tuple.blockList.size(),
                            0
                    );

                    GL20.glDisableVertexAttribArray(0);
                    GL20.glDisableVertexAttribArray(1);
                    GL20.glDisableVertexAttribArray(2);
                    GL20.glDisableVertexAttribArray(3);
                    GL20.glDisableVertexAttribArray(4);
                    GL20.glDisableVertexAttribArray(5);
                    GL20.glDisableVertexAttribArray(6);
                    GL20.glDisableVertexAttribArray(7);

                    GL30.glBindVertexArray(0);

                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
                }
            }
            Texture.unbind(0);
            Texture.unbind(1);
            Texture.unbind(2);

            ShaderProgram.unbind();
        }
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
        if (lightVbo != 0) {
            GL15.glDeleteBuffers(lightVbo);
        }

        if (vec4FloatColorBuff != null && vec4FloatColorBuff.capacity() != 0) {
            MemoryUtil.memFree(vec4FloatColorBuff);
            vec4FloatColorBuff = null;
        }

        if (lightFloatColorBuff != null && lightFloatColorBuff.capacity() != 0) {
            MemoryUtil.memFree(lightFloatColorBuff);
            lightFloatColorBuff = null;
        }

        if (mat4FloatModelBuff != null && mat4FloatModelBuff.capacity() != 0) {
            MemoryUtil.memFree(mat4FloatModelBuff);
            mat4FloatModelBuff = null;
        }

        buffered = false;
    }

//    public void updateLightDefs() {
//        if (buffered && needUpdateLights) {
//            updateLightColors();
//            needUpdateLights = false; // prevent calling it unless really necessary
//        }
//    }
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tuple other = (Tuple) obj;
        if (this.indicesNum != other.indicesNum) {
            return false;
        }
        if (this.verticesNum != other.verticesNum) {
            return false;
        }
        return Objects.equals(this.name, other.name);
    }

    public String texName() {
        return name.substring(0, 5);
    }

    public int faceBits() {
        return Integer.parseInt(name.substring(5));
    }

    public boolean isSolid() {
        return !texName().equals("water");
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
        return "Tuple{" + "name=" + name + '}';
    }

    public int getLightVbo() {
        return lightVbo;
    }

    public static FloatBuffer getLightFloatColorBuff() {
        return lightFloatColorBuff;
    }

    public int getVerticesNum() {
        return verticesNum;
    }

    public boolean isNeedUpdateLights() {
        return needUpdateLights;
    }

    public void setNeedUpdateLights(boolean needUpdateLights) {
        this.needUpdateLights = needUpdateLights;
    }

}
