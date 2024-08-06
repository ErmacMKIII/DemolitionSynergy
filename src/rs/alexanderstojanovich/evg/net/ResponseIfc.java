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

import java.util.concurrent.CompletableFuture;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameServer;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface ResponseIfc extends DSObject {

    /**
     * Response status
     */
    public static enum ResponseStatus {
        OK, ERR, INVALID
    }

    /**
     * Magic bytes of DSynergy response
     */
    public static final byte[] MAGIC_BYTES = {(byte) 0xAB, (byte) 0xCD, (byte) 0x0E, (byte) 0x14}; // 4 Bytes

    /**
     * Get Response Status (similar to HTTP REST services)
     *
     * @return
     */
    public ResponseStatus getResponseStatus();

    /**
     * Send response to client endpoint.
     *
     * @param server game server
     * @param session connection session
     * @throws java.lang.Exception if serialization fails
     */
    public void send(GameServer server, IoSession session) throws Exception;

    /**
     * Receive response from server endpoint.
     *
     * @param client game client
     * @param session connection session
     * @return Response.INVALID if deserialization failed otherwise valid
     * response
     * @throws java.lang.Exception if network error
     */
    public static ResponseIfc receive(Game client, IoSession session) throws Exception {
        Object message = session.read().await().getMessage();

        // Get message as Byte Buffer
        if (message instanceof IoBuffer) {
            IoBuffer buffer = (IoBuffer) message;

            // read data
            int dataContentLength = buffer.remaining() - Long.BYTES;
            byte[] dataContent = new byte[dataContentLength];
            buffer.get(dataContent);

            // read checksum
            long checksum = buffer.getLong();

            // Construct response (involves deserialization)
            ResponseIfc result = (ResponseIfc) new Response(checksum).deserialize(dataContent); // new request                

            return result;
        }

        return Response.INVALID;
    }

    /**
     * Receive async response from server endpoint. For game client. Provides
     * no-blocking.
     *
     * @param client game client
     * @param session connection session
     * @return Response.INVALID if deserialization failed otherwise valid
     * response
     */
    public static CompletableFuture<ResponseIfc> receiveAsync(Game client, IoSession session) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return receive(client, session);
            } catch (Exception e) {
                return Response.INVALID;
            }
        });
    }
}
