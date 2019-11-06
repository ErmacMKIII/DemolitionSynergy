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

import org.joml.Random;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import rs.alexanderstojanovich.evg.models.Block;

/**
 *
 * @author Coa
 */
public class RandomLevelGenerator {

    private final int POS_MAX = Math.round(LevelRenderer.SKYBOX_WIDTH);
    private final int POS_MIN = Math.round(-LevelRenderer.SKYBOX_WIDTH);

    private final Random RANDOM = new Random(0x123456789L);

    private final LevelRenderer levelRenderer;

    private int numberOfBlocks = 0;

    public RandomLevelGenerator(LevelRenderer levelRenderer) {
        this.levelRenderer = levelRenderer;
    }

    public RandomLevelGenerator(LevelRenderer levelRenderer, int numberOfBlocks) {
        this.levelRenderer = levelRenderer;
        this.numberOfBlocks = numberOfBlocks;
    }

    private Texture randomSolidTexture() {
        int randTexture = RANDOM.nextInt(3);
        switch (randTexture) {
            case 0:
                return Texture.STONE;
            case 1:
                return Texture.CRATE;
            case 2:
                return Texture.DOOM0;
        }
        return null;
    }

    private Block generateRandomSolidBlock() {
        float posx;
        float posy;
        float posz;
        Vector3f randPos;
        do {
            posx = 2.0f * (RANDOM.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN) % Math.round(2.0f * LevelRenderer.SKYBOX_WIDTH);
            posy = 2.0f * (RANDOM.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN) % Math.round(2.0f * LevelRenderer.SKYBOX_WIDTH);
            posz = 2.0f * (RANDOM.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN) % Math.round(2.0f * LevelRenderer.SKYBOX_WIDTH);
            randPos = new Vector3f(posx, posy, posz);
        } while (levelRenderer.getPosSolidMap().get(randPos) != null
                || levelRenderer.getPosFluidMap().get(randPos) != null
                || levelRenderer.getObserver().getModel().contains(randPos)
                || levelRenderer.getObserver().getCamera().getPos().equals(randPos));
        float colx = RANDOM.nextFloat();
        float coly = RANDOM.nextFloat();
        float colz = RANDOM.nextFloat();
        Vector3f pos = randPos;
        Vector4f col = new Vector4f(colx, coly, colz, 1.0f);
        Block solidBlock = new Block(false, randomSolidTexture(), pos, col, false);
        levelRenderer.getPosSolidMap().put(solidBlock.getPos(), solidBlock.hashCode());
        levelRenderer.getSolidBlocks().getBlockList().add(solidBlock);
        return solidBlock;
    }

    private Block generateRandomFluidBlock() {
        float posx;
        float posy;
        float posz;
        Vector3f randPos;
        do {
            posx = 2.0f * (RANDOM.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN) % Math.round(2.0f * LevelRenderer.SKYBOX_WIDTH);
            posy = 2.0f * (RANDOM.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN) % Math.round(2.0f * LevelRenderer.SKYBOX_WIDTH);
            posz = 2.0f * (RANDOM.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN) % Math.round(2.0f * LevelRenderer.SKYBOX_WIDTH);
            randPos = new Vector3f(posx, posy, posz);
        } while (levelRenderer.getPosSolidMap().get(randPos) != null
                || levelRenderer.getPosFluidMap().get(randPos) != null
                || levelRenderer.getObserver().getModel().contains(randPos)
                || levelRenderer.getObserver().getCamera().getPos().equals(randPos));
        float colx = RANDOM.nextFloat();
        float coly = RANDOM.nextFloat();
        float colz = RANDOM.nextFloat();
        Vector3f pos = randPos;
        Vector4f col = new Vector4f(colx, coly, colz, 0.5f);
        Block fluidBlock = new Block(false, Texture.WATER, pos, col, true);
        levelRenderer.getPosFluidMap().put(fluidBlock.getPos(), fluidBlock.hashCode());
        levelRenderer.getFluidBlocks().getBlockList().add(fluidBlock);
        return fluidBlock;
    }

    private Block generateRandomSolidBlockAdjacent(Block block) {
        int[] possibleFaces = block.getAdjacentFreeFaceNumbers();
        if (possibleFaces.length == 0) {
            return null;
        }
        int randFace;
        Vector3f adjPos;
        do {
            randFace = possibleFaces[RANDOM.nextInt(possibleFaces.length)];
            adjPos = new Vector3f(block.getPos().x, block.getPos().y, block.getPos().z);
            switch (randFace) {
                case Block.LEFT:
                    adjPos.x -= block.getWidth() / 2.0f + block.getWidth() / 2.0f;
                    break;
                case Block.RIGHT:
                    adjPos.x += block.getWidth() / 2.0f + block.getWidth() / 2.0f;
                    break;
                case Block.BOTTOM:
                    adjPos.y -= block.getHeight() / 2.0f + block.getHeight() / 2.0f;
                    break;
                case Block.TOP:
                    adjPos.y += block.getHeight() / 2.0f + block.getHeight() / 2.0f;
                    break;
                case Block.BACK:
                    adjPos.z -= block.getDepth() / 2.0f + block.getDepth() / 2.0f;
                    break;
                case Block.FRONT:
                    adjPos.z += block.getDepth() / 2.0f + block.getDepth() / 2.0f;
                    break;
                default:
                    break;
            }
        } while (levelRenderer.getPosSolidMap().get(adjPos) != null
                || levelRenderer.getPosFluidMap().get(adjPos) != null
                || levelRenderer.getObserver().getModel().contains(adjPos)
                || levelRenderer.getObserver().getCamera().getPos().equals(adjPos));
        float adjColx = RANDOM.nextFloat();
        float adjColy = RANDOM.nextFloat();
        float adjColz = RANDOM.nextFloat();
        Vector4f adjCol = new Vector4f(adjColx, adjColy, adjColz, 1.0f);
        Texture adjTexture = randomSolidTexture();
        Block solidAdjBlock = new Block(false, adjTexture, adjPos, adjCol, false);

        block.getAdjacentBlockMap().put(randFace, solidAdjBlock.hashCode());
        solidAdjBlock.getAdjacentBlockMap().put(randFace % 2 == 0 ? randFace + 1 : randFace - 1, block.hashCode());

        levelRenderer.getPosSolidMap().put(solidAdjBlock.getPos(), solidAdjBlock.hashCode());
        levelRenderer.getSolidBlocks().getBlockList().add(solidAdjBlock);
        return solidAdjBlock;
    }

    private Block generateRandomFluidBlockAdjacent(Block block) {
        int[] possibleFaces = block.getAdjacentFreeFaceNumbers();
        if (possibleFaces.length == 0) {
            return null;
        }
        int randFace;
        Vector3f adjPos;
        do {
            randFace = possibleFaces[RANDOM.nextInt(possibleFaces.length)];
            adjPos = new Vector3f(block.getPos().x, block.getPos().y, block.getPos().z);
            switch (randFace) {
                case Block.LEFT:
                    adjPos.x -= block.getWidth() / 2.0f + block.getWidth() / 2.0f;
                    break;
                case Block.RIGHT:
                    adjPos.x += block.getWidth() / 2.0f + block.getWidth() / 2.0f;
                    break;
                case Block.BOTTOM:
                    adjPos.y -= block.getHeight() / 2.0f + block.getHeight() / 2.0f;
                    break;
                case Block.TOP:
                    adjPos.y += block.getHeight() / 2.0f + block.getHeight() / 2.0f;
                    break;
                case Block.BACK:
                    adjPos.z -= block.getDepth() / 2.0f + block.getDepth() / 2.0f;
                    break;
                case Block.FRONT:
                    adjPos.z += block.getDepth() / 2.0f + block.getDepth() / 2.0f;
                    break;
                default:
                    break;
            }
        } while (levelRenderer.getPosSolidMap().get(adjPos) != null
                || levelRenderer.getPosFluidMap().get(adjPos) != null
                || levelRenderer.getObserver().getModel().contains(adjPos)
                || levelRenderer.getObserver().getCamera().getPos().equals(adjPos));
        float adjColx = RANDOM.nextFloat();
        float adjColy = RANDOM.nextFloat();
        float adjColz = RANDOM.nextFloat();
        Vector4f adjCol = new Vector4f(adjColx, adjColy, adjColz, 0.5f);
        Texture adjTexture = Texture.WATER;
        Block fluidAdjBlock = new Block(false, adjTexture, adjPos, adjCol, false);
        block.getAdjacentBlockMap().put(randFace, fluidAdjBlock.hashCode());
        fluidAdjBlock.getAdjacentBlockMap().put(randFace % 2 == 0 ? randFace + 1 : randFace - 1, block.hashCode());

        levelRenderer.getPosFluidMap().put(fluidAdjBlock.getPos(), fluidAdjBlock.hashCode());
        levelRenderer.getFluidBlocks().getBlockList().add(fluidAdjBlock);
        return fluidAdjBlock;
    }

    public void generate() {
        if (levelRenderer.getProgress() == 0) {
            int solidBlocks = 1 + RANDOM.nextInt(numberOfBlocks + 1);
            int fluidBlocks = numberOfBlocks - solidBlocks;

            final int totalAmount = solidBlocks + fluidBlocks;

            while ((solidBlocks > 0 || fluidBlocks > 0)
                    && !GLFW.glfwWindowShouldClose(levelRenderer.getMyWindow().getWindowID())) {

                float alpha = RANDOM.nextFloat();

                int maxSolidBatchSize = (int) ((1.0f - alpha) * solidBlocks);
                int maxFluidBatchSize = (int) (alpha * fluidBlocks);

                //------------------------------------------------------------------
                if (solidBlocks > 0) {
                    int solidBatch = 1 + RANDOM.nextInt(Math.min(maxSolidBatchSize, solidBlocks));
                    Block solidBlock = null;
                    Block solidAdjBlock = null;
                    while (solidBatch > 0
                            && !GLFW.glfwWindowShouldClose(levelRenderer.getMyWindow().getWindowID())) {
                        if (solidBlock == null) {
                            solidBlock = generateRandomSolidBlock();
                            solidAdjBlock = solidBlock;
                            solidBatch--;
                            solidBlocks--;
                            levelRenderer.updateSolidNeighbors();
                            // this provides external monitoring of level generation progress                        
                            levelRenderer.incProgress(80.0f / (float) totalAmount);
                        } else if (solidAdjBlock != null) {
                            solidAdjBlock = generateRandomSolidBlockAdjacent(solidBlock);
                            if (solidAdjBlock != null) {
                                solidBatch--;
                                solidBlocks--;
                                levelRenderer.updateSolidNeighbors();
                                // this provides external monitoring of level generation progress                        
                                levelRenderer.incProgress(80.0f / (float) totalAmount);
                            }
                            //--------------------------------------------------
                            if (RANDOM.nextInt(2) == 0) {
                                solidBlock = solidAdjBlock;
                            }
                        } else {
                            solidBlock = null;
                        }
                    }
                }
                //------------------------------------------------------------------
                if (fluidBlocks > 0) {
                    int fluidBatch = 1 + RANDOM.nextInt(Math.min(maxFluidBatchSize, fluidBlocks));
                    Block fluidBlock = null;
                    Block fluidAdjBlock = null;
                    while (fluidBatch > 0
                            && !GLFW.glfwWindowShouldClose(levelRenderer.getMyWindow().getWindowID())) {
                        if (fluidBlock == null) {
                            fluidBlock = generateRandomFluidBlock();
                            fluidAdjBlock = fluidBlock;
                            fluidBatch--;
                            fluidBlocks--;
                            levelRenderer.updateFluidNeighbors();
                            // this provides external monitoring of level generation progress                        
                            levelRenderer.incProgress(80.0f / (float) totalAmount);
                        } else if (fluidAdjBlock != null) {
                            fluidAdjBlock = generateRandomFluidBlockAdjacent(fluidBlock);
                            if (fluidAdjBlock != null) {
                                fluidBatch--;
                                fluidBlocks--;
                                levelRenderer.updateFluidNeighbors();
                                // this provides external monitoring of level generation progress                        
                                levelRenderer.incProgress(80.0f / (float) totalAmount);
                            } //--------------------------------------------------
                            if (RANDOM.nextInt(2) == 0) {
                                fluidBlock = fluidAdjBlock;
                            }
                        } else {
                            fluidBlock = null;
                        }
                    }
                }
                //------------------------------------------------------------------                                                
            }
        }
    }

    public LevelRenderer getLevelRenderer() {
        return levelRenderer;
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public void setNumberOfBlocks(int numberOfBlocks) {
        this.numberOfBlocks = numberOfBlocks;
    }

}
