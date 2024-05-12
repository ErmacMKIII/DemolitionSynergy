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
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.util.Arrays;
import rs.alexanderstojanovich.evg.main.GameServer;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.VOID;

public class Response implements ResponseIfc {

    protected byte[] content;
    protected ResponseStatus responseStatus;
    protected DataType dataType;
    protected Object data;

    protected DSMachine.MachineType machineType;
    protected ObjType objectType;
    protected int version = 0;

    public Response() {

    }

    public Response(ResponseStatus responseStatus, DataType dataType, Object data) {
        this.responseStatus = responseStatus;
        this.dataType = dataType;
        this.data = data;
    }

    @Override
    public ObjType getObjectType() {
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
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        // Write magic bytes
        try (DataOutputStream out = new DataOutputStream(byteStream)) {
            // Write magic bytes
            out.write(ResponseIfc.MAGIC_BYTES);

            // Write machine type, object type, status type, and data type
            out.writeInt(machine.getMachineType().ordinal());
            out.writeInt(getObjectType().ordinal());
            out.writeInt(responseStatus.ordinal());
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
                    case VOID:
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
        // Read magic bytes
        try (DataInputStream in = new DataInputStream(byteStream)) {
            // Read magic bytes
            byte[] magicBytes = new byte[ResponseIfc.MAGIC_BYTES.length];
            in.readFully(magicBytes);
            if (!Arrays.equals(magicBytes, ResponseIfc.MAGIC_BYTES)) {
                return false; // Magic bytes mismatch
            }

            // Read machine type, object type, status type, and data type
            int machineTypeOrdinal = in.readInt();
            int objTypeOrdinal = in.readInt();
            int statusTypeOrdinal = in.readInt();
            int dataTypeOrdinal = in.readInt();

            // Verify machine type, object type, and status type
            if (machineTypeOrdinal < 0 || machineTypeOrdinal >= DSMachine.MachineType.values().length
                    || objTypeOrdinal < 0 || objTypeOrdinal >= DSObject.ObjType.values().length
                    || statusTypeOrdinal < 0 || statusTypeOrdinal >= ResponseStatus.values().length
                    || dataTypeOrdinal < 0 || dataTypeOrdinal >= DataType.values().length) {
                return false; // Invalid machine type, object type, status type, or data type
            }

            machineType = DSMachine.MachineType.values()[machineTypeOrdinal];
            objectType = DSObject.ObjType.values()[objTypeOrdinal];
            responseStatus = ResponseStatus.values()[statusTypeOrdinal];
            dataType = DataType.values()[dataTypeOrdinal];

            // Read version
            version = in.readInt();

            // Read data
            switch (dataType) {
                case STRING:
                    int stringLength = in.readInt();
                    byte[] stringBytes = new byte[stringLength];
                    in.readFully(stringBytes);
                    data = new String(stringBytes, StandardCharsets.UTF_8);
                    break;
                default:
                    throw new Exception("Unsupported data type during deserialization!");
            }
        }
        this.content = content;
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

    public DSMachine.MachineType getMachineType() {
        return machineType;
    }

    public int getVersion() {
        return version;
    }

}
