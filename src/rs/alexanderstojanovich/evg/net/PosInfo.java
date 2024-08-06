/*
 * Copyright (C) 2024 Alexander Stojanovich <coas91@rocketmail.com>
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
package rs.alexanderstojanovich.evg.net;

import com.google.gson.Gson;
import org.joml.Vector3f;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class PosInfo {

    /**
     * Unique critter id
     */
    public final String uniqueId;

    /**
     * Critter VEC3f position
     */
    public final Vector3f pos;

    /**
     * Critter VEC3f view
     */
    public final Vector3f front;

    /**
     * Create new position info (unique id and pos). Json structure.
     *
     * @param uniqueId critter unique id
     * @param pos player pos vec3f
     * @param front player front vec3f
     */
    public PosInfo(String uniqueId, Vector3f pos, Vector3f front) {
        this.uniqueId = uniqueId;
        this.pos = pos;
        this.front = front;
    }

    /**
     * Get Unique critter id
     *
     * @return unique critter id
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Get Critter VEC3f position
     *
     * @return VEC3f position
     */
    public Vector3f getPos() {
        return pos;
    }

    /**
     * Get Player front VEC3f (view vector)
     *
     * @return player VEC3f front
     */
    public Vector3f getFront() {
        return front;
    }

    /**
     * Serialize Json to string
     *
     * @return serialized json to string
     */
    @Override
    public String toString() {
        return new Gson().toJson(this, PosInfo.class);
    }

    /**
     * Deserialize to object (instance).
     *
     * @param json json string
     * @return deserialized json
     */
    public static PosInfo fromJson(String json) {
        return new Gson().fromJson(json, PosInfo.class);
    }

}
