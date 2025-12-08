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

import java.util.Arrays;
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
import rs.alexanderstojanovich.evg.weapons.WeaponIfc;
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
    public static final float MIN_AMOUNT = -16.8f;
    /**
     * Max amount of iteration for collision control or gravity control (inner
     * loop)
     */
    public static final float MAX_AMOUNT = 16.8f;
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
    protected final int iterationMax = cfg.getOptimizationPasses();

    /**
     * Last Iteration (starting from 0). Need to know where to resume from last
     * point
     */
    protected int lastIteration = 0; // starting from one, cuz zero is not rendered

    /**
     * World Skybox - whole world (except Sun) is contained inside
     */
    public static final Block SKYBOX = new Block("night");

    /**
     * Main source of light. Outside of skybox.
     */
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

    public static final float BASE = 22.5f;
    public static final float SKYBOX_SCALE = BASE * BASE * BASE;
    public static final float SKYBOX_WIDTH = 2.0f * SKYBOX_SCALE;

    public static final Vector3f NIGHT_SKYBOX_COLOR_RGB = new Vector3f(0.25f, 0.5f, 0.75f); // cool bluish color for SKYBOX
    public static final Vector4f NIGHT_SKYBOX_COLOR = new Vector4f(0.25f, 0.5f, 0.75f, 0.15f); // cool bluish color for SKYBOX

    public static final int MAX_NUM_OF_BLOCKS = 131070;

    protected float progress = 0.0f;

    protected boolean working = false;

    public final LevelActors levelActors;

    /**
     * Position of all the solid blocks to texture name & neighbors
     */
    public static final BlockLocation AllBlockMap = new BlockLocation();

    /**
     * Various items like e.g. weapons on the ground
     */
    public final ItemSystem items = new ItemSystem();

    /**
     * Module responsible for caching chunks in SSD/HDD file(s).
     */
    public final CacheModule cacheModule;

    /**
     * Level Buffer to load or save (world) levels.
     */
    public final LevelBuffer levelBuffer;

    protected static boolean actorInFluid = false;

    protected float lastCycledDayTime = 0.0f;

    protected float fallVelocity = 0.0f;
    protected float jumpVelocity = 0.0f;

    /**
     * Weapon in hands
     */
    public final Weapons weapons;

    public boolean gravityOn = false;

    /**
     * Update on put neighbours location on tex byte properties
     *
     * @param vector vec3f where location is
     *
     * @return bits property
     */
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

    /**
     * Update on remove neighbours location on tex byte properties
     *
     * @param vector vec3f where location is
     *
     * @return bits property
     */
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
        this.levelBuffer = new LevelBuffer(this);

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
        items.clear();
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

                Model weaponItem = weapons.DESERT_EAGLE.asItem(new Vector3f(blk.pos.x, blk.pos.y + 2f, blk.pos.z), blk.getPrimaryRGBAColor());
                items.allWeaponItems.add(weaponItem);

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
        items.clear();
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
     * @throws Exception if player spawn fails
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
        items.clear();
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
     * @throws Exception if player spawn fails
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
        items.clear();
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
     * @throws Exception if spawn player fails
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
        while (!playerSpawned && !solidPopLoc.isEmpty() && !gameObject.gameWindow.shouldClose()) { // search through solid population location
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
            player.jumpY(Game.JUMP_STR_AMOUNT / 16f); // Some bad workaround
            player.switchViewToggle();
            player.switchViewToggle();
            gravityOn = true; // Re-enable gravity
        } else {
            // Handle case where no valid spawn location was found (optional)
            throw new Exception("Failed to spawn player in a valid location.");
        }
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
        Vector3f camPos = levelActors.mainActor().getPos();

        Vector3f obsCamPosAlign = new Vector3f(
                Math.round(camPos.x + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.y + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.z + 0.5f) & 0xFFFFFFFE
        );

        if (AllBlockMap.isLocationPopulated(obsCamPosAlign, false)) {
            return true;
        }

        // Check for all 
        for (int j = 0; j <= 13; j++) {
            Vector3f adjPos = Block.getAdjacentPos(camPos, j, 2.0f);
            Vector3f adjAlign = new Vector3f(
                    Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                    Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                    Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
            );

            boolean fluidOnLoc = AllBlockMap.isLocationPopulated(adjAlign, false);

            if (fluidOnLoc && Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, camPos)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is main actor player in fluid check.
     *
     * @param lc specified level container
     * @return if is in fluid
     */
    public static boolean isActorInFluidChk(LevelContainer lc) {
        Vector3f camPos = lc.levelActors.mainActor().getPos();

        Vector3f obsCamPosAlign = new Vector3f(
                Math.round(camPos.x + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.y + 0.5f) & 0xFFFFFFFE,
                Math.round(camPos.z + 0.5f) & 0xFFFFFFFE
        );

        if (AllBlockMap.isLocationPopulated(obsCamPosAlign, false)) {
            return true;
        }

        for (int j = 0; j <= 13; j++) {
            Vector3f adjPos = Block.getAdjacentPos(camPos, j, 2.0f);
            Vector3f adjAlign = new Vector3f(
                    Math.round(adjPos.x + 0.5f) & 0xFFFFFFFE,
                    Math.round(adjPos.y + 0.5f) & 0xFFFFFFFE,
                    Math.round(adjPos.z + 0.5f) & 0xFFFFFFFE
            );

            boolean fluidOnLoc = AllBlockMap.isLocationPopulated(adjAlign, false);

            if (fluidOnLoc && Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, camPos)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is main actor player in fluid check.
     *
     * @param lc specified level container
     * @param critter critter (to check for)
     * @return if is in fluid
     */
    public static boolean isActorInFluidChk(LevelContainer lc, Critter critter) {
        Vector3f camPos = lc.levelActors.mainActor().getPos();

        Vector3f obsCamPosAlign = alignVector(camPos);
        if (AllBlockMap.isLocationPopulated(obsCamPosAlign, false)) {
            return true;
        }

        for (int j = 0; j <= 13; j++) {
            Vector3f adjPos = Block.getAdjacentPos(camPos, j, 2.0f);
            Vector3f adjAlign = alignVector(adjPos);

            boolean fluidOnLoc = AllBlockMap.isLocationPopulated(adjAlign, false);

            if (fluidOnLoc && Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, camPos)) {
                return true;
            }
        }

        return false;
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
            return true;
        }

        Vector3f predictor = predictable.getPredictor();
        Vector3f predAlign = alignVector(predictor);

        if (AllBlockMap.isLocationPopulated(predAlign, true)) {
            return true;
        }

        // FIX: Only check the specific direction, not all opposite sides
        final int[] sides = Block.getSides(direction);
        for (int side : sides) {
            for (float amount = MIN_AMOUNT; amount <= MAX_AMOUNT; amount += STEP_AMOUNT) {
                Vector3f adjPos = Block.getAdjacentPos(predictor, side, amount);
                Vector3f adjAlign = alignVector(adjPos);

                if (AllBlockMap.isLocationPopulated(adjAlign, true)) {
                    if (Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, predictor)
                            || Model.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f, predictor, 0.075f, 0.075f, 0.075f)) {
                        return true;
                    }
                }
            }
        }

        return false;
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
            return true;
        }

        Vector3f observerPos = observer.getPos();
        Vector3f predAlign = alignVector(observerPos);

        if (AllBlockMap.isLocationPopulated(predAlign, true)) {
            return true;
        }

        // FIX: Only check the specific direction
        // Iterate through adjacent positions.
        final int[] sides = Block.getOppositeSidesXYZ(observer.getFront(), observer.getUp(), observer.getRight(), direction);
        for (int side : sides) {
            for (float amount = MIN_AMOUNT; amount <= MAX_AMOUNT; amount += STEP_AMOUNT) {
                Vector3f adjPos = Block.getAdjacentPos(observerPos, side, amount);
                Vector3f adjAlign = alignVector(adjPos);

                if (AllBlockMap.isLocationPopulated(adjAlign, true)) {
                    if (Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, observerPos)
                            || Block.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f,
                            observerPos, 0.075f, 0.075f, 0.075f)) {
                        return true;
                    }
                }
            }
        }

        return false;
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
            return true;
        }

        Vector3f predictor = critter.getPredictor();
        Vector3f predAlign = alignVector(predictor);

        if (AllBlockMap.isLocationPopulated(predAlign, true)) {
            return true;
        }

        // FIX: Only check the specific direction
        // Iterate through adjacent positions.
        final int[] sides = Block.getOppositeSidesXYZ(critter.getFront(), critter.getUp(), critter.getRight(), direction);
        for (int side : sides) {
            for (float amount = MIN_AMOUNT; amount <= MAX_AMOUNT; amount += STEP_AMOUNT) {
                Vector3f adjPos = Block.getAdjacentPos(predictor, side, amount);
                Vector3f adjAlign = alignVector(adjPos);

                if (AllBlockMap.isLocationPopulated(adjAlign, true)) {
                    if (Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, predictor)
                            || Model.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f,
                            predictor, 1.05f * critter.body.getWidth(),
                            1.05f * critter.body.getHeight(),
                            1.05f * critter.body.getDepth())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if a {@link Critter} has collision with the environment. Called on
     * motion. In 'singleplayerDo'. XZ only.
     *
     * @param critter the critter to check for collision.
     * @param direction direction XYZ of motion
     * @return {@code true} if collision is detected, {@code false} otherwise.
     */
    public static boolean hasCollisionXZWithEnvironment(Critter critter, Game.Direction direction) {
        if (!SKYBOX.containsInsideExactly(critter.getPredictor())
                || !SKYBOX.intersectsExactly(critter.getPredictor(), critter.body.getWidth(),
                critter.body.getHeight(), critter.body.getDepth())) {
            return true;
        }

        Vector3f predictor = critter.getPredictor();
        Vector3f predAlign = alignVector(predictor);

        if (AllBlockMap.isLocationPopulated(predAlign, true)) {
            return true;
        }

        // FIX: Only check the specific direction
        // Iterate through adjacent positions.
        final int[] sides = Block.getOppositeSidesXZ(critter.getFront(), critter.getRight(), direction);
        for (int side : sides) {
            for (float amount = MIN_AMOUNT; amount <= MAX_AMOUNT; amount += STEP_AMOUNT) {
                Vector3f adjPos = Block.getAdjacentPos(predictor, side, amount);
                Vector3f adjAlign = alignVector(adjPos);

                if (AllBlockMap.isLocationPopulated(adjAlign, true)) {
                    if (Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, predictor)
                            || Model.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f,
                            predictor, 1.05f * critter.body.getWidth(),
                            1.05f * critter.body.getHeight(),
                            1.05f * critter.body.getDepth())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if a {@link Critter} has collision with the environment. Called on
     * motion. In 'singleplayerDo'. Y only.
     *
     * @param critter the critter to check for collision.
     * @param direction direction XYZ of motion
     * @return {@code true} if collision is detected, {@code false} otherwise.
     */
    public static boolean hasCollisionYWithEnvironment(Critter critter, Game.Direction direction) {
        if (!SKYBOX.containsInsideExactly(critter.getPredictor())
                || !SKYBOX.intersectsExactly(critter.getPredictor(), critter.body.getWidth(),
                critter.body.getHeight(), critter.body.getDepth())) {
            return true;
        }

        Vector3f predictor = critter.getPredictor();
        Vector3f predAlign = alignVector(predictor);

        if (AllBlockMap.isLocationPopulated(predAlign, true)) {
            return true;
        }

        // FIX: Only check the specific direction
        // Iterate through adjacent positions.
        final int[] sides = Block.getOppositeSidesY(critter.getUp(), direction);
        for (int side : sides) {
            for (float amount = MIN_AMOUNT; amount <= MAX_AMOUNT; amount += STEP_AMOUNT) {
                Vector3f adjPos = Block.getAdjacentPos(predictor, side, amount);
                Vector3f adjAlign = alignVector(adjPos);

                if (AllBlockMap.isLocationPopulated(adjAlign, true)) {
                    if (Block.containsInsideEqually(adjAlign, 2.1f, 2.1f, 2.1f, predictor)
                            || Model.intersectsEqually(adjAlign, 2.1f, 2.1f, 2.1f,
                            predictor, 1.05f * critter.body.getWidth(),
                            1.05f * critter.body.getHeight(),
                            1.05f * critter.body.getDepth())) {
                        return true;
                    }
                }
            }
        }

        return false;
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
        final int[] sides = Block.getOppositeSidesXYZ(critter.getFront(), critter.getUp(), critter.getRight(), direction);
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
     * Checks if a {@link Critter} has collision with the environment.
     * Multiplayer. Called on motion. In 'multiplayerDo'. XZ only.
     *
     * @param critter the critter to check for collision.
     * @param playerServerPos player position according to server
     * @param direction direction XYZ of motion
     * @param interpFactor interpolation factor
     * @return {@code true} if collision is detected, {@code false} otherwise.
     */
    public static boolean hasCollisionXZWithEnvironment(Critter critter, Vector3f playerServerPos, Game.Direction direction, float interpFactor) {
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
        final int[] sides = Block.getOppositeSidesXZ(critter.getFront(), critter.getRight(), direction);
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
     * Checks if a {@link Critter} has collision with the environment.
     * Multiplayer. Called on motion. In 'multiplayerDo'. Y only.
     *
     * @param critter the critter to check for collision.
     * @param playerServerPos player position according to server
     * @param direction direction XYZ of motion
     * @param interpFactor interpolation factor
     * @return {@code true} if collision is detected, {@code false} otherwise.
     */
    public static boolean hasCollisionYWithEnvironment(Critter critter, Vector3f playerServerPos, Game.Direction direction, float interpFactor) {
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
        final int[] sides = Block.getOppositeSidesY(critter.getUp(), direction);
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
     * @param critter critter affected by gravity (submitted)
     * @param deltaTime The time elapsed since the last handleInput.
     * @return {@code GravityEnvironment.Result} if the player is affected by gravity
     * otherwise.
     */
    @Override
    public Result gravityDo(Critter critter, float deltaTime) {
        boolean collision = false;

        // Cache multiplayer mode
        final boolean isMultiplayer = gameObject.game.isConnected()
                && Game.getCurrentMode() == Game.Mode.MULTIPLAYER_JOIN
                && gameObject.game.isAsyncReceivedEnabled();
        // Hold gravity result as outcome of this method
        // Initialize as NEUTRAL
        Result result = Result.NEUTRAL;
        // Check for collision in any direction
        for (Game.Direction dir : Game.Direction.values()) {
            // Check for collision with environment in multiplayer or singleplayer mode
            if (isMultiplayer) {
                collision |= hasCollisionWithEnvironment(critter, gameObject.game.getPlayerServerPos(), dir, (float) gameObject.game.getInterpolationFactor());
            } else {
                collision |= hasCollisionWithEnvironment(critter, dir);
            }
            if (collision) {
                critter.setGravityResult(result);
//                DSLogger.reportInfo(result.toString(), null);
                // Not affected by gravity if collision detected
                return result; // Early exit if collision detected
            }
        }

        // Store initial predictor position
        final Vector3f predInit = new Vector3f(critter.getPredictor());

        // FIX: Switch to falling immediately when jump velocity depletes
        if (jumpVelocity <= 0.0f) {
            jumpVelocity = 0.0f;
            // Ensure smooth transition to falling
            if (fallVelocity == 0.0f && !actorInFluid) {
                fallVelocity = GRAVITY_CONSTANT * deltaTime * 0.5f; // Gentle start
            }
        }

        // Initialize test height
        final boolean goingDown = jumpVelocity == 0.0f;
        float tstHeight;
        // Initialize test velocities
        float tstFallVelocity = fallVelocity;
        float tstJumpVelocity = jumpVelocity;
        // Simulate movement in small increments to check for collisions
        final float tstStepTime = (float) Game.TICK_TIME / 3f;
        // Try to reach max as close as possible to deltaTime within and without collision
        TICKS:
        for (float tstTime = 0f; tstTime <= deltaTime; tstTime += tstStepTime) {
            // Calculate the test height and test velocities based on whether the critter is going up or down
            if (goingDown) {
                tstHeight = tstFallVelocity * tstTime + (GRAVITY_CONSTANT * tstTime * tstTime) / 2.0f;
                tstFallVelocity = Math.min(tstFallVelocity + GRAVITY_CONSTANT * tstTime, TERMINAL_VELOCITY);
                critter.movePredictorDown(tstHeight);
            } else {
                tstHeight = tstJumpVelocity * tstTime - (GRAVITY_CONSTANT * tstTime * tstTime) / 2.0f;
                tstJumpVelocity = Math.max(tstJumpVelocity - GRAVITY_CONSTANT * tstTime, 0.0f);
                critter.movePredictorUp(tstHeight);
            }

            // Check for collision in the intended opposite direction
            Game.Direction direction = goingDown ? Game.Direction.DOWN : Game.Direction.UP;
            // Check for collision with environment in multiplayer or singleplayer mode
            if (isMultiplayer) {
                collision |= hasCollisionYWithEnvironment(critter, gameObject.game.getPlayerServerPos(), direction, (float) gameObject.game.getInterpolationFactor());
            } else {
                collision |= hasCollisionYWithEnvironment(critter, direction);
            }

            // Check if collision detected
            if (collision) {
                // Adjust velocities based on collision, resetting the appropriate one
                if (goingDown) {
                    fallVelocity = 0.0f;
                } else {
                    jumpVelocity = 0.0f;
                    // FIX: Start falling immediately after collision
                    fallVelocity = GRAVITY_CONSTANT * tstTime * 0.25f;
                }
                result = Result.COLLISION;
                // Collision detected, exit the loop
                break;
            }
            // Continue to next increment
        }

        // Loop ended, restore initial predictor position
        critter.getPredictor().set(predInit);

        // No collision detected, apply movement (for the full deltaTime)
        if (!collision) {
            float deltaHeight; // height which will be applied
            // Calculate the test height and test velocities based on whether the critter is going up or down
            if (goingDown) {
                deltaHeight = fallVelocity * deltaTime + (GRAVITY_CONSTANT * deltaTime * deltaTime) / 2.0f;
                fallVelocity = Math.min(fallVelocity + GRAVITY_CONSTANT * deltaTime, TERMINAL_VELOCITY);
            } else {
                deltaHeight =  jumpVelocity * deltaTime - (GRAVITY_CONSTANT * deltaTime * deltaTime) / 2.0f;
                jumpVelocity = Math.max(jumpVelocity - GRAVITY_CONSTANT * deltaTime, 0.0f);
            }

            // Adjust for fluid environment
            if (actorInFluid) {
                // FIX: Reduce effective height change in fluid
                deltaHeight *= 0.125f;
                // FIX: Reduce jump strength in fluid
                jumpVelocity *= 0.95f;
            }

            // Apply gravity effects
            // Apply the calculated movement
            if (goingDown) {
                critter.movePredictorYDown(deltaHeight);
                critter.dropY(deltaHeight);
                result = Result.FALL;
            } else {
                critter.movePredictorYUp(deltaHeight);
                critter.jumpY(deltaHeight);
                result = Result.JUMP;
            }
        }

        // Respawn player if terminal velocity is reached
        if (fallVelocity == TERMINAL_VELOCITY) {
            try {
                fallVelocity = 0.0f;
                jumpVelocity = 0.0f;
                spawnPlayer(); // Respawn player if terminal velocity is reached
            } catch (Exception ex) {
                DSLogger.reportError("Unable to spawn player after the fall!", ex);
            }
        }

        // in case of multiplayer join send to the server
        if (gameObject.game.isConnected() && Game.getCurrentMode() == Game.Mode.MULTIPLAYER_JOIN && gameObject.game.isAsyncReceivedEnabled()) {
            gameObject.game.requestSetPlayerPos();
        }
        critter.setGravityResult(result);

//        DSLogger.reportInfo(result.toString(), null);

        return result;
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
        if (fallVelocity == 0.0f || (actorInFluid && jumpVelocity <= jumpStrength)) {
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
            for (int i = lastIteration; i < iterationMax; i++) {
                int chunkId = vChnkIdList.get(i % vChnkIdList.size());
                changed |= cacheModule.loadFromDisk(chunkId);
            }

            if (!changed) { // avoid same time save/load
                for (int i = lastIteration; i < iterationMax; i++) {
                    int chunkId = iChnkIdList.get(i % iChnkIdList.size());
                    changed |= cacheModule.saveToDisk(chunkId);
                }
            }

            lastIteration = (lastIteration + iterationMax) & (Chunk.CHUNK_NUM - 1);
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

            // min distance to pick up items
            final float minDist = 0.5f;
            // check for picked up items
            for (Model item : items.selectedWeaponItems) {
                // check if player is close enough to pick the item
                if (levelActors.player.getPos().distance(item.pos) <= minDist) {
                    // if weapon found, add to inventory
                    WeaponIfc weapon = Arrays.stream(weapons.AllWeapons).filter(w -> w.getTexName().equals(item.texName)).findFirst().orElse(null);
                    if (weapon != null) {
                        // add to inventory
                        switch (weapon.getClazz()) {
                            case OneHandedSmallGun:
                                if (levelActors.player.getSecondaryWeapon() == Weapons.NONE) {
                                    levelActors.player.setSecondaryWeapon(weapon);
                                }
                                break;
                            case TwoHandedSmallGun:
                            case TwoHandedBigGuns:
                                if (levelActors.player.getPrimaryWeapon() == Weapons.NONE) {
                                    levelActors.player.setPrimaryWeapon(weapon);
                                }
                                break;
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Optimize to update block environment & items in environment.
     * Block environment is ensuring that there is no created overhead in rendering.
     * Blocks from all the chunks are being taken into consideration.
     * All items are being preprocessed and selected prior render.
     */
    public void optimizeEnvironment() {
        if (!working && !vChnkIdList.isEmpty()) {
            Camera mainCamera = levelActors.mainCamera();
            // provide visible chunk identifier list, camera view eye and camera position
            // where all the blocks are pulled into optimized tuples
            synchronized (blockEnvironment) {
                blockEnvironment.optimizeTuples(vChnkIdList, mainCamera);
            }

            // preprocess items which are rendered
            items.preprocessItems(vChnkIdList, mainCamera);
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
        // render same camera to environmental (voxel) shaders
        for (ShaderProgram sp : ShaderProgram.ENVIRONMENTAL_SHADERS) {
            levelActors.mainCamera().render(sp);
        }

        if (!SUN.isBuffered()) {
            SUN.bufferAll();
        }

        if (SUNLIGHT.getIntensity() > 0.0f && levelActors.mainCamera().doesSeeEff(SUN, 15f)) {
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
            if (Editor.isDecalActive()) {
                if (!selectionDecal.isBuffered()) {
                    selectionDecal.bufferAll();
                }
                selectionDecal.renderContour(lightSources, ShaderProgram.getContourShader());
            }
        }

        // render weapons (animated)
        items.render(lightSources, ShaderProgram.getMainShader());

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

        if (SUNLIGHT.getIntensity() > 0.0f && levelActors.mainCamera().doesSeeEff(SUN, 15f)) {
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
            if (Editor.isDecalActive()) {
                if (!selectionDecal.isBuffered()) {
                    selectionDecal.bufferAll();
                }
                selectionDecal.renderContour(lightSources, baseShader);
            }
        }

        // render weapons
        items.render(lightSources, ShaderProgram.getMainShader());
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
        return gameObject.gameWindow;
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
