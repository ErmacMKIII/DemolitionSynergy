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

import java.util.List;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import rs.alexanderstojanovich.evg.core.Combo;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Pair;

/**
 *
 * @author Coa
 */
public abstract class OptionsMenu extends Menu {

    private DynamicText[] values; // correct and current values we display
    private Combo[] options; // options we can set we display

    public OptionsMenu(Window window, String title, List<Pair<String, Boolean>> itemPairs, String textureFileName) {
        super(window, title, itemPairs, textureFileName);
        init();
    }

    public OptionsMenu(Window window, String title, List<Pair<String, Boolean>> itemPairs, String textureFileName, Vector2f pos, float scale) {
        super(window, title, itemPairs, textureFileName, pos, scale);
        init();
    }

    private void init() {
        values = new DynamicText[items.size()];
        options = new Combo[items.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = new DynamicText(myWindow, Texture.FONT, "");
            values[i].getPos().x = items.get(i).getPos().x;
            values[i].getPos().x += (items.get(i).getContent().length() + 1) * items.get(i).getRelativeCharWidth();
            values[i].getPos().y = items.get(i).getPos().y;
        }
    }

    protected abstract void refreshValues();

    @Override
    public void open() {
        enabled = true;
        GLFW.glfwSetInputMode(myWindow.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        GLFW.glfwSetCursorPosCallback(myWindow.getWindowID(), new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                // get the new values
                float new_xposGL = (float) (xpos / myWindow.getWidth() - 0.5f) * 2.0f;
                float new_yposGL = (float) (0.5f - ypos / myWindow.getHeight()) * 2.0f;

                // if new and prev values aren't the same user moved the mouse
                if (new_xposGL != xposGL || new_yposGL != yposGL) {
                    useMouse = true;
                }

                // assign the new values (remember them)
                xposGL = new_xposGL;
                yposGL = new_yposGL;
            }
        });
        GLFW.glfwSetCharCallback(myWindow.getWindowID(), null);
        GLFW.glfwSetKeyCallback(myWindow.getWindowID(), new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                    enabled = false;
                    GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                    GLFW.glfwSetCharCallback(window, null);
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                    GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                    GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                    leave();
                } else if (key == GLFW.GLFW_KEY_UP && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    selectPrev();
                } else if (key == GLFW.GLFW_KEY_DOWN && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    selectNext();
                } else if (key == GLFW.GLFW_KEY_LEFT && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    if (options[selected] != null) {
                        options[selected].selectPrev();
                        execute();
                    }
                } else if (key == GLFW.GLFW_KEY_RIGHT && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    if (options[selected] != null) {
                        options[selected].selectNext();
                        execute();
                    }
                } else if (key == GLFW.GLFW_KEY_ENTER && action == GLFW.GLFW_PRESS) {
                    if (options[selected] != null) {
                        options[selected].selectNext();
                        execute();
                    }
                }
            }
        });

        GLFW.glfwSetMouseButtonCallback(myWindow.getWindowID(), new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS) {
                    if (options[selected] != null) {
                        options[selected].selectNext();
                        execute();
                    }
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_2 && action == GLFW.GLFW_PRESS) {
                    if (options[selected] != null) {
                        enabled = false;
                        GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                        GLFW.glfwSetCharCallback(window, null);
                        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                        GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                        GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                        leave();
                    }
                }
            }
        });
    }

    @Override
    public void render() {
        if (enabled) {
            refreshValues();
            int longest = longestWord();
            title.getPos().x = (alignmentAmount * (longest - title.getContent().length()) - longest / 2)
                    * title.getRelativeCharWidth() * itemScale + pos.x;
            title.getPos().y = DynamicText.LINE_SPACING * title.getRelativeCharHeight() * itemScale + pos.y;
            if (!title.isBuffered()) {
                title.buffer();
            }
            title.render();
            int index = 0;
            for (DynamicText item : items) {
                int itemDiff = longest - item.getContent().length();
                item.getPos().x = (alignmentAmount * itemDiff - longest / 2) * item.getRelativeCharWidth() * itemScale + pos.x;
                item.getPos().y = -DynamicText.LINE_SPACING * itemScale * (index + 1) * item.getRelativeCharHeight() + pos.y;

                if (!item.isBuffered()) {
                    item.buffer();
                }
                item.render();
                values[index].getPos().x = item.getPos().x;
                values[index].getPos().x += (item.getContent().length() + 1) * item.getRelativeCharWidth() * itemScale;
                values[index].getPos().y = item.getPos().y;
                if (!values[index].isBuffered()) {
                    values[index].buffer();
                }
                values[index].render();
                index++;
            }
            iterator.getPos().x = items.get(selected).getPos().x;
            iterator.getPos().x -= 2.0f * items.get(selected).getRelativeCharWidth() * itemScale;
            iterator.getPos().y = items.get(selected).getPos().y;
            iterator.setColor(items.get(selected).getColor());
            if (!iterator.isBuffered()) {
                iterator.buffer();
            }
            iterator.render();
        }
    }

    public DynamicText[] getValues() {
        return values;
    }

    public void setValues(DynamicText[] values) {
        this.values = values;
    }

    public Combo[] getOptions() {
        return options;
    }

    public void setOptions(Combo[] options) {
        this.options = options;
    }

}
