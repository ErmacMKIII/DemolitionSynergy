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
package rs.alexanderstojanovich.evg.level;

import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Editor {

    private static Block selectedNew = null;
    private static int blockColorNum = 0;

    private static Block selectedCurr = null;
    private static int selectedCurrIndex = -1;

    private static int texValue = 0; // value about which texture to use

    private static Block selectedNewDecal = null;
    private static Block selectedCurrDecal = null;

    public static void selectNew() {
        deselect();
        if (selectedNew == null) {
            selectedNew = new Block("crate");
        }
        selectTexture();
        // fetching..
        Camera camera = GameObject.getLevelContainer().levelActors.mainCamera();
        Vector3f pos = camera.getPos();
        Vector3f front = camera.getFront();

        final float skyboxWidth = LevelContainer.SKYBOX_WIDTH;
        // initial calculation (make it dependant to point player looking at)
        // and make it follows player camera        
        selectedNew.getPos().x = (Math.round(8.0f * front.x) + Math.round(pos.x) & 0xFFFFFFFE) % Math.round(skyboxWidth + 1);
        selectedNew.getPos().y = (Math.round(8.0f * front.y) + Math.round(pos.y) & 0xFFFFFFFE) % Math.round(skyboxWidth + 1);
        selectedNew.getPos().z = (Math.round(8.0f * front.z) + Math.round(pos.z) & 0xFFFFFFFE) % Math.round(skyboxWidth + 1);

        if (!cannotPlace()) {
            selectedNewDecal = new Block("decal", new Vector3f(selectedNew.getPos()), GlobalColors.GREEN_RGBA, true);
        }

        GameObject.getSoundFXPlayer().play(AudioFile.BLOCK_SELECT, selectedNew.getPos());
    }

    public static void selectCurrSolid() {
        deselect();
        Vector3f cameraPos = GameObject.getLevelContainer().levelActors.mainCamera().getPos();
        Vector3f cameraFront = GameObject.getLevelContainer().levelActors.mainCamera().getFront();
        float minDistanceOfSolid = Chunk.VISION;
        int currChunkId = Chunk.chunkFunc(cameraPos);
        Chunk chunk = GameObject.getLevelContainer().chunks.getChunk(currChunkId);

        int solidTargetIndex = -1;
        if (chunk != null) {
            int solidBlkIndex = 0;
            for (Block blk : chunk.getBlockList()) {
                if (blk.isSolid() && Block.intersectsRay(blk.getPos(), cameraFront, cameraPos)) {
                    float distance = Vector3f.distance(cameraPos.x, cameraPos.y, cameraPos.z,
                            blk.getPos().x, blk.getPos().y, blk.getPos().z);
                    if (distance < minDistanceOfSolid
                            && !Model.intersectsEqually(cameraPos, 2.0f, 2.0f, 2.0f, blk.pos, 2.0f, 2.0f, 2.0f)) {
                        minDistanceOfSolid = distance;
                        solidTargetIndex = solidBlkIndex;
                    }
                }
                solidBlkIndex++;
            }

            if (solidTargetIndex != -1) {
                selectedCurr = chunk.getBlockList().get(solidTargetIndex);
                selectedCurrIndex = solidBlkIndex;
                selectedCurrDecal = new Block("decal", new Vector3f(selectedCurr.getPos()), GlobalColors.YELLOW_RGBA, true);
            }
        }

    }

    public static void selectCurrFluid() {
        deselect();
        Vector3f cameraPos = GameObject.getLevelContainer().levelActors.mainCamera().getPos();
        Vector3f cameraFront = GameObject.getLevelContainer().levelActors.mainCamera().getFront();
        float minDistanceOfFluid = Chunk.VISION;
        int currChunkId = Chunk.chunkFunc(cameraPos);
        Chunk currFluidChunk = GameObject.getLevelContainer().chunks.getChunk(currChunkId);

        int fluidTargetIndex = -1;
        if (currFluidChunk != null) {
            int fluidBlkIndex = 0;
            for (Block blk : currFluidChunk.getBlockList()) {
                if (!blk.isSolid() && Block.intersectsRay(blk.getPos(), cameraFront, cameraPos)) {
                    float distance = Vector3f.distance(cameraPos.x, cameraPos.y, cameraPos.z,
                            blk.getPos().x, blk.getPos().y, blk.getPos().z);
                    if (distance < minDistanceOfFluid
                            && !Model.intersectsEqually(cameraPos, 2.0f, 2.0f, 2.0f, blk.getPos(), 2.0f, 2.0f, 2.0f)) {
                        minDistanceOfFluid = distance;
                        fluidTargetIndex = fluidBlkIndex;
                    }
                }
                fluidBlkIndex++;
            }

            if (fluidTargetIndex != -1) {
                selectedCurr = currFluidChunk.getBlockList().get(fluidTargetIndex);
                selectedCurrIndex = fluidBlkIndex;
                selectedCurrDecal = new Block("decal", new Vector3f(selectedCurr.getPos()), GlobalColors.YELLOW_RGBA, true);
            }
        }
    }

    public static void deselect() {
        selectedNew = selectedCurr = null;
        selectedCurrIndex = -1;
        selectedNewDecal = null;
        selectedCurrDecal = null;
    }

    public static void selectAdjacentSolid(int position) {
        deselect();
        selectCurrSolid();
        if (selectedCurr != null) {
            if (selectedNew == null) {
                selectedNew = new Block("crate");
            }
            selectTexture();
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

            if (!cannotPlace()) {
                selectedNewDecal = new Block("decal", new Vector3f(selectedNew.getPos()), GlobalColors.BLUE_RGBA, true);
            }
        }
    }

    public static void selectAdjacentFluid(int position) {
        deselect();
        selectCurrFluid();
        if (selectedCurr != null) {
            if (selectedNew == null) {
                selectedNew = new Block("crate");
            }
            selectTexture();
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

            if (!cannotPlace()) {
                selectedNewDecal = new Block("decal", new Vector3f(selectedNew.getPos()), GlobalColors.BLUE_RGBA, true);
            }
        }
    }

    private static boolean cannotPlace() {
        boolean cant = false;
        boolean placeOccupied = LevelContainer.ALL_BLOCK_MAP.isLocationPopulated(selectedNew.pos);
        //----------------------------------------------------------------------
        boolean intersects = false;
        int currChunkId = Chunk.chunkFunc(selectedNew.getPos());
        Chunk currSolidChunk = GameObject.getLevelContainer().chunks.getChunk(currChunkId);
        if (currSolidChunk != null) {
            for (Block solidBlock : currSolidChunk.getBlockList()) {
                intersects = selectedNew.intersectsExactly(solidBlock);
                if (intersects) {
                    break;
                }
            }
        }
        //----------------------------------------------------------------------
        boolean leavesSkybox = !LevelContainer.SKYBOX.intersectsEqually(selectedNew);
        if (selectedNew.isSolid()) {
            cant = GameObject.getLevelContainer().maxCountReached() || placeOccupied || intersects || leavesSkybox;
        }
        if (cant) {
            selectedNewDecal = new Block("decal", new Vector3f(selectedNew.getPos()), GlobalColors.RED_RGBA, true);
        }
        return cant;
    }

    public static void add() {
        if (selectedNew != null) {
            if (!cannotPlace() && !GameObject.getLevelContainer().levelActors.mainCamera().intersects(selectedNew)) {
                synchronized (GameObject.UPDATE_RENDER_MUTEX) { // potentially dangerous
                    GameObject.getLevelContainer().chunks.addBlock(selectedNew);
                }
                GameObject.getSoundFXPlayer().play(AudioFile.BLOCK_ADD, selectedNew.getPos());
                selectedNew = new Block(Texture.TEX_WORLD[texValue]);
            }
        }
        deselect();
    }

    public static void remove() {
        if (selectedCurr != null) {
            synchronized (GameObject.UPDATE_RENDER_MUTEX) { // potentially dangerous
                GameObject.getLevelContainer().chunks.removeBlock(selectedCurr);
            }
            GameObject.getSoundFXPlayer().play(AudioFile.BLOCK_REMOVE, selectedCurr.getPos());
        }
        deselect();
    }

    private static void selectTexture() {
        if (selectedNew != null) {
            synchronized (GameObject.UPDATE_RENDER_MUTEX) {
                String texName = Texture.TEX_WORLD[texValue];
                selectedNew.setTexNameWithDeepCopy(texName);
            }
        }
    }

    public static void selectPrevTexture() {
        if (selectedNew != null) {
            if (texValue > 0) {
                texValue--;
                selectTexture();
            }
        }
    }

    public static void selectNextTexture() {
        if (selectedNew != null) {
            if (texValue < Texture.TEX_WORLD.length - 1) {
                texValue++;
                selectTexture();
            }
        }
    }

    public static void cycleBlockColor() {
        if (selectedNew != null) {
            GlobalColors.ColorName[] values = GlobalColors.ColorName.values();
            selectedNew.setPrimaryRGBAColor(new Vector4f(GlobalColors.getRGBAColorOrDefault(values[++blockColorNum % values.length])));
        }
    }

    public static Block getSelectedNew() {
        return selectedNew;
    }

    public static Block getSelectedCurr() {
        return selectedCurr;
    }

    public static int getBlockColorNum() {
        return blockColorNum;
    }

    public static int getSelectedCurrIndex() {
        return selectedCurrIndex;
    }

    public static Block getSelectedNewDecal() {
        return selectedNewDecal;
    }

    public static Block getSelectedCurrDecal() {
        return selectedCurrDecal;
    }

}
