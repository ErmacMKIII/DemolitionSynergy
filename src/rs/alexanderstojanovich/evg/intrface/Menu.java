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
    protected Text title;

    protected String fileName;
    protected boolean enabled = false;

    protected Vector2f pos = new Vector2f();
    protected List<Text> items = new ArrayList<>();

    protected int selected = 0;

    protected Quad quad;

    protected float scale;

    protected float alignmentAmount = 0.0f;

    public Menu(Window window, String title, String fileName, String textureFileName) {
        this.myWindow = window;
        this.fileName = fileName;
        this.title = new Text(myWindow, textureFileName, title);
        this.scale = 1.0f;
        this.title.setScale(scale);
        this.title.setColor(new Vector3f(1.0f, 1.0f, 0.0f));                                
        readFromFile(fileName, textureFileName);
        Texture mngTexture = new Texture("minigun.png");
        quad = new Quad(window, 27, 27, mngTexture);
        quad.getPos().x = items.get(selected).getPos().x;
        quad.getPos().x -= items.get(selected).giveRelativeWidth();
        quad.getPos().y = items.get(selected).getPos().y;
        quad.setColor(items.get(selected).getColor());
        quad.setScale(scale);
    }

    public Menu(Window window, String title, String fileName, String textureFileName, Vector2f pos, float scale) {
        this.myWindow = window;
        this.fileName = fileName;
        this.title = new Text(myWindow, textureFileName, title);
        this.scale = scale;
        this.title.setScale(this.scale);
        this.title.setColor(new Vector3f(1.0f, 1.0f, 0.0f));
        this.enabled = false;
        this.pos = pos;                        
        readFromFile(fileName, textureFileName);
        Texture mngTexture = new Texture("minigun.png");
        quad = new Quad(window, 27, 27, mngTexture);
        quad.getPos().x = items.get(selected).getPos().x;
        quad.getPos().y = items.get(selected).getPos().y;
        quad.getColor().x = items.get(selected).getColor().x;
        quad.getPos().x -= items.get(selected).giveRelativeWidth();
        quad.getColor().y = items.get(selected).getColor().y;
        quad.setColor(items.get(selected).getColor());
        quad.setScale(scale);
    }

    private void readFromFile(String fileName, String textureFileName) {
        InputStream in = getClass().getResourceAsStream(Game.RESOURCES_DIR + fileName);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                String[] things = line.trim().split(":");
                Text item = new Text(myWindow, textureFileName, things[0]);
                item.setScale(scale);
                if (Boolean.parseBoolean(things[1].trim())) {
                    item.getColor().x = 0.0f;
                    item.getColor().y = 1.0f;
                    item.getColor().z = 0.0f;
                } else {
                    item.getColor().x = 1.0f;
                    item.getColor().y = 0.0f;
                    item.getColor().z = 0.0f;
                }
                item.getPos().x = pos.x;
                item.getPos().y = -Text.LINE_SPACING * items.size() * item.giveRelativeHeight() + pos.y;
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
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getContent().length() > longest) {
                longest = items.get(i).getContent().length();
            }
        }
        return longest;
    }

    public void render() {
        if (enabled) {
            int longest = longestWord();
            title.getPos().x = alignmentAmount * (longest - title.getContent().length())
                    * title.giveRelativeWidth() + pos.x;
            title.getPos().y = Text.LINE_SPACING * title.giveRelativeHeight() + pos.y;
            title.render();
            if (logo != null && title.getContent().equals("")) {
                logo.getPos().x = alignmentAmount * (longest - title.getContent().length())
                        * title.giveRelativeWidth() + pos.x;
                logo.getPos().y = logo.giveRelativeHeight() + pos.y;
                logo.render();
            }

            for (int i = 0; i < items.size(); i++) {
                items.get(i).getPos().x = alignmentAmount * (longest - items.get(i).getContent().length())
                        * items.get(i).giveRelativeWidth() + pos.x;
                items.get(i).getPos().y = -Text.LINE_SPACING * (i + 1) * items.get(i).giveRelativeHeight() + pos.y;
                items.get(i).render();
            }
            quad.getPos().x = items.get(selected).getPos().x;
            quad.getPos().x -= 2 * items.get(selected).giveRelativeWidth();
            quad.getPos().y = items.get(selected).getPos().y;
            quad.render();
        }
    }

    public void selectPrev() {
        selected--;
        if (selected < 0) {
            selected = items.size() - 1;
        }
        quad.setColor(items.get(selected).getColor());
    }

    public void selectNext() {
        selected++;
        if (selected > items.size() - 1) {
            selected = 0;
        }
        quad.setColor(items.get(selected).getColor());
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

    public List<Text> getItems() {
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

    public void setItems(List<Text> items) {
        this.items = items;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

    public Quad getQuad() {
        return quad;
    }

    public void setQuad(Quad quad) {
        this.quad = quad;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getAlignmentAmount() {
        return alignmentAmount;
    }

    public void setAlignmentAmount(float alignmentAmount) {
        this.alignmentAmount = alignmentAmount;
    }

}
