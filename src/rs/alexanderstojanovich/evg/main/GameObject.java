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

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public final class GameObject { // is mutual object for {Main, Renderer, Random Level Generator}
    // this class protects levelContainer, waterRenderer & Random Level Generator between the threads

    private static final Configuration cfg = Configuration.getInstance();

    public static final String TITLE = "Demolition Synergy - v31 BALTIC";

    // makes default window -> Renderer sets resolution from config
    public static final Window MY_WINDOW = Window.getInstance(cfg.getWidth(), cfg.getHeight(), TITLE); // creating the window

    protected final LevelContainer levelContainer;
    protected final WaterRenderer waterRenderer;
    protected final RandomLevelGenerator randomLevelGenerator;

    protected final Intrface intrface;

    protected final AudioPlayer musicPlayer = new AudioPlayer();
    protected final AudioPlayer soundFXPlayer = new AudioPlayer();

    protected boolean assertCollision = false;

    // everyone can access only one instance of the game object
    private static GameObject instance;

    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    private GameObject() {
        this.init();
        this.levelContainer = new LevelContainer(this);
        this.randomLevelGenerator = new RandomLevelGenerator(levelContainer);
        this.waterRenderer = new WaterRenderer(levelContainer);
        this.intrface = new Intrface(this);
    }

    private void init() {
        if (cfg.isFullscreen()) {
            GameObject.MY_WINDOW.fullscreen();
        } else {
            GameObject.MY_WINDOW.windowed();
        }
        if (cfg.isVsync()) {
            GameObject.MY_WINDOW.enableVSync();
        } else {
            GameObject.MY_WINDOW.disableVSync();
        }
        GameObject.MY_WINDOW.centerTheWindow();
        this.musicPlayer.setGain(cfg.getMusicVolume());
        this.soundFXPlayer.setGain(cfg.getSoundFXVolume());
    }

    // lazy initialization allowing only one instance
    /**
     * Get shared Game Object instance. Game Object is controller for the game.
     *
     * @return Game Object instance
     */
    public static GameObject getInstance() {
        if (instance == null) {
            instance = new GameObject();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    /**
     * Update Game Object stuff, like Environment (call only from main)
     *
     * @param deltaTime game object environment update time
     */
    public void update(float deltaTime) {
        if (!levelContainer.isWorking()) { // working check avoids locking the monitor
            PerspectiveRenderer.updatePerspective(MY_WINDOW); // update perspective for all the shaders            
            levelContainer.update(deltaTime);
        }
        intrface.update();
        intrface.setCollText(assertCollision);
    }

    /**
     * Gravity Environment (call only from main)
     *
     * @param deltaTime game object environment update time
     */
    public void gravityDo(float deltaTime) {
        levelContainer.gravityDo(deltaTime);
    }

    /**
     * Renderer method. Requires context to be set in the proper thread (call
     * only from renderer)
     */
    public void render() {
        MasterRenderer.render(); // it clears color bit and depth buffer bit            
        PerspectiveRenderer.render(); // it sets perspective matrix accross shaders       
        if (levelContainer.isWorking()) { // working check avoids locking the monitor
            intrface.getProgText().setEnabled(true);
            intrface.getProgText().setContent("Loading progress: " + Math.round(levelContainer.getProgress()) + "%");
        } else {
            Camera mainCamera = levelContainer.getLevelActors().mainCamera();
            mainCamera.render(ShaderProgram.SHADER_PROGRAMS);
            lock.readLock().lock();
            try {
                levelContainer.render();
                if (Game.isWaterEffects() && !levelContainer.getFluidChunks().getChunkList().isEmpty()) {
                    waterRenderer.render();
                }
            } finally {
                lock.readLock().unlock();
            }
            intrface.getProgText().setEnabled(false);
        }
        intrface.getGameModeText().setContent(Game.getCurrentMode().name());
        intrface.render(ShaderProgram.getIntrfaceShader());
        MY_WINDOW.render();
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
     * Auto load/save level container chunks
     *
     * @return did chunk operations modify anything (something changed).
     */
    public boolean chunkOperations() {
        boolean changed = false;
        lock.writeLock().lock();
        try {
            changed = levelContainer.chunkOperations();
        } finally {
            lock.writeLock().unlock();
        }
        return changed;
    }

    /**
     * Animation for water (and other fluids)
     *
     */
    public void animate() {
        levelContainer.animate();
    }

    /**
     * Optimize with special tuples
     */
    public void optimize() {
        levelContainer.optimize();
    }

    // -------------------------------------------------------------------------
    // Called from concurrent thread
    public void startNewLevel() {
        levelContainer.startNewLevel();
    }

    // Called from concurrent thread
    public boolean loadLevelFromFile(String fileName) {
        boolean ok = levelContainer.loadLevelFromFile(fileName);
        return ok;
    }

    // Called from concurrent thread
    public boolean saveLevelToFile(String fileName) {
        boolean ok = levelContainer.saveLevelToFile(fileName);
        return ok;
    }

    // Called from concurrent thread
    public boolean generateRandomLevel(int numberOfBlocks) {
        boolean ok = levelContainer.generateRandomLevel(randomLevelGenerator, numberOfBlocks);
        return ok;
    }

    // Checked from main and Renderer
    public boolean isWorking() {
        return levelContainer.isWorking();
    }
    // -------------------------------------------------------------------------

    /*    
    * Load the window context and destroyes the window.
     */
    public void destroy() {
        GameObject.MY_WINDOW.loadContext();
        GameObject.MY_WINDOW.destroy();
    }

    // collision detection - critter against solid obstacles
    public boolean hasCollisionWithCritter(Critter critter) {
        return levelContainer.hasCollisionWithEnvironment(critter);
    }

    // collision detection - critter against solid obstacles
    public boolean hasCollisionWithCritter(ModelCritter livingCritter) {
        return levelContainer.hasCollisionWithEnvironment(livingCritter);
    }

    // prints general and detailed information about solid and fluid chunks
    public void printInfo() {
        levelContainer.getSolidChunks().printInfo();
        levelContainer.getFluidChunks().printInfo();
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

    public boolean isAssertCollision() {
        return assertCollision;
    }

    public void setAssertCollision(boolean assertCollision) {
        this.assertCollision = assertCollision;
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

}
