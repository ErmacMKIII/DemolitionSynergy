/* 
 * Copyright (C) 2020 Alexander Stojanovich <coas91@rocketmail.com>
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

import com.jcraft.oggdecoder.OggData;
import com.jcraft.oggdecoder.OggDecoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.lwjgl.openal.AL10;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class AudioFile { // only ogg are supported

    private final String fileName;
    private int format = -1;
    private int channels;
    private int sampleRate;
    private ByteBuffer content;

    public static AudioFile AMBIENT = new AudioFile(Game.SOUND_ENTRY, "erokia_ambient.ogg");
    public static AudioFile INTERMISSION = new AudioFile(Game.SOUND_ENTRY, "erokia_intermission.ogg");

    public static AudioFile BLOCK_SELECT = new AudioFile(Game.SOUND_ENTRY, "block_selection.ogg");
    public static AudioFile BLOCK_ADD = new AudioFile(Game.SOUND_ENTRY, "block_addition.ogg");
    public static AudioFile BLOCK_REMOVE = new AudioFile(Game.SOUND_ENTRY, "block_removal.ogg");

    public AudioFile(String dirEntry, String fileName) {
        this.fileName = fileName;
        loadAudio(dirEntry, fileName);
    }

    private void loadAudio(String dirEntry, String fileName) {
        File extern = new File(dirEntry + fileName);
        File archive = new File(Game.DATA_ZIP);
        ZipFile zipFile = null;
        InputStream audioIn = null;
        OggData data = null;
        if (extern.exists()) {
            try {
                audioIn = new FileInputStream(extern);
            } catch (FileNotFoundException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else if (archive.exists()) {
            try {
                zipFile = new ZipFile(archive);
                for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                    if (zipEntry.getName().equals(dirEntry + fileName)) {
                        audioIn = zipFile.getInputStream(zipEntry);
                        break;
                    }
                }
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else {
            DSLogger.reportError("Cannot find zip archive " + Game.DATA_ZIP + " or relevant ingame files!", null);
        }
        //----------------------------------------------------------------------
        if (audioIn == null) {
            DSLogger.reportError("Cannot find resource " + dirEntry + fileName + "!", null);
            return;
        }
        OggDecoder decoder = new OggDecoder();
        try {
            data = decoder.getData(audioIn);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            try {
                audioIn.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                    DSLogger.reportFatalError(ex.getMessage(), ex);
                }
            }
        }
        if (data != null) {
            this.channels = data.channels;
            this.sampleRate = data.rate;
            this.content = data.data;
            if (channels == 1) {
                format = AL10.AL_FORMAT_MONO16;
            } else if (channels == 2) {
                format = AL10.AL_FORMAT_STEREO16;
            }
        }
    }

    public String getFileName() {
        return fileName;
    }

    public int getFormat() {
        return format;
    }

    public int getChannels() {
        return channels;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public ByteBuffer getContent() {
        return content;
    }

}
