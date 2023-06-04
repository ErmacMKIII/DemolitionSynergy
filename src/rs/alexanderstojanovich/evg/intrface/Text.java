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
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Text implements ComponentIfc {

    public static final int VERTEX_SIZE = 4;
    public static final int VERTEX_COUNT = 4;

    public static final float ALIGNMENT_LEFT = 0.0f;
    public static final float ALIGNMENT_CENTER = 0.5f;
    public static final float ALIGNMENT_RIGHT = 1.0f;

    protected Texture texture;
    protected String content;

    protected static final int GRID_SIZE = 16;
    protected static final float CELL_SIZE = 1.0f / GRID_SIZE;
    public static final float LINE_SPACING = 1.5f;

    // first is position, second is uvs
    protected final List<TextCharacter> txtChList = new GapList<>();

    protected boolean enabled;

    public static final int STD_FONT_WIDTH = 24;
    public static final int STD_FONT_HEIGHT = 24;

    protected float alignment = ALIGNMENT_LEFT; // per character alignment

    protected boolean buffered = false;

    protected Vector2f pos = new Vector2f();
    protected float scale = 1.0f;
    protected Vector3f color = Vector3fColors.WHITE;

    protected int charWidth = STD_FONT_WIDTH;
    protected int charHeight = STD_FONT_HEIGHT;

    protected boolean ignoreFactor = false;

    protected static final Vector2f[] VERTICES = {
        new Vector2f(-1.0f, -1.0f),
        new Vector2f(1.0f, -1.0f),
        new Vector2f(1.0f, 1.0f),
        new Vector2f(-1.0f, 1.0f)
    };

    protected Vector2f[] uvs = {
        new Vector2f(),
        new Vector2f(),
        new Vector2f(),
        new Vector2f()
    };

    protected static FloatBuffer floatBuffer = MemoryUtil.memAllocFloat(Quad.VERTEX_COUNT * VERTEX_SIZE);
    protected int vbo = 0;

    protected static final int[] INDICES = {0, 1, 2, 2, 3, 0};
    protected static IntBuffer intBuffer = MemoryUtil.memAllocInt(INDICES.length);
    protected int ibo = 0;

    public Text(Texture texture, String content) {
        this.texture = texture;
        this.content = content;
        this.enabled = true;
    }

    public Text(Texture texture, String content, Vector3f color, Vector2f pos) {
        this.texture = texture;
        this.content = content;
        this.color = color;
        this.pos = pos;
        this.enabled = true;
    }

    public Text(Texture texture, String content, Vector2f pos, int charWidth, int charHeight) {
        this.texture = texture;
        this.content = content;
        this.enabled = true;
        this.charWidth = charWidth;
        this.charHeight = charHeight;
    }

    @Override
    public void bufferVertices() {
        floatBuffer = MemoryUtil.memAllocFloat(Quad.VERTEX_COUNT * VERTEX_SIZE);
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

        if (floatBuffer.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, floatBuffer, GL15.GL_STATIC_DRAW);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (floatBuffer.capacity() != 0) {
            MemoryUtil.memFree(floatBuffer);
        }
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
        intBuffer = MemoryUtil.memAllocInt(INDICES.length);
        intBuffer.clear();
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

        if (intBuffer.capacity() != 0) {
            MemoryUtil.memFree(intBuffer);
        }
    }

    protected void setup() {
        txtChList.clear();
        String[] lines = content.split("\n");
        for (int l = 0; l < lines.length; l++) {
            for (int i = 0; i < lines[l].length(); i++) {
                int j = i % 64;
                int k = i / 64;
                char ch = lines[l].charAt(i);
                float xinc = (j - content.length() * alignment) * scale * getRelativeCharWidth();
                float ydec = (k + l * LINE_SPACING) * scale * getRelativeCharHeight();

                TextCharacter txtCh = new TextCharacter(xinc, ydec, ch);
                txtChList.add(txtCh);
            }
        }
    }

    @Override
    public synchronized void bufferAll() {
        setup();
        bufferVertices();
        bufferIndices();
        buffered = true;
    }

    @Override
    public synchronized void bufferSmart() {
        setup();
        if (vbo == 0) {
            bufferVertices();
        }
        updateVertices();
        bufferIndices();
        buffered = true;
    }

    @Override
    public void unbuffer() {
        buffered = false;
    }

    @Override
    public synchronized void render(ShaderProgram shaderProgram) {
        if (enabled && buffered) {
            Texture.enable();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv                                     
            shaderProgram.bind();

            shaderProgram.updateUniform(scale, "scale");
            shaderProgram.updateUniform(color, "color");
            texture.bind(0, shaderProgram, "ifcTexture");

            for (TextCharacter txtCh : txtChList) {
                uvs = txtCh.uvs;

                Matrix4f modelMatrix = calcModelMatrix(txtCh.xadv, txtCh.ydrop);
                shaderProgram.updateUniform(modelMatrix, "modelMatrix");
                GL11.glDrawElements(GL11.GL_TRIANGLES, INDICES.length, GL11.GL_UNSIGNED_INT, 0);
            }

            Texture.unbind(0);
            ShaderProgram.unbind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            Texture.disable();
        }
    }

    @Override
    public int getWidth() {
        return charWidth * content.length();
    }

    @Override
    public int getHeight() {
        return charHeight;
    }

    public float getRelativeCharWidth() {
        float widthFactor = (ignoreFactor) ? 1.0f : GameObject.MY_WINDOW.getWidth() / (float) Window.MIN_WIDTH;
        return charWidth * widthFactor / (float) GameObject.MY_WINDOW.getWidth();
    }

    public float getRelativeWidth() {
        float widthFactor = (ignoreFactor) ? 1.0f : GameObject.MY_WINDOW.getWidth() / (float) Window.MIN_WIDTH;
        return charWidth * widthFactor * content.length() / (float) GameObject.MY_WINDOW.getWidth();
    }

    public float getRelativeCharHeight() {
        float heightFactor = (ignoreFactor) ? 1.0f : GameObject.MY_WINDOW.getHeight() / (float) Window.MIN_HEIGHT;
        return charHeight * heightFactor / (float) GameObject.MY_WINDOW.getHeight();
    }

    // it aligns position to next char position (useful if characters are cut out or so)
    // call this method only once!
    public void alignToNextChar() {
        float srw = scale * getRelativeCharWidth(); // scaled relative width
        float srh = scale * getRelativeCharHeight(); // scaled relative height                                                                 

        float xrem = pos.x % srw;
        pos.x -= (pos.x < 0.0f) ? xrem : (xrem - srw);

        float yrem = pos.y % srh;
        pos.y -= yrem;
    }

    protected Matrix4f calcModelMatrix(float xinc, float ydec) {
        Matrix4f translationMatrix = new Matrix4f().setTranslation(pos.x + xinc, pos.y - ydec, 0.0f);
        Matrix4f rotationMatrix = new Matrix4f().identity();

        float sx = getRelativeCharWidth();
        float sy = getRelativeCharHeight();
        Matrix4f scaleMatrix = new Matrix4f().scaleXY(sx, sy).scale(scale);

        Matrix4f temp = new Matrix4f();
        Matrix4f modelMatrix = translationMatrix.mul(rotationMatrix.mul(scaleMatrix, temp), temp);
        return modelMatrix;
    }

    public List<TextCharacter> getTxtChList() {
        return txtChList;
    }

    public Vector2f[] getUvs() {
        return uvs;
    }

    @Override
    public int getVbo() {
        return vbo;
    }

    @Override
    public int getIbo() {
        return ibo;
    }

    public Window getMyWindow() {
        return GameObject.MY_WINDOW;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public String getContent() {
        return content;
    }

    public synchronized void setContent(String content) {
        this.content = content;
        buffered = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float getAlignment() {
        return alignment;
    }

    public void setAlignment(float alignment) {
        this.alignment = alignment;
    }

    @Override
    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    public int getCharWidth() {
        return charWidth;
    }

    public int getCharHeight() {
        return charHeight;
    }

    public boolean isIgnoreFactor() {
        return ignoreFactor;
    }

    public void setIgnoreFactor(boolean ignoreFactor) {
        this.ignoreFactor = ignoreFactor;
    }

    @Override
    public Vector2f getPos() {
        return pos;
    }

    @Override
    public float getScale() {
        return scale;
    }

    @Override
    public Vector3f getColor() {
        return color;
    }

    @Override
    public void setColor(Vector3f color) {
        this.color = color;
    }

    @Override
    public void setPos(Vector2f pos) {
        this.pos = pos;
        buffered = false;
    }

    public void setScale(float scale) {
        this.scale = scale;
        buffered = false;
    }

}
