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
package rs.alexanderstojanovich.evg.chunk;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.IList;
import static rs.alexanderstojanovich.evg.chunk.Tuple.MAT4_SIZE;
import static rs.alexanderstojanovich.evg.chunk.Tuple.VEC4_SIZE;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Vertex;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class TupleBufferObject {

    protected int bigVbo = 0;
    protected int vec4Vbo = 0;
    protected int mat4Vbo = 0;
    protected int bigIbo = 0;

    protected static FloatBuffer bigFloatBuff = null;
    protected static FloatBuffer vec4FloatColorBuff = null;
    protected static FloatBuffer mat4FloatModelBuff = null; // model matrix [col0, col1, col2, col3]
    protected static IntBuffer intBuffer = null;

    protected boolean bufferedVertices = false;
    protected boolean bufferedIndices = false;
    protected boolean buffered = false;

    public final IList<Tuple> tuples;

    /**
     * Construct new Tuple Buffer Object
     *
     * @param tuples @param tuples tuples to buffer together into batch
     *
     */
    public TupleBufferObject(IList<Tuple> tuples) {
        this.tuples = tuples;
    }

    /**
     * Buffer vertex data prior rendering
     *
     * @return if vertex data was successfully buffered
     */
    public boolean bufferVertices() { // Call before rendering
        // Calculate the total size needed for vertex data
        int someSize = 0;

        // Calculating the size
        for (Tuple tuple : tuples) {
            someSize += tuple.blockList.size() * tuple.verticesNum * Vertex.SIZE;
        }

        // Allocate memory for the vertex data buffer
        if (bigFloatBuff == null) {
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
        for (Tuple tuple : tuples) {
            for (Block block : tuple.blockList) {
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

        // Calculate the total size needed for vertex data
        int someSize = 0;

        // Calculating the size
        for (Tuple tuple : tuples) {
            someSize += tuple.blockList.size() * tuple.verticesNum * Vertex.SIZE;
        }

        // Allocate memory for the vertex data buffer
        if (bigFloatBuff == null) {
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
        for (Tuple tuple : tuples) {
            for (Block block : tuple.blockList) {
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

    /**
     * Buffer VEC3 colors - instanced rendering
     *
     * @return buffered success
     */
    protected boolean bufferColors() { // buffering colors
        int someSize = 0;
        for (Tuple tuple : tuples) {
            someSize += tuple.blockList.size() * VEC4_SIZE;
        }
        if (vec4FloatColorBuff == null) {
            vec4FloatColorBuff = MemoryUtil.memCallocFloat(someSize);
        } else if (vec4FloatColorBuff.capacity() != 0 && vec4FloatColorBuff.capacity() < someSize) {
            vec4FloatColorBuff = MemoryUtil.memRealloc(vec4FloatColorBuff, someSize);
        }
        vec4FloatColorBuff.position(0);
        vec4FloatColorBuff.limit(someSize);

        if (vec4FloatColorBuff.capacity() != 0 && MemoryUtil.memAddressSafe(vec4FloatColorBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        for (Tuple tuple : tuples) {
            for (Block block : tuple.blockList) {
                Vector4f color = block.getPrimaryRGBAColor();
                vec4FloatColorBuff.put(color.x);
                vec4FloatColorBuff.put(color.y);
                vec4FloatColorBuff.put(color.z);
                vec4FloatColorBuff.put(color.w);
            }
        }
        if (vec4FloatColorBuff.position() != 0) {
            vec4FloatColorBuff.flip();
        }

        if (vec4Vbo == 0) {
            vec4Vbo = GL15.glGenBuffers();
        }

        if (vec4FloatColorBuff.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vec4Vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vec4FloatColorBuff, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(3, VEC4_SIZE, GL11.GL_FLOAT, false, VEC4_SIZE * 4, 0); // this is for color
            GL33.glVertexAttribDivisor(3, 1);
            GL20.glDisableVertexAttribArray(3);
            //**
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

    /**
     * Buffer Model MAT4[col0, col1, col2, col3]
     *
     * @return buffered success
     */
    protected boolean bufferModelMatrices() { // buffering model matrices
        int someSize = 0;
        for (Tuple tuple : tuples) {
            someSize += tuple.blockList.size() * MAT4_SIZE;
        }
        if (mat4FloatModelBuff == null) {
            mat4FloatModelBuff = MemoryUtil.memCallocFloat(someSize);
        } else if (mat4FloatModelBuff.capacity() != 0 && mat4FloatModelBuff.capacity() < someSize) {
            mat4FloatModelBuff = MemoryUtil.memRealloc(mat4FloatModelBuff, someSize);
        }
        mat4FloatModelBuff.position(0);
        mat4FloatModelBuff.limit(someSize);

        for (Tuple tuple : tuples) {
            for (Block block : tuple.blockList) {
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
        }

        if (mat4FloatModelBuff.position() != 0) {
            mat4FloatModelBuff.flip();
        }

        if (mat4Vbo == 0) {
            mat4Vbo = GL15.glGenBuffers();
        }

        if (mat4FloatModelBuff.capacity() != 0) {
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
            //**
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

    /**
     * Buffer indices for selected tuples
     *
     * @return
     */
    public boolean bufferIndices() { // Call before rendering        
        int someSize = 0;
        for (Tuple tuple : tuples) {
            someSize += tuple.blockList.size() * tuple.indicesNum;
        }

        if (intBuffer == null) {
            intBuffer = MemoryUtil.memAllocInt(someSize);
        } else if (intBuffer.capacity() != 0 && intBuffer.capacity() < someSize) {
            intBuffer = MemoryUtil.memRealloc(intBuffer, someSize);
        }

        intBuffer.position(0);
        intBuffer.limit(someSize);

        if (intBuffer.capacity() != 0 && MemoryUtil.memAddressSafe(intBuffer) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        int baseConst = 0;
        for (Tuple tuple : tuples) {
            DSLogger.reportInfo("BaseConst=" + baseConst, null);
            for (Block block : tuple.blockList) {
                IList<Integer> indices = Block.createIndices(block.getFaceBits(), baseConst);
                DSLogger.reportInfo(indices.toString(), null);
                for (int i : indices) {
                    intBuffer.put(i);
                }
            }
            baseConst += tuple.facesNum;
        }

        if (intBuffer.position() != 0) {
            intBuffer.flip();
        }

        // Generate index buffer object if not already generated
        if (bigIbo == 0) {
            bigIbo = GL15.glGenBuffers();
        }

        if (intBuffer.capacity() != 0) {
            // Bind the index buffer and transfer data
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, bigIbo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, intBuffer, GL15.GL_STATIC_DRAW);
            // Store the ibo in the map
        }

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        return true;
    }

    /**
     * Buffer all tuples together (into single geometry).
     *
     */
    public void bufferBatchAll() {
        buffered = false;
        bufferedVertices = bufferVertices() && bufferColors() && bufferModelMatrices();
        bufferedIndices = bufferIndices();

        buffered |= bufferedVertices && bufferedIndices;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    public int getBigVbo() {
        return bigVbo;
    }

    public int getVec4Vbo() {
        return vec4Vbo;
    }

    public int getMat4Vbo() {
        return mat4Vbo;
    }

    public int getBigIbo() {
        return bigIbo;
    }

}
