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
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.audio.AudioFile;
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
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
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

        this.panel = new Quad(conwidth, conheight, intrface.gameObject.GameAssets.CONSOLE, intrface);
        this.panel.setColor(new Vector4f(LevelContainer.NIGHT_SKYBOX_COLOR_RGB, 0.75f));
        this.panel.setPos(new Vector2f(0.0f, 0.5f));
        this.panel.setIgnoreFactor(true);

        this.inText = new DynamicText(intrface.gameObject.GameAssets.FONT, "]_", new Vector2f(), 18, 18, this.intrface);
        this.inText.setColor(new Vector4f(GlobalColors.GREEN, 1.0f));
        this.inText.pos.x = -1.0f;
        this.inText.pos.y = 0.5f - panel.getPos().y + inText.getRelativeCharHeight(intrface);

        this.inText.setAlignment(Text.ALIGNMENT_LEFT);
        this.inText.alignToNextChar(intrface);

        this.completes = new DynamicText(intrface.gameObject.GameAssets.FONT, "", new Vector2f(), 18, 18, this.intrface);
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
                if (!enabled) {
                    return; // Ignore input when console is disabled
                }

                if (key == GLFW.GLFW_KEY_LEFT_CONTROL) {
                    if (action == GLFW.GLFW_RELEASE) {
                        ctrlPressed = true;
                    } else if (action == GLFW.GLFW_RELEASE) {
                        ctrlPressed = false;
                    }
                }

                if ((key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_GRAVE_ACCENT)) {
                    if (action == GLFW.GLFW_PRESS) {
                        // Toggle console state on key press
                        enabled = !enabled;

                        if (enabled) {
                            open(); // Reinitialize console callbacks and rendering
                        } else {
                            // use 'this' to avoid accidentally close the resource
                            Console.this.close(); // Reset callbacks and disable console rendering
                        }
                    }
                } else if (key == GLFW.GLFW_KEY_BACKSPACE && (action == GLFW.GLFW_RELEASE || action == GLFW.GLFW_REPEAT)) {
                    if (cursorPosition > 0) {
                        input.deleteCharAt(cursorPosition - 1);
                        cursorPosition--;
                        updateInputText();
                    }
                } else if (key == GLFW.GLFW_KEY_ENTER && action == GLFW.GLFW_RELEASE) {
                    if (!input.toString().equals("")) {
                        Command cmd = Command.getCommand(input.toString());

                        if (cmd.target != Command.Target.CLEAR) {
                            // add to queue
                            HistoryItem item;
                            try {
                                item = new HistoryItem(Console.this, cmd);
                                item.buildCmdText();

                                history.addFirst(item);
                            } catch (Exception ex) {
                                DSLogger.reportError("Unable to create console line! =>" + ex.getMessage(), ex);
                            }

                            // Position all messages
                            positionMessages();

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

                            // Maintain history capacity
                            if (history.size() > HISTORY_CAPACITY) {
                                history.removeLast();
                                positionMessages(); // Reposition after removal
                            }
                        }

                        cursorPosition = 0;
                        input.setLength(0);
                        inText.setContent("]_");
                    }
                } else if (key == GLFW.GLFW_KEY_TAB && action == GLFW.GLFW_RELEASE) {
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
                } else if (ctrlPressed && key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_RELEASE) {
                    GLFW.glfwSetClipboardString(intrface.gameObject.WINDOW.getWindowID(), input);
                } else if (ctrlPressed && key == GLFW.GLFW_KEY_V && action == GLFW.GLFW_RELEASE) {
                    String clipboard = GLFW.glfwGetClipboardString(intrface.gameObject.WINDOW.getWindowID());
                    clipboard = (clipboard.length() <= 256) ? clipboard : clipboard.substring(0, 256);

                    if (clipboard != null && !clipboard.isEmpty()) {
                        input.insert(cursorPosition, clipboard);
                        cursorPosition += clipboard.length();
                        updateInputText();
                        intrface.gameObject.getSoundFXPlayer().play(AudioFile.BLOCK_SELECT, new Vector3f());
                    } // Handle arrow keys
                } else if (key == GLFW.GLFW_KEY_LEFT && (action == GLFW.GLFW_RELEASE || action == GLFW.GLFW_REPEAT)) {
                    if (cursorPosition > 0) {
                        cursorPosition--;
                        updateInputText();
                    }
                } else if (key == GLFW.GLFW_KEY_RIGHT && (action == GLFW.GLFW_RELEASE || action == GLFW.GLFW_REPEAT)) {
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
                if (!enabled || codepoint == GLFW.GLFW_KEY_GRAVE_ACCENT) {
                    return; // Ignore input when console is disabled
                }
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
                if (!enabled) {
                    return; // Ignore input when console is disabled
                }
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
     * Opens the console, setting up callbacks and enabling rendering.
     */
    public void open() {
        if (input.length() == 0) {
            inText.setContent("]_");

            // Enable the cursor for text input
            GLFW.glfwSetInputMode(intrface.gameObject.WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);

            // Disable the default cursor position callback while in console
            GLFW.glfwSetCursorPosCallback(intrface.gameObject.WINDOW.getWindowID(), null);

            // Set the custom key and character callbacks for the console
            GLFW.glfwSetKeyCallback(intrface.gameObject.WINDOW.getWindowID(), glfwKeyCallback);
            GLFW.glfwSetCharCallback(intrface.gameObject.WINDOW.getWindowID(), glfwCharCallback);

            // Optionally set scroll behavior for console
            GLFW.glfwSetScrollCallback(intrface.gameObject.WINDOW.getWindowID(), glfwScrollCallback);

            // Avoid blocking the application with glfwWaitEvents
            GLFW.glfwPostEmptyEvent(); // Notify GLFW to process events immediately

            enabled = true;
        }
    }

    /**
     * Closes the console by resetting callbacks and disabling rendering.
     */
    public void close() {
        GLFW.glfwSetKeyCallback(intrface.gameObject.WINDOW.getWindowID(), Game.getDefaultKeyCallback());
        GLFW.glfwSetCharCallback(intrface.gameObject.WINDOW.getWindowID(), null);
        GLFW.glfwSetInputMode(intrface.gameObject.WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPosCallback(intrface.gameObject.WINDOW.getWindowID(), Game.getDefaultCursorCallback());
        GLFW.glfwSetScrollCallback(intrface.gameObject.WINDOW.getWindowID(), null);

        inText.setContent("");
        completes.setContent("");
        input.setLength(0);
        cursorPosition = 0;
        enabled = false;

        // Update guide text depending on the game mode
        if (Game.getCurrentMode() == Game.Mode.FREE) {
            intrface.getGuideText().setContent("Press 'ESC' to open Main Menu\nor press '~' to open Console");
            intrface.getGuideText().setEnabled(true);
        } else {
            intrface.getGuideText().setEnabled(false);
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
        cmd.args.add(m); // default status

        Command.execute(intrface.gameObject, cmd);

        synchronized (GameObject.UPDATE_RENDER_IFC_MUTEX) {
            try {
                // Add new message to history
                HistoryItem item = new HistoryItem(this, cmd);
                history.addFirst(item);

                // Position all messages
                positionMessages();

                // Maintain history capacity
                if (history.size() > HISTORY_CAPACITY) {
                    history.removeLast();
                    positionMessages(); // Reposition after removal
                }
            } catch (Exception ex) {
                DSLogger.reportError("Unable to create console line! =>" + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Write output to the Console with status. If isError is true it will be
     * logged as error. In other case, if isError is false it will be logged as
     * plain message.
     *
     * @param m message output
     * @param status message status (determines display color)
     */
    public void write(String m, Status status) {
        Command cmd = Command.getCommand(Command.Target.PRINT);
        cmd.mode = Command.Mode.SET;
        cmd.args.add(m);
        cmd.status = status;

        Command.execute(intrface.gameObject, cmd);

        synchronized (GameObject.UPDATE_RENDER_IFC_MUTEX) {
            try {
                // Add new message to history
                HistoryItem item = new HistoryItem(this, cmd);
                history.addFirst(item);

                // Position all messages
                positionMessages();

                // Maintain history capacity
                if (history.size() > HISTORY_CAPACITY) {
                    history.removeLast();
                    positionMessages(); // Reposition after removal
                }
            } catch (Exception ex) {
                DSLogger.reportError("Unable to create console line! =>" + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Positions all messages in the console with proper vertical stacking
     */
    private void positionMessages() {
        float startY = inText.pos.y + inText.getRelativeHeight(intrface) * Text.LINE_SPACING; // Start below input line
        float spacing = inText.getRelativeHeight(intrface) * 2.0f * Text.LINE_SPACING; // Consistent spacing

        for (int i = 0; i < history.size(); i++) {
            HistoryItem hi = history.get(i);
            hi.buildCmdText();

            // Position text
            hi.cmdText.pos.x = -1.0f + hi.cmdText.getRelativeCharWidth(intrface) * hi.cmdText.scale;
            hi.cmdText.pos.y = startY + (i * spacing) + hi.cmdText.getRelativeHeight(intrface);
            hi.cmdText.alignToNextChar(intrface);

            // Position quad (background)
            hi.quad.pos.x = hi.cmdText.pos.x - hi.cmdText.getRelativeCharWidth(intrface) * hi.cmdText.scale;
            hi.quad.pos.y = hi.cmdText.pos.y;
        }
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
            // Update and render panel
            if (!panel.isBuffered()) {
                panel.bufferSmart(intrface);
            }
            panel.render(intrface, shaderProgram);

            // Render input text
            if (!inText.isBuffered()) {
                inText.bufferSmart(intrface);
            }
            inText.renderContour(intrface, contourShaderProgram);

            // Render history messages
            for (HistoryItem item : history) {
                item.render(intrface, shaderProgram);
            }

            // Render completions if needed
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
    * Method to updateEnvironment the displayed input text with the cursor at the correct position
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
