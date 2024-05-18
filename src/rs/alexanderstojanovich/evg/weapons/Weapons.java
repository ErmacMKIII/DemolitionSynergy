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
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.util.ModelUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Weapons {

    public static final Vector3f WEAPON_POS = new Vector3f(1.0f, -1.0f, 3.0f);

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
    public static final String[] TEX_WEAPONS = {
        "W01M9",
        "W02M1",
        "W03DE",
        "W04UZ",
        "W05M5",
        "W06P9",
        "W07AK",
        "W08M4",
        "W09G3",
        "W10M6",
        "W11MS",
        "W12W2",
        "W13B9",
        "W14R7",
        "W15DR",
        "W16M8"
    };

    public static final Model M9_PISTOL = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W01M9.obj", "W01M9", false);
    public static final Model M1911_PISTOL = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W02M1.obj", "W02M1", false);
    public static final Model DESERT_EAGLE = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W03DE.obj", "W03DE", false);
    public static final Model MINI_UZI_SMG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W04UZ.obj", "W04UZ", false);
    public static final Model MP5_SMG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W05M5.obj", "W05M5", false);
    public static final Model P90_SMG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W06P9.obj", "W06P9", false);
    public static final Model AK47_RIFLE = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W07AK.obj", "W07AK", false);
    public static final Model M4A1_RIFLE = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W08M4.obj", "W08M4", false);
    public static final Model G36_RIFLE = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W09G3.obj", "W09G3", false);
    public static final Model M60_MG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W10M6.obj", "W10M6", false);
    public static final Model SAW_MG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W11MS.obj", "W11MS", false);
    public static final Model WINCHESTER_1200_SHOTGUN = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W12W2.obj", "W12W2", false);
    public static final Model BENELLI_SUPER_90_SHOTGUN = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W13B9.obj", "W13B9", false);
    public static final Model REMINGTON_700_SNIPER = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W14R7.obj", "W14R7", false);
    public static final Model DRAGUNOV_SNIPER = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W15DR.obj", "W15DR", false);
    public static final Model M82_SNIPER = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, "W16M8.obj", "W16M8", false);

    /**
     * Weapon models
     */
    public static final Model[] MODELS = {
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
