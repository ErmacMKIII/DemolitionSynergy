/*
 * Copyright (C) 2020 Coa
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
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Coa
 */
public class Player extends Observer {

    private double hitPoints = 100.0;
    private Model currWeapon;
    private final Model[] weapons = Model.WEAPONS;
    private final Matrix4f viewMatrix = new Matrix4f();

    public Player(String modelFileName, Texture texture, Vector3f pos, Vector4f color, float scale) {
        super(modelFileName, texture, pos, color, scale);
    }

    public Player(Camera camera, Model model) {
        super(camera, model);
    }

    public void switchWeapon(int num) {
        currWeapon = weapons[num - 1];
    }

    @Override
    public void render() {
        super.render();
        ShaderProgram.getPlayerShader().bind();
        ShaderProgram.getPlayerShader().updateUniform(viewMatrix, "viewMatrix");
        ShaderProgram.unbind();
        if (currWeapon != null) {
            currWeapon.render(ShaderProgram.getPlayerShader());
        }
    }

    public double getHitPoints() {
        return hitPoints;
    }

    public Model[] getWeapons() {
        return weapons;
    }

    public Model getCurrWeapon() {
        return currWeapon;
    }

    public void setCurrWeapon(Model currWeapon) {
        this.currWeapon = currWeapon;
    }

}
