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

import java.util.Random;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.models.Block;

/**
 *
 * @author Coa
 */
public class RandomLevelGenerator {

    private static final int MAX_FLUID_BATCH_SIZE = 100;
    private static final int MAX_SOLID_BATCH_SIZE = 10;

    private static final int POS_MAX = Math.round(LevelRenderer.SKYBOX_WIDTH);
    private static final int POS_MIN = Math.round(-LevelRenderer.SKYBOX_WIDTH);

    private static final Random RANDOM = new Random();

    private static Texture randomSolidTexture() {
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

    private static Block generateRandomSolidBlock(LevelRenderer levelRenderer) {
        float posx;
        float posy;
        float posz;
        Vector3f randPos;
        do {
            posx = 2.0f * (RANDOM.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN) % Math.round(2.0f * LevelRenderer.SKYBOX_WIDTH);
            posy = 2.0f * (RANDOM.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN) % Math.round(2.0f * LevelRenderer.SKYBOX_WIDTH);
            posz = 2.0f * (RANDOM.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN) % Math.round(2.0f * LevelRenderer.SKYBOX_WIDTH);
            randPos = new Vector3f(posx, posy, posz);
        } while (levelRenderer.isPlaceOccupiedBySolid(randPos)
                || levelRenderer.isPlaceOccupiedByFluid(randPos)
                || levelRenderer.getObserver().getModel().contains(randPos)
                || levelRenderer.getObserver().getCamera().getPos().equals(randPos));
        float colx = RANDOM.nextFloat();
        float coly = RANDOM.nextFloat();
        float colz = RANDOM.nextFloat();
        Vector3f pos = randPos;
        Vector4f col = new Vector4f(colx, coly, colz, 1.0f);
        Block solidBlock = new Block(false, Texture.STONE, pos, col, false);
        levelRenderer.getSolidBlocks().getBlockList().add(solidBlock);
        return solidBlock;
    }

    private static Block generateRandomFluidBlock(LevelRenderer levelRenderer) {
        float posx;
        float posy;
        float posz;
        Vector3f randPos;
        do {
            posx = 2.0f * (RANDOM.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN) % Math.round(2.0f * LevelRenderer.SKYBOX_WIDTH);
            posy = 2.0f * (RANDOM.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN) % Math.round(2.0f * LevelRenderer.SKYBOX_WIDTH);
            posz = 2.0f * (RANDOM.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN) % Math.round(2.0f * LevelRenderer.SKYBOX_WIDTH);
            randPos = new Vector3f(posx, posy, posz);
        } while (levelRenderer.isPlaceOccupiedBySolid(randPos)
                || levelRenderer.isPlaceOccupiedByFluid(randPos)
                || levelRenderer.getObserver().getModel().contains(randPos)
                || levelRenderer.getObserver().getCamera().getPos().equals(randPos));
        float colx = RANDOM.nextFloat();
        float coly = RANDOM.nextFloat();
        float colz = RANDOM.nextFloat();
        Vector3f pos = randPos;
        Vector4f col = new Vector4f(colx, coly, colz, 0.5f);
        Block fluidBlock = new Block(false, Texture.WATER, pos, col, true);
        levelRenderer.getFluidBlocks().getBlockList().add(fluidBlock);
        return fluidBlock;
    }

    private static Block generateRandomSolidBlockAdjacent(LevelRenderer levelRenderer, Block block) {
        int randFace;
        Vector3f adjPos;
        do {
            randFace = RANDOM.nextInt(6);
            adjPos = new Vector3f(block.getPos().x, block.getPos().y, block.getPos().z);
            switch (randFace) {
                case Block.LEFT:
                    adjPos.x += block.getWidth() / 2.0f + block.getWidth() / 2.0f;
                    break;
                case Block.RIGHT:
                    adjPos.x -= block.getWidth() / 2.0f + block.getWidth() / 2.0f;
                    break;
                case Block.BOTTOM:
                    adjPos.y += block.getHeight() / 2.0f + block.getHeight() / 2.0f;
                    break;
                case Block.TOP:
                    adjPos.y -= block.getHeight() / 2.0f + block.getHeight() / 2.0f;
                    break;
                case Block.BACK:
                    adjPos.z += block.getDepth() / 2.0f + block.getDepth() / 2.0f;
                    break;
                case Block.FRONT:
                    adjPos.z -= block.getDepth() / 2.0f + block.getDepth() / 2.0f;
                    break;
                default:
                    break;
            }
        } while (levelRenderer.isPlaceOccupiedBySolid(adjPos)
                || levelRenderer.isPlaceOccupiedByFluid(adjPos)
                || levelRenderer.getObserver().getModel().contains(adjPos)
                || levelRenderer.getObserver().getCamera().getPos().equals(adjPos));
        float adjColx = RANDOM.nextFloat();
        float adjColy = RANDOM.nextFloat();
        float adjColz = RANDOM.nextFloat();
        Vector4f adjCol = new Vector4f(adjColx, adjColy, adjColz, 1.0f);
        Texture adjTexture = randomSolidTexture();
        Block solidAdjBlock = new Block(false, adjTexture, adjPos, adjCol, false);
        levelRenderer.getSolidBlocks().getBlockList().add(solidAdjBlock);
        return solidAdjBlock;
    }

    private static Block generateRandomFluidBlockAdjacent(LevelRenderer levelRenderer, Block block) {
        int randFace;
        Vector3f adjPos;
        do {
            randFace = RANDOM.nextInt(6);
            adjPos = new Vector3f(block.getPos().x, block.getPos().y, block.getPos().z);
            switch (randFace) {
                case Block.LEFT:
                    adjPos.x += block.getWidth() / 2.0f + block.getWidth() / 2.0f;
                    break;
                case Block.RIGHT:
                    adjPos.x -= block.getWidth() / 2.0f + block.getWidth() / 2.0f;
                    break;
                case Block.BOTTOM:
                    adjPos.y += block.getHeight() / 2.0f + block.getHeight() / 2.0f;
                    break;
                case Block.TOP:
                    adjPos.y -= block.getHeight() / 2.0f + block.getHeight() / 2.0f;
                    break;
                case Block.BACK:
                    adjPos.z += block.getDepth() / 2.0f + block.getDepth() / 2.0f;
                    break;
                case Block.FRONT:
                    adjPos.z -= block.getDepth() / 2.0f + block.getDepth() / 2.0f;
                    break;
                default:
                    break;
            }
        } while (levelRenderer.isPlaceOccupiedBySolid(adjPos)
                || levelRenderer.isPlaceOccupiedByFluid(adjPos)
                || levelRenderer.getObserver().getModel().contains(adjPos)
                || levelRenderer.getObserver().getCamera().getPos().equals(adjPos));
        float adjColx = RANDOM.nextFloat();
        float adjColy = RANDOM.nextFloat();
        float adjColz = RANDOM.nextFloat();
        Vector4f adjCol = new Vector4f(adjColx, adjColy, adjColz, 0.5f);
        Texture adjTexture = Texture.WATER;
        Block fluidAdjBlock = new Block(false, adjTexture, adjPos, adjCol, false);
        levelRenderer.getFluidBlocks().getBlockList().add(fluidAdjBlock);
        return fluidAdjBlock;
    }

    public static void generate(LevelRenderer levelRenderer, int numberOfBlocks) {
        if (levelRenderer.getProgress() == 0) {
            int solidBlocks = 1 + RANDOM.nextInt(numberOfBlocks + 1);
            int fluidBlocks = numberOfBlocks - solidBlocks;

            while (solidBlocks > 0 && fluidBlocks > 0) {
                //------------------------------------------------------------------
                int solidBatch = 1 + RANDOM.nextInt(MAX_SOLID_BATCH_SIZE);
                Block solidBlock = generateRandomSolidBlock(levelRenderer);
                solidBatch--;
                solidBlocks--;
                while (solidBatch > 0) {
                    if (solidBlock.getAdjacentBlockMap().size() < 6) {
                        solidBlock = generateRandomSolidBlockAdjacent(levelRenderer, solidBlock);
                    } else {
                        solidBlock = generateRandomSolidBlock(levelRenderer);
                    }
                    solidBatch--;
                    solidBlocks--;
                    // this provides external monitoring of level generation progress
                    levelRenderer.setProgress(80 - Math.round(0.8f * (solidBlocks + fluidBlocks) / (float) (numberOfBlocks)));
                }
                //------------------------------------------------------------------
                int fluidBatch = 1 + RANDOM.nextInt(MAX_FLUID_BATCH_SIZE);
                Block fluidBlock = generateRandomFluidBlock(levelRenderer);
                fluidBatch--;
                fluidBlocks--;

                while (fluidBatch > 0) {
                    if (fluidBlock.getAdjacentBlockMap().size() < 6) {
                        fluidBlock = generateRandomFluidBlockAdjacent(levelRenderer, fluidBlock);
                    } else {
                        fluidBlock = generateRandomFluidBlock(levelRenderer);
                    }
                    fluidBatch--;
                    fluidBlocks--;
                    // this provides external monitoring of level generation progress
                    levelRenderer.setProgress(80 - Math.round(0.8f * (solidBlocks + fluidBlocks) / (float) (numberOfBlocks)));
                }
                // this provides external monitoring of level generation progress
                levelRenderer.setProgress(80 - Math.round(0.8f * (solidBlocks + fluidBlocks) / (float) (numberOfBlocks)));
            }
        }
    }

}
