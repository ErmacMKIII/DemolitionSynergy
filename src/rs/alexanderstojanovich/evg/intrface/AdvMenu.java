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

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import rs.alexanderstojanovich.evg.core.Combo;
import rs.alexanderstojanovich.evg.core.Texture;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.Game;

/**
 *
 * @author Coa
 */
public abstract class AdvMenu extends Menu {

    private DynamicText[] values; // correct and current values we display
    private Combo[] options; // options we can set we display

    public AdvMenu(Window window, String title, String fileName, String textureFileName) {
        super(window, title, fileName, textureFileName);
        init();
    }

    public AdvMenu(Window window, String title, String fileName, String textureFileName, Vector2f pos, float scale) {
        super(window, title, fileName, textureFileName, pos, scale);
        init();
    }

    private void init() {
        values = new DynamicText[items.size()];
        options = new Combo[items.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = new DynamicText(myWindow, Texture.FONT, "");
            values[i].getQuad().getPos().x = items.get(i).getQuad().getPos().x;
            values[i].getQuad().getPos().x += (items.get(i).getContent().length() + 1) * items.get(i).getQuad().giveRelativeWidth();
            values[i].getQuad().getPos().y = items.get(i).getQuad().getPos().y;
        }
    }

    protected abstract void refreshValues();

    @Override
    public void open() {
        enabled = true;
        GLFW.glfwSetInputMode(myWindow.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        GLFW.glfwSetCursorPosCallback(myWindow.getWindowID(), null);
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
    }

    @Override
    public void render() {
        if (enabled) {
            refreshValues();
            int longest = longestWord();
            title.getQuad().getPos().x = (alignmentAmount * (longest - title.getContent().length()) - longest / 2)
                    * title.getQuad().giveRelativeWidth() * itemScale + pos.x;
            title.getQuad().getPos().y = DynamicText.LINE_SPACING * title.getQuad().giveRelativeHeight() * itemScale + pos.y;
            title.render();
            int index = 0;
            for (DynamicText item : items) {
                Quad itemQuad = item.getQuad();
                int itemDiff = longest - item.getContent().length();
                itemQuad.getPos().x = (alignmentAmount * itemDiff - longest / 2) * itemQuad.giveRelativeWidth() * itemScale + pos.x;
                itemQuad.getPos().y = -DynamicText.LINE_SPACING * itemScale * (index + 1) * itemQuad.giveRelativeHeight() + pos.y;
                item.render();
                values[index].getQuad().getPos().x = item.getQuad().getPos().x;
                values[index].getQuad().getPos().x += (item.getContent().length() + 1) * item.getQuad().giveRelativeWidth() * itemScale;
                values[index].getQuad().getPos().y = item.getQuad().getPos().y;
                values[index].render();
                index++;
            }
            iterator.getPos().x = items.get(selected).getQuad().getPos().x;
            iterator.getPos().x -= 2.0f * items.get(selected).getQuad().giveRelativeWidth() * itemScale;
            iterator.getPos().y = items.get(selected).getQuad().getPos().y;
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
