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
import rs.alexanderstojanovich.evg.core.Texture;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.Tuple;

/**
 *
 * @author Coa
 */
public class BlocksSeries { // mutual class made from solid and fluid blocks with instanced rendering

    public static final int VEC4_SIZE = 4;
    public static final int MAT4_SIZE = 16;

    // array with offsets in the big float buffer
    // this is maximum amount of blocks of the type game can hold  
    private boolean buffered = false;
    // single mat4Vbo is for model matrix shared amongst the vertices of the same instance    
    // single vec4Vbo is color shared amongst the vertices of the same instance    
    //--------------------------A--------B--------C-------D--------E-----------------------------
    //------------------------blocks-mat4Vbos-vec4Vbos-texture-faceEnBits------------------------
    private final List<Tuple<Blocks, Integer, Integer, Texture, Integer>> blocksSeries = new LinkedList<>();

    private boolean cameraInFluid = false;

    // is initialization progress
    private int progress = 0;

    private Texture waterTexture;

    public BlocksSeries(Blocks blocks) {
        init(blocks);
    }

    private void init(Blocks blocks) {
        progress = 0;
        Tuple<Blocks, Integer, Integer, Texture, Integer> currTuple = null; // current processing tuple        
        for (Block block : blocks.getBlockList()) {
            Texture blockTexture = block.primaryTexture;
            Integer blockFaceBits = block.getFaceBits();

            int indexOfSeries = indexOfSeries(blockTexture, blockFaceBits);

            if (indexOfSeries == -1) {
                currTuple = new Tuple<>(new Blocks(), 0, 0, blockTexture, blockFaceBits);
                blocksSeries.add(currTuple);
            } else {
                currTuple = blocksSeries.get(indexOfSeries);
            }

            if (currTuple != null) {
                currTuple.getA().getBlockList().add(block);
            }

            progress += Math.round(1.0f / blocks.getBlockList().size());
        }
        progress = 100;
    }

    public int indexOfSeries(Texture keyTexture, Integer keyFaceBits) {
        int serIndex = 0;
        int keyIndex = -1;
        for (Tuple<Blocks, Integer, Integer, Texture, Integer> tuple : blocksSeries) {
            if (tuple.getD() != null && tuple.getD().equals(keyTexture)
                    && tuple.getE() != null && tuple.getE().equals(keyFaceBits)) {
                keyIndex = serIndex;
                break;
            }
            serIndex++;
        }
        return keyIndex;
    }

    public void bufferVectors(Blocks blocks, int seriesIndex) {
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
        blocksSeries.get(seriesIndex).setB(vec4Vbo);
    }

    public void bufferMatrices(Blocks blocks, int seriesIndex) {
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
        blocksSeries.get(seriesIndex).setC(mat4Vbo);
    }

    public void bufferAll() {
        int seriesIndex = 0;
        for (Tuple<Blocks, Integer, Integer, Texture, Integer> tuple : blocksSeries) {
            Blocks blocks = tuple.getA();
            blocks.bufferVertices();
            bufferMatrices(blocks, seriesIndex);
            bufferVectors(blocks, seriesIndex);
            seriesIndex++;
        }
        buffered = true;
    }

    public void animate() { // call only for fluid blocks
        for (Tuple<Blocks, Integer, Integer, Texture, Integer> tuple : blocksSeries) {
            tuple.getA().animate();
        }
    }

    public void prepare() { // call only for fluid blocks before rendering        
        for (Tuple<Blocks, Integer, Integer, Texture, Integer> tuple : blocksSeries) {
            Blocks blocks = tuple.getA();
            blocks.setCameraInFluid(cameraInFluid);
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

            for (Tuple<Blocks, Integer, Integer, Texture, Integer> tuple : blocksSeries) {
                // if tuple has any blocks to be rendered and
                // if face bits are greater than zero, i.e. tuple has something to be rendered
                if (!tuple.getA().getBlockList().isEmpty() && tuple.getE() > 0) {
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tuple.getA().getBigVbo());
                    GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // this is for pos            
                    GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // this is for normal                                        
                    GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // this is for uv 

                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tuple.getB());
                    GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, VEC4_SIZE * 4, 0); // this is for color
                    GL33.glVertexAttribDivisor(3, 1);

                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tuple.getC());
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

                    Texture blocksTexture = tuple.getD();
                    if (blocksTexture != null) {
                        blocksTexture.bind(0, shaderProgram, "modelTexture0");
                    }

                    shaderProgram.updateUniform(new Vector4f(1.0f, 1.0f, 0.0f, 1.0f), "modelColor1");

                    Block selected = Editor.getSelectedCurr();
                    int selectedIndex = tuple.getA().getBlockList().indexOf(selected);

                    shaderProgram.updateUniform(selectedIndex, "selectedIndex");

                    Editor.getSELECTED_TEXTURE().bind(1, shaderProgram, "modelTexture1");

                    if (waterTexture != null && Game.isWaterEffects()) {
                        shaderProgram.updateUniform(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), "modelColor2");
                        waterTexture.bind(2, shaderProgram, "modelTexture2");
                    }

                    GL32.glDrawElementsInstancedBaseVertex(
                            GL11.GL_TRIANGLES,
                            Block.createIntBuffer(tuple.getE()),
                            tuple.getA().getBlockList().size(),
                            tuple.getA().getVboEntries()[0]
                    );

                    Texture.unbind(0);
                    Texture.unbind(1);
                    Texture.unbind(2);

                    ShaderProgram.unbind();
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
                }
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

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    public List<Tuple<Blocks, Integer, Integer, Texture, Integer>> getBlocksSeries() {
        return blocksSeries;
    }

    public boolean isCameraInFluid() {
        return cameraInFluid;
    }

    public void setCameraInFluid(boolean cameraInFluid) {
        this.cameraInFluid = cameraInFluid;
    }

    public int getProgress() {
        return progress;
    }

    public Texture getWaterTexture() {
        return waterTexture;
    }

    public void setWaterTexture(Texture waterTexture) {
        this.waterTexture = waterTexture;
    }

}
