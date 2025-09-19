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
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.CRC32C;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.main.Game;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.ARRAY;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.BOOL;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.DOUBLE;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.FLOAT;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.INT;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.LONG;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.STRING;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.VEC3F;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.VEC4F;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.VOID;

/**
 * DSynergy Request implementation
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Request implements RequestIfc {

    protected String guid = "*";
    protected byte[] content;
    protected RequestType requestType;
    protected DataType dataType;
    protected Object data;
    protected int version = 0;

    public final InetAddress clientAddress;
    public final int clientPort;

    protected long checksum = 0L;
    protected long timestamp = 0L;
    protected String id = NIL_ID;

    /**
     * Invalid request. If receiving fails!
     */
    public static final Request INVALID = new Request(RequestType.INVALID, DataType.VOID, null);

    /**
     * Server side. Receiving request(s).
     *
     * @param clientAddress client Inet address who send request
     * @param clientPort client port who send request
     * @param checksum checksum of received data
     */
    public Request(InetAddress clientAddress, int clientPort, long checksum) {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.checksum = checksum;
    }

    /**
     * Client side. Send request(s).
     *
     * @param requestType request type (enum)
     * @param dataType data type associated with this request
     * @param data data object
     */
    public Request(RequestType requestType, DataType dataType, Object data) {
        this.requestType = requestType;
        this.dataType = dataType;
        this.data = data;
        this.clientAddress = null;
        this.clientPort = 0;
        // Automatically set timestamp for new requests
        this.timestamp = System.currentTimeMillis();
        // Automatically set id for new requests
        this.id = UUID.randomUUID().toString().substring(20, 36);
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

            // Write sender
            out.write(machine.getGuid().getBytes(StandardCharsets.UTF_8));

            // Write machine type, object type, request type, data type
            out.writeByte(machine.getMachineType().ordinal());
            out.writeByte(getObjectType().ordinal());
            out.writeShort(requestType.ordinal());
            out.writeByte(dataType.ordinal());

            // Write version
            out.writeByte(machine.getVersion());

            // Write data
            if (dataType != DataType.VOID) {
                switch (dataType) {
                    case OBJECT:
                    case STRING:
                        String message = (String) data;
                        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                        out.writeInt(messageBytes.length);
                        out.write(messageBytes);
                        break;
                    case BOOL:
                        out.writeBoolean((boolean) data);
                        break;
                    case INT:
                        out.writeInt((int) data);
                        break;
                    case LONG:
                        out.writeLong((long) data);
                        break;
                    case FLOAT:
                        out.writeFloat((float) data);
                        break;
                    case DOUBLE:
                        out.writeDouble((double) data);
                        break;
                    case VEC3F:
                        Vector3f vec3 = (Vector3f) data;
                        out.writeFloat(vec3.x);
                        out.writeFloat(vec3.y);
                        out.writeFloat(vec3.z);
                        break;
                    case VEC4F:
                        Vector4f vec4 = (Vector4f) data;
                        out.writeFloat(vec4.x);
                        out.writeFloat(vec4.y);
                        out.writeFloat(vec4.z);
                        out.writeFloat(vec4.w);
                        break;
                    case ARRAY:
                        if (data instanceof byte[]) {
                            out.writeByte(1);
                            byte[] arr1 = (byte[]) data;
                            out.writeShort(arr1.length);
                            for (byte b : arr1) {
                                out.writeByte(b);
                            }
                        } else if (data instanceof short[]) {
                            out.writeByte(2);
                            short[] arr2 = (short[]) data;
                            out.writeShort(arr2.length);
                            for (short sh : arr2) {
                                out.writeShort(sh);
                            }
                        } else if (data instanceof int[]) {
                            out.writeByte(4);
                            int[] arr4 = (int[]) data;
                            out.writeShort(arr4.length);
                            for (int b : arr4) {
                                out.writeInt(b);
                            }
                        } else {
                            throw new IOException("Unsupported ARRAY type during serialization!");
                        }
                        break;
                    default:
                        throw new IOException("Unsupported data type during serialization!");
                }
            }

            // Write Id
            out.writeBytes(id);
            // Write timestamp
            out.writeLong(timestamp); // Include the timestamp in serialization
        }
        this.content = byteStream.toByteArray();
    }

    @Override
    public DSObject deserialize(byte[] content) throws Exception {
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
                return Request.INVALID; // Magic bytes mismatch
            }   // Read machine type, object type, request type, and data type

            // Read guid of sender
            byte[] senderBytes = new byte[RequestIfc.GUID_LENGTH];
            in.readFully(senderBytes);
            guid = new String(senderBytes);

            int machineTypeOrdinal = in.readByte();
            int objTypeOrdinal = in.readByte();
            reqTypeOrdinal = in.readShort();
            dataTypeOrdinal = in.readByte();
            // Verify machine type, object type, and request type
            if (machineTypeOrdinal < 0 || machineTypeOrdinal >= DSMachine.MachineType.values().length
                    || objTypeOrdinal < 0 || objTypeOrdinal >= DSObject.ObjType.values().length
                    || reqTypeOrdinal < 0 || reqTypeOrdinal >= RequestType.values().length
                    || dataTypeOrdinal < 0 || dataTypeOrdinal >= DataType.values().length) {
                return Request.INVALID; // Invalid machine type, object type, request type, or data type
            }   // Read version
            version = in.readByte();
            // Read data
            // Read data based on data type
            switch (DataType.values()[dataTypeOrdinal]) {
                case OBJECT:
                case STRING:
                    int stringLength = in.readInt();
                    byte[] stringBytes = new byte[stringLength];
                    in.readFully(stringBytes);
                    data = new String(stringBytes, StandardCharsets.UTF_8);
                    break;
                case BOOL:
                    data = in.readBoolean();
                    break;
                case INT:
                    data = in.readInt();
                    break;
                case LONG:
                    data = in.readLong();
                    break;
                case FLOAT:
                    data = in.readFloat();
                    break;
                case DOUBLE:
                    data = in.readDouble();
                    break;
                case VEC3F:
                    float x = in.readFloat();
                    float y = in.readFloat();
                    float z = in.readFloat();
                    data = new Vector3f(x, y, z);
                    break;
                case VEC4F:
                    float vx = in.readFloat();
                    float vy = in.readFloat();
                    float vz = in.readFloat();
                    float vw = in.readFloat();
                    data = new Vector4f(vx, vy, vz, vw);
                    break;
                case VOID:
                    break;
                case ARRAY:
                    int sizeOf = in.readByte();
                    int length = in.readShort();
                    switch (sizeOf) {
                        case 1:
                            byte[] arr1 = new byte[length];
                            for (int i = 0; i < length; i++) {
                                arr1[i] = in.readByte();
                            }
                            data = arr1;
                            break;
                        case 2:
                            short[] arr2 = new short[length];
                            for (int i = 0; i < length; i++) {
                                arr2[i] = in.readShort();
                            }
                            data = arr2;
                        case 4:
                            int[] arr4 = new int[length];
                            for (int i = 0; i < length; i++) {
                                arr4[i] = in.readInt();
                            }
                            data = arr4;
                        default:
                            throw new IOException("Unsupported ARRAY format during deserialization!");
                    }
                    break;
                default:
                    throw new IOException("Unsupported data type during deserialization!");
            }

            // Read Id
            byte[] idBytes = new byte[ID_LENGTH];
            in.readFully(idBytes);
            this.id = new String(idBytes);
            // Read timestamp
            this.timestamp = in.readLong(); // Read the timestamp during deserialization
        }
        this.content = content;
        this.requestType = RequestType.values()[reqTypeOrdinal];
        this.dataType = DataType.values()[dataTypeOrdinal];
        this.version = version;

        return this;
    }

    @Override
    public RequestType getRequestType() {
        return this.requestType;
    }

    @Override
    public void send(Game client, IoSession session) throws Exception {
        serialize(client);
        // computing checksum
        CRC32C csObj = new CRC32C();
        csObj.update(content);
        this.checksum = csObj.getValue();
        // storing content with checksum
        final int capacity = content.length + Long.BYTES;
        IoBuffer buffer = IoBuffer.allocate(capacity, true);
        buffer.put(content);
        buffer.putLong(checksum);
        buffer.flip();

        session.write(buffer);
    }

    @Override
    public long getChecksum() {
        return checksum;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public InetAddress getClientAddress() {
        return clientAddress;
    }

    @Override
    public int getClientPort() {
        return clientPort;
    }

    @Override
    public String getSenderGuid() {
        return guid;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getId() {
        return id;
    }

}
