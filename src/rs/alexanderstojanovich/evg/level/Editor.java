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

import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Chunk;

/**
 *
 * @author Coa
 */
public class Editor {

    private static Block loaded = null;

    private static Block selectedNew = null;

    private static Block selectedCurr = null;
    private static int selectedCurrIndex = -1;

    private static int value = 0; // value about which texture to use
    private static final int MIN_VAL = 0;
    private static final int MAX_VAL = 3;

    public static void selectNew(GameObject gameObject) {
        deselect();
        if (loaded == null) // first time it's null
        {
            loaded = new Block(false);
            selectLoadedTexture();
        }
        selectedNew = loaded;

        // fetching..
        Observer obs = gameObject.getLevelContainer().getLevelActors().getPlayer();
        Vector3f pos = obs.getCamera().getPos();
        Vector3f front = obs.getCamera().getFront();
        final float skyboxWidth = LevelContainer.SKYBOX_WIDTH;
        // initial calculation (make it dependant to point player looking at)
        // and make it follows player camera        
        selectedNew.getPos().x = (Math.round(8.0f * front.x) + Math.round(pos.x)) % Math.round(skyboxWidth + 1);
        selectedNew.getPos().y = (Math.round(8.0f * front.y) + Math.round(pos.y)) % Math.round(skyboxWidth + 1);
        selectedNew.getPos().z = (Math.round(8.0f * front.z) + Math.round(pos.z)) % Math.round(skyboxWidth + 1);

        if (!cannotPlace(gameObject)) {
            selectedNew.getSecondaryColor().x = 0.0f;
            selectedNew.getSecondaryColor().y = 1.0f;
            selectedNew.getSecondaryColor().z = 0.0f;
            selectedNew.setDecal(true);
        }

        gameObject.getSoundFXPlayer().play(AudioFile.BLOCK_SELECT, selectedNew.getPos());
    }

    public static void selectCurrSolid(GameObject gameObject) {
        deselect();
        Vector3f cameraPos = gameObject.getLevelContainer().getLevelActors().getPlayer().getCamera().getPos();
        Vector3f cameraFront = gameObject.getLevelContainer().getLevelActors().getPlayer().getCamera().getFront();
        float minDistanceOfSolid = Chunk.C;
        int currChunkId = Chunk.chunkFunc(cameraPos);
        Chunk currSolidChunk = gameObject.getLevelContainer().getSolidChunks().getChunk(currChunkId);

        int solidTargetIndex = -1;
        if (currSolidChunk != null) {
            int solidBlkIndex = 0;
            for (Block solidBlock : currSolidChunk.getList()) {
                if (Block.intersectsRay(solidBlock.getPos(), cameraFront, cameraPos)) {
                    float distance = Vector3f.distance(cameraPos.x, cameraPos.y, cameraPos.z,
                            solidBlock.getPos().x, solidBlock.getPos().y, solidBlock.getPos().z);
                    if (distance < minDistanceOfSolid) {
                        minDistanceOfSolid = distance;
                        solidTargetIndex = solidBlkIndex;
                    }
                }
                solidBlkIndex++;
            }

            if (solidTargetIndex != -1) {
                selectedCurr = currSolidChunk.getList().get(solidTargetIndex);

                selectedCurr.getSecondaryColor().x = 1.0f;
                selectedCurr.getSecondaryColor().y = 1.0f;
                selectedCurr.getSecondaryColor().z = 0.0f;

                selectedCurr.setDecal(true);

                selectedCurrIndex = solidBlkIndex;
            }
        }
    }

    public static void selectCurrFluid(GameObject gameObject) {
        deselect();
        Vector3f cameraPos = gameObject.getLevelContainer().getLevelActors().getPlayer().getCamera().getPos();
        Vector3f cameraFront = gameObject.getLevelContainer().getLevelActors().getPlayer().getCamera().getFront();
        float minDistanceOfFluid = Chunk.C;
        int currChunkId = Chunk.chunkFunc(cameraPos);
        Chunk currFluidChunk = gameObject.getLevelContainer().getFluidChunks().getChunk(currChunkId);

        int fluidTargetIndex = -1;
        if (currFluidChunk != null) {
            int fluidBlkIndex = 0;
            for (Block fluidBlock : currFluidChunk.getList()) {
                if (Block.intersectsRay(fluidBlock.getPos(), cameraFront, cameraPos)) {
                    float distance = Vector3f.distance(cameraPos.x, cameraPos.y, cameraPos.z,
                            fluidBlock.getPos().x, fluidBlock.getPos().y, fluidBlock.getPos().z);
                    if (distance < minDistanceOfFluid) {
                        minDistanceOfFluid = distance;
                        fluidTargetIndex = fluidBlkIndex;
                    }
                }
                fluidBlkIndex++;
            }

            if (fluidTargetIndex != -1) {
                selectedCurr = currFluidChunk.getList().get(fluidTargetIndex);

                selectedCurr.getSecondaryColor().x = 1.0f;
                selectedCurr.getSecondaryColor().y = 1.0f;
                selectedCurr.getSecondaryColor().z = 0.0f;

                selectedCurr.setDecal(true);

                Block.deepCopyTo(selectedCurr.getVertices());

                selectedCurrIndex = fluidBlkIndex;
            }
        }
    }

    public static void deselect() {
        if (selectedCurr != null) {
            selectedCurr.setDecal(false);
            selectedCurr.getSecondaryColor().x = 1.0f;
            selectedCurr.getSecondaryColor().y = 1.0f;
            selectedCurr.getSecondaryColor().z = 1.0f;
        }
        selectedNew = selectedCurr = null;
        selectedCurrIndex = -1;
    }

    public static void selectAdjacentSolid(GameObject gameObject, int position) {
        deselect();
        selectCurrSolid(gameObject);
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

            if (!cannotPlace(gameObject)) {
                selectedNew.getSecondaryColor().x = 0.0f;
                selectedNew.getSecondaryColor().y = 0.0f;
                selectedNew.getSecondaryColor().z = 1.0f;

                selectedNew.setDecal(true);
            }
        }
    }

    public static void selectAdjacentFluid(GameObject gameObject, int position) {
        deselect();
        selectCurrFluid(gameObject);
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

            if (!cannotPlace(gameObject)) {
                selectedNew.getSecondaryColor().x = 0.0f;
                selectedNew.getSecondaryColor().y = 0.0f;
                selectedNew.getSecondaryColor().z = 1.0f;
                selectedNew.setDecal(true);

                selectedNew.enableAllFaces(false);
            }
        }
    }

    private static boolean cannotPlace(GameObject gameObject) {
        boolean cant = false;
        boolean placeOccupied = LevelContainer.ALL_SOLID_POS.contains(selectedNew.getPos())
                || LevelContainer.ALL_FLUID_POS.contains(selectedNew.getPos());
        //----------------------------------------------------------------------
        boolean intsSolid = false;
        int currChunkId = Chunk.chunkFunc(selectedNew.getPos());
        Chunk currSolidChunk = gameObject.getLevelContainer().getSolidChunks().getChunk(currChunkId);
        if (currSolidChunk != null) {
            for (Block solidBlock : currSolidChunk.getList()) {
                intsSolid = selectedNew.intersectsExactly(solidBlock);
                if (intsSolid) {
                    break;
                }
            }
        }
        //----------------------------------------------------------------------
        boolean intsFluid = false;
        if (!intsSolid) {
            Chunk currFluidChunk = gameObject.getLevelContainer().getFluidChunks().getChunk(currChunkId);
            if (currFluidChunk != null) {
                for (Block fluidBlock : currFluidChunk.getList()) {
                    intsSolid = selectedNew.intersectsExactly(fluidBlock);
                    if (intsFluid) {
                        break;
                    }
                }
            }
        }
        //----------------------------------------------------------------------
        boolean leavesSkybox = !LevelContainer.SKYBOX.intersectsEqually(selectedNew);
        if (selectedNew.isSolid()) {
            cant = gameObject.getLevelContainer().maxSolidReached() || placeOccupied || intsSolid || intsFluid || leavesSkybox;
        } else {
            cant = gameObject.getLevelContainer().maxFluidReached() || placeOccupied || intsSolid || intsFluid || leavesSkybox;
        }
        if (cant) {
            selectedNew.getSecondaryColor().x = 1.0f;
            selectedNew.getSecondaryColor().y = 0.0f;
            selectedNew.getSecondaryColor().z = 0.0f;
            selectedNew.setDecal(true);
        }
        return cant;
    }

    public static void add(GameObject gameObject) {
        if (selectedNew != null) {
            if (!cannotPlace(gameObject) && !gameObject.getLevelContainer().getLevelActors().getPlayer().getCamera().intersects(selectedNew)) {
                selectedNew.setDecal(false);
                int currentChunkId = Chunk.chunkFunc(selectedNew.getPos());
                if (selectedNew.isSolid()) { // else if block is solid
                    Chunk solidChunk = gameObject.getLevelContainer().getSolidChunks().getChunk(currentChunkId);
                    if (solidChunk != null && !solidChunk.isCached()) {
                        solidChunk.addBlock(selectedNew);
                        gameObject.getSoundFXPlayer().play(AudioFile.BLOCK_ADD, selectedNew.getPos());
                    }
                    //----------------------------------------------------------
                } else { // if block is fluid                    
                    Chunk fluidChunk = gameObject.getLevelContainer().getFluidChunks().getChunk(currentChunkId);
                    if (fluidChunk != null && !fluidChunk.isCached()) {
                        fluidChunk.addBlock(selectedNew);
                        gameObject.getLevelContainer().getFluidChunks().updateFluids(fluidChunk, true);
                        gameObject.getSoundFXPlayer().play(AudioFile.BLOCK_ADD, selectedNew.getPos());
                    }
                    //----------------------------------------------------------                   
                }
                loaded = new Block(false);
                selectLoadedTexture();
            }
        }
        deselect();
    }

    public static void remove(GameObject gameObject) {
        if (selectedCurr != null) {
            int currentChunkId = Chunk.chunkFunc(selectedCurr.getPos());
            if (selectedCurr.isSolid()) {
                //--------------------------------------------------------------
                Chunk solidChunk = gameObject.getLevelContainer().getSolidChunks().getChunk(currentChunkId);
                if (solidChunk != null && !solidChunk.isCached()) {
                    solidChunk.removeBlock(selectedCurr);
                    gameObject.getSoundFXPlayer().play(AudioFile.BLOCK_REMOVE, selectedCurr.getPos());
                }
            } else {
                Chunk fluidChunk = gameObject.getLevelContainer().getFluidChunks().getChunk(currentChunkId);
                if (fluidChunk != null && !fluidChunk.isCached()) {
                    fluidChunk.removeBlock(selectedCurr);
                    gameObject.getLevelContainer().getFluidChunks().updateFluids(fluidChunk, true);
                    gameObject.getSoundFXPlayer().play(AudioFile.BLOCK_REMOVE, selectedCurr.getPos());
                }
            }
        }
        deselect();
    }

    private static void selectLoadedTexture() {
        String texName = null;
        if (loaded != null) {
            switch (value) {
                case 0:
                    texName = "crate";
                    loaded.setSolid(true);
                    break;
                case 1:
                    texName = "stone";
                    loaded.setSolid(true);
                    break;
                case 2:
                    texName = "water";
                    loaded.setSolid(false);
                    break;
                case 3:
                    texName = "doom0";
                    loaded.setSolid(true);
                    break;
            }
            loaded.setTexName(texName);
        }
    }

    public static void selectPrevTexture(GameObject gameObject) {
        if (loaded != null) {
            if (value > MIN_VAL) {
                value--;
                selectLoadedTexture();
            }
        }
    }

    public static void selectNextTexture(GameObject gameObject) {
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

}
