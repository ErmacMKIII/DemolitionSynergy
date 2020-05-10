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

import java.util.List;
import org.joml.Random;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.models.Block;

/**
 *
 * @author Coa
 */
public class RandomLevelGenerator {

    private final int POS_MAX = Math.round(LevelContainer.SKYBOX_WIDTH / 2.0f - 2.0f);
    private final int POS_MIN = Math.round(-LevelContainer.SKYBOX_WIDTH / 2.0f + 2.0f);

    private final Random random = new Random(0x123456789L);

    private final LevelContainer levelContainer;

    private int numberOfBlocks = 0;

    public RandomLevelGenerator(LevelContainer levelContainer) {
        this.levelContainer = levelContainer;
    }

    public RandomLevelGenerator(LevelContainer levelContainer, int numberOfBlocks) {
        this.levelContainer = levelContainer;
        this.numberOfBlocks = numberOfBlocks;
    }

    private String randomSolidTexture() {
        int randTexture = random.nextInt(3);
        switch (randTexture) {
            case 0:
                return "stone";
            case 1:
                return "crate";
            case 2:
                return "doom0";
        }
        return null;
    }

    private Block generateRandomSolidBlock() {
        float posx;
        float posy;
        float posz;
        Vector3f randPos;
        do {
            posx = random.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN;
            posy = random.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN;
            posz = random.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN;
            randPos = new Vector3f(posx, posy, posz);
        } while (LevelContainer.ALL_SOLID_POS.contains(randPos)
                || LevelContainer.ALL_FLUID_POS.contains(randPos)
                || levelContainer.getLevelActors().getPlayer().getModel().containsInsideEqually(randPos)
                || levelContainer.getLevelActors().getPlayer().getCamera().getPos().equals(randPos));
        float colr = random.nextFloat();
        float colg = random.nextFloat();
        float colb = random.nextFloat();
        Vector3f pos = randPos;
        Vector3f col = new Vector3f(colr, colg, colb);
        Block solidBlock = new Block(false, randomSolidTexture(), pos, col, true);

        levelContainer.getSolidChunks().addBlock(solidBlock);
        return solidBlock;
    }

    private Block generateRandomFluidBlock() {
        float posx;
        float posy;
        float posz;
        Vector3f randPos;
        do {
            posx = random.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN;
            posy = random.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN;
            posz = random.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN;
            randPos = new Vector3f(posx, posy, posz);
        } while (LevelContainer.ALL_SOLID_POS.contains(randPos)
                || LevelContainer.ALL_FLUID_POS.contains(randPos)
                || levelContainer.getLevelActors().getPlayer().getModel().containsInsideEqually(randPos)
                || levelContainer.getLevelActors().getPlayer().getCamera().getPos().equals(randPos));
        float colr = random.nextFloat();
        float colg = random.nextFloat();
        float colb = random.nextFloat();
        Vector3f pos = randPos;
        Vector3f col = new Vector3f(colr, colg, colb);
        Block fluidBlock = new Block(false, "water", pos, col, false);

        levelContainer.getFluidChunks().addBlock(fluidBlock);
        return fluidBlock;
    }

    private Block generateRandomSolidBlockAdjacent(Block block) {
        List<Integer> possibleFaces = block.getAdjacentFreeFaceNumbers(
                LevelContainer.ALL_SOLID_POS,
                LevelContainer.ALL_FLUID_POS
        );
        if (possibleFaces.isEmpty()) {
            return null;
        }
        int randFace;
        Vector3f adjPos;
        do {
            randFace = possibleFaces.get(random.nextInt(possibleFaces.size()));
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
        } while (LevelContainer.ALL_SOLID_POS.contains(adjPos)
                || LevelContainer.ALL_FLUID_POS.contains(adjPos)
                || levelContainer.getLevelActors().getPlayer().getModel().containsInsideEqually(adjPos)
                || levelContainer.getLevelActors().getPlayer().getCamera().getPos().equals(adjPos));
        float adjColr = random.nextFloat();
        float adjColg = random.nextFloat();
        float adjColb = random.nextFloat();
        Vector3f adjCol = new Vector3f(adjColr, adjColg, adjColb);
        String adjTexture = randomSolidTexture();
        Block solidAdjBlock = new Block(false, adjTexture, adjPos, adjCol, true);

        levelContainer.getSolidChunks().addBlock(solidAdjBlock);
        return solidAdjBlock;
    }

    private Block generateRandomFluidBlockAdjacent(Block block) {
        List<Integer> possibleFaces = block.getAdjacentFreeFaceNumbers(
                LevelContainer.ALL_SOLID_POS,
                LevelContainer.ALL_FLUID_POS
        );
        if (possibleFaces.isEmpty()) {
            return null;
        }
        int randFace;
        Vector3f adjPos;
        do {
            randFace = possibleFaces.get(random.nextInt(possibleFaces.size()));
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
        } while (LevelContainer.ALL_SOLID_POS.contains(adjPos)
                || LevelContainer.ALL_FLUID_POS.contains(adjPos)
                || levelContainer.getLevelActors().getPlayer().getModel().containsInsideEqually(adjPos)
                || levelContainer.getLevelActors().getPlayer().getCamera().getPos().equals(adjPos));
        float adjColr = random.nextFloat();
        float adjColg = random.nextFloat();
        float adjColb = random.nextFloat();
        Vector3f adjCol = new Vector3f(adjColr, adjColg, adjColb);
        String adjTexture = "water";
        Block fluidAdjBlock = new Block(false, adjTexture, adjPos, adjCol, false);

        levelContainer.getFluidChunks().addBlock(fluidAdjBlock);
        return fluidAdjBlock;
    }

    public void generate() {
        if (levelContainer.getProgress() == 0.0f) {
            float alpha = random.nextFloat();
            int solidBlocks = Math.min(Math.round((1.0f - alpha) * numberOfBlocks), LevelContainer.MAX_NUM_OF_SOLID_BLOCKS);
            int fluidBlocks = Math.min(Math.round(alpha * numberOfBlocks), LevelContainer.MAX_NUM_OF_FLUID_BLOCKS);

            final int totalAmount = solidBlocks + fluidBlocks;

            while ((solidBlocks > 0 || fluidBlocks > 0)
                    && !levelContainer.getMyWindow().shouldClose()) {

                float beta = random.nextFloat();

                int maxSolidBatchSize = (int) ((1.0f - beta) * solidBlocks);
                int maxFluidBatchSize = (int) (beta * fluidBlocks);

                //------------------------------------------------------------------
                if (solidBlocks > 0) {
                    int solidBatch = 1 + random.nextInt(Math.min(maxSolidBatchSize, solidBlocks));
                    Block solidBlock = null;
                    Block solidAdjBlock = null;
                    while (solidBatch > 0
                            && !levelContainer.getMyWindow().shouldClose()) {
                        if (solidBlock == null) {
                            solidBlock = generateRandomSolidBlock();
                            solidAdjBlock = solidBlock;
                            solidBatch--;
                            solidBlocks--;
                            // this provides external monitoring of level generation progress                        
                            levelContainer.incProgress(100.0f / (float) totalAmount);
                        } else if (solidAdjBlock != null) {
                            solidAdjBlock = generateRandomSolidBlockAdjacent(solidBlock);
                            if (solidAdjBlock != null) {
                                solidBatch--;
                                solidBlocks--;
                                // this provides external monitoring of level generation progress                        
                                levelContainer.incProgress(100.0f / (float) totalAmount);
                            }
                            //--------------------------------------------------
                            if (random.nextInt(2) == 0) {
                                solidBlock = solidAdjBlock;
                            }
                        } else {
                            solidBlock = null;
                        }
                    }
                }
                //------------------------------------------------------------------
                if (fluidBlocks > 0) {
                    int fluidBatch = 1 + random.nextInt(Math.min(maxFluidBatchSize, fluidBlocks));
                    Block fluidBlock = null;
                    Block fluidAdjBlock = null;
                    while (fluidBatch > 0
                            && !levelContainer.getMyWindow().shouldClose()) {
                        if (fluidBlock == null) {
                            fluidBlock = generateRandomFluidBlock();
                            fluidAdjBlock = fluidBlock;
                            fluidBatch--;
                            fluidBlocks--;
                            // this provides external monitoring of level generation progress                        
                            levelContainer.incProgress(100.0f / (float) totalAmount);
                        } else if (fluidAdjBlock != null) {
                            fluidAdjBlock = generateRandomFluidBlockAdjacent(fluidBlock);
                            if (fluidAdjBlock != null) {
                                fluidBatch--;
                                fluidBlocks--;
                                // this provides external monitoring of level generation progress                        
                                levelContainer.incProgress(100.0f / (float) totalAmount);
                            } //--------------------------------------------------
                            if (random.nextInt(2) == 0) {
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

    public LevelContainer getLevelContainer() {
        return levelContainer;
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public void setNumberOfBlocks(int numberOfBlocks) {
        this.numberOfBlocks = numberOfBlocks;
    }

}
