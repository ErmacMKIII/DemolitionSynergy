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
import java.util.Date;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class ClientInfo {

    /**
     * Session to the endpoint
     */
    public final IoSession session;

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
    public int timeToLive;

    /**
     * Date assigned
     */
    public final Date dateAssigned = new Date(System.currentTimeMillis());

    /**
     * Request per second (rps or qps/query per second)
     */
    public int requestPerSecond = 0;

    /**
     * Failed hosts with number of attempts
     */
    public int failedAttempts = 0;

    /**
     * Create new client info for (game) server purposes.
     *
     * @param session session (MINA) between client and server
     * @param hostName client hostname
     * @param uniqueId client guid
     * @param timeToLive client ttl (Def: 120 sec)
     */
    public ClientInfo(IoSession session, String hostName, String uniqueId, int timeToLive) {
        this.session = session;
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

    /**
     * Apache MINA Session
     *
     * @return Apache MINA Session
     */
    public IoSession getSession() {
        return session;
    }

    /**
     * Client hostname (IP or name used to connect)
     *
     * @return
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Player unique id (upon registration). Determined by client.
     *
     * @return player unique id
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Time to live in seconds. When reach zero client will removed and/or
     * kicked.
     *
     * @return time to live (seconds).
     */
    public int getTimeToLive() {
        return timeToLive;
    }

    /**
     * Date Assigned
     *
     * @return date assigned
     */
    public Date getDateAssigned() {
        return dateAssigned;
    }

    /**
     * Request per second
     *
     * @return request per second
     */
    public int getRequestPerSecond() {
        return requestPerSecond;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

}
