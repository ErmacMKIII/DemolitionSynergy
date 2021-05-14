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
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public abstract class Dialog {

    protected final DynamicText dialog;
    protected final StringBuilder input = new StringBuilder(); // this is the answer we type from keyboard
    protected boolean enabled;
    protected boolean done;

    protected final String question; // question message
    protected final String success; // message if succesful execution
    protected final String fail; // message if failure

    public Dialog(Texture texture, Vector2f pos,
            String question, String success, String fail) {
        this.dialog = new DynamicText(texture, "");
        this.dialog.setPos(pos);
        this.enabled = false;
        this.done = false;
        this.question = question;
        this.success = success;
        this.fail = fail;
    }

    protected abstract boolean execute(String command); // we need to override this upon creation of the dialog        

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
                            boolean execStatus = execute(input.toString());
                            if (execStatus) {
                                dialog.setContent(success);
                                dialog.color = Vector3fColors.GREEN;
                            } else {
                                dialog.setContent(fail);
                                dialog.color = Vector3fColors.RED;
                            }
                        } else {
                            dialog.setContent("");
                            enabled = false;
                        }
                        input.setLength(0);
                        done = true;
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

    public void render(ShaderProgram shaderProgram) {
        if (enabled) {
            if (!dialog.isBuffered()) {
                dialog.bufferAll();
            }
            dialog.render(shaderProgram);
        }
    }

    public Window getMyWindow() {
        return GameObject.MY_WINDOW;
    }

    public DynamicText getDialog() {
        return dialog;
    }

    public StringBuilder getInput() {
        return input;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getQuestion() {
        return question;
    }

    public String getSuccess() {
        return success;
    }

    public String getFail() {
        return fail;
    }

}
