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
package rs.alexanderstojanovich.evg.intrface;

import org.joml.Vector2f;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.Game;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import rs.alexanderstojanovich.evg.core.Texture;

/**
 *
 * @author Coa
 */
public abstract class Dialog {

    private Window myWindow;
    private Text dialog;
    private StringBuilder input; // this is the answer we type from keyboard
    private boolean enabled;
    private boolean done;

    public Dialog(Window window, Texture texture, Vector2f pos) {
        this.myWindow = window;
        this.dialog = new Text(myWindow, texture, "");
        this.dialog.setPos(pos);
        this.enabled = false;
        this.done = false;
    }

    protected abstract boolean execute(String command); // we need to override this upon creation of the dialog     

    public void open(String question, String success, String fail) {
        if (input == null) {
            enabled = true;
            done = false;
            input = new StringBuilder();
            dialog.setContent(question + "_");
            dialog.getColor().x = 1.0f;
            dialog.getColor().y = 1.0f;
            dialog.getColor().z = 1.0f;
            GLFW.glfwSetInputMode(myWindow.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            GLFW.glfwSetCursorPosCallback(myWindow.getWindowID(), null);
            GLFW.glfwSetKeyCallback(myWindow.getWindowID(), new GLFWKeyCallback() {
                @Override
                public void invoke(long window, int key, int scancode, int action, int mods) {
                    if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                        GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                        GLFW.glfwSetCharCallback(window, null);
                        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                        GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                        dialog.setContent("");
                        input = null;
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
                                dialog.getColor().x = 0.0f;
                                dialog.getColor().y = 1.0f;
                                dialog.getColor().z = 0.0f;
                            } else {
                                dialog.setContent(fail);
                                dialog.getColor().x = 1.0f;
                                dialog.getColor().y = 0.0f;
                                dialog.getColor().z = 0.0f;
                            }
                        } else {
                            dialog.setContent("");
                            enabled = false;
                        }
                        input = null;
                        done = true;
                        // pls use getter for done and setter for enabled outside
                        // using timer to determine when to stop showing dialog 
                        // to set enabled to false
                    }
                }
            });
            GLFW.glfwWaitEvents();
            GLFW.glfwSetCharCallback(myWindow.getWindowID(), new GLFWCharCallback() {
                @Override
                public void invoke(long window, int codepoint) {
                    input.append((char) codepoint);
                    dialog.setContent(question + input + "_");
                }
            });
        }
    }

    public void render() {
        if (enabled) {
            dialog.render();
        }
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public void setMyWindow(Window myWindow) {
        this.myWindow = myWindow;
    }

    public Text getDialog() {
        return dialog;
    }

    public void setDialog(Text dialog) {
        this.dialog = dialog;
    }

    public StringBuilder getInput() {
        return input;
    }

    public void setInput(StringBuilder input) {
        this.input = input;
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

}
