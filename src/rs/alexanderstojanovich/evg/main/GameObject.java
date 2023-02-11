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
import rs.alexanderstojanovich.evg.critter.ModelCritter;
import rs.alexanderstojanovich.evg.intrface.Intrface;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.level.RandomLevelGenerator;
import rs.alexanderstojanovich.evg.models.Chunk;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public final class GameObject { // is mutual object for {Main, Renderer, Random Level Generator}
    // this class protects levelContainer, waterRenderer & Random Level Generator between the threads

    protected static boolean initialized = false;

    private static final Configuration cfg = Configuration.getInstance();

    public static final String TITLE = "Demolition Synergy - v31 BALTIC";

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

    /**
     * Init this game container.
     */
    public static void init() {
        MY_WINDOW = Window.getInstance(cfg.getWidth(), cfg.getHeight(), TITLE); // creating the window
        levelContainer = new LevelContainer();
        randomLevelGenerator = new RandomLevelGenerator(levelContainer);
        waterRenderer = new WaterRenderer(levelContainer);
        musicPlayer = new AudioPlayer();
        soundFXPlayer = new AudioPlayer();
        intrface = new Intrface();
        //----------------------------------------------------------------------
        if (cfg.isFullscreen()) {
            MY_WINDOW.fullscreen();
        } else {
            MY_WINDOW.windowed();
        }
        if (cfg.isVsync()) {
            MY_WINDOW.enableVSync();
        } else {
            MY_WINDOW.disableVSync();
        }
        MY_WINDOW.centerTheWindow();
        //----------------------------------------------------------------------
        musicPlayer.setGain(cfg.getMusicVolume());
        soundFXPlayer.setGain(cfg.getSoundFXVolume());
        //----------------------------------------------------------------------                        
        game = new Game(); // init game with given configuration and game object
        renderer = new GameRenderer(); // init renderer with given game object
        DSLogger.reportInfo("Game initialized.", null);
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
        DSLogger.reportInfo("Renderer started.", null);
        DSLogger.reportInfo("Game will start soon.", null);
        game.go();
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
            levelContainer.update(deltaTime);
            Vector3f pos = levelContainer.levelActors.getMainActor().getPosition();
            int chunkId = Chunk.chunkFunc(pos);
            intrface.getPosText().setContent(String.format("pos: (%.1f,%.1f,%.1f)", pos.x, pos.y, pos.z));
            intrface.getChunkText().setContent(String.format("chunkId: %d", chunkId));
            intrface.getGameModeText().setContent(Game.getCurrentMode().name());
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

        if (GameObject.getLevelContainer().getProgress() == 0.0f
                || GameObject.getLevelContainer().getProgress() == 100.0f) {
            GameObject.intrface.getProgText().setEnabled(false);
            GameObject.getLevelContainer().setProgress(0.0f);
        }

        intrface.update();
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
    public void gravityDo(float deltaTime) {
        if (!initialized) {
            return;
        }

        levelContainer.gravityDo(deltaTime);
    }

    /**
     * Renderer method. Requires context to be set in the proper thread (call
     * only from renderer)
     */
    public static void render() {
        if (!initialized) {
            return;
        }

        MasterRenderer.render(); // it clears color bit and depth buffer bit            
        PerspectiveRenderer.render(); // it sets perspective matrix accross shaders       
        if (!levelContainer.isWorking()) { // working check avoids locking the monitor
            Camera mainCamera = levelContainer.getLevelActors().mainCamera();
            mainCamera.render(ShaderProgram.SHADER_PROGRAMS);

            levelContainer.render();
            if (Game.isWaterEffects() && !levelContainer.getFluidChunks().getChunkList().isEmpty()) {
                waterRenderer.render();
            }
        }
        intrface.render(ShaderProgram.getIntrfaceShader());
        MY_WINDOW.render();
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
        boolean changed = false;
        changed = levelContainer.chunkOperations();

        return changed;
    }

    /**
     * Animation for water (and other fluids)
     *
     */
    public static void animate() {
        levelContainer.animate();
    }

    /**
     * Optimize with special tuples
     */
    public static void optimize() {
        levelContainer.optimize();
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

    // Checked from main and Renderer
    public static boolean isWorking() {
        return levelContainer.isWorking();
    }
    // -------------------------------------------------------------------------

    /*    
    * Load the window context and destroyes the window.
     */
    public static void destroy() {
        MY_WINDOW.loadContext();
        MY_WINDOW.destroy();
    }

    // collision detection - critter against solid obstacles
    public static boolean hasCollisionWithCritter(Critter critter) {
        return levelContainer.hasCollisionWithEnvironment(critter);
    }

    // collision detection - critter against solid obstacles
    public static boolean hasCollisionWithCritter(ModelCritter livingCritter) {
        return levelContainer.hasCollisionWithEnvironment(livingCritter);
    }

    // prints general and detailed information about solid and fluid chunks
    public static void printInfo() {
        levelContainer.getSolidChunks().printInfo();
        levelContainer.getFluidChunks().printInfo();
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
