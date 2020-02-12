/*
 * Copyright (C) 2019 Coa
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
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Coa
 */
public class Quad {

    private Window myWindow;

    private int width;
    private int height;
    private Texture texture;

    private Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
    private float scale = 1.0f;

    private Vector2f pos = new Vector2f();
    private boolean enabled = true;

    private boolean ignoreFactor = false;

    private static final Vector2f[] VERTICES = new Vector2f[4];
    private final Vector2f[] uvs = new Vector2f[4];
    private static final int[] INDICES = {0, 1, 2, 2, 3, 0};
    private static final IntBuffer CONST_INT_BUFFER = BufferUtils.createIntBuffer(6);
    private int vbo;

    public static final int VERTEX_SIZE = 4;
    public static final int VERTEX_COUNT = 4;

    private boolean buffered = false;

    static {
        VERTICES[0] = new Vector2f(-1.0f, -1.0f);
        VERTICES[1] = new Vector2f(1.0f, -1.0f);
        VERTICES[2] = new Vector2f(1.0f, 1.0f);
        VERTICES[3] = new Vector2f(-1.0f, 1.0f);

        for (int i : INDICES) {
            CONST_INT_BUFFER.put(i);
        }
        CONST_INT_BUFFER.flip();
    }

    public Quad(Window window, int width, int height, Texture texture) {
        this.myWindow = window;
        this.width = width;
        this.height = height;
        this.texture = texture;
        initUVs();
    }

    public Quad(Window window, int width, int height, Texture texture, boolean ignoreFactor) {
        this.myWindow = window;
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

    public void buffer() {
        FloatBuffer fb = BufferUtils.createFloatBuffer(4 * VERTEX_SIZE);
        for (int i = 0; i < 4; i++) {
            fb.put(VERTICES[i].x);
            fb.put(VERTICES[i].y);
            fb.put(uvs[i].x);
            fb.put(uvs[i].y);
        }
        fb.flip();
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fb, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        buffered = true;
    }

    public void render() { // used for crosshair
        if (enabled && buffered) {
            float relWidth = giveRelativeWidth();
            float relHeight = giveRelativeHeight();
            Texture.enable();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv                         
            ShaderProgram.getIntrfaceShader().bind();
            ShaderProgram.getIntrfaceShader().updateUniform(pos, "trans");
            ShaderProgram.getIntrfaceShader().updateUniform(relWidth, "width");
            ShaderProgram.getIntrfaceShader().updateUniform(relHeight, "height");
            ShaderProgram.getIntrfaceShader().updateUniform(scale, "scale");
            ShaderProgram.getIntrfaceShader().updateUniform(color, "color");
            texture.bind(0, ShaderProgram.getIntrfaceShader(), "ifcTexture");
            ShaderProgram.getIntrfaceShader().updateUniform(0.0f, "xinc");
            ShaderProgram.getIntrfaceShader().updateUniform(0.0f, "ydec");
            GL11.glDrawElements(GL11.GL_TRIANGLES, CONST_INT_BUFFER);

            Texture.unbind(0);
            ShaderProgram.unbind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            Texture.disable();
        }
    }

    public void render(float xinc, float ydec) { // used for fonts
        if (enabled && buffered) {
            float relWidth = giveRelativeWidth();
            float relHeight = giveRelativeHeight();
            Texture.enable();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv                         
            ShaderProgram.getIntrfaceShader().bind();
            ShaderProgram.getIntrfaceShader().updateUniform(pos, "trans");
            ShaderProgram.getIntrfaceShader().updateUniform(relWidth, "width");
            ShaderProgram.getIntrfaceShader().updateUniform(relHeight, "height");
            ShaderProgram.getIntrfaceShader().updateUniform(scale, "scale");
            ShaderProgram.getIntrfaceShader().updateUniform(color, "color");
            texture.bind(0, ShaderProgram.getIntrfaceShader(), "ifcTexture");
            ShaderProgram.getIntrfaceShader().updateUniform(xinc, "xinc");
            ShaderProgram.getIntrfaceShader().updateUniform(ydec, "ydec");
            GL11.glDrawElements(GL11.GL_TRIANGLES, CONST_INT_BUFFER);

            Texture.unbind(0);
            ShaderProgram.unbind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            Texture.disable();
        }
    }

    public float giveRelativeWidth() {
        float widthFactor = (ignoreFactor) ? 1.0f : myWindow.getWidth() / Window.MIN_WIDTH;
        return width * widthFactor / myWindow.getWidth();
    }

    public float giveRelativeHeight() {
        float heightFactor = (ignoreFactor) ? 1.0f : myWindow.getHeight() / Window.MIN_HEIGHT;
        return height * heightFactor / myWindow.getHeight();
    }

    public Window getWindow() {
        return myWindow;
    }

    public void setWindow(Window window) {
        this.myWindow = window;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

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

    public Vector3f getColor() {
        return color;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public Vector2f getPos() {
        return pos;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIgnoreFactor() {
        return ignoreFactor;
    }

    public void setIgnoreFactor(boolean ignoreFactor) {
        this.ignoreFactor = ignoreFactor;
    }

    public int getVbo() {
        return vbo;
    }

    public static Vector2f[] getVERTICES() {
        return VERTICES;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public Vector2f[] getUvs() {
        return uvs;
    }

    public void setPos(Vector2f pos) {
        this.pos = pos;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

}
