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
package rs.alexanderstojanovich.evg.intrface;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Quad implements ComponentIfc {

    public static final int VERTEX_SIZE = 4;
    public static final int VERTEX_COUNT = 4;

    protected int width;
    protected int height;
    protected Texture texture;

    protected Vector4f color = new Vector4f(GlobalColors.WHITE, 1.0f);
    protected float scale = 1.0f;

    protected Vector2f pos = new Vector2f();
    protected boolean enabled = true;

    protected boolean ignoreFactor = false;

    protected static final Vector2f[] VERTICES = {
        new Vector2f(-1.0f, -1.0f),
        new Vector2f(1.0f, -1.0f),
        new Vector2f(1.0f, 1.0f),
        new Vector2f(-1.0f, 1.0f)
    };

    protected static FloatBuffer FLOAT_BUFFER = null;

    protected Vector2f[] uvs = new Vector2f[4];
    protected static final int[] INDICES = {0, 1, 2, 2, 3, 0};

    protected int vbo = 0;

    protected static IntBuffer intBuffer = null;
    protected int ibo = 0;
    protected boolean buffered = false;

    public Quad(int width, int height, Texture texture) {
        this.width = width;
        this.height = height;
        this.texture = texture;
        initUVs();
    }

    public Quad(int width, int height, Texture texture, boolean ignoreFactor) {
        this.width = width;
        this.height = height;
        this.texture = texture;
        this.ignoreFactor = ignoreFactor;
        initUVs();
    }

    private void initUVs() {
        uvs[0] = new Vector2f(0.0f, 1.0f); // (-1.0f, -1.0f)
        uvs[1] = new Vector2f(1.0f, 1.0f); // (1.0f, -1.0f)
        uvs[2] = new Vector2f(1.0f, 0.0f); // (1.0f, 1.0f)
        uvs[3] = new Vector2f(0.0f, 0.0f); // (-1.0f, 1.0f)
    }

    @Override
    public boolean bufferVertices() {
        int someSize = 4 * VERTEX_COUNT;
        if (FLOAT_BUFFER == null || FLOAT_BUFFER.capacity() == 0) {
            FLOAT_BUFFER = MemoryUtil.memCallocFloat(someSize);
        } else if (FLOAT_BUFFER.capacity() != 0 && FLOAT_BUFFER.capacity() < someSize) {
            FLOAT_BUFFER = MemoryUtil.memRealloc(FLOAT_BUFFER, someSize);
        }
        FLOAT_BUFFER.position(0);
        FLOAT_BUFFER.limit(someSize);

        if (FLOAT_BUFFER.capacity() != 0 && MemoryUtil.memAddressSafe(FLOAT_BUFFER) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }
        for (int i = 0; i < 4; i++) {
            FLOAT_BUFFER.put(VERTICES[i].x);
            FLOAT_BUFFER.put(VERTICES[i].y);
            FLOAT_BUFFER.put(uvs[i].x);
            FLOAT_BUFFER.put(uvs[i].y);
        }
        FLOAT_BUFFER.flip();

        if (vbo == 0) {
            vbo = GL15.glGenBuffers();
        }

        if (FLOAT_BUFFER.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, FLOAT_BUFFER, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);

            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv                                                 

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

    @Override
    public boolean subBufferVertices() {
        if (vbo == 0) {
            DSLogger.reportError("Vertex buffer object is zero!", null);
            throw new RuntimeException("Vertex buffer object is zero!");
        }

        int someSize = 4 * VERTEX_COUNT;
        if (FLOAT_BUFFER == null || FLOAT_BUFFER.capacity() == 0) {
            FLOAT_BUFFER = MemoryUtil.memCallocFloat(someSize);
        } else if (FLOAT_BUFFER.capacity() != 0 && FLOAT_BUFFER.capacity() < someSize) {
            FLOAT_BUFFER = MemoryUtil.memRealloc(FLOAT_BUFFER, someSize);
        }
        FLOAT_BUFFER.position(0);
        FLOAT_BUFFER.limit(someSize);

        for (int i = 0; i < 4; i++) {
            FLOAT_BUFFER.put(VERTICES[i].x);
            FLOAT_BUFFER.put(VERTICES[i].y);
            FLOAT_BUFFER.put(uvs[i].x);
            FLOAT_BUFFER.put(uvs[i].y);
        }
        FLOAT_BUFFER.flip();

        if (FLOAT_BUFFER.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, FLOAT_BUFFER);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);

            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv 

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return true;
    }

    @Override
    public boolean bufferIndices() {
        int someSize = INDICES.length;
        if (intBuffer == null || intBuffer.capacity() == 0) {
            intBuffer = MemoryUtil.memCallocInt(someSize);
        } else if (intBuffer.capacity() != 0 && intBuffer.capacity() < someSize) {
            intBuffer = MemoryUtil.memRealloc(intBuffer, someSize);
        }
        intBuffer.position(0);
        intBuffer.limit(someSize);

        if (intBuffer.capacity() != 0 && MemoryUtil.memAddressSafe(intBuffer) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }
        for (int i : INDICES) {
            intBuffer.put(i);
        }
        intBuffer.flip();

        if (ibo == 0) {
            ibo = GL15.glGenBuffers();
        }

        if (intBuffer.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, intBuffer, GL15.GL_STATIC_DRAW);
        }
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        return true;
    }

    @Override
    public boolean subBufferIndices() {
        if (ibo == 0) {
            DSLogger.reportError("Index buffer object is zero!", null);
            return false;
        }
        int someSize = INDICES.length;
        if (intBuffer == null || intBuffer.capacity() == 0) {
            intBuffer = MemoryUtil.memCallocInt(someSize);
        } else if (intBuffer.capacity() != 0 && intBuffer.capacity() < someSize) {
            intBuffer = MemoryUtil.memRealloc(intBuffer, someSize);
        }
        intBuffer.position(0);
        intBuffer.limit(someSize);
        if (intBuffer.capacity() != 0 && MemoryUtil.memAddressSafe(intBuffer) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }
        for (int i : INDICES) {
            intBuffer.put(i);
        }
        intBuffer.flip();

        if (intBuffer.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, intBuffer);
        }
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        return true;
    }

    @Override
    public void bufferAll(Intrface intrface) {
        buffered = bufferVertices() && bufferIndices();
    }

    @Override
    public void bufferSmart(Intrface intrface) {
        if (FLOAT_BUFFER != null && vbo != 0 && ibo != 0) {
            buffered = subBufferVertices() && subBufferIndices();
        } else {
            buffered = bufferVertices() && bufferIndices();
        }
    }

    protected Matrix4f calcModelMatrix(Intrface intrface) {
        Matrix4f translationMatrix = new Matrix4f().setTranslation(pos.x, pos.y, 0.0f);
        Matrix4f rotationMatrix = new Matrix4f().identity();

        float sx = giveRelativeWidth(intrface);
        float sy = giveRelativeHeight(intrface);
        Matrix4f scaleMatrix = new Matrix4f().scaleXY(sx, sy).scale(scale);

        Matrix4f temp = new Matrix4f();
        Matrix4f modelMatrix = translationMatrix.mul(rotationMatrix.mul(scaleMatrix, temp), temp);

        return modelMatrix;
    }

    @Override
    public void render(Intrface intrface, ShaderProgram shaderProgram) { // used for crosshair
        if (enabled && buffered) {
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);

            shaderProgram.bind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv 

            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "uv");

            Matrix4f modelMatrix = (intrface != null) ? calcModelMatrix(intrface) : new Matrix4f().identity();
            shaderProgram.updateUniform(modelMatrix, "modelMatrix");

            shaderProgram.updateUniform(scale, "scale");
            shaderProgram.updateUniform(color, "color");
            texture.bind(0, shaderProgram, "ifcTexture");
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GL11.glDrawElements(GL11.GL_TRIANGLES, INDICES.length, GL11.GL_UNSIGNED_INT, 0);

            Texture.unbind(0);
            ShaderProgram.unbind();

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
        }
    }

    @Override
    public void release() {
        if (vbo != 0) {
            GL20.glDeleteBuffers(vbo);
        }
        if (ibo != 0) {
            GL20.glDeleteBuffers(ibo);
        }

        if (FLOAT_BUFFER != null && FLOAT_BUFFER.capacity() != 0) {
            MemoryUtil.memFree(FLOAT_BUFFER);
            FLOAT_BUFFER = null;
        }

        if (intBuffer != null && intBuffer.capacity() != 0) {
            MemoryUtil.memFree(intBuffer);
            intBuffer = null;
        }
    }

    @Deprecated
    public static void globlRelease() {
        if (FLOAT_BUFFER != null && FLOAT_BUFFER.capacity() != 0 && MemoryUtil.memAddressSafe(FLOAT_BUFFER) != MemoryUtil.NULL) {
            MemoryUtil.memFree(FLOAT_BUFFER);
        }
        if (intBuffer != null && intBuffer.capacity() != 0 && MemoryUtil.memAddressSafe(intBuffer) != MemoryUtil.NULL) {
            MemoryUtil.memFree(intBuffer);
        }
    }

    public float giveRelativeWidth(Intrface intrface) {
        float widthFactor = (ignoreFactor) ? 1.0f : intrface.gameObject.WINDOW.getWidth() / (float) Window.MIN_WIDTH;
        return width * widthFactor / (float) intrface.gameObject.WINDOW.getWidth();
    }

    public float giveRelativeHeight(Intrface intrface) {
        float heightFactor = (ignoreFactor) ? 1.0f : intrface.gameObject.WINDOW.getHeight() / (float) Window.MIN_HEIGHT;
        return height * heightFactor / (float) intrface.gameObject.WINDOW.getHeight();
    }

    /**
     * Animate2 (Subwater for instance)
     *
     * @param ifc Interface where is rendered.
     */
    public void triangSwap2(Intrface ifc) {
        int length = INDICES.length;
        for (int i = 0; i < length; i += 2) {
            // Get the UV coordinates for the vertices
            Vector2f a = uvs[INDICES[i]];
            Vector2f b = uvs[INDICES[i + 1]];

            // Swap the UV coordinates
            Vector2f temp = new Vector2f(a);
            a.set(b);
            b.set(temp);
        }

        // Update the buffer
        bufferSmart(ifc);
    }

    /**
     * Animate3 (Subwater for instance). Derived from animated meshes for water.
     *
     * @param ifc Interface where is rendered.
     */
    public void triangSwap3(Intrface ifc) {
        int length = INDICES.length;
        for (int i = 0; i < length; i += 3) {
            // Get the UV coordinates for the vertices
            Vector2f a = uvs[INDICES[i]];
            Vector2f b = uvs[INDICES[i + 1]];
            Vector2f c = uvs[INDICES[i + 2]];

            // Swap the UV coordinates
            Vector2f temp = new Vector2f(c);
            c.set(b);
            b.set(a);
            a.set(temp);
        }

        // Update the buffer
        bufferSmart(ifc);
    }

    @Override
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    @Override
    public Vector4f getColor() {
        return color;
    }

    @Override
    public void setColor(Vector4f color) {
        this.color = color;
    }

    @Override
    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    @Override
    public Vector2f getPos() {
        return pos;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIgnoreFactor() {
        return ignoreFactor;
    }

    public void setIgnoreFactor(boolean ignoreFactor) {
        this.ignoreFactor = ignoreFactor;
    }

    @Override
    public int getVbo() {
        return vbo;
    }

    public static Vector2f[] getVERTICES() {
        return VERTICES;
    }

    @Override
    public boolean isBuffered() {
        return buffered;
    }

    public Vector2f[] getUvs() {
        return uvs;
    }

    @Override
    public void setPos(Vector2f pos) {
        this.pos = pos;
//        calcModelMatrix();
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    @Override
    public void unbuffer() {
        buffered = false;
    }

    @Override
    public int getIbo() {
        return ibo;
    }

}
