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
public enum Weapon implements WeaponIfc {
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
    M9_PISTOL(WeaponConstants.M9_PISTOL),
    M1911_PISTOL(WeaponConstants.M1911_PISTOL),
    DESERT_EAGLE(WeaponConstants.DESERT_EAGLE),
    MINI_UZI_SMG(WeaponConstants.MINI_UZI_SMG),
    MP5_SMG(WeaponConstants.MP5_SMG),
    P90_SMG(WeaponConstants.P90_SMG),
    AK47_RIFLE(WeaponConstants.AK47_RIFLE),
    M4A1_RIFLE(WeaponConstants.M4A1_RIFLE),
    G36_RIFLE(WeaponConstants.G36_RIFLE),
    M60_MG(WeaponConstants.M60_MG),
    SAW_MG(WeaponConstants.SAW_MG),
    WINCHESTER_1200_SHOTGUN(WeaponConstants.WINCHESTER_1200_SHOTGUN),
    BENELLI_SUPER_90_SHOTGUN(WeaponConstants.BENELLI_SUPER_90_SHOTGUN),
    REMINGTON_700_SNIPER(WeaponConstants.REMINGTON_700_SNIPER),
    DRAGUNOV_SNIPER(WeaponConstants.DRAGUNOV_SNIPER),
    M82_SNIPER(WeaponConstants.M82_SNIPER);

    public final Model model;
    public final Model chrMdl;
    public final Model gndMdl;
    public final Model hndMdl;

    private Weapon(Model model) {
        this.model = model;
        this.hndMdl = new Model(model);
        this.hndMdl.pos = WeaponConstants.WEAPON_POS;
        this.hndMdl.setScale(1.0f);
        this.hndMdl.setrY((float) (-Math.PI / 2.0f));

        // TODO
        this.chrMdl = this.gndMdl = null;
    }

    @Override
    public String getTexName() {
        return model.texName;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public Model onCharacter() {
        return chrMdl;
    }

    @Override
    public Model onGround() {
        return gndMdl;
    }

    @Override
    public Model inHands() {
        return hndMdl;
    }

    @Override
    public AudioFile getFireSoundFX() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
