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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameServer;
import rs.alexanderstojanovich.evg.util.DSLogger;

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
     * @param clientAddress (game) client address
     * @param clientPort client port
     * @throws java.lang.Exception if serialization fails
     */
    public void send(GameServer server, InetAddress clientAddress, int clientPort) throws Exception;

    /**
     * Receive response from server endpoint.
     *
     * @param client game client
     * @return Response.INVALID if deserialization failed otherwise valid
     * response
     * @throws java.io.IOException if network error
     */
    public static ResponseIfc receive(Game client) throws Exception {
        final byte[] content = new byte[BUFF_SIZE];
        DatagramPacket p = new DatagramPacket(content, content.length);
        client.getServerEndpoint().receive(p);
        ResponseIfc result = (ResponseIfc) new Response().deserialize(p.getData()); // new request

        return result;
    }

    /**
     * Receive async response from server endpoint. For game client. Provides
     * no-blocking.
     *
     * @param client game client
     * @return Response.INVALID if deserialization failed otherwise valid
     * response
     */
    public static CompletableFuture<ResponseIfc> receiveAsync(Game client) {
        CompletableFuture<ResponseIfc> future = CompletableFuture.supplyAsync(() -> {
            ResponseIfc result = Response.INVALID;
            try {
                final byte[] content = new byte[BUFF_SIZE];
                DatagramPacket p = new DatagramPacket(content, content.length);
                client.getServerEndpoint().receive(p);
                result = (ResponseIfc) new Response().deserialize(p.getData()); // new request                
            } catch (IOException ex) {
                DSLogger.reportError("Error with server, while getting response!", ex);
                client.gameObject.intrface.getConsole().write("Error with server, while getting response!", true);
                DSLogger.reportError(ex.getMessage(), ex);
                client.disconnectFromServer();
                client.gameObject.clearEverything();
            }

            return result;

        }).exceptionally((ex) -> {
            return Response.INVALID;
        });

        return future;
    }
}
