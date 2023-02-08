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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.critter.ModelCritter;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Chunk;
import rs.alexanderstojanovich.evg.models.Chunks;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Pair;
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LevelContainer implements GravityEnviroment {

    public static final Block SKYBOX = new Block("night");
    public static final Model SUN = Model.readFromObjFile(Game.WORLD_ENTRY, "sun.obj", "suntx");
    public static final Vector3f SUN_COLOR = new Vector3f(0.75f, 0.5f, 0.25f); // orange-yellow color
    public static final float SUN_SCALE = 32.0f;
    public static final float SUN_INTENSITY = (float) (1 << 27);

    public static final LightSource SUNLIGHT
            = new LightSource(SUN.pos, SUN_COLOR, SUN_INTENSITY);

    protected final Chunks solidChunks = new Chunks(true);
    protected final Chunks fluidChunks = new Chunks(false);

    public static final LightSources LIGHT_SOURCES = new LightSources();

    public static final int QUEUE_CAPACITY = 9;
    public static final Comparator<Pair<Integer, Float>> VIPAIR_COMPARATOR = new Comparator<Pair<Integer, Float>>() {
        @Override
        public int compare(Pair<Integer, Float> o1, Pair<Integer, Float> o2) {
            if (o1 == null || o2 == null) {
                return 0;
            } else {
                if (o1.getValue() > o2.getValue()) {
                    return 1;
                } else if (Objects.equals(o1.getValue(), o2.getValue())) {
                    return 0;
                } else {
                    return -1;
                }
            }

        }
    };

    private final Queue<Integer> vChnkIdQueue = new ArrayDeque<>(QUEUE_CAPACITY);
    private final Queue<Integer> iChnkIdQueue = new ArrayDeque<>(QUEUE_CAPACITY);

    private final byte[] buffer = new byte[0x1000000]; // 16 MB Buffer
    private int pos = 0;

    public static final float BASE = 16.0f;
    public static final float SKYBOX_SCALE = BASE * BASE * BASE;
    public static final float SKYBOX_WIDTH = 2.0f * SKYBOX_SCALE;
    public static final Vector3f SKYBOX_COLOR = new Vector3f(0.25f, 0.5f, 0.75f); // cool bluish color for SKYBOX

    public static final int MAX_NUM_OF_SOLID_BLOCKS = 65535;
    public static final int MAX_NUM_OF_FLUID_BLOCKS = 65535;

    private float progress = 0.0f;

    private boolean working = false;

    public final LevelActors levelActors = new LevelActors();

    // position of all the solid blocks to texture name & neighbors
    public static final BlockLocation ALL_BLOCK_MAP = new BlockLocation();

    // std time to live
    public static final float STD_TTL = 30.0f * (float) Game.TICK_TIME;

    protected final CacheModule cacheModule;

    protected static boolean cameraInFluid = false;

    private static byte updatePutSolidNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            TexByte adjPair = ALL_BLOCK_MAP.getLocation(adjPos);
            if (adjPair != null && adjPair.solid) {
                bits |= mask;
                byte adjBits = adjPair.byteValue;
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits |= maskAdj;
                adjPair.byteValue = adjBits;
            }
        }
        return bits;
    }

    private static byte updatePutFluidNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            TexByte adjPair = ALL_BLOCK_MAP.getLocation(adjPos);
            if (adjPair != null && !adjPair.solid) {
                bits |= mask;
                byte adjBits = adjPair.byteValue;
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits |= maskAdj;
                adjPair.byteValue = adjBits;
            }
        }
        return bits;
    }

    private static byte updateRemSolidNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            TexByte adjPair = ALL_BLOCK_MAP.getLocation(adjPos);
            if (adjPair != null && adjPair.solid) {
                bits |= mask;
                byte adjBits = adjPair.byteValue;
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits &= ~maskAdj & 63;
                adjPair.byteValue = adjBits;
            }
        }
        return bits;
    }

    private static byte updateRemFluidNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            TexByte adjPair = ALL_BLOCK_MAP.getLocation(adjPos);
            if (adjPair != null && !adjPair.solid) {
                bits |= mask;
                byte adjBits = adjPair.byteValue;
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits &= ~maskAdj & 63;
                adjPair.byteValue = adjBits;
            }
        }
        return bits;
    }

    public static void putBlock(Block block) {
        Vector3f pos = block.getPos();
        String str = block.getTexName();
        if (block.isSolid()) {
            byte bits = updatePutSolidNeighbors(pos);
            TexByte pairX = new TexByte(str, bits, block.isSolid());
            ALL_BLOCK_MAP.putLocation(new Vector3f(pos), pairX);
        } else {
            byte bits = updatePutFluidNeighbors(pos);
            TexByte pairX = new TexByte(str, bits, block.isSolid());
            ALL_BLOCK_MAP.putLocation(new Vector3f(pos), pairX);
        }
    }

    public static void removeBlock(Block block) {
        Vector3f pos = block.getPos();
        if (block.isSolid()) {
            boolean rem = ALL_BLOCK_MAP.removeLocation(pos);
            if (rem) {
                updateRemSolidNeighbors(pos);
            }
        } else {
            boolean rem = ALL_BLOCK_MAP.removeLocation(pos);
            if (rem) {
                updateRemFluidNeighbors(pos);
            }
        }
    }

    static {
        // setting SKYBOX     
        SKYBOX.setPrimaryColor(SKYBOX_COLOR);
        SKYBOX.setUVsForSkybox();
        SKYBOX.setScale(SKYBOX_SCALE);
        SKYBOX.nullifyNormalsForFace(Block.BOTTOM);

        SUN.setPrimaryColor(SUN_COLOR);
        SUN.pos = new Vector3f(0.0f, 12288.0f, 0.0f);
        SUNLIGHT.pos = SUN.pos;
        SUN.setScale(SUN_SCALE);
    }

    public LevelContainer() {
        this.cacheModule = new CacheModule(this);

        LIGHT_SOURCES.lightSrcList.clear();
        LIGHT_SOURCES.lightSrcList.add(SUNLIGHT);
        LIGHT_SOURCES.lightSrcList.add(levelActors.playerLight);
    }

    public static void printPositionMaps() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("SOLID POSITION MAP");
        sb.append("(size = ").append(ALL_BLOCK_MAP.getPopulation()).append(")\n");
        sb.append("---------------------------");
        sb.append("\n");
        sb.append("FLUID POSITION MAP");
        sb.append("(size = ").append(ALL_BLOCK_MAP.getPopulation()).append(")\n");
        sb.append("---------------------------");
        DSLogger.reportInfo(sb.toString(), null);
    }

    public void printQueues() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("VISIBLE QUEUE\n");
        sb.append(vChnkIdQueue);
        sb.append("\n");
        sb.append("---------------------------");
        sb.append("\n");
        sb.append("INVISIBLE QUEUE\n");
        sb.append(iChnkIdQueue);
        sb.append("\n");
        sb.append("---------------------------");
        DSLogger.reportInfo(sb.toString(), null);
    }

    // -------------------------------------------------------------------------    
    // -------------------------------------------------------------------------
    public boolean startNewLevel() {
        if (working) {
            return false;
        }
        boolean success = false;
        working = true;
        progress = 0.0f;
        levelActors.freeze();
        GameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true);

        solidChunks.getChunkList().clear();
        fluidChunks.getChunkList().clear();

        ALL_BLOCK_MAP.init();

        LIGHT_SOURCES.lightSrcList.clear();
        LIGHT_SOURCES.lightSrcList.add(SUNLIGHT);
        LIGHT_SOURCES.lightSrcList.add(levelActors.playerLight);

        CacheModule.deleteCache();

        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                Block entity = new Block("doom0");
                entity.getPos().x = (4 * i) & 0xFFFFFFFE;
                entity.getPos().y = (4 * j) & 0xFFFFFFFE;
                entity.getPos().z = 3 & 0xFFFFFFFE;

                entity.getPrimaryColor().x = 0.5f * i + 0.25f;
                entity.getPrimaryColor().y = 0.5f * j + 0.25f;
                entity.getPrimaryColor().z = 0.0f;

                solidChunks.addBlock(entity, true);

                progress += 100.0f / 9.0f;
            }
        }

        levelActors.configureMainActor(new Vector3f(10.5f, 0.0f, -4.0f), new Vector3f(Camera.Z_AXIS), new Vector3f(Camera.Y_AXIS), new Vector3f(Camera.X_AXIS));

        levelActors.unfreeze();
        progress = 100.0f;
        working = false;
        success = true;
        GameObject.getMusicPlayer().stop();
        return success;
    }

    public boolean generateRandomLevel(RandomLevelGenerator randomLevelGenerator, int numberOfBlocks) {
        if (working) {
            return false;
        }
        working = true;
        levelActors.freeze();

        levelActors.configureMainActor(new Vector3f(10.5f, Chunk.BOUND >> 3, -4.0f), new Vector3f(Camera.Z_AXIS), new Vector3f(Camera.Y_AXIS), new Vector3f(Camera.X_AXIS));

        boolean success = false;
        progress = 0.0f;
        GameObject.getMusicPlayer().play(AudioFile.RANDOM, true);

        solidChunks.getChunkList().clear();
        fluidChunks.getChunkList().clear();

        ALL_BLOCK_MAP.init();

        LIGHT_SOURCES.lightSrcList.clear();
        LIGHT_SOURCES.lightSrcList.add(SUNLIGHT);
        LIGHT_SOURCES.lightSrcList.add(levelActors.playerLight);

        CacheModule.deleteCache();

        if (numberOfBlocks > 0 && numberOfBlocks <= MAX_NUM_OF_SOLID_BLOCKS + MAX_NUM_OF_FLUID_BLOCKS) {
            randomLevelGenerator.setNumberOfBlocks(numberOfBlocks);
            randomLevelGenerator.generate();
            success = true;
        }

//        solidChunks.updateSolids(this);
//        fluidChunks.updateFluids(this);
        progress = 100.0f;
        working = false;

        levelActors.unfreeze();
        GameObject.getMusicPlayer().stop();
        return success;
    }

    private boolean storeLevelToBuffer() {
        working = true;
        boolean success = false;
        if (progress > 0.0f) {
            return false;
        }
        progress = 0.0f;
        levelActors.freeze();
        GameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true);
        pos = 0;
        buffer[0] = 'D';
        buffer[1] = 'S';
        pos += 2;

        Camera camera = levelActors.mainCamera();
        if (camera == null) {
            return false;
        }

        byte[] campos = Vector3fUtils.vec3fToByteArray(camera.getPos());
        System.arraycopy(campos, 0, buffer, pos, campos.length);
        pos += campos.length;

        byte[] camfront = Vector3fUtils.vec3fToByteArray(camera.getFront());
        System.arraycopy(camfront, 0, buffer, pos, camfront.length);
        pos += camfront.length;

        byte[] camup = Vector3fUtils.vec3fToByteArray(camera.getUp());
        System.arraycopy(camup, 0, buffer, pos, camup.length);
        pos += camup.length;

        byte[] camright = Vector3fUtils.vec3fToByteArray(camera.getRight());
        System.arraycopy(camup, 0, buffer, pos, camright.length);
        pos += camright.length;

        buffer[pos++] = 'S';
        buffer[pos++] = 'O';
        buffer[pos++] = 'L';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        List<Block> solidBlocks = solidChunks.getTotalList();
        List<Block> fluidBlocks = fluidChunks.getTotalList();

        int solidNum = cacheModule.totalSize(true);
        buffer[pos++] = (byte) (solidNum);
        buffer[pos++] = (byte) (solidNum >> 8);

        //----------------------------------------------------------------------
        for (Block solidBlock : solidBlocks) {
            if (GameObject.MY_WINDOW.shouldClose()) {
                break;
            }
            byte[] byteArraySolid = solidBlock.toByteArray();
            System.arraycopy(byteArraySolid, 0, buffer, pos, 29);
            pos += 29;
            progress += 100.0f / (solidBlocks.size() + fluidBlocks.size());
        }

        buffer[pos++] = 'F';
        buffer[pos++] = 'L';
        buffer[pos++] = 'U';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        int fluidNum = cacheModule.totalSize(false);
        buffer[pos++] = (byte) (fluidNum);
        buffer[pos++] = (byte) (fluidNum >> 8);

        for (Block fluidBlock : fluidBlocks) {
            if (GameObject.MY_WINDOW.shouldClose()) {
                break;
            }
            byte[] byteArrayFluid = fluidBlock.toByteArray();
            System.arraycopy(byteArrayFluid, 0, buffer, pos, 29);
            pos += 29;
            progress += 100.0f / (solidBlocks.size() + fluidBlocks.size());
        }

        buffer[pos++] = 'E';
        buffer[pos++] = 'N';
        buffer[pos++] = 'D';

        levelActors.unfreeze();
        progress = 100.0f;

        if (progress == 100.0f && !GameObject.MY_WINDOW.shouldClose()) {
            success = true;
        }
        working = false;
        GameObject.getMusicPlayer().stop();
        return success;
    }

    private boolean loadLevelFromBuffer() {
        working = true;
        boolean success = false;
        if (progress > 0.0f) {
            return false;
        }
        progress = 0.0f;
        levelActors.freeze();
        GameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true);
        pos = 0;
        if (buffer[0] == 'D' && buffer[1] == 'S') {
            solidChunks.getChunkList().clear();
            fluidChunks.getChunkList().clear();

            ALL_BLOCK_MAP.init();

            LIGHT_SOURCES.lightSrcList.clear();
            LIGHT_SOURCES.lightSrcList.add(SUNLIGHT);
            LIGHT_SOURCES.lightSrcList.add(levelActors.playerLight);

            CacheModule.deleteCache();

            pos += 2;
            byte[] posArr = new byte[12];
            System.arraycopy(buffer, pos, posArr, 0, posArr.length);
            Vector3f campos = Vector3fUtils.vec3fFromByteArray(posArr);
            pos += posArr.length;

            byte[] frontArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, frontArr.length);
            Vector3f camfront = Vector3fUtils.vec3fFromByteArray(frontArr);
            pos += frontArr.length;

            byte[] upArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, upArr.length);
            Vector3f camup = Vector3fUtils.vec3fFromByteArray(upArr);
            pos += upArr.length;

            byte[] rightArr = new byte[12];
            System.arraycopy(buffer, pos, rightArr, 0, rightArr.length);
            Vector3f camright = Vector3fUtils.vec3fFromByteArray(rightArr);
            pos += rightArr.length;

            levelActors.configureMainActor(campos, camfront, camup, camright);

            char[] solid = new char[5];
            for (int i = 0; i < solid.length; i++) {
                solid[i] = (char) buffer[pos++];
            }
            String strSolid = String.valueOf(solid);

            if (strSolid.equals("SOLID")) {
                int solidNum = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                pos += 2;
                for (int i = 0; i < solidNum && !GameObject.MY_WINDOW.shouldClose(); i++) {
                    byte[] byteArraySolid = new byte[29];
                    System.arraycopy(buffer, pos, byteArraySolid, 0, 29);
                    Block solidBlock = Block.fromByteArray(byteArraySolid, true);
                    solidChunks.addBlock(solidBlock, true);
                    pos += 29;
                    progress += 50.0f / solidNum;
                }

//                solidChunks.updateSolids();
                char[] fluid = new char[5];
                for (int i = 0; i < fluid.length; i++) {
                    fluid[i] = (char) buffer[pos++];
                }
                String strFluid = String.valueOf(fluid);

                if (strFluid.equals("FLUID")) {
                    int fluidNum = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                    pos += 2;
                    for (int i = 0; i < fluidNum && !GameObject.MY_WINDOW.shouldClose(); i++) {
                        byte[] byteArrayFluid = new byte[29];
                        System.arraycopy(buffer, pos, byteArrayFluid, 0, 29);
                        Block fluidBlock = Block.fromByteArray(byteArrayFluid, false);
                        fluidChunks.addBlock(fluidBlock, true);
                        pos += 29;
                        progress += 50.0f / fluidNum;
                    }

//                    fluidChunks.updateFluids();
                    char[] end = new char[3];
                    for (int i = 0; i < end.length; i++) {
                        end[i] = (char) buffer[pos++];
                    }
                    String strEnd = String.valueOf(end);
                    if (strEnd.equals("END")) {
                        success = true;
                    }
                }

            }

        }
        levelActors.unfreeze();
        progress = 100.0f;
        working = false;
        GameObject.getMusicPlayer().stop();
        return success;
    }

    public boolean saveLevelToFile(String filename) {
        if (working) {
            return false;
        }
        boolean success = false;
        if (!filename.endsWith(".dat")) {
            filename += ".dat";
        }
        BufferedOutputStream bos = null;
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
        success = storeLevelToBuffer(); // saves level to bufferVertices first
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(buffer, 0, pos); // save bufferVertices to file at pos mark
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
        if (bos != null) {
            try {
                bos.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        }
        return success;
    }

    public boolean loadLevelFromFile(String filename) {
        if (working) {
            return false;
        }
        boolean success = false;
        if (filename.isEmpty()) {
            return false;
        }
        if (!filename.endsWith(".dat")) {
            filename += ".dat";
        }
        File file = new File(filename);
        BufferedInputStream bis = null;
        if (!file.exists()) {
            return false; // this prevents further fail
        }
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(buffer);
            success = loadLevelFromBuffer();
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
        if (bis != null) {
            try {
                bis.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        }
        return success;
    }

    public void animate() {
        if (!working) {
            fluidChunks.animate();
        }
    }

    public boolean isCameraInFluid() {
        boolean yea = false;
        Vector3f camPos = levelActors.mainCamera().getPos();

        Vector3f obsCamPosAlign = new Vector3f(
                Math.round(camPos.x + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.y + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.z + 0.5f) & 0xFFFFFFFE
        );

        yea = ALL_BLOCK_MAP.isLocationPopulated(obsCamPosAlign, false);

        if (!yea) {
            for (int j = 0; j <= 5; j++) {
                Vector3f adjPos = Block.getAdjacentPos(camPos, j, 2.83f);
                Vector3f adjAlign = new Vector3f(
                        Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
                );

                boolean fluidOnLoc = ALL_BLOCK_MAP.isLocationPopulated(adjAlign, false);

                if (fluidOnLoc) {
                    yea = Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, camPos);

                    if (yea) {
                        break;
                    }
                }
            }
        }

        return yea;
    }

    public boolean hasCollisionWithEnvironment(Critter critter) {
        boolean coll;
        coll = (!SKYBOX.containsInsideExactly(critter.getPredictor()));

        if (!coll) {
            final float stepAmount = 0.1f;

            Vector3f predAlign = new Vector3f(
                    Math.round(critter.getPredictor().x + 0.5f) & 0xFFFFFFFE,
                    Math.round(critter.getPredictor().y + 0.5f) & 0xFFFFFFFE,
                    Math.round(critter.getPredictor().z + 0.5f) & 0xFFFFFFFE
            );

            coll = ALL_BLOCK_MAP.isLocationPopulated(predAlign, true);

            if (!coll) {
                OUTER:
                for (int j = 0; j <= 5; j++) {
                    for (float amount = 0.0f; amount <= Game.AMOUNT * Game.TPS; amount += stepAmount) {
                        Vector3f adjPos = Block.getAdjacentPos(critter.getPredictor(), j, amount);
                        Vector3f adjAlign = new Vector3f(
                                Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                                Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                                Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
                        );

                        boolean solidOnLoc = ALL_BLOCK_MAP.isLocationPopulated(adjAlign, true);

                        if (solidOnLoc) {
                            coll = Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, critter.getPredictor())
                                    || Model.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f, critter.getPredictor(), 0.075f, 0.075f, 0.075f);
                            if (coll) {
                                break OUTER;
                            }
                        }
                    }
                }
            }
        }

        return coll;
    }

    public boolean hasCollisionWithEnvironment(ModelCritter livingCritter) {
        boolean coll;
        coll = (!SKYBOX.containsInsideExactly(livingCritter.getPredictor())
                || !SKYBOX.intersectsExactly(livingCritter.getPredictor(), livingCritter.getModel().getWidth(),
                        livingCritter.getModel().getHeight(), livingCritter.getModel().getDepth()));

        if (!coll) {
            final float stepAmount = 0.1f;

            Vector3f predAlign = new Vector3f(
                    Math.round(livingCritter.getPredictor().x + 0.5f) & 0xFFFFFFFE,
                    Math.round(livingCritter.getPredictor().y + 0.5f) & 0xFFFFFFFE,
                    Math.round(livingCritter.getPredictor().z + 0.5f) & 0xFFFFFFFE
            );

            coll = ALL_BLOCK_MAP.isLocationPopulated(predAlign, true);

            if (!coll) {
                OUTER:
                for (int j = 0; j <= 5; j++) {
                    for (float amount = 0.0f; amount <= Game.AMOUNT * Game.TPS; amount += stepAmount) {
                        Vector3f adjPos = Block.getAdjacentPos(livingCritter.getPredictor(), j, amount);
                        Vector3f adjAlign = new Vector3f(
                                Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                                Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                                Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
                        );

                        boolean solidOnLoc = ALL_BLOCK_MAP.isLocationPopulated(adjAlign, true);

                        if (solidOnLoc) {
                            coll = Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, livingCritter.getPredictor())
                                    || livingCritter.getModel().intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f);

                            if (coll) {
                                break OUTER;
                            }
                        }
                    }
                }
            }
        }

        return coll;
    }

    // thats what gravity does, object fells down if they don't have support below it (sky or other object)
    @Override
    public void gravityDo(float deltaTime) {
        if (working) {
            return;
        }
        /*
        float value = (GRAVITY_CONSTANT * deltaTime * deltaTime) / 2.0f;

        List<Vector3f> populatedFluidLocations = ALL_BLOCK_MAP.getPopulatedLocations(vChnkIdQueue, i -> !i.solid);
        for (Vector3f fluidLoc : populatedFluidLocations) {
            for (int j = 0; j <= 5; j++) {
                if (j != Block.TOP) {
                    Vector3f adjFluidLoc = Block.getAdjacentPos(fluidLoc, j);
                    if (!ALL_BLOCK_MAP.isLocationPopulated(adjFluidLoc)) {
                        TexByte texByte = ALL_BLOCK_MAP.getLocation(fluidLoc);
                        int chunkId = Chunk.chunkFunc(fluidLoc);
                        Chunk fluidChunk = fluidChunks.getChunk(chunkId);
                        if (fluidChunk != null) {
                            Tuple tuple = fluidChunk.getTuple(texByte.texName, ~texByte.byteValue & 63);
                            if (tuple != null) {
                                Block fluidBlk = tuple.getBlock(fluidLoc);
                                if (fluidBlk != null) {
                                    for (Vertex v : fluidBlk.getVertices()) {
                                        v.getPos().y -= value;
                                    }
                                    fluidChunk.unbuffer();
                                }
                            }
                        }
                    }
                }
            }
        }*/
    }

    // method for determining visible chunks
    public boolean determineVisible() {
        Camera mainCamera = levelActors.mainCamera();
        boolean val = Chunk.determineVisible(vChnkIdQueue, iChnkIdQueue, mainCamera.getPos());

        return val;
    }

    // method for saving invisible chunks / loading visible chunks
    public boolean chunkOperations() {
        boolean changed = false;
        if (!working) {
            Integer visibleId = vChnkIdQueue.poll();
            if (visibleId != null) {
                changed |= cacheModule.loadFromDisk(visibleId, true);
                changed |= cacheModule.loadFromDisk(visibleId, false);
            }
            //----------------------------------------------------------
            Integer invisibleId = iChnkIdQueue.poll();
            if (invisibleId != null) {
                changed |= cacheModule.saveToDisk(invisibleId, true);
                changed |= cacheModule.saveToDisk(invisibleId, false);
            }
        }

        return changed;
    }

    public void update(float deltaTime) { // call it externally from the main thread 
        if (!working) { // don't update if working, it may screw up!
            SKYBOX.setrY(SKYBOX.getrY() + deltaTime / 16.0f);
            SUN.pos.rotateAxis(deltaTime / 16.0f, 0.0f, 0.0f, 1.0f);
            cameraInFluid = isCameraInFluid();

            Camera mainCamera = levelActors.mainCamera();
            levelActors.playerLight.pos = mainCamera.getPos();

            LIGHT_SOURCES.modified = true;
        }
    }

    public void optimize() {
        if (!working) {
            Vector3f camFront = levelActors.mainCamera().getFront();
            solidChunks.optimize(vChnkIdQueue, camFront);
            fluidChunks.optimize(vChnkIdQueue, camFront);
        }
    }

    public void render() { // render for regular level rendering
        if (working) {
            return;
        }

        if (!SKYBOX.isBuffered()) {
            SKYBOX.bufferAll();
        }
        SKYBOX.render(LIGHT_SOURCES, ShaderProgram.getMainShader());

        if (!SUN.isBuffered()) {
            SUN.bufferAll();
        }
        SUN.render(LIGHT_SOURCES, ShaderProgram.getMainShader());

        // only visible & uncached are in chunk list      
        solidChunks.render(vChnkIdQueue, ShaderProgram.getVoxelShader(), LIGHT_SOURCES);

        // prepare alters tex coords based on whether or not camera is submerged in fluid   
        fluidChunks.prepareOptimized(cameraInFluid);
        // only visible & uncached are in chunk list 
        fluidChunks.render(vChnkIdQueue, ShaderProgram.getVoxelShader(), LIGHT_SOURCES);

        Block editorNew = Editor.getSelectedNew();
        if (editorNew != null) {
            if (!editorNew.isBuffered()) {
                editorNew.bufferAll();
            }
            editorNew.render(LIGHT_SOURCES, ShaderProgram.getMainShader());
        }

        Block selectedNewWireFrame = Editor.getSelectedNewWireFrame();
        if (selectedNewWireFrame != null) {
            if (!selectedNewWireFrame.isBuffered()) {
                selectedNewWireFrame.bufferAll();
            }
            selectedNewWireFrame.render(LIGHT_SOURCES, ShaderProgram.getMainShader());
        }

        Block selectedCurrFrame = Editor.getSelectedCurrWireFrame();
        if (selectedCurrFrame != null) {
            if (!selectedCurrFrame.isBuffered()) {
                selectedCurrFrame.bufferAll();
            }
            selectedCurrFrame.render(LIGHT_SOURCES, ShaderProgram.getMainShader());
        }

        levelActors.render(LIGHT_SOURCES, ShaderProgram.getPlayerShader(), ShaderProgram.getMainShader());

        LIGHT_SOURCES.modified = false;
    }

    public void render(Camera camera) { // render for both regular level rendering and framebuffer (water renderer)        
        if (working) {
            return;
        }

        camera.render(ShaderProgram.getWaterBaseShader());

        if (!SKYBOX.isBuffered()) {
            SKYBOX.bufferAll();
        }
        SKYBOX.render(LIGHT_SOURCES, ShaderProgram.getWaterBaseShader());

        if (!SUN.isBuffered()) {
            SUN.bufferAll();
        }
        SUN.render(LIGHT_SOURCES, ShaderProgram.getWaterBaseShader());

        camera.render(ShaderProgram.getWaterVoxelShader());
        // only visible & uncached are in chunk list      
        solidChunks.render(vChnkIdQueue, ShaderProgram.getWaterVoxelShader(), LIGHT_SOURCES);

        // prepare alters tex coords based on whether or not camera is submerged in fluid
        fluidChunks.prepareOptimized(cameraInFluid);
        // only visible & uncached are in chunk list 
        fluidChunks.render(vChnkIdQueue, ShaderProgram.getWaterVoxelShader(), LIGHT_SOURCES);

        Block editorNew = Editor.getSelectedNew();
        if (editorNew != null) {
            editorNew.setLight(camera.getPos());
            if (!editorNew.isBuffered()) {
                editorNew.bufferAll();
            }
            editorNew.render(LIGHT_SOURCES, ShaderProgram.getWaterBaseShader());
        }

        Block selectedNewWireFrame = Editor.getSelectedNewWireFrame();
        if (selectedNewWireFrame != null) {
            selectedNewWireFrame.setLight(camera.getPos());
            if (!selectedNewWireFrame.isBuffered()) {
                selectedNewWireFrame.bufferAll();
            }
            selectedNewWireFrame.render(LIGHT_SOURCES, ShaderProgram.getWaterBaseShader());
        }

        Block selectedCurrFrame = Editor.getSelectedCurrWireFrame();
        if (selectedCurrFrame != null) {
            selectedCurrFrame.setLight(camera.getPos());
            if (!selectedCurrFrame.isBuffered()) {
                selectedCurrFrame.bufferAll();
            }
            selectedCurrFrame.render(LIGHT_SOURCES, ShaderProgram.getWaterBaseShader());
        }

        levelActors.render(LIGHT_SOURCES, ShaderProgram.getPlayerShader(), ShaderProgram.getWaterBaseShader());

        LIGHT_SOURCES.modified = false;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    public boolean maxSolidReached() {
        return cacheModule.totalSize(true) == MAX_NUM_OF_SOLID_BLOCKS;
    }

    public boolean maxFluidReached() {
        return cacheModule.totalSize(false) == MAX_NUM_OF_FLUID_BLOCKS;
    }

    public void incProgress(float increment) {
        if (progress < 100.0f) {
            progress += increment;
        }
    }

    public Window getMyWindow() {
        return GameObject.MY_WINDOW;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public boolean isWorking() { // damn this one!
        return working || progress > 0.0f;
    }

    public Chunks getSolidChunks() {
        return solidChunks;
    }

    public Chunks getFluidChunks() {
        return fluidChunks;
    }

    public Queue<Integer> getvChnkIdQueue() {
        return vChnkIdQueue;
    }

    public Queue<Integer> getiChnkIdQueue() {
        return iChnkIdQueue;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getPos() {
        return pos;
    }

    public AudioPlayer getMusicPlayer() {
        return GameObject.getMusicPlayer();
    }

    public LevelActors getLevelActors() {
        return levelActors;
    }

}
