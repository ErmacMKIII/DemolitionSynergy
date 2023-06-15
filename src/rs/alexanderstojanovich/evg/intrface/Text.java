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
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 * Text component of the interface. Contains text. Renders string to the screen.
 * Widely used.
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
    protected final IList<TextCharacter> txtChList = new GapList<>();

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

    protected int vao = 0;

    protected static FloatBuffer floatBuffer = null;
    protected int vbo = 0;

    protected static final int[] INDICES = {0, 1, 2, 2, 3, 0};
    protected static IntBuffer intBuffer = null;
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
        this.pos = pos;
        this.enabled = true;
        this.charWidth = charWidth;
        this.charHeight = charHeight;
    }

    @Override
    public boolean bufferVertices() {
        floatBuffer = MemoryUtil.memCallocFloat(Quad.VERTEX_COUNT * VERTEX_SIZE);
        if (floatBuffer.capacity() != 0 && MemoryUtil.memAddressSafe(floatBuffer) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            return false;
        }
        for (int i = 0; i < 4; i++) {
            floatBuffer.put(VERTICES[i].x);
            floatBuffer.put(VERTICES[i].y);
            floatBuffer.put(uvs[i].x);
            floatBuffer.put(uvs[i].y);
        }
        floatBuffer.flip();

        if (vao == 0) {
            vao = GL30.glGenVertexArrays();
        }

        if (vbo == 0) {
            vbo = GL15.glGenBuffers();
        }

        if (floatBuffer.capacity() != 0) {
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, floatBuffer, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);

            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv   

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL30.glBindVertexArray(0);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (floatBuffer.capacity() != 0) {
            MemoryUtil.memFree(floatBuffer);
        }

        return true;
    }

    @Override
    public boolean updateVertices() {
        if (vao == 0) {
            DSLogger.reportError("Vertex array object is zero!", null);
            return false;
        }
        if (vbo == 0) {
            DSLogger.reportError("Vertex buffer object is zero!", null);
            return false;
        }
        floatBuffer = MemoryUtil.memCallocFloat(Quad.VERTEX_COUNT * VERTEX_SIZE);
        if (floatBuffer.capacity() != 0 && MemoryUtil.memAddressSafe(floatBuffer) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            return false;
        }
        for (int i = 0; i < 4; i++) {
            floatBuffer.put(VERTICES[i].x);
            floatBuffer.put(VERTICES[i].y);
            floatBuffer.put(uvs[i].x);
            floatBuffer.put(uvs[i].y);
        }
        floatBuffer.flip();

        if (floatBuffer.capacity() != 0) {
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, floatBuffer);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);

            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv            

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL30.glBindVertexArray(0);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (floatBuffer.capacity() != 0) {
            MemoryUtil.memFree(floatBuffer);
        }

        return true;
    }

    @Override
    public boolean bufferIndices() {
        intBuffer = MemoryUtil.memCallocInt(INDICES.length);
        if (intBuffer.capacity() != 0 && MemoryUtil.memAddressSafe(intBuffer) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            return false;
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

        if (intBuffer.capacity() != 0) {
            MemoryUtil.memFree(intBuffer);
        }

        return true;
    }

    @Override
    public boolean updateIndices() {
        if (ibo == 0) {
            DSLogger.reportError("Index buffer object is zero!", null);
            return false;
        }
        intBuffer = MemoryUtil.memCallocInt(INDICES.length);
        if (intBuffer.capacity() != 0 && MemoryUtil.memAddressSafe(intBuffer) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            return false;
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

        if (intBuffer.capacity() != 0) {
            MemoryUtil.memFree(intBuffer);
        }

        return true;
    }

    /**
     * Character size difference (now & before)
     *
     * @return char difference
     */
    protected int setup() {
        int prevSize = txtChList.size();
        txtChList.clear();
        final int lineSize = Math.round(relativeLineSize());
        String[] lines = content.split("\n");
        for (int l = 0; l < lines.length; l++) {
            for (int i = 0; i < lines[l].length(); i++) {
                int j = i % lineSize;
                int k = i / lineSize;
                char ch = lines[l].charAt(i);
                float xinc = Math.round(j - content.length() * alignment) * scale * getRelativeCharWidth();
                float ydec = (k + l) * scale * getRelativeCharHeight() * Text.LINE_SPACING;

                TextCharacter txtCh = new TextCharacter(xinc, ydec, ch);
                txtChList.add(txtCh);
            }
        }
        int thisSize = txtChList.size();
        return thisSize - prevSize;
    }

    @Override
    public void bufferAll() {
        buffered = false;
        setup();
        buffered = bufferVertices() && bufferIndices();
    }

    @Override
    public void bufferSmart() {
        int deltaSize = setup();
        if (floatBuffer != null && vbo != 0 && deltaSize == 0 && ibo != 0) {
            buffered = updateVertices() && updateIndices();
        } else {
            buffered = bufferVertices() && bufferIndices();
        }
    }

    @Override
    public void unbuffer() {
        buffered = false;
    }

    @Override
    public void render(ShaderProgram shaderProgram) {
        if (enabled && buffered && !txtChList.isEmpty()) {
            GL30.glBindVertexArray(vao);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);

            shaderProgram.bind();
            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "uv");

            shaderProgram.updateUniform(scale, "scale");
            shaderProgram.updateUniform(color, "color");
            texture.bind(0, shaderProgram, "ifcTexture");

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            for (TextCharacter txtCh : txtChList) {
                uvs = txtCh.uvs;

                Matrix4f modelMatrix = calcModelMatrix(txtCh.xadv, txtCh.ydrop);
                shaderProgram.updateUniform(modelMatrix, "modelMatrix");
                GL11.glDrawElements(GL11.GL_TRIANGLES, INDICES.length, GL11.GL_UNSIGNED_INT, 0);
            }

            Texture.unbind(0);
            ShaderProgram.unbind();

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);

            GL30.glBindVertexArray(0);
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
    }

    @Deprecated
    public static void globlRelease() {
        if (floatBuffer != null && floatBuffer.capacity() != 0 && MemoryUtil.memAddressSafe(floatBuffer) != MemoryUtil.NULL) {
            MemoryUtil.memFree(floatBuffer);
        }

        if (intBuffer != null && intBuffer.capacity() != 0 && MemoryUtil.memAddressSafe(intBuffer) != MemoryUtil.NULL) {
            MemoryUtil.memFree(floatBuffer);
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

    public float getRelativeHeight() {
        float heightFactor = (ignoreFactor) ? 1.0f : GameObject.MY_WINDOW.getHeight() / (float) Window.MIN_HEIGHT;
        return charHeight * heightFactor * numberOfLines() / (float) GameObject.MY_WINDOW.getHeight();
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

    protected Matrix4f calcModelMatrix(TextCharacter txtCh) {
        Matrix4f translationMatrix = new Matrix4f().setTranslation(pos.x + txtCh.xadv, pos.y - txtCh.ydrop, 0.0f);
        Matrix4f rotationMatrix = new Matrix4f().identity();

        float sx = getRelativeCharWidth();
        float sy = getRelativeCharHeight();
        Matrix4f scaleMatrix = new Matrix4f().scaleXY(sx, sy).scale(scale);

        Matrix4f temp = new Matrix4f();
        Matrix4f modelMatrix = translationMatrix.mul(rotationMatrix.mul(scaleMatrix, temp), temp);
        return modelMatrix;
    }

    public int numberOfLines() {
        final int lineSize = Math.round(relativeLineSize());
        int numOfLines = 0;
        numOfLines += content.codePoints().filter(ch -> (char) ch == '\n').count(); // #include all '\n'
        String[] parts = content.split("\n", -1);
        for (String part : parts) {
            numOfLines += part.length() / lineSize; // #include all word wraps
        }

        return 1 + numOfLines; // #lines start with 1
    }

    public float relativeLineSize() {
        return 2.0f / (float) getRelativeCharWidth();
    }

    public IList<TextCharacter> getTxtChList() {
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

    public void setContent(String content) {
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
