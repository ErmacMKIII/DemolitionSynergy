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
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Quad implements ComponentIfc {

    protected int width;
    protected int height;
    protected Texture texture;

    protected Vector3f color = Vector3fColors.WHITE;
    protected float scale = 1.0f;

    protected Vector2f pos = new Vector2f();
    protected boolean enabled = true;

    protected boolean ignoreFactor = false;

    protected static final Vector2f[] VERTICES = new Vector2f[4];
    protected final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(4 * VERTEX_SIZE);

    protected Vector2f[] uvs = new Vector2f[4];
    protected static final int[] INDICES = {0, 1, 2, 2, 3, 0};
    // protected static final IntBuffer CONST_INT_BUFFER = BufferUtils.createIntBuffer(6);
    protected int vbo = 0;

    protected final IntBuffer intBuffer = BufferUtils.createIntBuffer(4 * VERTEX_SIZE);
    protected int ibo = 0;

    public static final int VERTEX_SIZE = 4;
    public static final int VERTEX_COUNT = 4;

    protected boolean buffered = false;

    protected Matrix4f modelMatrix = calcModelMatrix();

    static {
        VERTICES[0] = new Vector2f(-1.0f, -1.0f);
        VERTICES[1] = new Vector2f(1.0f, -1.0f);
        VERTICES[2] = new Vector2f(1.0f, 1.0f);
        VERTICES[3] = new Vector2f(-1.0f, 1.0f);

//        for (int i : INDICES) {
//            CONST_INT_BUFFER.put(i);
//        }
//        CONST_INT_BUFFER.flip();
    }

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
    public void bufferVertices() {
        floatBuffer.clear();
        for (int i = 0; i < 4; i++) {
            floatBuffer.put(VERTICES[i].x);
            floatBuffer.put(VERTICES[i].y);
            floatBuffer.put(uvs[i].x);
            floatBuffer.put(uvs[i].y);
        }
        floatBuffer.flip();
        if (vbo == 0) {
            vbo = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, floatBuffer, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        buffered = true;
    }

    @Override
    public void updateVertices() {
        floatBuffer.clear();
        for (int i = 0; i < 4; i++) {
            floatBuffer.put(VERTICES[i].x);
            floatBuffer.put(VERTICES[i].y);
            floatBuffer.put(uvs[i].x);
            floatBuffer.put(uvs[i].y);
        }
        floatBuffer.flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, floatBuffer);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void bufferIndices() {
        intBuffer.clear();
        for (int i : INDICES) {
            intBuffer.put(i);
        }
        intBuffer.flip();

        if (ibo == 0) {
            ibo = GL15.glGenBuffers();
        }

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, intBuffer, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    @Override
    public void bufferAll() {
        bufferVertices();
        bufferIndices();
        buffered = true;
    }

    @Override
    public void bufferSmart() {
        updateVertices();
        bufferIndices();
        buffered = true;
    }

    protected Matrix4f calcModelMatrix() {
        Matrix4f translationMatrix = new Matrix4f().setTranslation(pos.x, pos.y, 0.0f);
        Matrix4f rotationMatrix = new Matrix4f().identity();

        float sx = giveRelativeWidth();
        float sy = giveRelativeHeight();
        Matrix4f scaleMatrix = new Matrix4f().scaleXY(sx, sy).scale(scale);

        Matrix4f temp = new Matrix4f();
        modelMatrix = translationMatrix.mul(rotationMatrix.mul(scaleMatrix, temp), temp);
        return modelMatrix;
    }

    @Override
    public void render(ShaderProgram shaderProgram) { // used for crosshair
        if (enabled && buffered) {
            Texture.enable();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv                                     
            shaderProgram.bind();

            calcModelMatrix();
            shaderProgram.updateUniform(modelMatrix, "modelMatrix");

            shaderProgram.updateUniform(scale, "scale");
            shaderProgram.updateUniform(color, "color");
            texture.bind(0, shaderProgram, "ifcTexture");

            GL11.glDrawElements(GL11.GL_TRIANGLES, INDICES.length, GL11.GL_UNSIGNED_INT, 0);

            Texture.unbind(0);
            ShaderProgram.unbind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            Texture.disable();
        }
    }

    public float giveRelativeWidth() {
        float widthFactor = (ignoreFactor) ? 1.0f : GameObject.MY_WINDOW.getWidth() / (float) Window.MIN_WIDTH;
        return width * widthFactor / (float) GameObject.MY_WINDOW.getWidth();
    }

    public float giveRelativeHeight() {
        float heightFactor = (ignoreFactor) ? 1.0f : GameObject.MY_WINDOW.getHeight() / (float) Window.MIN_HEIGHT;
        return height * heightFactor / (float) GameObject.MY_WINDOW.getHeight();
    }

    public Window getWindow() {
        return GameObject.MY_WINDOW;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
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
    public Vector3f getColor() {
        return color;
    }

    @Override
    public void setColor(Vector3f color) {
        this.color = color;
    }

    public Window getMyWindow() {
        return GameObject.MY_WINDOW;
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
        calcModelMatrix();
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    @Override
    public FloatBuffer getFloatBuffer() {
        return floatBuffer;
    }

    @Override
    public Matrix4f getModelMatrix() {
        return modelMatrix;
    }

    @Override
    public void unbuffer() {
        buffered = false;
    }

    @Override
    public IntBuffer getIntBuffer() {
        return intBuffer;
    }

    @Override
    public int getIbo() {
        return ibo;
    }

}
