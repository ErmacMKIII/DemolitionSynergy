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
package rs.alexanderstojanovich.evg.models;

import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 *
 * @author Coa
 */
public class Vertex {

    public static final int SIZE = 8; // size in floats -> it means 8 floats

    private Vector3f pos;
    private Vector3f normal;
    private Vector2f uv;
    private boolean enabled = true;

    public Vertex(float x, float y, float z) {
        this.pos = new Vector3f(x, y, z);
        this.normal = new Vector3f();
        this.uv = new Vector2f();
    }

    public Vertex(float pos_x, float pos_y, float pos_z, float norm_x, float norm_y, float norm_z) {
        this.pos = new Vector3f(pos_x, pos_y, pos_z);
        this.normal = new Vector3f(norm_x, norm_y, norm_z);
        this.uv = new Vector2f();
    }

    public Vertex(float pos_x, float pos_y, float pos_z, float norm_x, float norm_y, float norm_z, float uv_u, float uv_v) {
        this.pos = new Vector3f(pos_x, pos_y, pos_z);
        this.normal = new Vector3f(norm_x, norm_y, norm_z);
        this.uv = new Vector2f(uv_u, uv_v);
    }

    public Vertex(Vector3f pos) {
        this.pos = pos;
        this.normal = new Vector3f();
        this.uv = new Vector2f();
    }

    public Vertex(Vector3f pos, Vector3f normal) {
        this.pos = pos;
        this.normal = normal;
    }

    public Vertex(Vector3f pos, Vector3f normal, Vector2f uv) {
        this.pos = pos;
        this.normal = normal;
        this.uv = uv;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vertex) {
            Vertex that = (Vertex) obj;
            return (this.pos.equals(that.pos)
                    && this.normal.equals(that.normal)
                    && this.uv.equals(that.uv));
        }
        return false;
    }

    public Vector3f getPos() {
        return pos;
    }

    public void setPos(Vector3f pos) {
        this.pos = pos;
    }

    public Vector3f getNormal() {
        return normal;
    }

    public void setNormal(Vector3f normal) {
        this.normal = normal;
    }

    public Vector2f getUv() {
        return uv;
    }

    public void setUv(Vector2f uv) {
        this.uv = uv;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
