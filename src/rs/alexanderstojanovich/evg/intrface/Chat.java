/*
 * Copyright (C) 2025 coas9
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
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 * Chat system. Similar to chat (which was first implemented).
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Chat {

    private static final Configuration cfg = Configuration.getInstance();
    private final Quad panel;
    private final StringBuilder input = new StringBuilder();
    private final DynamicText inText;
    private final IList<DynamicText> history = new GapList<>();
    private boolean enabled = false;
    private int cursorPosition = 0; // Add cursor position (input text) variable

    public static final int HISTORY_CAPACITY = 8;
    public final Intrface intrface;

    protected GLFWCharCallback glfwCharCallback;
    protected GLFWKeyCallback glfwKeyCallback;
    protected boolean ctrlPressed = false;

    public Chat(Intrface intrface) {
        this.intrface = intrface;

        int pnlwidth = cfg.getWidth() / 2;
        int pnlheight = cfg.getHeight() / 2;

        this.panel = new Quad(pnlwidth, pnlheight, Texture.EMPTY, intrface);
        this.panel.setColor(new Vector4f(GlobalColors.WHITE, 0.75f));
        this.panel.setPos(new Vector2f(0.0f, 0.5f));
        this.panel.setIgnoreFactor(true);

        this.inText = new DynamicText(intrface.gameObject.GameAssets.FONT, "_", new Vector2f(), 18, 18, this.intrface);
        this.inText.setColor(new Vector4f(GlobalColors.GREEN, 1.0f));
        this.inText.pos.x = -1.0f;
        this.inText.pos.y = 0.5f - panel.getPos().y + inText.getRelativeCharHeight(intrface);

        this.inText.setAlignment(Text.ALIGNMENT_LEFT);
        this.inText.alignToNextChar(intrface);
        init();
    }

    /**
     * Initializes chat (with callbacks) - main thread
     */
    private void init() {
        glfwKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (!enabled) {
                    return; // Ignore input when chat is disabled
                }

                if (key == GLFW.GLFW_KEY_ESCAPE) {
                    if (action == GLFW.GLFW_PRESS) {
                        // Toggle chat state on key press
                        enabled = !enabled;

                        if (enabled) {
                            open(); // Reinitialize chat callbacks and rendering
                        } else {
                            // use 'this' to avoid accidentally close the resource
                            Chat.this.close(); // Reset callbacks and disable chat rendering
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
                        String content = input.toString();

                        // add to queue
                        DynamicText item;
                        try {
                            item = new DynamicText(intrface.gameObject.GameAssets.FONT, intrface.gameObject.levelContainer.levelActors.player.getName() + ":" + content, new Vector2f(inText.pos), 18, 18, intrface);
                            history.addFirst(item);
                        } catch (Exception ex) {
                            DSLogger.reportError("Unable to create chat line! =>" + ex.getMessage(), ex);
                        }

                        positionMessages();

                        // if over capacity deuque last
                        if (history.size() > HISTORY_CAPACITY) {
                            history.removeLast();
                            positionMessages();
                        }

                        cursorPosition = 0;
                        input.setLength(0);
                        inText.setContent("_");

                        // Multiplayer chat
                        Command cmd = Command.getCommand(Command.Target.SAY);
                        cmd.mode = Command.Mode.SET;
                        cmd.args.add(content);
                        cmd.status = Command.Status.PENDING;
                        Command.execute(intrface.gameObject, cmd);
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

                    if (candidates.size() == 1) {
                        input.setLength(0);
                        input.append(candidates.get(0));
                        inText.setContent(input + "_");

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
                    return; // Ignore input when chat is disabled
                }
                if (cursorPosition != 0 && input.length() == 0) {
                    return; // safe string insertion
                }

                input.insert(cursorPosition, (char) codepoint);
                cursorPosition++;
                updateInputText();
            }
        };

    }

    /*
     * Opens the chat, setting up callbacks and enabling rendering.
     */
    public void open() {
        inText.setContent("_");
        cursorPosition = 0;
        input.setLength(0);

        // Enable the cursor for text input
        GLFW.glfwSetInputMode(intrface.gameObject.WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);

        // Set the custom key and character callbacks for the chat
        GLFW.glfwSetKeyCallback(intrface.gameObject.WINDOW.getWindowID(), glfwKeyCallback);
        GLFW.glfwSetCharCallback(intrface.gameObject.WINDOW.getWindowID(), glfwCharCallback);

        // Avoid blocking the application with glfwWaitEvents
        GLFW.glfwPostEmptyEvent(); // Notify GLFW to process events immediately

        enabled = true;
    }

    /**
     * Closes the chat by resetting callbacks and disabling rendering.
     */
    public void close() {
        GLFW.glfwSetKeyCallback(intrface.gameObject.WINDOW.getWindowID(), Game.getDefaultKeyCallback());
        GLFW.glfwSetCharCallback(intrface.gameObject.WINDOW.getWindowID(), null);
        GLFW.glfwSetInputMode(intrface.gameObject.WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPosCallback(intrface.gameObject.WINDOW.getWindowID(), Game.getDefaultCursorCallback());
        GLFW.glfwSetScrollCallback(intrface.gameObject.WINDOW.getWindowID(), null);

        inText.setContent("");
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
     * Update the input text with cursor at correct position
     */
    private void updateInputText() {
        // Create the display text with cursor
        StringBuilder displayText = new StringBuilder(input);
        displayText.insert(cursorPosition, '_');
        inText.setContent(displayText.toString());

        // Ensure cursor stays within bounds
        if (cursorPosition < 0) {
            cursorPosition = 0;
        }
        if (cursorPosition > input.length()) {
            cursorPosition = input.length();
        }
    }

    /**
     * Write output to the Chat with proper positioning
     *
     * @param msg message output
     */
    public void write(String msg) {
        synchronized (GameObject.UPDATE_RENDER_IFC_MUTEX) {
            // add to queue
            DynamicText item;
            try {
                item = new DynamicText(intrface.gameObject.GameAssets.FONT, msg, new Vector2f(), 18, 18, intrface);
                history.addFirst(item);

                // Position all messages including the new one
                positionMessages();

            } catch (Exception ex) {
                DSLogger.reportError("Unable to create chat line! =>" + ex.getMessage(), ex);
            }

            // if over capacity dequeue last
            if (history.size() > HISTORY_CAPACITY) {
                history.removeLast();
                positionMessages(); // Reposition after removal
            }
        }
    }

    /**
     * Render in the interface (has to be enabled)
     *
     * @param intrface game interface
     * @param shaderProgram shader program
     * @param contourShaderProgram shader program animated (contour)
     */
    public void render(Intrface intrface, ShaderProgram shaderProgram, ShaderProgram contourShaderProgram) {
        if (enabled) {
            // Update and render panel
            if (!panel.isBuffered()) {
                panel.bufferSmart(intrface);
            }
            panel.render(intrface, shaderProgram);

            if (!inText.isBuffered()) {
                inText.bufferSmart(intrface);
            }
            inText.render(intrface, shaderProgram);

            // Render history messages
            for (var item : history) {
                if (!item.isBuffered()) {
                    item.bufferSmart(intrface);
                }
                item.render(intrface, shaderProgram);
            }
        }
    }

    /**
     * Positions all messages in the console with proper vertical stacking
     */
    public void positionMessages() {
        float startY = inText.pos.y - inText.getRelativeHeight(intrface) * Text.LINE_SPACING; // Start below input line
        float spacing = inText.getRelativeHeight(intrface) * 2.0f * Text.LINE_SPACING; // Consistent spacing

        for (int i = 0; i < history.size(); i++) {
            DynamicText item = history.get(i);

            // Position text
            item.pos.x = -1.0f + item.getRelativeCharWidth(intrface) * item.scale;
            item.pos.y = startY - (i * spacing) - item.getRelativeHeight(intrface);
            item.alignToNextChar(intrface);
            item.setBuffered(false); // !important! otherwise message won't move
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

    public static Configuration getCfg() {
        return cfg;
    }

    public StringBuilder getInput() {
        return input;
    }

    public DynamicText getInText() {
        return inText;
    }

    public IList<DynamicText> getHistory() {
        return history;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

}
