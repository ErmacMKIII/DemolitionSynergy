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
package rs.alexanderstojanovich.evg.level;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class TexByte {

    protected String texName;
    protected byte byteValue;
    protected boolean solid;

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

}
