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
import rs.alexanderstojanovich.evg.core.Window;

/**
 *
 * @author Coa
 */
public class GameTime extends Thread { // serves for measuring the time amongs the threads

    private final Window myWindow;
    private static double lastTime = GLFW.glfwGetTime();
    private static double currTime = 0.0;

    private static double diff = 0.0; // shows current global game time

    private static double upsDelta = 0; // for main thread
    private static double fpsDelta = 0; // for renderer thread

    public GameTime(Window myWindow) {
        super("GameTime");
        this.myWindow = myWindow;
    }

    @Override
    public void run() {
        while (!GLFW.glfwWindowShouldClose(myWindow.getWindowID())) {
            synchronized (Game.OBJ_MUTEX) {
                currTime = GLFW.glfwGetTime();
                diff = currTime - lastTime;
                upsDelta += diff * Game.getUpsCap(); // default upsCap=80
                if (upsDelta >= 1.0) {
                    synchronized (Game.OBJ_UPS) {
                        Game.OBJ_UPS.notify();
                    }
                }
                fpsDelta += diff * Game.getFpsMax(); // default fpsMax=100            
                if (fpsDelta >= 1.0) {
                    synchronized (Renderer.OBJ_FPS) {
                        Renderer.OBJ_FPS.notify();
                    }
                }
                lastTime = currTime;
            }
        }
        // last time for everyone to wake up before finish
        synchronized (Game.OBJ_UPS) {
            Game.OBJ_UPS.notify();
        }
        synchronized (Renderer.OBJ_FPS) {
            Renderer.OBJ_FPS.notify();
        }
    }

    public static void decUpsDelta() {
        upsDelta--;
    }

    public static void decFpsDelta() {
        fpsDelta--;
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public static double getUpsDelta() {
        return upsDelta;
    }

    public static double getFpsDelta() {
        return fpsDelta;
    }

}
