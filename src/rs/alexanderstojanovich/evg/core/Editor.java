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
package rs.alexanderstojanovich.evg.core;

import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Blocks;
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

    public static void selectNew(LevelRenderer levelRenderer) {
        deselect();
        if (loaded == null) // first time it's null
        {
            loaded = new Block(false);
            selectLoadedTexture();
        }
        selectedNew = loaded;

        // fetching..
        Critter obs = levelRenderer.getObserver();
        Vector3f pos = obs.getCamera().getPos();
        Vector3f front = obs.getCamera().getFront();
        final float skyboxWidth = LevelRenderer.SKYBOX_WIDTH;
        // initial calculation (make it dependant to point player looking at)
        // and make it follows player camera        
        selectedNew.getPos().x = (Math.round(8.0f * front.x) + Math.round(pos.x)) % Math.round(2.0f * skyboxWidth);
        selectedNew.getPos().y = (Math.round(8.0f * front.y) + Math.round(pos.y)) % Math.round(2.0f * skyboxWidth);
        selectedNew.getPos().z = (Math.round(8.0f * front.z) + Math.round(pos.z)) % Math.round(2.0f * skyboxWidth);

        if (!cannotPlace(levelRenderer)) {
            selectedNew.getSecondaryColor().x = 0.0f;
            selectedNew.getSecondaryColor().y = 1.0f;
            selectedNew.getSecondaryColor().z = 0.0f;
            selectedNew.getSecondaryColor().w = 1.0f;
            selectedNew.setSecondaryTexture(SELECTED_TEXTURE);
        }
    }

    public static void selectCurr(LevelRenderer levelRenderer) {
        deselect(); // algorithm is select the nearest that interesects the camera ray
        if (!levelRenderer.getSolidBlocks().getBlockList().isEmpty() || !levelRenderer.getFluidBlocks().getBlockList().isEmpty()) {
            Vector3f cameraPos = levelRenderer.getObserver().getCamera().getPos();
            float minDistanceOfSolid = Float.POSITIVE_INFINITY;
            float minDistanceOfFluid = Float.POSITIVE_INFINITY;

            Block minSolid = null;
            Block minFluid = null;
            int minSolidBlkIndex = -1;
            int solidBlkIndex = 0;
            for (Block solidBlock : levelRenderer.getSolidBlocks().getBlockList()) {
                Vector3f vect = solidBlock.getPos();
                float distance = Vector3f.distance(cameraPos.x, cameraPos.y, cameraPos.z, vect.x, vect.y, vect.z);
                if (solidBlock.intersectsRay(
                        levelRenderer.getObserver().getCamera().getFront(),
                        levelRenderer.getObserver().getCamera().getPos())) {
                    if (distance < minDistanceOfSolid) {
                        minDistanceOfSolid = distance;
                        minSolid = solidBlock;
                        minSolidBlkIndex = solidBlkIndex;
                    }
                }
                solidBlkIndex++;
            }

            int minFluidBlkIndex = -1;
            int fluidBlkIndex = 0;
            for (Block fluidBlock : levelRenderer.getFluidBlocks().getBlockList()) {
                Vector3f vect = fluidBlock.getPos();
                float distance = Vector3f.distance(cameraPos.x, cameraPos.y, cameraPos.z, vect.x, vect.y, vect.z);
                if (fluidBlock.intersectsRay(
                        levelRenderer.getObserver().getCamera().getFront(),
                        levelRenderer.getObserver().getCamera().getPos())) {
                    if (distance < minDistanceOfFluid) {
                        minDistanceOfFluid = distance;
                        minFluid = fluidBlock;
                        minFluidBlkIndex = fluidBlkIndex;
                    }
                }
                fluidBlkIndex++;
            }

            if (minDistanceOfSolid < minDistanceOfFluid) {
                if (minSolid != null) {
                    selectedCurr = minSolid;
                    selectedCurr.getSecondaryColor().x = 1.0f;
                    selectedCurr.getSecondaryColor().y = 1.0f;
                    selectedCurr.getSecondaryColor().z = 0.0f;
                    selectedCurr.getSecondaryColor().w = 1.0f;
                    selectedCurr.setSecondaryTexture(SELECTED_TEXTURE);
                    selectedCurrIndex = minSolidBlkIndex;
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
                }
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

    public static void selectAdjacent(LevelRenderer levelRenderer, int position) {
        deselect();
        selectCurr(levelRenderer);
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
                    selectedNew.getPos().x += selectedCurr.getWidth() / 2.0f + selectedNew.getWidth() / 2.0f;
                    break;
                case Block.RIGHT:
                    selectedNew.getPos().x -= selectedCurr.getWidth() / 2.0f + selectedNew.getWidth() / 2.0f;
                    break;
                case Block.BOTTOM:
                    selectedNew.getPos().y += selectedCurr.getHeight() / 2.0f + selectedNew.getHeight() / 2.0f;
                    break;
                case Block.TOP:
                    selectedNew.getPos().y -= selectedCurr.getHeight() / 2.0f + selectedNew.getHeight() / 2.0f;
                    break;
                case Block.BACK:
                    selectedNew.getPos().z += selectedCurr.getDepth() / 2.0f + selectedNew.getDepth() / 2.0f;
                    break;
                case Block.FRONT:
                    selectedNew.getPos().z -= selectedCurr.getDepth() / 2.0f + selectedNew.getDepth() / 2.0f;
                    break;
                default:
                    break;
            }

            if (!cannotPlace(levelRenderer)) {
                selectedNew.getSecondaryColor().x = 0.0f;
                selectedNew.getSecondaryColor().y = 0.0f;
                selectedNew.getSecondaryColor().z = 1.0f;
                selectedNew.getSecondaryColor().w = 1.0f;
                selectedNew.setSecondaryTexture(SELECTED_TEXTURE);
            }
        }
    }

    private static boolean cannotPlace(LevelRenderer levelRenderer) {
        boolean cant = false;
        boolean placeOccupied = levelRenderer.isPlaceOccupiedBySolid(selectedNew.getPos())
                || levelRenderer.isPlaceOccupiedByFluid(selectedNew.getPos());
        boolean intsSolid = false;
        for (int i = 0; i < levelRenderer.getSolidBlocks().getBlockList().size() && !intsSolid; i++) {
            intsSolid = selectedNew.intersectsExactly(levelRenderer.getSolidBlocks().getBlockList().get(i));
        }

        boolean intsFluid = false;
        for (int j = 0; j < levelRenderer.getFluidBlocks().getBlockList().size() && !intsFluid; j++) {
            intsFluid = selectedNew.intersectsExactly(levelRenderer.getFluidBlocks().getBlockList().get(j));
        }

        boolean leavesSkybox = !levelRenderer.getSkybox().containsExactly(selectedNew.getPos())
                || !levelRenderer.getSkybox().intersectsExactly(selectedNew);
        cant = placeOccupied || intsSolid || intsFluid || leavesSkybox;
        if (cant) {
            selectedNew.getSecondaryColor().x = 1.0f;
            selectedNew.getSecondaryColor().y = 0.0f;
            selectedNew.getSecondaryColor().z = 0.0f;
            selectedNew.getSecondaryColor().w = 1.0f;
            selectedNew.setSecondaryTexture(SELECTED_TEXTURE);
        }
        return cant;
    }

    public static void add(LevelRenderer levelRenderer) {
        if (selectedNew != null) {
            if (!cannotPlace(levelRenderer) && !levelRenderer.getObserver().getCamera().intersects(selectedNew)) {
                selectedNew.setSecondaryTexture(null);
                if (selectedNew.isPassable()) { // if block is solid
                    levelRenderer.getFluidBlocks().getBlockList().add(selectedNew); // add the block to the fluid blocks
                    levelRenderer.updateFluidNeighbors();
                    levelRenderer.updateFluidToSolidNeighbors();
                    levelRenderer.updateFluids();
                    //----------------------------------------------------------
                    int indexOfSeries = levelRenderer.getFluidSeries().indexOfSeries(
                            selectedNew.getPrimaryTexture(),
                            selectedNew.getFaceBits()
                    );
                    if (indexOfSeries == -1) { // make new tuple and add the block
                        Tuple<Blocks, Integer, Integer, Texture, Integer> tuple = new Tuple<>(
                                new Blocks(), 0, 0, selectedNew.getPrimaryTexture(), selectedNew.getFaceBits()
                        );
                        tuple.getA().getBlockList().add(selectedNew);
                        levelRenderer.getFluidSeries().getBlocksSeries().add(tuple);
                    } else { // add the block to the existing tuple
                        levelRenderer.getFluidSeries().getBlocksSeries().get(indexOfSeries).getA().getBlockList().add(selectedNew);
                    }
                    levelRenderer.getFluidSeries().setBuffered(false);
                } else { // else if block is fluid
                    levelRenderer.getSolidBlocks().getBlockList().add(selectedNew); // add the block to the solid blocks                   
                    levelRenderer.updateSolidNeighbors();
                    levelRenderer.updateSolidToFluidNeighbors();
                    //----------------------------------------------------------
                    int indexOfSeries = levelRenderer.getSolidSeries().indexOfSeries(
                            selectedNew.getPrimaryTexture(),
                            selectedNew.getFaceBits()
                    );
                    if (indexOfSeries == -1) { // make new tuple and add the block
                        Tuple<Blocks, Integer, Integer, Texture, Integer> tuple = new Tuple<>(
                                new Blocks(), 0, 0, selectedNew.getPrimaryTexture(), selectedNew.getFaceBits()
                        );
                        tuple.getA().getBlockList().add(selectedNew);
                        levelRenderer.getSolidSeries().getBlocksSeries().add(tuple);
                    } else { // add the block to the existing tuple
                        levelRenderer.getSolidSeries().getBlocksSeries().get(indexOfSeries).getA().getBlockList().add(selectedNew);
                    }
                    levelRenderer.getSolidSeries().setBuffered(false);
                }
                loaded = new Block(false);
                selectLoadedTexture();
            }
        }
        deselect();
    }

    public static void remove(LevelRenderer levelRenderer) {
        if (selectedCurr != null) {
            for (int i = 0; i <= 5; i++) {
                Block otherBlock = selectedCurr.getAdjacentBlockMap().get(i);
                if (otherBlock != null) {
                    if (i % 2 == 0) {
                        otherBlock.getAdjacentBlockMap().remove(i + 1);
                    } else {
                        otherBlock.getAdjacentBlockMap().remove(i - 1);
                    }
                }
            }
            if (selectedCurr.isPassable()) {
                levelRenderer.getFluidBlocks().getBlockList().remove(selectedCurr);
                levelRenderer.updateFluids();
                //--------------------------------------------------------------
                int indexOfSeries = levelRenderer.getFluidSeries().indexOfSeries(
                        selectedCurr.getPrimaryTexture(),
                        selectedCurr.getFaceBits()
                );
                if (indexOfSeries != -1) { // find the tuple and remove the block
                    Tuple<Blocks, Integer, Integer, Texture, Integer> tuple = levelRenderer.getFluidSeries().getBlocksSeries().get(indexOfSeries);
                    tuple.getA().getBlockList().remove(selectedCurr);
                    if (tuple.getA().getBlockList().isEmpty()) {
                        levelRenderer.getFluidSeries().getBlocksSeries().remove(tuple);
                    }
                }
                levelRenderer.getFluidSeries().setBuffered(false);
            } else {
                levelRenderer.getSolidBlocks().getBlockList().remove(selectedCurr);
                //--------------------------------------------------------------
                int indexOfSeries = levelRenderer.getSolidSeries().indexOfSeries(
                        selectedCurr.getPrimaryTexture(),
                        selectedCurr.getFaceBits()
                );
                if (indexOfSeries != -1) { // find the tuple and remove the block
                    Tuple<Blocks, Integer, Integer, Texture, Integer> tuple = levelRenderer.getSolidSeries().getBlocksSeries().get(indexOfSeries);
                    tuple.getA().getBlockList().remove(selectedCurr);
                    if (tuple.getA().getBlockList().isEmpty()) {
                        levelRenderer.getSolidSeries().getBlocksSeries().remove(tuple);
                    }
                }
                levelRenderer.getSolidSeries().setBuffered(false);
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
                    loaded.setPassable(false);
                    loaded.getPrimaryColor().w = 1.0f;
                    break;
                case 1:
                    texture = Texture.STONE;
                    loaded.setPassable(false);
                    loaded.getPrimaryColor().w = 1.0f;
                    break;
                case 2:
                    texture = Texture.WATER;
                    loaded.setPassable(true);
                    loaded.getPrimaryColor().w = 0.5f;
                    break;
                case 3:
                    texture = Texture.DOOM0;
                    loaded.setPassable(false);
                    loaded.getPrimaryColor().w = 1.0f;
                    break;
            }
            loaded.setPrimaryTexture(texture);
        }
    }

    public static void selectPrevTexture(LevelRenderer levelRenderer) {
        if (loaded != null) {
            if (value > MIN_VAL) {
                value--;
                selectLoadedTexture();
            }
        }
    }

    public static void selectNextTexture(LevelRenderer levelRenderer) {
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
