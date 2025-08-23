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
import java.util.function.Predicate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Vertex;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Series { // mutual class for both solid blocks and fluid blocks with improved rendering

    public static final int DYNAMIC_INCREMENT = Configuration.getInstance().getBlockDynamicSize();

    public final IList<Block> blockList = new BigList<>(DYNAMIC_INCREMENT);
    protected int bigVbo = 0;
    // array with offsets in the big float buffer
    // this is maximum amount of blocks of the type game can hold
    // --------------blkIndex---ibo-----------------------------
    protected boolean buffered = false;

//    protected static int dynamicSize = DYNAMIC_INCREMENT;
    protected static FloatBuffer bigFloatBuff = null;

    protected static IntBuffer intBuff;
    protected int ibo = 0;

    protected int indicesNum; // not used (unless in Tuples)
    protected int verticesNum; // not used (unless in Tuples)

    public Series() {
        verticesNum = Block.VERTEX_COUNT;
        indicesNum = Block.INDICES_COUNT;
    }

    /**
     * Buffer vertex data prior rendering
     *
     * @return if vertex data was successfully buffered
     */
    public boolean bufferVertices() { // Call before rendering
        // Calculate the total size needed for vertex data
        int someSize = blockList.size() * verticesNum * Vertex.SIZE;

        // Allocate memory for the vertex data buffer
        if (bigFloatBuff == null || bigFloatBuff.capacity() == 0) {
            bigFloatBuff = MemoryUtil.memCallocFloat(someSize);
        } else if (bigFloatBuff.capacity() != 0 && bigFloatBuff.capacity() < someSize) {
            bigFloatBuff = MemoryUtil.memRealloc(bigFloatBuff, someSize);
        }
        // Set buffer position and limit
        bigFloatBuff.position(0);
        bigFloatBuff.limit(someSize);

        if (bigFloatBuff.capacity() != 0 && MemoryUtil.memAddressSafe(bigFloatBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        // Iterate through blocks and vertices to populate the buffer
        for (Block block : blockList) {
            for (Vertex vertex : block.getVertices()) {
                if (vertex.isEnabled()) {
                    // Put vertex data into the buffer
                    bigFloatBuff.put(vertex.getPos().x)
                            .put(vertex.getPos().y)
                            .put(vertex.getPos().z)
                            .put(vertex.getNormal().x)
                            .put(vertex.getNormal().y)
                            .put(vertex.getNormal().z)
                            .put(vertex.getUv().x)
                            .put(vertex.getUv().y);
                }
            }
        }

        // Flip the buffer => to allow reading
        if (bigFloatBuff.position() != 0) {
            bigFloatBuff.flip();
        }

        // Generate vertex buffer object if not already generated
        if (bigVbo == 0) {
            bigVbo = GL15.glGenBuffers();
        }

        // Buffer vertex data
        if (bigFloatBuff.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bigFloatBuff, GL15.GL_STATIC_DRAW);

            // Enable vertex attributes
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            // Specify vertex attribute pointers
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 0); // Position
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 12); // Normal
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 24); // UV

            // Disable vertex attributes after configuration
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

    /**
     * SubBuffer vertex data prior rendering. And after at at least one vertex
     * data buffering
     *
     * @return if vertex data was successfully buffered
     */
    public boolean subBufferVertices() { // Call before rendering
        if (bigVbo == 0) {
            DSLogger.reportError("Vertex array object or vertex buffer object is zero!", null);
            throw new RuntimeException("Vertex array object or vertex buffer object is zero!");
        }

        // Allocate memory for the vertex data buffer
        int someSize = blockList.size() * verticesNum * Vertex.SIZE;
        if (bigFloatBuff == null || bigFloatBuff.capacity() == 0) {
            bigFloatBuff = MemoryUtil.memCallocFloat(someSize);
        } else if (bigFloatBuff.capacity() < someSize) {
            bigFloatBuff = MemoryUtil.memRealloc(bigFloatBuff, someSize);
        }
        // Set buffer position and limit
        bigFloatBuff.position(0);
        bigFloatBuff.limit(someSize);

        if (bigFloatBuff.capacity() != 0 && MemoryUtil.memAddressSafe(bigFloatBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        // Iterate through blocks and vertices to populate the buffer
        for (Block block : blockList) {
            for (Vertex vertex : block.getVertices()) {
                if (vertex.isEnabled()) {
                    // Put vertex data into the buffer
                    bigFloatBuff.put(vertex.getPos().x)
                            .put(vertex.getPos().y)
                            .put(vertex.getPos().z)
                            .put(vertex.getNormal().x)
                            .put(vertex.getNormal().y)
                            .put(vertex.getNormal().z)
                            .put(vertex.getUv().x)
                            .put(vertex.getUv().y);
                }
            }
        }

        // Flip the buffer => to allow reading
        if (bigFloatBuff.position() != 0) {
            bigFloatBuff.flip();
        }

        // Sub-Buffer vertex data
        if (bigFloatBuff.capacity() != 0) {
            // Update the vertex buffer object with new data
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, bigFloatBuff);

            // Enable vertex attributes
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            // Specify vertex attribute pointers
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 0); // Position
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 12); // Normal
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 24); // UV            

            // Disable vertex attributes after configuration
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);

        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

    public static int checkSize(int bitValue) {
        // Initialize a counter for counting ones
        int onesCount = 0;

        // Iterate until the number becomes 0
        while (bitValue != 0) {
            // Use bitwise AND operation to check if the least significant bit is 1
            // If it is, increment the counter
            onesCount += bitValue & 1;
            // Right shift the number by 1 bit to process the next bit
            bitValue >>= 1;
        }

        return onesCount * 6;
    }

    public boolean bufferIndices() { // Call before rendering        
        int blkIndex = 0;

        for (Block block : blockList) {
            int someSize = checkSize(block.getFaceBits());
            if (intBuff == null) {
                intBuff = Block.createIntBuffer(block.getFaceBits());
            } else if (intBuff.capacity() != 0 && intBuff.capacity() < someSize) {
                intBuff = Block.resizeIntBuffer(intBuff, block.getFaceBits());
            }
            intBuff.position(0);
            intBuff.limit(someSize);

            if (intBuff.capacity() != 0 && MemoryUtil.memAddressSafe(intBuff) == MemoryUtil.NULL) {
                DSLogger.reportError("Could not allocate memory address!", null);
                throw new RuntimeException("Could not allocate memory address!");
            }

            // Generate index buffer object if not already generated
            if (ibo == 0) {
                ibo = GL15.glGenBuffers();
            }

            if (intBuff.capacity() != 0) {
                // Bind the index buffer and transfer data
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, intBuff, GL15.GL_STATIC_DRAW);
                // Store the ibo in the map
            }

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

            blkIndex++;
        }

        return true;
    }

    public boolean bufferIndices(int baseConst) { // Call before rendering        
        int blkIndex = 0;

        for (Block block : blockList) {
            int someSize = checkSize(block.getFaceBits());
            if (intBuff == null || intBuff.capacity() == 0) {
                intBuff = Block.createIntBuffer(block.getFaceBits(), baseConst);
            } else if (intBuff.capacity() != 0 && intBuff.capacity() < someSize) {
                intBuff = Block.resizeIntBuffer(intBuff, block.getFaceBits(), baseConst);
            }
            intBuff.position(0);
            intBuff.limit(someSize);

            if (intBuff.capacity() != 0 && MemoryUtil.memAddressSafe(intBuff) == MemoryUtil.NULL) {
                DSLogger.reportError("Could not allocate memory address!", null);
                throw new RuntimeException("Could not allocate memory address!");
            }

            // Generate index buffer object if not already generated
            if (ibo == 0) {
                ibo = GL15.glGenBuffers();
            }

            if (intBuff.capacity() != 0) {
                // Bind the index buffer and transfer data
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, intBuff, GL15.GL_STATIC_DRAW);
                // Store the ibo in the map
            }

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

            blkIndex++;
        }

        return true;
    }

    public void bufferAll() { // Buffer both, call before rendering
        buffered = bufferVertices() && bufferIndices();
    }

    public void animate() { // Call only for fluid blocks
        if (!buffered || blockList.isEmpty()) {
            return;
        }

        // Perform animation
        for (Block block : blockList) {
            if (!block.isSolid()) {
                block.getMeshes().getFirst().triangSwap();
            }
        }

        subBufferVertices();
    }

    public void prepare(boolean cameraInFluid) { // Call only for fluid blocks before rendering
        if (!buffered || blockList.isEmpty()) {
            return;
        }

        // Prepare blocks based on camera position
        for (Block block : blockList) {
            if (!block.isSolid() && cameraInFluid ^ block.isVerticesReversed()) {
                block.reverseFaceVertexOrder();
            }
        }

        subBufferVertices();
    }

    public void render(ShaderProgram shaderProgram, LightSources lightSources) { // Standard render all
        if (buffered && shaderProgram != null && !blockList.isEmpty()) {
            // Enable vertex attribute arrays
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            int blkIndex = 0;
            shaderProgram.bind();

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 0); // Position
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 12); // Normal
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 24); // UV
            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "normal");
            shaderProgram.bindAttribute(2, "uv");

            lightSources.updateLightsInShaderIfModified(shaderProgram);

            // Render each block
            for (Block block : blockList) {
                block.transform(shaderProgram);

                Texture primaryTexture = Texture.getOrDefault(block.getTexName());
                if (primaryTexture != null) {
                    block.primaryColor(shaderProgram);
                    primaryTexture.bind(0, shaderProgram, "modelTexture0");
                }

                if (block.getWaterTexture() != null) {
                    block.secondaryColor(shaderProgram);
                    block.getWaterTexture().bind(1, shaderProgram, "modelTexture1");
                }

//                block.lightColor(shaderProgram);
                // Draw elements
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
                GL32.glDrawElementsBaseVertex(GL11.GL_TRIANGLES, Block.INDICES_COUNT, GL11.GL_UNSIGNED_INT, 0, blkIndex * Vertex.SIZE);

                Texture.unbind(0);
                Texture.unbind(1);

                blkIndex++;
            }
            ShaderProgram.unbind();

            // Disable vertex attribute arrays
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);

            // Unbind vertex array and buffers
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            // GL30.glBindVertexArray(0);
        }
    }

    public void renderIf(ShaderProgram shaderProgram, LightSources lightSources, Predicate<Block> predicate) { // Powerful render if block is visible by camera
        if (buffered && shaderProgram != null && !blockList.isEmpty()) {
            // Enable vertex attribute arrays
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            int blkIndex = 0;
            shaderProgram.bind();

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 0); // Position
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 12); // Normal
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 24); // UV
            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "normal");
            shaderProgram.bindAttribute(2, "uv");

            lightSources.updateLightsInShaderIfModified(shaderProgram);

            // Render each block based on predicate
            for (Block block : blockList) {
                if (predicate.test(block)) {
                    block.transform(shaderProgram);

                    Texture primaryTexture = Texture.getOrDefault(block.getTexName());
                    if (primaryTexture != null) {
                        block.primaryColor(shaderProgram);
                        primaryTexture.bind(0, shaderProgram, "modelTexture0");
                    }

                    if (block.getWaterTexture() != null) {
                        block.secondaryColor(shaderProgram);
                        block.getWaterTexture().bind(1, shaderProgram, "modelTexture1");
                    }

//                    block.lightColor(shaderProgram);
                    // Draw elements
                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
                    GL32.glDrawElementsBaseVertex(GL11.GL_TRIANGLES, Block.INDICES_COUNT, GL11.GL_UNSIGNED_INT, 0, blkIndex * Vertex.SIZE);

                    Texture.unbind(0);
                    Texture.unbind(1);
                }
                blkIndex++;
            }

            ShaderProgram.unbind();

            // Disable vertex attribute arrays
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);

            // Unbind vertex array and buffers
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            // GL30.glBindVertexArray(0);
        }
    }

    public void release() {
        if (bigVbo != 0) {
            GL15.glDeleteBuffers(bigVbo);
        }
        if (ibo != 0) {
            GL15.glDeleteBuffers(ibo);
        }

        if (bigFloatBuff != null && bigFloatBuff.capacity() != 0) {
            MemoryUtil.memFree(bigFloatBuff);
            bigFloatBuff = null;
        }

        if (intBuff != null && intBuff.capacity() != 0) {
            MemoryUtil.memFree(intBuff);
            intBuff = null;
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

}
