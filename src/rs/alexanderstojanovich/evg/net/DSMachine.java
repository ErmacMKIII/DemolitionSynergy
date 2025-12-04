/*
 * Copyright (C) 2024 Aleksandar Stojanovic <coas91@rocketmail.com>
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
 * Common term for both DSynergy Game Client and Game Server.
 *
 * Capable of serializing/deserializing DSObjects over network.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public interface DSMachine {

    /**
     * Possible machine configurations.
     */
    public static enum MachineType { // 8-Bytes (ASCII)
        DSCLIENT, DSSERVER
    }

    /**
     * Get machine type. Who is on endpoint.
     *
     * If machine is capable of handling clients over net, then is server.
     * Otherwise is client.
     *
     * @return client or server {DSCLIENT, DSSERVER}
     */
    public MachineType getMachineType();

    /**
     * Get DSClient/DSServer version.
     *
     * @return version of DSClient/DSServer
     */
    public int getVersion();

    /**
     * Is machine running.
     *
     * @return is machine (game client or game server) running
     */
    public boolean isRunning();

    /**
     * Get guid of this machine (player unique id)
     *
     * @return player guid
     */
    public String getGuid();
}
