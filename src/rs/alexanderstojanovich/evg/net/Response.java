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

import java.net.Socket;
import rs.alexanderstojanovich.evg.main.GameServer;

/**
 * DSynergy Response implementation
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Response implements ResponseIfc {

    protected byte[] content;
    protected ResponseStatus responseStatus;
    protected DataType dataType;
    protected Object data;

    public Response() {

    }

    public Response(ResponseStatus responseStatus, DataType dataType, Object data) {
        this.responseStatus = responseStatus;
        this.dataType = dataType;
        this.data = data;
    }

    @Override
    public ObjType getObjType() {
        return ObjType.RESPONSE;
    }

    @Override
    public byte[] getContent() {
        return this.content;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public void serialize(DSMachine machine) throws Exception {
        final int mchType = machine.getMachineType().ordinal();
        final int objType = getObjType().ordinal(); // request or response
        final int statType = getResponseStatus().ordinal();
        final int datType = getDataType().ordinal();

        final int version = machine.getVersion();

        final byte[] first = {(byte) mchType, (byte) objType, (byte) statType, (byte) datType}; // 4 Bytes
        final byte[] second = {(byte) (version >> 24), (byte) (version >> 16), (byte) (version >> 8), (byte) (version)}; // 4 Bytes
        final byte[] magic = ResponseIfc.MAGIC_BYTES; // 4 Bytes

        if (getDataType() != DataType.VOID) {
            final byte[] third;
            switch (getDataType()) {
                case STRING:
                    String message = (String) data;
                    third = message.getBytes("UTF-8");
                    break;
                default:
                    throw new Exception("Serialization failed!");
            }

            // Serialize
            content = new byte[first.length + second.length + magic.length + third.length];
            System.arraycopy(first, 0, content, 0, first.length); // 4 Bytes
            System.arraycopy(second, 0, content, 4, second.length); // 4 Bytes
            System.arraycopy(third, 0, content, 8, third.length); // 4 Bytes             
            System.arraycopy(magic, 0, content, 12, magic.length); // 4 Bytes             
        } else {
            // Serialize
            content = new byte[first.length + second.length + magic.length];
            System.arraycopy(first, 0, content, 0, first.length); // 4 Bytes
            System.arraycopy(second, 0, content, 4, second.length); // 4 Bytes
            System.arraycopy(magic, 0, content, 8, magic.length); // 4 Bytes             
        }
    }

    @Override
    public boolean deserialize(DSMachine machine, byte[] content) throws Exception {
        if (content == null || content.length < 12) {
            // The content is invalid or too short
            return false;
        }

        // Extract machine type, object type, status type, and data type
        int mchType = content[0];
        int objType = content[1];
        int statType = content[2];
        int datType = content[3];

        // Extract version
        int version = (content[4] << 24) | (content[5] << 16) | (content[6] << 8) | content[7];

        // Verify magic bytes
        byte[] magic = ResponseIfc.MAGIC_BYTES;
        for (int i = 0; i < magic.length; i++) {
            if (content[i + 8] != magic[i]) {
                // Magic bytes don't match, invalid content
                return false;
            }
        }

        // Update Response fields
        this.responseStatus = ResponseStatus.values()[statType];
        this.dataType = DataType.values()[datType];
//        machine.setMachineType(MachineType.values()[mchType]);
//        machine.setVersion(version);

        // Extract data
        if (this.dataType != DataType.VOID) {
            switch (this.dataType) {
                case STRING:
                    String message = new String(content, 12, content.length - 12, "UTF-8");
                    this.data = message;
                    break;
                default:
                    // Unsupported data type
                    return false;
            }

        }

        return true;
    }

    @Override
    public void send(GameServer server, Socket endpoint) throws Exception {
        serialize(server);
        endpoint.getOutputStream().write(content);
    }

    @Override
    public ResponseStatus getResponseStatus() {
        return this.responseStatus;
    }

}
