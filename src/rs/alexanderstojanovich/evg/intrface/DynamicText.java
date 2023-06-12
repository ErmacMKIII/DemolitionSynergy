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
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 * Dynamic Text as upgraded component of the interface. Contains text. Renders
 * string to the screen. Renders whole text at once. Widely used. Replaces
 * standard Text class.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class DynamicText extends Text {

    public static final int DYNAMIC_INCREMENT = Configuration.getInstance().getTextDynamicSize();

//    protected int dynamicSize = DYNAMIC_INCREMENT;
    protected int bigVbo = 0; // vbo containing all the quads (characters)
    protected int[] vboEntries = new int[1024];
    protected static FloatBuffer bigFloatBuff = null;

    public DynamicText(Texture texture, String content) {
        super(texture, content);
    }

    public DynamicText(Texture texture, String content, Vector3f color, Vector2f pos) {
        super(texture, content, color, pos);
    }

    public DynamicText(Texture texture, String content, Vector2f pos, int charWidth, int charHeight) {
        super(texture, content, pos, charWidth, charHeight);
    }

    @Override
    public boolean bufferVertices() {
        bigFloatBuff = MemoryUtil.memCallocFloat(txtChList.size() * Quad.VERTEX_COUNT * Quad.VERTEX_SIZE);
        if (bigFloatBuff.capacity() != 0 && MemoryUtil.memAddressSafe(bigFloatBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            return false;
        }

        int e = 0;
        int offset = 0;
        for (TextCharacter txtCh : txtChList) {
            vboEntries[e++] = offset;
            for (int v = 0; v < 4; v++) {
                bigFloatBuff.put(VERTICES[v].x);
                bigFloatBuff.put(VERTICES[v].y);
                bigFloatBuff.put(txtCh.uvs[v].x);
                bigFloatBuff.put(txtCh.uvs[v].y);
                offset++;
            }
        }

        if (bigFloatBuff.position() != 0) {
            bigFloatBuff.flip();
        }

        if (vao == 0) {
            vao = GL30.glGenVertexArrays();
        }

        if (bigVbo == 0) {
            bigVbo = GL15.glGenBuffers();
        }

        if (bigFloatBuff.capacity() != 0) {
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bigFloatBuff, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);

            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL30.glBindVertexArray(0);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (bigFloatBuff.capacity() != 0) {
            MemoryUtil.memFree(bigFloatBuff);
        }

        return true;
    }

    @Override
    public boolean updateVertices() {
        if (vao == 0) {
            DSLogger.reportError("Vertex array object is zero!", null);
            return false;
        }

        if (bigVbo == 0) {
            DSLogger.reportError("Vertex buffer object is zero!", null);
            return false;
        }

        // auto adjust dynamic size of float buff and do it on every 1024 element
        bigFloatBuff = MemoryUtil.memCallocFloat(txtChList.size() * Quad.VERTEX_COUNT * Quad.VERTEX_SIZE);
        if (bigFloatBuff.capacity() != 0 && MemoryUtil.memAddressSafe(bigFloatBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            return false;
        }

        int e = 0;
        int offset = 0;
        for (TextCharacter txtCh : txtChList) {
            vboEntries[e++] = offset;
            for (int v = 0; v < 4; v++) {
                bigFloatBuff.put(VERTICES[v].x);
                bigFloatBuff.put(VERTICES[v].y);
                bigFloatBuff.put(txtCh.uvs[v].x);
                bigFloatBuff.put(txtCh.uvs[v].y);
                offset++;
            }
        }

        if (bigFloatBuff.position() != 0) {
            bigFloatBuff.flip();
        }

        if (bigFloatBuff.capacity() != 0) {
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, bigFloatBuff);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);

            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);

            GL30.glBindVertexArray(0);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (bigFloatBuff.capacity() != 0) {
            MemoryUtil.memFree(bigFloatBuff);
        }

        return true;
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

    @Override
    public void bufferAll() {
        setup();
        buffered = bufferVertices() && bufferIndices();
    }

    @Override
    public void bufferSmart() {
        int deltaSize = setup();
        if (bigFloatBuff != null && bigVbo != 0 && deltaSize == 0) {
            buffered = updateVertices() && bufferIndices();
        } else {
            buffered = bufferVertices() && bufferIndices();
        }
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
            shaderProgram.updateUniform(color, "color");
            texture.bind(0, ShaderProgram.getIntrfaceShader(), "ifcTexture");

            int index = 0;
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            for (TextCharacter txtCh : txtChList) {
                Matrix4f modelMat4 = calcModelMatrix(txtCh);
                ShaderProgram.getIntrfaceShader().updateUniform(modelMat4, "modelMatrix");

                GL32.glDrawElementsBaseVertex(
                        GL11.GL_TRIANGLES,
                        INDICES.length,
                        GL11.GL_UNSIGNED_INT,
                        0,
                        vboEntries[index++]
                );
            }

            Texture.unbind(0);
            ShaderProgram.unbind();

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);

            GL30.glBindVertexArray(0);
        }
    }

    @Deprecated
    public static void globlRelease() {
        if (floatBuffer != null && floatBuffer.capacity() != 0 && MemoryUtil.memAddressSafe(floatBuffer) != MemoryUtil.NULL) {
            MemoryUtil.memFree(floatBuffer);
        }

        if (bigFloatBuff != null && bigFloatBuff.capacity() != 0 && MemoryUtil.memAddressSafe(bigFloatBuff) != MemoryUtil.NULL) {
            MemoryUtil.memFree(bigFloatBuff);
        }

        if (intBuffer != null && intBuffer.capacity() != 0 && MemoryUtil.memAddressSafe(intBuffer) != MemoryUtil.NULL) {
            MemoryUtil.memFree(floatBuffer);
        }
    }

    public int getBigVbo() {
        return bigVbo;
    }

//    public int getDynamicSize() {
//        return dynamicSize;
//    }
    public int[] getVboEntries() {
        return vboEntries;
    }

    public FloatBuffer getBigFloatBuff() {
        return bigFloatBuff;
    }

}
