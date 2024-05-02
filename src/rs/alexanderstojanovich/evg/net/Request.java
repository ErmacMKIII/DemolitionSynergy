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
import java.util.Arrays;
import rs.alexanderstojanovich.evg.main.Game;

/**
 * DSynergy Request implementation
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Request implements DSObject, RequestIfc {

    protected byte[] content;
    protected RequestType requestType;
    protected DataType dataType;
    protected Object data;

    public Request() {

    }

    public Request(RequestType requestType, DataType dataType, Object data) {
        this.requestType = requestType;
        this.dataType = dataType;
        this.data = data;
    }

    @Override
    public ObjType getObjType() {
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
        final int mchType = machine.getMachineType().ordinal();
        final int objType = getObjType().ordinal(); // request or response
        final int reqType = getRequestType().ordinal();
        final int datType = getDataType().ordinal();

        final int version = machine.getVersion();

        final byte[] first = {(byte) mchType, (byte) objType, (byte) reqType, (byte) datType}; // 4 Bytes
        final byte[] second = {(byte) (version >> 24), (byte) (version >> 16), (byte) (version >> 8), (byte) (version)}; // 4 Bytes
        final byte[] magic = RequestIfc.MAGIC_BYTES; // 4 Bytes

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
            System.arraycopy(third, 0, content, 12, third.length); // 4 Bytes   
            System.arraycopy(magic, 0, content, 8, magic.length); // 4 Bytes             
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
        // Extracting the relevant information from the byte array
        int mchType = content[0];
        if (DSMachine.MachineType.values()[mchType] != DSMachine.MachineType.DSCLIENT && DSMachine.MachineType.values()[mchType] != DSMachine.MachineType.DSSERVER) {
            return false; // bad request
        }
        int objType = content[1];
        if (DSObject.ObjType.values()[objType] != ObjType.REQUEST && DSObject.ObjType.values()[objType] != ObjType.RESPONSE) {
            return false; // bad request
        }

        int reqType = content[2];
        if (reqType < 0 || reqType >= RequestType.values().length) {
            return false; // bad request
        }
        int datType = content[3];
        if (datType < 0 || datType >= DataType.values().length) {
            return false; // bad request
        }
        int version = (content[4] << 24) | (content[5] << 16) | (content[6] << 8) | content[7];
        if (version < 39) {
            return false; // bad request
        }
        byte[] magic = Arrays.copyOfRange(content, 8, 12);
        if (Arrays.equals(magic, Request.MAGIC_BYTES)) {
            return false;
        }

        // If there is additional data, deserialize it based on the data type
        Object data0 = null;
        if (content.length > 12) {
            byte[] actualData = Arrays.copyOfRange(content, 12, content.length);

            // Deserialize based on data type
            switch (DataType.values()[datType]) {
                case STRING:
                    String message = new String(actualData, "UTF-8");
                    data0 = message;
                    break;
                // Add cases for other data types if needed
                default:
                    throw new Exception("Unsupported data type during deserialization!");
            }
        }

        // Constructing the request object
        this.requestType = RequestType.values()[reqType];
        this.dataType = DataType.values()[datType];
        this.data = data0;
        this.content = content;

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
