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

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameRenderer extends Thread implements Executor {

    private final GameObject gameObject;

    private static double fpsTicks = 0.0;
    private static int fps = 0;

    private int widthGL = Window.MIN_WIDTH;
    private int heightGL = Window.MIN_HEIGHT;

    public static final Queue<FutureTask<Object>> TASK_QUEUE = new ArrayDeque<>();

    public GameRenderer(GameObject gameObject) {
        super("Renderer");
        this.gameObject = gameObject;
    }

    @Override
    public void run() {
        MasterRenderer.initGL(GameObject.MY_WINDOW); // loads myWindow context, creates OpenGL context..
        MasterRenderer.setResolution(GameObject.MY_WINDOW.getWidth(), GameObject.MY_WINDOW.getHeight());
        ShaderProgram.initAllShaders(); // it's important that first GL is done and then this one 
        PerspectiveRenderer.updatePerspective(GameObject.MY_WINDOW); // updates perspective for all the existing shaders
        Texture.bufferAllTextures();
        gameObject.getWaterRenderer().getFrameBuffer().tune(); // it is tuned in the correct OpenGL context          

        double timer1 = 0.0;
        double timer2 = 0.0;

        fps = 0;

        double lastTime = Game.accumulator * Game.TICK_TIME;
        double currTime;
        double deltaTime = 0.0;

        while (!GameObject.MY_WINDOW.shouldClose()) {
            // changing resolution if necessary
            int width = GameObject.MY_WINDOW.getWidth();
            int height = GameObject.MY_WINDOW.getHeight();
            if (width != widthGL
                    || height != heightGL) {
                MasterRenderer.setResolution(width, height);
                PerspectiveRenderer.updatePerspective(GameObject.MY_WINDOW);
                widthGL = width;
                heightGL = height;
            }

            currTime = Game.accumulator * Game.TICK_TIME;
            deltaTime = currTime - lastTime;
            fpsTicks += deltaTime * Game.getFpsMax();
            lastTime = currTime;

            // Detecting critical status
            if (fps == 0 && deltaTime > Game.CRITICAL_TIME) {
                DSLogger.reportFatalError("Game status critical!", null);
                GameObject.MY_WINDOW.close();
                break;
            }

            int numOfPasses = 0;
            while (fpsTicks >= 1.0) {
                if (numOfPasses < Game.getUpsTicks()) {
                    gameObject.render();
                    if (!gameObject.isWorking() && Game.accumulator - timer2 > 20.0) {
                        gameObject.animate();
                    }
                    fps++;
                    numOfPasses++;
                }
                fpsTicks--;
            }

            // update text which shows dialog every 500.0 ticks                
            if (Game.accumulator - timer1 > 500.0) {
                if (gameObject.getIntrface().getSaveDialog().isDone()) {
                    gameObject.getIntrface().getSaveDialog().setEnabled(false);
                }
                if (gameObject.getIntrface().getLoadDialog().isDone()) {
                    gameObject.getIntrface().getLoadDialog().setEnabled(false);
                }
                if (gameObject.getIntrface().getLoadDialog().isDone()) {
                    gameObject.getIntrface().getLoadDialog().setEnabled(false);
                }
                if (gameObject.getIntrface().getRandLvlDialog().isDone()) {
                    gameObject.getIntrface().getRandLvlDialog().setEnabled(false);
                }

                if (gameObject.getIntrface().getSinglePlayerDialog().isDone()) {
                    gameObject.getIntrface().getSinglePlayerDialog().setEnabled(false);
                }

                gameObject.getIntrface().getCollText().setContent("");
                gameObject.getIntrface().getScreenText().setEnabled(false);

                timer1 += 500.0;
            }

            // update text which animates water every quarter of the second
            if (Game.accumulator - timer2 > 20.0) {
                if (gameObject.getLevelContainer().getProgress() == 100.0f) {
                    gameObject.getIntrface().getProgText().setEnabled(false);
                    gameObject.getLevelContainer().setProgress(0.0f);
                }
                timer2 += 20.0;
            }

            // lastly it executes the console tasks
            FutureTask<Object> task;
            while ((task = TASK_QUEUE.poll()) != null) {
                execute(task);
            }
        }

        // renderer is reaching end of life!
        Window.unloadContext();

        DSLogger.reportInfo("Renderer exited.", null);
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

}
