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

import org.lwjgl.glfw.GLFW;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Operations extends Thread {

    private final GameObject gameObject;

    public static final int OPS_MAX = 20;
    private static int ops = 0;
    private static double opsTicks = 0.0;
    private static final int OPS_MAX_PASSES = 5;
    private static int operPasses = 0;

    public Operations(GameObject gameObject) {
        super("Operations");
        this.gameObject = gameObject;
    }

    @Override
    public void run() {
        ops = 0;

        double lastTime = GLFW.glfwGetTime();
        double currTime;
        double diff;

        while (!GameObject.MY_WINDOW.shouldClose()) {
            currTime = GLFW.glfwGetTime();
            diff = currTime - lastTime;
            opsTicks += -Math.expm1(-diff * OPS_MAX);
            lastTime = currTime;

            // Detecting critical status
            if (ops == 0 && diff > Game.CRITICAL_TIME) {
                DSLogger.reportFatalError("Game status critical!", null);
                GameObject.MY_WINDOW.close();
                break;
            }

            while (opsTicks >= 1.0 && operPasses <= OPS_MAX_PASSES) {
                gameObject.chunkOperations();
                if (Game.isWaterEffects()) {
                    gameObject.getWaterRenderer().refresh();
                }
                ops++;
                opsTicks--;
                operPasses++;
            }
            operPasses = 0;

        }
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public static int getOps() {
        return ops;
    }

    public static void setOps(int ops) {
        Operations.ops = ops;
    }

    public static double getOpsTicks() {
        return opsTicks;
    }

}
