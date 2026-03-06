/*
 * Copyright (C) 2023 coas91@rocketmail.com>
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
package rs.alexanderstojanovich.evg.texture;

/**
 * Texture value. Used in Texture atlas. Contains texture reference, value and grid size. Value is used for calculating texture coordinates, while grid size is used for calculating texture coordinates and atlas size.
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class TexValue {

    protected final TextureIfc texture;
    protected final int value;
    protected final int gridSize;

    /**
     * Create texture value.
     * @param texture texture reference
     * @param value texture value (used for calculating texture coordinates)
     * @param gridSize grid size (used for calculating texture coordinates and atlas size)
     */
    public TexValue(TextureIfc texture, int value, int gridSize) {
        this.texture = texture;
        this.value = value;
        this.gridSize = gridSize;
    }

    /**
     * Get texture reference.
     * @return texture reference
     */
    public TextureIfc getTexture() {
        return texture;
    }

    /**
     * Get texture value.
     * @return texture value
     */
    public int getValue() {
        return value;
    }

    /**
     * Get grid size.
     * @return grid size
     */
    public int getGridSize() {
        return gridSize;
    }

}
