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
     * World name obtained from server
     */
    public String worldname;
    /**
     * Checksum of the level
     */
    public long chksum;
    /**
     * Size of level in bytes
     */
    public long sizebytes;

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
     * Clear level map info. Set worldname to null, chksum to 0 and sizebytes to -1.
     */
    public void clear() {
        this.worldname = "";
        this.chksum = 0L;
        this.sizebytes = -1L;
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

    public String getWorldname() {
        return worldname;
    }

    public void setWorldname(String worldname) {
        this.worldname = worldname;
    }

    public long getChksum() {
        return chksum;
    }

    public void setChksum(long chksum) {
        this.chksum = chksum;
    }

    public long getSizebytes() {
        return sizebytes;
    }

    public void setSizebytes(long sizebytes) {
        this.sizebytes = sizebytes;
    }

}
