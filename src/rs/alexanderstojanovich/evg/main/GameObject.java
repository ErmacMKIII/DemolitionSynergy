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

import java.util.Timer;
import java.util.TimerTask;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.PerspectiveRenderer;
import rs.alexanderstojanovich.evg.core.WaterRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.critter.Predictable;
import rs.alexanderstojanovich.evg.intrface.ConcurrentDialog;
import rs.alexanderstojanovich.evg.intrface.Intrface;
import rs.alexanderstojanovich.evg.intrface.Quad;
import rs.alexanderstojanovich.evg.level.Chunk;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.level.RandomLevelGenerator;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public final class GameObject { // is mutual object for {Main, Renderer, Random Level Generator}
    // this class protects levelContainer, waterRenderer & Random Level Generator between the threads
    // game logic is contained in here

    protected static boolean initialized = false;

    private static final Configuration cfg = Configuration.getInstance();

    public static final String TITLE = "Demolition Synergy - v34FOX";

    // makes default window -> Renderer sets resolution from config
    public static Window MY_WINDOW;

    protected static LevelContainer levelContainer;
    protected static WaterRenderer waterRenderer;
    protected static RandomLevelGenerator randomLevelGenerator;

    protected static Intrface intrface;

    protected static AudioPlayer musicPlayer;
    protected static AudioPlayer soundFXPlayer;

    protected static Game game;
    protected static GameRenderer renderer;

    public static Quad SPLASH_SCREEN;

    public static final Object MUTEX = new Object();

    /**
     * Init this game container.
     */
    public static void init() {
        MY_WINDOW = Window.getInstance(cfg.getWidth(), cfg.getHeight(), TITLE); // creating the window
        SPLASH_SCREEN = new Quad(GameObject.MY_WINDOW.getWidth(), GameObject.MY_WINDOW.getHeight(), Texture.CONSOLE, true);
        SPLASH_SCREEN.setColor(Vector3fColors.YELLOW);
        levelContainer = new LevelContainer();
        randomLevelGenerator = new RandomLevelGenerator(levelContainer);
        waterRenderer = new WaterRenderer(levelContainer);
        musicPlayer = new AudioPlayer();
        soundFXPlayer = new AudioPlayer();
        intrface = new Intrface();
        //----------------------------------------------------------------------
        MY_WINDOW.setFullscreen(cfg.isFullscreen());
//        MY_WINDOW.setVSync(cfg.isVsync()); //=> code disabled due to not in Renderer
        MY_WINDOW.centerTheWindow();
        //----------------------------------------------------------------------
        musicPlayer.setGain(cfg.getMusicVolume());
        soundFXPlayer.setGain(cfg.getSoundFXVolume());
        //----------------------------------------------------------------------                        
        game = new Game(); // init game with given configuration and game object
        renderer = new GameRenderer(); // init renderer with given game object
        DSLogger.reportDebug("Game initialized.", null);
        //----------------------------------------------------------------------
        initialized = true;
    }

    /**
     * Start this game container. Starts loop and renderer.
     */
    public static void start() {
        if (!initialized) {
            return;
        }

        Timer timer1 = new Timer("Timer Utils");
        TimerTask task1 = new TimerTask() {
            @Override
            public void run() {
                intrface.getUpdText().setContent("ups: " + Game.getUps());
                Game.setUps(0);
                intrface.getFpsText().setContent("fps: " + GameRenderer.getFps());
                GameRenderer.setFps(0);
            }
        };
        timer1.scheduleAtFixedRate(task1, 1000L, 1000L);
        //----------------------------------------------------------------------
        renderer.start();
        DSLogger.reportDebug("Renderer started.", null);
        DSLogger.reportDebug("Game will start soon.", null);
        game.go();
        game.cleanUp();
        intrface.cleanUp();
        //----------------------------------------------------------------------
        try {
            renderer.join(); // and it's blocked here until it finishes
        } catch (InterruptedException ex) {
            DSLogger.reportError(ex.getMessage(), ex);
        }
        timer1.cancel();
    }

    // -------------------------------------------------------------------------
    /**
     * Update Game Object stuff, like Environment (call only from main)
     *
     * @param deltaTime game object environment update time
     */
    public static void update(float deltaTime) {
        if (!initialized) {
            return;
        }

        if (levelContainer.isWorking()) {
            intrface.getProgText().setEnabled(true);
            intrface.getProgText().setContent("Loading progress: " + Math.round(levelContainer.getProgress()) + "%");
        } else { // working check avoids locking the monitor
            PerspectiveRenderer.updatePerspective(MY_WINDOW); // update perspective for all the shaders     
            synchronized (MUTEX) {
                levelContainer.update(deltaTime);
            }
            Vector3f pos = levelContainer.levelActors.mainActor().getPos();
            int chunkId = Chunk.chunkFunc(pos);
            intrface.getPosText().setContent(String.format("pos: (%.1f,%.1f,%.1f)", pos.x, pos.y, pos.z));
            intrface.getChunkText().setContent(String.format("chunkId: %d", chunkId));
            intrface.getGameModeText().setContent(Game.getCurrentMode().name());
            GameTime now = GameTime.Now();
            intrface.getGameTimeText().setContent(String.format("%02d:%02d:%02d", now.hours, now.minutes, now.seconds));
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

        if (intrface.getScreenText().isEnabled()) {
            intrface.getScreenText().setEnabled(false);
        }

        if (GameObject.getLevelContainer().getProgress() == 0.0f
                || GameObject.getLevelContainer().getProgress() == 100.0f) {
            GameObject.intrface.getProgText().setEnabled(false);
            GameObject.getLevelContainer().setProgress(0.0f);
        }

        synchronized (MUTEX) {
            intrface.update();
        }
    }

    public static void assertCheckCollision(boolean collision) {
        intrface.setCollText(collision);
        intrface.getCollText().unbuffer();
    }

    /**
     * Gravity Environment (call only from main)
     *
     * @param deltaTime game object environment update time
     */
    public static void gravityDo(float deltaTime) {
        if (!initialized) {
            return;
        }

        levelContainer.gravityDo(deltaTime);
    }

    /**
     * First optimization - There are blocks but nothing is rendered.
     *
     * @return is first optimization
     */
    public static boolean isFirstOptimization() {
        return !GameObject.levelContainer.chunks.getChunkList().isEmpty()
                && GameObject.levelContainer.chunks.getOptimizedTuples().isEmpty();
    }

    /**
     * Renderer method. Requires context to be set in the proper thread (call
     * only from renderer)
     */
    public static void render() {
        if (!initialized || GameObject.MY_WINDOW.shouldClose()) {
            return;
        }

        MasterRenderer.render(); // it clears color bit and depth buffer bit
        if (SPLASH_SCREEN.isEnabled()) {
            if (!SPLASH_SCREEN.isBuffered()) {
                SPLASH_SCREEN.bufferSmart();
            }
            SPLASH_SCREEN.render(ShaderProgram.getIntrfaceShader());
        } else {
            if (!PerspectiveRenderer.isBuffered()) {
                PerspectiveRenderer.bufferAndRender(); // it sets perspective matrix accross shaders       
            }

            if (!levelContainer.isWorking()) { // working check avoids locking the monitor
                Camera mainCamera = levelContainer.getLevelActors().mainCamera();
                mainCamera.render(ShaderProgram.SHADER_PROGRAMS);
                synchronized (MUTEX) {
                    levelContainer.render();
                    if (Game.isWaterEffects() && !levelContainer.getChunks().getChunkList().isEmpty()) {
                        waterRenderer.render();
                    }
                }
            }
            synchronized (MUTEX) {
                intrface.render(ShaderProgram.getIntrfaceShader());
            }
        }

        MY_WINDOW.render();
    }

    /*
    * Release all GL components (by deleting their buffers)
    *  Call from renderer.
     */
    public static void release() {
        GameObject.SPLASH_SCREEN.release();
        GameObject.intrface.release();
        GameObject.waterRenderer.release();
        GameObject.levelContainer.chunks.getOptimizedTuples().forEach(t -> t.release());
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
    public static boolean determineVisibleChunks() {
        return levelContainer.determineVisible();
    }

    /**
     * Auto load/save level container chunks
     *
     * @return did chunk operations modify anything (something changed).
     */
    public static boolean chunkOperations() {
        synchronized (MUTEX) {
            return levelContainer.chunkOperations();
        }
    }

    /**
     * Animation for water (and other fluids)
     *
     */
    public static void animate() {
        synchronized (MUTEX) {
            levelContainer.animate();
        }
    }

    /**
     * Optimize with special tuples
     */
    public static void optimize() {
        synchronized (MUTEX) {
            levelContainer.chunks.setOptimized(false); // this is also hint to not render
            levelContainer.optimize();
            LevelContainer.LIGHT_SOURCES.setAllModified();
        }
    }

    /**
     * Is level container optimized
     */
    public static void isOptimized() {
        synchronized (MUTEX) {
            levelContainer.chunks.isOptimized();
        }
    }

    /**
     * Set level container optimization flag
     *
     * @param optimized optimized flag to set
     */
    public static void setOptimized(boolean optimized) {
        synchronized (MUTEX) {
            levelContainer.chunks.setOptimized(optimized);
        }
    }

    // -------------------------------------------------------------------------
    // Called from concurrent thread
    public static void startNewLevel() {
        levelContainer.startNewLevel();
    }

    // Called from concurrent thread
    public static boolean loadLevelFromFile(String fileName) {
        boolean ok = levelContainer.loadLevelFromFile(fileName);
        return ok;
    }

    // Called from concurrent thread
    public static boolean saveLevelToFile(String fileName) {
        boolean ok = levelContainer.saveLevelToFile(fileName);
        return ok;
    }

    // Called from concurrent thread
    public static boolean generateRandomLevel(int numberOfBlocks) {
        boolean ok = levelContainer.generateRandomLevel(randomLevelGenerator, numberOfBlocks);
        return ok;
    }

    // Called from concurrent thread
    public static boolean generateSinglePlayerLevel(int numberOfBlocks) {
        boolean ok = levelContainer.generateSinglePlayerLevel(randomLevelGenerator, numberOfBlocks);
        return ok;
    }

    // TODO: Not implemented..
    public static boolean generateMultiPlayerLevel(int numberOfBlocks) {
        return false;
    }

    // Checked from main and Renderer
    public static boolean isWorking() {
        return levelContainer.isWorking();
    }
    // -------------------------------------------------------------------------

    /*    
    * Load the window context and destroyes the window.
     */
    public static void destroy() {
        ConcurrentDialog.EXECUTOR.shutdown();
        MY_WINDOW.destroy();
    }

    /**
     * Test collision with Environment. Must not leave the SKYBOX and not
     * collide with solid objects or critters.
     *
     * @param preditable Predictable to have collision with environment
     * @return test true/false
     */
    public static boolean hasCollisionWith(Predictable preditable) { // collision detection - critter against solid obstacles
        return LevelContainer.hasCollisionWithEnvironment(preditable);
    }

    /**
     * Test collision with Environment. Must not leave the SKYBOX and not
     * collide with solid objects or critters. (virtually) => 0.07f dimension
     *
     * @param observer Predictable to have collision with environment
     * @return test true/false
     */
    public static boolean hasCollisionWith(Observer observer) { // collision detection - critter against solid obstacles
        return LevelContainer.hasCollisionWithEnvironment(observer);
    }

    /**
     * Test collision with Environment. Must not leave the SKYBOX and not
     * collide with solid objects or critters.
     *
     * @param critter critter (implements predictable). Has (model) body.
     * @return test true/false
     */
    public static boolean hasCollisionWith(Critter critter) { // collision detection - critter against solid obstacles
        return LevelContainer.hasCollisionWithEnvironment(critter);
    }

    // prints general and detailed information about solid and fluid chunks
    public static void printInfo() {
        levelContainer.getChunks().printInfo();
    }

    public static LevelContainer getLevelContainer() {
        return levelContainer;
    }

    public static AudioPlayer getMusicPlayer() {
        return musicPlayer;
    }

    public static AudioPlayer getSoundFXPlayer() {
        return soundFXPlayer;
    }

    public static WaterRenderer getWaterRenderer() {
        return waterRenderer;
    }

    public static Intrface getIntrface() {
        return intrface;
    }

    public static RandomLevelGenerator getRandomLevelGenerator() {
        return randomLevelGenerator;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static Configuration getCfg() {
        return cfg;
    }

    public static Game getGame() {
        return game;
    }

    public static GameRenderer getRenderer() {
        return renderer;
    }

}
