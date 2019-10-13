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
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.core.Texture;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author Coa
 */
public class Quad {

    private Window myWindow;

    private float width;
    private float height;
    private Texture texture;

    private Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
    private float scale = 1.0f;

    private Vector2f pos = new Vector2f();
    private boolean enabled = true;

    private boolean ignoreFactor = false;

    public Quad(Window window, float width, float height, Texture texture) {
        this.myWindow = window;
        this.width = width;
        this.height = height;
        this.texture = texture;
    }

    public Quad(Window window, float width, float height, Texture texture, boolean ignoreFactor) {
        this.myWindow = window;
        this.width = width;
        this.height = height;
        this.texture = texture;
        this.ignoreFactor = ignoreFactor;
    }

    public void render() {
        if (enabled) {
            float x = giveRelativeWidth();
            float y = giveRelativeHeight();
            Texture.enable();
            texture.bind();
            GL11.glColor4f(color.x, color.y, color.z, 1.0f);
            GL11.glBegin(GL11.GL_QUADS);

            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex2f(-x + pos.x, -y + pos.y);

            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex2f(x + pos.x, -y + pos.y);

            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex2f(x + pos.x, y + pos.y);

            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex2f(-x + pos.x, y + pos.y);

            GL11.glEnd();
            Texture.unbind();
            Texture.disable();
        }
    }

    public void renderReversed() {
        if (enabled) {
            float x = giveRelativeWidth();
            float y = giveRelativeHeight();
            Texture.enable();
            texture.bind();
            GL11.glColor4f(color.x, color.y, color.z, 1.0f);
            GL11.glBegin(GL11.GL_QUADS);

            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex2f(-x + pos.x, -y + pos.y);

            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex2f(x + pos.x, -y + pos.y);

            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex2f(x + pos.x, y + pos.y);

            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex2f(-x + pos.x, y + pos.y);

            GL11.glEnd();
            Texture.unbind();
            Texture.disable();
        }
    }

    public float giveRelativeWidth() {
        float widthFactor = (ignoreFactor) ? 1.0f : myWindow.getWidth() / Window.MIN_WIDTH;
        return width * widthFactor * scale / myWindow.getWidth();
    }

    public float giveRelativeHeight() {
        float heightFactor = (ignoreFactor) ? 1.0f : myWindow.getHeight() / Window.MIN_HEIGHT;
        return height * heightFactor * scale / myWindow.getHeight();
    }

    public Window getWindow() {
        return myWindow;
    }

    public void setWindow(Window window) {
        this.myWindow = window;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public Vector3f getColor() {
        return color;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public Vector2f getPos() {
        return pos;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIgnoreFactor() {
        return ignoreFactor;
    }

    public void setIgnoreFactor(boolean ignoreFactor) {
        this.ignoreFactor = ignoreFactor;
    }

}
