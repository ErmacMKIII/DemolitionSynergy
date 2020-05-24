/*
 * Copyright (C) 2020 Coa
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.main.Renderer;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Pair;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Coa
 */
public class Console {

    private final Quad panel;
    private final StringBuilder input = new StringBuilder();
    private final DynamicText inText;
    private final List<Pair<DynamicText, Quad>> history = new ArrayList<>();
    private boolean enabled = false;

    public static final int HISTORY_CAPACITY = 12;

    public Console() {
        this.panel = new Quad(GameObject.MY_WINDOW.getWidth(),
                GameObject.MY_WINDOW.getHeight() / 2, Texture.CONSOLE);
        this.panel.setColor(LevelContainer.SKYBOX_COLOR);
        this.panel.setPos(new Vector2f(0.0f, 0.5f));
        this.panel.setIgnoreFactor(true);

        this.inText = new DynamicText(Texture.FONT, "]_");
        this.inText.setColor(Vector3fColors.GREEN);
        this.inText.pos.x = -1.0f;
        this.inText.pos.y = 0.5f - panel.getPos().y + inText.getRelativeCharHeight() / 2.0f;

        this.inText.setAlignment(Text.ALIGNMENT_LEFT);
    }

    public void open() {
        if (input.length() == 0) {
            enabled = true;

            inText.setContent("]_");
            inText.setColor(Vector3fColors.GREEN);

            GLFW.glfwSetInputMode(GameObject.MY_WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            GLFW.glfwSetCursorPosCallback(GameObject.MY_WINDOW.getWindowID(), null);
            GLFW.glfwSetKeyCallback(GameObject.MY_WINDOW.getWindowID(), new GLFWKeyCallback() {
                @Override
                public void invoke(long window, int key, int scancode, int action, int mods) {
                    if ((key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_GRAVE_ACCENT) && action == GLFW.GLFW_PRESS) {
                        GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                        GLFW.glfwSetCharCallback(window, null);
                        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                        GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                        inText.setContent("");
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
                            DynamicText text = new DynamicText(Texture.FONT, "");
                            Quad quad = new Quad(16, 16, Texture.LIGHT_BULB);
                            Command command = Command.getCommand(input.toString());
                            // if command is invalid it's null
                            if (command == Command.ERROR) {
                                text.setContent("Invalid Command!");
                                text.setColor(Vector3fColors.RED);
                            } else if (command.isRendererCommand()) {
                                boolean result = false;
                                FutureTask<Boolean> consoleTask = new FutureTask<Boolean>(command);
                                Renderer.TASK_QUEUE.add(consoleTask);
                                try {
                                    // waits for renderer to execute the task                       
                                    result = consoleTask.get();
                                } catch (InterruptedException | ExecutionException ex) {
                                    DSLogger.reportError(ex.getMessage(), ex);
                                }
                                quad.setColor(result ? Vector3fColors.GREEN : Vector3fColors.RED);
                                text.setContent(input.toString());
                            } else {
                                boolean ok = Command.execute(command);
                                quad.setColor(ok ? Vector3fColors.GREEN : Vector3fColors.RED);
                                text.setContent(input.toString());
                            }

                            text.pos = new Vector2f(inText.pos);
                            text.pos.y += (0.5f - text.getRelativeCharHeight()) * Text.LINE_SPACING;

                            quad.getPos().x = text.getRelativeCharWidth() * (text.content.length() + 1);
                            quad.getPos().y = text.pos.y;

                            text.setAlignment(Text.ALIGNMENT_LEFT);

                            history.add(0, new Pair<>(text, quad));

                            if (history.size() == HISTORY_CAPACITY) {
                                history.remove(history.size() - 1);
                            }

                            input.setLength(0);
                            inText.setContent("]_");
                        }
                    }
                }
            });
            GLFW.glfwWaitEvents();
            GLFW.glfwSetCharCallback(GameObject.MY_WINDOW.getWindowID(), new GLFWCharCallback() {
                @Override
                public void invoke(long window, int codepoint) {
                    input.append((char) codepoint);
                    inText.setContent("]" + input + "_");
                }
            });
        }
    }

    public void render() {
        if (enabled) {
            panel.setWidth(GameObject.MY_WINDOW.getWidth());
            panel.setHeight(GameObject.MY_WINDOW.getHeight() / 2);
            if (!panel.isBuffered()) {
                panel.buffer();
            }
            panel.render();
            inText.pos.x = -1.0f;
            inText.pos.y = 0.5f - panel.getPos().y + inText.getRelativeCharHeight() / 2.0f;
            if (!inText.isBuffered()) {
                inText.buffer();
            }
            inText.render();
            int index = 0;
            for (Pair<DynamicText, Quad> item : history) {
                DynamicText text = item.getKey();
                Quad quad = item.getValue();
                text.pos.x = -1.0f;
                quad.getPos().x = text.getRelativeCharWidth() * (text.content.length() + 2) - 1.0f;
                text.pos.y = inText.pos.y + (index + 1) * text.getRelativeCharHeight() * Text.LINE_SPACING;
                quad.getPos().y = text.pos.y;
                if (!text.isBuffered()) {
                    text.buffer();
                }
                text.render();
                if (!quad.isBuffered()) {
                    quad.buffer();
                }
                quad.render();
                index++;
            }
        }
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

    public List<Pair<DynamicText, Quad>> getHistory() {
        return history;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
