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

import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.WaterRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.intrface.Intrface;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.level.RandomLevelGenerator;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public final class GameObject { // is mutual object for {Main, Renderer, Random Level Generator}
    // this class protects levelContainer, waterRenderer & Random Level Generator between the threads

    public static final String TITLE = "Demolition Synergy - v20 URANIUM";

    public static final Object OBJ_MUTEX = new Object(); // mutex for window, used for game and renderer

    // makes default window -> Renderer sets resolution from config
    public static final Window MY_WINDOW = new Window(Window.MIN_WIDTH, Window.MIN_HEIGHT, TITLE); // creating the window

    private final LevelContainer levelContainer;
    private final WaterRenderer waterRenderer;
    private final RandomLevelGenerator randomLevelGenerator;

    private final Intrface intrface;

    private final AudioPlayer musicPlayer = new AudioPlayer();
    private final AudioPlayer soundFXPlayer = new AudioPlayer();

    private boolean assertCollision = false;

    // everyone can access only one instance of the game object
    private static GameObject instance;

    private GameObject() {
        this.levelContainer = new LevelContainer(this);
        this.randomLevelGenerator = new RandomLevelGenerator(levelContainer);
        this.waterRenderer = new WaterRenderer(levelContainer);
        this.intrface = new Intrface(this);
    }

    // lazy initialization allowing only one instance
    public static GameObject getInstance() {
        if (instance == null) {
            instance = new GameObject();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // update Game Object stuff (call only from main)
    public void update(float deltaTime) {
        if (!levelContainer.isWorking()) { // working check avoids locking the monitor
            levelContainer.update(deltaTime);
            if (Game.isWaterEffects()) {
                waterRenderer.refresh();
            }
        }
        intrface.update();
        intrface.setCollText(assertCollision);
    }

    // requires context to be set in the proper thread (call only from renderer)
    public void render() {
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
        intrface.render();
        MY_WINDOW.render();
    }

    // -------------------------------------------------------------------------
    // hint to the render that objects should be buffered
    public void unbuffer() {
        levelContainer.getSolidChunks().setBuffered(false);
        levelContainer.getFluidChunks().setBuffered(false);
    }

    // calls determine visible chunks
    public void determineVisibleChunks() {
        levelContainer.determineVisible();
    }

    // auto load/save level container chunks
    public void autoDoChunks() {
        levelContainer.chunkOperations();
    }

    // animation for water
    public void animate() {
        levelContainer.animate();
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

    // destroys the window
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
