/*
 * Copyright (C) 2024 coas9
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
 * DSObject is common term for Request and Response in DSynergy.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class LevelMapInfo {

    /**
     * world name obtained from server
     */
    public final String worldname;
    /**
     * checksum of the level
     */
    public final long chksum;
    /**
     * size of level in bytes
     */
    public final long sizebytes;

    /**
     * NULL Level Info. Initial value when send request.
     */
    public static final LevelMapInfo NULL = new LevelMapInfo(null, 0L, -1L);

    /**
     * Blank Level Info. Not used.
     */
    public static final LevelMapInfo BLANK = new LevelMapInfo("", 0L, 0L);

    /**
     * Level Map Info object
     *
     * @param worldname world name obtained from server
     * @param chksum checksum of the level
     * @param sizebytes size of level in bytes (filesystem)
     */
    public LevelMapInfo(String worldname, long chksum, long sizebytes) {
        this.worldname = worldname;
        this.chksum = chksum;
        this.sizebytes = sizebytes;
    }

    /**
     * Serialize Json to string
     *
     * @return serialized json to string
     */
    @Override
    public String toString() {
        return new Gson().toJson(this, LevelMapInfo.class);
    }

    /**
     * Deserialize to object (instance).
     *
     * @param json json string
     * @return deserialized json
     */
    public static LevelMapInfo fromJson(String json) {
        return new Gson().fromJson(json, LevelMapInfo.class);
    }
}
