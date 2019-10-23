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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import rs.alexanderstojanovich.evg.core.Critter;
import rs.alexanderstojanovich.evg.core.LevelRenderer;
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.PerspectiveRenderer;
import rs.alexanderstojanovich.evg.core.WaterRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.intrface.Intrface;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

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

    private final Object objFps = new Object();
    private final Object objMutex; // got from the Game    

    public Renderer(Window myWindow, Object objMutex) {
        super("Renderer");
        this.myWindow = myWindow;
        this.objMutex = objMutex;
        masterRenderer = new MasterRenderer(myWindow);
        levelRenderer = new LevelRenderer(myWindow);
        waterRenderer = new WaterRenderer(myWindow, levelRenderer);
        intrface = new Intrface(myWindow, levelRenderer, waterRenderer, objMutex);
        PerspectiveRenderer.updatePerspective(myWindow.getWidth(), myWindow.getHeight(), ShaderProgram.getMainShader());
    }

    @Override
    public void run() {

        long timer0 = System.currentTimeMillis();
        long timer1 = System.currentTimeMillis();
        long timer2 = System.currentTimeMillis();

        int fps = 0;
        while (!GLFW.glfwWindowShouldClose(myWindow.getWindowID())) {
            synchronized (objFps) {
                try {
                    objFps.wait();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Renderer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            synchronized (objMutex) {
                myWindow.loadContext();
                GL.setCapabilities(MasterRenderer.getGlCaps());

                masterRenderer.render();
                if (!levelRenderer.isWorking()) {
                    levelRenderer.render(ShaderProgram.getMainShader());
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

                Critter obs = levelRenderer.getObserver();
                boolean bool = levelRenderer.hasCollisionWithCritter(obs);
                intrface.setCollText(bool);

                intrface.render();
                myWindow.render();
                fps++;

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
                        levelRenderer.getFluidBlocks().animate();
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

    public Object getObjFps() {
        return objFps;
    }

    public Object getObjMutex() {
        return objMutex;
    }

}
