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

import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.models.Model;

/**
 * Demolition Synergy weapon interface. Contains all texture, models and sounds
 * associated with weapon.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface WeaponIfc {

    /*
        01 - M9 Pistol                - "W01M9.obj"
        02 - M1911 Pistol             - "W02M1.obj"
        03 - Desert Eagle             - "W03DE.obj"
        04 - Mini Uzi SMG             - "W04UZ.obj"
        05 - MP5 SMG                  - "W05M5.obj"
        06 - P90 SMG                  - "W06P9.obj"
        07 - AK47 Rifle               - "W07AK.obj"
        08 - M4A1 Rifle               - "W08M4.obj"
        09 - G36 Rifle                - "W09G3.obj"
        10 - M60 MG                   - "W10M6.obj"
        11 - SAW MG                   - "W11MS.obj"
        12 - Winchester 1200 Shotgun  - "W12W2.obj"
        13 - Benelli Super 90 Shotgun - "W13B9.obj"
        14 - Remington 700 Sniper     - "W14R7.obj"
        15 - Dragunov Sniper          - "W15DR.obj"
        16 - M82 Sniper               - "W16M8.obj"
     */
    /**
     * Weapon class definition
     */
    public static enum Clazz {
        /**
         * Class None. Unarmed.
         */
        None,
        /**
         * All Pistols. M9 Pistol, M1911 Pistol, Desert Eagle
         */
        OneHandedSmallGun,
        /**
         * All SMGs, Rifles, Shotguns and Snipers apart from M82 Sniper.
         */
        TwoHandedSmallGun,
        /**
         * Big guns. Machine guns. M60 MG, SAW MG. Plus M82 Sniper.
         */
        TwoHandedBigGuns
    }

    /**
     * Position of the weapon in the game world (uses Weapon GLSL Shader)
     */
    public static final Vector3f WEAPON_POS = new Vector3f(1.0f, -1.0f, 2.2f);

    /**
     * Get Weapon class definition
     *
     * @return
     */
    public Clazz getClazz();

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
     * @param critter critter having that weapon
     * @return model on character
     */
    public Model deriveBodyModel(Critter critter);

    /**
     * Model on ground. Use Main GLSL Shader.
     *
     * @return
     */
    public Model deriveOnGroundItem();

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
