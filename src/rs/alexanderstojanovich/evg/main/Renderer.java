/*
 * Copyright (C) 2019 Coa
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

import org.lwjgl.glfw.GLFW;
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.PerspectiveRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Coa
 */
public class Renderer extends Thread {

    private final GameObject gameObject;

    private boolean assertCollision = false;

    private static double fpsTicks = 0.0;
    private int fps = 0;

    private static int renPasses = 0;
    public static final int REN_MAX_PASSES = 10;

    public Renderer(GameObject gameObject) {
        super("Renderer");
        this.gameObject = gameObject;
    }

    @Override
    public void run() {
        MasterRenderer.initGL(gameObject.getMyWindow()); // loads myWindow context, creates OpenGL context..
        ShaderProgram.initAllShaders(); // it's important that first GL is done and then this one 
        PerspectiveRenderer.updatePerspective(gameObject.getMyWindow()); // updates perspective for all the existing shaders
        Texture.bufferAllTextures();

        double timer0 = GLFW.glfwGetTime();
        double timer1 = GLFW.glfwGetTime();
        double timer2 = GLFW.glfwGetTime();

        double lastTime = GLFW.glfwGetTime();
        double currTime;
        double diff;

        while (!gameObject.getMyWindow().shouldClose()) {
            currTime = GLFW.glfwGetTime();
            diff = currTime - lastTime;
            fpsTicks += diff * Game.getFpsMax();
            lastTime = currTime;

            // Detecting critical status
            if (fps == 0 && diff > Game.CRITICAL_TIME) {
                DSLogger.reportFatalError("Game status critical!", null);
                gameObject.getMyWindow().close();
                break;
            }

            if (fpsTicks >= 1.0) {
                synchronized (Main.OBJ_MUTEX) {
                    gameObject.getMyWindow().loadContext();
                    while (fpsTicks >= 1.0 && renPasses < REN_MAX_PASSES) {
                        gameObject.render();
                        fps++;
                        fpsTicks--;
                        renPasses++;
                    }
                    renPasses = 0;
                    Window.unloadContext();
                }
            }

            // update text which shows ups and fps every second
            if (GLFW.glfwGetTime() > timer0 + 1.0) {
                gameObject.getIntrface().getFpsText().setContent("fps: " + fps);
                fps = 0;
                timer0 += 1.0;
            }

            // update text which shows dialog every 5 seconds
            if (GLFW.glfwGetTime() > timer1 + 5.0) {
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

                timer1 += 5.0;
            }

            // update text which animates water every quarter of the second
            if (GLFW.glfwGetTime() > timer2 + 0.25) {
                if (gameObject.getLevelContainer().getProgress() == 100) {
                    gameObject.getIntrface().getProgText().setEnabled(false);
                    gameObject.getLevelContainer().setProgress(0);
                }

                if (gameObject.getLevelContainer().getProgress() == 0.0f && !gameObject.getLevelContainer().isWorking()) {
                    synchronized (Main.OBJ_MUTEX) {
                        gameObject.getMyWindow().loadContext();
                        gameObject.animate();
                        Window.unloadContext();
                    }
                }
                timer2 += 0.25;
            }
        }

    }

    public LevelContainer getLevelContainer() {
        return gameObject.getLevelContainer();
    }

    public boolean isAssertCollision() {
        return assertCollision;
    }

    public void setAssertCollision(boolean assertCollision) {
        this.assertCollision = assertCollision;
    }

    public static double getFpsTicks() {
        return fpsTicks;
    }

    public static void setFpsTicks(double fpsTicks) {
        Renderer.fpsTicks = fpsTicks;
    }

    public int getFps() {
        return fps;
    }

    public static int getRenPasses() {
        return renPasses;
    }

}
