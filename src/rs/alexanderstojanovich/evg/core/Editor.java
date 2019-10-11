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

import java.util.Collections;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Block;

/**
 *
 * @author Coa
 */
public class Editor {

    private static Block loaded = null;
    public static Block selectedNew = null;
    private static Block selectedCurr = null;
    private static final Texture SELECTED_TEXTURE = new Texture("minigun.png");

    private static int value = 0; // value about which texture to use
    private static final int MIN_VAL = 0;
    private static final int MAX_VAL = 3;

    public static void selectNew(LevelRenderer levelRenderer) {
        deselect();
        if (loaded == null) // first time it's null
        {
            loaded = new Block(levelRenderer.getShaderProgram());
            selectLoadedTexture();
        }
        selectedNew = loaded;

        Vector3f frontBuff = new Vector3f();
        frontBuff.x = 2.0f * levelRenderer.getObserver().getCamera().getFront().x * selectedNew.getWidth();
        frontBuff.y = 2.0f * levelRenderer.getObserver().getCamera().getFront().y * selectedNew.getHeight();
        frontBuff.z = 2.0f * levelRenderer.getObserver().getCamera().getFront().z * selectedNew.getDepth();
        Vector3f temp = new Vector3f();
        Vector3f vect = levelRenderer.getObserver().getCamera().getPos().add(frontBuff, temp);

        selectedNew.getPos().x = vect.x;
        selectedNew.getPos().y = vect.y;
        selectedNew.getPos().z = vect.z;

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
        if (!levelRenderer.getSolidBlocks().isEmpty() || !levelRenderer.getFluidBlocks().isEmpty()) {
            Vector3f cameraPos = levelRenderer.getObserver().getCamera().getPos();
            float minDistanceOfSolid = Float.POSITIVE_INFINITY;
            float minDistanceOfFluid = Float.POSITIVE_INFINITY;

            Block minSolid = null;
            Block minFluid = null;
            for (Block solidBlock : levelRenderer.getSolidBlocks()) {
                Vector3f vect = solidBlock.getPos();
                float distance = Vector3f.distance(cameraPos.x, cameraPos.y, cameraPos.z, vect.x, vect.y, vect.z);
                if (solidBlock.intersectsRay(
                        levelRenderer.getObserver().getCamera().getFront(),
                        levelRenderer.getObserver().getCamera().getPos())) {
                    if (distance < minDistanceOfSolid) {
                        minDistanceOfSolid = distance;
                        minSolid = solidBlock;
                    }
                }
            }

            for (Block fluidBlock : levelRenderer.getFluidBlocks()) {
                Vector3f vect = fluidBlock.getPos();
                float distance = Vector3f.distance(cameraPos.x, cameraPos.y, cameraPos.z, vect.x, vect.y, vect.z);
                if (fluidBlock.intersectsRay(
                        levelRenderer.getObserver().getCamera().getFront(),
                        levelRenderer.getObserver().getCamera().getPos())) {
                    if (distance < minDistanceOfFluid) {
                        minDistanceOfFluid = distance;
                        minFluid = fluidBlock;
                    }
                }
            }

            if (minDistanceOfSolid < minDistanceOfFluid) {
                if (minSolid != null) {
                    selectedCurr = minSolid;
                    selectedCurr.getSecondaryColor().x = 1.0f;
                    selectedCurr.getSecondaryColor().y = 1.0f;
                    selectedCurr.getSecondaryColor().z = 0.0f;
                    selectedCurr.getSecondaryColor().w = 1.0f;
                    selectedCurr.setSecondaryTexture(SELECTED_TEXTURE);
                }
            } else if (minDistanceOfSolid >= minDistanceOfFluid) {
                if (minFluid != null) {
                    selectedCurr = minFluid;
                    selectedCurr.getSecondaryColor().x = 1.0f;
                    selectedCurr.getSecondaryColor().y = 1.0f;
                    selectedCurr.getSecondaryColor().z = 0.0f;
                    selectedCurr.getSecondaryColor().w = 1.0f;
                    selectedCurr.setSecondaryTexture(SELECTED_TEXTURE);
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
    }

    public static void selectAdjacent(LevelRenderer levelRenderer, int position) {
        deselect();
        selectCurr(levelRenderer);
        if (selectedCurr != null) {
            if (loaded == null) // first time it's null
            {
                loaded = new Block(levelRenderer.getShaderProgram());
                selectLoadedTexture();
            }
            selectedNew = loaded;
            selectedNew.getPos().x = selectedCurr.getPos().x;
            selectedNew.getPos().y = selectedCurr.getPos().y;
            selectedNew.getPos().z = selectedCurr.getPos().z;

            switch (position) {
                default:
                case Block.LEFT:
                    selectedNew.getPos().x += selectedCurr.getWidth() / 2.0f + selectedNew.getWidth() / 2.0f + Game.EPSILON;
                    break;
                case Block.RIGHT:
                    selectedNew.getPos().x -= selectedCurr.getWidth() / 2.0f + selectedNew.getWidth() / 2.0f + Game.EPSILON;
                    break;
                case Block.BOTTOM:
                    selectedNew.getPos().y += selectedCurr.getHeight() / 2.0f + selectedNew.getHeight() / 2.0f + Game.EPSILON;
                    break;
                case Block.TOP:
                    selectedNew.getPos().y -= selectedCurr.getHeight() / 2.0f + selectedNew.getHeight() / 2.0f + Game.EPSILON;
                    break;
                case Block.BACK:
                    selectedNew.getPos().z += selectedCurr.getDepth() / 2.0f + selectedNew.getDepth() / 2.0f + Game.EPSILON;
                    break;
                case Block.FRONT:
                    selectedNew.getPos().z -= selectedCurr.getDepth() / 2.0f + selectedNew.getDepth() / 2.0f + Game.EPSILON;
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
        for (int i = 0; i < levelRenderer.getSolidBlocks().size() && !intsSolid; i++) {
            intsSolid = selectedNew.intersects(levelRenderer.getSolidBlocks().get(i));
        }

        boolean intsFluid = false;
        for (int j = 0; j < levelRenderer.getFluidBlocks().size() && !intsFluid; j++) {
            intsFluid = selectedNew.intersects(levelRenderer.getFluidBlocks().get(j));
        }

        cant = placeOccupied || intsSolid || intsFluid;
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
                if (selectedNew.isPassable()) {
                    levelRenderer.getFluidBlocks().add(selectedNew);
                    Collections.shuffle(levelRenderer.getFluidBlocks());
                    Collections.sort(levelRenderer.getFluidBlocks());
                    levelRenderer.updateFluids();
                } else {
                    levelRenderer.getSolidBlocks().add(selectedNew);
                    Collections.shuffle(levelRenderer.getSolidBlocks());
                    Collections.sort(levelRenderer.getSolidBlocks());
                }
                loaded = new Block(levelRenderer.getShaderProgram());
                selectLoadedTexture();
            }
        }
        deselect();
    }

    public static void remove(LevelRenderer levelRenderer) {
        if (selectedCurr != null) {
            if (selectedCurr.isPassable()) {
                levelRenderer.getFluidBlocks().remove(selectedCurr);
                levelRenderer.updateFluids();
            } else {
                levelRenderer.getSolidBlocks().remove(selectedCurr);
            }
        }
        deselect();
    }

    private static void selectLoadedTexture() {
        Texture texture = null;
        if (loaded != null) {
            switch (value) {
                case 0:
                    texture = new Texture("crate.png");
                    loaded.setPassable(false);
                    loaded.getPrimaryColor().w = 1.0f;
                    break;
                case 1:
                    texture = new Texture("stone.png");
                    loaded.setPassable(false);
                    loaded.getPrimaryColor().w = 1.0f;
                    break;
                case 2:
                    texture = new Texture("water.png");
                    loaded.setPassable(true);
                    loaded.getPrimaryColor().w = 0.5f;
                    break;
                case 3:
                    texture = new Texture("doom0.png");
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

}
