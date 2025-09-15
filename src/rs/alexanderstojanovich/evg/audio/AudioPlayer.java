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

import org.joml.Vector3f;
import org.lwjgl.openal.AL10;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 * Audio player is used to play decoded audio tracks. Loop can be optionally
 * set. Volume can be adjusted.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class AudioPlayer {

    private int bufferPointer = 0;
    private int sourcePointer = 0;

    public AudioPlayer() {
        //Request space for the buffer
        bufferPointer = AL10.alGenBuffers();
        //Request a source
        sourcePointer = AL10.alGenSources();
    }

    /**
     * Play audio track.
     *
     * @param audioFile audio track
     * @param stopIfPlaying stop playing current track (optional)
     * @param loop repeat playing track after its end.
     */
    public void play(AudioFile audioFile, boolean stopIfPlaying, boolean loop) {
        if (!MasterAudio.isInitialized()) {
            DSLogger.reportError("Master Audio not initialized!", null);
        }
        if (isPlaying() && stopIfPlaying) {
            stop();
        }
        AL10.alSourcei(sourcePointer, AL10.AL_BUFFER, 0);
        AL10.alBufferData(bufferPointer, audioFile.getFormat(), audioFile.getContent(), audioFile.getSampleRate());
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

    /**
     * Play audio sound effects.
     *
     * @param audioFile audio sound FX.
     * @param pos position in 3D space of the sound FX.
     */
    public void play(AudioFile audioFile, Vector3f pos) {
        if (!MasterAudio.isInitialized()) {
            DSLogger.reportError("Master Audio not initialized!", null);
        }
        if (isPlaying()) {
            stop();
        }
        if (audioFile.getContent() == null) {
            return;
        }
        AL10.alSourcei(sourcePointer, AL10.AL_BUFFER, 0);
        AL10.alBufferData(bufferPointer, audioFile.getFormat(), audioFile.getContent(), audioFile.getSampleRate());

        // set position
        AL10.alSource3f(sourcePointer, AL10.AL_POSITION, pos.x, pos.y, pos.z);

        //Assign the sound we just loaded to the source
        AL10.alSourcei(sourcePointer, AL10.AL_BUFFER, bufferPointer);

        //Play the sound
        AL10.alSourcePlay(sourcePointer);
    }

    public void play() {
        if (sourcePointer != 0) {
            AL10.alSourcePlay(sourcePointer);
        }
    }

    public void pause() {
        if (sourcePointer != 0) {
            AL10.alSourcePause(sourcePointer);
        }
    }

    public void stop() {
        if (sourcePointer != 0) {
            AL10.alSourceStop(sourcePointer);
        }
    }

    public boolean isPlaying() {
        if (sourcePointer != 0) {
            return AL10.alGetSourcei(sourcePointer, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
        }
        return false;
    }

    public void setGain(float gain) {
        if (sourcePointer != 0) {
            AL10.alSourcef(sourcePointer, AL10.AL_GAIN, gain);
        }
    }

    public float getGain() {
        if (sourcePointer != 0) {
            return AL10.alGetSourcef(sourcePointer, AL10.AL_GAIN);
        }
        return 0.0f;
    }

}
