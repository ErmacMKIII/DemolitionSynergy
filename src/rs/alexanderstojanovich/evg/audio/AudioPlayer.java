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

import org.lwjgl.openal.AL10;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Coa
 */
public class AudioPlayer {

    private int bufferPointer = 0;
    private int sourcePointer = 0;

    private int load(AudioFile audioFile) {
        //Request space for the buffer
        bufferPointer = AL10.alGenBuffers();
        //Send the data to OpenAL
        AL10.alBufferData(bufferPointer, audioFile.getFormat(), audioFile.getContent(), audioFile.getSampleRate());
        return bufferPointer;
    }

    public void play(AudioFile audioFile, boolean loop) {
        if (!MasterAudio.isInitialized()) {
            DSLogger.reportError("Master Audio not initialized!");
        }
        bufferPointer = load(audioFile);
        //Request a source
        sourcePointer = AL10.alGenSources();
        if (loop) {
            AL10.alSourcei(sourcePointer, AL10.AL_LOOPING, AL10.AL_TRUE);
        } else {
            AL10.alSourcei(sourcePointer, AL10.AL_LOOPING, AL10.AL_FALSE);
        }
        //Assign the sound we just loaded to the source
        AL10.alSourcei(sourcePointer, AL10.AL_BUFFER, bufferPointer);

        //Play the sound
        AL10.alSourcePlay(sourcePointer);
    }

    public void pause() {
        AL10.alSourcePause(sourcePointer);
    }

    public void stop() {
        AL10.alSourceStop(sourcePointer);
    }

}
