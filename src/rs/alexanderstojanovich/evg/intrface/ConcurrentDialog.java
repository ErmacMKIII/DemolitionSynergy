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
package rs.alexanderstojanovich.evg.intrface;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public abstract class ConcurrentDialog extends Dialog { // execution is done in another thread                

    private final Runnable command = new Runnable() { // executable command (calls execute method)
        @Override
        public void run() {
            boolean ok = execute(input.toString());
            if (ok) {
                dialog.setContent(success);
                dialog.color = Vector3fColors.GREEN;
            } else {
                dialog.setContent(fail);
                dialog.color = Vector3fColors.RED;
            }
            input.setLength(0);
            done = true;
        }
    };

    private Thread dialogThread; // thread which executes command     

    public ConcurrentDialog(Texture texture, Vector2f pos, String question, String success, String fail) {
        super(texture, pos, question, success, fail);
    }

    @Override
    protected abstract boolean execute(String command); // we need to override this upon creation of the dialog     

    @Override
    public void open() {
        if (input.length() == 0) {
            enabled = true;
            done = false;
            dialog.setContent(question + "_");
            dialog.color = Vector3fColors.WHITE;
            GLFW.glfwSetInputMode(GameObject.MY_WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            GLFW.glfwSetCursorPosCallback(GameObject.MY_WINDOW.getWindowID(), null);
            GLFW.glfwSetKeyCallback(GameObject.MY_WINDOW.getWindowID(), new GLFWKeyCallback() {
                @Override
                public void invoke(long window, int key, int scancode, int action, int mods) {
                    if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                        GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                        GLFW.glfwSetCharCallback(window, null);
                        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                        GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                        dialog.setContent("");
                        input.setLength(0);
                        enabled = false;
                        done = true;
                    } else if (key == GLFW.GLFW_KEY_BACKSPACE && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                        if (input.length() > 0) {
                            input.deleteCharAt(input.length() - 1);
                            dialog.setContent(question + input + "_");
                        }
                    } else if (key == GLFW.GLFW_KEY_ENTER && action == GLFW.GLFW_PRESS) {
                        GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                        GLFW.glfwSetCharCallback(window, null);
                        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                        GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                        if (!input.toString().equals("")) {
                            dialogThread = new Thread(command, "Concurrent Dialog Thread");
                            dialogThread.start();
                        } else {
                            dialog.setContent("");
                            enabled = false;
                            done = true;
                        }
                        // pls use getter for done and setter for enabled outside
                        // using timer to determine when to stop showing dialog 
                        // to set enabled to false
                    }
                }
            });
            GLFW.glfwWaitEvents();
            GLFW.glfwSetCharCallback(GameObject.MY_WINDOW.getWindowID(), new GLFWCharCallback() {
                @Override
                public void invoke(long window, int codepoint) {
                    input.append((char) codepoint);
                    dialog.setContent(question + input + "_");
                }
            });
        }
    }

    public Runnable getCommand() {
        return command;
    }

    public Thread getDialogThread() {
        return dialogThread;
    }

}
