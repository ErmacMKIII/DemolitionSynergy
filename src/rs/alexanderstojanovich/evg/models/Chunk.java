/*
 * Copyright (C) 2020 Coa
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.joml.Intersectionf;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.level.Editor;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Pair;
import rs.alexanderstojanovich.evg.util.Tuple;
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Coa
 */
public class Chunk {

    public static final int VEC3_SIZE = 3;
    public static final int MAT4_SIZE = 16;

    // A, B, C are used in chunkCheck and for determining visible chunks
    public static final int A = Math.round(LevelContainer.SKYBOX_WIDTH); // modulator
    public static final int B = 16; // divider (number of chunks is calculated as 2 * B + 1)   
    public static final float C = 100.0f; // determines visibility

    // id of the chunk (signed)
    private final int id;
    private final boolean solid;

    // is a group of blocks which are prepared for instanced rendering
    // where each tuple is considered as:                
    //--------------------------A--------B--------C-------D--------E-----------------------------
    //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
    private final Set<Tuple<Blocks, Integer, Integer, String, Integer>> tupleSet = new HashSet<>();

    private Texture waterTexture;

    private boolean buffered = false;

    private boolean visible = false;

    private final byte[] memory = new byte[0x100000];
    private int pos = 0;
    private boolean cached = false;

    public Chunk(int id, boolean solid) {
        this.id = id;
        this.solid = solid;
    }

    public Tuple<Blocks, Integer, Integer, String, Integer> getTuple(String keyTexture, Integer keyFaceBits) {
        Tuple<Blocks, Integer, Integer, String, Integer> result = null;
        for (Tuple<Blocks, Integer, Integer, String, Integer> tuple : tupleSet) {
            if (tuple.getD() != null && tuple.getD().equals(keyTexture)
                    && tuple.getE() != null && tuple.getE().equals(keyFaceBits)) {
                result = tuple;
                break;
            }
        }
        return result;
    }

    public void addBlock(Block block) {
        if (block.solid) {
            LevelContainer.addSolidPos(block.pos);
        } else {
            LevelContainer.addFluidPos(block.pos);
        }
        String blockTexture = block.texName;
        int blockFaceBits = block.getFaceBits();
        Tuple<Blocks, Integer, Integer, String, Integer> tuple = getTuple(blockTexture, blockFaceBits);

        if (tuple == null) {
            tuple = new Tuple<>(new Blocks(), 0, 0, blockTexture, blockFaceBits);
            tupleSet.add(tuple);
        }

        tuple.getA().getBlockList().add(block);
        tuple.getA().getBlockList().sort(Block.Y_AXIS_COMP);
    }

    public void removeBlock(Block block) {
//        if (block.solid) {
//            LevelContainer.ALL_SOLID_POS.remove(block.pos);
//        } else {
//            LevelContainer.ALL_FLUID_POS.remove(block.pos);
//        }
        String blockTexture = block.texName;
        int blockFaceBits = block.getFaceBits();
        Tuple<Blocks, Integer, Integer, String, Integer> target = getTuple(blockTexture, blockFaceBits);
        if (target != null) {
            target.getA().getBlockList().remove(block);
            // if tuple has no blocks -> remove it
            if (target.getA().getBlockList().isEmpty()) {
                tupleSet.remove(target);
            }
        }
    }

    public void bufferVectors(Blocks blocks, Tuple<Blocks, Integer, Integer, String, Integer> tuple) {
        FloatBuffer vec3FloatBuff = BufferUtils.createFloatBuffer(blocks.getBlockList().size() * VEC3_SIZE);
        for (Block block : blocks.getBlockList()) {
            Vector3f color = block.getPrimaryColor();
            vec3FloatBuff.put(color.x);
            vec3FloatBuff.put(color.y);
            vec3FloatBuff.put(color.z);
        }
        vec3FloatBuff.flip();
        int vec4Vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vec4Vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vec3FloatBuff, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        tuple.setB(vec4Vbo);
    }

    public void bufferMatrices(Blocks blocks, Tuple<Blocks, Integer, Integer, String, Integer> tuple) {
        FloatBuffer mat4FloatBuff = BufferUtils.createFloatBuffer(blocks.getBlockList().size() * MAT4_SIZE);
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
        int mat4Vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, mat4Vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, mat4FloatBuff, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        tuple.setC(mat4Vbo);
    }

    public void bufferAll() {
        if (!cached) {
            for (Tuple<Blocks, Integer, Integer, String, Integer> tuple : tupleSet) {
                Blocks blocks = tuple.getA();
                blocks.bufferVertices();
                bufferMatrices(blocks, tuple);
                bufferVectors(blocks, tuple);
            }
            buffered = true;
        }
    }

    public void animate() { // call only for fluid blocks
        for (Tuple<Blocks, Integer, Integer, String, Integer> tuple : tupleSet) {
            tuple.getA().animate();
        }
    }

    public void prepare() { // call only for fluid blocks before rendering        
        for (Tuple<Blocks, Integer, Integer, String, Integer> tuple : tupleSet) {
            Blocks blocks = tuple.getA();
            blocks.prepare();
        }
    }

    // set camera in fluid for underwater effects (call only for fluid)
    public void setCameraInFluid(boolean cameraInFluid) {
        for (Tuple<Blocks, Integer, Integer, String, Integer> tuple : tupleSet) {
            tuple.getA().setCameraInFluid(cameraInFluid);
        }
    }

    // it renders all of them instanced if they're visible
    public void render(ShaderProgram shaderProgram, Vector3f lightSrc) {
        if (buffered && shaderProgram != null && !tupleSet.isEmpty() && visible) {
            Texture.enable();

            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);
            GL20.glEnableVertexAttribArray(3);
            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            GL20.glEnableVertexAttribArray(6);
            GL20.glEnableVertexAttribArray(7);

            for (Tuple<Blocks, Integer, Integer, String, Integer> tuple : tupleSet) {
                // if tuple has any blocks to be rendered and
                // if face bits are greater than zero, i.e. tuple has something to be rendered
                if (!tuple.getA().getBlockList().isEmpty() && tuple.getE() > 0) {
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tuple.getA().getBigVbo());
                    GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // this is for pos            
                    GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // this is for normal                                        
                    GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // this is for uv 

                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tuple.getB());
                    GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, VEC3_SIZE * 4, 0); // this is for color
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

                    shaderProgram.updateUniform(solid ? 1.0f : 0.5f, "modelAlpha");

                    Texture blocksTexture = Texture.TEX_MAP.getOrDefault(tuple.getD(), Texture.QMARK);
                    if (blocksTexture != null) {
                        blocksTexture.bind(0, shaderProgram, "modelTexture0");
                    }

                    shaderProgram.updateUniform(new Vector4f(1.0f, 1.0f, 0.0f, 1.0f), "modelColor1");

                    Block selected = Editor.getSelectedCurr();
                    int selectedIndex = tuple.getA().getBlockList().indexOf(selected);

                    shaderProgram.updateUniform(selectedIndex, "selectedIndex");

                    if (Editor.getSelectedCurr() != null) {
                        shaderProgram.updateUniform(new Vector3f(1.0f, 1.0f, 0.0f), "modelColor1");
                        Texture.MINIGUN.bind(1, shaderProgram, "modelTexture1");
                    }

                    if (waterTexture != null && Game.isWaterEffects()) {
                        shaderProgram.updateUniform(new Vector3f(1.0f, 1.0f, 1.0f), "modelColor2");
                        waterTexture.bind(2, shaderProgram, "modelTexture2");
                    }

                    GL32.glDrawElementsInstancedBaseVertex(
                            GL11.GL_TRIANGLES,
                            Block.createIntBuffer(tuple.getE()),
                            tuple.getA().getBlockList().size(),
                            0
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

    // deallocates Chunk from graphic card
    public void release() {
        if (buffered && !cached) {
            //--------------------------A--------B--------C-------D--------E-----------------------------
            //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
            for (Tuple<Blocks, Integer, Integer, String, Integer> tuple : tupleSet) {
                GL15.glDeleteBuffers(tuple.getA().getBigVbo());
                GL15.glDeleteBuffers(tuple.getB());
                GL15.glDeleteBuffers(tuple.getC());
            }
            buffered = false;
        }
    }

    // determine chunk (where am I)
    public static int chunkFunc(Vector3f pos) {
        float x = Math.round((pos.x % (A + 1)));
        float y = Math.round((pos.y % (A + 1)));
        float z = Math.round((pos.z % (A + 1)));

        return Math.round(((x + y + z) / (3.0f * B)));
    }

    // determine if chunk is visible
    public static boolean chunkCheck(Vector3f pos, Vector3f front) {
        boolean yea = false;

        float d = C / 2.0f;
        Vector3f temp1 = new Vector3f();
        Vector3f min = pos.sub(d, d, d, temp1);
        Vector3f temp2 = new Vector3f();
        Vector3f max = pos.add(d, d, d, temp2);
        Vector2f result = new Vector2f();
        boolean ints = Intersectionf.intersectRayAab(pos, front, min, max, result);
        if (ints && result.x <= C) {
            yea = true;
        }
        return yea;
    }

    // determine which chunks are visible by this chunk
    public static List<Integer> determineVisible(Vector3f pos, Vector3f front) {
        List<Integer> result = new ArrayList<>();

        int x = Chunk.chunkFunc(pos);
        if (!result.contains(x) && chunkCheck(pos, front)) {
            result.add(x);
        }

        Vector3f va = new Vector3f();
        pos.add(C, 0.0f, 0.0f, va);
        int a = Chunk.chunkFunc(va);
        if (!result.contains(a) && Chunk.chunkCheck(va, front)) {
            result.add(a);
        }

        Vector3f vb = new Vector3f();
        pos.add(0.0f, C, 0.0f, vb);
        int b = Chunk.chunkFunc(vb);
        if (!result.contains(b) && Chunk.chunkCheck(vb, front)) {
            result.add(b);
        }

        Vector3f vc = new Vector3f();
        pos.add(0.0f, C, 0.0f, vc);
        int c = Chunk.chunkFunc(vc);
        if (!result.contains(c) && Chunk.chunkCheck(vc, front)) {
            result.add(c);
        }

        Vector3f vd = new Vector3f();
        pos.add(-C, 0.0f, 0.0f, vd);
        int d = Chunk.chunkFunc(vd);
        if (!result.contains(d) && Chunk.chunkCheck(vd, front)) {
            result.add(d);
        }

        Vector3f ve = new Vector3f();
        pos.add(0.0f, -C, 0.0f, ve);
        int e = Chunk.chunkFunc(ve);
        if (!result.contains(e) && Chunk.chunkCheck(ve, front)) {
            result.add(e);
        }

        Vector3f vf = new Vector3f();
        pos.add(0.0f, 0.0f, -C, vf);
        int f = Chunk.chunkFunc(vf);
        if (!result.contains(f) && Chunk.chunkCheck(vf, front)) {
            result.add(f);
        }

        return result;
    }

    public int size() { // for debugging purposes
        int size = 0;
        if (cached) {
            size = (pos + 1 - 3) / 29;
        } else {
            for (Tuple<Blocks, Integer, Integer, String, Integer> tuple : tupleSet) {
                size += tuple.getA().getBlockList().size();
            }
        }
        return size;
    }

    public List<Block> getList() {
        List<Block> result = new GapList<>();
        for (Tuple<Blocks, Integer, Integer, String, Integer> tuple : tupleSet) {
            result.addAll(tuple.getA().getBlockList());
        }
        return result;
    }

    public void saveToMemory() {
        if (!buffered && !cached) {
            List<Block> blocks = getList();
            pos = 0;
            memory[pos++] = (byte) id;
            memory[pos++] = (byte) blocks.size();
            memory[pos++] = (byte) (blocks.size() >> 8);
            for (Block block : blocks) {
                byte[] texName = block.texName.getBytes();
                System.arraycopy(texName, 0, memory, pos, 5);
                pos += 5;
                byte[] solidPos = Vector3fUtils.vec3fToByteArray(block.getPos());
                System.arraycopy(solidPos, 0, memory, pos, solidPos.length);
                pos += solidPos.length;
                Vector3f primCol = block.getPrimaryColor();
                byte[] solidCol = Vector3fUtils.vec3fToByteArray(primCol);
                System.arraycopy(solidCol, 0, memory, pos, solidCol.length);
                pos += solidCol.length;
            }
            tupleSet.clear();
            cached = true;
        }
    }

    public void loadFromMemory() {
        if (!buffered && cached) {
            pos = 1;
            int len = ((memory[pos + 1] & 0xFF) << 8) | (memory[pos] & 0xFF);
            pos += 2;
            for (int i = 0; i < len; i++) {
                char[] texNameArr = new char[5];
                for (int k = 0; k < texNameArr.length; k++) {
                    texNameArr[k] = (char) memory[pos++];
                }
                String texName = String.valueOf(texNameArr);

                byte[] blockPosArr = new byte[12];
                System.arraycopy(memory, pos, blockPosArr, 0, blockPosArr.length);
                Vector3f blockPos = Vector3fUtils.vec3fFromByteArray(blockPosArr);
                pos += blockPosArr.length;

                byte[] blockPosCol = new byte[12];
                System.arraycopy(memory, pos, blockPosCol, 0, blockPosCol.length);
                Vector3f blockCol = Vector3fUtils.vec3fFromByteArray(blockPosCol);
                pos += blockPosCol.length;

                Block block = new Block(false, texName, blockPos, blockCol, solid);
                addBlock(block);
            }
            cached = false;
        }
    }

    public int getId() {
        return id;
    }

    public boolean isSolid() {
        return solid;
    }

    public Set<Tuple<Blocks, Integer, Integer, String, Integer>> getTupleSet() {
        return tupleSet;
    }

    public Texture getWaterTexture() {
        return waterTexture;
    }

    public void setWaterTexture(Texture waterTexture) {
        this.waterTexture = waterTexture;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public byte[] getMemory() {
        return memory;
    }

    public int getPos() {
        return pos;
    }

    public boolean isCached() {
        return cached;
    }

}
