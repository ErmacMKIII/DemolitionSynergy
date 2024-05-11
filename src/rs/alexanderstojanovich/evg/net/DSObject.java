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
        VOID, BOOL, INT, FLOAT, LONG, DOUBLE, STRING, VEC3F, VEC4F
    }

    /**
     * Get intern data type
     *
     * @return
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
     * @param machine game machine who deserializes.
     * @param content byte content to deserialize
     * @return operation status - true if successful and false otherwise
     * @throws java.lang.Exception if serialization fails or error is
     * encountered
     */
    public boolean deserialize(DSMachine machine, byte[] content) throws Exception;
}
