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

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class TexByte {

    public String texName;
    public byte byteValue;
    public boolean solid;

    public TexByte(String texName, byte byteValue, boolean solid) {
        this.texName = texName;
        this.byteValue = byteValue;
        this.solid = solid;
    }

    public String getTexName() {
        return texName;
    }

    public void setTexName(String texName) {
        this.texName = texName;
    }

    public byte getByteValue() {
        return byteValue;
    }

    public void setByteValue(byte byteValue) {
        this.byteValue = byteValue;
    }

    public boolean isSolid() {
        return solid;
    }

    public void setSolid(boolean solid) {
        this.solid = solid;
    }

    @Override
    public String toString() {
        return "TexByte{" + "texName=" + texName + ", byteValue=" + byteValue + ", solid=" + solid + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 73 * hash + Objects.hashCode(this.texName);
        hash = 73 * hash + this.byteValue;
        hash = 73 * hash + (this.solid ? 1 : 0);
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
        return Objects.equals(this.texName, other.texName);
    }

}
