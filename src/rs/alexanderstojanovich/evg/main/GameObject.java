/*
 * Copyright (C) 2020 Coa
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

import org.joml.Vector2f;
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.WaterRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.intrface.Intrface;
import rs.alexanderstojanovich.evg.level.LevelContainer;

/**
 *
 * @author Coa
 */
public final class GameObject { // is mutual object for {Main, Renderer, Random Level Generator}

    private final Window myWindow;
    private final LevelContainer levelContainer;
    private final WaterRenderer waterRenderer;

    private final Intrface intrface;

    private final AudioPlayer musicPlayer = new AudioPlayer();
    private final AudioPlayer soundFXPlayer = new AudioPlayer();

    private boolean assertCollision = false;

    public GameObject(Window myWindow) {
        this.myWindow = myWindow;
        this.levelContainer = new LevelContainer(this);
        this.waterRenderer = new WaterRenderer(this);
        this.intrface = new Intrface(this);
    }

    // update Game Object stuff (call only from main)
    public synchronized void update(float deltaTime) {
        levelContainer.update(deltaTime);
        waterRenderer.refresh();
        intrface.update();
        intrface.setCollText(assertCollision);
    }

    // requires context to be set in the proper thread (call only from renderer)
    public synchronized void render() {
        MasterRenderer.render(); // it clears color bit and depth buffer bit
        if (!levelContainer.isWorking()) {
            levelContainer.render();
            if (Game.isWaterEffects() && !levelContainer.getFluidChunks().getChunkList().isEmpty()) {
                waterRenderer.render();
            }
        } else {
            intrface.getProgText().setContent("Loading progress: " + Math.round(levelContainer.getProgress()) + "%");
            if (!intrface.getProgText().isBuffered()) {
                intrface.getProgText().buffer();
            }
            intrface.getProgText().render();
        }
        intrface.getGameModeText().setContent(Game.getCurrentMode().name());
        intrface.getGameModeText().setOffset(new Vector2f(-Game.getCurrentMode().name().length(), 1.0f));
        intrface.render();
        myWindow.render();
    }

    // hint to the render that objects should be buffered
    public synchronized void unbuffer() {
        levelContainer.getSolidChunks().setBuffered(false);
        levelContainer.getFluidChunks().setBuffered(false);
    }

    // animation for water
    public synchronized void animate() {
        levelContainer.animate();
    }

    // collision detection - critter against solid obstacles
    public boolean hasCollisionWithCritter(Critter critter) {
        return levelContainer.hasCollisionWithCritter(critter);
    }

    public void printInfo() {
        levelContainer.getSolidChunks().printInfo();
        levelContainer.getFluidChunks().printInfo();
    }

    public Window getMyWindow() {
        return myWindow;
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

}
