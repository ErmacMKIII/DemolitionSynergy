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

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Pair;

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
    public static final float LINE_SPACING = 1.5f;

    private final List<Quad> quadList = new GapList<>();
    private final List<Pair<Float, Float>> pairList = new ArrayList<>();

    protected boolean enabled;

    public static final int STD_FONT_WIDTH = 24;
    public static final int STD_FONT_HEIGHT = 24;

    protected Vector2f offset = new Vector2f();

    protected boolean buffered = false;

    protected Vector2f pos = new Vector2f();
    protected float scale = 1.0f;
    protected Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);

    protected int charWidth = STD_FONT_WIDTH;
    protected int charHeight = STD_FONT_HEIGHT;

    protected boolean ignoreFactor = false;

    public Text(Window window, Texture texture, String content) {
        this.myWindow = window;
        this.texture = texture;
        this.content = content;
        this.enabled = true;
    }

    public Text(Window window, Texture texture, String content, Vector3f color, Vector2f pos) {
        this.myWindow = window;
        this.texture = texture;
        this.content = content;
        this.color = color;
        this.pos = pos;
        this.enabled = true;
    }

    public Text(Window window, Texture texture, String content, Vector2f pos, int charWidth, int charHeight) {
        this.myWindow = window;
        this.texture = texture;
        this.content = content;
        this.enabled = true;
        this.charWidth = charWidth;
        this.charHeight = charHeight;
    }

    private void init() {
        quadList.clear();
        pairList.clear();
        String[] lines = content.split("\n");
        for (int l = 0; l < lines.length; l++) {
            for (int i = 0; i < lines[l].length(); i++) {
                int j = i % 64;
                int k = i / 64;
                int asciiCode = (int) (lines[l].charAt(i));

                float cellU = (int) (asciiCode % GRID_SIZE) * CELL_SIZE;
                float cellV = (int) (asciiCode / GRID_SIZE) * CELL_SIZE;

                float xinc = j + offset.x;
                float ydec = k + l * LINE_SPACING + offset.y;

                pairList.add(new Pair<>(xinc, ydec));

                Quad quad = new Quad(myWindow, charWidth, charHeight, texture);
                quad.setColor(color);
                quad.setPos(pos);
                quad.setScale(scale);

                quad.getUvs()[0].x = cellU;
                quad.getUvs()[0].y = cellV + CELL_SIZE;

                quad.getUvs()[1].x = cellU + CELL_SIZE;
                quad.getUvs()[1].y = cellV + CELL_SIZE;

                quad.getUvs()[2].x = cellU + CELL_SIZE;
                quad.getUvs()[2].y = cellV;

                quad.getUvs()[3].x = cellU;
                quad.getUvs()[3].y = cellV;

                quad.buffer();

                quadList.add(quad);
            }
        }
    }

    public void buffer() {
        init();
        buffered = true;
    }

    public void render() {
        if (enabled && buffered) {
            int index = 0;
            for (Quad quad : quadList) {
                Pair<Float, Float> pair = pairList.get(index);
                float xinc = pair.getKey();
                float ydec = pair.getValue();
                quad.render(xinc, ydec);
                index++;
            }
        }
    }

    public float getRelativeCharWidth() {
        float widthFactor = (ignoreFactor) ? 1.0f : myWindow.getWidth() / Window.MIN_WIDTH;
        return charWidth * widthFactor / (float) myWindow.getWidth();
    }

    public float getRelativeWidth() {
        float widthFactor = (ignoreFactor) ? 1.0f : myWindow.getWidth() / Window.MIN_WIDTH;
        return charWidth * widthFactor * content.length() / (float) myWindow.getWidth();
    }

    public float getRelativeCharHeight() {
        float heightFactor = (ignoreFactor) ? 1.0f : myWindow.getHeight() / Window.MIN_HEIGHT;
        return charHeight * heightFactor / (float) myWindow.getHeight();
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
        buffered = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Quad> getQuadList() {
        return quadList;
    }

    public List<Pair<Float, Float>> getPairList() {
        return pairList;
    }

    public Vector2f getOffset() {
        return offset;
    }

    public void setOffset(Vector2f offset) {
        this.offset = offset;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

    public int getCharWidth() {
        return charWidth;
    }

    public int getCharHeight() {
        return charHeight;
    }

    public boolean isIgnoreFactor() {
        return ignoreFactor;
    }

    public void setIgnoreFactor(boolean ignoreFactor) {
        this.ignoreFactor = ignoreFactor;
    }

    public Vector2f getPos() {
        return pos;
    }

    public float getScale() {
        return scale;
    }

    public Vector3f getColor() {
        return color;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }

    public void setPos(Vector2f pos) {
        this.pos = pos;
        buffered = false;
    }

    public void setScale(float scale) {
        this.scale = scale;
        buffered = false;
    }

}
