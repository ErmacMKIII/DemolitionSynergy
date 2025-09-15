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
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Material {

    protected Vector4f ambient = new Vector4f(GlobalColors.WHITE, 1.0f);
    protected Vector4f diffuse = new Vector4f(GlobalColors.WHITE, 1.0f);
    protected Vector4f specular = new Vector4f(GlobalColors.WHITE, 1.0f);

    protected Vector4f color = new Vector4f(new Vector3f(GlobalColors.WHITE), 1.0f);
    protected Texture texture;

    public Material(Texture texture) {
        this.texture = texture;
    }

    public Material(Vector4f ambient, Vector4f diffuse, Vector4f specular, Texture texture) {
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
        this.texture = texture;
    }

    public Vector4f getAmbient() {
        return ambient;
    }

    public Vector4f getDiffuse() {
        return diffuse;
    }

    public Vector4f getSpecular() {
        return specular;
    }

    public Vector4f getColor() {
        return color;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setAmbient(Vector4f ambient) {
        this.ambient = ambient;
    }

    public void setDiffuse(Vector4f diffuse) {
        this.diffuse = diffuse;
    }

    public void setSpecular(Vector4f specular) {
        this.specular = specular;
    }

    public void setColor(Vector4f color) {
        this.color = color;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public Vector4f getLightColor() {
        float red = ambient.x + diffuse.x + specular.x;
        float green = ambient.y + diffuse.y + specular.y;
        float blue = ambient.z + diffuse.z + specular.z;
        float alpha = 1.0f;

        return new Vector4f(new Vector3f(red, green, blue), alpha);
    }

}
