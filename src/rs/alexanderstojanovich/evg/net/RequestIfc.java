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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameServer;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
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
         * Get player position & view from the server
         */
        GET_POS,
        /**
         * Set player position & view on the server
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
         * Get (Download) Level (Map) N-Fragment
         */
        GET_FRAGMENT,
        /**
         * Confirm received packets (mandatory for UDP)
         */
        CONFIRM,
        /**
         * Register player (UUID) to game server
         */
        REGISTER,
        /**
         * Get player info from all players on the server as Json array (or
         * list)
         */
        GET_PLAYER_INFO,
        /**
         * Force disconnect to player
         */
        KICK_PLAYER,
        /**
         * Get world info {world name, size in bytes & checksum)
         */
        WORLD_INFO,
        /**
         * Update player info from client (Key: Guid). Similar to register.
         */
        SET_PLAYER_INFO
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
     * @param session connection session
     * @throws java.lang.Exception
     */
    public void send(Game client, IoSession session) throws Exception;

    /**
     * Get client address from this request
     *
     * @return client address
     */
    public InetAddress getClientAddress();

    /**
     * Get client port from this request
     *
     * @return client port
     */
    public int getClientPort();

    /**
     * Receive request from client endpoint.
     *
     * @param server game server
     * @param session connection session
     * @param message message received
     * @return null if deserialization failed otherwise valid request
     *
     * @throws java.io.IOException if network error
     */
    public static RequestIfc receive(GameServer server, IoSession session, Object message) throws Exception {
        // Get message as Byte Buffer
        if (message instanceof IoBuffer) {
            IoBuffer buffer = (IoBuffer) message;

            // Get client's socket address
            SocketAddress cliSockAddr = session.getRemoteAddress();
            if (cliSockAddr instanceof InetSocketAddress) {
                InetSocketAddress inetCliSocketAddress = (InetSocketAddress) cliSockAddr;

                InetAddress clientAddress = inetCliSocketAddress.getAddress();
                int clientPort = inetCliSocketAddress.getPort();

                // read data
                int dataContentLength = buffer.remaining() - Long.BYTES;
                byte[] dataContent = new byte[dataContentLength];
                buffer.get(dataContent);

                // read checksum
                long checksum = buffer.getLong();

                // Construct request (involves deserialization)
                RequestIfc result = (RequestIfc) new Request(clientAddress, clientPort, checksum).deserialize(dataContent); // new request                

                return result;
            }

            return Request.INVALID;
        }

        return Request.INVALID;
    }

    /**
     * Get sender guid
     *
     * @return sender guid (of this request)
     */
    public String getSenderGuid();

    /**
     * Get the timestamp associated with the request.
     *
     * @return the timestamp as a long
     */
    public long getTimestamp();

    /**
     * Get globally unique id of this request
     *
     * @return (globally) unique id
     */
    @Override
    public String getId();
}
