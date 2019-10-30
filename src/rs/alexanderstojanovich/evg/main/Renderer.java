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
import org.lwjgl.opengl.GL;
import rs.alexanderstojanovich.evg.core.LevelRenderer;
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.WaterRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.intrface.Intrface;

/**
 *
 * @author Coa
 */
public class Renderer extends Thread {

    private final Window myWindow;
    private final MasterRenderer masterRenderer;
    private final LevelRenderer levelRenderer;
    private final WaterRenderer waterRenderer;
    private final Intrface intrface;

    private final Object objMutex; // got from the Game    

    private boolean assertCollision = false;

    private static double fpsTicks = 0.0;

    public Renderer(Window myWindow, Object objMutex) {
        super("Renderer");
        this.myWindow = myWindow;
        this.objMutex = objMutex;
        masterRenderer = new MasterRenderer(myWindow);
        levelRenderer = new LevelRenderer(myWindow);
        waterRenderer = new WaterRenderer(myWindow, levelRenderer);
        intrface = new Intrface(myWindow, levelRenderer, waterRenderer, objMutex);
    }

    @Override
    public void run() {

        long timer0 = System.currentTimeMillis();
        long timer1 = System.currentTimeMillis();
        long timer2 = System.currentTimeMillis();

        int fps = 0;

        double lastTime = GLFW.glfwGetTime();
        double currTime;
        double diff;

        while (!GLFW.glfwWindowShouldClose(myWindow.getWindowID())) {
            synchronized (objMutex) {
                myWindow.loadContext();
                GL.setCapabilities(MasterRenderer.getGlCaps());

                currTime = GLFW.glfwGetTime();
                diff = currTime - lastTime;
                fpsTicks += diff * Game.getFpsMax();
                lastTime = currTime;

                if (fpsTicks >= 1.0 && Game.getUpsTicks() < 1.0) {
                    masterRenderer.render();

                    if (!levelRenderer.isWorking()) {
                        levelRenderer.render();
                        if (Game.isWaterEffects()) {
                            waterRenderer.render();
                        }
                    } else {
                        intrface.getProgText().setContent("Loading progress: " + levelRenderer.getProgress() + "%");
                        if (!intrface.getProgText().isBuffered()) {
                            intrface.getProgText().buffer();
                        }
                        intrface.getProgText().render();
                    }

                    intrface.setCollText(assertCollision);

                    intrface.render();
                    myWindow.render();
                    fps++;
                    fpsTicks--;
                }

                if (System.currentTimeMillis() > timer0 + 1000) {
                    intrface.getInfoText().getQuad().getColor().x = 0.0f;
                    intrface.getInfoText().getQuad().getColor().y = 1.0f;
                    intrface.getInfoText().getQuad().getColor().z = 0.0f;
                    intrface.getInfoText().setContent("ups: " + Game.getUps() + " | fps: " + fps);
                    fps = 0;
                    timer0 += 1000;
                }

                if (System.currentTimeMillis() > timer1 + 5000) {
                    if (intrface.getCommandDialog().isDone()) {
                        intrface.getCommandDialog().setEnabled(false);
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
                    timer1 += 5000;
                }

                if (System.currentTimeMillis() > timer2 + 250) {

                    if (levelRenderer.getProgress() == 100) {
                        intrface.getProgText().setEnabled(false);
                        levelRenderer.setProgress(0);
                    }

                    if (levelRenderer.getProgress() == 0) {
                        levelRenderer.animate();
                    }

                    timer2 += 250;
                }

                GL.setCapabilities(null);
                Window.unloadContext();
            }
        }

    }

    public Window getMyWindow() {
        return myWindow;
    }

    public MasterRenderer getMasterRenderer() {
        return masterRenderer;
    }

    public LevelRenderer getLevelRenderer() {
        return levelRenderer;
    }

    public WaterRenderer getWaterRenderer() {
        return waterRenderer;
    }

    public Intrface getIntrface() {
        return intrface;
    }

    public Object getObjMutex() {
        return objMutex;
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

}
