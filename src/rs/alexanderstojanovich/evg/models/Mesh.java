/*
 * Copyright (C) 2023 coas91@rocketmail.com
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
import java.util.List;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Mesh {

    public final IList<Vertex> vertices = new GapList<>();
    public final IList<Integer> indices = new GapList<>(); // refers which vertex we want to use when

    protected static FloatBuffer fb;
    protected static IntBuffer ib;

    protected int vbo = 0; // vertex buffer object
    protected int ibo = 0; // index buffer object  

    protected boolean buffered = false;

    public boolean bufferVertices() {
        int someSize = vertices.size() * Vertex.SIZE;
        // Allocate memory for the vertex data buffer
        if (fb == null || fb.capacity() == 0) {
            fb = MemoryUtil.memCallocFloat(someSize);
        } else if (fb.capacity() != 0 && fb.capacity() < someSize) {
            fb = MemoryUtil.memRealloc(fb, someSize);
        }

        // Set buffer position and limit
        fb.position(0);
        fb.limit(someSize);

        if (MemoryUtil.memAddressSafe(fb) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        // Fill the vertex buffer with data
        for (Vertex vertex : vertices) {
            if (vertex.isEnabled()) {
                fb.put(vertex.getPos().x)
                        .put(vertex.getPos().y)
                        .put(vertex.getPos().z)
                        .put(vertex.getNormal().x)
                        .put(vertex.getNormal().y)
                        .put(vertex.getNormal().z)
                        .put(vertex.getUv().x)
                        .put(vertex.getUv().y);
            }
        }

        if (fb.position() != 0) {
            fb.flip();
        }

        if (vbo == 0) {
            vbo = GL15.glGenBuffers();
        }

        // Bind the vertex array and vertex buffer
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // Transfer vertex data to GPU
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fb, GL15.GL_STATIC_DRAW);

        // Enable vertex attributes
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        // Specify vertex attribute pointers
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // Position
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // Normal
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // UV

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);

        // Unbind vertex array and vertex buffer
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

    public boolean subBufferVertices() {
        // Check if vertex array and vertex buffer objects are initialized
        if (vbo == 0) {
            DSLogger.reportError("Vertex buffer object is zero!", null);
            throw new RuntimeException("Vertex buffer object is zero!");
        }

        // Allocate memory for vertex data
        fb = MemoryUtil.memCallocFloat(vertices.size() * Vertex.SIZE);
        if (MemoryUtil.memAddressSafe(fb) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        // Fill the vertex buffer with data
        for (Vertex vertex : vertices) {
            if (vertex.isEnabled()) {
                fb.put(vertex.getPos().x)
                        .put(vertex.getPos().y)
                        .put(vertex.getPos().z)
                        .put(vertex.getNormal().x)
                        .put(vertex.getNormal().y)
                        .put(vertex.getNormal().z)
                        .put(vertex.getUv().x)
                        .put(vertex.getUv().y);
            }
        }
        if (fb.position() != 0) {
            fb.flip();
        }

        // Bind the vertex array and vertex buffer
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // Update vertex data on GPU
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, fb);

        return true;
    }

    public boolean bufferIndices() {
        int someSize = indices.size();
        // Allocate memory for index data
        if (ib == null || ib.capacity() == 0) {
            ib = MemoryUtil.memCallocInt(someSize);
        } else if (ib.capacity() != 0 && ib.capacity() < someSize) {
            ib = MemoryUtil.memRealloc(ib, someSize);
        }
        ib.position(0);
        ib.limit(someSize);

        if (MemoryUtil.memAddressSafe(ib) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        // Fill the index buffer with data
        for (Integer index : indices) {
            ib.put(index);
        }
        ib.flip();

        // Generate index buffer object if not already generated
        if (ibo == 0) {
            ibo = GL15.glGenBuffers();
        }

        // Bind the index buffer
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        // Transfer index data to GPU
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ib, GL15.GL_STATIC_DRAW);

        return true;
    }

    public void bufferAll() { // explicit call to buffer unbuffered before the rendering        
        buffered = bufferVertices() && bufferIndices();
    }

    public void unbuffer() {
        buffered = false;
    }

    public void calcNormals() {
        for (int i = 0; i < indices.size(); i += 3) {
            int i0 = indices.get(i);
            int i1 = indices.get(i + 1);
            int i2 = indices.get(i + 2);

            Vector3f v1 = vertices.get(i1).getPos().sub(vertices.get(i0).getPos());
            Vector3f v2 = vertices.get(i2).getPos().sub(vertices.get(i0).getPos());

            Vector3f normal = v1.cross(v2).normalize();
            vertices.get(i0).setNormal(vertices.get(i0).getNormal().add(normal));
            vertices.get(i1).setNormal(vertices.get(i1).getNormal().add(normal));
            vertices.get(i2).setNormal(vertices.get(i2).getNormal().add(normal));
        }

        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).setNormal(vertices.get(i).getNormal().normalize());
        }
    }

    public void nullifyNormals() {
        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).getNormal().zero();
        }
        buffered = false;
    }

    public void negateNormals() {
        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).getNormal().negate();
        }
        buffered = false;
    }

    /**
     * Swaps the UV coordinates of the vertices in each triangle.
     *
     * This method iterates through the indices list in steps of three,
     * corresponding to the vertices of each triangle. For each triangle, it
     * swaps the UV coordinates of the vertices such that: - Vertex A gets the
     * UV of Vertex C - Vertex B gets the UV of Vertex A - Vertex C gets the UV
     * of Vertex B
     *
     * This operation is useful for modifying the texture mapping of the mesh.
     * After performing the swap, the `buffered` flag is set to false,
     * indicating that the vertex buffer needs to be updated.
     */
    public void triangSwap() {
        // Get the size of the indices list
        int size = indices.size();

        // Loop through each triangle (step by 3)
        for (int i = 0; i < size; i += 3) {
            // Get the indices for the vertices of the current triangle
            int indexA = indices.get(i);
            int indexB = indices.get(i + 1);
            int indexC = indices.get(i + 2);

            // Get the vertices corresponding to these indices
            Vertex a = vertices.get(indexA);
            Vertex b = vertices.get(indexB);
            Vertex c = vertices.get(indexC);

            // Get the UV coordinates of these vertices
            Vector2f uvA = a.getUv();
            Vector2f uvB = b.getUv();
            Vector2f uvC = c.getUv();

            // Swap the UV coordinates
            c.setUv(uvB); // C gets UV of B
            b.setUv(uvA); // B gets UV of A
            a.setUv(uvC); // A gets UV of C
        }
        // Mark the vertex buffer as needing an update
        buffered = false;
    }

    public static void triangSwap(List<Vertex> vertices, List<Integer> indices) {
        for (int i = 0; i < indices.size(); i += 3) {
            Vertex a = vertices.get(indices.get(i));
            Vertex b = vertices.get(indices.get(i + 1));
            Vertex c = vertices.get(indices.get(i + 2));
            Vector2f temp = c.getUv();
            c.setUv(b.getUv());
            b.setUv(a.getUv());
            a.setUv(temp);
        }
    }

    public IList<Vertex> getVertices() {
        return vertices;
    }

    public IList<Integer> getIndices() {
        return indices;
    }

    public void release() {
        if (vbo != 0) {
            GL15.glDeleteBuffers(vbo);
        }
        if (ibo != 0) {
            GL15.glDeleteBuffers(ibo);
        }

        if (fb != null && fb.capacity() != 0) {
            // Free memory allocated for vertex data
            MemoryUtil.memFree(fb);
            fb = null;
        }

        if (ib != null && ib.capacity() != 0) {
            // Free memory allocated for index data
            MemoryUtil.memFree(ib);
            ib = null;
        }

        buffered = false;
    }
}
