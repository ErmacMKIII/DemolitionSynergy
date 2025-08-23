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
package rs.alexanderstojanovich.evg.util;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.UUID;
import rs.alexanderstojanovich.evg.main.Configuration;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class HardwareUtils {

    /**
     * Retrieves hardware information, such as MAC address.
     *
     * @return hardware information string
     * @throws NoSuchAlgorithmException if the specified algorithm for SHA-1
     * hashing is not available
     * @throws SocketException if an I/O error occurs while retrieving the
     * network interface information
     */
    public static String getHardwareInfo() throws NoSuchAlgorithmException, SocketException {
        StringBuilder sb = new StringBuilder();

        // Get MAC address
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            byte[] mac = networkInterface.getHardwareAddress();
            if (mac != null) {
                for (int i = 0; i < mac.length; i++) {
                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                }
                break;
            }
        }

        // Get CPU serial number or any other hardware information
        // You may need to use a library or platform-specific code to retrieve this information
        return sb.toString();
    }

    /**
     * Generates a UUID based on hardware information.
     *
     * @return hardware-based UUID
     */
    public static String generateHardwareUUID() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            Configuration cfg = Configuration.getInstance();
            byte[] hwBytes = getHardwareInfo().getBytes();
            md.update(hwBytes);
            byte[] hash = md.digest();
            long mostSignificantBits = 0;
            for (int i = 0; i < 8; i++) {
                mostSignificantBits = (mostSignificantBits << 8) | (hash[i] & 0xff);
            }
            long leastSignificantBits = 0;
            for (int i = 8; i < 16; i++) {
                leastSignificantBits = (leastSignificantBits << 8) | (hash[i] & 0xff);
            }
            return new UUID(cfg.isUseBakGuid() ? Long.reverseBytes(mostSignificantBits) : mostSignificantBits, cfg.isUseBakGuid() ? Long.reverseBytes(leastSignificantBits) : leastSignificantBits).toString().substring(20, 36);
        } catch (NoSuchAlgorithmException | SocketException ex) {
            DSLogger.reportFatalError("Could not generate hardware Unique ID!", ex);
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }

        return null;
    }
}
