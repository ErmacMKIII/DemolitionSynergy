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

import java.util.List;
import org.joml.Random;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;
import rs.alexanderstojanovich.evg.util.MathUtils;

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
    public static final float CONST2 = 7.0f;

    protected long seed = 0x12345678L;
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
        return LevelContainer.AllBlockMap.isLocationPopulated(pos)
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
        if (random.nextFloat() >= 0.95f) {
            tex = randomSolidTexture(random.nextFloat() <= 0.5f);
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
        if (random.nextInt(3) != 0) {
            possibleFaces.remove((Integer) Block.BOTTOM);
            possibleFaces.remove((Integer) Block.TOP);
        }
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
        if (random.nextFloat() >= 0.95f) {
            adjTex = randomSolidTexture(random.nextFloat() <= 0.5f);
        }

        Block solidAdjBlock = new Block(adjTex, adjPos, new Vector4f(color, 1.0f), true);

        levelContainer.chunks.addBlock(solidAdjBlock);
        return solidAdjBlock;
    }

    private Block generateRandomFluidBlockAdjacent(Block block) {
        List<Integer> possibleFaces = block.getAdjacentFreeFaceNumbers();
        if (random.nextInt(3) != 0) {
            possibleFaces.remove((Integer) Block.BOTTOM);
            possibleFaces.remove((Integer) Block.TOP);
        }
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
    private int generateByNoise(int solidBlocks, int fluidBlocks, int totalAmount, int posMin, int posMax, int hMin, int hMax) {
        int genBlks = 0; // holds result

        final int numOctaves = 16;
        final float scale = 0.007f;
        final float lacunarity = 2.0f;
        final float persistence = 0.5f;

        float[] frequencies = new float[numOctaves];
        float[] amplitudes = new float[numOctaves];

        float frequency = scale;
        float amplitude = 1.0f;
        for (int i = 0; i < numOctaves; i++) {
            frequencies[i] = frequency;
            amplitudes[i] = amplitude;
            frequency *= lacunarity;
            amplitude *= persistence;
        }

        noiseMain:
        for (int x = posMin; x <= posMax; x += 2) {
            for (int z = posMin; z <= posMax; z += 2) {
                if (solidBlocks == 0 && fluidBlocks == 0) {
                    break noiseMain;
                }

                boolean boundary = (x == posMin || x == posMax || z == posMin || z == posMax);

                int yMid = Math.round(MathUtils.noise2(numOctaves, x, z, 0.5f, hMin, hMax, frequencies, amplitudes)) & 0xFFFFFFFE;
                int yTop = Math.round(MathUtils.noise2(numOctaves, x, z, 0.5f, yMid, hMax, frequencies, amplitudes)) & 0xFFFFFFFE;
                int yBottom = Math.round(MathUtils.noise2(numOctaves, x, z, 0.5f, hMin, yMid, frequencies, amplitudes)) & 0xFFFFFFFE;
                int yHalf = (yTop - yBottom) >> 1;

                // solid & fluid generating
                noiseInner:
                for (int y = yBottom; y <= yTop; y += 2) {
                    Vector3f pos = new Vector3f(x, y, z);
                    if (repeatCondition(pos)) {
                        continue;
                    }

                    if (solidBlocks > 0 && y >= yHalf && boundary) {
                        // color chance
                        Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
                        if (random.nextFloat() >= 0.95f) {
                            Vector3f tempc = new Vector3f();
                            color = color.mul(random.nextFloat(), random.nextFloat(), random.nextFloat(), tempc);
                        }

                        String tex = "stone"; // make "stone" terrain

                        if (random.nextFloat() >= 0.95f) {
                            tex = randomSolidTexture(random.nextFloat() <= 0.5f);
                        }

                        Block solidBlock = new Block(tex, pos, new Vector4f(color, 1.0f), true);
                        levelContainer.chunks.addBlock(solidBlock);
                        levelContainer.incProgress(100.0f / (float) totalAmount);
                        solidBlocks--;
                        genBlks++;
                    } else if (fluidBlocks > 0 && y < yHalf && !boundary) {
                        // color chance
                        Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
                        if (random.nextFloat() >= 0.95f) {
                            Vector3f tempc = new Vector3f();
                            color = color.mul(random.nextFloat(), random.nextFloat(), random.nextFloat(), tempc);
                        }

                        String tex = "water"; // make water terrain

                        Block fluidBlock = new Block(tex, pos, new Vector4f(color, 0.5f), false);
                        levelContainer.chunks.addBlock(fluidBlock);
                        levelContainer.incProgress(100.0f / (float) totalAmount);
                        fluidBlocks--;
                        genBlks++;
                    }

                    if (solidBlocks == 0 && fluidBlocks == 0) {
                        break;// noiseInner;
                    }
                }
            }
        }

        return genBlks;
    }

    private int generateByRandom(int solidBlocks, int fluidBlocks, int totalAmount, int posMin, int posMax, int hMin, int hMax) {
        int genBlks = 0; // holds result

        //beta 
        float beta = 0.67f;
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
                        genBlks++;
                        // this provides external monitoring of level generation progress                        
                        levelContainer.incProgress(100.0f / (float) totalAmount);
                    } else if (solidAdjBlock != null) {
                        solidAdjBlock = generateRandomSolidBlockAdjacent(solidBlock);
                        if (solidAdjBlock != null) {
                            solidBatch--;
                            solidBlocks--;
                            genBlks++;
                            // this provides external monitoring of level generation progress                        
                            levelContainer.incProgress(100.0f / (float) totalAmount);
                        } else {
                            break;
                        }
                        //--------------------------------------------------
                        if (random.nextInt(solidBatch) != 0) {
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
                        genBlks++;
                        // this provides external monitoring of level generation progress                        
                        levelContainer.incProgress(50.0f / (float) totalAmount);
                    } else if (fluidAdjBlock != null) {
                        fluidAdjBlock = generateRandomFluidBlockAdjacent(fluidBlock);
                        if (fluidAdjBlock != null) {
                            fluidBatch--;
                            fluidBlocks--;
                            genBlks++;
                            // this provides external monitoring of level generation progress                        
                            levelContainer.incProgress(50.0f / (float) totalAmount);
                        } else {
                            break;
                        }
                        //--------------------------------------------------
                        if (random.nextInt(fluidBatch) != 0) {
                            fluidBlock = fluidAdjBlock;
                        } else {
                            fluidBlock = null;
                        }
                    }
                }
            }
        }

        return genBlks;
    }

    private void generateFluidSeries(int solidBlocks) {
        // Clouds arent generated only fluid
        levelContainer.setProgress(0.0f);
        IList<Vector3f> allFluidPos = LevelContainer.AllBlockMap.getPopulatedLocations(tb -> !tb.solid);
        for (Vector3f fldPos : allFluidPos) {
            if (levelContainer.getMyWindow().shouldClose()) {
                break;
            }

            if (solidBlocks == 0) {
                break;
            }

            List<Integer> freeFaces = Block.getAdjacentFreeFaceNumbers(fldPos);
            for (int faceNum : freeFaces) {
                if (faceNum == Block.TOP && random.nextFloat() >= 0.25f) {
                    continue;
                }
                Vector3f spos = Block.getAdjacentPos(fldPos, faceNum);
                Block solidBlock = new Block("stone", spos, GlobalColors.WHITE_RGBA, true);
                levelContainer.chunks.addBlock(solidBlock);
                solidBlocks--;
                if (solidBlocks == 0) {
                    break;
                }
            }
            levelContainer.incProgress(100.0f / (float) allFluidPos.size());
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------
    public void generate() {
        if (levelContainer.getProgress() == 0.0f) {
            DSLogger.reportDebug("Generating random level (" + numberOfBlocks + " blocks).. with seed = " + seed, null);
            // define alpha: solid to fluid ratio
            final float alpha = 0.46f;

            // define beta: random to noise ratio
            final float beta = 0.16f;

            numOfLights = 0;
            maxNumOfLights = Math.round(LightSources.MAX_LIGHTS * numberOfBlocks / 200000.0f);

            if (numberOfBlocks > 0) {
                //---------------------------------------------------------------------------------------------------------------------------
                final int totalNoise = Math.round((1.0f - beta) * numberOfBlocks);
                final int totalRandom = Math.round(beta * numberOfBlocks);

                final int soildNoise = Math.round(alpha * totalNoise);
                final int fluidNoise = Math.round((1.0f - alpha) * totalNoise);

                final int solidRandom = Math.round(alpha * totalRandom);
                final int fluidRandom = Math.round((1.0f - alpha) * totalRandom);

                //---------------------------------------------------------------------------------------------------------------------------
                float valueK = (float) MathUtils.pow(totalNoise, 0.33f) * 2.0f;
                int valueK0 = Math.round(valueK) & 0xFFFFFFFE;

                final int posN_Min = -valueK0;
                final int posN_Max = valueK0;

                final int hNMin = posN_Min >> 2;
                final int hNMax = posN_Max >> 2;

                float valueR = (float) MathUtils.pow(totalRandom, 0.33f) * 2.0f;
                final int posR_Min = Math.round(-valueR) & 0xFFFFFFFE;
                final int posR_Max = Math.round(valueR) & 0xFFFFFFFE;

                final int hRMin = posR_Min >> 2;
                final int hRMax = posR_Max >> 2;

                levelContainer.gameObject.intrface.getInfoMsgText().setEnabled(true);
                DSLogger.reportDebug(String.format("Generating Part I - Noise (%d blocks)", totalNoise), null);
                levelContainer.gameObject.intrface.getInfoMsgText().setContent(String.format("Generating Part I - Noise (%d blocks)", totalNoise));
                // 1. Noise Part                                   
                int blocksNoise = generateByNoise(soildNoise, fluidNoise, totalNoise, posN_Min, posN_Max, hNMin, hNMax);
                DSLogger.reportDebug("Done.", null);
                // --------------------------------------------------------------
                //--------------------------------------------------------------------------------------------------------------------------- 
                DSLogger.reportDebug(String.format("Generating Part II - Random (%d blocks)", totalRandom), null);
                levelContainer.gameObject.intrface.getInfoMsgText().setContent(String.format("Generating Part II - Random (%d blocks)", totalNoise));
                // 2. Random Part                 
                int blocksRandom = generateByRandom(solidRandom, fluidRandom, totalRandom, posR_Min, posR_Max, hRMin, hRMax);
                DSLogger.reportDebug("Done.", null);
                // --------------------------------------------------------------
                DSLogger.reportDebug("Generating Part III - Fluid Series", null);
                levelContainer.gameObject.intrface.getInfoMsgText().setContent(String.format("Generating Part III - Fluid Series", totalNoise));
                // 3. Fluid Series
                generateFluidSeries(numberOfBlocks - blocksNoise - blocksRandom);
                DSLogger.reportDebug("Done.", null);
                levelContainer.gameObject.intrface.getInfoMsgText().setContent("Done.");
                // --------------------------------------------------------------
            }
        }

        levelContainer.gameObject.intrface.getInfoMsgText().setEnabled(false);
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
