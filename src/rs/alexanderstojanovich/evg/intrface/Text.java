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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.Vector2f;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.core.Texture;
import org.lwjgl.opengl.GL11;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.Shader;

/**
 *
 * @author Coa
 */
public class Text {

    private Window myWindow;
    private Texture texture;
    private String content;
    private Vector3f color;
    private Vector2f pos;

    private static final int GRID_SIZE = 16;
    private static final float CELL_SIZE = 1.0f / GRID_SIZE;
    public static final float LINE_SPACING = 1.15f;

    private float charWidth;
    private float charHeight;

    private float scale;

    private boolean enabled;

    public static String readFromFile(String fileName) {
        StringBuilder text = new StringBuilder();
        InputStream in = Text.class.getResourceAsStream(Game.RESOURCES_DIR + fileName);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line).append("\n");
            }
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Shader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Shader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return text.toString();
    }

    public Text(Window window, String textureFileName, String content) {
        this.myWindow = window;
        this.texture = new Texture(textureFileName);
        this.content = content;
        this.color = new Vector3f(1.0f, 1.0f, 1.0f);
        this.pos = new Vector2f();
        this.charWidth = 24;
        this.charHeight = 24;
        this.scale = 1.0f;
        this.enabled = true;
    }

    public Text(Window window, String textureFileName, String content, Vector3f color, Vector2f pos) {
        this.myWindow = window;
        this.texture = new Texture(textureFileName);
        this.content = content;
        this.color = color;
        this.pos = pos;
        this.charWidth = 24;
        this.charHeight = 24;
        this.scale = 1.0f;
        this.enabled = true;
    }

    public Text(Window window, String textureFileName, String content, Vector2f pos, float charWidth, float charHeight) {
        this.myWindow = window;
        this.texture = new Texture(textureFileName);
        this.content = content;
        this.pos = pos;
        this.charWidth = charWidth;
        this.charHeight = charHeight;
        this.scale = 1.0f;
        this.enabled = true;
    }

    public void render() {
        if (enabled) {
            String[] lines = content.split("\n");
            for (int l = 0; l < lines.length; l++) {
                for (int i = 0; i < lines[l].length(); i++) {
                    int j = i % 31;
                    int k = i / 31;
                    int asciiCode = (int) (lines[l].charAt(i));

                    float cellU = (int) (asciiCode % GRID_SIZE) * CELL_SIZE;
                    float cellV = (int) (asciiCode / GRID_SIZE) * CELL_SIZE;

                    float x = giveRelativeWidth();
                    float xinc = j * giveRelativeWidth();

                    float y = giveRelativeHeight();
                    float ydec = (k + l * LINE_SPACING) * giveRelativeHeight();

                    Texture.enable();
                    texture.bind();

                    GL11.glColor4f(color.x, color.y, color.z, 1.0f);

                    GL11.glBegin(GL11.GL_QUADS);

                    GL11.glTexCoord2f(cellU, cellV + CELL_SIZE);
                    GL11.glVertex2f(-x + xinc + pos.x, -y - ydec + pos.y);

                    GL11.glTexCoord2f(cellU + CELL_SIZE, cellV + CELL_SIZE);
                    GL11.glVertex2f(x + xinc + pos.x, -y - ydec + pos.y);

                    GL11.glTexCoord2f(cellU + CELL_SIZE, cellV);
                    GL11.glVertex2f(x + xinc + pos.x, y - ydec + pos.y);

                    GL11.glTexCoord2f(cellU, cellV);
                    GL11.glVertex2f(-x + xinc + pos.x, y - ydec + pos.y);

                    GL11.glEnd();

                    Texture.unbind();
                    Texture.disable();
                }
            }
        }
    }

    public float giveRelativeWidth() {
        float widthFactor = myWindow.getWidth() / Window.MIN_WIDTH;
        return scale * widthFactor * charWidth / myWindow.getWidth();
    }

    public float giveRelativeHeight() {
        float heightFactor = myWindow.getHeight() / Window.MIN_HEIGHT;
        return scale * heightFactor * charHeight / myWindow.getHeight();
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public void setMyWindow(Window myWindow) {
        this.myWindow = myWindow;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Vector3f getColor() {
        return color;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }

    public Vector2f getPos() {
        return pos;
    }

    public void setPos(Vector2f pos) {
        this.pos = pos;
    }

    public float getCharWidth() {
        return charWidth;
    }

    public void setCharWidth(float charWidth) {
        this.charWidth = charWidth;
    }

    public float getCharHeight() {
        return charHeight;
    }

    public void setCharHeight(float charHeight) {
        this.charHeight = charHeight;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
