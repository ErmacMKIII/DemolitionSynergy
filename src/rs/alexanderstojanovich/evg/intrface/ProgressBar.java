/*
 * Copyright (C) 2024 coas9
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

import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * Progress Bar. Could be used in conjuction with text.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class ProgressBar {

    protected final Quad quad;
    protected int value = 0;

    protected final int increment;

    public ProgressBar(int width, int height, Texture texture, Intrface intrface) {
        this.increment = width;
        this.quad = new Quad(0, height, texture, intrface);
    }

    /**
     * Get percentage value
     *
     * @return percentage
     */
    public int getValue() {
        return value;
    }

    /**
     * Set percentage value
     *
     * @param value value to set
     * @param ifc intrface
     */
    public void setValue(int value, Intrface ifc) {
        this.value = value;
        this.quad.setWidth(increment * (int) value);
    }

    /**
     * Render progress bar to the interface
     *
     * @param ifc interface provided
     * @param sp shader program (ifc one or contour ifc)
     */
    public void render(Intrface ifc, ShaderProgram sp) {
        if (!this.quad.isBuffered()) {
            this.quad.bufferSmart(ifc);
        }

        this.quad.render(ifc, sp);
    }

    public Quad getQuad() {
        return quad;
    }

    public int getIncrement() {
        return increment;
    }

}
