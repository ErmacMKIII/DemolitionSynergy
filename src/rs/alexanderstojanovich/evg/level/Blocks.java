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
package rs.alexanderstojanovich.evg.level;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Vertex;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Blocks { // mutual class for both solid blocks and fluid blocks with improved rendering

    public static final int DYNAMIC_INCREMENT = Configuration.getInstance().getBlockDynamicSize();

    protected final IList<Block> blockList = new BigList<>(DYNAMIC_INCREMENT);
    protected int bigVbo = 0;
    // array with offsets in the big float buffer
    // this is maximum amount of blocks of the type game can hold
    protected final Map<Integer, Integer> vboEntries = new HashMap<>();
    // --------------blkIndex---ibo-----------------------------
    protected final Map<Integer, Integer> iboMap = new HashMap<>();
    protected boolean buffered = false;

//    protected static int dynamicSize = DYNAMIC_INCREMENT;
    protected static FloatBuffer bigFloatBuff = MemoryUtil.memAllocFloat(DYNAMIC_INCREMENT * Block.VERTEX_COUNT * Vertex.SIZE);

    public void bufferVertices() { // call it before any rendering
        bigFloatBuff = MemoryUtil.memAllocFloat(blockList.size() * Block.VERTEX_COUNT * Vertex.SIZE);

        bigFloatBuff.clear();
        int offset = 0;
        int blkIndex = 0;
        for (Block block : blockList) {
            vboEntries.put(blkIndex, offset);
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
        bigFloatBuff.flip();
        if (bigVbo == 0) {
            bigVbo = GL15.glGenBuffers();
        }

        if (bigFloatBuff.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bigFloatBuff, GL15.GL_STATIC_DRAW);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (bigFloatBuff.capacity() != 0) {
            MemoryUtil.memFree(bigFloatBuff);
        }
    }

    public void updateVertices() { // call it before any rendering           
        bigFloatBuff = MemoryUtil.memAllocFloat(blockList.size() * Block.VERTEX_COUNT * Vertex.SIZE);
        bigFloatBuff.clear();
        int offset = 0;
        int blkIndex = 0;
        for (Block block : blockList) {
            vboEntries.put(blkIndex, offset);
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
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, bigFloatBuff);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (bigFloatBuff.capacity() != 0) {
            MemoryUtil.memFree(bigFloatBuff);
        }
    }

    public void bufferIndices() { // call it before any rendering
        int blkIndex = 0;
        for (Block block : blockList) {
            IntBuffer intBuff = Block.createIntBuffer(block.getFaceBits());
            // storing indices buffer on the graphics card
            int ibo = iboMap.getOrDefault(blkIndex, 0);
            if (ibo == 0) {
                ibo = GL15.glGenBuffers();
            }
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, intBuff, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            // finally assigning it to the array element
            iboMap.put(blkIndex, ibo);

            if (intBuff.capacity() != 0) {
                MemoryUtil.memFree(intBuff);
            }

            blkIndex++;
        }
    }

    public void bufferAll() { // buffer both, call it before any rendering
        bufferVertices();
        bufferIndices();
        buffered = true;
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

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 0); // this is for pos
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 12); // this is for normal                                        
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 24); // this is for uv                                   

            int blkIndex = 0;
            shaderProgram.bind();
            lightSources.updateLightsInShader(shaderProgram);
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
                        vboEntries.get(blkIndex)
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

        }
    }

    // powerful render if block is visible by camera
    public void renderIf(ShaderProgram shaderProgram, LightSources lightSources, Predicate<Block> predicate) {
        if (buffered && shaderProgram != null && !blockList.isEmpty()) {

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 0); // this is for pos
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 12); // this is for normal                                        
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 24); // this is for uv                                   

            int blkIndex = 0;
            shaderProgram.bind();
            lightSources.updateLightsInShader(shaderProgram);
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
                            vboEntries.get(blkIndex)
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

        }
    }

    @Deprecated
    public void release() {
        if (buffered) {
            GL15.glDeleteBuffers(bigVbo);
            for (Integer ibo : iboMap.values()) {
                GL15.glDeleteBuffers(ibo);
            }
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
