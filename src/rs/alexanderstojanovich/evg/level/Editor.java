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
package rs.alexanderstojanovich.evg.level;

import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.critter.Observer;
import java.util.List;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Blocks;
import rs.alexanderstojanovich.evg.models.Chunk;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Tuple;

/**
 *
 * @author Coa
 */
public class Editor {

    private static Block loaded = null;

    private static Block selectedNew = null;

    private static Block selectedCurr = null;
    private static int selectedCurrIndex = -1;

    private static final Texture SELECTED_TEXTURE = Texture.MINIGUN;

    private static int value = 0; // value about which texture to use
    private static final int MIN_VAL = 0;
    private static final int MAX_VAL = 3;

    public static void selectNew(LevelContainer levelContainer) {
        deselect();
        if (loaded == null) // first time it's null
        {
            loaded = new Block(false);
            selectLoadedTexture();
        }
        selectedNew = loaded;

        // fetching..
        Observer obs = levelContainer.getLevelActors().getPlayer();
        Vector3f pos = obs.getCamera().getPos();
        Vector3f front = obs.getCamera().getFront();
        final float skyboxWidth = LevelContainer.SKYBOX_WIDTH;
        // initial calculation (make it dependant to point player looking at)
        // and make it follows player camera        
        selectedNew.getPos().x = (Math.round(8.0f * front.x) + Math.round(pos.x)) % Math.round(skyboxWidth);
        selectedNew.getPos().y = (Math.round(8.0f * front.y) + Math.round(pos.y)) % Math.round(skyboxWidth);
        selectedNew.getPos().z = (Math.round(8.0f * front.z) + Math.round(pos.z)) % Math.round(skyboxWidth);

        if (!cannotPlace(levelContainer)) {
            selectedNew.getSecondaryColor().x = 0.0f;
            selectedNew.getSecondaryColor().y = 1.0f;
            selectedNew.getSecondaryColor().z = 0.0f;
            selectedNew.getSecondaryColor().w = 1.0f;
            selectedNew.setSecondaryTexture(SELECTED_TEXTURE);
        }

        levelContainer.getSoundFXPlayer().play(AudioFile.BLOCK_SELECT, selectedNew.getPos());
    }

    public static void selectCurr(LevelContainer levelContainer) {
        deselect(); // algorithm is select the nearest that interesects the camera ray
        Vector3f cameraPos = levelContainer.getLevelActors().getPlayer().getCamera().getPos();
        Vector3f cameraFront = levelContainer.getLevelActors().getPlayer().getCamera().getFront();
        int currChunkId = Chunk.chunkFunc(cameraPos, cameraFront);
        Chunk currSolidChunk = levelContainer.getSolidChunks().getChunk(currChunkId);
        List<Block> bigSolidList = null;
        if (currSolidChunk != null) {
            bigSolidList = currSolidChunk.getList();
        }
        Chunk currFluidChunk = levelContainer.getFluidChunks().getChunk(currChunkId);
        List<Block> bigFluidList = null;
        if (currFluidChunk != null) {
            bigFluidList = currFluidChunk.getList();
        }
        float minDistanceOfSolid = Float.POSITIVE_INFINITY;
        float minDistanceOfFluid = Float.POSITIVE_INFINITY;

        Block minSolid = null;
        Block minFluid = null;
        int minSolidBlkIndex = -1;
        int solidBlkIndex = 0;
        //----------------------------------------------------------------------
        if (bigSolidList != null) {
            for (Block solidBlock : bigSolidList) {
                Vector3f vect = solidBlock.getPos();
                float distance = Vector3f.distance(cameraPos.x, cameraPos.y, cameraPos.z, vect.x, vect.y, vect.z);
                if (solidBlock.intersectsRay(
                        levelContainer.getLevelActors().getPlayer().getCamera().getFront(),
                        levelContainer.getLevelActors().getPlayer().getCamera().getPos())) {
                    if (distance < minDistanceOfSolid) {
                        minDistanceOfSolid = distance;
                        minSolid = solidBlock;
                        minSolidBlkIndex = solidBlkIndex;
                    }
                }
                solidBlkIndex++;
            }
        }

        int minFluidBlkIndex = -1;
        int fluidBlkIndex = 0;
        if (bigFluidList != null) {
            for (Block fluidBlock : bigFluidList) {
                Vector3f vect = fluidBlock.getPos();
                float distance = Vector3f.distance(cameraPos.x, cameraPos.y, cameraPos.z, vect.x, vect.y, vect.z);
                if (fluidBlock.intersectsRay(
                        levelContainer.getLevelActors().getPlayer().getCamera().getFront(),
                        levelContainer.getLevelActors().getPlayer().getCamera().getPos())) {
                    if (distance < minDistanceOfFluid) {
                        minDistanceOfFluid = distance;
                        minFluid = fluidBlock;
                        minFluidBlkIndex = fluidBlkIndex;
                    }
                }
                fluidBlkIndex++;
            }
        }
        //----------------------------------------------------------------------
        if (minDistanceOfSolid < minDistanceOfFluid) { // SOLID PREFERANCE
            if (minSolid != null) {
                selectedCurr = minSolid;
                selectedCurr.getSecondaryColor().x = 1.0f;
                selectedCurr.getSecondaryColor().y = 1.0f;
                selectedCurr.getSecondaryColor().z = 0.0f;
                selectedCurr.getSecondaryColor().w = 1.0f;
                selectedCurr.setSecondaryTexture(SELECTED_TEXTURE);
                selectedCurrIndex = minSolidBlkIndex;
                levelContainer.getSoundFXPlayer().play(AudioFile.BLOCK_SELECT, selectedCurr.getPos());
            }
        } else if (minDistanceOfSolid >= minDistanceOfFluid) {
            if (minFluid != null) {
                selectedCurr = minFluid;
                selectedCurr.getSecondaryColor().x = 1.0f;
                selectedCurr.getSecondaryColor().y = 1.0f;
                selectedCurr.getSecondaryColor().z = 0.0f;
                selectedCurr.getSecondaryColor().w = 1.0f;
                selectedCurr.setSecondaryTexture(SELECTED_TEXTURE);
                selectedCurrIndex = minFluidBlkIndex;
                levelContainer.getSoundFXPlayer().play(AudioFile.BLOCK_SELECT, selectedCurr.getPos());
            }
        }
    }

    public static void deselect() {
        if (selectedCurr != null) {
            selectedCurr.setSecondaryTexture(null);
            selectedCurr.getSecondaryColor().x = 1.0f;
            selectedCurr.getSecondaryColor().y = 1.0f;
            selectedCurr.getSecondaryColor().z = 1.0f;
            selectedCurr.getSecondaryColor().w = 1.0f;
        }
        selectedNew = selectedCurr = null;
        selectedCurrIndex = -1;
    }

    public static void selectAdjacent(LevelContainer levelContainer, int position) {
        deselect();
        selectCurr(levelContainer);
        if (selectedCurr != null) {
            if (loaded == null) // first time it's null
            {
                loaded = new Block(false);
                selectLoadedTexture();
            }
            selectedNew = loaded;
            selectedNew.getPos().x = selectedCurr.getPos().x;
            selectedNew.getPos().y = selectedCurr.getPos().y;
            selectedNew.getPos().z = selectedCurr.getPos().z;

            switch (position) {
                case Block.LEFT:
                    selectedNew.getPos().x -= selectedCurr.getWidth() / 2.0f + selectedNew.getWidth() / 2.0f;
                    break;
                case Block.RIGHT:
                    selectedNew.getPos().x += selectedCurr.getWidth() / 2.0f + selectedNew.getWidth() / 2.0f;
                    break;
                case Block.BOTTOM:
                    selectedNew.getPos().y -= selectedCurr.getHeight() / 2.0f + selectedNew.getHeight() / 2.0f;
                    break;
                case Block.TOP:
                    selectedNew.getPos().y += selectedCurr.getHeight() / 2.0f + selectedNew.getHeight() / 2.0f;
                    break;
                case Block.BACK:
                    selectedNew.getPos().z -= selectedCurr.getDepth() / 2.0f + selectedNew.getDepth() / 2.0f;
                    break;
                case Block.FRONT:
                    selectedNew.getPos().z += selectedCurr.getDepth() / 2.0f + selectedNew.getDepth() / 2.0f;
                    break;
                default:
                    break;
            }

            if (!cannotPlace(levelContainer)) {
                selectedNew.getSecondaryColor().x = 0.0f;
                selectedNew.getSecondaryColor().y = 0.0f;
                selectedNew.getSecondaryColor().z = 1.0f;
                selectedNew.getSecondaryColor().w = 1.0f;
                selectedNew.setSecondaryTexture(SELECTED_TEXTURE);
            }
        }
    }

    private static boolean cannotPlace(LevelContainer levelContainer) {
        boolean cant = false;
        boolean placeOccupied = LevelContainer.getPOS_SOLID_MAP().get(selectedNew.getPos()) != null
                || LevelContainer.getPOS_FLUID_MAP().get(selectedNew.getPos()) != null;
        //----------------------------------------------------------------------
        boolean intsSolid = false;
        int solidChunkId = Chunk.chunkFunc(selectedNew.getPos());
        Chunk solidChunk = levelContainer.getSolidChunks().getChunk(solidChunkId);

        if (solidChunk != null) {
            for (Tuple<Blocks, Integer, Integer, Texture, Integer> solidTuple : solidChunk.getTupleList()) {
                for (Block solidBlock : solidTuple.getA().getBlockList()) {
                    intsSolid = selectedNew.intersectsExactly(solidBlock);
                    if (intsSolid) {
                        break;
                    }
                }
            }
        }
        //----------------------------------------------------------------------
        boolean intsFluid = false;
        int fluidChunkId = Chunk.chunkFunc(selectedNew.getPos());
        Chunk fluidChunk = levelContainer.getFluidChunks().getChunk(fluidChunkId);

        if (fluidChunk != null) {
            for (Tuple<Blocks, Integer, Integer, Texture, Integer> fluidTuple : fluidChunk.getTupleList()) {
                for (Block fluidBlock : fluidTuple.getA().getBlockList()) {
                    intsFluid = selectedNew.intersectsExactly(fluidBlock);
                    if (intsFluid) {
                        break;
                    }
                }
            }
        }
        //----------------------------------------------------------------------
        boolean leavesSkybox = !levelContainer.getSkybox().containsExactly(selectedNew.getPos())
                || !levelContainer.getSkybox().intersectsExactly(selectedNew);
        if (selectedNew.isSolid()) {
            cant = levelContainer.maxSolidReached() || placeOccupied || intsSolid || intsFluid || leavesSkybox;
        } else {
            cant = levelContainer.maxFluidReached() || placeOccupied || intsSolid || intsFluid || leavesSkybox;
        }
        if (cant) {
            selectedNew.getSecondaryColor().x = 1.0f;
            selectedNew.getSecondaryColor().y = 0.0f;
            selectedNew.getSecondaryColor().z = 0.0f;
            selectedNew.getSecondaryColor().w = 1.0f;
            selectedNew.setSecondaryTexture(SELECTED_TEXTURE);
        }
        return cant;
    }

    public static void add(LevelContainer levelContainer) {
        if (selectedNew != null) {
            if (!cannotPlace(levelContainer) && !levelContainer.getLevelActors().getPlayer().getCamera().intersects(selectedNew)) {
                selectedNew.setSecondaryTexture(null);
                if (selectedNew.isSolid()) { // else if block is solid
                    levelContainer.getSolidChunks().addBlock(selectedNew); // add the block to the solid blocks                    
                    levelContainer.getSolidChunks().setBuffered(false);
                    levelContainer.getSoundFXPlayer().play(AudioFile.BLOCK_ADD, selectedNew.getPos());
                    //----------------------------------------------------------
                } else { // if block is fluid                    
                    levelContainer.getFluidChunks().addBlock(selectedNew); // add the block to the fluid blocks 
                    levelContainer.getFluidChunks().updateFluids(true);
                    levelContainer.getFluidChunks().setBuffered(false);
                    levelContainer.getSoundFXPlayer().play(AudioFile.BLOCK_ADD, selectedNew.getPos());
                    //----------------------------------------------------------                   
                }
                loaded = new Block(false);
                selectLoadedTexture();
            }
        }
        deselect();
    }

    public static void remove(LevelContainer levelContainer) {
        if (selectedCurr != null) {
            if (selectedCurr.isSolid()) {
                //--------------------------------------------------------------
                levelContainer.getSolidChunks().removeBlock(selectedCurr);
                levelContainer.getSolidChunks().setBuffered(false);
                levelContainer.getSoundFXPlayer().play(AudioFile.BLOCK_REMOVE, selectedCurr.getPos());
            } else {
                //--------------------------------------------------------------                
                levelContainer.getFluidChunks().removeBlock(selectedCurr);
                levelContainer.getFluidChunks().updateFluids(false);
                levelContainer.getFluidChunks().setBuffered(false);
                levelContainer.getSoundFXPlayer().play(AudioFile.BLOCK_REMOVE, selectedCurr.getPos());
            }
        }
        deselect();
    }

    private static void selectLoadedTexture() {
        Texture texture = null;
        if (loaded != null) {
            switch (value) {
                case 0:
                    texture = Texture.CRATE;
                    loaded.setSolid(true);
                    loaded.getPrimaryColor().w = 1.0f;
                    break;
                case 1:
                    texture = Texture.STONE;
                    loaded.setSolid(true);
                    loaded.getPrimaryColor().w = 1.0f;
                    break;
                case 2:
                    texture = Texture.WATER;
                    loaded.setSolid(false);
                    loaded.getPrimaryColor().w = 0.5f;
                    break;
                case 3:
                    texture = Texture.DOOM0;
                    loaded.setSolid(true);
                    loaded.getPrimaryColor().w = 1.0f;
                    break;
            }
            loaded.setPrimaryTexture(texture);
        }
    }

    public static void selectPrevTexture(LevelContainer levelContainer) {
        if (loaded != null) {
            if (value > MIN_VAL) {
                value--;
                selectLoadedTexture();
            }
        }
    }

    public static void selectNextTexture(LevelContainer levelContainer) {
        if (loaded != null) {
            if (value < MAX_VAL) {
                value++;
                selectLoadedTexture();
            }
        }
    }

    public static Block getSelectedNew() {
        return selectedNew;
    }

    public static Block getSelectedCurr() {
        return selectedCurr;
    }

    public static int getSelectedCurrIndex() {
        return selectedCurrIndex;
    }

    public static Texture getSELECTED_TEXTURE() {
        return SELECTED_TEXTURE;
    }

}
