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
package rs.alexanderstojanovich.evg.weapons;

import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.models.Model;

/**
 * Class for all Weapons. Weapons could be model in player hands, models as
 * level item on the ground.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Weapons {

    public final LevelContainer levelContainer;
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

    public static final WeaponIfc NONE = new Weapon(WeaponIfc.Clazz.None, Model.MODEL_NONE);

    public final WeaponIfc M9_PISTOL;
    public final WeaponIfc M1911_PISTOL;
    public final WeaponIfc DESERT_EAGLE;
    public final WeaponIfc MINI_UZI_SMG;
    public final WeaponIfc MP5_SMG;
    public final WeaponIfc P90_SMG;
    public final WeaponIfc AK47_RIFLE;
    public final WeaponIfc M4A1_RIFLE;
    public final WeaponIfc G36_RIFLE;
    public final WeaponIfc M60_MG;
    public final WeaponIfc SAW_MG;
    public final WeaponIfc WINCHESTER_1200_SHOTGUN;
    public final WeaponIfc BENELLI_SUPER_90_SHOTGUN;
    public final WeaponIfc REMINGTON_700_SNIPER;
    public final WeaponIfc DRAGUNOV_SNIPER;
    public final WeaponIfc M82_SNIPER;

    /**
     * Field to access array of all ingame weapons
     */
    public final WeaponIfc[] AllWeapons;

    public Weapons(LevelContainer levelContainer) {
        this.levelContainer = levelContainer;

        this.M9_PISTOL = new Weapon(WeaponIfc.Clazz.OneHandedSmallGun, levelContainer.gameObject.GameAssets.M9_PISTOL);
        this.M1911_PISTOL = new Weapon(WeaponIfc.Clazz.OneHandedSmallGun, levelContainer.gameObject.GameAssets.M1911_PISTOL);
        this.DESERT_EAGLE = new Weapon(WeaponIfc.Clazz.OneHandedSmallGun, levelContainer.gameObject.GameAssets.DESERT_EAGLE);
        this.MINI_UZI_SMG = new Weapon(WeaponIfc.Clazz.TwoHandedSmallGun, levelContainer.gameObject.GameAssets.MINI_UZI_SMG);
        this.MP5_SMG = new Weapon(WeaponIfc.Clazz.TwoHandedSmallGun, levelContainer.gameObject.GameAssets.MP5_SMG);
        this.P90_SMG = new Weapon(WeaponIfc.Clazz.TwoHandedSmallGun, levelContainer.gameObject.GameAssets.P90_SMG);
        this.AK47_RIFLE = new Weapon(WeaponIfc.Clazz.TwoHandedSmallGun, levelContainer.gameObject.GameAssets.AK47_RIFLE);
        this.M4A1_RIFLE = new Weapon(WeaponIfc.Clazz.TwoHandedSmallGun, levelContainer.gameObject.GameAssets.M4A1_RIFLE);
        this.G36_RIFLE = new Weapon(WeaponIfc.Clazz.TwoHandedSmallGun, levelContainer.gameObject.GameAssets.G36_RIFLE);
        this.M60_MG = new Weapon(WeaponIfc.Clazz.TwoHandedBigGuns, levelContainer.gameObject.GameAssets.M60_MG);
        this.SAW_MG = new Weapon(WeaponIfc.Clazz.TwoHandedBigGuns, levelContainer.gameObject.GameAssets.SAW_MG);
        this.WINCHESTER_1200_SHOTGUN = new Weapon(WeaponIfc.Clazz.TwoHandedSmallGun, levelContainer.gameObject.GameAssets.WINCHESTER_1200_SHOTGUN);
        this.BENELLI_SUPER_90_SHOTGUN = new Weapon(WeaponIfc.Clazz.TwoHandedSmallGun, levelContainer.gameObject.GameAssets.BENELLI_SUPER_90_SHOTGUN);
        this.REMINGTON_700_SNIPER = new Weapon(WeaponIfc.Clazz.TwoHandedSmallGun, levelContainer.gameObject.GameAssets.REMINGTON_700_SNIPER);
        this.DRAGUNOV_SNIPER = new Weapon(WeaponIfc.Clazz.TwoHandedSmallGun, levelContainer.gameObject.GameAssets.DRAGUNOV_SNIPER);
        this.M82_SNIPER = new Weapon(WeaponIfc.Clazz.TwoHandedBigGuns, levelContainer.gameObject.GameAssets.M82_SNIPER);
        this.AllWeapons = new WeaponIfc[]{
            M9_PISTOL,
            M1911_PISTOL,
            DESERT_EAGLE,
            MINI_UZI_SMG,
            MP5_SMG,
            P90_SMG,
            AK47_RIFLE,
            M4A1_RIFLE,
            G36_RIFLE,
            M60_MG,
            SAW_MG,
            WINCHESTER_1200_SHOTGUN,
            BENELLI_SUPER_90_SHOTGUN,
            REMINGTON_700_SNIPER,
            DRAGUNOV_SNIPER,
            M82_SNIPER
        };
    }

    public LevelContainer getLevelContainer() {
        return levelContainer;
    }

    public WeaponIfc getM9_PISTOL() {
        return M9_PISTOL;
    }

    public WeaponIfc getM1911_PISTOL() {
        return M1911_PISTOL;
    }

    public WeaponIfc getDESERT_EAGLE() {
        return DESERT_EAGLE;
    }

    public WeaponIfc getMINI_UZI_SMG() {
        return MINI_UZI_SMG;
    }

    public WeaponIfc getMP5_SMG() {
        return MP5_SMG;
    }

    public WeaponIfc getP90_SMG() {
        return P90_SMG;
    }

    public WeaponIfc getAK47_RIFLE() {
        return AK47_RIFLE;
    }

    public WeaponIfc getM4A1_RIFLE() {
        return M4A1_RIFLE;
    }

    public WeaponIfc getG36_RIFLE() {
        return G36_RIFLE;
    }

    public WeaponIfc getM60_MG() {
        return M60_MG;
    }

    public WeaponIfc getSAW_MG() {
        return SAW_MG;
    }

    public WeaponIfc getWINCHESTER_1200_SHOTGUN() {
        return WINCHESTER_1200_SHOTGUN;
    }

    public WeaponIfc getBENELLI_SUPER_90_SHOTGUN() {
        return BENELLI_SUPER_90_SHOTGUN;
    }

    public WeaponIfc getREMINGTON_700_SNIPER() {
        return REMINGTON_700_SNIPER;
    }

    public WeaponIfc getDRAGUNOV_SNIPER() {
        return DRAGUNOV_SNIPER;
    }

    public WeaponIfc getM82_SNIPER() {
        return M82_SNIPER;
    }

}
