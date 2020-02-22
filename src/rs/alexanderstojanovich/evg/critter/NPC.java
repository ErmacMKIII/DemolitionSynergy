/*
 * Copyright (C) 2020 Coa
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
package rs.alexanderstojanovich.evg.critter;

import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Coa
 */
public class NPC extends Observer {

    public NPC(String modelFileName, Texture texture, Vector3f pos, Vector4f color, float scale) {
        super(modelFileName, texture, pos, color, scale);
    }

    public NPC(Camera camera, Model model) {
        super(camera, model);
    }

}
