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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final GameObject gameObject;

    public static final Block SKYBOX = new Block("night");
    public static final Model SUN = Model.readFromObjFile(Game.WORLD_ENTRY, "sun.obj", "suntx");
    public static final Vector3f SUN_COLOR = new Vector3f(0.75f, 0.5f, 0.25f); // orange-yellow color
    public static final float SUN_SCALE = 64.0f;

    protected final Chunks solidChunks = new Chunks(true);
    protected final Chunks fluidChunks = new Chunks(false);

    public static final int MAX_LIGHTS = 256;
    public static final List<LightSource> LIGHT_SRC = new ArrayList<>();

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
    public static final Map<Vector3f, Pair<String, Byte>> ALL_SOLID_MAP = new HashMap<>(MAX_NUM_OF_SOLID_BLOCKS >> 4);

    // position of all the fluid blocks to texture name & neighbors
    public static final Map<Vector3f, Pair<String, Byte>> ALL_FLUID_MAP = new HashMap<>(MAX_NUM_OF_FLUID_BLOCKS >> 4);

    // std time to live
    public static final float STD_TTL = 30.0f * (float) Game.TICK_TIME;

    protected final CacheModule cacheModule;

    protected static boolean cameraInFluid = false;

    private static byte updatePutSolidNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            Pair<String, Byte> adjPair = ALL_SOLID_MAP.get(adjPos);
            if (adjPair != null) {
                bits |= mask;
                byte adjBits = adjPair.getValue();
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits |= maskAdj;
                adjPair.setValue(adjBits);
            }
        }
        return bits;
    }

    private static byte updatePutFluidNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            Pair<String, Byte> adjPair = ALL_FLUID_MAP.get(adjPos);
            if (adjPair != null) {
                bits |= mask;
                byte adjBits = adjPair.getValue();
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits |= maskAdj;
                adjPair.setValue(adjBits);
            }
        }
        return bits;
    }

    private static byte updateRemSolidNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            Pair<String, Byte> adjPair = ALL_SOLID_MAP.get(adjPos);
            if (adjPair != null) {
                bits |= mask;
                byte adjBits = adjPair.getValue();
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits &= ~maskAdj & 63;
                adjPair.setValue(adjBits);
            }
        }
        return bits;
    }

    private static byte updateRemFluidNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            Pair<String, Byte> adjPair = ALL_FLUID_MAP.get(adjPos);
            if (adjPair != null) {
                bits |= mask;
                byte adjBits = adjPair.getValue();
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits &= ~maskAdj & 63;
                adjPair.setValue(adjBits);
            }
        }
        return bits;
    }

    public static void putBlock(Block block) {
        Vector3f pos = block.getPos();
        String str = block.getTexName();
        if (block.isSolid()) {
            byte bits = updatePutSolidNeighbors(pos);
            Pair<String, Byte> pairX = new Pair<>(str, bits);
            ALL_SOLID_MAP.put(new Vector3f(pos), pairX);
        } else {
            byte bits = updatePutFluidNeighbors(pos);
            Pair<String, Byte> pairX = new Pair<>(str, bits);
            ALL_FLUID_MAP.put(new Vector3f(pos), pairX);
        }
    }

    public static void removeBlock(Block block) {
        Vector3f pos = block.getPos();
        if (block.isSolid()) {
            Pair<String, Byte> pair = ALL_SOLID_MAP.remove(pos);
            if (pair != null && pair.getValue() > 0) {
                updateRemSolidNeighbors(pos);
            }
        } else {
            Pair<String, Byte> pair = ALL_FLUID_MAP.remove(pos);
            if (pair != null && pair.getValue() > 0) {
                updateRemFluidNeighbors(pos);
            }
        }
    }

    static {
        // setting SKYBOX     
        SKYBOX.setPrimaryColor(SKYBOX_COLOR);
        SKYBOX.setUVsForSkybox();
        SKYBOX.setScale(SKYBOX_SCALE);

        SUN.setPrimaryColor(SUN_COLOR);
        SUN.pos = new Vector3f(0.0f, 8192.0f, 0.0f);
        SUN.setScale(SUN_SCALE);
    }

    public LevelContainer(GameObject gameObject) {
        this.gameObject = gameObject;
        this.cacheModule = new CacheModule(this);
    }

    public static void printPositionMaps() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("SOLID POSITION MAP");
        sb.append("(size = ").append(ALL_SOLID_MAP.size()).append(")\n");
        sb.append("---------------------------");
        sb.append("\n");
        sb.append("FLUID POSITION MAP");
        sb.append("(size = ").append(ALL_FLUID_MAP.size()).append(")\n");
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
        gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true);

        solidChunks.getChunkList().clear();
        fluidChunks.getChunkList().clear();

        ALL_SOLID_MAP.clear();
        ALL_FLUID_MAP.clear();
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
        gameObject.getMusicPlayer().stop();
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
        gameObject.getMusicPlayer().play(AudioFile.RANDOM, true);

        solidChunks.getChunkList().clear();
        fluidChunks.getChunkList().clear();

        ALL_SOLID_MAP.clear();
        ALL_FLUID_MAP.clear();
        LIGHT_SRC.clear();
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
        gameObject.getMusicPlayer().stop();
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
        gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true);
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
        gameObject.getMusicPlayer().stop();
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
        gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true);
        pos = 0;
        if (buffer[0] == 'D' && buffer[1] == 'S') {
            solidChunks.getChunkList().clear();
            fluidChunks.getChunkList().clear();

            ALL_SOLID_MAP.clear();
            ALL_FLUID_MAP.clear();
            LIGHT_SRC.clear();

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
        gameObject.getMusicPlayer().stop();
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

        yea = ALL_FLUID_MAP.containsKey(obsCamPosAlign);

        if (!yea) {
            for (int j = 0; j <= 5; j++) {
                Vector3f adjPos = Block.getAdjacentPos(camPos, j, 2.83f);
                Vector3f adjAlign = new Vector3f(
                        Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
                );

                boolean fluidOnLoc = ALL_FLUID_MAP.containsKey(adjAlign);

                if (fluidOnLoc) {
                    yea = Block.containsInsideEqually(adjAlign, 2.83f, 2.83f, 2.83f, camPos);

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
            final float stepAmount = 0.05f;

            Vector3f predAlign = new Vector3f(
                    Math.round(critter.getPredictor().x + 0.5f) & 0xFFFFFFFE,
                    Math.round(critter.getPredictor().y + 0.5f) & 0xFFFFFFFE,
                    Math.round(critter.getPredictor().z + 0.5f) & 0xFFFFFFFE
            );

            coll = ALL_SOLID_MAP.containsKey(predAlign);

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

                        boolean solidOnLoc = ALL_SOLID_MAP.containsKey(adjAlign);

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
            final float stepAmount = 0.05f;

            Vector3f predAlign = new Vector3f(
                    Math.round(livingCritter.getPredictor().x + 0.5f) & 0xFFFFFFFE,
                    Math.round(livingCritter.getPredictor().y + 0.5f) & 0xFFFFFFFE,
                    Math.round(livingCritter.getPredictor().z + 0.5f) & 0xFFFFFFFE
            );

            coll = ALL_SOLID_MAP.containsKey(predAlign);

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

                        boolean solidOnLoc = ALL_SOLID_MAP.containsKey(adjAlign);

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
//        float value = (GRAVITY_CONSTANT * deltaTime * deltaTime) / 2.0f;
//        Map<Vector3f, Integer> solidMap = solidChunks.getPosMap();
//        for (Vector3f solidBlockPos : solidMap.keyQueue()) {
//            Vector3f bottom = new Vector3f(solidBlockPos);
//            bottom.y -= 1.0f;
//            boolean massBelow = false;
//            for (Vector3f otherSolidBlockPos : solidMap.keyQueue()) {
//                if (!solidBlockPos.equals(otherSolidBlockPos)
//                        && Block.containsOnXZEqually(otherSolidBlockPos, 2.0f, bottom)) {
//                    massBelow = true;
//                    break;
//                }
//            }
//            boolean inSkybox = LevelContainer.SKYBOX.containsInsideExactly(bottom);
//            if (!massBelow && inSkybox) {
//                solidBlockPos.y -= value;
//            }
//        }
//        solidChunks.setBuffered(false);
    }

    // method for determining visible chunks
    public void determineVisible() {
        Camera mainCamera = levelActors.mainCamera();
        Chunk.determineVisible(vChnkIdQueue, iChnkIdQueue, mainCamera.getPos());
    }

    // method for saving invisible chunks / loading visible chunks
    public void chunkOperations() {
        if (!working) {
            Integer visibleId = vChnkIdQueue.peek();
            if (visibleId != null) {
                if (CacheModule.isCached(visibleId, true)) {
                    cacheModule.loadFromDisk(visibleId, true);
                }

                if (CacheModule.isCached(visibleId, false)) {
                    cacheModule.loadFromDisk(visibleId, false);
                }
            }
            //----------------------------------------------------------
            Integer invisibleId = iChnkIdQueue.peek();
            if (invisibleId != null) {
                cacheModule.saveToDisk(invisibleId, true);

                cacheModule.saveToDisk(invisibleId, false);
            }
        }
    }

    public void update(float deltaTime) { // call it externally from the main thread 
        if (!working) { // don't update if working, it may screw up!
            SKYBOX.setrY(SKYBOX.getrY() + deltaTime / 16.0f);
            SUN.pos.rotateAxis(deltaTime / 16.0f, 0.0f, 0.0f, 1.0f);
            cameraInFluid = isCameraInFluid();

            Camera mainCamera = levelActors.mainCamera();
            if (LIGHT_SRC.isEmpty()) {
                LIGHT_SRC.add(new LightSource(mainCamera.getPos(), SUN_COLOR, 1.5f));
                LIGHT_SRC.add(new LightSource(SUN.pos, SUN_COLOR, (float) Math.pow(2.0, 28.0)));
            } else {
                LIGHT_SRC.set(0, new LightSource(mainCamera.getPos(), SUN_COLOR, 1.5f));
                LIGHT_SRC.set(1, new LightSource(SUN.pos, SUN_COLOR, (float) Math.pow(2.0, 28.0)));
            }

        }
    }

    public void render() { // render for regular level rendering
        if (working) {
            return;
        }

        Camera mainCamera = levelActors.mainCamera();
        mainCamera.render(ShaderProgram.getMainShader());
        mainCamera.render(ShaderProgram.getVoxelShader());

        if (!SKYBOX.isBuffered()) {
            SKYBOX.bufferAll();
        }
        SKYBOX.render(LIGHT_SRC, ShaderProgram.getMainShader());

        if (!SUN.isBuffered()) {
            SUN.bufferAll();
        }
        SUN.render(LIGHT_SRC, ShaderProgram.getMainShader());

        // only visible & uncached are in chunk list      
        solidChunks.render(vChnkIdQueue, ShaderProgram.getVoxelShader(), LIGHT_SRC);

        // prepare alters tex coords based on whether or not camera is submerged in fluid
        fluidChunks.prepare(cameraInFluid);
        // only visible & uncached are in chunk list 
        fluidChunks.render(vChnkIdQueue, ShaderProgram.getVoxelShader(), LIGHT_SRC);

        Block editorNew = Editor.getSelectedNew();
        if (editorNew != null) {
            editorNew.setLight(mainCamera.getPos());
            if (!editorNew.isBuffered()) {
                editorNew.bufferAll();
            }
            editorNew.render(LIGHT_SRC, ShaderProgram.getMainShader());
        }

        Block selectedNewWireFrame = Editor.getSelectedNewWireFrame();
        if (selectedNewWireFrame != null) {
            if (!selectedNewWireFrame.isBuffered()) {
                selectedNewWireFrame.bufferAll();
            }
            selectedNewWireFrame.render(LIGHT_SRC, ShaderProgram.getMainShader());
        }

        Block selectedCurrFrame = Editor.getSelectedCurrWireFrame();
        if (selectedCurrFrame != null) {
            if (!selectedCurrFrame.isBuffered()) {
                selectedCurrFrame.bufferAll();
            }
            selectedCurrFrame.render(LIGHT_SRC, ShaderProgram.getMainShader());
        }

        levelActors.render(LIGHT_SRC, ShaderProgram.getPlayerShader(), ShaderProgram.getMainShader());
    }

    public void render(Camera camera) { // render for both regular level rendering and framebuffer (water renderer)        
        if (working) {
            return;
        }

        camera.render(ShaderProgram.getWaterBaseShader());
        if (!SKYBOX.isBuffered()) {
            SKYBOX.bufferAll();
        }
        SKYBOX.render(LIGHT_SRC, ShaderProgram.getWaterBaseShader());

        if (!SUN.isBuffered()) {
            SUN.bufferAll();
        }
        SUN.render(LIGHT_SRC, ShaderProgram.getWaterBaseShader());

        camera.render(ShaderProgram.getWaterVoxelShader());
        // only visible & uncached are in chunk list      
        solidChunks.render(vChnkIdQueue, ShaderProgram.getWaterVoxelShader(), LIGHT_SRC);

        // prepare alters tex coords based on whether or not camera is submerged in fluid
        fluidChunks.prepare(cameraInFluid);
        // only visible & uncached are in chunk list 
        fluidChunks.render(vChnkIdQueue, ShaderProgram.getWaterVoxelShader(), LIGHT_SRC);

        Block editorNew = Editor.getSelectedNew();
        if (editorNew != null) {
            editorNew.setLight(camera.getPos());
            if (!editorNew.isBuffered()) {
                editorNew.bufferAll();
            }
            editorNew.render(LIGHT_SRC, ShaderProgram.getWaterBaseShader());
        }

        Block selectedNewWireFrame = Editor.getSelectedNewWireFrame();
        if (selectedNewWireFrame != null) {
            selectedNewWireFrame.setLight(camera.getPos());
            if (!selectedNewWireFrame.isBuffered()) {
                selectedNewWireFrame.bufferAll();
            }
            selectedNewWireFrame.render(LIGHT_SRC, ShaderProgram.getWaterBaseShader());
        }

        Block selectedCurrFrame = Editor.getSelectedCurrWireFrame();
        if (selectedCurrFrame != null) {
            selectedCurrFrame.setLight(camera.getPos());
            if (!selectedCurrFrame.isBuffered()) {
                selectedCurrFrame.bufferAll();
            }
            selectedCurrFrame.render(LIGHT_SRC, ShaderProgram.getWaterBaseShader());
        }

        levelActors.render(LIGHT_SRC, ShaderProgram.getPlayerShader(), ShaderProgram.getWaterBaseShader());
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

    public GameObject getGameObject() {
        return gameObject;
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
        return gameObject.getMusicPlayer();
    }

    public LevelActors getLevelActors() {
        return levelActors;
    }

    public static List<LightSource> getLIGHT_SRC() {
        return LIGHT_SRC;
    }

}
