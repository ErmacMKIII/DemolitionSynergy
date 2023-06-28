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

import java.util.List;
import java.util.concurrent.FutureTask;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.main.GameRenderer;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Console {

    private final Quad panel;
    private final StringBuilder input = new StringBuilder();
    private final DynamicText inText;
    private final IList<HistoryItem> history = new GapList<>();
    private boolean enabled = false;
    private final DynamicText completes;

    public static final int HISTORY_CAPACITY = 12;

    protected GLFWCharCallback glfwCharCallback;
    protected GLFWKeyCallback glfwKeyCallback;

    public Console() {
        this.panel = new Quad(GameObject.MY_WINDOW.getWidth(),
                GameObject.MY_WINDOW.getHeight() / 2, Texture.CONSOLE);
        this.panel.setColor(new Vector4f(LevelContainer.SKYBOX_COLOR_RGB, 0.75f));
        this.panel.setPos(new Vector2f(0.0f, 0.5f));
        this.panel.setIgnoreFactor(true);

        this.inText = new DynamicText(Texture.FONT, "]_", new Vector2f(), 18, 18);
        this.inText.setColor(new Vector4f(GlobalColors.GREEN, 1.0f));
        this.inText.pos.x = -1.0f;
        this.inText.pos.y = 0.5f - panel.getPos().y + inText.getRelativeCharHeight();

        this.inText.setAlignment(Text.ALIGNMENT_LEFT);
        this.inText.alignToNextChar();

        this.completes = new DynamicText(Texture.FONT, "", new Vector2f(), 18, 18);
        this.completes.color = new Vector4f(GlobalColors.YELLOW, 1.0f);
        this.completes.pos.x = -1.0f;
        this.completes.pos.y = -0.5f + panel.getPos().y - inText.getRelativeCharHeight();
        this.completes.alignToNextChar();
        init();
    }

    /**
     * Initializes console (with callbacks) - main thread
     */
    private void init() {
        glfwKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if ((key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_GRAVE_ACCENT) && action == GLFW.GLFW_PRESS) {
                    GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                    GLFW.glfwSetCharCallback(window, null);
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                    GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                    inText.setContent("");
                    completes.setContent("");
                    input.setLength(0);
                    enabled = false;
                } else if (key == GLFW.GLFW_KEY_BACKSPACE && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    if (input.length() > 0) {
                        input.deleteCharAt(input.length() - 1);
                        inText.setContent("]" + input + "_");
                    }
                } else if (key == GLFW.GLFW_KEY_ENTER && action == GLFW.GLFW_PRESS) {
                    if (!input.toString().equals("")) {
//                            for (DynamicText item : history) {
//                                item.pos.y += item.getRelativeCharHeight() * Text.LINE_SPACING;
//                            }                        
                        Command cmd = Command.getCommand(input.toString());
// if cmd is invalid it's null
                        synchronized (GameObject.UPDATE_MUTEX) { // using commands are known to crash the game => missing element in foreach loop
                            if (cmd.isRendererCommand()) {
                                FutureTask<Object> consoleTask = new FutureTask<>(cmd);
                                cmd.status = Command.Status.PENDING;
                                GameRenderer.TASK_QUEUE.add(consoleTask);
                            } else if (cmd.isGameCommand()) {
                                Command.execute(cmd);
                            }
                        }

                        if (cmd.target != Command.Target.CLEAR) {
                            // add to queue
                            HistoryItem item = new HistoryItem(cmd);
                            history.addFirst(item);

                            // shift them
                            history.forEach(hi -> {
                                hi.buildCmdText();
                                hi.cmdText.pos.x = -1.0f;
                                hi.cmdText.pos.y += ((inText.getRelativeHeight() + inText.getRelativeCharHeight()) * inText.scale + (hi.cmdText.getRelativeCharHeight() + hi.cmdText.getRelativeHeight()) * hi.cmdText.scale) * Text.LINE_SPACING;
                                hi.cmdText.alignToNextChar();

                                hi.quad.pos.x = hi.cmdText.pos.x + (hi.cmdText.getRelativeWidth() + hi.cmdText.getRelativeCharWidth()) * hi.cmdText.scale;
                                hi.quad.pos.y = hi.cmdText.pos.y;
                            });

                            // if over capacity deuque last
                            if (history.size() > HISTORY_CAPACITY) {
                                history.removeLast();
                            }
                        }

                        input.setLength(0);
                        inText.setContent("]_");
                    }
                } else if (key == GLFW.GLFW_KEY_TAB && action == GLFW.GLFW_PRESS) {
                    List<String> candidates = Command.autoComplete(input.toString());
                    StringBuilder sb = new StringBuilder();
                    int index = 0;
                    for (String candidate : candidates) {
                        sb.append(candidate);
                        if (index < candidates.size() - 1) {
                            sb.append("\n");
                        }
                    }
                    completes.setContent(sb.toString());
                    completes.setAlignment(Text.ALIGNMENT_LEFT);

                    if (candidates.size() == 1) {
                        input.setLength(0);
                        input.append(candidates.get(0));
                        inText.setContent("]" + input + "_");
                    }
                }
            }
        };

        glfwCharCallback = new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                input.append((char) codepoint);
                inText.setContent("]" + input + "_");
            }
        };
    }

    /**
     * When open callbacks are changed (take input from keyboard, mouse etc)
     * Enabled is set to true for rendering.
     */
    public void open() {
        if (input.length() == 0) {
            enabled = true;

            inText.setContent("]_");

            GLFW.glfwSetInputMode(GameObject.MY_WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            GLFW.glfwSetCursorPosCallback(GameObject.MY_WINDOW.getWindowID(), null);
            GLFW.glfwSetKeyCallback(GameObject.MY_WINDOW.getWindowID(), glfwKeyCallback);
            GLFW.glfwWaitEvents();
            GLFW.glfwSetCharCallback(GameObject.MY_WINDOW.getWindowID(), glfwCharCallback);
        }
    }

    /**
     * Render in the interface (has to be enabled)
     *
     * @param shaderProgram shader program to use
     */
    public void render(ShaderProgram shaderProgram) {
        if (enabled) {
            panel.setWidth(GameObject.MY_WINDOW.getWidth());
            panel.setHeight(GameObject.MY_WINDOW.getHeight() / 2);
            if (!panel.isBuffered()) {
                panel.bufferAll();
            }
            panel.render(shaderProgram);
            inText.pos.x = -1.0f;
            inText.pos.y = 0.5f - panel.getPos().y + inText.getRelativeCharHeight();
            inText.alignToNextChar(); // this changes both pos.x and pos.y for inText

            if (!inText.isBuffered()) {
                inText.bufferAll();
            }
            inText.render(shaderProgram);

            for (HistoryItem item : history) {
                item.render(shaderProgram);
            }

            if (!completes.isBuffered()) {
                completes.bufferAll();
            }
            completes.render(shaderProgram);
        }
    }

    /*
    * Releases all the callbacks by this component.
     */
    public void cleanUp() {
        this.glfwCharCallback.free();
        this.glfwKeyCallback.free();
    }

    /*
    * Delete all the GL Buffers.
     */
    public void release() {
        this.history.forEach(i -> i.release());
        this.inText.release();
    }

    /**
     * Mapping from command status to color
     *
     * @param status Command status
     * @return status color {PENDING = WHITE, FAILED = RED, SUCCEEDED = GREEN }
     */
    public static Vector4f StatusColor(Command.Status status) {
        switch (status) {
            case PENDING:
                return new Vector4f(GlobalColors.WHITE, 1.0f);
            default:
            case FAILED:
                return new Vector4f(GlobalColors.RED, 1.0f);
            case SUCCEEDED:
                return new Vector4f(GlobalColors.GREEN, 1.0f);
        }

    }

    /*
    * Clear the history & intext
     */
    public void clear() {
        inText.setContent("");
        history.clear();
    }

    public Window getMyWindow() {
        return GameObject.MY_WINDOW;
    }

    public Quad getPanel() {
        return panel;
    }

    public StringBuilder getInput() {
        return input;
    }

    public DynamicText getInText() {
        return inText;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public DynamicText getCompletes() {
        return completes;
    }

    public List<HistoryItem> getHistory() {
        return history;
    }

}
