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
package rs.alexanderstojanovich.evg.resources;

import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.ModelUtils;

/**
 * Game assets.
 *
 * All game textures and models.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Assets {

    public static final String[] TEX_WORLD = {"crate", "doom0", "stone", "water", "reflc"};
    public static final int GRID_SIZE_WORLD = 3;

    public final Texture DECAL = new Texture(Game.WORLD_ENTRY, "decal.png", Texture.Format.RGBA8);
    public final Texture QMARK = new Texture(Game.WORLD_ENTRY, "qmark.png", Texture.Format.RGBA8);

    public final Texture SUN = new Texture(Game.WORLD_ENTRY, "suntx.png", Texture.Format.RGBA8);
    public final Texture DAY = new Texture(Game.WORLD_ENTRY, "day.png", Texture.Format.RGBA8);
    public final Texture NIGHT = new Texture(Game.WORLD_ENTRY, "night.png", Texture.Format.RGBA8);

    public final Texture LOGO = new Texture(Game.INTRFACE_ENTRY, "dsynergy-title.png", Texture.Format.RGBA8);
    public final Texture CROSSHAIR = new Texture(Game.INTRFACE_ENTRY, "crosshairUltimate.png", Texture.Format.RGBA8);
    public final Texture POINTER = new Texture(Game.INTRFACE_ENTRY, "deagle.png", Texture.Format.RGBA8);
    public final Texture FONT = new Texture(Game.INTRFACE_ENTRY, "font.png", Texture.Format.RGBA8);
    public final Texture CONSOLE = new Texture(Game.INTRFACE_ENTRY, "console.png", Texture.Format.RGBA8);
    public final Texture SPLASH = new Texture(Game.INTRFACE_ENTRY, "splash.png", Texture.Format.RGBA8);
    public final Texture LIGHT_BULB = new Texture(Game.INTRFACE_ENTRY, "lbulb.png", Texture.Format.RGBA8);

    public final Texture ALEX = new Texture(Game.CHARACTER_ENTRY, "alex.png", Texture.Format.RGBA8);
    public final Texture STEVE = new Texture(Game.CHARACTER_ENTRY, "steve.png", Texture.Format.RGBA8);
    public final int GRID_SIZE_PLAYER_WEAPONS = 4;

    public final Texture WORLD = Texture.buildTextureAtlas("WORLD", Game.WORLD_ENTRY, TEX_WORLD, GRID_SIZE_WORLD, Texture.Format.RGBA8);
    public final Texture PLAYER_WEAPONS = Texture.buildTextureAtlas("WEAPONS", Game.WEAPON_ENTRY, TEX_WEAPONS, GRID_SIZE_PLAYER_WEAPONS, Texture.Format.RGBA8);

    public final Texture WATERFX = new Texture(Game.WORLD_ENTRY, "waterfx.png", Texture.Format.RGB5_A1);

    public IList<String> LIGHT_TEX_LIST = new GapList<String>() {
        {
            add("suntx");
            add("reflc");
        }
    };

    /**
     * Model of player unarmed (Default)
     */
    public final Model PLAYER_BODY_DEFAULT = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player.obj", "alex", 1, true);
    /**
     * Model of player with one-handed small guns (Pistols)
     */
    public final Model PLAYER_BODY_1H_SG = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-1H-SG.obj", "alex", 1, true);
    /**
     * Model of player with two-handed small guns (SMGs, Rifles, Shotguns &
     * Sniper non M82)
     */
    public final Model PLAYER_BODY_2H_SG = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG.obj", "alex", 1, true);
    /**
     * Model of player with two-handed big guns (M60, SAW & M82)
     */
    public final Model PLAYER_BODY_2H_BG = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-BG.obj", "alex", 1, true);

    /**
     * Position of the weapon in the game world
     */
//    public static final Vector3f WEAPON_POS = new Vector3f(1.0f, -1.0f, 3.0f);
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
     * * Array of weapon texture names
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

    public final Model M9_PISTOL = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W01M9 + ".obj", W01M9, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model M1911_PISTOL = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W02M1 + ".obj", W02M1, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model DESERT_EAGLE = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W03DE + ".obj", W03DE, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model MINI_UZI_SMG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W04UZ + ".obj", W04UZ, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model MP5_SMG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W05M5 + ".obj", W05M5, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model P90_SMG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W06P9 + ".obj", W06P9, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model AK47_RIFLE = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W07AK + ".obj", W07AK, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model M4A1_RIFLE = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W08M4 + ".obj", W08M4, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model G36_RIFLE = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W09G3 + ".obj", W09G3, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model M60_MG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W10M6 + ".obj", W10M6, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model SAW_MG = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W11MS + ".obj", W11MS, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model WINCHESTER_1200_SHOTGUN = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W12W2 + ".obj", W12W2, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model BENELLI_SUPER_90_SHOTGUN = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W13B9 + ".obj", W13B9, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model REMINGTON_700_SNIPER = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W14R7 + ".obj", W14R7, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model DRAGUNOV_SNIPER = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W15DR + ".obj", W15DR, GRID_SIZE_PLAYER_WEAPONS, false);
    public final Model M82_SNIPER = ModelUtils.readFromObjFile(Game.WEAPON_ENTRY, W16M8 + ".obj", W16M8, GRID_SIZE_PLAYER_WEAPONS, false);

    /**
     * Buffer All Textures. Call from Game Renderer.
     */
    public void bufferAllTextures() {
        // intrface
        SPLASH.bufferAll();
        LOGO.bufferAll();
        CROSSHAIR.bufferAll();
        POINTER.bufferAll();
        FONT.bufferAll();
        CONSOLE.bufferAll();
        LIGHT_BULB.bufferAll();
        // world        
        DECAL.bufferAll();
        SUN.bufferAll();
        QMARK.bufferAll();
        WORLD.bufferAll();
        DAY.bufferAll();
        NIGHT.bufferAll();
        WATERFX.bufferAll();
        // player
        ALEX.bufferAll();
        STEVE.bufferAll();
        // weapons
        PLAYER_WEAPONS.bufferAll();

        DSLogger.reportDebug("Textures loaded!", null);
    }

    public void bufferAllModels() {

    }
}
