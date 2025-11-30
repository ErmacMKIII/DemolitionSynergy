/*
 * Copyright (C) 2024 Aleksandar Stojanovic <coas91@rocketmail.com>
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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.session.IoSession;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameServer;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
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
     * @param guid (recipient) guid
     * @param server game server
     * @param session connection session
     * @throws java.lang.Exception if serialization fails
     */
    public void send(String guid, GameServer server, IoSession session) throws Exception;

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
        ReadFuture read = session.read();
        Object message = null;

        // read message within specified timeout
        if (read.await(client.getTimeout())) {
            message = read.getMessage();
        }

        // Get message as Byte Buffer
        if (message instanceof IoBuffer) {
            IoBuffer buffer = (IoBuffer) message;

            // read data
            int dataContentLength = buffer.remaining() - Long.BYTES - ID_LENGTH;
            byte[] dataContent = new byte[dataContentLength];
            buffer.get(dataContent);

            // read checksum
            long checksum = buffer.getLong();

            // Get unique id
            String id = buffer.getString(ID_LENGTH, StandardCharsets.US_ASCII.newDecoder());

            // Construct response (involves deserialization)
            ResponseIfc result = (ResponseIfc) new Response(id, checksum).deserialize(dataContent); // new request                

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
     * @param executor executor service (async support)
     * @return Response.INVALID if deserialization failed otherwise valid
     * response
     */
    public static CompletableFuture<ResponseIfc> receiveAsync(Game client, IoSession session, ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!client.isAsyncReceivedEnabled()) {
                    return Response.INVALID;
                }
                return receive(client, session);
            } catch (Exception e) {
                return Response.INVALID;
            }
        }, executor);
    }

    /**
     * Return (recipient) guid.
     *
     * @return recipient guid
     */
    public String getReceiverGuid();

    /**
     * Get globally unique id of the request to which this response belongs to
     *
     * @return (globally) unique id
     */
    @Override
    public String getId();
}
