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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.joml.Vector2f;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Coa
 */
public class Text {

    protected Window myWindow;
    protected Texture texture;
    protected String content;

    protected static final int GRID_SIZE = 16;
    protected static final float CELL_SIZE = 1.0f / GRID_SIZE;
    public static final float LINE_SPACING = 1.35f;

    protected final Quad quad;

    protected boolean enabled;

    public static final int STD_FONT_WIDTH = 24;
    public static final int STD_FONT_HEIGHT = 24;

    public static String readFromFile(String fileName) {
        File file = new File(Game.DATA_ZIP);
        if (!file.exists()) {
            DSLogger.reportError("Cannot find zip archive " + Game.DATA_ZIP + "!", null);
            return null;
        }
        StringBuilder text = new StringBuilder();
        ZipFile zipFile = null;
        BufferedReader br = null;
        try {
            zipFile = new ZipFile(file);
            InputStream txtInput = null;
            for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                if (zipEntry.getName().equals(Game.INTRFACE_ENTRY + fileName)) {
                    txtInput = zipFile.getInputStream(zipEntry);
                }
            }
            br = new BufferedReader(new InputStreamReader(txtInput));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line).append("\n");
            }
            br.close();
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                    DSLogger.reportFatalError(ex.getMessage(), ex);
                }
            }
        }
        return text.toString();
    }

    public Text(Window window, Texture texture, String content) {
        this.myWindow = window;
        this.texture = texture;
        this.content = content;
        this.quad = new Quad(window, STD_FONT_WIDTH, STD_FONT_HEIGHT, texture);
        this.enabled = true;
    }

    public Text(Window window, Texture texture, String content, Vector3f color, Vector2f pos) {
        this.myWindow = window;
        this.texture = texture;
        this.content = content;
        this.quad = new Quad(window, STD_FONT_WIDTH, STD_FONT_HEIGHT, texture);
        quad.setPos(pos);
        quad.setColor(color);
        this.enabled = true;
    }

    public Text(Window window, Texture texture, String content, Vector2f pos, int charWidth, int charHeight) {
        this.myWindow = window;
        this.texture = texture;
        this.content = content;
        this.quad = new Quad(window, charWidth, charHeight, texture);
        quad.setPos(pos);
        this.enabled = true;
    }

    public void render() {
        if (enabled) {
            String[] lines = content.split("\n");
            for (int l = 0; l < lines.length; l++) {
                for (int i = 0; i < lines[l].length(); i++) {
                    int j = i % 64;
                    int k = i / 64;
                    int asciiCode = (int) (lines[l].charAt(i));

                    float cellU = (int) (asciiCode % GRID_SIZE) * CELL_SIZE;
                    float cellV = (int) (asciiCode / GRID_SIZE) * CELL_SIZE;

                    float xinc = j;
                    float ydec = k + l * LINE_SPACING;

                    quad.getUvs()[0].x = cellU;
                    quad.getUvs()[0].y = cellV + CELL_SIZE;

                    quad.getUvs()[1].x = cellU + CELL_SIZE;
                    quad.getUvs()[1].y = cellV + CELL_SIZE;

                    quad.getUvs()[2].x = cellU + CELL_SIZE;
                    quad.getUvs()[2].y = cellV;

                    quad.getUvs()[3].x = cellU;
                    quad.getUvs()[3].y = cellV;

                    quad.buffer();

                    quad.render(xinc, ydec);
                }
            }
        }
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Quad getQuad() {
        return quad;
    }

}
