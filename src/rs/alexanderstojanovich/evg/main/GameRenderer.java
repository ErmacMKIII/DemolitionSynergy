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
import org.lwjgl.glfw.GLFW;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameRenderer extends Thread implements Executor {

    protected static final Configuration cfg = Configuration.getInstance();
    protected final GameObject gameObject;

    public static final int NUM_OF_PASSES_MAX = cfg.getRendererPasses(); // DEFAULT is 10.
    private static double fpsTicks = 0.0;
    private static int fps = 0;
    private static int numOfPasses = 0;

    public static final Queue<FutureTask<Object>> TASK_QUEUE = new ArrayDeque<>();

    protected FutureTask<Object> task;
    protected static double animationTimer = 0.0;
    protected static double glCommandTimer = 0.0;

    protected static double ANIMATION_RATE = 0.25;
    protected static double GL_COMMAND_POLL_RATE = Game.TICK_TIME;

    /**
     * Core component. Game renderer. Everything rendered to the screen happens
     * here.
     *
     * @param gameObject Game Object
     */
    public GameRenderer(GameObject gameObject) {
        super("Renderer");
        this.gameObject = gameObject;
    }

    @Override
    public void run() {
        gameObject.masterRenderer.initGL(cfg); // loads myWindow context, creates OpenGL context..        
        ShaderProgram.initAllShaders(); // it's important that first GL is done and then this one 
        gameObject.perspectiveRenderer.init();
        Texture.bufferAllTextures();
        gameObject.GameAssets.bufferAllTextures();
//        DSLogger.reportDebug("Textures loaded!", null);
        try {
            gameObject.waterRenderer.getFrameBuffer().initBuffer(); // it is tuned in the correct OpenGL context (color & depth buffer)
            gameObject.shadowRenderer.getFrameBuffer().initBuffer(); // it is tuned in the correct OpenGL context (depth buffer only) 
        } catch (Exception ex) {
            DSLogger.reportInfo(ex.getMessage(), ex);
        }
        do {
            gameObject.render(); // render splash screen
        } while ((Game.accumulator * Game.TPS) < 1.0);
        gameObject.splashScreen.setEnabled(false);

        // resolution config
        gameObject.masterRenderer.setResolution(cfg.getWidth(), cfg.getHeight());
        gameObject.perspectiveRenderer.updatePerspective();
        animationTimer = 0;
        glCommandTimer = 0;

        fps = 0;

        double lastTime = GLFW.glfwGetTime();
        double currTime;
        double deltaTime; // time between frames

        while (!gameObject.WINDOW.shouldClose()) {
            currTime = GLFW.glfwGetTime();
            deltaTime = currTime - lastTime;
            fpsTicks += deltaTime * Game.getFpsMax();
            lastTime = currTime;

            // Detecting critical status
            if (fps == 0 && deltaTime > Game.CRITICAL_TIME) {
                DSLogger.reportFatalError("Game status critical!", null);
                gameObject.WINDOW.close();
                break;
            }

            // also avoid rendering when game is updating
            numOfPasses = 0; // Start with PASS0
            while (couldRender()) {
                // render the scene
                gameObject.render();
                fps++;
                numOfPasses++;
                fpsTicks--;
            }

            // animates water every quarter of the second
            animationTimer += deltaTime;
            if (animationTimer >= GameRenderer.ANIMATION_RATE) {
                if (!gameObject.isWorking()) {
                    gameObject.animate(); // avoid swap and animate in the same time - 'awful effect'
                }

                animationTimer = 0.0;
            }

            glCommandTimer += deltaTime;
            // lastly it executes the console tasks
            if (glCommandTimer >= GameRenderer.GL_COMMAND_POLL_RATE) {
                if ((task = TASK_QUEUE.poll()) != null) {
                    execute(task); // requires GL-context
                }

                glCommandTimer = 0.0;
            }
        }

        // clean up resources from GL side
        release();

        // renderer is reaching end of life!
        Window.unloadContext();

        DSLogger.reportDebug("Renderer exited.", null);
    }

    /**
     * Is rendering first frame.
     *
     * @return is rendering first frame
     */
    public static boolean isFirstFrame() {
        return fpsTicks >= 1.0 && numOfPasses == 0;
    }

    /**
     * Is rendering last frame.
     *
     * @return is rendering last frame
     */
    public static boolean isLastFrame() {
        return fpsTicks < 1.0 || numOfPasses >= NUM_OF_PASSES_MAX;
    }

    /**
     * Should or Could Game Render render. Game can be rendered (again) if
     * lesser than enough passes and if is not updating.
     *
     * @return could render bool
     */
    public static boolean couldRender() {
        return fpsTicks >= 1.0 && GameRenderer.numOfPasses < GameRenderer.NUM_OF_PASSES_MAX;// && (Game.accumulator * Game.TPS) < 2.0;
    }

//    /**
//     * Could Game Render animate.
//     *
//     * @return could render bool
//     */
//    public static boolean couldAnimate() {
//        return (Math.round(fpsTicks) & 7) == 0;
//    }
    /**
     * Cleans all the buffers from the renderer (optimized tuples contains them,
     * interface as well).
     */
    protected void release() {
        gameObject.release();
        DSLogger.reportDebug("Game content resources deleted.", null);
    }

    @Override
    public void execute(Runnable command) {
        command.run();
    }

    public LevelContainer getLevelContainer() {
        return gameObject.getLevelContainer();
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

    public static double getGlCommandTimer() {
        return glCommandTimer;
    }

    public static void setAnimationTimer(int animationTimer) {
        GameRenderer.animationTimer = animationTimer;
    }

    public static void setGlCommandTimer(int glCommandTimer) {
        GameRenderer.glCommandTimer = glCommandTimer;
    }

    public static int getNumOfPasses() {
        return numOfPasses;
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public FutureTask<Object> getTask() {
        return task;
    }

}
