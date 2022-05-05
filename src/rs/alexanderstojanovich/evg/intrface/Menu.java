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

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Pair;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public abstract class Menu {

    private Quad logo; // only basic menus have logo
    protected DynamicText title;

    protected List<Pair<String, Boolean>> itemPairs;
    protected boolean enabled = false;

    protected Vector2f pos = new Vector2f();
    protected List<DynamicText> items = new ArrayList<>();

    protected float itemScale = 1.0f;

    protected int selected = 0;

    protected Quad iterator; // is minigun iterator

    protected float alignmentAmount = Text.ALIGNMENT_LEFT;

    // coordinates of the cursor (in OpenGL) when menu is opened
    protected float xposGL = 0.0f;
    protected float yposGL = 0.0f;

    protected boolean useMouse = false;

    public Menu(String title, List<Pair<String, Boolean>> itemPairs, String textureFileName) {
        this.title = new DynamicText(Texture.FONT, title);
        this.title.setColor(Vector3fColors.YELLOW);
        this.itemPairs = itemPairs;
        Texture mngTexture = Texture.MINIGUN;
        iterator = new Quad(24, 24, mngTexture);
        iterator.scale = itemScale;
        makeItems();
        updateIterator();
    }

    public Menu(String title, List<Pair<String, Boolean>> itemPairs, String textureFileName, Vector2f pos, float scale) {
        this.title = new DynamicText(Texture.FONT, title);
        this.title.setScale(scale);
        this.title.setColor(Vector3fColors.YELLOW);
        this.itemPairs = itemPairs;
        this.enabled = false;
        this.pos = pos;
        this.itemScale = scale;
        Texture mngTexture = Texture.MINIGUN;
        iterator = new Quad(24, 24, mngTexture);
        iterator.scale = scale;
        makeItems();
        updateIterator();
    }

    private void makeItems() {
        for (Pair<String, Boolean> pair : itemPairs) {
            DynamicText item = new DynamicText(Texture.FONT, pair.getKey());
            if (pair.getValue()) {
                item.color = Vector3fColors.GREEN;
            } else {
                item.color = Vector3fColors.RED;
            }
            item.getPos().x = pos.x;
            item.getPos().y = -DynamicText.LINE_SPACING * items.size() * item.getRelativeCharHeight() + pos.y;
            item.setScale(itemScale);
            item.setAlignment(alignmentAmount);
            items.add(item);
        }

        int index = 0;
        int longestWord = longestWord();
        for (DynamicText item : items) {
            item.setAlignment(alignmentAmount);
            item.getPos().x = (alignmentAmount - 0.5f) * (longestWord * itemScale * item.getRelativeCharWidth()) + pos.x;
            item.getPos().y = -DynamicText.LINE_SPACING * itemScale * (index + 1) * item.getRelativeCharHeight() + pos.y;
            index++;
        }
    }

    protected abstract void leave(); // we can do something after leaving..

    protected abstract void execute(); // we don't know the menu functionality

    public void open() {
        enabled = true;
        GLFW.glfwSetInputMode(GameObject.MY_WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        GLFW.glfwSetCursorPosCallback(GameObject.MY_WINDOW.getWindowID(), new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                // get the new values
                float new_xposGL = (float) (xpos / GameObject.MY_WINDOW.getWidth() - 0.5f) * 2.0f;
                float new_yposGL = (float) (0.5f - ypos / GameObject.MY_WINDOW.getHeight()) * 2.0f;

                // if new and prev values aren't the same user moved the mouse
                if (new_xposGL != xposGL || new_yposGL != yposGL) {
                    useMouse = true;
                }

                // assign the new values (remember them)
                xposGL = new_xposGL;
                yposGL = new_yposGL;
            }
        });
        GLFW.glfwSetCharCallback(GameObject.MY_WINDOW.getWindowID(), null);
        GLFW.glfwSetKeyCallback(GameObject.MY_WINDOW.getWindowID(), new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                    enabled = false;
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
                } else if (key == GLFW.GLFW_KEY_ENTER && action == GLFW.GLFW_PRESS) {
                    enabled = false;
                    GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                    GLFW.glfwSetCharCallback(window, null);
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                    GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                    GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
                    GLFW.glfwSetCursorPos(GameObject.MY_WINDOW.getWindowID(), GameObject.MY_WINDOW.getWidth() / 2.0, GameObject.MY_WINDOW.getHeight() / 2.0);
                    execute();
                }
            }
        });

        GLFW.glfwSetMouseButtonCallback(GameObject.MY_WINDOW.getWindowID(), new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS) {
                    enabled = false;
                    GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                    GLFW.glfwSetCharCallback(window, null);
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                    GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                    GLFW.glfwSetMouseButtonCallback(window, Game.getDefaultMouseButtonCallback());
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
        });
    }

    protected int longestWord() {
        int longest = 0;
        for (DynamicText item : items) {
            if (item.getContent().length() > longest) {
                longest = item.getContent().length();
            }
        }
        return longest;
    }

    public void render(ShaderProgram shaderProgram) {
        if (enabled) {
            int longest = longestWord();
            title.setAlignment(alignmentAmount);
            title.getPos().x = (alignmentAmount - 0.5f) * (longest * itemScale * title.getRelativeCharWidth()) + pos.x;
            title.getPos().y = DynamicText.LINE_SPACING * title.getRelativeCharHeight() * itemScale + pos.y;
            if (!title.isBuffered()) {
                title.bufferAll();
            }
            title.render(shaderProgram);
            if (logo != null && title.getContent().equals("")) {
                logo.getPos().x = (alignmentAmount - 0.5f) + pos.x;
                logo.getPos().y = logo.giveRelativeHeight() * logo.getScale() + pos.y;
                if (!logo.isBuffered()) {
                    logo.bufferAll();
                }
                logo.render(shaderProgram);
            }
            int index = 0;
            for (DynamicText item : items) {
                item.setAlignment(alignmentAmount);
                item.getPos().x = (alignmentAmount - 0.5f) * (longest * itemScale * item.getRelativeCharWidth()) + pos.x;
                item.getPos().y = -DynamicText.LINE_SPACING * itemScale * (index + 1) * item.getRelativeCharHeight() + pos.y;

                if (!item.isBuffered()) {
                    item.bufferAll();
                }

                item.render(shaderProgram);
                index++;
            }

            if (!iterator.isBuffered()) {
                iterator.bufferAll();
            }
            iterator.render(shaderProgram);
        }
    }

    private void updateIterator() {
        if (selected >= 0 && selected < items.size()) {
            iterator.getPos().x = items.get(selected).getPos().x;
            iterator.getPos().x -= items.get(selected).getRelativeWidth() * alignmentAmount * itemScale;
            iterator.getPos().x -= 1.5f * iterator.giveRelativeWidth() * iterator.getScale();
            iterator.getPos().y = items.get(selected).getPos().y;
            iterator.setColor(items.get(selected).getColor());
        }
    }

    public void selectPrev() {
        useMouse = false;
        selected--;
        if (selected < 0) {
            selected = items.size() - 1;
        }
        updateIterator();
    }

    public void selectNext() {
        useMouse = false;
        selected++;
        if (selected > items.size() - 1) {
            selected = 0;
        }
        updateIterator();
    }

    // if menu is enabled; it's gonna track mouse cursor position 
    // to determine selected item
    public void update() {
        if (enabled && useMouse) {
            int index = 0;
            for (DynamicText item : items) {
                float xMin = item.pos.x; // it already contains pos.x
                float xMax = xMin + itemScale * item.getRelativeWidth();

                float yMin = item.pos.y; // it already contains pos.y
                float yMax = yMin + itemScale * item.getRelativeCharHeight();

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
        updateIterator();
    }

    public Window getMyWindow() {
        return GameObject.MY_WINDOW;
    }

    public Quad getLogo() {
        return logo;
    }

    public void setLogo(Quad logo) {
        this.logo = logo;
    }

    public List<Pair<String, Boolean>> getItemPairs() {
        return itemPairs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Vector2f getPos() {
        return pos;
    }

    public List<DynamicText> getItems() {
        return items;
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

    public void setItems(List<DynamicText> items) {
        this.items = items;
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

}
