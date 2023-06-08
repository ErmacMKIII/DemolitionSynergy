/*
 * Copyright (C) 2023 coas91@rocketmail.com
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

import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Material {

    protected Vector4f ambient = new Vector4f(Vector3fColors.WHITE, 1.0f);
    protected Vector4f diffuse = new Vector4f(Vector3fColors.WHITE, 1.0f);
    protected Vector4f specular = new Vector4f(Vector3fColors.WHITE, 1.0f);

    protected Vector3f color = new Vector3f(Vector3fColors.WHITE);
    protected float alpha = 1.0f;
    protected final Texture texture;

    public Material(Texture texture) {
        this.texture = texture;
    }

    public Material(Vector4f ambient, Vector4f diffuse, Vector4f specular, Texture texture) {
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
        this.texture = texture;
    }

    public Vector3f getColor() {
        return color;
    }

    public Texture getTexture() {
        return texture;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

}
