package rs.alexanderstojanovich.evg.net;

import com.google.gson.Gson;
import org.joml.Vector4f;

/**
 * Player information class.
 *
 * Author: Alexander Stojanovich <coas91@rocketmail.com>
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
     * Player weapon
     */
    public final String weapon;

    /**
     * Create new Player Info (Json)
     *
     * @param name player name
     * @param texModel player (character) texture model
     * @param uniqueId player unique id
     * @param color player color (of body)
     * @param weapon player weapon
     */
    public PlayerInfo(String name, String texModel, String uniqueId, Vector4f color, String weapon) {
        this.name = name;
        this.texModel = texModel;
        this.uniqueId = uniqueId;
        this.color = color;
        this.weapon = weapon;
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

    /**
     * Get Player weapon
     *
     * @return player weapon
     */
    public String getWeapon() {
        return weapon;
    }

}
