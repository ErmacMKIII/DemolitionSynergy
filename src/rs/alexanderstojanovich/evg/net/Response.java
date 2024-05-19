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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.main.GameServer;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.OBJECT;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.VOID;

public class Response implements ResponseIfc {

    protected byte[] content;
    protected ResponseStatus responseStatus;
    protected DataType dataType;
    protected Object data;

    protected DSMachine.MachineType machineType;
    protected ObjType objectType;
    protected int version = 0;

    /**
     * Invalid response. If receiving fails!
     */
    public static final Response INVALID = new Response(ResponseStatus.INVALID, VOID, null);

    public Response() {
    }

    public Response(ResponseStatus responseStatus, DataType dataType, Object data) {
        this.responseStatus = responseStatus;
        this.dataType = dataType;
        this.data = data;
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
            // Write machine type, object type, status type, and data type
            out.writeInt(machine.getMachineType().ordinal());
            out.writeInt(getObjectType().ordinal());
            out.writeInt(responseStatus.ordinal());
            out.writeInt(dataType.ordinal());
            out.writeInt(machine.getVersion());

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
                return null; // Magic bytes mismatch
            }

            // Read machine type, object type, status type, and data type
            int machineTypeOrdinal = in.readInt();
            int objTypeOrdinal = in.readInt();
            int statusTypeOrdinal = in.readInt();
            int dataTypeOrdinal = in.readInt();

            // Verify machine type, object type, status type, and data type
            machineType = DSMachine.MachineType.values()[machineTypeOrdinal];
            objectType = DSObject.ObjType.values()[objTypeOrdinal];
            responseStatus = ResponseStatus.values()[statusTypeOrdinal];
            dataType = DataType.values()[dataTypeOrdinal];

            version = in.readInt();

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
                default:
                    throw new IOException("Unsupported data type during deserialization!");
            }
        }
        this.content = content;
        return this;
    }

    @Override
    public void send(GameServer server, InetAddress clientAddress, int clientPort) throws Exception {
        serialize(server);
        DatagramPacket packet = new DatagramPacket(content, content.length, clientAddress, clientPort);
        server.getEndpoint().send(packet);
    }

    @Override
    public ResponseStatus getResponseStatus() {
        return this.responseStatus;
    }

    @Override
    public ObjType getObjectType() {
        return ObjType.RESPONSE;
    }

}
