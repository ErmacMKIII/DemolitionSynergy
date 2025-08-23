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
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.GameTime;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 * Dynamic Text as upgraded component of the interface. Contains text. Renders
 * string to the screen. Renders whole text at once. Widely used. Replaces
 * standard Text class.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class DynamicText extends Text {

    public static final int DYNAMIC_INCREMENT = Configuration.getInstance().getTextDynamicSize();

//    protected int dynamicSize = DYNAMIC_INCREMENT;
    protected int bigVbo = 0; // vbo containing all the quads (characters)
    protected static FloatBuffer bigFloatBuff = null;

    public DynamicText(Texture texture, String content, Intrface ifc) {
        super(texture, content, ifc);
    }

    public DynamicText(Texture texture, String content, Vector4f color, Vector2f pos, Intrface ifc) {
        super(texture, content, color, pos, ifc);
    }

    public DynamicText(Texture texture, String content, Vector2f pos, int charWidth, int charHeight, Intrface ifc) {
        super(texture, content, pos, charWidth, charHeight, ifc);
    }

    @Override
    public boolean bufferVertices() {
        int someSize = txtChList.size() * Quad.VERTEX_COUNT * Quad.VERTEX_SIZE;
        if (bigFloatBuff == null || bigFloatBuff.capacity() == 0) {
            bigFloatBuff = MemoryUtil.memCallocFloat(someSize);
        } else if (bigFloatBuff.capacity() != 0 && bigFloatBuff.capacity() < someSize) {
            bigFloatBuff = MemoryUtil.memRealloc(bigFloatBuff, someSize);
        }

        if (!txtChList.isEmpty()) {
            bigFloatBuff.position(0);
            bigFloatBuff.limit(someSize);
        }

        if (bigFloatBuff.capacity() != 0 && MemoryUtil.memAddressSafe(bigFloatBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        for (TextCharacter txtCh : txtChList) {
            for (int v = 0; v < 4; v++) {
                bigFloatBuff.put(VERTICES[v].x);
                bigFloatBuff.put(VERTICES[v].y);
                bigFloatBuff.put(txtCh.uvs[v].x);
                bigFloatBuff.put(txtCh.uvs[v].y);
            }
        }

        if (bigFloatBuff.position() != 0) {
            bigFloatBuff.flip();
        }

        if (bigVbo == 0) {
            bigVbo = GL15.glGenBuffers();
        }

        if (bigFloatBuff.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bigFloatBuff, GL15.GL_STATIC_DRAW);

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
        if (bigVbo == 0) {
            DSLogger.reportError("Vertex buffer object is zero!", null);
            throw new RuntimeException("Vertex buffer object is zero!");
        }

        // auto adjust dynamic size of float buff and do it on every 1024 element
        int someSize = txtChList.size() * Quad.VERTEX_COUNT * Quad.VERTEX_SIZE;
        if (bigFloatBuff == null || bigFloatBuff.capacity() == 0) {
            bigFloatBuff = MemoryUtil.memCallocFloat(someSize);
        } else if (bigFloatBuff.capacity() != 0 && bigFloatBuff.capacity() < someSize) {
            bigFloatBuff = MemoryUtil.memRealloc(bigFloatBuff, someSize);
        }

        if (!txtChList.isEmpty()) {
            bigFloatBuff.position(0);
            bigFloatBuff.limit(someSize);
        }

        if (bigFloatBuff.capacity() != 0 && MemoryUtil.memAddressSafe(bigFloatBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        for (TextCharacter txtCh : txtChList) {
            for (int v = 0; v < 4; v++) {
                bigFloatBuff.put(VERTICES[v].x);
                bigFloatBuff.put(VERTICES[v].y);
                bigFloatBuff.put(txtCh.uvs[v].x);
                bigFloatBuff.put(txtCh.uvs[v].y);
            }
        }

        if (bigFloatBuff.position() != 0) {
            bigFloatBuff.flip();
        }

        if (bigFloatBuff.capacity() != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, bigFloatBuff);

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
    public void bufferAll(Intrface intrface) {
        buffered = false;
        setup(intrface);
        buffered = bufferVertices() && bufferIndices();
    }

    @Override
    public void bufferSmart(Intrface intrface) {
        int deltaSize = setup(intrface);
        if (bigFloatBuff != null && bigVbo != 0 && deltaSize == 0 && ibo != 0) {
            buffered = subBufferVertices() && subBufferIndices();
        } else {
            buffered = bufferVertices() && bufferIndices();
        }
    }

    @Override
    public void render(Intrface intrface, ShaderProgram shaderProgram) {
        if (enabled && buffered && !txtChList.isEmpty()) {
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);

            shaderProgram.bind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv 
            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "uv");
            shaderProgram.updateUniform(color, "color");
            texture.bind(0, shaderProgram, "ifcTexture");

            int index = 0;
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            for (TextCharacter txtCh : txtChList) {
                shaderProgram.updateUniform(txtCh.modelMat4, "modelMatrix");

                GL32.glDrawElementsBaseVertex(
                        GL11.GL_TRIANGLES,
                        INDICES.length,
                        GL11.GL_UNSIGNED_INT,
                        0,
                        index++ << 2 // vertex size is 4
                );
            }

            Texture.unbind(0);
            ShaderProgram.unbind();

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
        }
    }

    @Override
    public void renderContour(Intrface intrface, ShaderProgram contourShaderProgram) {
        if (enabled && buffered && !txtChList.isEmpty()) {
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);

            contourShaderProgram.bind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv 
            contourShaderProgram.bindAttribute(0, "pos");
            contourShaderProgram.bindAttribute(1, "uv");

            // Update uniforms
            contourShaderProgram.updateUniform(1.0f / (float) Texture.TEX_SIZE, "unit");
            contourShaderProgram.updateUniform((float) GameTime.Now().getTime(), "gameTime");

            contourShaderProgram.updateUniform(color, "color");
            texture.bind(0, contourShaderProgram, "ifcTexture");

            int index = 0;
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            for (TextCharacter txtCh : txtChList) {
                contourShaderProgram.updateUniform(txtCh.modelMat4, "modelMatrix");

                GL32.glDrawElementsBaseVertex(
                        GL11.GL_TRIANGLES,
                        INDICES.length,
                        GL11.GL_UNSIGNED_INT,
                        0,
                        index++ << 2 // vertex size is 4
                );
            }

            Texture.unbind(0);
            ShaderProgram.unbind();

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
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

    @Override
    public void release() {
        super.release();
        if (bigFloatBuff != null && bigFloatBuff.capacity() != 0) {
            MemoryUtil.memFree(bigFloatBuff);
            bigFloatBuff = null;
        }
    }

    public int getBigVbo() {
        return bigVbo;
    }

    public FloatBuffer getBigFloatBuff() {
        return bigFloatBuff;
    }

}
