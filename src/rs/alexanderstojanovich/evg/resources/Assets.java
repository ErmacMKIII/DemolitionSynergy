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
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
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

    public final int GRID_SIZE_PLAYER_WEAPONS = 4;
    public final int GRID_SIZE_PLAYER = 5;

    public final Texture WORLD = Texture.buildTextureAtlas("WORLD", Game.WORLD_ENTRY, TEX_WORLD, GRID_SIZE_WORLD, Texture.Format.RGBA8);
    public final Texture PLAYER = Texture.buildTextureAtlas("PLAYER", Game.CHARACTER_ENTRY, TEX_PLAYER, GRID_SIZE_PLAYER, Texture.Format.RGBA8);
    public final Texture PLAYER_WEAPONS = Texture.buildTextureAtlas("WEAPONS", Game.WEAPON_ENTRY, TEX_WEAPONS, GRID_SIZE_PLAYER_WEAPONS, Texture.Format.RGBA8);

    public final Texture WATERFX = new Texture(Game.WORLD_ENTRY, "waterfx.png", Texture.Format.RGB5_A1);

    public IList<String> LIGHT_TEX_LIST = new GapList<String>() {
        {
            add("suntx");
            add("reflc");
        }
    };

    // -------------------------------------------------------------------------
    /**
     * Model of player unarmed (Default)
     */
    public final Model ALEX_BODY_DEFAULT = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player.obj", "alex", GRID_SIZE_PLAYER, true);

    // -------------------------------------------------------------------------
    /**
     * Model of player with one-handed small guns (W01M9)
     */
    public final Model ALEX_BODY_1H_SG_W01M9 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-1H-SG-W01M9.obj", new String[]{"alex", "W01M9"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with one-handed small guns (W02M1)
     */
    public final Model ALEX_BODY_1H_SG_W02M1 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-1H-SG-W02M1.obj", new String[]{"alex", "W02M1"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with one-handed small guns (W03DE)
     */
    public final Model ALEX_BODY_1H_SG_W03DE = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-1H-SG-W03DE.obj", new String[]{"alex", "W03DE"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with one-handed small guns (W04UZ)
     */
    public final Model ALEX_BODY_1H_SG_W04UZ = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-1H-SG-W04UZ.obj", new String[]{"alex", "W04UZ"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed big guns (W10M6)
     */
    public final Model ALEX_BODY_2H_BG_W10M6 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-BG-W10M6.obj", new String[]{"alex", "W10M6"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed big guns (W11MS)
     */
    public final Model ALEX_BODY_2H_BG_W11MS = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-BG-W11MS.obj", new String[]{"alex", "W11MS"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed big guns (W16M8)
     */
    public final Model ALEX_BODY_2H_BG_W16M8 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-BG-W16M8.obj", new String[]{"alex", "W16M8"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W05M5)
     */
    public final Model ALEX_BODY_2H_SG_W05M5 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W05M5.obj", new String[]{"alex", "W05M5"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W06P9)
     */
    public final Model ALEX_BODY_2H_SG_W06P9 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W06P9.obj", new String[]{"alex", "W06P9"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W07AK)
     */
    public final Model ALEX_BODY_2H_SG_W07AK = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W07AK.obj", new String[]{"alex", "W07AK"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W08M4)
     */
    public final Model ALEX_BODY_2H_SG_W08M4 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W08M4.obj", new String[]{"alex", "W08M4"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W09G3)
     */
    public final Model ALEX_BODY_2H_SG_W09G3 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W09G3.obj", new String[]{"alex", "W09G3"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W12W2)
     */
    public final Model ALEX_BODY_2H_SG_W12W2 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W12W2.obj", new String[]{"alex", "W12W2"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W13B9)
     */
    public final Model ALEX_BODY_2H_SG_W13B9 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W13B9.obj", new String[]{"alex", "W13B9"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W14R7)
     */
    public final Model ALEX_BODY_2H_SG_W14R7 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W14R7.obj", new String[]{"alex", "W14R7"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W15DR)
     */
    public final Model ALEX_BODY_2H_SG_W15DR = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W15DR.obj", new String[]{"alex", "W15DR"}, GRID_SIZE_PLAYER, true);

    //--------------------------------------------------------------------------
    /**
     * Model of player unarmed (Default)
     */
    public final Model STEVE_BODY_DEFAULT = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player.obj", "steve", GRID_SIZE_PLAYER, true);

    // -------------------------------------------------------------------------
    /**
     * Model of player with one-handed small guns (W01M9)
     */
    public final Model STEVE_BODY_1H_SG_W01M9 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-1H-SG-W01M9.obj", new String[]{"steve", "W01M9"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with one-handed small guns (W02M1)
     */
    public final Model STEVE_BODY_1H_SG_W02M1 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-1H-SG-W02M1.obj", new String[]{"steve", "W02M1"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with one-handed small guns (W03DE)
     */
    public final Model STEVE_BODY_1H_SG_W03DE = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-1H-SG-W03DE.obj", new String[]{"steve", "W03DE"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with one-handed small guns (W04UZ)
     */
    public final Model STEVE_BODY_1H_SG_W04UZ = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-1H-SG-W04UZ.obj", new String[]{"steve", "W04UZ"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed big guns (W10M6)
     */
    public final Model STEVE_BODY_2H_BG_W10M6 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-BG-W10M6.obj", new String[]{"steve", "W10M6"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed big guns (W11MS)
     */
    public final Model STEVE_BODY_2H_BG_W11MS = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-BG-W11MS.obj", new String[]{"steve", "W11MS"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed big guns (W16M8)
     */
    public final Model STEVE_BODY_2H_BG_W16M8 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-BG-W16M8.obj", new String[]{"steve", "W16M8"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W05M5)
     */
    public final Model STEVE_BODY_2H_SG_W05M5 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W05M5.obj", new String[]{"steve", "W05M5"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W06P9)
     */
    public final Model STEVE_BODY_2H_SG_W06P9 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W06P9.obj", new String[]{"steve", "W06P9"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W07AK)
     */
    public final Model STEVE_BODY_2H_SG_W07AK = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W07AK.obj", new String[]{"steve", "W07AK"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W08M4)
     */
    public final Model STEVE_BODY_2H_SG_W08M4 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W08M4.obj", new String[]{"steve", "W08M4"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W09G3)
     */
    public final Model STEVE_BODY_2H_SG_W09G3 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W09G3.obj", new String[]{"steve", "W09G3"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W12W2)
     */
    public final Model STEVE_BODY_2H_SG_W12W2 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W12W2.obj", new String[]{"steve", "W12W2"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W13B9)
     */
    public final Model STEVE_BODY_2H_SG_W13B9 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W13B9.obj", new String[]{"steve", "W13B9"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W14R7)
     */
    public final Model STEVE_BODY_2H_SG_W14R7 = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W14R7.obj", new String[]{"steve", "W14R7"}, GRID_SIZE_PLAYER, true);

    /**
     * Model of player with two-handed small guns (W15DR)
     */
    public final Model STEVE_BODY_2H_SG_W15DR = ModelUtils.readFromObjFile(Game.CHARACTER_ENTRY, "player-2H-SG-W15DR.obj", new String[]{"steve", "W15DR"}, GRID_SIZE_PLAYER, true);

    //--------------------------------------------------------------------------
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

    /**
     * Array of player texture names. Armed with weapon or unarmed without
     * weapon (default).
     */
    public static final String[] TEX_PLAYER = {
        "alex",
        "steve",
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
        PLAYER.bufferAll();
        // weapons
        PLAYER_WEAPONS.bufferAll();

        DSLogger.reportDebug("Textures loaded!", null);
    }

    public void bufferAllModels() {
        ALEX_BODY_DEFAULT.bufferAll();

        ALEX_BODY_1H_SG_W01M9.bufferAll();
        ALEX_BODY_1H_SG_W02M1.bufferAll();
        ALEX_BODY_1H_SG_W03DE.bufferAll();
        ALEX_BODY_1H_SG_W04UZ.bufferAll();

        ALEX_BODY_2H_SG_W05M5.bufferAll();
        ALEX_BODY_2H_SG_W06P9.bufferAll();
        ALEX_BODY_2H_SG_W07AK.bufferAll();
        ALEX_BODY_2H_SG_W08M4.bufferAll();
        ALEX_BODY_2H_SG_W09G3.bufferAll();
        ALEX_BODY_2H_SG_W12W2.bufferAll();
        ALEX_BODY_2H_SG_W13B9.bufferAll();
        ALEX_BODY_2H_SG_W14R7.bufferAll();
        ALEX_BODY_2H_SG_W15DR.bufferAll();

        ALEX_BODY_2H_BG_W10M6.bufferAll();
        ALEX_BODY_2H_BG_W11MS.bufferAll();
        ALEX_BODY_2H_BG_W16M8.bufferAll();

        STEVE_BODY_DEFAULT.bufferAll();

        STEVE_BODY_1H_SG_W01M9.bufferAll();
        STEVE_BODY_1H_SG_W02M1.bufferAll();
        STEVE_BODY_1H_SG_W03DE.bufferAll();
        STEVE_BODY_1H_SG_W04UZ.bufferAll();

        STEVE_BODY_2H_SG_W05M5.bufferAll();
        STEVE_BODY_2H_SG_W06P9.bufferAll();
        STEVE_BODY_2H_SG_W07AK.bufferAll();
        STEVE_BODY_2H_SG_W08M4.bufferAll();
        STEVE_BODY_2H_SG_W09G3.bufferAll();
        STEVE_BODY_2H_SG_W12W2.bufferAll();
        STEVE_BODY_2H_SG_W13B9.bufferAll();
        STEVE_BODY_2H_SG_W14R7.bufferAll();
        STEVE_BODY_2H_SG_W15DR.bufferAll();

        STEVE_BODY_2H_BG_W10M6.bufferAll();
        STEVE_BODY_2H_BG_W11MS.bufferAll();
        STEVE_BODY_2H_BG_W16M8.bufferAll();
    }
}
