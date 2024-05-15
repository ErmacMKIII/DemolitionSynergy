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

import java.io.IOException;
import java.net.Socket;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameServer;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface RequestIfc extends DSObject {

    /**
     * Magic bytes of DSynergy request
     */
    public static final byte[] MAGIC_BYTES = {(byte) 0xAB, (byte) 0xCD, (byte) 0x0D, (byte) 0x13}; // 4 Bytes

    /**
     * Allowed request type. To send to DSynergy server.
     */
    public static enum RequestType {
        /**
         * Occurs on server side if invalid request is received
         */
        INVALID,
        /**
         * Send hello request to authenticate game client
         */
        HELLO,
        /**
         * Get ingame time from server
         */
        GET_TIME,
        /**
         * Set ingame time on the server
         */
        SET_TIME,
        /**
         * Get player position from the server
         */
        GET_POS,
        /**
         * Set player position on the server
         */
        SET_POS,
        /**
         * Send chat message to the server (global)
         */
        SAY,
        /**
         * Ping the server. Trip round-time.
         */
        PING,
        /**
         * Send goodbye-disconnect request to leave the server
         */
        GOODBYE,
        /**
         * Request Download Level (Map)
         */
        DOWNLOAD,
        /**
         * Register player (UUID) to game server
         */
        REGISTER
    }

    /**
     * Get Request Type. One of the {HELLO, UPDATE, HANDLE_INPUT, SYNC_TIME,
     * SAY, LOAD_CHUNK, GOODBYE }.
     *
     * @return Request Type
     */
    public RequestType getRequestType();

    /**
     * Send request to server endpoint.
     *
     * @param client game client
     * @param endpoint endpoint to (game) server.
     * @throws java.lang.Exception
     */
    public void send(Game client, Socket endpoint) throws Exception;

    /**
     * Receive request from client endpoint.
     *
     * @param server game server
     * @param endpoint endpoint to (game) client.
     * @return null if deserialization failed otherwise valid request
     * @throws java.io.IOException if network error
     */
    public static RequestIfc receive(GameServer server, Socket endpoint) throws IOException, Exception {
        final byte[] content = new byte[512];
        final int totalBytes = endpoint.getInputStream().read(content);
        if (totalBytes > 0) {
            RequestIfc result = (RequestIfc) new Request().deserialize(content); // new request

            return result;
        }

        return Request.INVALID;
    }
}
