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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.main.GameServer;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.INT;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.LONG;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.OBJECT;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.VOID;

public class Response implements ResponseIfc {

    protected String guid = "*";
    protected byte[] content;
    protected ResponseStatus responseStatus;
    protected DataType dataType;
    protected Object data;

    protected DSMachine.MachineType machineType;
    protected ObjType objectType;
    protected int version = 0;

    public final long checksum;
    public final String id;

    /**
     * Invalid response. If receiving fails!
     */
    public static final Response INVALID = new Response(RequestIfc.NIL_ID, 0L, ResponseStatus.INVALID, VOID, null);

    /**
     * Create empty response with given checksum
     *
     * @param id unique id of this response from it's request
     * @param checksum checksum to assign
     */
    public Response(String id, long checksum) {
        this.checksum = checksum;
        this.id = id;
    }

    /**
     * Create given response with response status
     *
     * @param id unique id of this request
     * @param checksum checksum to assign
     * @param responseStatus reponse status {INVALID, ERR, OK}
     * @param dataType data type (most often 'STRING')
     * @param data data (argument)
     */
    public Response(String id, long checksum, ResponseStatus responseStatus, DataType dataType, Object data) {
        this.checksum = checksum;
        this.responseStatus = responseStatus;
        this.dataType = dataType;
        this.data = data;
        this.id = id;
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
    public void serialize(DSMachine machine) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(byteStream)) {
            // Write magic bytes
            out.write(ResponseIfc.MAGIC_BYTES);

            // Write sender
            out.write(guid.getBytes(StandardCharsets.UTF_8));

            // Write machine type, object type, status type, and data type
            out.writeByte(machine.getMachineType().ordinal());
            out.writeByte(getObjectType().ordinal());
            out.writeByte(responseStatus.ordinal());
            out.writeByte(dataType.ordinal());
            out.writeByte(machine.getVersion());

            if (dataType != DataType.VOID) {
                switch (dataType) {
                    case OBJECT:
                    case STRING:
                        // Write string length and bytes
                        String message = (String) data;
                        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                        out.writeInt(messageBytes.length);
                        out.write(messageBytes);
                        break;
                    case BOOL:
                        // Write boolean
                        out.writeBoolean((boolean) data);
                        break;
                    case INT:
                        out.writeInt((int) data);
                        break;
                    case LONG:
                        out.writeLong((long) data);
                        break;
                    case FLOAT:
                        // Write float
                        out.writeFloat((float) data);
                        break;
                    case DOUBLE:
                        // Write double
                        out.writeDouble((double) data);
                        break;
                    case VEC3F:
                        // Write Vector3f
                        Vector3f vec3 = (Vector3f) data;
                        out.writeFloat(vec3.x);
                        out.writeFloat(vec3.y);
                        out.writeFloat(vec3.z);
                        break;
                    case VEC4F:
                        // Write Vector4f
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
                            byte[] arr4 = (byte[]) data;
                            out.writeShort(arr4.length);
                            for (byte b : arr4) {
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
        }
        this.content = byteStream.toByteArray();
    }

    @Override
    public Response deserialize(byte[] content) throws IOException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(content);
        try (DataInputStream in = new DataInputStream(byteStream)) {
            // Read magic bytes
            byte[] magicBytes = new byte[ResponseIfc.MAGIC_BYTES.length];
            in.readFully(magicBytes);
            if (!Arrays.equals(magicBytes, ResponseIfc.MAGIC_BYTES)) {
                return Response.INVALID; // Magic bytes mismatch
            }

            // Read guid of recipient
            byte[] senderBytes = new byte[RequestIfc.GUID_LENGTH];
            in.readFully(senderBytes);
            guid = new String(senderBytes);

            // Read machine type, object type, status type, and data type
            int machineTypeOrdinal = in.readByte();
            int objTypeOrdinal = in.readByte();
            int statusTypeOrdinal = in.readByte();
            int dataTypeOrdinal = in.readByte();

            // Verify machine type, object type, status type, and data type
            machineType = DSMachine.MachineType.values()[machineTypeOrdinal];
            objectType = DSObject.ObjType.values()[objTypeOrdinal];
            responseStatus = ResponseStatus.values()[statusTypeOrdinal];
            dataType = DataType.values()[dataTypeOrdinal];

            version = in.readByte();

            // Read data based on data type
            switch (dataType) {
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
                    int length = in.readUnsignedShort();
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
                                arr2[i] = in.readByte();
                            }
                            data = arr2;
                        case 4:
                            int[] arr4 = new int[length];
                            for (int i = 0; i < length; i++) {
                                arr4[i] = in.readByte();
                            }
                            data = arr4;
                        default:
                            throw new IOException("Unsupported ARRAY format during deserialization!");
                    }
                    break;
                default:
                    throw new IOException("Unsupported data type during deserialization!");
            }
        }
        this.content = content;
        return this;
    }

    @Override
    public void send(String guid, GameServer server, IoSession session) throws IOException {
        this.guid = guid;
        serialize(server);
        // storing content with checksum
        final int capacity = content.length + Long.BYTES + DSObject.ID_LENGTH;
        IoBuffer buffer = IoBuffer.allocate(capacity, true);
        buffer.put(content);
        // checksum
        buffer.putLong(checksum);
        // Id 
        buffer.put(id.getBytes(StandardCharsets.US_ASCII));
        buffer.flip();

        session.write(buffer);
    }

    @Override
    public ResponseStatus getResponseStatus() {
        return this.responseStatus;
    }

    @Override
    public ObjType getObjectType() {
        return ObjType.RESPONSE;
    }

    @Override
    public long getChecksum() {
        return checksum;
    }

    @Override
    public String getReceiverGuid() {
        return guid;
    }

    /**
     * Get globally unique id of the request to which this response belongs to
     *
     * @return (globally) unique id
     */
    @Override
    public String getId() {
        return this.id;
    }

}
