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
package rs.alexanderstojanovich.evg.main;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.cache.CacheModule;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.PerspectiveRenderer;
import rs.alexanderstojanovich.evg.core.ShadowRenderer;
import rs.alexanderstojanovich.evg.core.WaterRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.critter.Predictable;
import rs.alexanderstojanovich.evg.intrface.Command;
import rs.alexanderstojanovich.evg.intrface.DynamicText;
import rs.alexanderstojanovich.evg.intrface.Intrface;
import rs.alexanderstojanovich.evg.intrface.Quad;
import rs.alexanderstojanovich.evg.level.BlockEnvironment;
import rs.alexanderstojanovich.evg.level.Editor;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.level.RandomLevelGenerator;
import rs.alexanderstojanovich.evg.resources.Assets;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 * Game Engine composed of Game (Loop), Game Renderer and core components.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public final class GameObject { // is mutual object for {Main, Renderer, Random Level Generator}
    // this class protects levelContainer, waterRenderer & Random Level Generator between the threads
    // game logic is contained in here

    /**
     * All Game Assets (Models, Textures etc.)
     */
    public final Assets GameAssets = new Assets();

    protected boolean initializedWindow = false;
    protected boolean initializedCore = false;

    private final Configuration cfg = Configuration.getInstance();

    public static final boolean IS_DEVELOPMENT = false;
    public static final int VERSION = 56;
    public static final String WINDOW_TITLE = String.format("Demolition Synergy - v%s%s", VERSION, IS_DEVELOPMENT ? " (DEVELOPMENT)" : "");
    // makes default window -> Renderer sets resolution from config

    /**
     * Game GLFW Window
     */
    public final Window WINDOW;

    public final MasterRenderer masterRenderer;
    public final LevelContainer levelContainer;
    public final WaterRenderer waterRenderer;
    public final ShadowRenderer shadowRenderer;
    public final RandomLevelGenerator randomLevelGenerator;
    public final PerspectiveRenderer perspectiveRenderer;

    public Intrface intrface;

    protected AudioPlayer musicPlayer;
    protected AudioPlayer soundFXPlayer;

    public final Game game;
    public final GameServer gameServer;
    public final GameRenderer renderer;

    /**
     * Max number of attempts to download the level
     */
    public static final int MAX_ATTEMPTS = 3;

    /**
     * Async Task Executor (async receive responses)
     */
    public final ExecutorService TaskExecutor = Executors.newSingleThreadExecutor();

    /**
     * Update/Render for Interface Mutex
     */
    protected Quad splashScreen; // on loading
    protected DynamicText initText; // displayed with splash screen
    protected static GameObject instance = null;
    protected boolean chunkOperationPerformed = false;

//    protected Quad splashScreen; // splash screen during initialization
    /**
     * Gets one instance of the GameObject (creates new if not exists).
     *
     * @return single window instance
     * @throws java.lang.Exception if not initializedWindow
     */
    public static GameObject getInstance() throws Exception {
        if (instance == null) {
            throw new Exception("Game Object not initialized!");
        }
        return instance;
    }

    /**
     * Init this game container with core components.
     *
     * @throws java.lang.Exception if water renderer or shadow renderer is
     * improperly configured
     */
    public GameObject() throws Exception {
        final int width = cfg.getWidth();
        final int height = cfg.getHeight();
        // creating the window
        WINDOW = Window.getInstance(width, height, WINDOW_TITLE);

        WINDOW.setFullscreen(cfg.isFullscreen());
//        WINDOW.setVSync(cfg.isVsync()); //=> code disabled due to not in Renderer
        WINDOW.centerTheWindow();
        //----------------------------------------------------------------------        
        this.splashScreen = new Quad(width, height, GameAssets.SPLASH, true, null);
        this.splashScreen.setColor(new Vector4f(1.1f, 1.37f, 0.1f, 1.0f));

        this.initText = new DynamicText(GameAssets.FONT, "Initializing...", GlobalColors.GREEN_RGBA, new Vector2f(-1.0f, -1.0f), null);
        //----------------------------------------------------------------------        
        //----------------------------------------------------------------------
        initializedWindow = true;
        //----------------------------------------------------------------------
        masterRenderer = new MasterRenderer(this);
        perspectiveRenderer = new PerspectiveRenderer(this);
        levelContainer = new LevelContainer(this);
        randomLevelGenerator = new RandomLevelGenerator(this.levelContainer);
        waterRenderer = new WaterRenderer(this);
        shadowRenderer = new ShadowRenderer(this);
        musicPlayer = new AudioPlayer();
        soundFXPlayer = new AudioPlayer();
        //----------------------------------------------------------------------
        musicPlayer.setGain(cfg.getMusicVolume());
        soundFXPlayer.setGain(cfg.getSoundFXVolume());
        //----------------------------------------------------------------------
        initializedCore = true;
        //----------------------------------------------------------------------        
        //----------------------------------------------------------------------
        // init game loop
        game = new Game(this); // init game with given configuration and game object
        gameServer = new GameServer(this); // create new server from game object
        DSLogger.reportDebug("Game initialized.", null);
        this.initText.setContent("Game initialized.");
        // game interacts with the whole game container
        renderer = new GameRenderer(this); // init renderer with given game object
        DSLogger.reportDebug("Game Renderer initialized.", null);
        this.initText.setContent("Game Renderer initialized.");
        // Intrface holding stuff initialized
        intrface = new Intrface(this);
        DSLogger.reportDebug("Game Interface initialized.", null);
        this.initText.setContent("Game Interface initialized.");
        this.initText.alignToNextChar(intrface);
        instance = this;
    }

    /**
     * Start this game container. Starts loop and renderer.
     */
    public void start() {
        if (!initializedWindow || !initializedCore) {
            return;
        }
        //----------------------------------------------------------------------
        // Schedule timer task to clear ups & fps values
        Timer timer0 = new Timer("Timer Utils");
        TimerTask task1 = new TimerTask() {
            @Override
            public void run() {
                intrface.getUpdText().setContent("ups: " + Game.getUps());
                Game.setUps(0);
                intrface.getFpsText().setContent("fps: " + GameRenderer.getFps());
                GameRenderer.setFps(0);
            }
        };
        timer0.scheduleAtFixedRate(task1, 1000L, 1000L);
        //----------------------------------------------------------------------
        renderer.start();
        DSLogger.reportDebug("Renderer started.", null);
        this.initText.setContent("Game Renderer started.");
        DSLogger.reportDebug("Game will start soon.", null);
        this.initText.setContent("Game will start soon.");
        game.go(); // after the loop end

        gameServer.stopServer(); // stop the server
        gameServer.shutDown();
        timer0.cancel();
        game.cleanUp();
        intrface.cleanUp();
        //----------------------------------------------------------------------
        try {
            renderer.join(); // and it's blocked here until it finishes
        } catch (InterruptedException ex) {
            DSLogger.reportError(ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------------------------------
    /**
     * Clear optimization
     */
    public void clear() {
        if (!GameRenderer.couldRender()) {
            levelContainer.blockEnvironment.clear();
        }
    }

    /**
     * Clear chunk lists (visible chunks and invisible chunks)
     */
    public void clearChunkLists() {
        levelContainer.getvChnkIdList().clear();
        levelContainer.getiChnkIdList().clear();
    }

    /**
     * Auto init/save level container chunks.
     *
     * Perform chunking loading (or saving). From HDD/SSD disk.
     *
     * @return any chunk operation performed
     */
    public boolean utilChunkOperations() {
        this.chunkOperationPerformed = false;
        this.chunkOperationPerformed |= this.levelContainer.chunkOperations();
        return chunkOperationPerformed;
    }

    /**
     * Perform optimization (of chunks). Optimization is collecting all tuples
     * with blocklist from all chunks into one tuple selection.
     */
    public void updateNoptimizeChunks() {
        if (!isWorking()) {
            // run block environment updateEnvironment n optimizaiton
            levelContainer.optimizeBlockEnvironment();
        }
    }

    // -------------------------------------------------------------------------
    /**
     * Get Interpolated Color
     *
     * @param value value to interpolate
     * @return interpolated color (green/yellow/red)
     */
    private static Vector4f getInterpolatedColor(float value) {
        // Normalize the ping value between 0 and 1 for interpolation
        float ratio;
        if (value <= 50f) {
            return GlobalColors.GREEN_RGBA; // Pure green
        } else if (value <= 100f) {
            Vector4f srcCol = new Vector4f(GlobalColors.GREEN_RGBA);
            Vector4f dest = new Vector4f(GlobalColors.GREEN_RGBA);
            ratio = (value - 50f) / 50f; // 50-100 → 0 to 1
            return srcCol.lerp(GlobalColors.YELLOW_RGBA, ratio, dest);
        } else if (value <= 200f) {
            Vector4f srcCol = new Vector4f(GlobalColors.YELLOW_RGBA);
            Vector4f dest = new Vector4f();
            ratio = (value - 100f) / 100f; // 100-200 → 0 to 1
            return srcCol.lerp(GlobalColors.RED_RGBA, ratio, dest);
        } else {
            return GlobalColors.RED_RGBA; // Pure red (or darker red if needed)
        }
    }

    // -------------------------------------------------------------------------
    /**
     * Update Game Object stuff, like Environment (call only from main)
     *
     * @param deltaTime deltaTime in ticks
     */
    public void update(float deltaTime) {
        if (!initializedWindow) {
            return;
        }

        if (levelContainer.isWorking()) {
            intrface.getProgText().setEnabled(true);
            final int percent = Math.round(levelContainer.getProgress());
            intrface.getProgText().setContent("Loading progress: " + percent + "%");
            intrface.getProgressBar().setValue(percent, intrface);
            this.intrface.getProgressBar().getQuad().setEnabled(true);
        } else { // working check avoids locking the monitor
            levelContainer.update();
            // if single player gravity is affected or if multiplayer and player is registered
            if (levelContainer.gravityOn && (Game.getCurrentMode() == Game.Mode.SINGLE_PLAYER)
                    || ((Game.getCurrentMode() == Game.Mode.MULTIPLAYER_HOST || Game.getCurrentMode() == Game.Mode.MULTIPLAYER_JOIN) && levelContainer.levelActors.player.isRegistered())) {
                levelContainer.gravityDo(levelContainer.levelActors.player, deltaTime);
            }
            perspectiveRenderer.updatePerspective(); // subBufferVertices perspective for all the shaders (aoart from shadow ones)

            Vector3f pos = levelContainer.levelActors.mainActor().getPos();
            Vector3f view = levelContainer.levelActors.mainActor().getFront();
            int chunkId = Chunk.chunkFunc(pos);
            intrface.getPosText().setContent(String.format("pos: (%.1f,%.1f,%.1f)", pos.x, pos.y, pos.z));
            intrface.getViewText().setContent(String.format("view: (%.2f,%.2f,%.2f)", view.x, view.y, view.z));
            intrface.getChunkText().setContent(String.format("chunkId: %d", chunkId));
            intrface.getGameModeText().setContent(Game.getCurrentMode().name());
            GameTime now = GameTime.Now();
            intrface.getGameTimeText().setContent(String.format("Day %d %02d:%02d:%02d", now.days, now.hours, now.minutes, now.seconds));
            intrface.getChat().getHistory().forEach(item -> {
                item.getColor().w -= deltaTime;
                if (item.getColor().w <= 0.0) {
                    item.setEnabled(false);
                }
            });
            intrface.getChat().getHistory().removeIf(item -> !item.isEnabled());
            if (Game.getCurrentMode() == Game.Mode.MULTIPLAYER_JOIN) {
                intrface.getPingText().setEnabled(true);
                intrface.getPingText().setColor(getInterpolatedColor((float) game.getPing()));
                intrface.getPingText().setContent(String.format("%.1f ms", game.getPing()));
            } else {
                intrface.getPingText().setEnabled(false);
            }
        }

        if (intrface.getSaveDialog().isDone()) {
            intrface.getSaveDialog().setEnabled(false);
        }

        if (intrface.getLoadDialog().isDone()) {
            intrface.getLoadDialog().setEnabled(false);
        }

        if (intrface.getLoadDialog().isDone()) {
            intrface.getLoadDialog().setEnabled(false);
        }

        if (intrface.getRandLvlDialog().isDone()) {
            intrface.getRandLvlDialog().setEnabled(false);
        }

        if (intrface.getSinglePlayerDialog().isDone()) {
            intrface.getSinglePlayerDialog().setEnabled(false);
        }

        if (intrface.getMultiPlayerDialog().isDone()) {
            intrface.getMultiPlayerDialog().setEnabled(false);
        }

        if (intrface.getScreenText().isEnabled()) {
            intrface.getScreenText().setEnabled(false);
        }

        if (!isWorking() || this.getLevelContainer().getProgress() == 100.0f) {
            this.getLevelContainer().setProgress(0.0f);
            this.intrface.getProgText().setEnabled(false);
            this.intrface.getProgressBar().getQuad().setEnabled(false);
        }

        intrface.update();
    }

    public void assertCheckCollision(boolean collision) {
        intrface.setCollText(collision);
        intrface.getCollText().unbuffer();
    }

    /**
     * First optimization - There are blocks but nothing is rendered.
     *
     * @return is first optimization
     */
    public boolean isFirstOptimization() {
        return !this.levelContainer.chunks.tupleList.isEmpty();
    }

    /**
     * Renderer method. Requires context to be set in the proper thread (call
     * only from renderer)
     */
    public void render() {
        if (!initializedWindow || this.WINDOW.shouldClose()) {
            return;
        }

        masterRenderer.render(); // it clears color bit and depth buffer bit
        if (splashScreen != null && splashScreen.isEnabled()) {
            if (!splashScreen.isBuffered()) {
                splashScreen.bufferSmart(intrface);
            }
            splashScreen.render(null, ShaderProgram.getIntrfaceContourShader());
            if (!initText.isBuffered()) {
                initText.bufferSmart(intrface);
            }
            initText.render(null, ShaderProgram.getIntrfaceShader());
        } else {
            int renderFlag = 0;
            renderFlag |= BlockEnvironment.LIGHT_MASK;

            if (waterRenderer.getEffectsQuality() != WaterRenderer.WaterEffectsQuality.NONE) {
                renderFlag |= BlockEnvironment.WATER_MASK;
            }

            if (shadowRenderer.getEffectsQuality() != ShadowRenderer.ShadowEffectsQuality.NONE) {
                renderFlag |= BlockEnvironment.SHADOW_MASK;
            }

            perspectiveRenderer.render(); // it sets projection matrix {perspective, orthogonal} accross shaders
            if ((Game.getCurrentMode() == Game.Mode.SINGLE_PLAYER)
                    || (Game.getCurrentMode() == Game.Mode.MULTIPLAYER_HOST || Game.getCurrentMode() == Game.Mode.MULTIPLAYER_JOIN)) {
                levelContainer.levelActors.player.renderWeaponInHand(levelContainer.lightSources, ShaderProgram.getWeaponShader());
            }
            if (!isWorking()) {
                // Render Original Scene
                levelContainer.prepare();

                // Render Effects
                if ((renderFlag & BlockEnvironment.WATER_MASK) != 0) {
                    waterRenderer.render();
                }

                if ((renderFlag & BlockEnvironment.SHADOW_MASK) != 0) {
                    shadowRenderer.render();
                }

                levelContainer.render(renderFlag);
            }

            intrface.render(ShaderProgram.getIntrfaceShader(), ShaderProgram.getIntrfaceContourShader());
        }

        WINDOW.render();
    }

    /*
    * Release all GL components (by deleting their buffers)
    *  Call from renderer.
     */
    public void release() {
        this.splashScreen.release();
        this.perspectiveRenderer.release();
        this.intrface.release();
        this.waterRenderer.release();
        this.shadowRenderer.release();
        this.levelContainer.blockEnvironment.release();
        DSLogger.reportDebug("Optimized tuples deleted.", null);

//        Quad.globlRelease();
//        DynamicText.globlRelease();
//        DSLogger.reportDebug("Global release of the buffers. Done.", null);
        Texture.releaseAllTextures();
        DSLogger.reportDebug("Textures deleted.", null);
    }

    // -------------------------------------------------------------------------
    /**
     * Calls chunk functions to determine visible chunks
     *
     * @return is changed
     */
    public boolean createOrUpdateChunkLists() {
        boolean changed = false;
        if (!isWorking()) {
            changed |= levelContainer.determineVisible();
        }

        return changed;
    }

    /**
     * Animation for water (and other fluids)
     *
     */
    public void animate() {
        if (!isWorking()) {
            levelContainer.animate();
            // animate2 light overlay
            levelContainer.lightSources.lightOverlay.triangSwap2(intrface);

            // animate3 light overlay
            levelContainer.lightSources.lightOverlay.triangSwap3(intrface);

            // animate2 light overlay
            levelContainer.lightSources.lightOverlay.triangSwap2(intrface);
        }
    }

    // -------------------------------------------------------------------------
    /**
     * Clear Everything. Game will be 'Free'.
     */
    public void clearEverything() {
        Editor.deselect();
        CacheModule.deleteCache();
        LevelContainer.AllBlockMap.init();

        levelContainer.chunks.clear();
        levelContainer.blockEnvironment.clear();

        Arrays.fill(levelContainer.buffer, (byte) 0x00);
        Arrays.fill(levelContainer.bak_buffer, (byte) 0x00);
        levelContainer.pos = 0;
        levelContainer.bak_pos = 0;

        levelContainer.levelActors.player.setPos(new Vector3f());
        levelContainer.levelActors.player.setRegistered(false);
        levelContainer.levelActors.spectator.setPos(new Vector3f());
        levelContainer.levelActors.npcList.clear();
        levelContainer.levelActors.otherPlayers.clear();
        if (!gameServer.isShutDownSignal()) {
            WINDOW.setTitle(GameObject.WINDOW_TITLE);
        }
        Game.setCurrentMode(Game.Mode.FREE);
        // fix menu being opened
        intrface.getGameMenu().setEnabled(false);
        // set this as help text (it is reset)
        intrface.getGuideText().setEnabled(true);
        // set this to false (don't show 'connected')
        intrface.getInfoMsgText().setEnabled(false);
    }

    /**
     * Start new level from editor. Editor by default adds 9 'Doom' blocks at
     * the starting position. Called from concurrent thread.
     */
    public void startNewLevel() {
        levelContainer.startNewLevel();
    }

    /**
     * Load level from external file (which is in root of game dir). Called from
     * concurrent thread.
     *
     * @param fileName file name in filesystem.
     * @return success of operation
     */
    public boolean loadLevelFromFile(String fileName) {
        boolean ok = false;
        this.clearEverything();

        ok |= levelContainer.loadLevelFromFile(fileName);
        intrface.getGuideText().setEnabled(false);

        return ok;
    }

    /**
     * Save level to external file (which is in root of game dir). Called from
     * concurrent thread. Could be shared in opened in another game clients.
     *
     * @param fileName file name in filesystem.
     * @return success of operation
     */
    public boolean saveLevelToFile(String fileName) {
        boolean ok = false;

        ok |= levelContainer.saveLevelToFile(fileName);
        intrface.getGuideText().setEnabled(false);

        return ok;
    }

    /**
     * Start new randomly generated level from editor. New level could be
     * generated and subsequently edited from (game) client. Notice that there
     * is no 'SMALL', 'MEDIUM', 'LARGE', 'HUGE'. It is coded to parameter
     * 'numberOfBlocks'.
     *
     * Called from concurrent thread.
     *
     * @param numberOfBlocks max number of blocks to generate
     * @return success of operation
     */
    public boolean generateRandomLevel(int numberOfBlocks) {
        boolean ok = false;
        this.clearEverything();

        ok |= levelContainer.generateRandomLevel(randomLevelGenerator, numberOfBlocks);
        intrface.getGuideText().setEnabled(false);

        return ok;
    }

    /**
     * Start new randomly generated level in single player. New level will be
     * generated. Notice that there is no 'SMALL', 'MEDIUM', 'LARGE', 'HUGE'. It
     * is coded to parameter 'numberOfBlocks'.
     *
     * Called from concurrent thread.
     *
     * @param numberOfBlocks max number of blocks to generate
     * @return success of operation
     */
    public boolean generateSinglePlayerLevel(int numberOfBlocks) {
        boolean ok = false;
        this.clearEverything();

        try {
            ok |= levelContainer.generateSinglePlayerLevel(randomLevelGenerator, numberOfBlocks);
        } catch (Exception ex) {
            DSLogger.reportError(ex.getMessage(), ex);
        }
        intrface.getGuideText().setEnabled(false);

        return ok;
    }

    /**
     * Host new randomly generated level in multiplayer. All players on join
     * will download the saved level from game server 'world name'.
     *
     * @param numberOfBlocks max number of blocks to generate
     * @return success of operation
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public boolean generateMultiPlayerLevelAsHost(int numberOfBlocks) throws InterruptedException, ExecutionException {
        boolean ok = false;

        this.clearEverything();
        try {
            ok |= levelContainer.generateMultiPlayerLevel(randomLevelGenerator, numberOfBlocks);
        } catch (Exception ex) {
            DSLogger.reportError("Unable to spawn player after the fall!", ex);
        }

        // Save level to file asynchronously
        CompletableFuture.supplyAsync(() -> {
            return levelContainer.saveLevelToFile(gameServer.getWorldName() + ".ndat");
        }, this.TaskExecutor).thenApply((Boolean rez) -> {
            levelContainer.levelActors.player.setRegistered(rez);
            intrface.getGuideText().setEnabled(false);
            return null;
        });

        return ok;
    }

    /**
     * Join new randomly generated level in multiplayer host. All players on
     * join will download the saved level from game server 'world name'.
     *
     * @return success of operation
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     * @throws java.io.UnsupportedEncodingException
     */
    public boolean generateMultiPlayerLevelAsJoin() throws InterruptedException, ExecutionException, UnsupportedEncodingException, Exception {
        boolean success = false;
        this.clearEverything();

        // Register player with Unique ID (UUID)
        if (game.registerPlayer()) {
            // Get world info from server
            game.getWorldInfo();
            // Configure-set main actor
            // Spawn player (set position)
            levelContainer.spawnPlayer();
            // Player set position
            game.requestSetPlayerPos();
            // Disable guide text (.. player is gonna spawn)
            intrface.getGuideText().setEnabled(false);
            // !IMPORTANT -- ENABLE ASYNC WRITEPOINT 
            game.setAsyncReceivedForceEnabled(true);
            // It is successful
            success = true;
        } else {
            DSLogger.reportWarning("Not able to register player. Disconnecting.", null);
            intrface.getConsole().write("Not able to register player. Disconnecting.", Command.Status.FAILED);
            game.disconnectFromServer();
            this.clearEverything();
        }

        return success;
    }

    /**
     * Check if level container is generating/loading/saving level.
     *
     * @return
     */
    public boolean isWorking() {
        return levelContainer.isWorking();
    }
    // -------------------------------------------------------------------------

    /*    
    * Load the window context and destroyes the window.
     */
    public void destroy() {
        TaskExecutor.shutdown();
        WINDOW.destroy();
    }

    /**
     * Test collision with Environment. Must not leave the SKYBOX and not
     * collide with solid objects or critters.
     *
     * @param preditable Predictable to have collision with environment
     * @param direction direciton of motion
     * @return test true/false
     */
    public boolean hasCollisionWith(Predictable preditable, Game.Direction direction) { // collision detection - critter against solid obstacles
        return LevelContainer.hasCollisionWithEnvironment(preditable, direction);
    }

    /**
     * Test collision with Environment. Must not leave the SKYBOX and not
     * collide with solid objects or critters. (virtually) => 0.07f dimension
     *
     * @param observer Predictable to have collision with environment
     * @param direction direction of motion
     * @return test true/false
     */
    public boolean hasCollisionWith(Observer observer, Game.Direction direction) { // collision detection - critter against solid obstacles
        return LevelContainer.hasCollisionWithEnvironment(observer, direction);
    }

    /**
     * Test collision with Environment. Must not leave the SKYBOX and not
     * collide with solid objects or critters.
     *
     * @param critter critter (implements predictable). Has (model) body.
     * @param direction direction of motion
     * @return test true/false
     */
    public boolean hasCollisionWith(Critter critter, Game.Direction direction) { // collision detection - critter against solid obstacles
        return LevelContainer.hasCollisionWithEnvironment(critter, direction);
    }

    // prints general and detailed information about solid and fluid chunks
    public void printInfo() {
//        levelContainer.chunks.printInfo();
    }

    public LevelContainer getLevelContainer() {
        return levelContainer;
    }

    public AudioPlayer getMusicPlayer() {
        return musicPlayer;
    }

    public AudioPlayer getSoundFXPlayer() {
        return soundFXPlayer;
    }

    public WaterRenderer getWaterRenderer() {
        return waterRenderer;
    }

    public Intrface getIntrface() {
        return intrface;
    }

    public RandomLevelGenerator getRandomLevelGenerator() {
        return randomLevelGenerator;
    }

    public boolean isInitializedWindow() {
        return initializedWindow;
    }

    public Configuration getCfg() {
        return cfg;
    }

    public Game getGame() {
        return game;
    }

    public GameRenderer getRenderer() {
        return renderer;
    }

    public boolean isInitializedCore() {
        return initializedCore;
    }

    public MasterRenderer getMasterRenderer() {
        return masterRenderer;
    }

    public ShadowRenderer getShadowRenderer() {
        return shadowRenderer;
    }

    public PerspectiveRenderer getPerspectiveRenderer() {
        return perspectiveRenderer;
    }

    public Quad getSplashScreen() {
        return splashScreen;
    }

    public GameServer getGameServer() {
        return gameServer;
    }

    public boolean isChunkOperationPerformed() {
        return chunkOperationPerformed;
    }

}
