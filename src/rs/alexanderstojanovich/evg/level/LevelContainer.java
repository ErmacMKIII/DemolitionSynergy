/* 
 * Copyright (C) 2020 Alexander Stojanonullch <coas91@rocketmail.com>
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
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.regex.Pattern;
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
import rs.alexanderstojanovich.evg.resources.Assets;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.ModelUtils;
import rs.alexanderstojanovich.evg.util.VectorFloatUtils;
import rs.alexanderstojanovich.evg.weapons.Weapons;

/**
 * World container. Contains everything.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class LevelContainer implements GravityEnviroment {

    // Constants for collision control & gravity handling
    /**
     * Min amount of iteration for collision control or gravity control (inner
     * loop)
     */
    public static final float MIN_AMOUNT = -14f;
    /**
     * Max amount of iteration for collision control or gravity control (inner
     * loop)
     */
    public static final float MAX_AMOUNT = 14f;
    /**
     * Step amount of iteration for collision control or gravity control (inner
     * loop).
     */
    public static final float STEP_AMOUNT = 0.05f;

    // -------------------------------------------------
    /**
     * World level map format. For save/load use.
     */
    public static enum LevelMapFormat {
        /**
         * Old format. Exists for quite long time.
         */
        DAT,
        /**
         * New Format. Blocks are grouped by texture name.
         */
        NDAT
    }

    public final GameObject gameObject;
    protected final Configuration cfg = Configuration.getInstance();

    public static final Block SKYBOX = new Block("night");

    public static final Model SUN = ModelUtils.readFromObjFile(Game.WORLD_ENTRY, "sun.obj", "suntx");
    public static final Vector4f SUN_COLOR_RGBA = new Vector4f(0.75f, 0.5f, 0.25f, 1.0f); // orange-yellow color
    public static final Vector3f SUN_COLOR_RGB = new Vector3f(0.75f, 0.5f, 0.25f); // orange-yellow color RGB

    public static final float SUN_SCALE = 32.0f;
    public static final float SUN_INTENSITY = (float) (1 << 28); // 268.4M

    public static final LightSource SUNLIGHT
            = new LightSource(SUN.pos, SUN_COLOR_RGB, SUN_INTENSITY);

    public final Chunks chunks = new Chunks();
    public final BlockEnvironment blockEnvironment;
    public final LightSources lightSources;

    private final IList<Integer> vChnkIdList = new GapList<>();
    private final IList<Integer> iChnkIdList = new GapList<>();

    public final byte[] buffer = new byte[0x1000000]; // 16 MB Buffer
    public int pos = 0;

    public final byte[] bak_buffer = new byte[0x1000000]; // 16 MB BAK Buffer
    public int bak_pos = 0;

    public static final float BASE = 22.5f;
    public static final float SKYBOX_SCALE = BASE * BASE * BASE;
    public static final float SKYBOX_WIDTH = 2.0f * SKYBOX_SCALE;

    public static final Vector3f NIGHT_SKYBOX_COLOR_RGB = new Vector3f(0.25f, 0.5f, 0.75f); // cool bluish color for SKYBOX
    public static final Vector4f NIGHT_SKYBOX_COLOR = new Vector4f(0.25f, 0.5f, 0.75f, 0.15f); // cool bluish color for SKYBOX

    public static final int MAX_NUM_OF_BLOCKS = 131070;

    private float progress = 0.0f;

    private boolean working = false;

    public final LevelActors levelActors;

    // position of all the solid blocks to texture name & neighbors
    public static final BlockLocation AllBlockMap = new BlockLocation();

    public final CacheModule cacheModule;

    protected static boolean actorInFluid = false;

    protected float lastCycledDayTime = 0.0f;

    protected float fallVelocity = 0.0f;
    protected float jumpVelocity = 0.0f;

    public final Weapons weapons;

    public boolean gravityOn = false;

    public static final int NUM_OF_PASSES_MAX = Configuration.getInstance().getOptimizationPasses();

    private static byte updatePutNeighbors(Vector3f vector) {
        byte bits = 0;
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            int mask = 1 << j;
            Vector3f adjPos = Block.getAdjacentPos(vector, j);
            TexByte locVal = AllBlockMap.getLocation(adjPos);
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
            TexByte locVal = AllBlockMap.getLocation(adjPos);
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

    /**
     * Put block into All Block Map.
     *
     * @param block block
     */
    public static void putBlock(Block block) {
        Vector3f pos = block.getPos();
        String str = block.getTexName();
        byte bits = updatePutNeighbors(pos);
        TexByte locVal = new TexByte(block.getPrimaryRGBAColor(), str, bits, block.isSolid(), block.getId());
        AllBlockMap.putLocation(new Vector3f(pos), locVal);
    }

    /**
     * Remove block from All Block Map.
     *
     * @param block block
     */
    public static void removeBlock(Block block) {
        Vector3f pos = block.getPos();
        boolean rem = AllBlockMap.removeLocation(pos);
        if (rem) {
            updateRemNeighbors(pos);
        }
    }

    static {
        // setting SKYBOX             
        SKYBOX.setPrimaryRGBColor(NIGHT_SKYBOX_COLOR_RGB);
        SKYBOX.setUVsForSkybox();
        SKYBOX.setScale(SKYBOX_SCALE);
        SKYBOX.nullifyNormalsForFace(Block.BOTTOM);
        SKYBOX.setPrimaryColorAlpha(0.15f);

        SUN.setPrimaryRGBColor(new Vector3f(SUN_COLOR_RGB));
        SUN.pos = new Vector3f(0.0f, -10240.0f, 0.0f);
        SUNLIGHT.pos = SUN.pos;
        SUN.setScale(SUN_SCALE);
        SUN.setPrimaryColorAlpha(1.00f);
    }

    public LevelContainer(GameObject gameObject) {
        this.gameObject = gameObject;
        this.blockEnvironment = new BlockEnvironment(gameObject, chunks);
        this.cacheModule = new CacheModule(this);
        this.lightSources = new LightSources();

        this.weapons = new Weapons(this);
        this.levelActors = new LevelActors(this);

        lightSources.addLight(levelActors.player.light);
        lightSources.addLight(SUNLIGHT);
    }

    public static void printPositionMaps() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("POSITION MAP");
        sb.append("(size = ").append(AllBlockMap.getPopulation()).append(")\n");
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
    /**
     * Start new editor scene with 9 'Doom' blocks.
     *
     * @return on success
     */
    public boolean startNewLevel() {
        if (working) {
            return false;
        }
        boolean success = false;
        working = true;
        progress = 0.0f;
        levelActors.freeze();
        gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true, true);

        chunks.clear();
        levelActors.npcList.clear();
        AllBlockMap.init();

        lightSources.retainLights(2);

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

        blockEnvironment.clear();
//        NPC npc = new NPC(critter.getModel());
//        npc.getModel().setPos(new Vector3f(0f, 20f, 0f));
//        levelActors.npcList.add(npc);
        levelActors.unfreeze();
        progress = 100.0f;
        working = false;
        success = true;
        gameObject.getMusicPlayer().stop();
        return success;
    }

    /**
     * Generate random level for editor
     *
     * @param randomLevelGenerator random level generator (from game object)
     * @param numberOfBlocks pre-defined number of blocks
     * @return on success
     */
    public boolean generateRandomLevel(RandomLevelGenerator randomLevelGenerator, int numberOfBlocks) {
        if (working) {
            return false;
        }
        working = true;
        levelActors.freeze();

        levelActors.configureMainObserver(new Vector3f(10.5f, Chunk.BOUND >> 3, -4.0f), new Vector3f(Camera.Z_AXIS), new Vector3f(Camera.Y_AXIS), new Vector3f(Camera.X_AXIS));

        boolean success = false;
        progress = 0.0f;
        if (gameObject.intrface.isHugeLevel()) {
            gameObject.getMusicPlayer().play(AudioFile.RANDOM_PULSE, true, true);
        } else {
            gameObject.getMusicPlayer().play(AudioFile.RANDOM, true, true);
        }

        chunks.clear();

        AllBlockMap.init();

        lightSources.retainLights(2);

        CacheModule.deleteCache();

        if (numberOfBlocks > 0 && numberOfBlocks <= MAX_NUM_OF_BLOCKS) {
            randomLevelGenerator.setNumberOfBlocks(numberOfBlocks);
            randomLevelGenerator.generate();
            success = true;
        }

        blockEnvironment.clear();

        progress = 100.0f;
        working = false;

        levelActors.unfreeze();
        gameObject.getMusicPlayer().stop();
        return success;
    }

    /**
     * Generate random level for single player
     *
     * @param randomLevelGenerator random level generator (from game object)
     * @param numberOfBlocks pre-defined number of blocks
     * @return on success
     * @throws java.lang.Exception if player spawn fails
     */
    public boolean generateSinglePlayerLevel(RandomLevelGenerator randomLevelGenerator, int numberOfBlocks) throws Exception {
        if (working) {
            return false;
        }
        working = true;
        levelActors.freeze();

        boolean success = false;
        progress = 0.0f;
        if (gameObject.intrface.isHugeLevel()) {
            gameObject.getMusicPlayer().play(AudioFile.RANDOM_PULSE, true, true);
        } else {
            gameObject.getMusicPlayer().play(AudioFile.RANDOM, true, true);
        }

        chunks.clear();

        AllBlockMap.init();

        lightSources.retainLights(2);

        CacheModule.deleteCache();

        if (numberOfBlocks > 0 && numberOfBlocks <= MAX_NUM_OF_BLOCKS) {
            // generate blocks
            randomLevelGenerator.setNumberOfBlocks(numberOfBlocks);
            randomLevelGenerator.generate();

            spawnPlayer();

            success = true;
        }

        blockEnvironment.clear();

        progress = 100.0f;
        working = false;

        levelActors.unfreeze();
        gameObject.getMusicPlayer().stop();

        return success;
    }

    /**
     * Generate random level for multiplayer (Host)
     *
     * @param randomLevelGenerator random level generator (from game object)
     * @param numberOfBlocks pre-defined number of blocks
     * @return on success
     * @throws java.lang.Exception if player spawn fails
     */
    public boolean generateMultiPlayerLevel(RandomLevelGenerator randomLevelGenerator, int numberOfBlocks) throws Exception {
        if (working) {
            return false;
        }
        working = true;
        levelActors.freeze();

        boolean success = false;
        progress = 0.0f;
        if (gameObject.intrface.isHugeLevel()) {
            gameObject.getMusicPlayer().play(AudioFile.RANDOM_PULSE, true, true);
        } else {
            gameObject.getMusicPlayer().play(AudioFile.RANDOM, true, true);
        }

        chunks.clear();

        AllBlockMap.init();

        lightSources.retainLights(2);

        CacheModule.deleteCache();

        if (numberOfBlocks > 0 && numberOfBlocks <= MAX_NUM_OF_BLOCKS) {
            // generate blocks
            randomLevelGenerator.setNumberOfBlocks(numberOfBlocks);
            randomLevelGenerator.generate();

            spawnPlayer();

            success = true;
        }

        blockEnvironment.clear();

        progress = 100.0f;
        working = false;

        levelActors.unfreeze();
        gameObject.getMusicPlayer().stop();

        return success;
    }

    /**
     * Set player position. Spawn him/her.
     *
     * @throws java.lang.Exception if spawn player fails
     */
    public void spawnPlayer() throws Exception {
        // Manually turn off gravity so it doesn't affect player during spawn
        gravityOn = false;

        // Place player on his/her position
        LevelContainer levelContainer = gameObject.getLevelContainer();
        Player player = (Player) levelContainer.levelActors.player;
        player.setPos(new Vector3f(0.0f, 256.0f, 0.0f));

        Random random = gameObject.randomLevelGenerator.random;

        // Find suitable chunk in centre to spawn       
        final int halfGrid = Chunk.GRID_SIZE >> 1;
        IList<Integer> midChunks = new GapList<>(Arrays.asList(
                Chunk.GRID_SIZE * (halfGrid - 1) + (halfGrid - 1),
                Chunk.GRID_SIZE * (halfGrid - 1) + halfGrid,
                Chunk.GRID_SIZE * halfGrid + (halfGrid - 1),
                Chunk.GRID_SIZE * halfGrid + halfGrid
        ));

        IList<Vector3f> solidPopLoc;
        do {
            int chunkId = midChunks.get(random.nextInt(midChunks.size()));
//            DSLogger.reportInfo("chunkid=" + chunkId, null);
            // Find non-empty solid location(s)              
            solidPopLoc = LevelContainer.AllBlockMap.getPopulatedLocations(
                    chunkId, loc -> loc.solid && (~loc.byteValue & Block.Y_MASK) == 0
            );
            midChunks.remove((Integer) chunkId);
        } while (solidPopLoc.isEmpty() && !midChunks.isEmpty());

        // Remove populated locations around the chosen location to avoid crowding
        if (solidPopLoc.size() > 500) {
            final int[] testSides = {
                Block.LEFT, Block.RIGHT, Block.BACK, Block.FRONT
            };
            for (float radius = 2.0f; radius <= 16.0f && solidPopLoc.size() > 500; radius += 2.0f) {
                for (int side : testSides) {
                    final float amount = radius;
                    solidPopLoc.removeIf(loc -> AllBlockMap.isLocationPopulated(
                            Block.getAdjacentPos(loc, side, amount), true
                    ));
                }
            }
        }

        // Try to spawn the player at a valid location
        boolean playerSpawned = false;
        OUTER:
        while (!playerSpawned && !solidPopLoc.isEmpty()) { // search through solid population location
            int randomIndex = random.nextInt(solidPopLoc.size());
            Vector3f solidLoc = solidPopLoc.get(randomIndex);
            Vector3f playerLoc = new Vector3f(solidLoc.x, solidLoc.y + 2.2f, solidLoc.z); // Adjust y position

            player.setPos(playerLoc);
            INNER:
            for (Game.Direction dir : Game.Direction.values()) {
                if (hasCollisionWithEnvironment((Critter) player, dir)) { // if any direction causes collision continue OUTER search
                    continue OUTER;
                } else {
                    solidPopLoc.remove(solidLoc);  // Remove invalid location
                }
            }

            playerSpawned = true;
        }

        // Adjust player position if successfully spawned
        if (playerSpawned) {
            player.jumpY(0.0f); // Some bad workaround
            gravityOn = true; // Re-enable gravity
        } else {
            // Handle case where no valid spawn location was found (optional)
            throw new Exception("Failed to spawn player in a valid location.");
        }
    }

    private static String ensureCorrectExtension(String filename) {
        Pattern pattern = Pattern.compile("\\.(dat|ndat)$");
        if (!pattern.matcher(filename).find()) {
            filename += ".dat"; // Default to .dat if no valid extension is found
        }

        return filename;
    }

    /**
     * Store level in binary DAT (old) format to internal level container
     * buffer.
     *
     * @return on success
     */
    public boolean storeLevelToBufferOldFormat() {
        working = true;
        boolean success = false;
//        if (progress > 0.0f) {
//            return false;
//        }
        progress = 0.0f;
        levelActors.freeze();
        gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true, true);
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

        IList<Vector3f> solidPos = AllBlockMap.getPopulatedLocations(tb -> tb.solid);
        IList<Vector3f> fluidPos = AllBlockMap.getPopulatedLocations(tb -> !tb.solid);

        buffer[pos++] = 'S';
        buffer[pos++] = 'O';
        buffer[pos++] = 'L';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        int solidNum = solidPos.size();
        buffer[pos++] = (byte) (solidNum);
        buffer[pos++] = (byte) (solidNum >> 8);

        //----------------------------------------------------------------------
        for (Vector3f sp : solidPos) {
            if (gameObject.WINDOW.shouldClose()) {
                break;
            }
            byte[] byteArraySolid = Block.toByteArray(sp, AllBlockMap.getLocation(sp));
            System.arraycopy(byteArraySolid, 0, buffer, pos, 29);
            pos += 29;
            progress += 50.0f / (float) solidPos.size();
        }

        buffer[pos++] = 'F';
        buffer[pos++] = 'L';
        buffer[pos++] = 'U';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        int fluidNum = fluidPos.size();
        buffer[pos++] = (byte) (fluidNum);
        buffer[pos++] = (byte) (fluidNum >> 8);

        for (Vector3f fp : fluidPos) {
            if (gameObject.WINDOW.shouldClose()) {
                break;
            }
            byte[] byteArrayFluid = Block.toByteArray(fp, AllBlockMap.getLocation(fp));
            System.arraycopy(byteArrayFluid, 0, buffer, pos, 29);
            pos += 29;
            progress += 50.0f / (float) fluidPos.size();
        }

        buffer[pos++] = 'E';
        buffer[pos++] = 'N';
        buffer[pos++] = 'D';

        levelActors.unfreeze();
        progress = 100.0f;

        if (progress == 100.0f && !gameObject.WINDOW.shouldClose()) {
            success = true;
        }
        working = false;
        gameObject.getMusicPlayer().stop();
        return success;
    }

    /**
     * Store level in binary NDAT (new) format to internal level container
     * buffer. Used in Multiplayer.
     *
     * @return on success
     */
    public boolean storeLevelToBufferNewFormat() {
        working = true;
        boolean success = false;
//        if (progress > 0.0f) {
//            return false;
//        }
        progress = 0.0f;
        levelActors.freeze();
        gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true, true);
        pos = 0;
        buffer[0] = 'D';
        buffer[1] = 'S';
        buffer[2] = '2';
        pos += 3;

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
        System.arraycopy(camright, 0, buffer, pos, camright.length);
        pos += camright.length;

        int allBlkSize = AllBlockMap.locationProperties.size();

        buffer[pos++] = 'B';
        buffer[pos++] = 'L';
        buffer[pos++] = 'K';
        buffer[pos++] = 'S';

        // Store the total number of blocks
        buffer[pos++] = (byte) (allBlkSize);
        buffer[pos++] = (byte) (allBlkSize >> 8);
        buffer[pos++] = (byte) (allBlkSize >> 16);
        buffer[pos++] = (byte) (allBlkSize >> 24);

        for (String texName : Assets.TEX_WORLD) {
            IList<Vector3f> blkPos = AllBlockMap.getPopulatedLocations(tb -> tb.texName.equals(texName));
            int count = blkPos.size();
            byte[] texNameBytes = texName.getBytes(Charset.forName("US-ASCII"));
            for (int i = 0; i < 5; i++) {
                buffer[pos++] = texNameBytes[i];
            }
            buffer[pos++] = (byte) (count);
            buffer[pos++] = (byte) (count >> 8);
            buffer[pos++] = (byte) (count >> 16);
            buffer[pos++] = (byte) (count >> 24);
            for (Vector3f p : blkPos) {
                if (gameObject.WINDOW.shouldClose()) {
                    break;
                }
                byte[] byteArray = Block.toByteArray(p, AllBlockMap.getLocation(p));
                System.arraycopy(byteArray, 5, buffer, pos, 24);
                pos += 29;
                progress += 100.0f / (float) allBlkSize;
            }
        }

        buffer[pos++] = 'E';
        buffer[pos++] = 'N';
        buffer[pos++] = 'D';

        levelActors.unfreeze();
        progress = 100.0f;

        if (progress == 100.0f && !gameObject.WINDOW.shouldClose()) {
            success = true;
        }
        working = false;
        gameObject.getMusicPlayer().stop();
        return success;
    }

    /**
     * Load level in binary format DAT (old) to internal level container buffer.
     *
     * @return on success
     */
    public boolean loadLevelFromBufferOldFormat() {
        working = true;
        boolean success = false;
//        if (progress > 0.0f) {
//            return false;
//        }
        progress = 0.0f;
        levelActors.freeze();
        gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true, true);
        pos = 0;
        if (buffer[0] == 'D' && buffer[1] == 'S') {
            chunks.clear();

            AllBlockMap.init();

            lightSources.retainLights(2);

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
                for (int i = 0; i < solidNum && !gameObject.WINDOW.shouldClose(); i++) {
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
                    for (int i = 0; i < fluidNum && !gameObject.WINDOW.shouldClose(); i++) {
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
        blockEnvironment.clear();

        progress = 100.0f;
        working = false;
        gameObject.getMusicPlayer().stop();

        return success;
    }

    /**
     * Load level in binary NDAT (new) format from internal level container
     * buffer. Used in Multiplayer.
     *
     * @return on success
     * @throws java.io.UnsupportedEncodingException
     */
    public boolean loadLevelFromBufferNewFormat() throws UnsupportedEncodingException, Exception {
        working = true;
        boolean success = false;
//        if (progress > 0.0f) {
//            return false;
//        }
        progress = 0.0f;
        levelActors.freeze();
        gameObject.getMusicPlayer().play(AudioFile.INTERMISSION, true, true);
        pos = 0;

        // Check the initial format identifiers
        if (buffer[pos++] == 'D' && buffer[pos++] == 'S' && buffer[pos++] == '2') {
            AllBlockMap.init();
            lightSources.retainLights(2);
            CacheModule.deleteCache();

            byte[] posArr = new byte[12];
            System.arraycopy(buffer, pos, posArr, 0, posArr.length);
            Vector3f campos = VectorFloatUtils.vec3fFromByteArray(posArr);
            pos += posArr.length;

            byte[] frontArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, frontArr.length);
            Vector3f camfront = VectorFloatUtils.vec3fFromByteArray(frontArr);
            pos += frontArr.length;

            byte[] upArr = new byte[12];
            System.arraycopy(buffer, pos, upArr, 0, upArr.length);
            Vector3f camup = VectorFloatUtils.vec3fFromByteArray(upArr);
            pos += upArr.length;

            byte[] rightArr = new byte[12];
            System.arraycopy(buffer, pos, rightArr, 0, rightArr.length);
            Vector3f camright = VectorFloatUtils.vec3fFromByteArray(rightArr);
            pos += rightArr.length;

            levelActors.configureMainObserver(campos, camfront, camup, camright);

            if (buffer[pos++] == 'B' && buffer[pos++] == 'L' && buffer[pos++] == 'K' && buffer[pos++] == 'S') {
                // Read the total number of blocks
                int totalBlocks = (buffer[pos++] & 0xFF) | ((buffer[pos++] & 0xFF) << 8)
                        | ((buffer[pos++] & 0xFF) << 16) | ((buffer[pos++] & 0xFF) << 24);

                if (totalBlocks <= 0) { // 'Empty world'
                    levelActors.unfreeze();
                    blockEnvironment.clear();

                    progress = 100.0f;
                    working = false;
                    gameObject.getMusicPlayer().stop();
                    throw new Exception("No blocks to process!");
                }

                while (true) {
                    char[] texNameChars = new char[5];
                    for (int i = 0; i < texNameChars.length; i++) {
                        texNameChars[i] = (char) buffer[pos++];
                    }
                    String texName = new String(texNameChars);

                    int count = (buffer[pos++] & 0xFF) | ((buffer[pos++] & 0xFF) << 8)
                            | ((buffer[pos++] & 0xFF) << 16) | ((buffer[pos++] & 0xFF) << 24);

                    for (int i = 0; i < count && !gameObject.WINDOW.shouldClose(); i++) {
                        if (gameObject.game.isConnected() && ((i & 4095) == 0)) { // each 4x 1024-th block
                            gameObject.game.sendPingRequest();
                        }

                        byte[] byteArrayBlock = new byte[29];
                        System.arraycopy(texName.getBytes("US-ASCII"), 0, byteArrayBlock, 0, 5);
                        System.arraycopy(buffer, pos, byteArrayBlock, 5, 24);
                        Block block = Block.fromByteArray(byteArrayBlock, !texName.equals("water"));
                        chunks.addBlock(block);
                        pos += 29;
                        progress += 100.0f / (float) totalBlocks;
                    }

                    if (buffer[pos] == 'E' && buffer[pos + 1] == 'N' && buffer[pos + 2] == 'D') {
                        pos += 3;
                        success = true;
                        break;
                    }
                }
            }
        }

        levelActors.unfreeze();
        blockEnvironment.clear();

        progress = 100.0f;
        working = false;
        gameObject.getMusicPlayer().stop();

        return success;
    }

    /**
     * Save level to exported file. Extension is chosen (dat|ndat) in filename
     * arg.
     *
     * @param filename filename to export on drive (dat|ndat).
     * @return on success
     */
    public boolean saveLevelToFile(String filename) {
        if (working) {
            return false;
        }
        boolean success = false;
        ensureCorrectExtension(filename);

        BufferedOutputStream bos = null;
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }

        if (filename.endsWith(".dat")) {
            success |= storeLevelToBufferOldFormat(); // saves level to buffer first
        } else if (filename.endsWith(".ndat")) {
            success |= storeLevelToBufferNewFormat(); // saves level to buffer first
        }

        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(buffer, 0, pos); // save buffer to file at pos mark
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

    /**
     * Load level from imported file. Extension is chosen (dat|ndat) in filename
     * arg.
     *
     * @param filename filename to export on drive (dat|ndat).
     * @return on success
     */
    public boolean loadLevelFromFile(String filename) {
        if (working) {
            return false;
        }
        boolean success = false;
        if (filename.isEmpty()) {
            return false;
        }
        ensureCorrectExtension(filename);

        File file = new File(filename);
        BufferedInputStream bis = null;
        if (!file.exists()) {
            return false; // this prevents further fail
        }
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(buffer);
            if (filename.endsWith(".dat")) {
                success |= loadLevelFromBufferOldFormat();
            } else if (filename.endsWith(".ndat")) {
                success |= loadLevelFromBufferNewFormat();
            }

        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (Exception ex) {
            DSLogger.reportError(ex.getMessage(), ex); // zero blocks
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

    /**
     * Animate fluid blocks
     */
    public void animate() {
        if (!working) {
            synchronized (blockEnvironment) {
                blockEnvironment.animate();
            }
        }
    }

    /**
     * Prepare before above water underwater scene
     */
    public void prepare() {
        if (!working) {
            synchronized (blockEnvironment) {
                blockEnvironment.prepare(levelActors.mainActor().getFront(), actorInFluid);
            }
        }
    }

    /**
     * Is main actor player in fluid check.
     *
     * @return if is in fluid
     */
    public boolean isActorInFluidChk() {
        boolean yea = false;
        Vector3f camPos = levelActors.mainActor().getPos();

        Vector3f obsCamPosAlign = new Vector3f(
                Math.round(camPos.x + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.y + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.z + 0.5f) & 0xFFFFFFFE
        );

        yea = AllBlockMap.isLocationPopulated(obsCamPosAlign, false);

        if (!yea) {
            for (int j = 0; j <= 13; j++) {
                Vector3f adjPos = Block.getAdjacentPos(camPos, j, 2.0f);
                Vector3f adjAlign = new Vector3f(
                        Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
                );

                boolean fluidOnLoc = AllBlockMap.isLocationPopulated(adjAlign, false);

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

    /**
     * Is main actor player in fluid check.
     *
     * @param lc specified level container
     * @return if is in fluid
     */
    public static boolean isActorInFluidChk(LevelContainer lc) {
        boolean yea = false;
        Vector3f camPos = lc.levelActors.mainActor().getPos();

        Vector3f obsCamPosAlign = new Vector3f(
                Math.round(camPos.x + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.y + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.z + 0.5f) & 0xFFFFFFFE
        );

        yea = AllBlockMap.isLocationPopulated(obsCamPosAlign, false);

        if (!yea) {
            for (int j = 0; j <= 13; j++) {
                Vector3f adjPos = Block.getAdjacentPos(camPos, j, 2.0f);
                Vector3f adjAlign = new Vector3f(
                        Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                        Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
                );

                boolean fluidOnLoc = AllBlockMap.isLocationPopulated(adjAlign, false);

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

    /**
     * Update static variable 'actor in fluid' by perform checking
     *
     * @param lc Level Container specified.
     */
    public static void updateActorInFluid(LevelContainer lc) {
        actorInFluid = isActorInFluidChk(lc);
    }

    /**
     * Align vector on even and round it (in some order)
     *
     * @param vector vec3f to align
     * @return new vec3f
     */
    public static Vector3f alignVector(Vector3f vector) {
        return new Vector3f(
                Math.round(vector.x + 0.5f) & 0xFFFFFFFE,
                Math.round(vector.y + 0.5f) & 0xFFFFFFFE,
                Math.round(vector.z + 0.5f) & 0xFFFFFFFE
        );
    }

    /**
     * Checks if a {@link Predictable} has collision with the environment.
     * Called on motion. Not used. Predictable interface.
     *
     * @param predictable the object to check for collision.
     * @param direction direction XYZ of motion
     * @return {@code true} if collision is detected, {@code false} otherwise.
     */
    public static boolean hasCollisionWithEnvironment(Predictable predictable, Game.Direction direction) {
        if (!SKYBOX.containsInsideExactly(predictable.getPredictor())) {
            return false; // No need to continue if outside the skybox.
        }

        Vector3f predictor = predictable.getPredictor();

        // Round the predictor's coordinates to align with the grid.
        Vector3f predAlign = alignVector(predictor);

        // Check collision with solid blocks at the predicted position.
        if (AllBlockMap.isLocationPopulated(predAlign, true)) {
            return true;
        }

        // Iterate through adjacent positions.
        final int[] sides = Block.getOppositeSides(direction);
        OUTER:
        for (int j : sides) {
            for (float amount = MIN_AMOUNT; amount <= MAX_AMOUNT; amount += STEP_AMOUNT) {
                Vector3f adjPos = Block.getAdjacentPos(predictor, j, amount);
                Vector3f adjAlign = alignVector(adjPos);

                // Check collision with solid blocks at the adjacent position.
                if (AllBlockMap.isLocationPopulated(adjAlign, true)) {
                    if (Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, predictor)
                            || Model.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f, predictor, 0.075f, 0.075f, 0.075f)) {
                        return true; // Collision detected, no need to continue checking.
                    }
                }
            }
        }

        return false; // No collision detected.
    }

    /**
     * Checks if an {@link Observer} has collision with the environment. Called
     * on motion. In 'editorDo'. Observer (interface) has camera.
     *
     * @param observer the observer to check for collision.
     * @param direction direction XYZ of motion
     * @return {@code true} if collision is detected, {@code false} otherwise.
     */
    public static boolean hasCollisionWithEnvironment(Observer observer, Game.Direction direction) {
        if (!SKYBOX.containsInsideExactly(observer.getPos())
                || !SKYBOX.intersectsExactly(observer.getPos(), 0.075f, 0.075f, 0.075f)) {
            return true; // Collision detected outside the skybox or with its boundary.
        }

        Vector3f observerPos = observer.getPos();

        // Round the observer's coordinates to align with the grid.
        Vector3f predAlign = alignVector(observerPos);

        // Check collision with solid blocks at the predicted position.
        if (AllBlockMap.isLocationPopulated(predAlign, true)) {
            return true;
        }

        // Iterate through adjacent positions.
        final int[] sides = Block.getOppositeSides(direction);
        OUTER:
        for (int j : sides) {
            for (float amount = MIN_AMOUNT; amount <= MAX_AMOUNT; amount += STEP_AMOUNT) {
                Vector3f adjPos = Block.getAdjacentPos(observerPos, j, amount);
                Vector3f adjAlign = alignVector(adjPos);

                // Check collision with solid blocks at the adjacent position.
                if (AllBlockMap.isLocationPopulated(adjAlign, true)) {
                    if (Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, observerPos)
                            || Block.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f,
                                    observerPos, 0.075f, 0.075f, 0.075f)) {
                        return true; // Collision detected, no need to continue checking.
                    }
                }
            }
        }

        return false; // No collision detected.
    }

    /**
     * Checks if a {@link Critter} has collision with the environment. Called on
     * motion. In 'singleplayerDo'.
     *
     * @param critter the critter to check for collision.
     * @param direction direction XYZ of motion
     * @return {@code true} if collision is detected, {@code false} otherwise.
     */
    public static boolean hasCollisionWithEnvironment(Critter critter, Game.Direction direction) {
        if (!SKYBOX.containsInsideExactly(critter.getPredictor())
                || !SKYBOX.intersectsExactly(critter.getPredictor(), critter.body.getWidth(),
                        critter.body.getHeight(), critter.body.getDepth())) {
            return true; // Collision detected outside the skybox or with its boundary.
        }

        Vector3f predictor = critter.getPredictor();

        // Round the critter's predictor coordinates to align with the grid.
        Vector3f predAlign = alignVector(predictor);

        // Check collision with solid blocks at the predicted position.
        if (AllBlockMap.isLocationPopulated(predAlign, true)) {
            return true;
        }

        // Iterate through adjacent positions.
        final int[] sides = Block.getOppositeSides(direction);
        OUTER:
        for (int j : sides) {
            SCAN:
            for (float amount = MIN_AMOUNT; amount <= MAX_AMOUNT; amount += STEP_AMOUNT) {
                Vector3f adjPos = Block.getAdjacentPos(predictor, j, amount);
                Vector3f adjAlign = alignVector(adjPos);

                // Check collision with solid blocks at the adjacent position.
                if (AllBlockMap.isLocationPopulated(adjAlign, true)) {
                    if (Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, predictor)
                            || Model.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f,
                                    predictor, 1.05f * critter.body.getWidth(),
                                    1.05f * critter.body.getHeight(),
                                    1.05f * critter.body.getDepth())) {
                        return true; // Collision detected, no need to continue checking.
                    }
                }
            }
        }

        return false; // No collision detected.
    }

    /**
     * Checks if a {@link Critter} has collision with the environment.
     * Multiplayer. Called on motion. In 'multiplayerDo'.
     *
     * @param critter the critter to check for collision.
     * @param playerServerPos player position according to server
     * @param direction direction XYZ of motion
     * @param interpFactor interpolation factor
     * @return {@code true} if collision is detected, {@code false} otherwise.
     */
    public static boolean hasCollisionWithEnvironment(Critter critter, Vector3f playerServerPos, Game.Direction direction, float interpFactor) {
        if (!SKYBOX.containsInsideExactly(critter.getPredictor())
                || !SKYBOX.intersectsExactly(critter.getPredictor(), critter.body.getWidth(),
                        critter.body.getHeight(), critter.body.getDepth())) {
            return true; // Collision detected outside the skybox or with its boundary.
        }

        Vector3f predictor = critter.getPredictor();

        // Round the critter's predictor coordinates to align with the grid.
        Vector3f predAlign = alignVector(predictor);

        // Check collision with solid blocks at the predicted position.
        if (AllBlockMap.isLocationPopulated(predAlign, true)) {
            return true;
        }

        // Iterate through adjacent positions.        
        final int[] sides = Block.getOppositeSides(direction);
        OUTER:
        for (int j : sides) {
            SCAN:
            for (float amount = MIN_AMOUNT; amount <= MAX_AMOUNT; amount += STEP_AMOUNT) {
                // Interpolate the critter's position based on the current interpolation time.
                Vector3f interpolatedPos = new Vector3f(critter.getPredictor()).lerp(playerServerPos, interpFactor);

                Vector3f adjPos = Block.getAdjacentPos(interpolatedPos, j, amount);
                Vector3f adjAlign = alignVector(adjPos);

                // Check collision with solid blocks at the adjacent position.
                if (AllBlockMap.isLocationPopulated(adjAlign, true)) {
                    if (Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, interpolatedPos)
                            || Model.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f,
                                    interpolatedPos, 1.05f * critter.body.getWidth(),
                                    1.05f * critter.body.getHeight(),
                                    1.05f * critter.body.getDepth())) {
                        return true; // Collision detected, no need to continue checking.
                    }
                }
            }
        }

        return false; // No collision detected.
    }

    /**
     * Applies gravity to the player, making them fall downwards if not
     * supported below. Called on motion. Called in 'update'.
     *
     * @param critter critter affected by gravity
     * @param deltaTime The time elapsed since the last handleInput.
     * @return {@code true} if the player is affected by gravity, {@code false}
     * otherwise.
     */
    @Override
    public boolean gravityDo(Critter critter, float deltaTime) {
        boolean collision = false;
        // Check for collision in any direciton
        for (Game.Direction dir : Game.Direction.values()) {
            collision |= hasCollisionWithEnvironment(critter, dir);
            if (collision) {
                fallVelocity = 0.0f;
                jumpVelocity = 0.0f;
                return false;
            }
        }
        // Initial predictor position
        final Vector3f predInit = new Vector3f(critter.getPredictor());

        // Iterate over time steps to check for collisions        
        TICKS:
        for (float tstTime = 0.0f; tstTime <= 2.0f * deltaTime; tstTime += (float) Game.TICK_TIME / 4.0f) {
            float tstHeight;
            final int[] sides;
            if (jumpVelocity == 0.0f) {
                tstHeight = fallVelocity * tstTime + (GRAVITY_CONSTANT * tstTime * tstTime) / 2.0f;
                sides = new int[]{Block.BOTTOM, Block.BOTTOM_BACK, Block.BOTTOM_FRONT, Block.LEFT_BOTTOM, Block.RIGHT_BOTTOM, Block.LEFT, Block.RIGHT, Block.BACK, Block.FRONT};
                critter.movePredictorDown(tstHeight);
            } else {
                tstHeight = jumpVelocity * tstTime - (GRAVITY_CONSTANT * tstTime * tstTime) / 2.0f;
                sides = new int[]{Block.TOP, Block.TOP_BACK, Block.TOP_FRONT, Block.LEFT_TOP, Block.RIGHT_TOP, Block.LEFT, Block.RIGHT, Block.BACK, Block.FRONT};
                critter.movePredictorUp(tstHeight);
            }

            // Check collision on all sides
            for (int side : sides) {
                SCAN:
                for (float amount = MIN_AMOUNT; amount <= MAX_AMOUNT; amount += STEP_AMOUNT) {
                    Vector3f adjPos = Block.getAdjacentPos(critter.getPredictor(), side, amount);
                    Vector3f adjPosAlign = alignVector(adjPos);

                    boolean solidOnLoc = AllBlockMap.isLocationPopulated(adjPosAlign, true);
                    if (solidOnLoc) {
                        collision = checkCollisionInternal(adjPosAlign, critter);
                        if (collision) {
                            fallVelocity = 0.0f;
                            jumpVelocity = 0.0f;
                            break TICKS;
                        }
                    }
                }
            }

            if (jumpVelocity == 0.0f) {
                critter.movePredictorUp(tstHeight);
            } else {
                critter.movePredictorDown(tstHeight);
            }
        }
        critter.getPredictor().set(predInit); // Ensure predictor is reset after loop

        // If no collision, apply gravity and move the player
        if (!collision) {
            float deltaHeight;
            if (jumpVelocity == 0.0f) {
                deltaHeight = fallVelocity * deltaTime + (GRAVITY_CONSTANT * deltaTime * deltaTime) / 2.0f;
            } else {
                deltaHeight = jumpVelocity * deltaTime - (GRAVITY_CONSTANT * deltaTime * deltaTime) / 2.0f;
            }
            // Adjust height for actor in fluid (water)
            if (actorInFluid) {
                deltaHeight *= 0.125f;
            }

            if (jumpVelocity == 0.0f) {
                critter.movePredictorYDown(deltaHeight);
                critter.dropY(deltaHeight);
            } else {
                critter.movePredictorYUp(deltaHeight);
                critter.jumpY(deltaHeight);
            }

            if (jumpVelocity == 0.0f) {
                fallVelocity = Math.min(fallVelocity + GRAVITY_CONSTANT * deltaTime, TERMINAL_VELOCITY);
            } else {
                jumpVelocity = Math.max(jumpVelocity - GRAVITY_CONSTANT * deltaTime, 0.0f);
            }

            if (fallVelocity == TERMINAL_VELOCITY) {
                try {
                    spawnPlayer(); // Respawn player if terminal velocity is reached
                } catch (Exception ex) {
                    DSLogger.reportError("Unable to spawn player after the fall!", ex);
                }
            }

            // in case of multiplayer join send to the server
            if (gameObject.game.isConnected() && Game.getCurrentMode() == Game.Mode.MULTIPLAYER_JOIN && gameObject.game.isAsyncReceivedEnabled()) {
                gameObject.game.requestSetPlayerPos();
            }
        }

        return !collision;
    }

//    /**
//     * Calculates the thrust experienced by the player in a fluid.
//     *
//     * @param deltaTime The time elapsed since the last handleInput.
//     * @return The calculated thrust.
//     */
//    private float calculateFluidThrust(float deltaTime) {
//        final float thrustArea = 4.41f;
//        thrustVelocity = GRAVITY_CONSTANT * deltaTime;
//
//        // FORCE = DENS * AREA * VELOCITY * VELOCITY
//        final float thrustForce = WATER_DENSITY * thrustArea * thrustVelocity * thrustVelocity;
//        final float mass = 75f;
//        final float accel = thrustForce / mass;
//
//        // Calculate the thrust height
//        final float resh = accel * deltaTime * deltaTime / 2.0f;
//
//        return resh;
//    }
    /**
     * Makes the player jump upwards.
     *
     * @param critter The player.
     * @param jumpStrength The amount of upward movement.
     * @return {@code true} if the player successfully jumped, {@code false}
     * otherwise.
     */
    @Override
    public boolean jump(Critter critter, float jumpStrength) {
        if (working) {
            return false;
        }

        // Initialize jump velocity if player is starting the jump
        if (fallVelocity == 0.0f || (actorInFluid && jumpVelocity < jumpStrength)) {
            jumpVelocity = jumpStrength;
            return true;
        }

        return false;
    }

    /**
     * Makes the player push downwards, pressuring the bottom surface (or air)
     *
     * @param critter The player.
     * @param crouchStrength The amount of downward movement.
     * @return was crouch performed by player (was able to)
     */
    @Override
    public boolean crouch(Critter critter, float crouchStrength) {
        if (working) {
            return false;
        }

        if (jumpVelocity > 0.0f) {
            jumpVelocity -= crouchStrength;
        }

        return true;
    }

    /**
     * /**
     * Internal check for collision with environment or other objects.
     *
     * @param blkPos The block position to check.
     * @param critter The critter to test against.
     * @return {@code true} if collision is detected, {@code false} otherwise.
     */
    /**
     * /**
     * Internal check for collision with environment or other objects.
     *
     * @param blkPos The block position to check.
     * @param critter The critter to test against.
     * @return {@code true} if collision is detected, {@code false} otherwise.
     */
    private static boolean checkCollisionInternal(Vector3f blkPos, Critter critter) {
        return Block.containsInsideEqually(blkPos, 2.1f, 2.1f, 2.1f, critter.getPredictor())
                || Model.intersectsEqually(blkPos, 2.1f, 2.1f, 2.1f,
                        critter.getPredictor(), 1.05f * critter.body.getWidth(),
                        1.05f * critter.body.getHeight(), 1.05f * critter.body.getDepth())
                || !SKYBOX.containsInsideExactly(critter.getPredictor())
                || !SKYBOX.intersectsExactly(critter.getPredictor(),
                        critter.body.getWidth(), critter.body.getHeight(), critter.body.getDepth());
    }

    /**
     * Method to determine visible chunks.
     *
     * @return
     */
    public boolean determineVisible() {
        boolean changed = Chunk.determineVisible(vChnkIdList, iChnkIdList, levelActors.mainCamera());

        return changed;
    }

    /**
     * Method for saving invisible chunks / loading visible chunks. Operates
     * using the Cache module.
     *
     * @return true if any chunks were loaded or saved; false otherwise
     */
    public boolean chunkOperations() {
        boolean changed = false;

        if (!working) {
            for (int chunkId : vChnkIdList) {
                changed |= cacheModule.loadFromDisk(chunkId);
            }

            if (!changed) { // avoid same time save/load
                for (int chunkId : iChnkIdList) {
                    changed |= cacheModule.saveToDisk(chunkId);
                }
            }
        }

        return changed;
    }

    /**
     * Perform update to the day/night cycle. Sun position & sunlight is
     * updated. Skybox rotates counter-clockwise (from -right to right)
     */
    public void update() { // call it externally from the main thread 
        if (!working) { // don't subBufferVertices if working, it may screw up!   
            final float now = (float) GameTime.Now().getTime();
            float dtime = now - lastCycledDayTime;
            lastCycledDayTime = now;

            final float dangle = org.joml.Math.toRadians(dtime * 360.0f / 24.0f);

            SKYBOX.setrY(SKYBOX.getrY() + dangle);
            SUN.pos.rotateZ(dangle);

            final float sunAngle = org.joml.Math.atan2(SUN.pos.y, SUN.pos.x);
            float inten = org.joml.Math.sin(sunAngle);

            if (inten < 0.0f) { // night
                SKYBOX.setTexName("night");
                SKYBOX.setPrimaryRGBAColor(new Vector4f((new Vector3f(NIGHT_SKYBOX_COLOR_RGB)).mul(0.15f), 0.15f));
            } else if (inten >= 0.0f) { // day
                SKYBOX.setTexName("day");
                SKYBOX.setPrimaryRGBAColor(new Vector4f((new Vector3f(NIGHT_SKYBOX_COLOR_RGB)).mul(Math.max(inten, 0.15f)), 0.15f));
            }

            final float sunInten = Math.max(inten, 0.0f);
            SUN.setPrimaryRGBAColor(new Vector4f((new Vector3f(SUN_COLOR_RGB)).mul(sunInten), 1.0f));
            SUNLIGHT.setIntensity(sunInten * SUN_INTENSITY);
            SUNLIGHT.pos.set(SUN.pos);

            // always handleInput sunlight (sun/pos)
            lightSources.updateLight(1, SUNLIGHT);
            lightSources.setModified(1, true); // SUNLIGHT index is always 1

            // handleInput - player light - only in correct mode
            if (Game.getCurrentMode() == Game.Mode.FREE || Game.getCurrentMode() == Game.Mode.EDITOR) {
                levelActors.player.light.setIntensity(0.0f);
            } else {
                levelActors.player.light.setIntensity(LightSource.PLAYER_LIGHT_INTENSITY);
                lightSources.updateLight(0, levelActors.player.light);
            }
            lightSources.setModified(0, true); // Player index is always 0
            // updateEnvironment light blocks set light modified for visible lights
            lightSources.sourceList.forEach(ls -> {
                int chnkId = Chunk.chunkFunc(ls.pos);
                if (vChnkIdList.contains(chnkId)) {
                    lightSources.setModified(ls.pos, true);
                }
            });
        }
    }

    /**
     * Optimize to update block environment. Block environment is ensuring that
     * there is no created overhead in rendering.
     *
     * Blocks from all the chunks are being taken into consideration.
     *
     */
    public void optimizeBlockEnvironment() {
        if (!working && !vChnkIdList.isEmpty()) {
            Camera mainCamera = levelActors.mainCamera();
            // provide visible chunk id(entifier) list, camera view eye and camera position
            // where all the blocks are pulled into optimized tuples
            synchronized (blockEnvironment) {
                blockEnvironment.optimizeTuples(vChnkIdList, mainCamera);
            }
        }
    }

    /**
     * Regular Rendering on the screen
     *
     * @param renderFlag what is rendered {LIGHT, WATER, SHADOW}
     */
    public void render(int renderFlag) { // renderStaticTBO for regular level rendering
        if (working) {
            return;
        }

        // this portion of the code renders level actors
        // render observer/player
        levelActors.render(lightSources, ShaderProgram.getPlayerShader(), ShaderProgram.getMainShader());
        // render same camera to eniromental (voxel) shaders
        for (ShaderProgram sp : ShaderProgram.ENVIRONMENTAL_SHADERS) {
            levelActors.mainCamera().render(sp);
        }

        if (!SUN.isBuffered()) {
            SUN.bufferAll();
        }

        if (SUNLIGHT.getIntensity() > 0.0f) {
            SUN.render(lightSources, ShaderProgram.getMainShader());
        }

        if (!SKYBOX.isBuffered()) {
            SKYBOX.bufferAll();
        }
        SKYBOX.render(lightSources, ShaderProgram.getSkyboxShader());

        // only visible & uncached are in chunk list
        synchronized (blockEnvironment) {
            blockEnvironment.renderStatic(ShaderProgram.getVoxelShader(), renderFlag);
        }
        // ----------------------------------------------       
        if (Game.getCurrentMode() == Game.Mode.EDITOR) {
            Block editorNew = Editor.getSelectedNew();
            if (editorNew != null) {
                if (!editorNew.isBuffered()) {
                    editorNew.bufferAll();
                }
                editorNew.render(lightSources, ShaderProgram.getMainShader());
            }

            Block selectionDecal = Editor.Decal;
            if (selectionDecal != null && Editor.isDecalActive()) {
                if (!selectionDecal.isBuffered()) {
                    selectionDecal.bufferAll();
                }
                selectionDecal.renderContour(lightSources, ShaderProgram.getContourShader());
            }
        }
        // render light overlay
        LightSources.render(gameObject.intrface, levelActors.mainCamera(), this, ShaderProgram.getLightShader());
        lightSources.resetAllModified();
    }

    /**
     * Render environment by using the specifying shader. And specific camera.
     * And by specifics. This method of rendering is used by Water Renderer and
     * Shadow Renderer.
     *
     * @param camera camera to use
     * @param baseShader base shader program to render
     * @param instanceShader instanced (rendering) shader
     * @param renderFlag configurable render flag
     */
    public void render(Camera camera, ShaderProgram baseShader, ShaderProgram instanceShader, int renderFlag) { // renderStaticTBO for both regular level rendering and framebuffer (water renderer)        
        if (working) {
            return;
        }

        camera.render(baseShader);
        camera.render(instanceShader);

        if (!SUN.isBuffered()) {
            SUN.bufferAll();
        }

        if (SUNLIGHT.getIntensity() > 0.0f) {
            SUN.render(lightSources, baseShader);
        }

        if (!SKYBOX.isBuffered()) {
            SKYBOX.bufferAll();
        }
        SKYBOX.render(lightSources, baseShader);

        // only visible & uncached are in chunk list 
        blockEnvironment.renderStatic(instanceShader, renderFlag);

        if (Game.getCurrentMode() == Game.Mode.EDITOR) {
            Block editorNew = Editor.getSelectedNew();
            if (editorNew != null) {
                if (!editorNew.isBuffered()) {
                    editorNew.bufferAll();
                }
                editorNew.render(lightSources, baseShader);
            }

            Block selectionDecal = Editor.Decal;
            if (selectionDecal != null && Editor.isDecalActive()) {
                if (!selectionDecal.isBuffered()) {
                    selectionDecal.bufferAll();
                }
                selectionDecal.renderContour(lightSources, baseShader);
            }
        }
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
        return gameObject.WINDOW;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public boolean isWorking() {
        return working;
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
        return gameObject.getMusicPlayer();
    }

    public LevelActors getLevelActors() {
        return levelActors;
    }

    public Configuration getCfg() {
        return cfg;
    }

    public float getLastCycledDayTime() {
        return lastCycledDayTime;
    }

    @Override
    public float getFallVelocity() {
        return fallVelocity;
    }

    public BlockEnvironment getBlockEnvironment() {
        return blockEnvironment;
    }

    public LightSources getLightSources() {
        return lightSources;
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public static boolean isActorInFluid() {
        return actorInFluid;
    }

    @Override
    public boolean isGravityOn() {
        return gravityOn;
    }

    public float getJumpVelocity() {
        return jumpVelocity;
    }

    public Weapons getWeapons() {
        return weapons;
    }

}
