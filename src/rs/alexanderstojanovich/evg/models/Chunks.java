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

import java.util.Comparator;
import java.util.List;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Tuple;

/**
 *
 * @author Coa
 */
public class Chunks {

    private boolean buffered = false;
    // single mat4Vbo is for model matrix shared amongst the vertices of the same instance    
    // single vec4Vbo is color shared amongst the vertices of the same instance    
    //--------------------------A--------B--------C-------D--------E-----------------------------
    //------------------------blocks-vec4Vbos-mat4Vbos-texture-faceEnBits------------------------
    private final List<Chunk> chunkList = new GapList<>();

    private static final Comparator<Chunk> COMPARATOR = new Comparator<Chunk>() {
        @Override
        public int compare(Chunk o1, Chunk o2) {
            if (o1.getId() > o2.getId()) {
                return 1;
            } else if (o1.getId() == o2.getId()) {
                return 0;
            } else {
                return -1;
            }
        }
    };

    // for both internal (Init) and external use (Editor)
    public void addBlock(Block block) {
        if (block.solid) {
            LevelContainer.ALL_SOLID_POS.add(block.pos);
        } else {
            LevelContainer.ALL_FLUID_POS.add(block.pos);
        }

        //----------------------------------------------------------------------
        int chunkId = Chunk.chunkFunc(block.pos);
        Chunk chunk = getChunk(chunkId);

        if (chunk == null) {
            chunk = new Chunk(chunkId, block.solid);
            chunkList.add(chunk);
        }

        chunk.addBlock(block);

        chunkList.sort(COMPARATOR);
    }

//    public void addBlock(byte[] byteArray, boolean solid) {
//        byte[] posBytes = new byte[12];
//        System.arraycopy(byteArray, 6, posBytes, 0, 12);
//        Vector3f vector = Vector3fUtils.vec3fFromByteArray(posBytes);
//
//        if (solid) {
//            LevelContainer.ALL_SOLID_POS.add(vector);
//        } else {
//            LevelContainer.ALL_FLUID_POS.add(vector);
//        }
//
//        int chunkId = Chunk.chunkCheck(vector);
//        System.out.println("chunkId = " + chunkId);
//        Chunk chunk = getChunk(chunkId);
//
//        if (chunk == null) {
//            chunk = new Chunk(chunkId, solid);
//            chunkList.add(chunk);
//        }
//
//        chunk.addBlock(byteArray);
//    }
    // for removing blocks (Editor)
    public void removeBlock(Block block) {
        if (block.solid) {
            LevelContainer.ALL_SOLID_POS.add(block.pos);
        } else {
            LevelContainer.ALL_FLUID_POS.add(block.pos);
        }

        int chunkId = Chunk.chunkFunc(block.pos);
        Chunk chunk = getChunk(chunkId);

        if (chunk != null) { // if chunk exists already                            
            chunk.removeBlock(block);
            // if chunk is empty (with no tuples) -> remove it
            if (chunk.getTupleList().isEmpty()) {
                chunkList.remove(chunk);
            }
        }
    }

    private void transfer(Chunk chunk, Block fluidBlock) { // update fluids use this to transfer fluid blocks between tuples
        String fluidTexture = fluidBlock.texName;
        int fluidFaceBits = fluidBlock.getFaceBits();

        Tuple<Blocks, Integer, Integer, String, Integer> srcTuple = chunk.getTuple(fluidTexture, 63);
        srcTuple.getA().getBlockList().remove(fluidBlock);
        if (srcTuple.getA().getBlockList().isEmpty()) {
            chunk.getTupleList().remove(srcTuple);
        }

        Tuple<Blocks, Integer, Integer, String, Integer> dstTuple = chunk.getTuple(fluidTexture, fluidFaceBits);
        if (dstTuple == null) {
            dstTuple = new Tuple<>(new Blocks(), 0, 0, fluidTexture, fluidFaceBits);
            chunk.getTupleList().add(dstTuple);
        }
        dstTuple.getA().getBlockList().add(fluidBlock);
        dstTuple.getA().getBlockList().sort(Block.Y_AXIS_COMP);
    }

    public void updateFluids(Chunk fluidChunk, boolean useTransfer) { // call only for fluid blocks after adding
        if (!fluidChunk.isSolid()) {
            for (Block fluidBlock : fluidChunk.getList()) {
                fluidBlock.enableAllFaces(false);
                int faceBitsBefore = fluidBlock.getFaceBits();
                for (int j = 0; j <= 5; j++) { // j - face number
                    if (LevelContainer.ALL_FLUID_POS.contains(Block.getAdjacentPos(fluidBlock.getPos(), j))) {
                        fluidBlock.disableFace(j, false);
                    }
                }
                int faceBitsAfter = fluidBlock.getFaceBits();
                if (useTransfer && faceBitsBefore != faceBitsAfter) { // if bits changed, i.e. some face(s) got disabled
                    transfer(fluidChunk, fluidBlock);
                }
            }
        }
    }

    public Chunk getChunk(int chunkId) { // linear search through chunkList to get the chunk
        Chunk result = null;
        for (Chunk chunk : chunkList) {
            if (chunk.isCached() && chunk.getMemory()[0] == chunkId
                    || !chunk.isCached() && chunk.getId() == chunkId) {
                result = chunk;
                break;
            }
        }
        return result;
    }

    public void animate() { // call only for fluid blocks
        for (Chunk chunk : getChunkList()) {
            chunk.animate();
        }
    }

    public void prepare() { // call only for fluid blocks before rendering        
        for (Chunk chunk : chunkList) {
            chunk.prepare();
        }
    }

    // for each instanced rendering
    @Deprecated
    public void render(ShaderProgram shaderProgram, Vector3f lightSrc) {
        for (Chunk chunk : chunkList) {
            chunk.render(shaderProgram, lightSrc);
        }
    }

    // very useful -> it should be like this initially
    public void saveAllToMemory() {
        for (Chunk chunk : chunkList) {
            chunk.saveToMemory();
        }
    }

    // useful when saving and wanna load everything into memory
    public void loadAllFromMemory() {
        for (Chunk chunk : chunkList) {
            chunk.loadFromMemory();
        }
    }

    // total size
    public int totalSize() {
        int result = 0;
        for (Chunk chunk : chunkList) {
            result += chunk.size();
        }
        return result;
    }

    // all blocks from all the chunks in one big list
    public List<Block> getTotalList() {
        List<Block> result = new GapList<>();
        for (Chunk chunk : chunkList) {
            result.addAll(chunk.getList());
        }
        return result;
    }

    public void printInfo() { // for debugging purposes
        StringBuilder sb = new StringBuilder();
        sb.append("CHUNKS\n");
        sb.append("CHUNKS TOTAL SIZE = ").append(totalSize()).append("\n");
        sb.append("NUMBER OF CHUNKS = ").append(chunkList.size()).append("\n");
        sb.append("DETAILED INFO\n");
        for (Chunk chunk : chunkList) {
            sb.append("id = ").append(chunk.getId())
                    .append(" | solid = ").append(chunk.isSolid())
                    .append(" | size = ").append(chunk.size())
                    .append(" | visible = ").append(chunk.isVisible())
                    .append(" | buffered = ").append(chunk.isBuffered())
                    .append(" | cached = ").append(chunk.isCached())
                    .append("\n");
        }
        sb.append("------------------------------------------------------------");
        DSLogger.reportInfo(sb.toString(), null);
    }

    public List<Chunk> getChunkList() {
        return chunkList;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
        for (Chunk chunk : getChunkList()) {
            chunk.setBuffered(buffered);
        }
    }

    @Deprecated
    public void setCameraInFluid(boolean cameraInFluid) {
        for (Chunk chunk : getChunkList()) {
            for (Tuple<Blocks, Integer, Integer, String, Integer> tuple : chunk.getTupleList()) {
                tuple.getA().setCameraInFluid(cameraInFluid);
            }
        }
    }

}
