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

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.PerspectiveRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.MathUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameRenderer extends Thread implements Executor {

    protected static final Configuration cfg = Configuration.getInstance();
    public static final int NUM_OF_PASSES_MAX = cfg.getRendererPasses();
    private static double fpsTicks = 0.0;
    private static int fps = 0;

    public static final Queue<FutureTask<Object>> TASK_QUEUE = new ArrayDeque<>();

    protected FutureTask<Object> task;
    protected static double animationTimer = 0.0;

    /**
     * Core component. Game renderer. Everything rendered to the screen happens
     * here.
     */
    public GameRenderer() {
        super("Renderer");
    }

    @Override
    public void run() {
        MasterRenderer.initGL(GameObject.MY_WINDOW, cfg); // loads myWindow context, creates OpenGL context..
        MasterRenderer.setResolution(GameObject.MY_WINDOW.getWidth(), GameObject.MY_WINDOW.getHeight());
        ShaderProgram.initAllShaders(); // it's important that first GL is done and then this one 
        PerspectiveRenderer.updatePerspective(GameObject.MY_WINDOW); // updates perspective for all the existing shaders
        Texture.bufferAllTextures();
        GameObject.getWaterRenderer().getFrameBuffer().init(); // it is tuned in the correct OpenGL context
        do {
            GameObject.render(); // render splash screen
        } while (Game.upsTicks < 1.0);
        GameObject.SPLASH_SCREEN.setEnabled(false);
        animationTimer = 0.0;

        fps = 0;

        double lastTime = Game.accumulator * Game.TICK_TIME;
        double currTime;
        double deltaTime = 0.0;

        while (!GameObject.MY_WINDOW.shouldClose()) {
            currTime = Game.accumulator * Game.TICK_TIME;
            deltaTime = Math.max(currTime - lastTime, 0.0);
            fpsTicks += MathUtils.lerp(deltaTime * Game.getFpsMax(), deltaTime * fps, 5.0E-4);
            lastTime = currTime;

            // Detecting critical status
            if (fps == 0 && deltaTime > Game.CRITICAL_TIME) {
                DSLogger.reportFatalError("Game status critical!", null);
                GameObject.MY_WINDOW.close();
                break;
            }

            int numOfPasses = 0;
            // also avoid rendering when game is updating 
            while (fpsTicks >= 1.0 && numOfPasses < NUM_OF_PASSES_MAX && Game.getUpsTicks() < 1.0) {
                GameObject.render();
                fps++;
                numOfPasses++;
                fpsTicks--;
            }

            // update text which animates water every quarter of the second
            if (Game.accumulator - animationTimer > 20.0) {
                if (!GameObject.isWorking() && Game.getUpsTicks() < 1.0) {
                    GameObject.animate();
                }

                animationTimer += 20.0;
            }

            // lastly it executes the console tasks
            if ((task = TASK_QUEUE.poll()) != null) {
                execute(task);
            }
        }

        // clean up resources from GL side
        release();

        // renderer is reaching end of life!
        Window.unloadContext();

        DSLogger.reportDebug("Renderer exited.", null);
    }

    /**
     * Cleans all the buffers from the renderer (optimized tuples contains them,
     * interface as well).
     */
    protected void release() {
        GameObject.release();
        DSLogger.reportDebug("Game content resources deleted.", null);
    }

    @Override
    public void execute(Runnable command) {
        command.run();
    }

    public LevelContainer getLevelContainer() {
        return GameObject.getLevelContainer();
    }

    public static double getFpsTicks() {
        return fpsTicks;
    }

    public static void setFpsTicks(double fpsTicks) {
        GameRenderer.fpsTicks = fpsTicks;
    }

    public static int getFps() {
        return fps;
    }

    public static void setFps(int fps) {
        GameRenderer.fps = fps;
    }

    public static Configuration getCfg() {
        return cfg;
    }

    public static double getAnimationTimer() {
        return animationTimer;
    }

    public static void setAnimationTimer(double animationTimer) {
        GameRenderer.animationTimer = animationTimer;
    }

}
