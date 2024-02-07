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

import rs.alexanderstojanovich.evg.light.LightSources;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Vertex;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Blocks { // mutual class for both solid blocks and fluid blocks with improved rendering

    public static final int DYNAMIC_INCREMENT = Configuration.getInstance().getBlockDynamicSize();

    public final IList<Block> blockList = new BigList<>(DYNAMIC_INCREMENT);
    protected int vao = 0;
    protected int bigVbo = 0;
    // array with offsets in the big float buffer
    // this is maximum amount of blocks of the type game can hold
    // --------------blkIndex---ibo-----------------------------
    protected final Map<Integer, Integer> iboMap = new LinkedHashMap<>();
    protected boolean buffered = false;

//    protected static int dynamicSize = DYNAMIC_INCREMENT;
    protected static FloatBuffer bigFloatBuff = null;

    public boolean bufferVertices() { // call it before any rendering
        bigFloatBuff = MemoryUtil.memCallocFloat(blockList.size() * Block.VERTEX_COUNT * Vertex.SIZE);
        if (bigFloatBuff.capacity() != 0 && MemoryUtil.memAddressSafe(bigFloatBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            return false;
        }
        for (Block block : blockList) {
            for (Vertex vertex : block.getVertices()) { // for each vertex
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
        bigFloatBuff.flip();
        if (bigVbo == 0) {
            bigVbo = GL15.glGenBuffers();
        }

        if (bigFloatBuff.capacity() != 0) {
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bigFloatBuff, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 0); // this is for pos
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 12); // this is for normal                                        
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 24); // this is for uv   

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);

            GL30.glBindVertexArray(0);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (bigFloatBuff.capacity() != 0) {
            MemoryUtil.memFree(bigFloatBuff);
        }

        return true;
    }

    public boolean updateVertices() { // call it before any rendering
        if (vao == 0) {
            DSLogger.reportError("Vertex array object is zero!", null);
            return false;
        }
        if (bigVbo == 0) {
            DSLogger.reportError("Vertex buffer object is zero!", null);
            return false;
        }
        bigFloatBuff = MemoryUtil.memCallocFloat(blockList.size() * Block.VERTEX_COUNT * Vertex.SIZE);
        if (bigFloatBuff.capacity() != 0 && MemoryUtil.memAddressSafe(bigFloatBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            return false;
        }
        int offset = 0;
        int blkIndex = 0;
        for (Block block : blockList) {
            for (Vertex vertex : block.getVertices()) { // for each vertex
                if (vertex.isEnabled()) {
                    bigFloatBuff.put(vertex.getPos().x);
                    bigFloatBuff.put(vertex.getPos().y);
                    bigFloatBuff.put(vertex.getPos().z);
                    bigFloatBuff.put(vertex.getNormal().x);
                    bigFloatBuff.put(vertex.getNormal().y);
                    bigFloatBuff.put(vertex.getNormal().z);
                    bigFloatBuff.put(vertex.getUv().x);
                    bigFloatBuff.put(vertex.getUv().y);
                    offset++;
                }
            }
            blkIndex++;
        }
        if (bigFloatBuff.position() != 0) {
            bigFloatBuff.flip();
        }

        if (bigFloatBuff.capacity() != 0) {
            GL30.glBindVertexArray(vao);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, bigFloatBuff);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 0); // this is for pos
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 12); // this is for normal                                        
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 24); // this is for uv   

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);

            GL30.glBindVertexArray(0);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (bigFloatBuff.capacity() != 0) {
            MemoryUtil.memFree(bigFloatBuff);
        }

        return true;
    }

    public boolean bufferIndices() { // call it before any rendering
        if (vao == 0) {
            vao = GL30.glGenVertexArrays();
        }
        int blkIndex = 0;
        for (Block block : blockList) {
            IntBuffer intBuff = Block.createIntBuffer(block.getFaceBits());
            if (intBuff == null) {
                return false; // failed to create (mem calloc failed)
            }
            // storing indices buffer on the graphics card
            int ibo = iboMap.getOrDefault(blkIndex, 0);
            if (ibo == 0) {
                ibo = GL15.glGenBuffers();
            }
            if (intBuff.capacity() != 0) {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, intBuff, GL15.GL_STATIC_DRAW);
                // finally assigning it to the array element
                iboMap.put(blkIndex, ibo);
            }
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

            if (intBuff.capacity() != 0) {
                MemoryUtil.memFree(intBuff);
            }

            blkIndex++;
        }

        return true;
    }

    public void bufferAll() { // buffer both, call it before any rendering
        buffered = bufferVertices() && bufferIndices();
    }

    public void animate() { // call only for fluid blocks
        if (!buffered || blockList.isEmpty()) {
            return;
        }

        for (Block block : blockList) {
            if (!block.isSolid()) {
                block.getMeshes().getFirst().triangSwap();
            }
        }

        updateVertices();
    }

    public void prepare(boolean cameraInFluid) { // call only for fluid blocks before rendering                      
        if (!buffered || blockList.isEmpty()) {
            return;
        }

        for (Block block : blockList) {
            if (!block.isSolid() && cameraInFluid ^ block.isVerticesReversed()) {
                block.reverseFaceVertexOrder();
            }
        }

        updateVertices();
    }

    // standard render all
    public void render(ShaderProgram shaderProgram, LightSources lightSources) {
        if (buffered && shaderProgram != null && !blockList.isEmpty()) {
            GL30.glBindVertexArray(vao);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            int blkIndex = 0;
            shaderProgram.bind();

            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "normal");
            shaderProgram.bindAttribute(2, "uv");

            lightSources.updateLightsInShaderIfModified(shaderProgram);
            for (Block block : blockList) {
                block.transform(shaderProgram);
                Texture primaryTexture = Texture.getOrDefault(block.getTexName());
                if (primaryTexture != null) { // this is primary texture
                    block.primaryColor(shaderProgram);
                    primaryTexture.bind(0, shaderProgram, "modelTexture0");
                }

                if (block.getWaterTexture() != null) { // this is reflective texture
                    block.secondaryColor(shaderProgram);
                    block.getWaterTexture().bind(1, shaderProgram, "modelTexture1");
                }

                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboMap.get(blkIndex));
                GL32.glDrawElementsBaseVertex(
                        GL11.GL_TRIANGLES,
                        Block.INDICES_COUNT,
                        GL11.GL_UNSIGNED_INT,
                        0,
                        blkIndex * Vertex.SIZE
                );

                Texture.unbind(0);
                Texture.unbind(1);

                blkIndex++;
            }
            ShaderProgram.unbind();

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

            GL30.glBindVertexArray(0);
        }
    }

    // powerful render if block is visible by camera
    public void renderIf(ShaderProgram shaderProgram, LightSources lightSources, Predicate<Block> predicate) {
        if (buffered && shaderProgram != null && !blockList.isEmpty()) {
            GL30.glBindVertexArray(vao);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            int blkIndex = 0;
            shaderProgram.bind();

            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "normal");
            shaderProgram.bindAttribute(2, "uv");

            lightSources.updateLightsInShaderIfModified(shaderProgram);
            for (Block block : blockList) {
                if (predicate.test(block)) {
                    block.transform(shaderProgram);
                    Texture primaryTexture = Texture.getOrDefault(block.getTexName());
                    if (primaryTexture != null) { // this is primary texture
                        block.primaryColor(shaderProgram);
                        primaryTexture.bind(0, shaderProgram, "modelTexture0");
                    }

                    if (block.getWaterTexture() != null) { // this is reflective texture
                        block.secondaryColor(shaderProgram);
                        block.getWaterTexture().bind(1, shaderProgram, "modelTexture1");
                    }

                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboMap.get(blkIndex));
                    GL32.glDrawElementsBaseVertex(
                            GL11.GL_TRIANGLES,
                            Block.INDICES_COUNT,
                            GL11.GL_UNSIGNED_INT,
                            0,
                            blkIndex * Vertex.SIZE
                    );
                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
                    Texture.unbind(0);
                    Texture.unbind(1);
                }
                blkIndex++;
            }

            ShaderProgram.unbind();

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            GL30.glBindVertexArray(0);
        }
    }

    public void release() {
        if (bigVbo != 0) {
            GL15.glDeleteBuffers(bigVbo);
        }
        for (Integer ibo : iboMap.values()) {
            GL15.glDeleteBuffers(ibo);
        }

        buffered = false;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public IList<Block> getBlockList() {
        return blockList;
    }

    public int getBigVbo() {
        return bigVbo;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

//    public int getDynamicSize() {
//        return dynamicSize;
//    }
}
