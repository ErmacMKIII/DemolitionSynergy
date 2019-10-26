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

import rs.alexanderstojanovich.evg.core.Window;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.Vector2f;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.core.Texture;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import rs.alexanderstojanovich.evg.shaders.Shader;

/**
 *
 * @author Coa
 */
public abstract class Menu {

    public static final float ALIGNMENT_LEFT = 0.0f;
    public static final float ALIGNMENT_RIGHT = 1.0f;
    public static final float ALIGNMENT_CENTER = 0.5f;

    protected Window myWindow;

    private Quad logo; // only basic menus have logo
    protected DynamicText title;

    protected String fileName;
    protected boolean enabled = false;

    protected Vector2f pos = new Vector2f();
    protected List<DynamicText> items = new ArrayList<>();

    protected float itemScale = 1.0f;

    protected int selected = 0;

    protected Quad iterator; // is minigun iterator

    protected float alignmentAmount = ALIGNMENT_LEFT;

    public Menu(Window window, String title, String fileName, String textureFileName) {
        this.myWindow = window;
        this.fileName = fileName;
        this.title = new DynamicText(myWindow, Texture.FONT, title);
        this.title.getQuad().setColor(new Vector3f(1.0f, 1.0f, 0.0f));
        readFromFile(fileName);
        Texture mngTexture = Texture.MINIGUN;
        iterator = new Quad(window, 24, 24, mngTexture);
        iterator.getPos().x = -items.get(selected).getQuad().getPos().x;
        iterator.getPos().y = items.get(selected).getQuad().getPos().y;
        iterator.setColor(items.get(selected).getQuad().getColor());
    }

    public Menu(Window window, String title, String fileName, String textureFileName, Vector2f pos, float scale) {
        this.myWindow = window;
        this.fileName = fileName;
        this.title = new DynamicText(myWindow, Texture.FONT, title);
        this.title.getQuad().setScale(scale);
        this.title.getQuad().setColor(new Vector3f(1.0f, 1.0f, 0.0f));
        this.enabled = false;
        this.pos = pos;
        this.itemScale = scale;
        readFromFile(fileName);
        Texture mngTexture = Texture.MINIGUN;
        iterator = new Quad(window, 24, 24, mngTexture);
        iterator.getPos().x = -items.get(selected).getQuad().getPos().x;
        iterator.getPos().y = items.get(selected).getQuad().getPos().y;
        iterator.getColor().x = items.get(selected).getQuad().getColor().x;
        iterator.getColor().y = items.get(selected).getQuad().getColor().y;
        iterator.setColor(items.get(selected).getQuad().getColor());
        iterator.setScale(scale);
    }

    private void readFromFile(String fileName) {
        InputStream in = getClass().getResourceAsStream(Game.RESOURCES_DIR + Game.INTRFACE_SUBDIR + fileName);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                String[] things = line.trim().split(":");
                DynamicText item = new DynamicText(myWindow, Texture.FONT, things[0]);
                if (Boolean.parseBoolean(things[1].trim())) {
                    item.getQuad().getColor().x = 0.0f;
                    item.getQuad().getColor().y = 1.0f;
                    item.getQuad().getColor().z = 0.0f;
                } else {
                    item.getQuad().getColor().x = 1.0f;
                    item.getQuad().getColor().y = 0.0f;
                    item.getQuad().getColor().z = 0.0f;
                }
                item.getQuad().getPos().x = pos.x;
                item.getQuad().getPos().y = -DynamicText.LINE_SPACING * items.size() * item.getQuad().giveRelativeHeight() + pos.y;
                item.getQuad().setScale(itemScale);
                items.add(item);
            }
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Shader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Shader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected abstract void leave(); // we can do something after leaving..

    protected abstract void execute(); // we don't know the menu functionality

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
                } else if (key == GLFW.GLFW_KEY_ENTER && action == GLFW.GLFW_PRESS) {
                    enabled = false;
                    GLFW.glfwSetKeyCallback(window, Game.getDefaultKeyCallback());
                    GLFW.glfwSetCharCallback(window, null);
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                    GLFW.glfwSetCursorPosCallback(window, Game.getDefaultCursorCallback());
                    execute();
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

    public void render() {
        if (enabled) {
            int longest = longestWord();
            title.getQuad().getPos().x = (alignmentAmount * (longest - title.getContent().length()) - longest / 2)
                    * title.getQuad().giveRelativeWidth() * itemScale + pos.x;
            title.getQuad().getPos().y = DynamicText.LINE_SPACING * title.getQuad().giveRelativeHeight() * itemScale + pos.y;
            if (!title.isBuffered()) {
                title.buffer();
            }
            title.render();
            if (logo != null && title.getContent().equals("")) {
                logo.getPos().x = pos.x;
                logo.getPos().y = logo.giveRelativeHeight() * logo.getScale() + pos.y;
                if (!logo.isBuffered()) {
                    logo.buffer();
                }
                logo.render();
            }
            int index = 0;
            for (DynamicText item : items) {
                Quad itemQuad = item.getQuad();
                int itemDiff = longest - item.getContent().length();
                itemQuad.getPos().x = (alignmentAmount * itemDiff - longest / 2) * itemQuad.giveRelativeWidth() * itemScale + pos.x;
                itemQuad.getPos().y = -DynamicText.LINE_SPACING * itemScale * (index + 1) * itemQuad.giveRelativeHeight() + pos.y;

                if (!item.isBuffered()) {
                    item.buffer();
                }

                item.render();
                index++;
            }
            iterator.getPos().x = items.get(selected).getQuad().getPos().x;
            iterator.getPos().x -= 2.0f * items.get(selected).getQuad().giveRelativeWidth() * itemScale;
            iterator.getPos().y = items.get(selected).getQuad().getPos().y;
            if (!iterator.isBuffered()) {
                iterator.buffer();
            }
            iterator.render();
        }
    }

    public void selectPrev() {
        selected--;
        if (selected < 0) {
            selected = items.size() - 1;
        }
        iterator.setColor(items.get(selected).getQuad().getColor());
    }

    public void selectNext() {
        selected++;
        if (selected > items.size() - 1) {
            selected = 0;
        }
        iterator.setColor(items.get(selected).getQuad().getColor());
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public void setMyWindow(Window myWindow) {
        this.myWindow = myWindow;
    }

    public Quad getLogo() {
        return logo;
    }

    public void setLogo(Quad logo) {
        this.logo = logo;
    }

    public String getFileName() {
        return fileName;
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

    public int getSelected() {
        return selected;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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
