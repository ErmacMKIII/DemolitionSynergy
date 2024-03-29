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
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;
import org.joml.Random;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.cache.CacheModule;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.chunk.Chunks;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.critter.Player;
import rs.alexanderstojanovich.evg.critter.Predictable;
import rs.alexanderstojanovich.evg.light.LightSource;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.location.BlockLocation;
import rs.alexanderstojanovich.evg.location.TexByte;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.main.GameTime;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.ModelUtils;
import rs.alexanderstojanovich.evg.util.Pair;
import rs.alexanderstojanovich.evg.util.VectorFloatUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LevelContainer implements GravityEnviroment {

    public static int PLAYER_LIGHT_INDEX = 0;
    public static int SUNLIGHT_INDEX = 0;

    protected final Configuration cfg = Configuration.getInstance();

    public static final Block SKYBOX = new Block("night");

    public static final Model SUN = ModelUtils.readFromObjFile(Game.WORLD_ENTRY, "sun.obj", "suntx");
    public static final Vector4f SUN_COLOR_RGBA = new Vector4f(0.75f, 0.5f, 0.25f, 1.0f); // orange-yellow color
    public static final Vector3f SUN_COLOR_RGB = new Vector3f(0.75f, 0.5f, 0.25f); // orange-yellow color RGB

    public static final float SUN_SCALE = 12.0f;
    public static final float SUN_INTENSITY = (float) (1 << 28);

    public static final LightSource SUNLIGHT
            = new LightSource(SUN.pos, SUN_COLOR_RGB, SUN_INTENSITY);

    public final Chunks chunks = new Chunks();

    public static final LightSources LIGHT_SOURCES = new LightSources();

    public static final int LIST_CAPACITY = 8;
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

    private final IList<Integer> vChnkIdList = new GapList<>(LIST_CAPACITY);
    private final IList<Integer> iChnkIdList = new GapList<>(LIST_CAPACITY);

    private final byte[] buffer = new byte[0x1000000]; // 16 MB Buffer
    private int pos = 0;

    public static final float BASE = 20.0f;
    public static final float SKYBOX_SCALE = BASE * BASE * BASE;
    public static final float SKYBOX_WIDTH = 2.0f * SKYBOX_SCALE;

    public static final Vector3f SKYBOX_COLOR_RGB = new Vector3f(0.25f, 0.5f, 0.75f); // cool bluish color for SKYBOX
    public static final Vector4f SKYBOX_COLOR = new Vector4f(0.25f, 0.5f, 0.75f, 0.15f); // cool bluish color for SKYBOX

    public static final int MAX_NUM_OF_BLOCKS = 131070;

    private float progress = 0.0f;

    private boolean working = false;

    public final LevelActors levelActors = new LevelActors();

    // position of all the solid blocks to texture name & neighbors
    public static final BlockLocation ALL_BLOCK_MAP = new BlockLocation();

    // std time to live
    public static final float STD_TTL = 30.0f * (float) Game.TICK_TIME;

    protected final CacheModule cacheModule;

    protected static boolean cameraInFluid = false;

    protected float lastCycledDayTime = 0.0f;

    private static byte updatePutNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            TexByte locVal = ALL_BLOCK_MAP.getLocation(adjPos);
            if (locVal != null) {
                bits |= mask;
                byte adjBits = locVal.byteValue;
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits |= maskAdj;
                locVal.byteValue = adjBits;
            }
        }
        return bits;
    }

    private static byte updateRemNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            TexByte locVal = ALL_BLOCK_MAP.getLocation(adjPos);
            if (locVal != null) {
                bits |= mask;
                byte adjBits = locVal.byteValue;
                int k = ((j & 1) == 0 ? j + 1 : j - 1);
                int maskAdj = 1 << k;
                adjBits &= ~maskAdj & 63;
                locVal.byteValue = adjBits;
            }
        }
        return bits;
    }

    public static void putBlock(Block block) {
        Vector3f pos = block.getPos();
        String str = block.getTexName();
        byte bits = updatePutNeighbors(pos);
        TexByte locVal = new TexByte(str, bits, block.isSolid());
        ALL_BLOCK_MAP.putLocation(new Vector3f(pos), locVal);
    }

    public static void removeBlock(Block block) {
        Vector3f pos = block.getPos();
        boolean rem = ALL_BLOCK_MAP.removeLocation(pos);
        if (rem) {
            updateRemNeighbors(pos);
        }
    }

    static {
        // setting SKYBOX             
        SKYBOX.setPrimaryRGBColor(SKYBOX_COLOR_RGB);
        SKYBOX.setUVsForSkybox();
        SKYBOX.setScale(SKYBOX_SCALE);
        SKYBOX.nullifyNormalsForFace(Block.BOTTOM);
        SKYBOX.setPrimaryColorAlpha(0.15f);

        SUN.setPrimaryRGBColor(SUN_COLOR_RGB);
        SUN.pos = new Vector3f(0.0f, -12288.0f, 0.0f);
        SUNLIGHT.pos = SUN.pos;
        SUN.setScale(SUN_SCALE);
        SUN.setPrimaryColorAlpha(1.0f);
    }

    public LevelContainer() {
        this.cacheModule = new CacheModule(this);

        LIGHT_SOURCES.addLight(levelActors.player.light);
        LIGHT_SOURCES.addLight(SUNLIGHT);
    }

    public static void printPositionMaps() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("POSITION MAP");
        sb.append("(size = ").append(ALL_BLOCK_MAP.getPopulation()).append(")\n");
        sb.append("---------------------------");
        DSLogger.reportDebug(sb.toString(), null);
    }

    public void printQueues() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("VISIBLE QUEUE\n");
        sb.append(vChnkIdList);
        sb.append("\n");
        sb.append("---------------------------");
        sb.append("\n");
        sb.append("INVISIBLE QUEUE\n");
        sb.append(iChnkIdList);
        sb.append("\n");
        sb.append("---------------------------");
        DSLogger.reportDebug(sb.toString(), null);
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

        chunks.clear();
        levelActors.npcList.clear();
        ALL_BLOCK_MAP.init();

        LIGHT_SOURCES.retainLights(2);

        CacheModule.deleteCache();

        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                Block blk = new Block("doom0");
                blk.getPos().x = (4 * i) & 0xFFFFFFFE;
                blk.getPos().y = (4 * j) & 0xFFFFFFFE;
                blk.getPos().z = 3 & 0xFFFFFFFE;

                blk.getPrimaryRGBAColor().x = 0.5f * i + 0.25f;
                blk.getPrimaryRGBAColor().y = 0.5f * j + 0.25f;
                blk.getPrimaryRGBAColor().z = 0.0f;

                chunks.addBlock(blk);

                progress += 100.0f / 9.0f;
            }
        }

        levelActors.configureMainObserver(new Vector3f(10.5f, 0.0f, -4.0f), new Vector3f(Camera.Z_AXIS), new Vector3f(Camera.Y_AXIS), new Vector3f(Camera.X_AXIS));

//        NPC npc = new NPC(critter.getModel());
//        npc.getModel().setPos(new Vector3f(0f, 20f, 0f));
//        levelActors.npcList.add(npc);
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

        levelActors.configureMainObserver(new Vector3f(10.5f, Chunk.BOUND >> 3, -4.0f), new Vector3f(Camera.Z_AXIS), new Vector3f(Camera.Y_AXIS), new Vector3f(Camera.X_AXIS));

        boolean success = false;
        progress = 0.0f;
        GameObject.getMusicPlayer().play(AudioFile.RANDOM, true);

        chunks.clear();

        ALL_BLOCK_MAP.init();

        LIGHT_SOURCES.retainLights(2);

        CacheModule.deleteCache();

        if (numberOfBlocks > 0 && numberOfBlocks <= MAX_NUM_OF_BLOCKS) {
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

    public boolean generateSinglePlayerLevel(RandomLevelGenerator randomLevelGenerator, int numberOfBlocks) {
        if (working) {
            return false;
        }
        working = true;
        levelActors.freeze();

        boolean success = false;
        progress = 0.0f;
        GameObject.getMusicPlayer().play(AudioFile.RANDOM, true);

        chunks.clear();

        ALL_BLOCK_MAP.init();

        LIGHT_SOURCES.retainLights(2);

        CacheModule.deleteCache();

        if (numberOfBlocks > 0 && numberOfBlocks <= MAX_NUM_OF_BLOCKS) {
            // generate blocks
            randomLevelGenerator.setNumberOfBlocks(numberOfBlocks);
            randomLevelGenerator.generate();

            // place player on his/her position
            LevelContainer levelContainer = GameObject.getLevelContainer();
            Player player = (Player) levelContainer.levelActors.player;
            Random random = GameObject.getRandomLevelGenerator().getRandom();

            // random on elements in the center
            final int halfDim = Chunk.GRID_SIZE / 2;
            final int ldim = Math.max(halfDim - 1, 0);
            final int rdim = Math.min(halfDim + 1, Chunk.CHUNK_NUM - 1);

            int attempts = 0;
            IList<Vector3f> solidPopLoc;
            do {
                // choosing random solid location                            
                int randCol = ldim + random.nextInt(rdim - ldim);
                int randRow = ldim + random.nextInt(rdim - ldim);
                int chunkId = randRow * Chunk.GRID_SIZE + randCol;
                solidPopLoc = LevelContainer.ALL_BLOCK_MAP.getPopulatedLocations(chunkId, loc -> loc.solid && ((loc.byteValue & (~Block.Y_MASK & 63))) != 0);
                attempts++;
            } while (solidPopLoc.isEmpty() && attempts < Chunk.GRID_SIZE);

            do {
                int rindex = random.nextInt(solidPopLoc.size());
                Vector3f solidLoc = solidPopLoc.get(rindex);
                Vector3f playerLoc = new Vector3f(solidLoc.x, solidLoc.y + 2.1f, solidLoc.z);
                player.setPos(playerLoc);
            } while (LevelContainer.hasCollisionWithEnvironment((Critter) player));
            player.movePredictorUp(0.0f);
            player.ascend(0.0f); // Stop player changing location work            

            success = true;
        }

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

        byte[] campos = VectorFloatUtils.vec3fToByteArray(camera.getPos());
        System.arraycopy(campos, 0, buffer, pos, campos.length);
        pos += campos.length;

        byte[] camfront = VectorFloatUtils.vec3fToByteArray(camera.getFront());
        System.arraycopy(camfront, 0, buffer, pos, camfront.length);
        pos += camfront.length;

        byte[] camup = VectorFloatUtils.vec3fToByteArray(camera.getUp());
        System.arraycopy(camup, 0, buffer, pos, camup.length);
        pos += camup.length;

        byte[] camright = VectorFloatUtils.vec3fToByteArray(camera.getRight());
        System.arraycopy(camup, 0, buffer, pos, camright.length);
        pos += camright.length;

        IList<Block> allBlocks = chunks.getTotalList();
        Predicate predSolid = (Predicate<Block>) (Block t) -> t.isSolid();
        IList<Block> solidBlocks = (IList<Block>) allBlocks.filter(predSolid);

        Predicate predFluid = (Predicate<Block>) (Block t) -> !t.isSolid();
        IList<Block> fluidBlocks = (IList<Block>) allBlocks.filter(predFluid);

        buffer[pos++] = 'S';
        buffer[pos++] = 'O';
        buffer[pos++] = 'L';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        int solidNum = solidBlocks.size();
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
            progress += 50.0f / (float) allBlocks.size();
        }

        buffer[pos++] = 'F';
        buffer[pos++] = 'L';
        buffer[pos++] = 'U';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        int fluidNum = fluidBlocks.size();
        buffer[pos++] = (byte) (fluidNum);
        buffer[pos++] = (byte) (fluidNum >> 8);

        for (Block fluidBlock : fluidBlocks) {
            if (GameObject.MY_WINDOW.shouldClose()) {
                break;
            }
            byte[] byteArrayFluid = fluidBlock.toByteArray();
            System.arraycopy(byteArrayFluid, 0, buffer, pos, 29);
            pos += 29;
            progress += 50.0f / (float) allBlocks.size();
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
            chunks.clear();

            ALL_BLOCK_MAP.init();

            LIGHT_SOURCES.retainLights(2);

            CacheModule.deleteCache();

            pos += 2;
            byte[] posArr = new byte[12];
            System.arraycopy(buffer, pos, posArr, 0, posArr.length);
            Vector3f campos = VectorFloatUtils.vec3fFromByteArray(posArr);
            pos += posArr.length;

            byte[] frontArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, frontArr.length);
            Vector3f camfront = VectorFloatUtils.vec3fFromByteArray(frontArr);
            pos += frontArr.length;

            byte[] upArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, upArr.length);
            Vector3f camup = VectorFloatUtils.vec3fFromByteArray(upArr);
            pos += upArr.length;

            byte[] rightArr = new byte[12];
            System.arraycopy(buffer, pos, rightArr, 0, rightArr.length);
            Vector3f camright = VectorFloatUtils.vec3fFromByteArray(rightArr);
            pos += rightArr.length;

            levelActors.configureMainObserver(campos, camfront, camup, camright);

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
                    chunks.addBlock(solidBlock);
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
                        chunks.addBlock(fluidBlock);
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

    private boolean loadLevelFromBufferAsNewFormat() {
        working = true;
        boolean success = false;
        if (progress > 0.0f) {
            return false;
        }
        progress = 0.0f;
        levelActors.freeze();
        GameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true);
        pos = 0;
        if (buffer[0] == 'D' && buffer[1] == 'S' && buffer[2] == 'X') {
            chunks.clear();

            ALL_BLOCK_MAP.init();

            LIGHT_SOURCES.retainLights(2);

            CacheModule.deleteCache();

            pos += 3;
            byte[] posArr = new byte[12];
            System.arraycopy(buffer, pos, posArr, 0, posArr.length);
            Vector3f campos = VectorFloatUtils.vec3fFromByteArray(posArr);
            pos += posArr.length;

            byte[] frontArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, frontArr.length);
            Vector3f camfront = VectorFloatUtils.vec3fFromByteArray(frontArr);
            pos += frontArr.length;

            byte[] upArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, upArr.length);
            Vector3f camup = VectorFloatUtils.vec3fFromByteArray(upArr);
            pos += upArr.length;

            byte[] rightArr = new byte[12];
            System.arraycopy(buffer, pos, rightArr, 0, rightArr.length);
            Vector3f camright = VectorFloatUtils.vec3fFromByteArray(rightArr);
            pos += rightArr.length;

            levelActors.configureMainObserver(campos, camfront, camup, camright);

            char[] solid = new char[5];
            for (int i = 0; i < solid.length; i++) {
                solid[i] = (char) buffer[pos++];
            }
            String strSolid = String.valueOf(solid);

            if (strSolid.equals("SOLID")) {
                int solidNum = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                pos += 2;
                for (int i = 0; i < solidNum && !GameObject.MY_WINDOW.shouldClose(); i++) {
                    byte[] byteArraySolid = new byte[40];
                    System.arraycopy(buffer, pos, byteArraySolid, 0, 30);
                    Block solidBlock = Block.fromNewByteArray(byteArraySolid);
                    chunks.addBlock(solidBlock);
                    pos += 30;
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
                        byte[] byteArrayFluid = new byte[30];
                        System.arraycopy(buffer, pos, byteArrayFluid, 0, 30);
                        Block fluidBlock = Block.fromNewByteArray(byteArrayFluid);
                        chunks.addBlock(fluidBlock);
                        pos += 30;
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
            chunks.animate();
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

    public static boolean hasCollisionWithEnvironment(Predictable predictable) {
        boolean coll;
        coll = (!SKYBOX.containsInsideExactly(predictable.getPredictor()));

        if (!coll) {
            final float stepAmount = 0.0125f;

            Vector3f predAlign = new Vector3f(
                    Math.round(predictable.getPredictor().x + 0.5f) & 0xFFFFFFFE,
                    Math.round(predictable.getPredictor().y + 0.5f) & 0xFFFFFFFE,
                    Math.round(predictable.getPredictor().z + 0.5f) & 0xFFFFFFFE
            );

            coll = ALL_BLOCK_MAP.isLocationPopulated(predAlign, true);

            if (!coll) {
                OUTER:
                for (int j = 0; j <= 13; j++) {
                    for (float amount = 0.0f; amount <= 4.0f * Game.AMOUNT; amount += stepAmount) { // double amount precision
                        Vector3f adjPos = Block.getAdjacentPos(predictable.getPredictor(), j, amount);
                        Vector3f adjAlign = new Vector3f(
                                Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                                Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                                Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
                        );

                        boolean solidOnLoc = ALL_BLOCK_MAP.isLocationPopulated(adjAlign, true);

                        if (solidOnLoc) {
                            coll = Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, predictable.getPredictor())
                                    || Model.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f, predictable.getPredictor(), 0.075f, 0.075f, 0.075f);
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

    public static boolean hasCollisionWithEnvironment(Observer observer) {
        boolean coll;
        coll = (!SKYBOX.containsInsideExactly(observer.getPos())
                || !SKYBOX.intersectsExactly(observer.getPos(), 0.07f, 0.07f, 0.07f));

        if (!coll) {
            final float stepAmount = 0.0125f;

            Vector3f predAlign = new Vector3f(
                    Math.round(observer.getPos().x + 0.5f) & 0xFFFFFFFE,
                    Math.round(observer.getPos().y + 0.5f) & 0xFFFFFFFE,
                    Math.round(observer.getPos().z + 0.5f) & 0xFFFFFFFE
            );

            coll = ALL_BLOCK_MAP.isLocationPopulated(predAlign, true);

            if (!coll) {
                OUTER:
                for (int j = 0; j <= 13; j++) {
                    for (float amount = 0.0f; amount <= 4.0f * Game.AMOUNT; amount += stepAmount) { // double amount precision
                        Vector3f adjPos = Block.getAdjacentPos(observer.getPos(), j, amount);
                        Vector3f adjAlign = new Vector3f(
                                Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                                Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                                Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
                        );

                        boolean solidOnLoc = ALL_BLOCK_MAP.isLocationPopulated(adjAlign, true);

                        if (solidOnLoc) {
                            coll = Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, observer.getPos())
                                    || Block.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f,
                                            observer.getPos(), 0.075f, 0.075f, 0.075f);

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

    public static boolean hasCollisionWithEnvironment(Critter critter) {
        boolean coll;
        coll = (!SKYBOX.containsInsideExactly(critter.getPredictor())
                || !SKYBOX.intersectsExactly(critter.getPredictor(), critter.body.getWidth(),
                        critter.body.getHeight(), critter.body.getDepth()));

        if (!coll) {
            final float stepAmount = 0.0125f;

            Vector3f predAlign = new Vector3f(
                    Math.round(critter.getPredictor().x + 0.5f) & 0xFFFFFFFE,
                    Math.round(critter.getPredictor().y + 0.5f) & 0xFFFFFFFE,
                    Math.round(critter.getPredictor().z + 0.5f) & 0xFFFFFFFE
            );

            coll = ALL_BLOCK_MAP.isLocationPopulated(predAlign, true);

            if (!coll) {
                OUTER:
                for (int j = 0; j <= 13; j++) {
                    for (float amount = 0.0f; amount <= 4.0f * Game.AMOUNT; amount += stepAmount) { // double amount precision
                        Vector3f adjPos = Block.getAdjacentPos(critter.getPredictor(), j, amount);
                        Vector3f adjAlign = new Vector3f(
                                Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                                Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                                Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
                        );

                        boolean solidOnLoc = ALL_BLOCK_MAP.isLocationPopulated(adjAlign, true);

                        if (solidOnLoc) {
                            coll = Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, critter.getPredictor())
                                    || Model.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f,
                                            critter.getPredictor(), 1.0f * critter.body.getWidth(), 1.0f * critter.body.getHeight(), 1.0f * critter.body.getDepth());
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
    public boolean gravityDo(float deltaTime) {
        if (working) {
            return false;
        }

        float thrustHeight = 0.0f;
        if (cameraInFluid) {
            final float thrustForce = 8.0f * WATER_DENSITY * GRAVITY_CONSTANT;
            final float mass = 2.5f;
            final float accel = thrustForce / mass;
            thrustHeight = accel * deltaTime * deltaTime / 2.1f;
        }

        boolean collision = false;
        float deltaHeight = (GRAVITY_CONSTANT * deltaTime * deltaTime - 8E-4f * thrustHeight) / 2.1f;
        final float stepAmount = 0.0125f;

        int[] testSides = {Block.BOTTOM, Block.LEFT_BOTTOM, Block.RIGHT_BOTTOM, Block.BOTTOM_BACK, Block.BOTTOM_FRONT};
        SCAN:
        for (float amount = 0.0f; amount <= 16.0f * Game.AMOUNT; amount += stepAmount) { // quad-precision
            for (int i = 0; i < testSides.length; i++) {
                Vector3f adjBottom = Block.getAdjacentPos(levelActors.player.getPos(), testSides[i], amount);
                Vector3f adjBottomAlign = new Vector3f(
                        Math.round(adjBottom.x + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjBottom.y + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjBottom.z + 0.5f) & 0xFFFFFFFE
                );

                boolean solidOnLoc = ALL_BLOCK_MAP.isLocationPopulated(adjBottomAlign, true);
                if (solidOnLoc) {
                    levelActors.player.movePredictorYDown(deltaHeight);
                    // solid object collision
                    collision |= Block.containsInsideEqually(adjBottomAlign, 2.1f, 2.1f, 2.1f, levelActors.player.getPredictor())
                            || Model.intersectsEqually(adjBottomAlign, 2.1f, 2.1f, 2.1f,
                                    levelActors.player.getPredictor(), 1.0f * levelActors.player.body.getWidth(), 1.0f * levelActors.player.body.getHeight(), 1.0f * levelActors.player.body.getDepth());
                    // skybox collision
                    collision |= (!SKYBOX.containsInsideExactly(levelActors.player.getPredictor())
                            || !SKYBOX.intersectsExactly(levelActors.player.getPredictor(), levelActors.player.body.getWidth(),
                                    levelActors.player.body.getHeight(), levelActors.player.body.getDepth()));
                    levelActors.player.movePredictorYUp(deltaHeight);
                    if (collision) {
                        break SCAN;
                    }
                }
            }
        }

        if (!collision) {
            levelActors.player.movePredictorYDown(deltaHeight);
            levelActors.player.sinkY(deltaHeight);
        }

        return !collision;
    }

    @Override
    public boolean jump(Critter critter, float amountY, float deltaTime) {
        float height0 = amountY * deltaTime;
        float thrustHeight = 0.0f;
        if (cameraInFluid) {
            final float thrustForce = 8.0f * WATER_DENSITY * GRAVITY_CONSTANT;
            final float mass = 2.5f;
            final float accel = thrustForce / mass;
            thrustHeight = accel * deltaTime * deltaTime / 2.1f;
        }

        float deltaHeight = (GRAVITY_CONSTANT * deltaTime * deltaTime) / 2.1f;

        float height1 = Math.max(height0 + 5E-4f * thrustHeight - deltaHeight, 0.0f);

        final float stepAmount = 0.0125f;

        boolean collision = false;
        int[] testSides = {Block.TOP, Block.LEFT_TOP, Block.RIGHT_TOP, Block.TOP_BACK, Block.TOP_FRONT};
        SCAN:
        for (float amount = 0.0f; amount <= 16.0f * Game.AMOUNT; amount += stepAmount) { // quad-precision
            for (int i = 0; i < testSides.length; i++) {
                Vector3f adjTop = Block.getAdjacentPos(critter.getPos(), testSides[i], amount);
                Vector3f adjTopAlign = new Vector3f(
                        Math.round(adjTop.x + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjTop.y + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjTop.z + 0.5f) & 0xFFFFFFFE
                );

                boolean solidOnLoc = ALL_BLOCK_MAP.isLocationPopulated(adjTopAlign, true);
                if (solidOnLoc) {
                    critter.movePredictorYUp(height1);
                    // solid object collision
                    collision |= Block.containsInsideEqually(adjTopAlign, 2.1f, 2.1f, 2.1f, critter.getPredictor())
                            || Model.intersectsEqually(adjTopAlign, 2.1f, 2.1f, 2.1f,
                                    critter.getPredictor(), 1.0f * critter.body.getWidth(), 1.0f * critter.body.getHeight(), 1.0f * critter.body.getDepth());
                    // skybox collision
                    collision |= (!SKYBOX.containsInsideExactly(critter.getPredictor())
                            || !SKYBOX.intersectsExactly(critter.getPredictor(), critter.body.getWidth(),
                                    critter.body.getHeight(), levelActors.player.body.getDepth()));
                    critter.movePredictorYDown(height1);
                    if (collision) {
                        break SCAN;
                    }
                }
            }
        }

        if (!collision) {
            critter.movePredictorYUp(height1);
            critter.jumpY(height1);
        }

        return !collision;
    }

    // method for determining visible chunks
    public boolean determineVisible() {
        boolean changed = Chunk.determineVisible(vChnkIdList, iChnkIdList, levelActors.mainCamera());

        return changed;
    }

    // method for saving invisible chunks / loading visible chunks
    public boolean chunkOperations() {
        boolean changed = false;
        if (!working) {
            for (int v : vChnkIdList) {
                changed |= cacheModule.loadFromDisk(v);
            }
            for (int i : iChnkIdList) {
                changed |= cacheModule.saveToDisk(i);
            }
        }

        return changed;
    }

    public void update() { // call it externally from the main thread 
        if (!working) { // don't update if working, it may screw up!   
            final float now = GameTime.Now().getTime();
            float dtime = now - lastCycledDayTime;
            lastCycledDayTime = now;

            final float dangle = org.joml.Math.toRadians(dtime * 360.0f / 24.0f);

            SKYBOX.setrY(SKYBOX.getrY() + dangle);
            SUN.pos.rotateZ(dangle);

            final float sunAngle = org.joml.Math.atan2(SUN.pos.y, SUN.pos.x);
            float inten = org.joml.Math.max(org.joml.Math.sin(sunAngle), 0.0f);

            SUN.setPrimaryRGBAColor(new Vector4f((new Vector3f(SUN_COLOR_RGB)).mul(inten), 1.0f));
            SKYBOX.setPrimaryRGBAColor(new Vector4f((new Vector3f(SKYBOX_COLOR_RGB)).mul(Math.max(inten, 0.15f)), 0.15f));
            SUNLIGHT.setIntensity(inten * SUN_INTENSITY);
            cameraInFluid = isCameraInFluid();
            Camera mainCamera = levelActors.mainCamera();
            levelActors.player.light.pos = mainCamera.getPos();
//            chunks.getChunkList().forEach(ch -> ch.decTimeToLive(deltaTime));
        }
    }

    public void optimize() {
        if (!working) {
            Camera mainCamera = levelActors.mainCamera();
            // provide visible chunk id(entifier) list, camera view eye and camera position
            // where all the blocks are pulled into optimized tuples
            chunks.optimizeFast(vChnkIdList, mainCamera);
        }
    }

    public void render() { // render for regular level rendering
        if (working) {
            return;
        }

        if (!SUN.isBuffered()) {
            SUN.bufferAll();
        }

        if (SUNLIGHT.getIntensity() > 0.0f) {
            SUN.render(LIGHT_SOURCES, ShaderProgram.getMainShader());
        }

        if (!SKYBOX.isBuffered()) {
            SKYBOX.bufferAll();
        }
        SKYBOX.renderContour(LIGHT_SOURCES, ShaderProgram.getSkyboxShader());

        // only visible & uncached are in chunk list      
        // prepare alters tex coords based on whether or not camera is submerged in fluid   
        chunks.prepareOptimized(cameraInFluid);
        // only visible & uncached are in chunk list 
        chunks.renderOptimizedReduced(ShaderProgram.getVoxelShader(), LIGHT_SOURCES);

        Block editorNew = Editor.getSelectedNew();
        if (editorNew != null) {
            if (!editorNew.isBuffered()) {
                editorNew.bufferAll();
            }
            editorNew.render(LIGHT_SOURCES, ShaderProgram.getMainShader());

            Block selectedNewWireFrame = Editor.getSelectedNewDecal();
            if (selectedNewWireFrame != null) {
                if (!selectedNewWireFrame.isBuffered()) {
                    selectedNewWireFrame.bufferAll();
                }
                selectedNewWireFrame.renderContour(LIGHT_SOURCES, ShaderProgram.getContourShader());
            }

        }

        Block selectedCurrFrame = Editor.getSelectedCurrDecal();
        if (selectedCurrFrame != null) {
            if (!selectedCurrFrame.isBuffered()) {
                selectedCurrFrame.bufferAll();
            }
            selectedCurrFrame.renderContour(LIGHT_SOURCES, ShaderProgram.getContourShader());
        }
        levelActors.render(LIGHT_SOURCES, ShaderProgram.getPlayerShader(), ShaderProgram.getMainShader());

        LightSources.render(levelActors.mainCamera(), LIGHT_SOURCES, ShaderProgram.getLightShader());
        LIGHT_SOURCES.resetAllModified();
    }

    public void render(Camera camera) { // render for both regular level rendering and framebuffer (water renderer)        
        if (working) {
            return;
        }

        camera.render(ShaderProgram.getWaterBaseShader());

        if (!SUN.isBuffered()) {
            SUN.bufferAll();
        }

        if (SUNLIGHT.getIntensity() > 0.0f) {
            SUN.render(LIGHT_SOURCES, ShaderProgram.getWaterBaseShader());
        }

        if (!SKYBOX.isBuffered()) {
            SKYBOX.bufferAll();
        }
        SKYBOX.render(LIGHT_SOURCES, ShaderProgram.getWaterBaseShader());

        camera.render(ShaderProgram.getWaterVoxelShader());

        // prepare alters tex coords based on whether or not camera is submerged in fluid
        chunks.prepareOptimized(cameraInFluid);
        // only visible & uncached are in chunk list 
        chunks.renderOptimizedReduced(ShaderProgram.getWaterVoxelShader(), LIGHT_SOURCES);

        Block editorNew = Editor.getSelectedNew();
        if (editorNew != null) {
            if (!editorNew.isBuffered()) {
                editorNew.bufferAll();
            }
            editorNew.render(LIGHT_SOURCES, ShaderProgram.getWaterBaseShader());

            Block selectedNewWireFrame = Editor.getSelectedNewDecal();
            if (selectedNewWireFrame != null) {
                if (!selectedNewWireFrame.isBuffered()) {
                    selectedNewWireFrame.bufferAll();
                }
                selectedNewWireFrame.render(LIGHT_SOURCES, ShaderProgram.getWaterBaseShader());
            }

        }

        Block selectedCurrFrame = Editor.getSelectedCurrDecal();
        if (selectedCurrFrame != null) {
            if (!selectedCurrFrame.isBuffered()) {
                selectedCurrFrame.bufferAll();
            }
            selectedCurrFrame.render(LIGHT_SOURCES, ShaderProgram.getWaterBaseShader());
        }
        levelActors.render(LIGHT_SOURCES, ShaderProgram.getWaterBaseShader(), ShaderProgram.getWaterBaseShader());

        LightSources.render(camera, LIGHT_SOURCES, ShaderProgram.getLightShader());
        LIGHT_SOURCES.resetAllModified();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    public boolean maxCountReached() {
        return cacheModule.totalSize() == MAX_NUM_OF_BLOCKS;
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

    public Chunks getChunks() {
        return chunks;
    }

    public CacheModule getCacheModule() {
        return cacheModule;
    }

    public IList<Integer> getvChnkIdList() {
        return vChnkIdList;
    }

    public IList<Integer> getiChnkIdList() {
        return iChnkIdList;
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
