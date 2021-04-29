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
package rs.alexanderstojanovich.evg.models;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Tuple { // tuple is distinct rendering object for instanced rendering
    // all blocks in the tuple have the same properties, 
    // like model matrices, color and texture name, and enabled faces in 6-bit represenation

    public static final int VEC3_SIZE = 3;
    public static final int MAT4_SIZE = 16;

    private final Blocks blocks = new Blocks();

    private int vec3Vbo = 0;
    private FloatBuffer vec3FloatBuff;

    private int mat4Vbo = 0;
    private FloatBuffer mat4FloatBuff;

    private final String texName;
    private final int faceEnBits;

    private final IntBuffer intBuff;

    public Tuple(String texName, int faceEnBits) {
        this.texName = texName;
        this.faceEnBits = faceEnBits;
        this.intBuff = Block.createIntBuffer(faceEnBits);
    }

    // buffering colors
    public void bufferVectors() {
        if (vec3FloatBuff == null || vec3FloatBuff.capacity() / VEC3_SIZE <= blocks.getDynamicSize()) {
            vec3FloatBuff = BufferUtils.createFloatBuffer(blocks.getDynamicSize() * VEC3_SIZE);
        }
        vec3FloatBuff.clear();

        for (Block block : blocks.getBlockList()) {
            Vector3f color = block.getPrimaryColor();
            vec3FloatBuff.put(color.x);
            vec3FloatBuff.put(color.y);
            vec3FloatBuff.put(color.z);
        }
        vec3FloatBuff.flip();

        if (vec3Vbo == 0) {
            vec3Vbo = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vec3Vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vec3FloatBuff, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    // buffering model matrices
    public void bufferMatrices() {
        if (mat4FloatBuff == null || mat4FloatBuff.capacity() / MAT4_SIZE <= blocks.getDynamicSize()) {
            mat4FloatBuff = BufferUtils.createFloatBuffer(blocks.getDynamicSize() * MAT4_SIZE);
        }
        mat4FloatBuff.clear();

        for (Block block : blocks.getBlockList()) {
            block.calcModelMatrix();
            Vector4f[] vectArr = new Vector4f[4];
            for (int i = 0; i < 4; i++) {
                vectArr[i] = new Vector4f();
                Matrix4f modelMatrix = block.calcModelMatrix();
                modelMatrix.getColumn(i, vectArr[i]);
                mat4FloatBuff.put(vectArr[i].x);
                mat4FloatBuff.put(vectArr[i].y);
                mat4FloatBuff.put(vectArr[i].z);
                mat4FloatBuff.put(vectArr[i].w);
            }
        }
        mat4FloatBuff.flip();

        if (mat4Vbo == 0) {
            mat4Vbo = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mat4Vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mat4FloatBuff, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    // renderer does this stuff prior to any rendering
    public synchronized void buffer() {
        blocks.bufferVertices();
        bufferVectors();
        bufferMatrices();
    }

    @Deprecated
    public synchronized void release() {
        GL15.glDeleteBuffers(blocks.getBigVbo());
        GL15.glDeleteBuffers(vec3Vbo);
        GL15.glDeleteBuffers(mat4Vbo);
    }

    public synchronized void render(ShaderProgram shaderProgram, boolean solid, Vector3f lightSrc, Texture waterTexture) {
        // if tuple has any blocks to be rendered and
        // if face bits are greater than zero, i.e. tuple has something to be rendered
        if (!blocks.getBlockList().isEmpty() && faceEnBits > 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, blocks.getBigVbo());
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // this is for pos            
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // this is for normal                                        
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // this is for uv 

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vec3Vbo);
            GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, VEC3_SIZE * 4, 0); // this is for color
            GL33.glVertexAttribDivisor(3, 1);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mat4Vbo);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 0); // this is for column0
            GL20.glVertexAttribPointer(5, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 16); // this is for column1
            GL20.glVertexAttribPointer(6, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 32); // this is for column2
            GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, MAT4_SIZE * 4, 48); // this is for column3                       

            GL33.glVertexAttribDivisor(4, 1);
            GL33.glVertexAttribDivisor(5, 1);
            GL33.glVertexAttribDivisor(6, 1);
            GL33.glVertexAttribDivisor(7, 1);

            shaderProgram.bind();

            shaderProgram.updateUniform(lightSrc, "modelLight");

            shaderProgram.updateUniform(solid ? 1.0f : 0.5f, "modelAlpha");

            Texture blocksTexture = Texture.TEX_MAP.getOrDefault(texName, Texture.QMARK);
            if (blocksTexture != null) {
                blocksTexture.bind(0, shaderProgram, "modelTexture0");
            }

            if (waterTexture != null && Game.isWaterEffects()) {
                shaderProgram.updateUniform(new Vector3f(1.0f, 1.0f, 1.0f), "modelColor1");
                waterTexture.bind(1, shaderProgram, "modelTexture1");
            }

            GL32.glDrawElementsInstancedBaseVertex(
                    GL11.GL_TRIANGLES,
                    intBuff,
                    blocks.getBlockList().size(),
                    0
            );

            Texture.unbind(0);
            Texture.unbind(1);

            ShaderProgram.unbind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

    public Blocks getBlocks() {
        return blocks;
    }

    public int getVec3Vbo() {
        return vec3Vbo;
    }

    public int getMat4Vbo() {
        return mat4Vbo;
    }

    public String getTexName() {
        return texName;
    }

    public int getFaceEnBits() {
        return faceEnBits;
    }

    public FloatBuffer getVec3FloatBuff() {
        return vec3FloatBuff;
    }

    public FloatBuffer getMat4FloatBuff() {
        return mat4FloatBuff;
    }

    public IntBuffer getIntBuff() {
        return intBuff;
    }

}
