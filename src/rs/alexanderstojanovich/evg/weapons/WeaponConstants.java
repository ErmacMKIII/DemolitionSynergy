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
 * Weapons interface defining weapon models and textures. Contains constants for
 * weapon positions, texture names, and model objects.
 *
 * Author: Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface WeaponConstants {

    /**
     * Position of the weapon in the game world
     */
    public static final Vector3f WEAPON_POS = new Vector3f(1.0f, -1.0f, 3.0f);

    // WeaponConstants for weapon texture names
    public static final String W01M9 = "W01M9";
    public static final String W02M1 = "W02M1";
    public static final String W03DE = "W03DE";
    public static final String W04UZ = "W04UZ";
    public static final String W05M5 = "W05M5";
    public static final String W06P9 = "W06P9";
    public static final String W07AK = "W07AK";
    public static final String W08M4 = "W08M4";
    public static final String W09G3 = "W09G3";
    public static final String W10M6 = "W10M6";
    public static final String W11MS = "W11MS";
    public static final String W12W2 = "W12W2";
    public static final String W13B9 = "W13B9";
    public static final String W14R7 = "W14R7";
    public static final String W15DR = "W15DR";
    public static final String W16M8 = "W16M8";

    /**
     * Array of weapon texture names
     */
    public static final String[] TEX_WEAPONS = {
        W01M9,
        W02M1,
        W03DE,
        W04UZ,
        W05M5,
        W06P9,
        W07AK,
        W08M4,
        W09G3,
        W10M6,
        W11MS,
        W12W2,
        W13B9,
        W14R7,
        W15DR,
        W16M8
    };

    // WeaponConstants for weapon model objects
    public static final Model M9_PISTOL = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W01M9 + ".obj", W01M9, 4, false);
    public static final Model M1911_PISTOL = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W02M1 + ".obj", W02M1, 4, false);
    public static final Model DESERT_EAGLE = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W03DE + ".obj", W03DE, 4, false);
    public static final Model MINI_UZI_SMG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W04UZ + ".obj", W04UZ, 4, false);
    public static final Model MP5_SMG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W05M5 + ".obj", W05M5, 4, false);
    public static final Model P90_SMG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W06P9 + ".obj", W06P9, 4, false);
    public static final Model AK47_RIFLE = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W07AK + ".obj", W07AK, 4, false);
    public static final Model M4A1_RIFLE = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W08M4 + ".obj", W08M4, 4, false);
    public static final Model G36_RIFLE = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W09G3 + ".obj", W09G3, 4, false);
    public static final Model M60_MG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W10M6 + ".obj", W10M6, 4, false);
    public static final Model SAW_MG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W11MS + ".obj", W11MS, 4, false);
    public static final Model WINCHESTER_1200_SHOTGUN = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W12W2 + ".obj", W12W2, 4, false);
    public static final Model BENELLI_SUPER_90_SHOTGUN = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W13B9 + ".obj", W13B9, 4, false);
    public static final Model REMINGTON_700_SNIPER = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W14R7 + ".obj", W14R7, 4, false);
    public static final Model DRAGUNOV_SNIPER = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W15DR + ".obj", W15DR, 4, false);
    public static final Model M82_SNIPER = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W16M8 + ".obj", W16M8, 4, false);

    /**
     * Array of weapon models
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
