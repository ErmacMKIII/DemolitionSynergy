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
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public abstract class OptionsMenu extends Menu {

    protected static enum InputMode {
        INIT, GET, PUT
    }
    protected static InputMode mode = InputMode.INIT;

    protected boolean inputEdited = false;

    protected final StringBuilder input = new StringBuilder(); // this is the answer we type from keyboard

    protected GLFWCharCallback glfwCharCallback;

    public OptionsMenu(Intrface intrface, String title, IList<MenuItem> items, String textureFileName) throws Exception {
        super(intrface, title, items, textureFileName);
        additionalInit(intrface);
    }

    public OptionsMenu(Intrface intrface, String title, IList<MenuItem> items, String textureFileName, Vector2f pos, float scale) throws Exception {
        super(intrface, title, items, textureFileName, pos, scale);
        additionalInit(intrface);
    }

    /**
     * Init additional callbacks (or override existing)
     */
    private void additionalInit(Intrface intrface) {
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

                if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                    enabled = false;
                    input.setLength(0);
                    GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                    GLFW.glfwSetCharCallback(window, null);
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                    GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                    GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                    GLFW.glfwSetCursorPos(intrface.gameObject.WINDOW.getWindowID(), intrface.gameObject.WINDOW.getWidth() / 2.0, intrface.gameObject.WINDOW.getHeight() / 2.0);
                    leave();
                } else if (key == GLFW.GLFW_KEY_UP && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    selectPrev(intrface);
                    intrface.gameObject.getSoundFXPlayer().play(AudioFile.MENU_SELECT, new Vector3f());
                } else if (key == GLFW.GLFW_KEY_DOWN && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    selectNext(intrface);
                    intrface.gameObject.getSoundFXPlayer().play(AudioFile.MENU_SELECT, new Vector3f());
                } else if (key == GLFW.GLFW_KEY_LEFT && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    MenuItem selectedMenuItem = items.get(selected);
                    if (selectedMenuItem != null && selectedMenuItem.editType == Menu.EditType.EditMultiValue) {
                        MultiValue selectedMultiValue = (MultiValue) selectedMenuItem.menuValue;
                        selectedMultiValue.selectPrev();
                        execute();
                        intrface.gameObject.getSoundFXPlayer().play(AudioFile.MENU_OPTIONS, new Vector3f());
                    }
                } else if (key == GLFW.GLFW_KEY_RIGHT && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    MenuItem selectedMenuItem = items.get(selected);
                    if (selectedMenuItem != null && selectedMenuItem.editType == Menu.EditType.EditMultiValue) {
                        MultiValue selectedMultiValue = (MultiValue) selectedMenuItem.menuValue;
                        selectedMultiValue.selectNext();
                        execute();
                        intrface.gameObject.getSoundFXPlayer().play(AudioFile.MENU_OPTIONS, new Vector3f());
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
                                selectedMenuItem.menuValue.setCurrentValue(input.toString());
                                mode = InputMode.INIT;
                                execute();
                                intrface.gameObject.getSoundFXPlayer().play(AudioFile.MENU_OPTIONS, new Vector3f());
                                break;
                        }
                    } else if (selectedMenuItem != null && selectedMenuItem.editType == Menu.EditType.EditNoValue) {
                        enabled = false;
                        GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                        GLFW.glfwSetCharCallback(window, null);
                        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                        GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                        GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                        GLFW.glfwSetCursorPos(intrface.gameObject.WINDOW.getWindowID(), intrface.gameObject.WINDOW.getWidth() / 2.0, intrface.gameObject.WINDOW.getHeight() / 2.0);
                        execute();
                        intrface.gameObject.getSoundFXPlayer().play(AudioFile.MENU_OPTIONS, new Vector3f());
                    }
                } else if (key == GLFW.GLFW_KEY_BACKSPACE && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    MenuItem selectedMenuItem = items.get(selected);
                    if (input.length() > 0 && mode == InputMode.GET) {
                        input.deleteCharAt(input.length() - 1);
                    }
                    selectedMenuItem.menuValue.getValueText().setContent(input.toString() + "_");
                } else if (ctrlPressed && key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_PRESS) {
                    GLFW.glfwSetClipboardString(intrface.gameObject.WINDOW.getWindowID(), OptionsMenu.this.items.get(selected).menuValue.getCurrentValue().toString());
                } else if (ctrlPressed && key == GLFW.GLFW_KEY_V && action == GLFW.GLFW_PRESS) {
                    final String clipboard = GLFW.glfwGetClipboardString(intrface.gameObject.WINDOW.getWindowID());
                    if (clipboard != null) {
                        OptionsMenu.this.items.get(selected).menuValue.setCurrentValue(clipboard);
                        execute();
                    }
                }
            }
        };

        glfwMouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS) {
                    MenuItem selectedMenuItem = items.get(OptionsMenu.this.selected);
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
                                selectedMenuItem.menuValue.setCurrentValue(input.toString());
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
                        GLFW.glfwSetCursorPos(intrface.gameObject.WINDOW.getWindowID(), intrface.gameObject.WINDOW.getWidth() / 2.0, intrface.gameObject.WINDOW.getHeight() / 2.0);
                        execute();
                        intrface.gameObject.getSoundFXPlayer().play(AudioFile.MENU_OPTIONS, new Vector3f());
                    }
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_2 && action == GLFW.GLFW_PRESS) {
                    if (items.get(OptionsMenu.this.selected) != null) {
                        enabled = false;
                        GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                        GLFW.glfwSetCharCallback(window, null);
                        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                        GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                        GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                        GLFW.glfwSetCursorPos(intrface.gameObject.WINDOW.getWindowID(), intrface.gameObject.WINDOW.getWidth() / 2.0, intrface.gameObject.WINDOW.getHeight() / 2.0);
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
        GLFW.glfwSetInputMode(intrface.gameObject.WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        GLFW.glfwSetCursorPosCallback(intrface.gameObject.WINDOW.getWindowID(), glfwCursorPosCallback);
        GLFW.glfwSetKeyCallback(intrface.gameObject.WINDOW.getWindowID(), glfwKeyCallback);
        GLFW.glfwWaitEvents();
        GLFW.glfwSetCharCallback(intrface.gameObject.WINDOW.getWindowID(), glfwCharCallback);

        GLFW.glfwSetMouseButtonCallback(intrface.gameObject.WINDOW.getWindowID(), glfwMouseButtonCallback);
    }

    @Override
    public void render(Intrface intface, ShaderProgram shaderProgram) {
        if (enabled) {
            //setOptionValues();
            int longest = longestWord();
            title.setAlignment(alignmentAmount);
            title.getPos().x = (alignmentAmount - 0.5f) * (longest * itemScale * title.getRelativeCharWidth(intface)) + pos.x;
            title.getPos().y = title.getRelativeCharHeight(intface) * itemScale * Text.LINE_SPACING + pos.y;
            if (!title.isBuffered()) {
                title.bufferSmart(intface);
            }
            title.render(intface, shaderProgram);
            int index = 0;
            for (MenuItem item : items) {
                item.keyText.setAlignment(alignmentAmount);
                item.keyText.getPos().x = (alignmentAmount - 0.5f) * (longest * itemScale * item.keyText.getRelativeCharWidth(intface)) + pos.x;
                item.keyText.getPos().y = -itemScale * (index + 1) * item.keyText.getRelativeCharHeight(intface) * Text.LINE_SPACING + pos.y;

                if (item.menuValue != null && item.menuValue.getValueText() != null) {
                    item.menuValue.getValueText().getPos().x = item.keyText.getPos().x + itemScale * (item.keyText.getRelativeWidth(intface) + item.keyText.getRelativeCharWidth(intface)) * (1.0f - alignmentAmount);
                    item.menuValue.getValueText().getPos().y = item.keyText.getPos().y;
                }
                item.render(intface, shaderProgram);

                index++;
            }

            if (!iterator.isBuffered()) {
                iterator.bufferSmart(intface);
            }
            iterator.render(intface, shaderProgram);
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
