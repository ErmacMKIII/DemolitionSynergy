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
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Coa
 */
public class Console {

    private final Window myWindow;
    private final Quad panel;
    private final StringBuilder input = new StringBuilder();
    private final DynamicText inText;
    private final List<DynamicText> history = new ArrayList<>();
    private boolean enabled = false;

    private final Commands commands;

    public Console(Window myWindow, Object objMutex, AudioPlayer musicPlayer, AudioPlayer soundFXPlayer) {
        this.myWindow = myWindow;
        this.panel = new Quad(myWindow, myWindow.getWidth(), myWindow.getHeight() / 2, Texture.STONE);
        this.panel.setColor(new Vector3f(0.25f, 0.5f, 0.75f));
        this.panel.setPos(new Vector2f(0.0f, 0.5f));

        this.inText = new DynamicText(myWindow, Texture.FONT, "]_");
        this.inText.setColor(new Vector3f(0.0f, 1.0f, 0.0f));
        this.inText.pos.x = -1.0f;
        this.inText.pos.y += inText.getRelativeCharHeight() / 2.0f;

        this.inText.setOffset(new Vector2f(1.0f, 0.0f));

        this.commands = new Commands(myWindow, objMutex, musicPlayer, soundFXPlayer);
    }

    public void open() {
        if (input.length() == 0) {
            enabled = true;

            inText.setContent("]_");
            inText.setColor(new Vector3f(0.0f, 1.0f, 0.0f));

            GLFW.glfwSetInputMode(myWindow.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            GLFW.glfwSetCursorPosCallback(myWindow.getWindowID(), null);
            GLFW.glfwSetKeyCallback(myWindow.getWindowID(), new GLFWKeyCallback() {
                @Override
                public void invoke(long window, int key, int scancode, int action, int mods) {
                    if ((key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_GRAVE_ACCENT) && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
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
                            for (DynamicText item : history) {
                                item.pos.y += item.getRelativeCharHeight() * Text.LINE_SPACING;
                            }
                            DynamicText text = new DynamicText(myWindow, Texture.FONT, "");
                            boolean execStatus = commands.execute(input.toString());
                            if (execStatus) {
                                text.setContent(input.toString());
                                text.setColor(new Vector3f(1.0f, 1.0f, 1.0f));
                            } else {
                                text.setContent("Invalid Command!");
                                text.setColor(new Vector3f(1.0f, 0.0f, 0.0f));
                            }
                            text.pos = new Vector2f(inText.pos);
                            text.pos.y += text.getRelativeCharHeight() * Text.LINE_SPACING;
                            text.setOffset(new Vector2f(1.0f, 0.0f));
                            history.add(text);
                            input.setLength(0);
                            inText.setContent("]_");
                        }
                    }
                }
            });
            GLFW.glfwWaitEvents();
            GLFW.glfwSetCharCallback(myWindow.getWindowID(), new GLFWCharCallback() {
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
            if (!panel.isBuffered()) {
                panel.buffer();
            }
            panel.render();
            for (DynamicText command : history) {
                if (!command.isBuffered()) {
                    command.buffer();
                }
                command.render();
            }

            if (!inText.isBuffered()) {
                inText.buffer();
            }
            inText.render();
        }
    }
}
