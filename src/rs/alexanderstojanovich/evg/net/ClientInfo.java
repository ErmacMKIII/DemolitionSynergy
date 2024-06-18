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

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class ClientInfo {

    /**
     * Client IP (or hostname)
     */
    public final String hostName;

    /**
     * Unique player id
     */
    public final String uniqueId;

    /**
     * Time to live
     */
    public final int timeToLive;

    public ClientInfo(String hostName, String uniqueId, int timeToLive) {
        this.hostName = hostName;
        this.uniqueId = uniqueId;
        this.timeToLive = timeToLive;
    }

    /**
     * Serialize Json to string
     *
     * @return serialized json to string
     */
    @Override
    public String toString() {
        return new Gson().toJson(this, ClientInfo.class);
    }

    /**
     * Deserialize to object (instance).
     *
     * @param json json string
     * @return deserialized json
     */
    public static ClientInfo fromJson(String json) {
        return new Gson().fromJson(json, ClientInfo.class);
    }

    public String getHostName() {
        return hostName;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

}
