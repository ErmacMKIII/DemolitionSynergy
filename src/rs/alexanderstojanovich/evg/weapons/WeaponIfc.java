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
package rs.alexanderstojanovich.evg.weapons;

import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.models.Model;

/**
 * Demolition Synergy weapon interface. Contains all texture, models and sounds
 * associated with weapon.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface WeaponIfc {

    /**
     * Get Texture name associated with this
     *
     * @return texture name (string)
     */
    public String getTexName();

    /**
     * Get model as is, unaltered. Loaded from Model utils.
     *
     * @return unaltered model
     */
    public Model getModel();

    /**
     * Model on character. Use Main GLSL Shader.
     *
     * @return model on character
     */
    public Model onCharacter();

    /**
     * Model on ground. Use Main GLSL Shader.
     *
     * @return
     */
    public Model onGround();

    /**
     * Model in hands. Use Player GLSL Shader.
     *
     * @return model in hands
     */
    public Model inHands();

    /**
     * Get audio file Play sound fx from Audio File
     *
     * @return
     */
    public AudioFile getFireSoundFX();
}
