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
package rs.alexanderstojanovich.evg.critter;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Player extends Observer {

    private double hitPoints = 100.0;
    private Model currWeapon;
    private final Matrix4f viewMatrix = new Matrix4f();

    public static final Model PISTOL = new Model(false, Game.PLAYER_ENTRY, "pistol.obj",
            "pistol", new Vector3f(1.0f, -1.0f, 3.0f), LevelContainer.SKYBOX_COLOR, false);
    public static final Model SUB_MACHINE_GUN = new Model(false, Game.PLAYER_ENTRY, "sub_machine_gun.obj",
            "smg", new Vector3f(1.0f, -1.0f, 3.0f), LevelContainer.SKYBOX_COLOR, false);
    public static final Model SHOTGUN = new Model(false, Game.PLAYER_ENTRY, "shotgun.obj",
            "shotgun", new Vector3f(1.0f, -1.0f, 3.0f), LevelContainer.SKYBOX_COLOR, false);
    public static final Model ASSAULT_RIFLE = new Model(false, Game.PLAYER_ENTRY, "assault_rifle.obj",
            "assrifle", new Vector3f(1.0f, -1.0f, 3.0f), LevelContainer.SKYBOX_COLOR, false);
    public static final Model MACHINE_GUN = new Model(false, Game.PLAYER_ENTRY, "machine_gun.obj",
            "machgun", new Vector3f(1.0f, -1.0f, 3.0f), LevelContainer.SKYBOX_COLOR, false);
    public static final Model SNIPER_RIFLE = new Model(false, Game.PLAYER_ENTRY, "sniper_rifle.obj",
            "sniper", new Vector3f(1.0f, -1.0f, 3.0f), LevelContainer.SKYBOX_COLOR, false);
    public static final Model[] WEAPONS = {PISTOL, SUB_MACHINE_GUN, SHOTGUN, ASSAULT_RIFLE, MACHINE_GUN, SNIPER_RIFLE};

    static {
        for (Model weapon : WEAPONS) {
            weapon.setScale(6.0f);
            weapon.setrY((float) (-Math.PI / 2.0f));
        }
    }

    public Player(Model currWeapon, String modelFileName, String texName, Vector3f pos, Vector3f color, float scale) {
        super(modelFileName, texName, pos, color, scale);
        this.currWeapon = currWeapon;
    }

    public Player(Model currWeapon, Camera camera, Model model) {
        super(camera, model);
        this.currWeapon = currWeapon;
    }

    public void switchWeapon(int num) {
        currWeapon = WEAPONS[num - 1];
    }

    @Override
    public void render() {
        super.render();
        ShaderProgram.getPlayerShader().bind();
        ShaderProgram.getPlayerShader().updateUniform(viewMatrix, "viewMatrix");
        ShaderProgram.unbind();
        if (currWeapon != null) {
            if (!currWeapon.isBuffered()) {
                currWeapon.bufferAll();
            }
            currWeapon.render(ShaderProgram.getPlayerShader());
        }
    }

    public double getHitPoints() {
        return hitPoints;
    }

    public Model getCurrWeapon() {
        return currWeapon;
    }

    public void setCurrWeapon(Model currWeapon) {
        this.currWeapon = currWeapon;
    }

}
