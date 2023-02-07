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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import rs.alexanderstojanovich.evg.level.LightSources;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Tuple extends Blocks {
    // tuple is distinct rendering object for instanced rendering
    // all blocks in the tuple have the same properties, 
    // like model matrices, color and texture name, and enabled faces in 6-bit represenation

    public static final int VEC3_SIZE = 3;
    public static final int MAT4_SIZE = 16;

    protected int vec3Vbo = 0;
    protected FloatBuffer vec3FloatBuff;

    protected int mat4Vbo = 0;
    protected FloatBuffer mat4FloatBuff;

    protected final String name;

    protected final IntBuffer intBuff;
    protected int ibo = 0;
    protected final int indicesNum;
    protected final int verticesNum;

    public static final Comparator<Tuple> TUPLE_COMP = new Comparator<Tuple>() {
        @Override
        public int compare(Tuple o1, Tuple o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    public Tuple(String texName, int faceEnBits) {
        this.name = String.format("%s%02d", texName, faceEnBits);
        this.intBuff = Block.createIntBuffer(faceEnBits);

        int numberOfOnes = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            if ((faceEnBits & mask) != 0) {
                numberOfOnes++;
            }
        }
        this.verticesNum = 4 * numberOfOnes;
        this.indicesNum = 6 * numberOfOnes;
    }

    /**
     * Gets Block from the tuple block list (duplicates may exist but in very
     * low quantity). Complexity is O(log(n)+k).
     *
     * @param pos Vector3f position of the block
     * @return block if found (null if not found)
     */
    public Block getBlock(Vector3f pos) {
        Integer key = Vector3fUtils.float3ToUniqueInteger(pos, this.texName());

        int left = 0;
        int right = this.blockList.size() - 1;
        int startIndex = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            Block candidate = this.blockList.get(mid);
            Integer candInt = Vector3fUtils.float3ToUniqueInteger(candidate.pos, candidate.texName);
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
            Integer candInt = Vector3fUtils.float3ToUniqueInteger(candidate.pos, candidate.texName);
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

    @Override
    public void bufferVertices() { // call it before any rendering
        // auto adjust dynamic size of float buff and do it on every 1000th element
        if (blockList.size() > dynamicSize) {
            dynamicSize = blockList.size() + DYNAMIC_INCREMENT;
            bigFloatBuff = BufferUtils.createFloatBuffer(dynamicSize * verticesNum * Vertex.SIZE);
        }

        bigFloatBuff.clear();
        for (Block block : blockList) {
            for (Vertex vertex : block.vertices) { // for each vertex
                if (vertex.isEnabled()) {
                    bigFloatBuff.put(vertex.getPos().x);
                    bigFloatBuff.put(vertex.getPos().y);
                    bigFloatBuff.put(vertex.getPos().z);
                    bigFloatBuff.put(vertex.getNormal().x);
                    bigFloatBuff.put(vertex.getNormal().y);
                    bigFloatBuff.put(vertex.getNormal().z);
                    bigFloatBuff.put(vertex.getUv().x);
                    bigFloatBuff.put(vertex.getUv().y);
                }
            }
        }

        if (bigFloatBuff.position() != 0) {
            bigFloatBuff.flip();
        }

        if (bigVbo == 0) {
            bigVbo = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bigFloatBuff, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void updateVertices() { // call it before any rendering        
        bigFloatBuff.clear();
        int blkIndex = 0;
        for (Block block : blockList) {
            for (Vertex vertex : block.vertices) { // for each vertex
                if (vertex.isEnabled()) {
                    bigFloatBuff.put(vertex.getPos().x);
                    bigFloatBuff.put(vertex.getPos().y);
                    bigFloatBuff.put(vertex.getPos().z);
                    bigFloatBuff.put(vertex.getNormal().x);
                    bigFloatBuff.put(vertex.getNormal().y);
                    bigFloatBuff.put(vertex.getNormal().z);
                    bigFloatBuff.put(vertex.getUv().x);
                    bigFloatBuff.put(vertex.getUv().y);
                }
            }
            blkIndex++;
        }
        bigFloatBuff.flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, bigFloatBuff);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    // buffering colors
    public void bufferVectors() {
        if (vec3FloatBuff == null || vec3FloatBuff.capacity() / VEC3_SIZE <= dynamicSize) {
            vec3FloatBuff = BufferUtils.createFloatBuffer(dynamicSize * VEC3_SIZE);
        }
        vec3FloatBuff.clear();

        for (Block block : blockList) {
            Vector3f color = block.getPrimaryColor();
            vec3FloatBuff.put(color.x);
            vec3FloatBuff.put(color.y);
            vec3FloatBuff.put(color.z);
        }
        vec3FloatBuff.flip();

        if (vec3Vbo == 0) {
            vec3Vbo = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vec3Vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vec3FloatBuff, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    // buffering model matrices
    public void bufferMatrices() {
        if (mat4FloatBuff == null || mat4FloatBuff.capacity() / MAT4_SIZE <= dynamicSize) {
            mat4FloatBuff = BufferUtils.createFloatBuffer(dynamicSize * MAT4_SIZE);
        }
        mat4FloatBuff.clear();

        for (Block block : blockList) {
            Vector4f[] vectArr = new Vector4f[4];
            for (int i = 0; i < 4; i++) {
                vectArr[i] = new Vector4f();
                Matrix4f modelMatrix = block.calcModelMatrix();
                modelMatrix.getColumn(i, vectArr[i]);
                mat4FloatBuff.put(vectArr[i].x);
                mat4FloatBuff.put(vectArr[i].y);
                mat4FloatBuff.put(vectArr[i].z);
                mat4FloatBuff.put(vectArr[i].w);
            }
        }
        mat4FloatBuff.flip();

        if (mat4Vbo == 0) {
            mat4Vbo = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mat4Vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mat4FloatBuff, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void bufferIndices() {
        // storing indices buffer on the graphics card
        if (ibo == 0) {
            ibo = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, intBuff, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    // renderer does this stuff prior to any rendering
    @Override
    public void bufferAll() {
        bufferVertices();
        bufferVectors();
        bufferMatrices();
        bufferIndices();
        buffered = true;
    }

    @Deprecated
    @Override
    public void release() {
//        GL15.glDeleteBuffers(blocks.getBigVbo());
//        GL15.glDeleteBuffers(vec3Vbo);
//        GL15.glDeleteBuffers(mat4Vbo);
    }

    public void renderInstanced(ShaderProgram shaderProgram, boolean solid, LightSources lightSources, Texture waterTexture) {
        // if tuple has any blocks to be rendered and
        // if face bits are greater than zero, i.e. tuple has something to be rendered
        String texName = name.substring(0, 5);
        int faceEnBits = Integer.parseInt(name.substring(5));
        if (buffered && !blockList.isEmpty() && faceEnBits > 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // this is for pos            
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // this is for normal                                        
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // this is for uv 

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vec3Vbo);
            GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, VEC3_SIZE * 4, 0); // this is for color
            GL33.glVertexAttribDivisor(3, 1);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mat4Vbo);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 0); // this is for column0
            GL20.glVertexAttribPointer(5, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 16); // this is for column1
            GL20.glVertexAttribPointer(6, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 32); // this is for column2
            GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 48); // this is for column3                       

            GL33.glVertexAttribDivisor(4, 1);
            GL33.glVertexAttribDivisor(5, 1);
            GL33.glVertexAttribDivisor(6, 1);
            GL33.glVertexAttribDivisor(7, 1);

            shaderProgram.bind();

            lightSources.updateLightsInShader(shaderProgram);

            shaderProgram.updateUniform(solid ? 1.0f : 0.5f, "modelAlpha");

            Texture blocksTexture = Texture.TEX_MAP.get(texName).getKey();
            if (blocksTexture != null) {
                blocksTexture.bind(0, shaderProgram, "modelTexture0");
            }

            if (waterTexture != null && Game.isWaterEffects()) {
                shaderProgram.updateUniform(new Vector3f(1.0f, 1.0f, 1.0f), "modelColor1");
                waterTexture.bind(1, shaderProgram, "modelTexture1");
            }

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

            ShaderProgram.unbind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

    public String texName() {
        return name.substring(0, 5);
    }

    public int faceBits() {
        return Integer.parseInt(name.substring(5));
    }

    public int getVec3Vbo() {
        return vec3Vbo;
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

    public FloatBuffer getVec3FloatBuff() {
        return vec3FloatBuff;
    }

    public FloatBuffer getMat4FloatBuff() {
        return mat4FloatBuff;
    }

    public IntBuffer getIntBuff() {
        return intBuff;
    }

    @Override
    public String toString() {
        return "Tuple{" + "name=" + name + '}';
    }

}
