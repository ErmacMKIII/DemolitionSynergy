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
import rs.alexanderstojanovich.evg.core.Combo;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.Game;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

/**
 *
 * @author Coa
 */
public abstract class AdvMenu extends Menu {

    private Text[] values; // correct and current values we display
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
        values = new Text[items.size()];
        options = new Combo[items.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = new Text(myWindow, "hack.png", "");
            values[i].getPos().x = items.get(i).getPos().x;
            values[i].getPos().x += (items.get(i).getContent().length() + 1) * items.get(i).giveRelativeWidth();
            values[i].getPos().y = items.get(i).getPos().y;
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
            title.getPos().x = alignmentAmount * (longest - title.getContent().length())
                    * title.giveRelativeWidth() + pos.x;
            title.getPos().y = Text.LINE_SPACING * title.giveRelativeHeight() + pos.y;
            title.render();
            for (int i = 0; i < items.size(); i++) {
                items.get(i).getPos().x = alignmentAmount * (longest - items.get(i).getContent().length())
                        * items.get(i).giveRelativeWidth() + pos.x;
                items.get(i).getPos().y = -Text.LINE_SPACING * (i + 1) * items.get(i).giveRelativeHeight() + pos.y;
                items.get(i).render();
                values[i].getPos().x = items.get(i).getPos().x;
                values[i].getPos().x += (items.get(i).getContent().length() + 1) * items.get(i).giveRelativeWidth();
                values[i].getPos().y = items.get(i).getPos().y;
                values[i].render();
            }
            quad.getPos().x = items.get(selected).getPos().x;
            quad.getPos().x -= 2 * items.get(selected).giveRelativeWidth();
            quad.getPos().y = items.get(selected).getPos().y;
            quad.render();
        }
    }

    public Text[] getValues() {
        return values;
    }

    public void setValues(Text[] values) {
        this.values = values;
    }

    public Combo[] getOptions() {
        return options;
    }

    public void setOptions(Combo[] options) {
        this.options = options;
    }

}
