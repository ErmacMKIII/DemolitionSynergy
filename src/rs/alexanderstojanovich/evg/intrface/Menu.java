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
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public abstract class Menu {

    protected static enum EditType {
        EditNoValue, EditSingleValue, EditMultiValue
    };

    private Quad logo; // only basic menus have logo
    protected DynamicText title;

    protected final IList<MenuItem> items;
    protected boolean enabled = false;

    protected Vector2f pos = new Vector2f();
    protected float itemScale = 1.0f;

    protected int selected = 0;

    protected Quad iterator; // is minigun iterator

    protected float alignmentAmount = Text.ALIGNMENT_LEFT;

    // coordinates of the cursor (in OpenGL) when menu is opened
    protected float xposGL = 0.0f;
    protected float yposGL = 0.0f;

    protected boolean useMouse = false;

    protected GLFWCursorPosCallback glfwCursorPosCallback;
    protected GLFWKeyCallback glfwKeyCallback;
    protected GLFWMouseButtonCallback glfwMouseButtonCallback;
    protected final Intrface intrface;

    protected boolean ctrlPressed = false;

    public Menu(Intrface intrface, String title, IList<MenuItem> items, String textureFileName) throws Exception {
        this.intrface = intrface;
        this.title = new DynamicText(intrface.gameObject.GameAssets.FONT, title, intrface);
        this.title.setColor(new Vector4f(GlobalColors.YELLOW, 1.0f));
        this.items = items;
        Texture mngTexture = intrface.gameObject.GameAssets.POINTER;
        iterator = new Quad(24, 24, mngTexture, intrface);
        iterator.scale = itemScale;
        makeItems(intrface);
        updateIterator(intrface);
        init(intrface);
    }

    public Menu(Intrface intrface, String title, IList<MenuItem> items, String textureFileName, Vector2f pos, float scale) throws Exception {
        this.intrface = intrface;
        this.title = new DynamicText(intrface.gameObject.GameAssets.FONT, title, intrface);
        this.title.setScale(scale);
        this.title.setColor(new Vector4f(GlobalColors.YELLOW, 1.0f));
        this.items = items;
        this.enabled = false;
        this.pos = pos;
        this.itemScale = scale;
        Texture mngTexture = intrface.gameObject.GameAssets.POINTER;
        iterator = new Quad(24, 24, mngTexture, intrface);
        iterator.scale = scale;
        makeItems(intrface);
        updateIterator(intrface);
        init(intrface);
    }

    private void makeItems(Intrface intrface) {
        int index = 0;
        int longestWord = longestWord();
        for (MenuItem item : items) {
            item.keyText.color = new Vector4f(GlobalColors.GREEN, 1.0f);
            item.keyText.setScale(itemScale);
            item.keyText.setAlignment(alignmentAmount);
            item.keyText.getPos().x = (alignmentAmount - 0.5f) * (longestWord * itemScale * item.keyText.getRelativeCharWidth(intrface)) + pos.x;
            item.keyText.getPos().y = -itemScale * (index + 1) * item.keyText.getRelativeCharHeight(intrface) + pos.y;
            item.keyText.alignToNextChar(intrface);
            index++;
        }
    }

    protected abstract void leave(); // we can do something after leaving..

    protected abstract void execute(); // we don't know the menu functionality

    /**
     * Initialize menu with callbacks.
     */
    private void init(Intrface intrface) {
        glfwCursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                // get the new values
                float new_xposGL = (float) (xpos / intrface.gameObject.gameWindow.getWidth() - 0.5f) * 2.0f;
                float new_yposGL = (float) (0.5f - ypos / intrface.gameObject.gameWindow.getHeight()) * 2.0f;

                // if new and prev values aren't the same user moved the mouse
                if (new_xposGL != xposGL || new_yposGL != yposGL) {
                    useMouse = true;
                }

                // assign the new values (remember them)
                xposGL = new_xposGL;
                yposGL = new_yposGL;
            }
        };
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
                    GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                    GLFW.glfwSetCharCallback(window, null);
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                    GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                    GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                    GLFW.glfwSetCursorPos(intrface.gameObject.gameWindow.getWindowID(), intrface.gameObject.gameWindow.getWidth() / 2.0, intrface.gameObject.gameWindow.getHeight() / 2.0);
                    leave();
                } else if (key == GLFW.GLFW_KEY_UP && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    selectPrev(intrface);
                    intrface.gameObject.getSoundFXPlayer().play(AudioFile.MENU_SELECT, new Vector3f());
                } else if (key == GLFW.GLFW_KEY_DOWN && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    selectNext(intrface);
                    intrface.gameObject.getSoundFXPlayer().play(AudioFile.MENU_SELECT, new Vector3f());
                } else if (key == GLFW.GLFW_KEY_ENTER && action == GLFW.GLFW_PRESS) {
                    enabled = false;
                    GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                    GLFW.glfwSetCharCallback(window, null);
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                    GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                    GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                    GLFW.glfwSetCursorPos(intrface.gameObject.gameWindow.getWindowID(), intrface.gameObject.gameWindow.getWidth() / 2.0, intrface.gameObject.gameWindow.getHeight() / 2.0);
                    intrface.gameObject.getSoundFXPlayer().play(AudioFile.MENU_ACCEPT, new Vector3f());
                    execute();
                } else if (ctrlPressed && key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_PRESS) {
                    GLFW.glfwSetClipboardString(intrface.gameObject.gameWindow.getWindowID(), Menu.this.items.get(selected).menuValue.getCurrentValue().toString());
                } else if (ctrlPressed && key == GLFW.GLFW_KEY_V && action == GLFW.GLFW_PRESS) {
                    String clipboard = GLFW.glfwGetClipboardString(intrface.gameObject.gameWindow.getWindowID());
                    clipboard = (clipboard.length() <= 256) ? clipboard : clipboard.substring(0, 256);
                    if (clipboard != null) {
                        Menu.this.items.get(selected).menuValue.setCurrentValue(clipboard);
                        intrface.gameObject.getSoundFXPlayer().play(AudioFile.BLOCK_SELECT, new Vector3f());
                        execute();
                    }
                }

            }
        };
        glfwMouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS) {
                    enabled = false;
                    GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                    GLFW.glfwSetCharCallback(window, null);
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                    GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                    GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                    intrface.gameObject.getSoundFXPlayer().play(AudioFile.MENU_ACCEPT, new Vector3f());
                    execute();
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_2 && action == GLFW.GLFW_PRESS) {
                    enabled = false;
                    GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                    GLFW.glfwSetCharCallback(window, null);
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                    GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                    GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                    leave();
                }
            }
        };

    }

    /**
     * When open callbacks are changed (take input from keyboard, mouse etc)
     * Enabled is set to true for rendering.
     */
    public void open() {
        enabled = true;
        GLFW.glfwSetInputMode(intrface.gameObject.gameWindow.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        GLFW.glfwSetCursorPosCallback(intrface.gameObject.gameWindow.getWindowID(), glfwCursorPosCallback);
        GLFW.glfwSetCharCallback(intrface.gameObject.gameWindow.getWindowID(), null);
        GLFW.glfwSetKeyCallback(intrface.gameObject.gameWindow.getWindowID(), glfwKeyCallback);
        GLFW.glfwSetMouseButtonCallback(intrface.gameObject.gameWindow.getWindowID(), glfwMouseButtonCallback);
    }

    protected int longestWord() {
        int longest = 0;
        for (MenuItem item : items) {
            if (item.keyText.getContent().length() > longest) {
                longest = item.keyText.content.length();
            }
        }
        return longest;
    }

    public void render(Intrface intrface, ShaderProgram shaderProgram) {
        if (enabled) {
            int longest = longestWord();
            title.setAlignment(alignmentAmount);
            title.getPos().x = (alignmentAmount - 0.5f) * (longest * itemScale * title.getRelativeCharWidth(intrface)) + pos.x;
            title.getPos().y = title.getRelativeCharHeight(intrface) * itemScale * Text.LINE_SPACING + pos.y;
            if (!title.isBuffered()) {
                title.bufferSmart(intrface);
            }
            title.render(intrface, shaderProgram);
            if (logo != null && title.getContent().equals("")) {
                logo.getPos().x = (alignmentAmount - 0.5f) * (longest * logo.getScale() * title.getRelativeCharWidth(intrface)) + pos.x;
                logo.getPos().y = logo.giveRelativeHeight(intrface) * logo.getScale() + pos.y;
                if (!logo.isBuffered()) {
                    logo.bufferSmart(intrface);
                }
                logo.render(intrface, shaderProgram);
            }
            int index = 0;
            for (MenuItem item : items) {
                item.keyText.setAlignment(alignmentAmount);
                item.keyText.getPos().x = (alignmentAmount - 0.5f) * (longest * itemScale * item.keyText.getRelativeCharWidth(intrface)) + pos.x;
                item.keyText.getPos().y = -itemScale * (index + 1) * item.keyText.getRelativeCharHeight(intrface) * Text.LINE_SPACING + pos.y;

                item.render(intrface, shaderProgram);
                index++;
            }

            if (!iterator.isBuffered()) {
                iterator.bufferSmart(intrface);
            }
            iterator.render(intrface, shaderProgram);
        }
    }

    private void updateIterator(Intrface intrface) {
        if (selected >= 0 && selected < items.size()) {
            iterator.getPos().x = items.get(selected).keyText.getPos().x;
            iterator.getPos().x -= items.get(selected).keyText.getRelativeWidth(intrface) * alignmentAmount * itemScale;
            iterator.getPos().x -= 1.5f * iterator.giveRelativeWidth(intrface) * iterator.getScale();
            iterator.getPos().y = items.get(selected).keyText.getPos().y;
            iterator.setColor(items.get(selected).keyText.color);
        }
    }

    public void selectPrev(Intrface intrface) {
        useMouse = false;
        selected--;
        if (selected < 0) {
            selected = items.size() - 1;
        }
        updateIterator(intrface);
    }

    public void selectNext(Intrface intrface) {
        useMouse = false;
        selected++;
        if (selected > items.size() - 1) {
            selected = 0;
        }
        updateIterator(intrface);
    }

    // if menu is enabled; it's gonna track mouse cursor position 
    // to determine selected item
    public void update() {
        if (enabled && useMouse) {
            int index = 0;
            for (MenuItem item : items) {
                float xMin = item.keyText.pos.x - item.keyText.scale * item.keyText.getRelativeWidth(intrface);
                float xMax = item.keyText.pos.x + item.keyText.scale * item.keyText.getRelativeWidth(intrface);

                float yMin = item.keyText.pos.y - item.keyText.scale * item.keyText.getRelativeCharHeight(intrface);
                float yMax = item.keyText.pos.y + item.keyText.scale * item.keyText.getRelativeCharHeight(intrface);

                if (xposGL >= xMin
                        && xposGL <= xMax
                        && yposGL >= yMin
                        && yposGL <= yMax) {
                    selected = index;
                    break;
                }
                index++;
            }
            useMouse = false;
        }
        updateIterator(intrface);
    }

    public void cleanUp() {
        if (glfwCursorPosCallback != null) {
            glfwCursorPosCallback.free();
        }

        if (glfwKeyCallback != null) {
            glfwKeyCallback.free();
        }

        if (glfwMouseButtonCallback != null) {
            glfwMouseButtonCallback.free();
        }
    }

    /**
     * Release all GL components. GL Buffers are deleted.
     */
    public void release() {
        if (this.logo != null) {
            this.logo.release();
        }

        if (this.title != null) {
            this.title.release();
        }

        this.items.forEach(i -> i.release());
    }

    public Window getMyWindow() {
        return intrface.gameObject.gameWindow;
    }

    public Quad getLogo() {
        return logo;
    }

    public void setLogo(Quad logo) {
        this.logo = logo;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Vector2f getPos() {
        return pos;
    }

    public float getXposGL() {
        return xposGL;
    }

    public float getYposGL() {
        return yposGL;
    }

    public boolean isUseMouse() {
        return useMouse;
    }

    public int getSelected() {
        return selected;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

    public Quad getIterator() {
        return iterator;
    }

    public void setIterator(Quad iterator) {
        this.iterator = iterator;
    }

    public float getAlignmentAmount() {
        return alignmentAmount;
    }

    public void setAlignmentAmount(float alignmentAmount) {
        this.alignmentAmount = alignmentAmount;
    }

    public void setPos(Vector2f pos) {
        this.pos = pos;
    }

    public DynamicText getTitle() {
        return title;
    }

    public float getItemScale() {
        return itemScale;
    }

    public IList<MenuItem> getItems() {
        return items;
    }

}
