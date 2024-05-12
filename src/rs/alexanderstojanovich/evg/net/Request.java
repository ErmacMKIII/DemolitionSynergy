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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import rs.alexanderstojanovich.evg.main.Game;

/**
 * DSynergy Request implementation
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Request implements RequestIfc {

    protected byte[] content;
    protected RequestType requestType;
    protected DataType dataType;
    protected Object data;
    protected int version = 0;

    public Request() {

    }

    public Request(RequestType requestType, DataType dataType, Object data) {
        this.requestType = requestType;
        this.dataType = dataType;
        this.data = data;
    }

    @Override
    public ObjType getObjectType() {
        return ObjType.REQUEST;
    }

    @Override
    public byte[] getContent() {
        return this.content;
    }

    @Override
    public DataType getDataType() {
        return this.dataType;
    }

    @Override
    public Object getData() {
        return this.data;
    }

    @Override
    public void serialize(DSMachine machine) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        // Write magic bytes
        try (DataOutputStream out = new DataOutputStream(byteStream)) {
            // Write magic bytes
            out.write(RequestIfc.MAGIC_BYTES);

            // Write machine type, object type, request type, data type
            out.writeInt(machine.getMachineType().ordinal());
            out.writeInt(getObjectType().ordinal());
            out.writeInt(requestType.ordinal());
            out.writeInt(dataType.ordinal());

            // Write version
            out.writeInt(machine.getVersion());

            // Write data
            if (dataType != DataType.VOID) {
                switch (dataType) {
                    case STRING:
                        String message = (String) data;
                        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                        out.writeInt(messageBytes.length);
                        out.write(messageBytes);
                        break;
                    default:
                        throw new Exception("Unsupported data type during serialization!");
                }
            }
        }
        this.content = byteStream.toByteArray();
    }

    @Override
    public boolean deserialize(DSMachine machine, byte[] content) throws Exception {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(content);
        int reqTypeOrdinal;
        int dataTypeOrdinal;
        int version;
        // Read magic bytes
        try (DataInputStream in = new DataInputStream(byteStream)) {
            // Read magic bytes
            byte[] magicBytes = new byte[RequestIfc.MAGIC_BYTES.length];
            in.readFully(magicBytes);
            if (!Arrays.equals(magicBytes, RequestIfc.MAGIC_BYTES)) {
                return false; // Magic bytes mismatch
            }   // Read machine type, object type, request type, and data type
            int machineTypeOrdinal = in.readInt();
            int objTypeOrdinal = in.readInt();
            reqTypeOrdinal = in.readInt();
            dataTypeOrdinal = in.readInt();
            // Verify machine type, object type, and request type
            if (machineTypeOrdinal < 0 || machineTypeOrdinal >= DSMachine.MachineType.values().length
                    || objTypeOrdinal < 0 || objTypeOrdinal >= DSObject.ObjType.values().length
                    || reqTypeOrdinal < 0 || reqTypeOrdinal >= RequestType.values().length
                    || dataTypeOrdinal < 0 || dataTypeOrdinal >= DataType.values().length) {
                return false; // Invalid machine type, object type, request type, or data type
            }   // Read version
            version = in.readInt();
            // Read data
            switch (DataType.values()[dataTypeOrdinal]) {
                case STRING:
                    int stringLength = in.readInt();
                    byte[] stringBytes = new byte[stringLength];
                    in.readFully(stringBytes);
                    data = new String(stringBytes, StandardCharsets.UTF_8);
                    break;
                case VOID:
                    break;
                default:
                    throw new Exception("Unsupported data type during deserialization!");
            }
        }
        this.content = content;
        this.requestType = RequestType.values()[reqTypeOrdinal];
        this.dataType = DataType.values()[dataTypeOrdinal];
        this.version = version;
        return true;
    }

    @Override
    public RequestType getRequestType() {
        return this.requestType;
    }

    @Override
    public void send(Game client, Socket endpoint) throws Exception {
        serialize(client);
        endpoint.getOutputStream().write(content);
    }
}
