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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.intrface.Command.Status;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.main.GameRenderer;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Console {

    public final ExecutorService commandExecutor = Executors.newCachedThreadPool();

    private static final Configuration cfg = Configuration.getInstance();
    private final Quad panel;
    private final StringBuilder input = new StringBuilder();
    private final DynamicText inText;
    private final IList<HistoryItem> history = new GapList<>();
    private boolean enabled = false;
    private final DynamicText completes;
    private int cursorPosition = 0; // Add cursor position (input text) variable

    public static final int HISTORY_CAPACITY = 120;
    protected GLFWCharCallback glfwCharCallback;
    protected GLFWKeyCallback glfwKeyCallback;
    protected GLFWScrollCallback glfwScrollCallback;

    public final Intrface intrface;

    protected boolean ctrlPressed = false;

    public Console(Intrface intrface) {
        this.intrface = intrface;
        int conwidth = cfg.getWidth();
        int conheight = cfg.getHeight() / 2;

        this.panel = new Quad(conwidth, conheight, intrface.gameObject.GameAssets.CONSOLE);
        this.panel.setColor(new Vector4f(LevelContainer.NIGHT_SKYBOX_COLOR_RGB, 0.75f));
        this.panel.setPos(new Vector2f(0.0f, 0.5f));
        this.panel.setIgnoreFactor(true);

        this.inText = new DynamicText(intrface.gameObject.GameAssets.FONT, "]_", new Vector2f(), 18, 18);
        this.inText.setColor(new Vector4f(GlobalColors.GREEN, 1.0f));
        this.inText.pos.x = -1.0f;
        this.inText.pos.y = 0.5f - panel.getPos().y + inText.getRelativeCharHeight(intrface);

        this.inText.setAlignment(Text.ALIGNMENT_LEFT);
        this.inText.alignToNextChar(intrface);

        this.completes = new DynamicText(intrface.gameObject.GameAssets.FONT, "", new Vector2f(), 18, 18);
        this.completes.color = new Vector4f(GlobalColors.YELLOW, 1.0f);
        this.completes.pos.x = -1.0f;
        this.completes.pos.y = -0.5f + panel.getPos().y - inText.getRelativeCharHeight(intrface);
        this.completes.alignToNextChar(intrface);
        init();
    }

    /**
     * Initializes console (with callbacks) - main thread
     */
    private void init() {
        glfwKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW.GLFW_KEY_LEFT_CONTROL) {
                    if (action == GLFW.GLFW_PRESS) {
                        ctrlPressed = true;
                    } else if (action == GLFW.GLFW_RELEASE) {
                        ctrlPressed = false;
                    }
                }

                if ((key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_GRAVE_ACCENT) && action == GLFW.GLFW_PRESS) {
                    GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                    GLFW.glfwSetCharCallback(window, null);
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                    GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                    GLFW.glfwSetScrollCallback(window, null);
                    inText.setContent("");
                    completes.setContent("");
                    input.setLength(0);
                    enabled = false;

                    if (Game.getCurrentMode() == Game.Mode.FREE) {
                        intrface.getGuideText().setContent("Press 'ESC' to open Main Menu\nor press '~' to open Console");
                        intrface.getGuideText().setEnabled(true);
                    } else {
                        intrface.getGuideText().setEnabled(false);
                    }

                } else if (key == GLFW.GLFW_KEY_BACKSPACE && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    if (cursorPosition > 0) {
                        input.deleteCharAt(cursorPosition - 1);
                        cursorPosition--;
                        updateInputText();
                    }
                } else if (key == GLFW.GLFW_KEY_ENTER && action == GLFW.GLFW_PRESS) {
                    if (!input.toString().equals("")) {
                        Command cmd = Command.getCommand(input.toString());

                        if (cmd.target != Command.Target.CLEAR) {
                            // add to queue
                            HistoryItem item;
                            try {
                                item = new HistoryItem(Console.this, cmd);
                                history.addFirst(item);
                            } catch (Exception ex) {
                                DSLogger.reportError("Unable to create console line! =>" + ex.getMessage(), ex);
                            }

                            // shift them
                            history.forEach(hi -> {
                                hi.buildCmdText();
                                hi.cmdText.pos.x = -1.0f;
                                hi.cmdText.pos.y += ((inText.getRelativeHeight(intrface) + inText.getRelativeCharHeight(intrface)) * inText.scale + (hi.cmdText.getRelativeCharHeight(intrface) + hi.cmdText.getRelativeHeight(intrface)) * hi.cmdText.scale) * Text.LINE_SPACING;
                                hi.cmdText.alignToNextChar(intrface);

                                hi.quad.pos.x = hi.cmdText.pos.x + (hi.cmdText.getRelativeWidth(intrface) + hi.cmdText.getRelativeCharWidth(intrface)) * hi.cmdText.scale;
                                hi.quad.pos.y = hi.cmdText.pos.y;
                            });

                            synchronized (GameObject.UPDATE_RENDER_IFC_MUTEX) { // using commands are known to crash the game => missing element in foreach loop
                                if (cmd.isRendererCommand()) {
                                    FutureTask<Object> consoleTask = new FutureTask<>(cmd);
                                    cmd.status = Command.Status.PENDING;
                                    GameRenderer.TASK_QUEUE.add(consoleTask);
                                } else if (cmd.isGameCommand()) {
                                    CompletableFuture.runAsync(() -> {
                                        Command.execute(intrface.gameObject, cmd);
                                    }, commandExecutor);
                                    cmd.status = Command.Status.PENDING;
                                }
                            }

                            // if over capacity deuque last
                            if (history.size() > HISTORY_CAPACITY) {
                                history.removeLast();
                            }
                        }

                        cursorPosition = 0;
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
                        
                        // position cursor in the end when only one candidate
                        cursorPosition = input.length();
                    }
                } else if (ctrlPressed && key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_PRESS) {
                    GLFW.glfwSetClipboardString(intrface.gameObject.WINDOW.getWindowID(), input);
                } else if (ctrlPressed && key == GLFW.GLFW_KEY_V && action == GLFW.GLFW_PRESS) {
                    final String clipboard = GLFW.glfwGetClipboardString(intrface.gameObject.WINDOW.getWindowID());
                    if (clipboard != null && !clipboard.isEmpty()) {
                        input.insert(cursorPosition, clipboard);
                        cursorPosition += clipboard.length();
                        updateInputText();
                    } // Handle arrow keys
                } else if (key == GLFW.GLFW_KEY_LEFT && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    if (cursorPosition > 0) {
                        cursorPosition--;
                        updateInputText();
                    }
                } else if (key == GLFW.GLFW_KEY_RIGHT && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    if (cursorPosition < input.length()) {
                        cursorPosition++;
                        updateInputText();
                    }
                }
            }
        };

        glfwCharCallback = new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                if (cursorPosition != 0 && input.length() == 0) {
                    return; // safe string insertion
                }
                input.insert(cursorPosition, (char) codepoint);
                cursorPosition++;
                updateInputText();
            }
        };

        glfwScrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                float scrollAmount = (float) yoffset * 0.1f; // Adjust scroll speed here

                for (HistoryItem hi : history) {
                    hi.cmdText.pos.y += scrollAmount;
                    hi.cmdText.enabled = (hi.cmdText.pos.y >= -1.0f && hi.cmdText.pos.y <= 1.0f);
                    hi.quad.pos.y = hi.cmdText.pos.y;
                    hi.quad.enabled = hi.cmdText.enabled;
                }
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

            GLFW.glfwSetInputMode(intrface.gameObject.WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            GLFW.glfwSetCursorPosCallback(intrface.gameObject.WINDOW.getWindowID(), null);
            GLFW.glfwSetKeyCallback(intrface.gameObject.WINDOW.getWindowID(), glfwKeyCallback);
            GLFW.glfwWaitEvents();
            GLFW.glfwSetCharCallback(intrface.gameObject.WINDOW.getWindowID(), glfwCharCallback);
            GLFW.glfwSetScrollCallback(intrface.gameObject.WINDOW.getWindowID(), glfwScrollCallback);
        }
    }

    /**
     * Write output to the Console.
     *
     * @param m message output
     */
    public void write(String m) {
        Command cmd = Command.getCommand(Command.Target.PRINT);
        cmd.mode = Command.Mode.SET;
        cmd.args.add(m);

        Command.execute(intrface.gameObject, cmd);
        synchronized (GameObject.UPDATE_RENDER_IFC_MUTEX) {
            // add to queue
            HistoryItem item;
            try {
                item = new HistoryItem(this, cmd);
                history.addFirst(item);
            } catch (Exception ex) {
                DSLogger.reportError("Unable to create console line! =>" + ex.getMessage(), ex);
            }

            // if over capacity deuque last
            if (history.size() > HISTORY_CAPACITY) {
                history.removeLast();
            }

            // shift them
            history.forEach(hi -> {
                hi.buildCmdText();
                hi.cmdText.pos.x = -1.0f;
                hi.cmdText.pos.y += ((inText.getRelativeHeight(intrface) + inText.getRelativeCharHeight(intrface)) * inText.scale + (hi.cmdText.getRelativeCharHeight(intrface) + hi.cmdText.getRelativeHeight(intrface)) * hi.cmdText.scale) * Text.LINE_SPACING;
                hi.cmdText.alignToNextChar(intrface);

                hi.quad.pos.x = hi.cmdText.pos.x + (hi.cmdText.getRelativeWidth(intrface) + hi.cmdText.getRelativeCharWidth(intrface)) * hi.cmdText.scale;
                hi.quad.pos.y = hi.cmdText.pos.y;
            });
        }
    }

    /**
     * Write output to the Console. If isError is true it will be logged as
     * error. In other case, if isError is false it will be logged as plain
     * message.
     *
     * @param m message output
     * @param status is message logged flag, light bulb color
     */
    public void write(String m, Status status) {
        Command cmd = Command.getCommand(Command.Target.PRINT);
        cmd.mode = Command.Mode.SET;
        cmd.args.add(m);

        Command.execute(intrface.gameObject, cmd);
        // Assign status to display color
        cmd.status = status;

        synchronized (GameObject.UPDATE_RENDER_IFC_MUTEX) {
            // add to queue
            HistoryItem item;
            try {
                item = new HistoryItem(this, cmd);
                history.addFirst(item);
            } catch (Exception ex) {
                DSLogger.reportError("Unable to create console line! =>" + ex.getMessage(), ex);
            }

            // if over capacity deuque last
            if (history.size() > HISTORY_CAPACITY) {
                history.removeLast();
            }
        }

        // shift them
        history.forEach(hi -> {
            hi.buildCmdText();
            hi.cmdText.pos.x = -1.0f;
            hi.cmdText.pos.y += ((inText.getRelativeHeight(intrface) + inText.getRelativeCharHeight(intrface)) * inText.scale + (hi.cmdText.getRelativeCharHeight(intrface) + hi.cmdText.getRelativeHeight(intrface)) * hi.cmdText.scale) * Text.LINE_SPACING;
            hi.cmdText.alignToNextChar(intrface);

            hi.quad.pos.x = hi.cmdText.pos.x + (hi.cmdText.getRelativeWidth(intrface) + hi.cmdText.getRelativeCharWidth(intrface)) * hi.cmdText.scale;
            hi.quad.pos.y = hi.cmdText.pos.y;
        });
    }

    /**
     * Render in the interface (has to be enabled)
     *
     * @param intrface intrface to render to
     * @param shaderProgram shader program to use
     * @param contourShaderProgram shader program to use
     * (contour-animation-through)
     */
    public void render(Intrface intrface, ShaderProgram shaderProgram, ShaderProgram contourShaderProgram) {
        if (enabled) {
            panel.setWidth(intrface.gameObject.WINDOW.getWidth());
            panel.setHeight(intrface.gameObject.WINDOW.getHeight() / 2);
            if (!panel.isBuffered()) {
                panel.bufferSmart(intrface);
            }
            panel.render(intrface, shaderProgram);
            inText.pos.x = -1.0f;
            inText.pos.y = 0.5f - panel.getPos().y + inText.getRelativeCharHeight(intrface);
            inText.alignToNextChar(intrface); // this changes both pos.x and pos.y for inText

            if (!inText.isBuffered()) {
                inText.bufferSmart(intrface);
            }
            inText.renderContour(intrface, contourShaderProgram);

            for (HistoryItem item : history.unmodifiableList()) {
                item.render(intrface, shaderProgram);
            }

            if (!completes.isBuffered()) {
                completes.bufferSmart(intrface);
            }
            completes.render(intrface, shaderProgram);
        }
    }

    /*
    * Releases all the callbacks by this component.
     */
    public void cleanUp() {
        this.glfwCharCallback.free();
        this.glfwKeyCallback.free();
        this.glfwScrollCallback.free();

        this.commandExecutor.shutdown();
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
     * @return status color {PENDING = WHITE, FAILED = RED, SUCCEEDED = GREEN,
     * WARNING = YELLOW}
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
            case WARNING:
                return new Vector4f(GlobalColors.YELLOW, 1.0f);
        }

    }

    /*
        * Method to update the displayed input text with the cursor at the correct position
     */
    private void updateInputText() {
        // Ensure cursorPosition is within the valid range
        if (cursorPosition < 0) {
            cursorPosition = 0;
        } else if (cursorPosition > input.length()) {
            cursorPosition = input.length();
        }

        // Create a copy of the input text to modify
        StringBuilder displayText = new StringBuilder(input);

        // Insert the cursor (underscore) at the current position
        displayText.insert(cursorPosition, '_');

        // Update the displayed text with a prefix and the modified input
        inText.setContent("]" + displayText.toString());
    }

    /*
    * Clear the history & intext
     */
    public void clear() {
        cursorPosition = 0;
        inText.setContent("");
        history.clear();
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

    public Intrface getIntrface() {
        return intrface;
    }

    public boolean isCtrlPressed() {
        return ctrlPressed;
    }

}
