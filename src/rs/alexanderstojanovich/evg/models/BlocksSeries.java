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
package rs.alexanderstojanovich.evg.models;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import rs.alexanderstojanovich.evg.core.Editor;
import rs.alexanderstojanovich.evg.core.FrameBuffer;
import rs.alexanderstojanovich.evg.core.Texture;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Coa
 */
public class BlocksSeries { // mutual class made from solid and fluid blocks with instanced rendering

    public static final int VEC4_SIZE = 4;
    public static final int MAT4_SIZE = 16;
    
    private final List<Integer> mat4Vbos = new ArrayList<>(); // is for model matrix shared amongst the vertices of the same instance
    private final List<Integer> vec4Vbos = new ArrayList<>(); // this is for color shared amongst the vertices of the same instance
    private final List<Texture> blocksTextures = new ArrayList<>(); // this is for texture series;
    // it has 2^6 combinations, cuz each face can be enabled or disabled
    private final List<IntBuffer> intBuffs = new ArrayList<>();

    // array with offsets in the big float buffer
    // this is maximum amount of blocks of the type game can hold
    private final int[] vboEntries = new int[65536];
    private boolean buffered = false;
    
    private final List<Blocks> blocksSeries = new LinkedList<>();
    
    public BlocksSeries(Blocks blocks) {
        Texture currTexture = null;
        Blocks currSeries = null;        
        int currBits = -1;
        for (Block block : blocks.getBlockList()) {
            // on texture change make new series or
            // on faces bits change make new series                
            int blockFacesBits = block.getEnabledFacesBits();
            if (block.getPrimaryTexture() != currTexture
                    || currBits != blockFacesBits) {                
                currSeries = new Blocks();
                blocksSeries.add(currSeries);
                currTexture = block.getPrimaryTexture();
                currBits = blockFacesBits;
                blocksTextures.add(currTexture);

                // storing indices in the buffer
                IntBuffer intBuff = BufferUtils.createIntBuffer(block.indices.size());
                for (Integer index : block.indices) {
                    intBuff.put(index);
                }
                intBuff.flip();
                intBuffs.add(intBuff);
            }            
            
            if (currSeries != null) {
                currSeries.getBlockList().add(block);
            }
            
        }
    }
    
    public void bufferMatrices(Blocks blocks) {
        FloatBuffer mat4FloatBuff = BufferUtils.createFloatBuffer(blocks.getBlockList().size() * MAT4_SIZE);
        for (Block block : blocks.getBlockList()) {
            block.calcModelMatrix();
            Vector4f[] vectArr = new Vector4f[4];
            for (int i = 0; i < 4; i++) {
                vectArr[i] = new Vector4f();
                block.modelMatrix.getColumn(i, vectArr[i]);
                mat4FloatBuff.put(vectArr[i].x);
                mat4FloatBuff.put(vectArr[i].y);
                mat4FloatBuff.put(vectArr[i].z);
                mat4FloatBuff.put(vectArr[i].w);
            }
        }
        mat4FloatBuff.flip();
        int mat4Vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mat4Vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mat4FloatBuff, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        mat4Vbos.add(mat4Vbo);
    }
    
    public void bufferVectors(Blocks blocks) {
        FloatBuffer vec4FloatBuff = BufferUtils.createFloatBuffer(blocks.getBlockList().size() * VEC4_SIZE);
        for (Block block : blocks.getBlockList()) {
            Vector4f color = block.getPrimaryColor();
            vec4FloatBuff.put(color.x);
            vec4FloatBuff.put(color.y);
            vec4FloatBuff.put(color.z);
            vec4FloatBuff.put(color.w);
        }
        vec4FloatBuff.flip();
        int vec4Vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vec4Vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vec4FloatBuff, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        vec4Vbos.add(vec4Vbo);
    }
    
    public void bufferAll() {
        for (Blocks blocks : blocksSeries) {
            blocks.bufferVertices();
            blocks.bufferIndices();
            bufferMatrices(blocks);
            bufferVectors(blocks);
        }
        buffered = true;
    }
    
    public void animate() { // call only for fluid blocks
//        for (Blocks blocks : blocksSeries) {
//            blocks.animate();
//        }
    }
    
    public void prepare() { // call only for fluid blocks before rendering
        for (Blocks blocks : blocksSeries) {
            blocks.prepare();
        }
    }

    // it always renders all of them instanced
    public void render(ShaderProgram shaderProgram, Vector3f lightSrc) {
        if (buffered && shaderProgram != null && !blocksSeries.isEmpty()) {
            Texture.enable();
            
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);
            GL20.glEnableVertexAttribArray(3);
            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            GL20.glEnableVertexAttribArray(6);
            GL20.glEnableVertexAttribArray(7);
            
            int seriesIndex = 0;
            for (Blocks blocks : blocksSeries) {                
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, blocks.getBigVbo());
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // this is for pos            
                GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // this is for normal                                        
                GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // this is for uv 

                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vec4Vbos.get(seriesIndex));
                GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, VEC4_SIZE * 4, 0); // this is for color
                GL33.glVertexAttribDivisor(3, 1);
                
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mat4Vbos.get(seriesIndex));
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
                
                Texture blocksTexture = blocksTextures.get(seriesIndex);
                if (blocksTexture != null) {
                    blocksTexture.bind(0, shaderProgram, "modelTexture0");
                }
                
                shaderProgram.updateUniform(new Vector4f(1.0f, 1.0f, 0.0f, 1.0f), "modelColor1");
                shaderProgram.updateUniform(Editor.getSelectedCurrIndex(), "selectedIndex");
                Editor.getSELECTED_TEXTURE().bind(1, shaderProgram, "modelTexture1");
                shaderProgram.updateUniform(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), "modelColor2");
                FrameBuffer.getTexture().bind(2, shaderProgram, "modelTexture2");
                
                GL32.glDrawElementsInstancedBaseVertex(
                        GL11.GL_TRIANGLES,
                        intBuffs.get(seriesIndex),
                        blocks.getBlockList().size(),
                        vboEntries[0]
                );
                
                Texture.unbind(0);
                Texture.unbind(1);
                Texture.unbind(2);
                ShaderProgram.unbind();
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
                
                seriesIndex++;
            }
            
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
            GL20.glDisableVertexAttribArray(3);
            GL20.glDisableVertexAttribArray(4);
            GL20.glDisableVertexAttribArray(5);
            GL20.glDisableVertexAttribArray(6);
            GL20.glDisableVertexAttribArray(7);
            
            Texture.disable();
        }
    }
    
    public List<Integer> getMat4Vbos() {
        return mat4Vbos;
    }
    
    public List<Integer> getVec4Vbos() {
        return vec4Vbos;
    }
    
    public List<Blocks> getBlocksSeries() {
        return blocksSeries;
    }
    
    public int[] getVboEntries() {
        return vboEntries;
    }
    
    public boolean isBuffered() {
        return buffered;
    }
    
    public List<Texture> getBlocksTextures() {
        return blocksTextures;
    }
    
}
