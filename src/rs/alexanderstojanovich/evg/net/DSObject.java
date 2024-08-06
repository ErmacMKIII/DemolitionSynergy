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

/**
 * DSObject is common term for Request and Response in DSynergy.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface DSObject {

    /**
     * Object type to be send over net.
     */
    public static enum ObjType {
        REQUEST, RESPONSE
    }

    /**
     * Get object type {REQUEST, RESPONSE}
     *
     * @return DSynergy object type
     */
    public ObjType getObjectType();

    /**
     * Get whole object content (byte array)
     *
     * @return object content as whole
     */
    public byte[] getContent();

    /**
     * Intern data type
     */
    public static enum DataType {
        VOID, OBJECT, BOOL, INT, FLOAT, LONG, DOUBLE, STRING, VEC3F, VEC4F
    }

    /**
     * Get intern data type
     *
     * @return data type which was sent over 'DS' network.
     */
    public DataType getDataType();

    /**
     * Get data from DSObject (could be body)
     *
     * @return data object
     */
    public Object getData();

    /**
     * Serialize-self into byte array.
     *
     * @param machine game machine who serializes.
     * @throws java.lang.Exception if serialization fails
     */
    public void serialize(DSMachine machine) throws Exception;

    /**
     * Derialize-self into byte array. Result is written to content.
     *
     * @param content byte content to deserialize
     * @return this; if this is not null operation is successful
     * @throws java.lang.Exception if serialization fails or error is
     * encountered
     */
    public DSObject deserialize(byte[] content) throws Exception;

    /**
     * Get checksum of data. This is the only way to pair request with response.
     *
     * @return checksum
     */
    public long getChecksum();
}
