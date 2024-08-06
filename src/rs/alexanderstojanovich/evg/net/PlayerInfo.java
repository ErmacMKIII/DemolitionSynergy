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
import org.joml.Vector4f;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class PlayerInfo {

    /**
     * Player name
     */
    public final String name;
    /**
     * Player texture (body) model
     */
    public final String texModel;
    /**
     * Unique player id
     */
    public final String uniqueId;
    /**
     * Player color (of body)
     */
    public final Vector4f color;

    /**
     * Create new Player Info (Json)
     *
     * @param name player name
     * @param texModel player (character) texture model
     * @param uniqueId player unique id
     * @param color player color (of body)
     */
    public PlayerInfo(String name, String texModel, String uniqueId, Vector4f color) {
        this.name = name;
        this.texModel = texModel;
        this.uniqueId = uniqueId;
        this.color = color;
    }

    /**
     * Serialize Json to string
     *
     * @return serialized json to string
     */
    @Override
    public String toString() {
        return new Gson().toJson(this, PlayerInfo.class);
    }

    /**
     * Deserialize to object (instance).
     *
     * @param json json string
     * @return deserialized json
     */
    public static PlayerInfo fromJson(String json) {
        return new Gson().fromJson(json, PlayerInfo.class);
    }

    /**
     * Get Player name
     *
     * @return player name
     */
    public String getName() {
        return name;
    }

    /**
     * Player texture (body) model
     *
     * @return body model
     */
    public String getTexModel() {
        return texModel;
    }

    /**
     * Unique player id
     *
     * @return unique player id
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Player color (of body)
     *
     * @return player color of body
     */
    public Vector4f getColor() {
        return color;
    }

}
