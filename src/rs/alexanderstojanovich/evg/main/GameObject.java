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
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.WaterRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.critter.Critter;
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

    public static final String TITLE = "Demolition Synergy - v25 ZENITH";

    // makes default window -> Renderer sets resolution from config
    public static final Window MY_WINDOW = new Window(Window.MIN_WIDTH, Window.MIN_HEIGHT, TITLE); // creating the window

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
        this.levelContainer = new LevelContainer(this);
        this.randomLevelGenerator = new RandomLevelGenerator(levelContainer);
        this.waterRenderer = new WaterRenderer(levelContainer);
        this.intrface = new Intrface(this);
        this.init();
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
        musicPlayer.setGain(cfg.getMusicVolume());
        soundFXPlayer.setGain(cfg.getSoundFXVolume());
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
        try {
            lock.writeLock().lock();
            if (!levelContainer.isWorking()) { // working check avoids locking the monitor
                levelContainer.update(deltaTime);
            }
            intrface.update();
            intrface.setCollText(assertCollision);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Renderer method. Requires context to be set in the proper thread (call
     * only from renderer)
     */
    public void render() {
        lock.readLock().lock();
        try {
            MasterRenderer.render(); // it clears color bit and depth buffer bit
            if (levelContainer.isWorking()) { // working check avoids locking the monitor
                intrface.getProgText().setEnabled(true);
                intrface.getProgText().setContent("Loading progress: " + Math.round(levelContainer.getProgress()) + "%");
            } else {
                levelContainer.render();
                if (Game.isWaterEffects() && !levelContainer.getFluidChunks().getChunkList().isEmpty()) {
                    waterRenderer.render();
                }
                intrface.getProgText().setEnabled(false);
            }
            intrface.getGameModeText().setContent(Game.getCurrentMode().name());
            intrface.render(ShaderProgram.getIntrfaceShader());
            MY_WINDOW.render();
        } finally {
            lock.readLock().unlock();
        }
    }

    // -------------------------------------------------------------------------
    /**
     * Calls chunk functions to determine visible chunks
     */
    public void determineVisibleChunks() {
        levelContainer.determineVisible();
    }

    /**
     * Auto load/save level container chunks
     */
    public void chunkOperations() {
        lock.writeLock().lock();
        try {
            levelContainer.chunkOperations();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Animation for water (and other fluids)
     *
     */
    public void animate() {
        lock.readLock().lock();
        try {
            levelContainer.animate();
        } finally {
            lock.readLock().unlock();
        }
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
        return levelContainer.hasCollisionWithCritter(critter);
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
