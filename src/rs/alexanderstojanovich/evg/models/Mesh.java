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
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Mesh {

    protected final IList<Vertex> vertices = new GapList<>();
    protected final IList<Integer> indices = new GapList<>(); // refers which vertex we want to use when

    protected static FloatBuffer fb;
    protected static IntBuffer ib;

    protected int vbo = 0; // vertex buffer object
    protected int ibo = 0; // index buffer object  

    protected boolean buffered = false;

    public void bufferVertices() {
        // storing vertices and normals in the buffer
        fb = MemoryUtil.memAllocFloat(vertices.size() * Vertex.SIZE);
        fb.clear();
        for (Vertex vertex : vertices) {
            if (vertex.isEnabled()) {
                fb.put(vertex.getPos().x);
                fb.put(vertex.getPos().y);
                fb.put(vertex.getPos().z);

                fb.put(vertex.getNormal().x);
                fb.put(vertex.getNormal().y);
                fb.put(vertex.getNormal().z);

                fb.put(vertex.getUv().x);
                fb.put(vertex.getUv().y);
            }
        }
        fb.flip();
        // storing vertices and normals buffer on the graphics card
        if (vbo == 0) {
            vbo = GL15.glGenBuffers();
        }

        if (fb.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fb, GL15.GL_STATIC_DRAW);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (fb.capacity() != 0) {
            MemoryUtil.memFree(fb);
        }
    }

    public void updateVertices() {
        fb = MemoryUtil.memAllocFloat(vertices.size() * Vertex.SIZE);
        fb.clear();
        // storing vertices and normals in the buffer        
        for (Vertex vertex : vertices) {
            if (vertex.isEnabled()) {
                fb.put(vertex.getPos().x);
                fb.put(vertex.getPos().y);
                fb.put(vertex.getPos().z);

                fb.put(vertex.getNormal().x);
                fb.put(vertex.getNormal().y);
                fb.put(vertex.getNormal().z);

                fb.put(vertex.getUv().x);
                fb.put(vertex.getUv().y);
            }
        }
        fb.flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, fb);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (fb.capacity() != 0) {
            MemoryUtil.memFree(fb);
        }
    }

    public void bufferIndices() {
        // storing indices in the buffer
        ib = MemoryUtil.memAllocInt(indices.size());
        for (Integer index : indices) {
            ib.put(index);
        }
        ib.flip();
        // storing indices buffer on the graphics card
        if (ibo == 0) {
            ibo = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ib, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        if (ib.capacity() != 0) {
            MemoryUtil.memFree(ib);
        }
    }

    public void bufferAll() { // explicit call to buffer unbuffered before the rendering
        bufferVertices();
        bufferIndices();
        buffered = true;
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

    public void triangSwap() {
        for (int i = 0; i < indices.size(); i += 3) {
            Vertex a = vertices.get(indices.get(i));
            Vertex b = vertices.get(indices.get(i + 1));
            Vertex c = vertices.get(indices.get(i + 2));
            Vector2f temp = c.getUv();
            c.setUv(b.getUv());
            b.setUv(a.getUv());
            a.setUv(temp);
        }
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

}
