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

import rs.alexanderstojanovich.evg.light.LightSources;
import java.util.List;
import org.joml.Random;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.MathUtils;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class RandomLevelGenerator {

    public static final int H_MAX = Chunk.BOUND >> 2;
    public static final int H_MIN = -Chunk.BOUND >> 2;

    public static final int POS_MAX = Chunk.GRID_SIZE * Chunk.BOUND;
    public static final int POS_MIN = -Chunk.GRID_SIZE * Chunk.BOUND;

    public static final float CUBIC = 1.067E-14f;
    public static final float QUADRATIC = -8.0E-10f;
    public static final float LINEAR = 2.67E-4f;
    public static final float CONST = 23.0f;

    protected long seed = 0x123456789L;
    protected Random random = new Random(seed);
    public static final int RAND_MAX_ATTEMPTS = 1000;

    private final LevelContainer levelContainer;

    private int numberOfBlocks = 0;

    // MAX NUMER OF LIGHTS MUST NOT REACH 255 (+1 Reserved for player)
    public static int numOfLights = 0;
    public static int maxNumOfLights = 0;

    public RandomLevelGenerator(LevelContainer levelContainer) {
        this.levelContainer = levelContainer;
    }

    public RandomLevelGenerator(LevelContainer levelContainer, int numberOfBlocks) {
        this.levelContainer = levelContainer;
        this.numberOfBlocks = numberOfBlocks;
    }

    private String randomSolidTexture(boolean includingLight) {
        int randTexture = random.nextInt(includingLight ? 4 : 3);
        switch (randTexture) {
            case 0:
                return "stone";
            case 1:
                return "crate";
            case 2:
                return "doom0";
            case 3:
                if (numOfLights < maxNumOfLights) {
                    numOfLights++;
                    return "reflc";
                } else {
                    return "stone";
                }
        }

        return null;
    }

    private boolean repeatCondition(Vector3f pos) {
        return LevelContainer.ALL_BLOCK_MAP.isLocationPopulated(pos)
                || levelContainer.getLevelActors().getPlayer().body.containsInsideEqually(pos)
                || levelContainer.getLevelActors().spectator.getPos().equals(pos)
                || levelContainer.getMyWindow().shouldClose();
    }

    private Block generateRandomSolidBlock(int posMin, int posMax, int hMin, int hMax) {
        float posx;
        float posy;
        float posz;
        Vector3f randPos;
        int randomAttempts = 0;
        do {
            posx = (random.nextInt(posMax - posMin + 1) + posMin) & 0xFFFFFFFE;
            posy = (random.nextInt(hMax - hMin + 1) + hMin) & 0xFFFFFFFE;
            posz = (random.nextInt(posMax - posMin + 1) + posMin) & 0xFFFFFFFE;

            randPos = new Vector3f(posx, posy, posz);
            randomAttempts++;
//            DSLogger.reportDebug("randomAttemps = " + randomAttempts, null);
        } while (repeatCondition(randPos) && randomAttempts < RAND_MAX_ATTEMPTS && !levelContainer.getMyWindow().shouldClose());

        if (randomAttempts == RAND_MAX_ATTEMPTS) {
            return null;
        }

        Vector3f pos = randPos;
        // color chance
        Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
        if (random.nextFloat() >= 0.95f) {
            Vector3f temp = new Vector3f();
            color = color.mul(random.nextFloat(), random.nextFloat(), random.nextFloat(), temp);
        }

        String tex = "stone";
        if (random.nextFloat() >= 0.5f) {
            tex = randomSolidTexture(true);
        }

        Block solidBlock = new Block(tex, pos, new Vector4f(color, 1.0f), true);

        levelContainer.chunks.addBlock(solidBlock);
        return solidBlock;
    }

    private Block generateRandomFluidBlock(int posMin, int posMax, int hMin, int hMax) {
        float posx;
        float posy;
        float posz;
        Vector3f randPos;
        int randomAttempts = 0;
        do {
            posx = (random.nextInt(posMax - posMin + 1) + posMin) & 0xFFFFFFFE;
            posy = (random.nextInt(hMax - hMin + 1) + hMin) & 0xFFFFFFFE;
            posz = (random.nextInt(posMax - posMin + 1) + posMin) & 0xFFFFFFFE;

            randPos = new Vector3f(posx, posy, posz);
            randomAttempts++;
//            DSLogger.reportDebug("randomAttemps = " + randomAttempts, null);
        } while (repeatCondition(randPos) && randomAttempts < RAND_MAX_ATTEMPTS && !levelContainer.getMyWindow().shouldClose());

        if (randomAttempts == RAND_MAX_ATTEMPTS) {
            return null;
        }

        Vector3f pos = randPos;
        Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
        if (random.nextFloat() >= 0.75f) {
            Vector3f temp = new Vector3f();
            color = color.mul(random.nextFloat(), random.nextFloat(), random.nextFloat(), temp);
        }
        Block fluidBlock = new Block("water", pos, new Vector4f(color, 0.5f), false);

        levelContainer.chunks.addBlock(fluidBlock);
        return fluidBlock;
    }

    private Block generateRandomSolidBlockAdjacent(Block block) {
        List<Integer> possibleFaces = block.getAdjacentFreeFaceNumbers();
        if (possibleFaces.isEmpty()) {
            return null;
        }
        int randFace;
        Vector3f adjPos;
        int randomAttempts = 0;
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
            randomAttempts++;
//            DSLogger.reportDebug("randomAttemps = " + randomAttempts, null);
        } while (repeatCondition(adjPos) && randomAttempts < RAND_MAX_ATTEMPTS && !levelContainer.getMyWindow().shouldClose());

        if (randomAttempts == RAND_MAX_ATTEMPTS) {
            return null;
        }

        Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
        if (random.nextFloat() >= 0.95f) {
            Vector3f temp = new Vector3f();
            color = color.mul(random.nextFloat(), random.nextFloat(), random.nextFloat(), temp);
        }

        String adjTex = "stone";
        if (random.nextFloat() >= 0.5f) {
            adjTex = randomSolidTexture(random.nextFloat() <= 0.5f);
        }

        Block solidAdjBlock = new Block(adjTex, adjPos, new Vector4f(color, 1.0f), true);

        levelContainer.chunks.addBlock(solidAdjBlock);
        return solidAdjBlock;
    }

    private Block generateRandomFluidBlockAdjacent(Block block) {
        List<Integer> possibleFaces = block.getAdjacentFreeFaceNumbers();
        if (possibleFaces.isEmpty()) {
            return null;
        }
        int randFace;
        Vector3f adjPos;
        int randomAttempts = 0;
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
            randomAttempts++;
//            DSLogger.reportDebug("randomAttemps = " + randomAttempts, null);
        } while (repeatCondition(adjPos) && randomAttempts < RAND_MAX_ATTEMPTS && !levelContainer.getMyWindow().shouldClose());

        if (randomAttempts == RAND_MAX_ATTEMPTS) {
            return null;
        }

        String adjTexture = "water";
        Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
        if (random.nextFloat() >= 0.75f) {
            Vector3f temp = new Vector3f();
            color = color.mul(random.nextFloat(), random.nextFloat(), random.nextFloat(), temp);
        }
        Block fluidAdjBlock = new Block(adjTexture, adjPos, new Vector4f(color, 0.5f), false);

        levelContainer.chunks.addBlock(fluidAdjBlock);
        return fluidAdjBlock;
    }

    //---------------------------------------------------------------------------------------------------------------------------
    //---------------------------------------------------------------------------------------------------------------------------
    //---------------------------------------------------------------------------------------------------------------------------
    private void generateByNoise(int solidBlocks, int fluidBlocks, int totalAmount, int posMin, int posMax, int hMin, int hMax) {
//        DSLogger.reportDebug("By Noise: solidBlks = " + solidBlocks + ", fluidBlks = " + fluidBlocks, null);
        // make "stone" terrain
        noiseMain:
        for (int x = posMin; x <= posMax; x += 2) {
            for (int z = posMin; z <= posMax; z += 2) {
                if (solidBlocks == 0 && fluidBlocks == 0) {
                    break noiseMain;
                }

                int yMid = Math.round(MathUtils.noise2(16, x, z, 0.5f, 0.007f, hMin, hMax, 2.0f)) & 0xFFFFFFFE;
                int yTop = Math.round(MathUtils.noise2(16, x, z, 0.5f, 0.007f, yMid, hMax, 2.0f)) & 0xFFFFFFFE;
                int yBottom = Math.round(MathUtils.noise2(16, x, z, 0.5f, 0.007f, posMin, yMid, 2.0f)) & 0xFFFFFFFE;

                noise1:
                for (int y = yMid; y <= yTop; y += 2) {
                    Vector3f pos = new Vector3f(x, y, z);
                    if (repeatCondition(pos)) {
                        continue;
                    }
                    float value = MathUtils.noise3(16, x, y, z, 0.5f, 0.007f, yMid, yTop, 2.0f);
                    if (solidBlocks > 0 && value >= 0.0f) {
                        // color chance
                        Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
                        if (random.nextFloat() >= 0.95f) {
                            Vector3f tempc = new Vector3f();
                            color = color.mul(random.nextFloat(), random.nextFloat(), random.nextFloat(), tempc);
                        }

                        if (solidBlocks > 0) {
                            String tex = "stone";

                            if (random.nextFloat() >= 0.95f) {
                                tex = randomSolidTexture(random.nextFloat() <= 0.5f);
                            }

                            Block solidBlock = new Block(tex, pos, new Vector4f(color, 1.0f), true);
                            levelContainer.chunks.addBlock(solidBlock);
                            levelContainer.incProgress(100.0f / (float) totalAmount);
                            solidBlocks--;
                        }
                    }

                    if (solidBlocks == 0) {
                        break noise1;
                    }
                }

                noise2:
                for (int y = yBottom; y <= yMid; y += 2) {
                    Vector3f pos = new Vector3f(x, y, z);
                    if (repeatCondition(pos)) {
                        continue;
                    }
                    float value = MathUtils.noise3(16, x, y, z, 0.5f, 0.007f, yMid, yTop, 2.0f);
                    if (fluidBlocks > 0 && value < 0.0f) {
                        // color chance
                        Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
                        if (random.nextFloat() >= 0.95f) {
                            Vector3f tempc = new Vector3f();
                            color = color.mul(random.nextFloat(), random.nextFloat(), random.nextFloat(), tempc);
                        }

                        int sbits = 0;
                        int fbits = 0;
                        TexByte pair = LevelContainer.ALL_BLOCK_MAP.getLocation(pos);
                        if (pair != null && pair.solid) {
                            sbits = pair.getByteValue();
                        } else if (pair != null && !pair.solid) {
                            fbits = pair.getByteValue();
                        }

                        final int mask1 = 0x08; // bottom only mask
                        final int mask2 = 0x17; // bottom exclusive mask
                        int tbits = (sbits & mask1) | (~fbits & mask2);

                        if (fluidBlocks > 0 && tbits != 0) {
                            String tex = "water";

                            Block fluidBlock = new Block(tex, pos, new Vector4f(color, 0.5f), false);
                            levelContainer.chunks.addBlock(fluidBlock);
                            levelContainer.incProgress(100.0f / (float) totalAmount);
                            fluidBlocks--;
                        }

                        if (fluidBlocks == 0) {
                            break noise2;
                        }
                    }
                }
            }
        }
    }

    private void generateByRandom(int solidBlocks, int fluidBlocks, int totalAmount, int posMin, int posMax, int hMin, int hMax) {
//        DSLogger.reportDebug("By Random: solidBlks = " + solidBlocks + ", fluidBlks = " + fluidBlocks, null);
        // 2. Random part
        //beta 
        float beta = random.nextFloat();
        int maxSolidBatchSize = (int) ((1.0f - beta) * solidBlocks);
        int maxFluidBatchSize = (int) (beta * fluidBlocks);

        while ((solidBlocks > 0 || fluidBlocks > 0)
                && !levelContainer.getMyWindow().shouldClose()) {
            if (solidBlocks > 0) {
                int solidBatch = 1 + random.nextInt(Math.min(maxSolidBatchSize, solidBlocks));
                Block solidBlock = null;
                Block solidAdjBlock = null;
                while (solidBatch > 0
                        && !levelContainer.getMyWindow().shouldClose()) {
                    if (solidBlock == null) {
                        solidBlock = generateRandomSolidBlock(posMin, posMax, hMin, hMax);
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
                        } else {
                            break;
                        }
                        //--------------------------------------------------
                        if (random.nextInt(2) == 0) {
                            solidBlock = solidAdjBlock;
                        } else {
                            solidBlock = null;
                        }
                    }
                }
            }

            if (fluidBlocks > 0) {
                int fluidBatch = 1 + random.nextInt(Math.min(maxFluidBatchSize, fluidBlocks));
                Block fluidBlock = null;
                Block fluidAdjBlock = null;
                while (fluidBatch > 0
                        && !levelContainer.getMyWindow().shouldClose()) {
                    if (fluidBlock == null) {
                        fluidBlock = generateRandomFluidBlock(posMin, posMax, hMin, hMax);
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
                        } else {
                            break;
                        }
                        //--------------------------------------------------
                        if (random.nextInt(2) == 0) {
                            fluidBlock = fluidAdjBlock;
                        } else {
                            fluidBlock = null;
                        }
                    }
                }
            }
        }
    }

    private void generateFluidSeries(int solidBlocks) {
//        DSLogger.reportDebug("By Noise: solidBlks = " + solidBlocks, null);
        levelContainer.setProgress(0.0f);
        List<Block> totalFldBlkList = levelContainer.chunks.getTotalList();
        for (Block fluidBlock : totalFldBlkList) {
            if (levelContainer.getMyWindow().shouldClose()) {
                break;
            }

            if (solidBlocks == 0) {
                break;
            }

            List<Integer> freeFaces = fluidBlock.getAdjacentFreeFaceNumbers();
            for (int faceNum : freeFaces) {
                if (faceNum == Block.TOP && random.nextFloat() >= 0.25f) {
                    continue;
                }
                Vector3f spos = fluidBlock.getAdjacentPos(faceNum);
                Block solidBlock = new Block("stone", spos, GlobalColors.WHITE_RGBA, true);
                levelContainer.chunks.addBlock(solidBlock);
                solidBlocks--;
                if (solidBlocks == 0) {
                    break;
                }
            }
            levelContainer.incProgress(100.0f / (float) totalFldBlkList.size());
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------
    public void generate() {
        if (levelContainer.getProgress() == 0.0f) {
            DSLogger.reportDebug("Generating random level (" + numberOfBlocks + " blocks).. with seed = " + seed, null);
            // define alpha: solid to fluid ratio
            final float alpha = 0.48676294f;
            int solidBlocks = Math.min(Math.round(alpha * numberOfBlocks), LevelContainer.MAX_NUM_OF_BLOCKS / 2);
            int fluidBlocks = Math.min(numberOfBlocks - solidBlocks, LevelContainer.MAX_NUM_OF_BLOCKS / 2);

            final int totalAmount = solidBlocks + fluidBlocks;

            numOfLights = 0;
            maxNumOfLights = Math.round(0.19f * LightSources.MAX_LIGHTS * totalAmount / 25000.0f);

            if (totalAmount > 0) {
                //---------------------------------------------------------------------------------------------------------------------------
                // define beta: noise to random ratio
                final float beta = 0.7193977f;
                final float gamma = 0.84f;

                final int solidBlocksN = Math.round(beta * solidBlocks);
                final int solidBlocksN1 = Math.round(gamma * solidBlocksN);
                final int solidBlocksN2 = solidBlocks - solidBlocksN1;

                final int fluidBlocksN = Math.round(beta * fluidBlocks);

                final int solidBlocksR = solidBlocks - solidBlocksN;
                final int solidBlocksR1 = Math.round(gamma * solidBlocksR);
                final int solidBlocksR2 = solidBlocksR - solidBlocksR1;

                final int fluidBlocksR = fluidBlocks - fluidBlocksN;

                // define gamma: random fluid to series solid ratio
                //---------------------------------------------------------------------------------------------------------------------------
                float valueK = 1.5f * MathUtils.polynomial(CUBIC, QUADRATIC, LINEAR, CONST, solidBlocksN + fluidBlocksN);
                int valueK0 = Math.round(valueK) & 0xFFFFFFFE;

                final int posN_Min = -valueK0;
                final int posN_Max = valueK0;

                final int hNMin = posN_Min >> 1;
                final int hNMax = posN_Max >> 1;

                float valueR = 2.0f * MathUtils.polynomial(CUBIC, QUADRATIC, LINEAR, CONST / 4.0f, solidBlocksR + fluidBlocksR);
                final int posR_Min = Math.round(-valueR) & 0xFFFFFFFE;
                final int posR_Max = Math.round(valueR) & 0xFFFFFFFE;

                final int hRMin = posR_Min >> 1;
                final int hRMax = posR_Max >> 1;

                DSLogger.reportDebug(String.format("Generating Part I - Noise (%d blocks)", solidBlocksN + fluidBlocksN), null);
                // 1. Noise Part                                   
                generateByNoise(solidBlocksN1, fluidBlocksN, totalAmount, posN_Min, posN_Max, hNMin, hNMax);
                DSLogger.reportDebug("Done.", null);
                // --------------------------------------------------------------
                //--------------------------------------------------------------------------------------------------------------------------- 
                DSLogger.reportDebug(String.format("Generating Part II - Random (%d blocks)", solidBlocksR + solidBlocksR), null);
                // 2. Random Part                 
                generateByRandom(solidBlocksR1, fluidBlocksR, totalAmount, posR_Min, posR_Max, hRMin, hRMax);
                DSLogger.reportDebug("Done.", null);
                // --------------------------------------------------------------
                DSLogger.reportDebug("Generating Part III - Fluid Series", null);
                // 3. Fluid Series
                generateFluidSeries(solidBlocksN2 + solidBlocksR2);
                DSLogger.reportDebug("Done.", null);
            }
        }

        DSLogger.reportDebug("All finished!", null);
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

    public final long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    public Random getRandom() {
        return random;
    }

}
