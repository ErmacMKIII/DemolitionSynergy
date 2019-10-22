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
    private double lastTime = GLFW.glfwGetTime();
    private double currTime = 0.0;

    private double diff = 0.0; // shows current global game time

    private double upsDelta = 0.0; // for main thread, ups ticks
    private double fpsDelta = 0.0; // for renderer thread, fps ticks

    private final Object objMutex; // for mutual exclusion
    private final Object objUps; // for waking up the Game (Main) Thread
    private final Object objFps; // for waking up the Renderer Thread    

    public GameTime(Window myWindow, Object objMutex, Object objUps, Object objFps) {
        super("GameTime");
        this.myWindow = myWindow;
        this.objMutex = objMutex;
        this.objUps = objUps;
        this.objFps = objFps;
    }

    @Override
    public void run() {
        while (!GLFW.glfwWindowShouldClose(myWindow.getWindowID())) {
            currTime = GLFW.glfwGetTime();
            diff = currTime - lastTime;
            upsDelta += diff * Game.getUpsCap(); // default upsCap=80                
            if (upsDelta >= 1.0) {
                upsDelta--;
                synchronized (objUps) {
                    objUps.notify();
                }
            }
            fpsDelta += diff * Game.getFpsMax(); // default fpsMax=100            
            if (fpsDelta >= 1.0 && upsDelta < 1.0) { // this upsDelta less than one prevents lag                    
                fpsDelta--;
                synchronized (objFps) {
                    objFps.notify();
                }
            }
            lastTime = currTime;
        }

        // last time for everyone to wake up before finish
        synchronized (objUps) {
            objUps.notify();
        }
        synchronized (objFps) {
            objFps.notify();
        }
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public double getLastTime() {
        return lastTime;
    }

    public double getCurrTime() {
        return currTime;
    }

    public double getDiff() {
        return diff;
    }

    public double getUpsDelta() {
        return upsDelta;
    }

    public double getFpsDelta() {
        return fpsDelta;
    }

    public Object getObjMutex() {
        return objMutex;
    }

    public Object getObjUps() {
        return objUps;
    }

    public Object getObjFps() {
        return objFps;
    }

}
