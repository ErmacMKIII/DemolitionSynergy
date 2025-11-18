/*
 * Copyright (C) 2022 Alexander Stojanovich <coas91@rocketmail.com>
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
package rs.alexanderstojanovich.evg.location;

import java.util.Objects;
import org.joml.Vector4f;

/**
 * Location property. Used in {@link BlockLocation} class.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class TexByte {

    /**
     * Block color
     */
    public final Vector4f color;
    /**
     * Texture Name
     */
    public final String texName;
    /**
     * Byte value (facebits)
     */
    public byte byteValue;
    /**
     * Solid property
     */
    public final boolean solid;
    /**
     * Unique block id (provide faster search)
     */
    public final int blkId;

    /**
     * Get properties of block.
     *
     * @param color block color
     * @param texName block texture name
     * @param solid is block solid
     * @param blkId block id (primary key) occupying the slot
     */
    public TexByte(Vector4f color, String texName, boolean solid, int blkId) {
        this.color = color;
        this.texName = texName;
        this.solid = solid;
        this.blkId = blkId;
    }

    /**
     * Get properties of block.
     *
     * @param color block color
     * @param texName block texture name
     * @param byteValue facebits of the block
     * @param solid is block solid
     * @param blkId block id (primary key) occupying the slot
     */
    public TexByte(Vector4f color, String texName, byte byteValue, boolean solid, int blkId) {
        this.color = color;
        this.texName = texName;
        this.byteValue = byteValue;
        this.solid = solid;
        this.blkId = blkId;
    }

    /**
     * Get block color (RGBA)
     *
     * @return vec4 RGBA of color
     */
    public Vector4f getColor() {
        return color;
    }

    /**
     * Get texture name of block occupying the place
     *
     * @return occupying block texture name
     */
    public String getTexName() {
        return texName;
    }

    /**
     * Get facebits of block occupying the place
     *
     * @return occupying block face bits
     */
    public byte getByteValue() {
        return byteValue;
    }

    /**
     * Get solid property of block occuping the place
     *
     * @return occupying block texture name
     */
    public boolean isSolid() {
        return solid;
    }

    /**
     * Get unique id (primary key) of this block
     *
     * @return
     */
    public int getBlkId() {
        return blkId;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.color);
        hash = 97 * hash + Objects.hashCode(this.texName);
        hash = 97 * hash + this.byteValue;
        hash = 97 * hash + (this.solid ? 1 : 0);
        hash = 97 * hash + this.blkId;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TexByte other = (TexByte) obj;
        if (this.byteValue != other.byteValue) {
            return false;
        }
        if (this.solid != other.solid) {
            return false;
        }
        if (this.blkId != other.blkId) {
            return false;
        }
        if (!Objects.equals(this.texName, other.texName)) {
            return false;
        }
        return Objects.equals(this.color, other.color);
    }

}
