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
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.magicwerk.brownies.collections.IList;
import static rs.alexanderstojanovich.evg.intrface.Menu.EditType.EditMultiValue;
import static rs.alexanderstojanovich.evg.intrface.Menu.EditType.EditNoValue;
import static rs.alexanderstojanovich.evg.intrface.Menu.EditType.EditSingleValue;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public abstract class OptionsMenu extends Menu {

    protected static enum InputMode {
        INIT, GET, PUT
    }
    protected static InputMode mode = InputMode.INIT;

    protected boolean inputEdited = false;

    protected final StringBuilder input = new StringBuilder(); // this is the answer we type from keyboard

    protected GLFWCharCallback glfwCharCallback;

    public OptionsMenu(String title, IList<MenuItem> items, String textureFileName) {
        super(title, items, textureFileName);
        additionalInit();
    }

    public OptionsMenu(String title, IList<MenuItem> items, String textureFileName, Vector2f pos, float scale) {
        super(title, items, textureFileName, pos, scale);
        additionalInit();
    }

    /**
     * Init additional callbacks (or override existing)
     */
    private void additionalInit() {
        glfwKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                    enabled = false;
                    input.setLength(0);
                    GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                    GLFW.glfwSetCharCallback(window, null);
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                    GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                    GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                    GLFW.glfwSetCursorPos(GameObject.MY_WINDOW.getWindowID(), GameObject.MY_WINDOW.getWidth() / 2.0, GameObject.MY_WINDOW.getHeight() / 2.0);
                    leave();
                } else if (key == GLFW.GLFW_KEY_UP && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    selectPrev();
                } else if (key == GLFW.GLFW_KEY_DOWN && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    selectNext();
                } else if (key == GLFW.GLFW_KEY_LEFT && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    MenuItem selectedMenuItem = items.get(selected);
                    if (selectedMenuItem != null && selectedMenuItem.editType == Menu.EditType.EditMultiValue) {
                        MultiValue selectedMultiValue = (MultiValue) selectedMenuItem.menuValue;
                        selectedMultiValue.selectPrev();
                        execute();
                    }
                } else if (key == GLFW.GLFW_KEY_RIGHT && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    MenuItem selectedMenuItem = items.get(selected);
                    if (selectedMenuItem != null && selectedMenuItem.editType == Menu.EditType.EditMultiValue) {
                        MultiValue selectedMultiValue = (MultiValue) selectedMenuItem.menuValue;
                        selectedMultiValue.selectNext();
                        execute();
                    }
                } else if (key == GLFW.GLFW_KEY_ENTER && action == GLFW.GLFW_PRESS) {
                    MenuItem selectedMenuItem = items.get(selected);
                    if (selectedMenuItem != null && selectedMenuItem.editType == Menu.EditType.EditSingleValue) {
                        // if both are reset
                        switch (mode) {
                            case INIT:
                                input.setLength(0);
                                input.append(selectedMenuItem.menuValue.getCurrentValue());
                                selectedMenuItem.menuValue.getValueText().setContent(input.toString() + "_");
                                inputEdited = false;
                                mode = InputMode.GET;
                                break;
                            case GET:
                                mode = InputMode.PUT;
                                break;
                            case PUT:
                                selectedMenuItem.menuValue.setCurrentValue(Long.valueOf(input.toString()));
                                mode = InputMode.INIT;
                                execute();
                                break;
                        }
                    } else if (selectedMenuItem != null && selectedMenuItem.editType == Menu.EditType.EditNoValue) {
                        enabled = false;
                        GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                        GLFW.glfwSetCharCallback(window, null);
                        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                        GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                        GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                        GLFW.glfwSetCursorPos(GameObject.MY_WINDOW.getWindowID(), GameObject.MY_WINDOW.getWidth() / 2.0, GameObject.MY_WINDOW.getHeight() / 2.0);
                        execute();
                    }
                } else if (key == GLFW.GLFW_KEY_BACKSPACE && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    MenuItem selectedMenuItem = items.get(selected);
                    if (input.length() > 0 && mode == InputMode.GET) {
                        input.deleteCharAt(input.length() - 1);
                    }
                    selectedMenuItem.menuValue.getValueText().setContent(input.toString() + "_");
                }
            }
        };

        glfwMouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS) {
                    MenuItem selectedMenuItem = items.get(OptionsMenu.this.selected);
                    if (selectedMenuItem != null) {
                        switch (selectedMenuItem.editType) {
                            case EditSingleValue:
                                switch (mode) {
                                    case INIT:
                                        input.setLength(0);
                                        input.append(selectedMenuItem.menuValue.getCurrentValue());
                                        selectedMenuItem.menuValue.getValueText().setContent(input.toString() + "_");
                                        inputEdited = false;
                                        mode = InputMode.GET;
                                        break;
                                    case GET:
                                        mode = InputMode.PUT;
                                        break;
                                    case PUT:
                                        selectedMenuItem.menuValue.setCurrentValue(Long.valueOf(input.toString()));
                                        mode = InputMode.INIT;
                                        execute();
                                        break;
                                }
                                break;
                            case EditMultiValue:
                                MultiValue selectedMultiValue = (MultiValue) selectedMenuItem.menuValue;
                                selectedMultiValue.selectNext();
                                execute();
                                break;
                            case EditNoValue:
                            default:
                                break;
                        }
                        execute();
                    }
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_2 && action == GLFW.GLFW_PRESS) {
                    if (items.get(OptionsMenu.this.selected) != null) {
                        enabled = false;
                        GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                        GLFW.glfwSetCharCallback(window, null);
                        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                        GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                        GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                        GLFW.glfwSetCursorPos(GameObject.MY_WINDOW.getWindowID(), GameObject.MY_WINDOW.getWidth() / 2.0, GameObject.MY_WINDOW.getHeight() / 2.0);
                        leave();
                    }
                }
            }
        };

        glfwCharCallback = new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                if (mode == InputMode.GET) {
                    input.append((char) codepoint);
                    MenuItem selectedMenuItem = items.get(selected);
                    if (selectedMenuItem != null && selectedMenuItem.menuValue != null) {
                        selectedMenuItem.menuValue.getValueText().setContent(input.toString() + "_");
                    }
                    inputEdited = true;
                }
            }
        };
    }

    @Override
    public void open() {
        enabled = true;
        inputEdited = false;
        mode = InputMode.INIT;
        input.setLength(0);
        GLFW.glfwSetInputMode(GameObject.MY_WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        GLFW.glfwSetCursorPosCallback(GameObject.MY_WINDOW.getWindowID(), glfwCursorPosCallback);
        GLFW.glfwSetKeyCallback(GameObject.MY_WINDOW.getWindowID(), glfwKeyCallback);
        GLFW.glfwWaitEvents();
        GLFW.glfwSetCharCallback(GameObject.MY_WINDOW.getWindowID(), glfwCharCallback);

        GLFW.glfwSetMouseButtonCallback(GameObject.MY_WINDOW.getWindowID(), glfwMouseButtonCallback);
    }

    @Override
    public void render(ShaderProgram shaderProgram) {
        if (enabled) {
            //setOptionValues();
            int longest = longestWord();
            title.setAlignment(alignmentAmount);
            title.getPos().x = (alignmentAmount - 0.5f) * (longest * itemScale * title.getRelativeCharWidth()) + pos.x;
            title.getPos().y = title.getRelativeCharHeight() * itemScale * Text.LINE_SPACING + pos.y;
            if (!title.isBuffered()) {
                title.bufferSmart();
            }
            title.render(shaderProgram);
            int index = 0;
            for (MenuItem item : items) {
                item.keyText.setAlignment(alignmentAmount);
                item.keyText.getPos().x = (alignmentAmount - 0.5f) * (longest * itemScale * item.keyText.getRelativeCharWidth()) + pos.x;
                item.keyText.getPos().y = -itemScale * (index + 1) * item.keyText.getRelativeCharHeight() * Text.LINE_SPACING + pos.y;

                if (item.menuValue != null && item.menuValue.getValueText() != null) {
                    item.menuValue.getValueText().getPos().x = item.keyText.getPos().x + itemScale * (item.keyText.getRelativeWidth() + item.keyText.getRelativeCharWidth()) * (1.0f - alignmentAmount);
                    item.menuValue.getValueText().getPos().y = item.keyText.getPos().y;
                }
                item.render(shaderProgram);

                index++;
            }

            if (!iterator.isBuffered()) {
                iterator.bufferSmart();
            }
            iterator.render(shaderProgram);
        }
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        if (glfwCharCallback != null) {
            glfwCharCallback.free();
        }
    }

}
