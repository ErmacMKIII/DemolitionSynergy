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
import java.util.function.Predicate;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import rs.alexanderstojanovich.evg.core.Texture;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Coa
 */
public class Blocks { // mutual class for both solid blocks and fluid blocks with improved rendering

    private final List<Block> blockList = new LinkedList<>();
    private int bigVbo;
    private boolean verticesBuffered = false;
    private boolean cameraInFluid = false;
    private boolean verticesReversed = false;
    // array with offsets in the big float buffer
    // this is maximum amount of blocks of the type game can hold
    private final int[] vboEntries = new int[65536];
    private final int[] ibos = new int[65536];
    public static final IntBuffer CONST_INT_BUFFER = getConstIntBuffer();
    private int mutualIbo;
    private boolean indicesBuffered = false;

    public void bufferVertices() { // call it before any rendering        
        FloatBuffer bigFloatBuff = BufferUtils.createFloatBuffer(blockList.size() * Block.VERTEX_COUNT * Vertex.SIZE);
        int blkIndex = 0;
        int offset = 0;
        for (Block block : blockList) {
            vboEntries[blkIndex] = offset;
            for (int faceNum = 0; faceNum <= 5; faceNum++) {
                if (block.getEnabledFaces()[faceNum]) {
                    for (Vertex vertex : block.getFaceVertices(faceNum)) { // for each vertex
                        bigFloatBuff.put(vertex.getPos().x);
                        bigFloatBuff.put(vertex.getPos().y);
                        bigFloatBuff.put(vertex.getPos().z);
                        bigFloatBuff.put(vertex.getNormal().x);
                        bigFloatBuff.put(vertex.getNormal().y);
                        bigFloatBuff.put(vertex.getNormal().z);
                        bigFloatBuff.put(vertex.getUv().x);
                        bigFloatBuff.put(vertex.getUv().y);
                        offset++;
                    }
                }
            }
            blkIndex++;
        }
        bigFloatBuff.flip();
        bigVbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bigFloatBuff, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        verticesBuffered = true;
    }

    public void bufferIndices() { // call it before any rendering
        int blkIndex = 0;
        for (Block block : blockList) {
            List<Integer> indices = new ArrayList<>();
            for (int j = 0; j < block.getNumOfEnabledFaces(); j++) { // i - face number                                
                indices.add(4 * j);
                indices.add(4 * j + 1);
                indices.add(4 * j + 2);

                indices.add(4 * j + 2);
                indices.add(4 * j + 3);
                indices.add(4 * j);
            }
            // storing indices in the buffer
            IntBuffer intBuff = BufferUtils.createIntBuffer(indices.size());
            for (Integer index : indices) {
                intBuff.put(index);
            }
            intBuff.flip();
            // storing indices buffer on the graphics card
            int ibo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, intBuff, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            // finally assigning it to the array element
            ibos[blkIndex] = ibo;
            blkIndex++;
        }
        indicesBuffered = true;
    }

    public void bufferAll() { // buffer both, call it before any rendering
        bufferVertices();
        bufferIndices();
    }

    public static IntBuffer getConstIntBuffer() {
        IntBuffer intBuff = BufferUtils.createIntBuffer(Block.INDICES_COUNT);
        for (Integer index : Block.CONST.indices) {
            intBuff.put(index);
        }
        intBuff.flip();
        return intBuff;
    }

    public void animate() { // call only for fluid blocks
        for (Block block : blockList) {
            block.animate(false);
        }
        bufferVertices();
    }

    public void prepare() { // call only for fluid blocks before rendering
        if (Boolean.logicalXor(cameraInFluid, verticesReversed)) {
            for (Block block : blockList) {
                block.reverseFaceVertexOrder(false);
            }
            verticesReversed = !verticesReversed;
            bufferVertices();
        }
    }

    // standard render all
    public void render(ShaderProgram shaderProgram, Vector3f lightSrc) {
        if (verticesBuffered && indicesBuffered && shaderProgram != null) {
            Texture.enable();

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, mutualIbo);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 0); // this is for pos
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 12); // this is for normal                                        
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 24); // this is for uv                                   

            int blkIndex = 0;
            shaderProgram.bind();
            for (Block block : blockList) {
                block.light = lightSrc;
                block.transform(shaderProgram);
                block.useLight(shaderProgram);
                if (block.primaryTexture != null) { // this is primary texture
                    block.primaryColor(shaderProgram);
                    block.primaryTexture.bind(0, shaderProgram, "modelTexture0");
                }
                if (block.secondaryTexture != null) { // this is editor overlay texture
                    block.secondaryColor(shaderProgram);
                    block.secondaryTexture.bind(1, shaderProgram, "modelTexture1");
                }
                if (block.tertiaryTexture != null) { // this is reflective texture
                    block.tertiaryColor(shaderProgram);
                    block.tertiaryTexture.bind(2, shaderProgram, "modelTexture2");
                }

                GL32.glDrawElementsBaseVertex(
                        GL11.GL_TRIANGLES,
                        Block.INDICES_COUNT,
                        GL11.GL_UNSIGNED_INT,
                        0,
                        vboEntries[blkIndex]
                );

                Texture.unbind(0);
                Texture.unbind(1);
                Texture.unbind(2);

                blkIndex++;
            }
            ShaderProgram.unbind();

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

            Texture.disable();
        }
    }

    // powerful render if block is visible by camera
    public void renderIf(ShaderProgram shaderProgram, Vector3f lightSrc, Predicate<Block> predicate) {
        if (verticesBuffered && indicesBuffered && shaderProgram != null) {
            Texture.enable();

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);

            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 0); // this is for pos
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 12); // this is for normal                                        
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, (Vertex.SIZE) * 4, 24); // this is for uv                                   

            int blkIndex = 0;
            shaderProgram.bind();
            for (Block block : blockList) {
                block.light = lightSrc;
                if (predicate.test(block)) {
                    block.transform(shaderProgram);
                    block.useLight(shaderProgram);
                    if (block.primaryTexture != null) { // this is primary texture
                        block.primaryColor(shaderProgram);
                        block.primaryTexture.bind(0, shaderProgram, "modelTexture0");
                    }
                    if (block.secondaryTexture != null) { // this is editor overlay texture
                        block.secondaryColor(shaderProgram);
                        block.secondaryTexture.bind(1, shaderProgram, "modelTexture1");
                    }
                    if (block.tertiaryTexture != null) { // this is reflective texture
                        block.tertiaryColor(shaderProgram);
                        block.tertiaryTexture.bind(2, shaderProgram, "modelTexture2");
                    }

                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibos[blkIndex]);
                    GL32.glDrawElementsBaseVertex(
                            GL11.GL_TRIANGLES,
                            Block.INDICES_COUNT,
                            GL11.GL_UNSIGNED_INT,
                            0,
                            vboEntries[blkIndex]
                    );
                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
                    Texture.unbind(0);
                    Texture.unbind(1);
                    Texture.unbind(2);
                }
                blkIndex++;
            }

            ShaderProgram.unbind();

            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            Texture.disable();
        }
    }

    public List<Block> getBlockList() {
        return blockList;
    }

    public int getBigVbo() {
        return bigVbo;
    }

    public boolean isVerticesBuffered() {
        return verticesBuffered;
    }

    public boolean isVerticesReversed() {
        return verticesReversed;
    }

    public boolean isCameraInFluid() {
        return cameraInFluid;
    }

    public void setCameraInFluid(boolean cameraInFluid) {
        this.cameraInFluid = cameraInFluid;
    }

    public int[] getVboEntries() {
        return vboEntries;
    }

    public int getMutualIbo() {
        return mutualIbo;
    }

    public boolean isIndicesBuffered() {
        return indicesBuffered;
    }

}
