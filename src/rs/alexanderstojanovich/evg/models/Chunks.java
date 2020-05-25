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

import java.util.Comparator;
import java.util.List;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Tuple;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
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
        //----------------------------------------------------------------------
        int chunkId = Chunk.chunkFunc(block.pos);
        Chunk chunk = getChunk(chunkId);

        if (chunk == null) {
            chunk = new Chunk(chunkId, block.solid);
            chunkList.add(chunk);
            chunkList.sort(COMPARATOR);
        }

        chunk.addBlock(block);
    }

    // for removing blocks (Editor)
    public void removeBlock(Block block) {
        int chunkId = Chunk.chunkFunc(block.pos);
        Chunk chunk = getChunk(chunkId);

        if (chunk != null) { // if chunk exists already                            
            chunk.removeBlock(block);
            // if chunk is empty (with no tuples) -> remove it
            if (chunk.getTupleSet().isEmpty()) {
                chunkList.remove(chunk);
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
            if (!chunk.isCached() && chunk.isBuffered()) {
                chunk.animate();
            }
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

    // variation on the topic
    public void saveInvisibleToMemory() {
        for (Chunk chunk : chunkList) {
            if (!chunk.isVisible()) {
                chunk.saveToMemory();
            }
        }
    }

    // useful when saving and wanna load everything into memory
    public void loadAllFromMemory() {
        for (Chunk chunk : chunkList) {
            chunk.loadFromMemory();
        }
    }

    // variation on the topic
    public void loadVisibleToMemory() {
        for (Chunk chunk : chunkList) {
            if (chunk.isVisible()) {
                chunk.loadFromMemory();
            }
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
            for (Tuple<Blocks, Integer, Integer, String, Integer> tuple : chunk.getTupleSet()) {
                tuple.getA().setCameraInFluid(cameraInFluid);
            }
        }
    }

}
