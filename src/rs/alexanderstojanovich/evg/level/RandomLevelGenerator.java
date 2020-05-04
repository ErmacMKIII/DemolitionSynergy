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
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Coa
 */
public class RandomLevelGenerator {

    private final int POS_MAX = Math.round(LevelContainer.SKYBOX_WIDTH / 2.0f);
    private final int POS_MIN = Math.round(-LevelContainer.SKYBOX_WIDTH / 2.0f);

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

    private Texture randomSolidTexture() {
        int randTexture = random.nextInt(3);
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
            posx = random.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN;
            posy = random.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN;
            posz = random.nextInt(POS_MAX - POS_MIN + 1) + POS_MIN;
            randPos = new Vector3f(posx, posy, posz);
        } while (levelContainer.getSolidChunks().getPosMap().get(randPos) != null
                || levelContainer.getFluidChunks().getPosMap().get(randPos) != null
                || levelContainer.getLevelActors().getPlayer().getModel().containsInsideEqually(randPos)
                || levelContainer.getLevelActors().getPlayer().getCamera().getPos().equals(randPos));
        float colx = random.nextFloat();
        float coly = random.nextFloat();
        float colz = random.nextFloat();
        Vector3f pos = randPos;
        Vector4f col = new Vector4f(colx, coly, colz, 1.0f);
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
        } while (levelContainer.getSolidChunks().getPosMap().get(randPos) != null
                || levelContainer.getFluidChunks().getPosMap().get(randPos) != null
                || levelContainer.getLevelActors().getPlayer().getModel().containsInsideEqually(randPos)
                || levelContainer.getLevelActors().getPlayer().getCamera().getPos().equals(randPos));
        float colx = random.nextFloat();
        float coly = random.nextFloat();
        float colz = random.nextFloat();
        Vector3f pos = randPos;
        Vector4f col = new Vector4f(colx, coly, colz, 0.5f);
        Block fluidBlock = new Block(false, Texture.WATER, pos, col, false);

        levelContainer.getFluidChunks().addBlock(fluidBlock);
        return fluidBlock;
    }

    private Block generateRandomSolidBlockAdjacent(Block block) {
        List<Integer> possibleFaces = block.getAdjacentFreeFaceNumbers(
                levelContainer.getSolidChunks().getPosMap(),
                levelContainer.getFluidChunks().getPosMap()
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
        } while (levelContainer.getSolidChunks().getPosMap().get(adjPos) != null
                || levelContainer.getFluidChunks().getPosMap().get(adjPos) != null
                || levelContainer.getLevelActors().getPlayer().getModel().containsInsideEqually(adjPos)
                || levelContainer.getLevelActors().getPlayer().getCamera().getPos().equals(adjPos));
        float adjColx = random.nextFloat();
        float adjColy = random.nextFloat();
        float adjColz = random.nextFloat();
        Vector4f adjCol = new Vector4f(adjColx, adjColy, adjColz, 1.0f);
        Texture adjTexture = randomSolidTexture();
        Block solidAdjBlock = new Block(false, adjTexture, adjPos, adjCol, true);

        levelContainer.getSolidChunks().addBlock(solidAdjBlock);
        return solidAdjBlock;
    }

    private Block generateRandomFluidBlockAdjacent(Block block) {
        List<Integer> possibleFaces = block.getAdjacentFreeFaceNumbers(
                levelContainer.getSolidChunks().getPosMap(),
                levelContainer.getFluidChunks().getPosMap()
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
        } while (levelContainer.getSolidChunks().getPosMap().get(adjPos) != null
                || levelContainer.getFluidChunks().getPosMap().get(adjPos) != null
                || levelContainer.getLevelActors().getPlayer().getModel().containsInsideEqually(adjPos)
                || levelContainer.getLevelActors().getPlayer().getCamera().getPos().equals(adjPos));
        float adjColx = random.nextFloat();
        float adjColy = random.nextFloat();
        float adjColz = random.nextFloat();
        Vector4f adjCol = new Vector4f(adjColx, adjColy, adjColz, 0.5f);
        Texture adjTexture = Texture.WATER;
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
