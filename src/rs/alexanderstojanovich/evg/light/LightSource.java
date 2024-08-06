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
package rs.alexanderstojanovich.evg.light;

import java.util.Objects;
import org.joml.Vector3f;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LightSource {

    public static final float DEFAULT_LIGHT_INTENSITY = 8.0f;
    public static final float PLAYER_LIGHT_INTENSITY = 2.0f;

    public Vector3f pos;
    protected Vector3f color;
    protected float intensity;

    /**
     * Define light source with (pos, color, intensity)
     *
     * @param pos VEC3 pos
     * @param color VEC3 color
     * @param intensity float intensity
     */
    public LightSource(Vector3f pos, Vector3f color, float intensity) {
        this.pos = pos;
        this.color = color;
        this.intensity = intensity;
    }

    public Vector3f getPos() {
        return pos;
    }

    public Vector3f getColor() {
        return color;
    }

    public float getIntensity() {
        return intensity;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.pos);
        hash = 83 * hash + Objects.hashCode(this.color);
        hash = 83 * hash + Float.floatToIntBits(this.intensity);
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
        final LightSource other = (LightSource) obj;
        if (Float.floatToIntBits(this.intensity) != Float.floatToIntBits(other.intensity)) {
            return false;
        }
        if (!Objects.equals(this.pos, other.pos)) {
            return false;
        }
        return Objects.equals(this.color, other.color);
    }

    public void setPos(Vector3f pos) {
        this.pos = pos;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LightSource{");
        sb.append("pos=").append(pos);
        sb.append(", color=").append(color);
        sb.append(", intensity=").append(intensity);
//        sb.append(", lightViewMatrix=").append(lightViewMatrix);
        sb.append('}');
        return sb.toString();
    }

}
