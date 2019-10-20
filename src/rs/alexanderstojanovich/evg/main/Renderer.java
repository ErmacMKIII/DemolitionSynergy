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

    public static final Object OBJ_FPS = new Object();

    public Renderer(Window myWindow) {
        super("Renderer");
        this.myWindow = myWindow;
        masterRenderer = new MasterRenderer(myWindow);
        levelRenderer = new LevelRenderer(myWindow);
        waterRenderer = new WaterRenderer(myWindow, levelRenderer);
        intrface = new Intrface(myWindow, levelRenderer, waterRenderer);
        PerspectiveRenderer.updatePerspective(myWindow.getWidth(), myWindow.getHeight(), ShaderProgram.getMainShader());
    }

    @Override
    public void run() {

        long timer0 = System.currentTimeMillis();
        long timer1 = System.currentTimeMillis();
        long timer2 = System.currentTimeMillis();

        int fps = 0;
        while (!GLFW.glfwWindowShouldClose(myWindow.getWindowID())) {
            synchronized (OBJ_FPS) {
                try {
                    OBJ_FPS.wait();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Renderer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            synchronized (Game.OBJ_MUTEX) {
                myWindow.loadContext();
                GL.setCapabilities(MasterRenderer.getGlCaps());

                if (GameTime.getFpsDelta() >= 1.0) { // ensurance that this will go 100*diff -> 100 times per second 
                    masterRenderer.render();
                    levelRenderer.render(ShaderProgram.getMainShader());
                    if (Game.isWaterEffects()) {
                        waterRenderer.render();
                    }
                    intrface.render();
                    myWindow.render();
                    fps++;
                    GameTime.decFpsDelta();
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
                    timer1 += 5000;
                }

                if (System.currentTimeMillis() > timer2 + 250) {
//                    levelRenderer.animate();
                    levelRenderer.getFluidBlocks().animate();
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

}
