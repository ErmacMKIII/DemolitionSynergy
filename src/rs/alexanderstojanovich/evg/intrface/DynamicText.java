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
import org.lwjgl.opengl.GL32;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Pair;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class DynamicText extends Text {

    protected int dynamicSize = 100;
    protected int bigVbo = 0; // vbo containing all the quads (characters)
    protected int[] vboEntries = new int[1024];
    protected static final Vector2f[] VERTICES = new Vector2f[4]; //            
    protected static final int[] INDICES = {0, 1, 2, 2, 3, 0};
    protected static final IntBuffer CONST_INT_BUFFER = BufferUtils.createIntBuffer(6);
    protected FloatBuffer bigFloatBuff = BufferUtils.createFloatBuffer(dynamicSize * Quad.VERTEX_COUNT * Quad.VERTEX_SIZE);

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

    public DynamicText(Texture texture, String content) {
        super(texture, content);
    }

    public DynamicText(Texture texture, String content, Vector3f color, Vector2f pos) {
        super(texture, content, color, pos);
    }

    public DynamicText(Texture texture, String content, Vector2f pos, int charWidth, int charHeight) {
        super(texture, content, pos, charWidth, charHeight);
    }

    protected void bufferVbo() {
        // auto adjust dynamic size of float buff and do it on every 1024 element
        if (content.length() >= dynamicSize) {
            dynamicSize = content.length() + 100;
            bigFloatBuff = BufferUtils.createFloatBuffer(dynamicSize * Quad.VERTEX_COUNT * Quad.VERTEX_SIZE);
        }
        bigFloatBuff.clear();
        pairList.clear();
        String[] lines = content.split("\n");
        int e = 0;
        int offset = 0;
        for (int l = 0; l < lines.length; l++) {
            for (int i = 0; i < lines[l].length(); i++) {
                vboEntries[e++] = offset;
                int j = i % 64;
                int k = i / 64;
                int asciiCode = (int) (lines[l].charAt(i));

                float cellU = (asciiCode % GRID_SIZE) * CELL_SIZE;
                float cellV = (asciiCode / GRID_SIZE) * CELL_SIZE;

                float xinc = (j - content.length() * alignment) * scale * getRelativeCharWidth();
                float ydec = (k + l * LINE_SPACING) * scale * getRelativeCharHeight();

                pairList.add(new Pair<>(xinc, ydec));

                Vector2f[] uvs = new Vector2f[4];

                for (int v = 0; v < 4; v++) {
                    uvs[v] = new Vector2f();
                }

                uvs[0].x = cellU;
                uvs[0].y = cellV + CELL_SIZE;

                uvs[1].x = cellU + CELL_SIZE;
                uvs[1].y = cellV + CELL_SIZE;

                uvs[2].x = cellU + CELL_SIZE;
                uvs[2].y = cellV;

                uvs[3].x = cellU;
                uvs[3].y = cellV;

                for (int v = 0; v < Quad.VERTEX_COUNT; v++) {
                    bigFloatBuff.put(VERTICES[v].x);
                    bigFloatBuff.put(VERTICES[v].y);
                    bigFloatBuff.put(uvs[v].x);
                    bigFloatBuff.put(uvs[v].y);
                    offset++;
                }

            }
        }

        bigFloatBuff.flip();
        if (bigVbo == 0) {
            bigVbo = GL15.glGenBuffers();
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bigFloatBuff, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public synchronized void buffer() {
        bufferVbo();
        buffered = true;
    }

    private Matrix4f calcModelMatrix(float xinc, float ydec) {
        Matrix4f translationMatrix = new Matrix4f().setTranslation(pos.x + xinc, pos.y + ydec, 0.0f);
        Matrix4f rotationMatrix = new Matrix4f().identity();

        float sx = getRelativeCharWidth();
        float sy = getRelativeCharHeight();
        Matrix4f scaleMatrix = new Matrix4f().scaleXY(sx, sy).scale(scale);

        Matrix4f temp = new Matrix4f();
        Matrix4f modelMatrix = translationMatrix.mul(rotationMatrix.mul(scaleMatrix, temp), temp);
        return modelMatrix;
    }

    @Override
    public synchronized void render() {
        if (enabled && buffered && !content.isEmpty()) {
            Texture.enable();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv
            ShaderProgram.getIntrfaceShader().bind();
            ShaderProgram.getIntrfaceShader().updateUniform(pos, "trans");
            ShaderProgram.getIntrfaceShader().updateUniform(scale, "scale");
            ShaderProgram.getIntrfaceShader().updateUniform(color, "color");
            texture.bind(0, ShaderProgram.getIntrfaceShader(), "ifcTexture");
            for (int k = 0; k < content.length(); k++) {
                int index = vboEntries[k] >> 2;

                // again some fail safes
                if (index < 0 || index >= pairList.size()) {
                    break;
                }

                Pair<Float, Float> pair = pairList.get(index);
                float xinc = (float) pair.getKey();
                float ydec = (float) pair.getValue();

                Matrix4f projMat4 = new Matrix4f();
                ShaderProgram.getIntrfaceShader().updateUniform(projMat4, "projectionMatrix");

                Matrix4f modelMat4 = calcModelMatrix(xinc, ydec);
                ShaderProgram.getIntrfaceShader().updateUniform(modelMat4, "modelMatrix");

                GL32.glDrawElementsBaseVertex(GL11.GL_TRIANGLES, CONST_INT_BUFFER, vboEntries[k]);
            }
            Texture.unbind(0);
            ShaderProgram.unbind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            Texture.disable();
        }
    }

    @Override
    public synchronized void setContent(String content) {
        super.setContent(content);
        buffered = false;
    }

    public int getBigVbo() {
        return bigVbo;
    }

    public int getDynamicSize() {
        return dynamicSize;
    }

}
