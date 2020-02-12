/*
 * Copyright (C) 2020 Coa
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
package rs.alexanderstojanovich.evg.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

/**
 *
 * @author Coa
 */
public class MasterAudio {

    public static final String DEFAULT_DEVICE_NAME = ALC10.alcGetString(0, ALC10.ALC_DEFAULT_DEVICE_SPECIFIER);
    private static final long DEFAULT_DEVICE = ALC10.alcOpenDevice(DEFAULT_DEVICE_NAME);
    private static int[] attributes = {0};
    private static long context = 0;

    private static ALCCapabilities aLCCapabilities;
    private static ALCapabilities aLCapabilities;

    private static boolean initialized = false;

    public static void init() {
        context = ALC10.alcCreateContext(DEFAULT_DEVICE, attributes);
        ALC10.alcMakeContextCurrent(context);

        aLCCapabilities = ALC.createCapabilities(DEFAULT_DEVICE);
        aLCapabilities = AL.createCapabilities(aLCCapabilities);

        initialized = true;
    }

    public static void destroy() {
        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(DEFAULT_DEVICE);
    }

    public static long getDEFAULT_DEVICE() {
        return DEFAULT_DEVICE;
    }

    public static int[] getAttributes() {
        return attributes;
    }

    public static long getContext() {
        return context;
    }

    public static ALCCapabilities getaLCCapabilities() {
        return aLCCapabilities;
    }

    public static ALCapabilities getaLCapabilities() {
        return aLCapabilities;
    }

    public static boolean isInitialized() {
        return initialized;
    }

}
