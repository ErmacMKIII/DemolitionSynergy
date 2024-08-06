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
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 * Standard YES/NO dialog from command line interface (CLI). Execution takes
 * place in the caller thread.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public abstract class Dialog {

    public static enum ExecStatus {
        NOT_RUNNING, IN_PROGRESS, SUCCESS, FAILURE
    }

    protected ExecStatus execStatus = ExecStatus.NOT_RUNNING;

    protected final DynamicText dialog;
    protected final StringBuilder input = new StringBuilder(); // this is the answer we type from keyboard
    protected boolean enabled;
    protected boolean done;

    protected final String question; // question message
    protected final String success; // message if succesful execution
    protected final String fail; // message if failure

    protected final GLFWCharCallback charCallback;
    protected final GLFWKeyCallback keyCallback;

    public Dialog(Texture texture, Vector2f pos,
            String question, String success, String fail) throws Exception {
        this.dialog = new DynamicText(texture, "");
        this.dialog.setPos(pos);
        this.enabled = false;
        this.done = false;
        this.question = question;
        this.success = success;
        this.fail = fail;

        charCallback = new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                input.append((char) codepoint);
                dialog.setContent(question + input + "_");
            }
        };

        keyCallback = new GLFWKeyCallback() {
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
                    onCommand();
                    // pls use getter for done and setter for enabled outside
                    // using timer to determine when to stop showing dialog 
                    // to set enabled to false
                }
            }
        };
    }

    protected abstract ExecStatus execute(String arg); // we need to override this upon creation of the dialog  

    /*
     * What happens on command
     */
    protected void onCommand() {// what is happening internally on command
        execStatus = ExecStatus.NOT_RUNNING;
        if (!input.toString().equals("")) {
            execStatus = execute(input.toString());
            if (execStatus == ExecStatus.SUCCESS) {
                dialog.setContent(success);
                dialog.color = GlobalColors.GREEN_RGBA;
            } else if (execStatus == ExecStatus.FAILURE) {
                dialog.setContent(fail);
                dialog.color = GlobalColors.RED_RGBA;
            }
        } else {
            dialog.setContent("");
            enabled = false;
        }
        input.setLength(0);
        done = true;
    }

    /**
     * Displays dialog on the screeen. When opened callbacks are set accordingly
     * (mouse & keyboard input). Enabled is set to true for rendering.
     *
     * @param intrface underlaying interface
     */
    public void open(Intrface intrface) {
        if (input.length() == 0) {
            enabled = true;
            done = false;
            dialog.setContent(question + "_");
            dialog.color = GlobalColors.WHITE_RGBA;
            GLFW.glfwSetInputMode(intrface.gameObject.WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            GLFW.glfwSetCursorPosCallback(intrface.gameObject.WINDOW.getWindowID(), null);
            GLFW.glfwSetKeyCallback(intrface.gameObject.WINDOW.getWindowID(), keyCallback);
            GLFW.glfwWaitEvents();
            GLFW.glfwSetCharCallback(intrface.gameObject.WINDOW.getWindowID(), charCallback);
        }
    }

    /**
     * Renders dialog. Rendering takes part in the game interface.
     *
     * @param intrface intrface
     * @param shaderProgram shader program to use.
     */
    public void render(Intrface intrface, ShaderProgram shaderProgram) {
        if (enabled) {
            if (!dialog.isBuffered()) {
                dialog.bufferSmart(intrface);
            }
            dialog.render(intrface, shaderProgram);
        }
    }

    /**
     * *
     * Cleans up callbacks from this component.
     */
    public void cleanUp() {
        charCallback.free();
        keyCallback.free();
    }

    /**
     * Release this GL component by deleting GL Buffers.
     */
    public void release() {
        dialog.release();
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
