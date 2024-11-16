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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.zip.CRC32C;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.magicwerk.brownies.collections.GapList;
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
import rs.alexanderstojanovich.evg.net.LevelMapInfo;
import rs.alexanderstojanovich.evg.net.PlayerInfo;
import rs.alexanderstojanovich.evg.resources.Assets;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 * Game Engine composed of Game (Loop), Game Renderer and core components.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
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

    public static final boolean IS_DEVELOPMENT = true;
    public static final int VERSION = 52;
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
     * Update/Generate for Level Container Mutex. Responsible for read/write to
     * chunks.
     */
    public static final Lock updateRenderLCLock = new ReentrantLock();

    /**
     * Update/Render for Interface Mutex
     */
    public static final Object UPDATE_RENDER_IFC_MUTEX = new Object();

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
        this.splashScreen = new Quad(width, height, GameAssets.SPLASH, true);
        this.splashScreen.setColor(new Vector4f(1.1f, 1.37f, 0.1f, 1.0f));

        this.initText = new DynamicText(GameAssets.FONT, "Initializing...", GlobalColors.GREEN_RGBA, new Vector2f(-1.0f, -1.0f));
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
     * Auto init/save level container chunks.
     *
     * Perform chunking loading (or saving). From HDD/SSD disk.
     *
     * @return any chunk operation performed
     */
    public boolean utilChunkOperations() {
        this.chunkOperationPerformed = this.levelContainer.chunkOperations();

        return chunkOperationPerformed;
    }

    /**
     * Perform optimization (of chunks). Optimization is collecting all tuples
     * with blocklist from all chunks into one tuple selection.
     */
    public void utilOptimization() {
        if (!isWorking()) {
            updateRenderLCLock.lock();
            try {
                levelContainer.optimize();
            } finally {
                updateRenderLCLock.unlock();
            }
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
                boolean underGravity = levelContainer.gravityDo(deltaTime);
                levelContainer.levelActors.player.setUnderGravity(underGravity);
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

        synchronized (UPDATE_RENDER_IFC_MUTEX) {
            intrface.update();
        }
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
        return !this.levelContainer.chunks.getChunkList().isEmpty();
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
                this.prepare();
                // Render Effects
                if ((renderFlag & BlockEnvironment.WATER_MASK) != 0) {
                    waterRenderer.render();
                }

                if ((renderFlag & BlockEnvironment.SHADOW_MASK) != 0) {
                    shadowRenderer.render();
                }

                // Render Original Scene
                updateRenderLCLock.lock();
                try {
                    levelContainer.render(renderFlag);
                } finally {
                    updateRenderLCLock.unlock();
                }
            }

            synchronized (UPDATE_RENDER_IFC_MUTEX) {
                intrface.render(ShaderProgram.getIntrfaceShader(), ShaderProgram.getIntrfaceContourShader());
            }
        }

        WINDOW.render();
    }

    /**
     * Pull optimized tuples to working tuples in Block Environment.
     */
    public void pull() {
        if (isWorking()) {
            return;
        }
        levelContainer.blockEnvironment.pull();
    }

    /**
     * Push working to fullyOptimized tuples tuples in Block Environment.
     */
    public void push() {
        if (isWorking()) {
            return;
        }
        levelContainer.blockEnvironment.push();
    }

    /**
     * Swap working tuples & optimizing tuples in Block Environment. Zero cost
     * operation. Doesn't require synchronized block.
     */
    public void swap() {
        if (isWorking() || levelContainer.blockEnvironment.isOptimizing()) {
            return;
        }
        levelContainer.blockEnvironment.swap();
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
    public boolean determineVisibleChunks() {
        return levelContainer.determineVisible();
    }

    /**
     * Prepare for water (and other fluids). Sub-buffer vertices based on
     * whether or not camera is submerged in fluid. Cause high CPU consumption.
     *
     */
    public void prepare() {
        if (!isWorking() && GameRenderer.isFirstFrame()) {
            updateRenderLCLock.lock();
            try {
                levelContainer.prepare();
            } finally {
                updateRenderLCLock.unlock();
            }
        }
    }

    /**
     * Animation for water (and other fluids)
     *
     */
    public void animate() {
        if (!isWorking() && GameRenderer.isLastFrame()) {
            updateRenderLCLock.lock();
            try {
                levelContainer.animate();
                // animate2 light overlay
                levelContainer.lightSources.lightOverlay.triangSwap2(intrface);

                // animate3 light overlay
                levelContainer.lightSources.lightOverlay.triangSwap3(intrface);

                // animate2 light overlay
                levelContainer.lightSources.lightOverlay.triangSwap2(intrface);
            } finally {
                updateRenderLCLock.unlock();
            }
        }
    }

//    /**
//     * Optimize with working/special tuples
//     */
//    private void optimize() {
//        updateRenderLCLock.lock();
//        try {
//            levelContainer.optimize();
//        } finally {
//            updateRenderLCLock.unlock();
//        }
//    }
    // -------------------------------------------------------------------------
    /**
     * Clear Everything. Game will be 'Free'.
     */
    public void clearEverything() {
        Editor.deselect();
        CacheModule.deleteCache();
        LevelContainer.AllBlockMap.init();
        updateRenderLCLock.lock();
        try {
            levelContainer.chunks.clear();
            levelContainer.blockEnvironment.clear();
        } finally {
            updateRenderLCLock.unlock();
        }

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
        updateRenderLCLock.lock();
        try {
            ok |= levelContainer.loadLevelFromFile(fileName);
        } finally {
            updateRenderLCLock.unlock();
        }

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
        updateRenderLCLock.lock();
        try {
            ok |= levelContainer.saveLevelToFile(fileName);
        } finally {
            updateRenderLCLock.unlock();
        }

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
        updateRenderLCLock.lock();
        try {
            ok |= levelContainer.generateRandomLevel(randomLevelGenerator, numberOfBlocks);
        } finally {
            updateRenderLCLock.unlock();
        }

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
        updateRenderLCLock.lock();
        try {
            ok |= levelContainer.generateSinglePlayerLevel(randomLevelGenerator, numberOfBlocks);
        } catch (Exception ex) {
            DSLogger.reportError("Unable to spawn player after the fall!", ex);
        } finally {
            updateRenderLCLock.unlock();
        }

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
        updateRenderLCLock.lock();
        try {
            ok |= levelContainer.generateMultiPlayerLevel(randomLevelGenerator, numberOfBlocks);
        } catch (Exception ex) {
            DSLogger.reportError("Unable to spawn player after the fall!", ex);
        } finally {
            updateRenderLCLock.unlock();
        }

        // Save level to file asynchronously
        CompletableFuture.supplyAsync(() -> {
            return levelContainer.saveLevelToFile(gameServer.getWorldName() + ".ndat");
        }, this.TaskExecutor).thenApply((Boolean rez) -> {
            levelContainer.levelActors.player.setRegistered(rez);
            return null;
        });

//        if (ok) {
//            // !IMPORTANT -- ENABLE ASYNC READPOINT 
//            game.asyncReceivedEnabled = true;
//        }
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
    public boolean generateMultiPlayerLevelAsJoin() throws InterruptedException, ExecutionException, UnsupportedEncodingException {
        boolean success = false;
        this.clearEverything();

        // Register player with Unique ID (UUID)
        if (game.registerPlayer()) {
            // Get world info from server
            LevelMapInfo worldInfo = game.getWorldInfo();

            int numberOfAttempts = 0;
            Game.DownloadStatus status = Game.DownloadStatus.ERR;

            // Locate all level map files with dat or ndat extension
            File clientDir = new File("./");
            String worldNameEscaped = Pattern.quote(gameServer.worldName);
            final Pattern pattern = Pattern.compile(worldNameEscaped + "\\.(n)?dat$", Pattern.CASE_INSENSITIVE);
            List<String> datFileList = Arrays.asList(clientDir.list((dir, name) -> pattern.matcher(name.toLowerCase()).find()));
            GapList<String> datFileListCopy = GapList.create(datFileList);
            String mapFileOrNull = datFileListCopy.getFirstOrNull();
            boolean lvlMapIsCorrect = false;
            CRC32C checksum = new CRC32C();

            if (mapFileOrNull != null) {
                File mapFileLevel = new File(mapFileOrNull);

                if (mapFileLevel.exists()) {
                    try (FileChannel fileChannel = new FileInputStream(mapFileLevel).getChannel()) {
                        int sizeBytes = (int) Files.size(Path.of(mapFileOrNull));
                        ByteBuffer buffer = ByteBuffer.allocate((int) fileChannel.size());
                        while (fileChannel.read(buffer) > 0) {
                            // Do nothing, just read the file into the buffer
                        }
                        buffer.flip();
                        checksum.update(buffer);

                        lvlMapIsCorrect = worldInfo.sizebytes == sizeBytes && worldInfo.chksum == checksum.getValue();
                    } catch (IOException ex) {
                        DSLogger.reportError(ex.getMessage(), ex);
                    } catch (Exception ex) {
                        DSLogger.reportError(String.format("Error - level map info is null! %s", ex.getMessage()), ex);
                    }
                }
            }

            // If level does not exist or is not correct (checksum value is different)
            if (lvlMapIsCorrect) {
                DSLogger.reportInfo("Level Map OK!", null);
                intrface.getConsole().write("Level Map OK!");

                levelContainer.loadLevelFromFile(mapFileOrNull);
            } else {
                do {
                    status = game.downloadLevel();
                } while (status == Game.DownloadStatus.ERR && ++numberOfAttempts <= MAX_ATTEMPTS);

                if (status == Game.DownloadStatus.WARNING) {
                    DSLogger.reportWarning("Connected to empty world - disconnect!", null);
                    intrface.getConsole().write("Connected to empty world - disconnect!");
                    game.disconnectFromServer();
                    return false;
                }

                // Load downloaded level (from fragments)
                levelContainer.loadLevelFromBufferNewFormat();
                // Save world to world name + ndat. For example MyWorld.ndat
                levelContainer.saveLevelToFile(gameServer.worldName + ".ndat");
            }

            if (lvlMapIsCorrect || status == Game.DownloadStatus.OK) {
                try {

                    DSLogger.reportInfo(String.format("World '%s.ndat' saved!", gameServer.worldName), null);
                    intrface.getConsole().write(String.format("World '%s.ndat' saved!", gameServer.worldName));
                    // Avoid reconnection errors
                    // On reconnecting, the player needs to be registered again
                    if (!levelContainer.levelActors.player.isRegistered()) {
                        game.registerPlayer();
                    }

                    // Request player info self+others
                    // It is done because of other players
                    PlayerInfo[] playerInfo = game.getPlayerInfo();
                    if (playerInfo == null) {
                        DSLogger.reportError("Unable to get player info!", null);
                        intrface.getConsole().write("Unable to get player info!", Command.Status.FAILED);
                        return false;
                    }

                    // Configure other players
                    levelContainer.levelActors.configOtherPlayers(playerInfo);
                    // Configure-set main actor
                    // Spawn player (set position)
                    levelContainer.spawnPlayer();
                    // Player set position
                    game.requestSetPlayerPosition();

                    // !IMPORTANT -- ENABLE ASYNC READPOINT 
                    game.asyncReceivedEnabled = true;

                    success = true;
                } catch (Exception ex) {
                    DSLogger.reportWarning("Not able to spawn player. Disconnecting.", null);
                    intrface.getConsole().write("Not able to spawn player. Disconnecting.", Command.Status.WARNING);
                    game.disconnectFromServer();
                    this.clearEverything();
                }
            } else {
                DSLogger.reportWarning("Not able to load level. Disconnecting.", null);
                intrface.getConsole().write("Not able to load level. Disconnecting.", Command.Status.FAILED);
                game.disconnectFromServer();
                this.clearEverything();
            }
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
     * @return test true/false
     */
    public boolean hasCollisionWith(Predictable preditable) { // collision detection - critter against solid obstacles
        return LevelContainer.hasCollisionWithEnvironment(preditable);
    }

    /**
     * Test collision with Environment. Must not leave the SKYBOX and not
     * collide with solid objects or critters. (virtually) => 0.07f dimension
     *
     * @param observer Predictable to have collision with environment
     * @return test true/false
     */
    public boolean hasCollisionWith(Observer observer) { // collision detection - critter against solid obstacles
        return LevelContainer.hasCollisionWithEnvironment(observer);
    }

    /**
     * Test collision with Environment. Must not leave the SKYBOX and not
     * collide with solid objects or critters.
     *
     * @param critter critter (implements predictable). Has (model) body.
     * @return test true/false
     */
    public boolean hasCollisionWith(Critter critter) { // collision detection - critter against solid obstacles
        return LevelContainer.hasCollisionWithEnvironment(critter);
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
