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

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
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
     * @throws java.lang.Exception if network error
     */
    public static ResponseIfc receive(Game client) throws Exception {
        final byte[] buff = new byte[BUFF_SIZE];
        DatagramPacket p = new DatagramPacket(buff, buff.length);
        client.getServerEndpoint().receive(p);

        byte[] dataContent = Arrays.copyOfRange(p.getData(), 0, p.getLength() - Long.BYTES);
        byte[] dataChksum = Arrays.copyOfRange(p.getData(), p.getLength() - Long.BYTES, p.getLength());
        long checksum = Long.reverseBytes(new BigInteger(dataChksum).longValue());

        ResponseIfc result = (ResponseIfc) new Response(checksum).deserialize(dataContent); // new response

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
        return CompletableFuture.supplyAsync(() -> {
            try {
                return receive(client);
            } catch (Exception e) {
                return Response.INVALID;
            }
        });
    }
}
