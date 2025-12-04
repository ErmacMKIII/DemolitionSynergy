/* 
 * Copyright (C) 2020 Aleksandar Stojanovic <coas91@rocketmail.com>
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
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.resources.Assets;

/**
 * Is non-player capabilities. Doesn't have camera.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class NPC extends Critter {

    public NPC(Assets assets, Model body) {
        super(assets, body);
    }

    public NPC(Assets assets, Vector3f pos, Model body) {
        super(assets, pos, body);
    }

    public NPC(Assets assets, String uniqueId, Model body) {
        super(assets, uniqueId, body);
    }

}
