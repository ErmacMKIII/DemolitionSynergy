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

import java.util.List;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
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

    private void addNeighbors(Block block) { // called for all blocks before adding        
        Integer hashCode = block.hashCode();
        for (int j = 0; j <= 5; j++) { // j - facenum
            // updating neighbor from adding block perspective 
            Vector3f otherBlockPos = block.getAdjacentPos(block.getPos(), j);
            Integer hashCode1 = LevelContainer.getPOS_SOLID_MAP().get(otherBlockPos);
            Integer hashCode2 = LevelContainer.getPOS_FLUID_MAP().get(otherBlockPos);
            if (hashCode1 == null && hashCode2 == null) {
                block.getAdjacentBlockMap().remove(j);
            } else if (hashCode1 != null && hashCode2 == null) {
                block.getAdjacentBlockMap().put(j, hashCode1);
            } else if (hashCode1 == null && hashCode2 != null) {
                block.getAdjacentBlockMap().put(j, hashCode2);
            }
            // updating neighbor from other blocks perspective
            int otherBlockChunkId = Chunk.chunkFunc(otherBlockPos);
            Chunk chunk = getChunk(otherBlockChunkId);
            if (chunk != null) {
                for (Block otherBlock : chunk.getList()) {
                    if (otherBlock.pos.equals(otherBlockPos)) {
                        // ok we found out which at which side
                        otherBlock.getAdjacentBlockMap().put(j % 2 == 0 ? j + 1 : j - 1, hashCode);
                        break; // goal reached, now leave
                    }
                }
            }
        }
    }

    private void removeNeighbors(Block block) { // called for all blocks before removing       
        Integer hashCode = block.hashCode();
        for (int j = 0; j <= 5; j++) { // j - facenum
            // updating neighbor from removing block perspective 
            Vector3f otherBlockPos = block.getAdjacentPos(block.getPos(), j);
            Integer hashCode1 = LevelContainer.getPOS_SOLID_MAP().get(otherBlockPos);
            Integer hashCode2 = LevelContainer.getPOS_FLUID_MAP().get(otherBlockPos);
            if (hashCode1 == null && hashCode2 == null) {
                block.getAdjacentBlockMap().remove(j);
            } else if (hashCode1 != null && hashCode2 == null) {
                block.getAdjacentBlockMap().remove(j, hashCode1);
            } else if (hashCode1 == null && hashCode2 != null) {
                block.getAdjacentBlockMap().remove(j, hashCode2);
            }
            // updating neighbor from other blocks perspective
            int otherBlockChunkId = Chunk.chunkFunc(otherBlockPos);
            Chunk chunk = getChunk(otherBlockChunkId);
            if (chunk != null) {
                for (Block otherBlock : chunk.getList()) {
                    if (otherBlock.pos.equals(otherBlockPos)) {
                        // ok we found out which at which side
                        otherBlock.getAdjacentBlockMap().remove(j % 2 == 0 ? j + 1 : j - 1, hashCode);
                        break; // goal reached, now leave
                    }
                }
            }
        }
    }

    // for both internal (Init) and external use (Editor)
    public void addBlock(Block block) {
        if (block.solid) {
            LevelContainer.getPOS_SOLID_MAP().put(block.pos, block.hashCode());
        } else {
            LevelContainer.getPOS_FLUID_MAP().put(block.pos, block.hashCode());
        }

        addNeighbors(block);

        //----------------------------------------------------------------------
        int chunkId = Chunk.chunkFunc(block.pos);
        Chunk chunk = getChunk(chunkId);

        if (chunk == null) {
            chunk = new Chunk(chunkId);
            chunkList.add(chunk);
        }

        Texture blockTexture = block.primaryTexture;
        int blockFaceBits = block.getFaceBits();
        Tuple<Blocks, Integer, Integer, Texture, Integer> tuple = chunk.getTuple(blockTexture, blockFaceBits);

        if (tuple == null) {
            tuple = new Tuple<>(new Blocks(), 0, 0, blockTexture, blockFaceBits);
            chunk.getTupleList().add(tuple);
        }

        tuple.getA().getBlockList().add(block);
        tuple.getA().getBlockList().sort(Block.Y_AXIS_COMP);
    }

    // for removing blocks (Editor)
    public void removeBlock(Block block) {
        if (block.solid) {
            LevelContainer.getPOS_SOLID_MAP().remove(block.pos);
        } else {
            LevelContainer.getPOS_FLUID_MAP().remove(block.pos);
        }

        removeNeighbors(block);

        int chunkId = Chunk.chunkFunc(block.pos);
        Chunk chunk = getChunk(chunkId);

        if (chunk != null) { // if chunk exists already                            
            Texture blockTexture = block.primaryTexture;
            int blockFaceBits = block.getFaceBits();
            Tuple<Blocks, Integer, Integer, Texture, Integer> target = chunk.getTuple(blockTexture, blockFaceBits);
            if (target != null) {
                target.getA().getBlockList().remove(block);
                // if tuple has no blocks -> remove it
                if (target.getA().getBlockList().isEmpty()) {
                    chunk.getTupleList().remove(target);
                }
            }
            // if chunk is empty (with no tuples) -> remove it
            if (chunk.getTupleList().isEmpty()) {
                chunkList.remove(chunk);
            }
        }
    }

    private void transfer(Block fluidBlock) { // update fluids use this to transfer fluid blocks between tuples
        Chunk chunk = getChunk(Chunk.chunkFunc(fluidBlock.pos));
        if (chunk != null) {
            Texture fluidTexture = fluidBlock.primaryTexture;
            int fluidFaceBits = fluidBlock.getFaceBits();

            Tuple<Blocks, Integer, Integer, Texture, Integer> srcTuple = chunk.getTuple(fluidTexture, 63);
            srcTuple.getA().getBlockList().remove(fluidBlock);
            if (srcTuple.getA().getBlockList().isEmpty()) {
                chunk.getTupleList().remove(srcTuple);
            }

            Tuple<Blocks, Integer, Integer, Texture, Integer> dstTuple = chunk.getTuple(fluidTexture, fluidFaceBits);
            if (dstTuple == null) {
                dstTuple = new Tuple<>(new Blocks(), 0, 0, fluidTexture, fluidFaceBits);
                chunk.getTupleList().add(dstTuple);
            }
            dstTuple.getA().getBlockList().add(fluidBlock);
            dstTuple.getA().getBlockList().sort(Block.Y_AXIS_COMP);
        }
    }

    public void updateFluids(boolean useTransfer) { // call only for fluid blocks after adding
        for (Block fluidBlock : getTotalList()) {
            fluidBlock.enableAllFaces(false);
            int faceBitsBefore = fluidBlock.getFaceBits();
            for (int j = 0; j <= 5; j++) { // j - face number
                Integer hash1 = fluidBlock.getAdjacentBlockMap().get(j);
                Integer hash2 = LevelContainer.getPOS_FLUID_MAP().get(fluidBlock.getAdjacentPos(fluidBlock.getPos(), j));
                if (hash1 != null && hash1.equals(hash2)) {
                    fluidBlock.disableFace(j, false);
                }
            }
            int faceBitsAfter = fluidBlock.getFaceBits();
            if (useTransfer && faceBitsBefore != faceBitsAfter) { // if bits changed, i.e. some face(s) got disabled
                transfer(fluidBlock);
            }
        }
    }

    public Chunk getChunk(int chunkId) { // linear search through chunkList to get the chunk
        Chunk result = null;
        for (Chunk chunk : chunkList) {
            if (chunk.getId() == chunkId) {
                result = chunk;
                break;
            }
        }
        return result;
    }

    public void bufferAll() {
        for (Chunk chunk : chunkList) {
            chunk.bufferAll();
        }
        buffered = true;
    }

    public void animate() { // call only for fluid blocks
        for (Chunk chunk : getVisibleChunks()) {
            chunk.animate();
        }
    }

    public void prepare() { // call only for fluid blocks before rendering        
        for (Chunk chunk : chunkList) {
            chunk.prepare();
        }
    }

    // for each instanced rendering
    public void render(ShaderProgram shaderProgram, Vector3f lightSrc) {
        for (Chunk chunk : getVisibleChunks()) {
            chunk.render(shaderProgram, lightSrc);
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

    public List<Chunk> getVisibleChunks() {
        List<Chunk> result = new GapList<>();
        for (Chunk chunk : chunkList) {
            if (chunk.isVisible()) {
                result.add(chunk);
            }
        }
        return result;
    }

    // all visible blocks from all the chunks in one big list
    public List<Block> getTotalVisibleList() {
        List<Block> result = new GapList<>();
        for (Chunk chunk : getVisibleChunks()) {
            result.addAll(chunk.getList());
        }
        return result;
    }

    public List<Chunk> getChunkList() {
        return chunkList;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
        for (Chunk chunk : getVisibleChunks()) {
            chunk.setBuffered(buffered);
        }
    }

    public void setCameraInFluid(boolean cameraInFluid) {
        for (Chunk chunk : getVisibleChunks()) {
            for (Tuple<Blocks, Integer, Integer, Texture, Integer> tuple : chunk.getTupleList()) {
                tuple.getA().setCameraInFluid(cameraInFluid);
            }
        }
    }

}
