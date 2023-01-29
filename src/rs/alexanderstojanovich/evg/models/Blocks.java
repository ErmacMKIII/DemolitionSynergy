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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.magicwerk.brownies.collections.BigList;
import rs.alexanderstojanovich.evg.level.LightSources;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Blocks { // mutual class for both solid blocks and fluid blocks with improved rendering

    public static final int DYNAMIC_INCREMENT = Configuration.getInstance().getBlockDynamicSize();

    protected final List<Block> blockList = new BigList<>(DYNAMIC_INCREMENT);
    protected int bigVbo = 0;
    protected boolean verticesReversed = false;
    // array with offsets in the big float buffer
    // this is maximum amount of blocks of the type game can hold
    protected final Map<Integer, Integer> vboEntries = new HashMap<>();
    // --------------blkIndex---ibo-----------------------------
    protected final Map<Integer, Integer> iboMap = new HashMap<>();
    protected boolean buffered = false;

    protected int dynamicSize = 0;
    protected FloatBuffer bigFloatBuff;

    public void bufferVertices() { // call it before any rendering
        // auto adjust dynamic size of float buff and do it on every 1000th element
        if (bigFloatBuff == null || blockList.size() > dynamicSize) {
            dynamicSize = blockList.size() + DYNAMIC_INCREMENT;
            bigFloatBuff = BufferUtils.createFloatBuffer(dynamicSize * Block.VERTEX_COUNT * Vertex.SIZE);
        }
        bigFloatBuff.clear();
        int offset = 0;
        int blkIndex = 0;
        for (Block block : blockList) {
            vboEntries.put(blkIndex, offset);
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
                    offset++;
                }
            }
            blkIndex++;
        }
        bigFloatBuff.flip();
        if (bigVbo == 0) {
            bigVbo = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bigFloatBuff, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void updateVertices() { // call it before any rendering        
        bigFloatBuff.clear();
        int offset = 0;
        int blkIndex = 0;
        for (Block block : blockList) {
            vboEntries.put(blkIndex, offset);
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
                    offset++;
                }
            }
            blkIndex++;
        }
        bigFloatBuff.flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, bigFloatBuff);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
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
            blkIndex++;
        }
    }

    public void bufferAll() { // buffer both, call it before any rendering
        bufferVertices();
        bufferIndices();
        buffered = true;
    }

    public void animate() { // call only for fluid blocks
        for (Block block : blockList) {
            block.animate();
        }

        if (bigFloatBuff == null) {
            bufferVertices();
        } else {
            updateVertices();
        }
    }

    public void prepare(boolean cameraInFluid) { // call only for fluid blocks before rendering
        if (Boolean.logicalXor(cameraInFluid, verticesReversed)) {
            for (Block block : blockList) {
                block.reverseFaceVertexOrder();
            }
            verticesReversed = !verticesReversed;
            if (bigFloatBuff == null) {
                bufferVertices();
            } else {
                updateVertices();
            }
        }
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
                Texture primaryTexture = Texture.TEX_MAP.get(block.texName).getKey();
                if (primaryTexture != null) { // this is primary texture
                    block.primaryColor(shaderProgram);
                    primaryTexture.bind(0, shaderProgram, "modelTexture0");
                }

                if (block.waterTexture != null) { // this is reflective texture
                    block.secondaryColor(shaderProgram);
                    block.waterTexture.bind(1, shaderProgram, "modelTexture1");
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
                    Texture primaryTexture = Texture.TEX_MAP.get(block.texName).getKey();
                    if (primaryTexture != null) { // this is primary texture
                        block.primaryColor(shaderProgram);
                        primaryTexture.bind(0, shaderProgram, "modelTexture0");
                    }

                    if (block.waterTexture != null) { // this is reflective texture
                        block.secondaryColor(shaderProgram);
                        block.waterTexture.bind(1, shaderProgram, "modelTexture1");
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

    public List<Block> getBlockList() {
        return blockList;
    }

    public int getBigVbo() {
        return bigVbo;
    }

    public boolean isVerticesReversed() {
        return verticesReversed;
    }

    public void setVerticesReversed(boolean verticesReversed) {
        this.verticesReversed = verticesReversed;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    public int getDynamicSize() {
        return dynamicSize;
    }

}
