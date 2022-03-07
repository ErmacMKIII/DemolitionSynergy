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

import java.util.List;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.models.Model;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface Critter { // interface for Observer, Player and NPC

    public void moveForward(float amount);

    public void moveBackward(float amount);

    public void moveLeft(float amount);

    public void moveRight(float amount);

    //--------------------------------------------------------------------------
    public void movePredictorForward(float amount);

    public void movePredictorBackward(float amount);

    public void movePredictorLeft(float amount);

    public void movePredictorRight(float amount);

    //--------------------------------------------------------------------------
    public void turnLeft(float angle);

    public void turnRight(float angle);

    public void lookAt(float mouseSensitivity, float xoffset, float yoffset);

    public void render(List<Vector3f> lightSrc);

    //--------------------------------------------------------------------------
    public boolean isBuffered();

    public void bufferAll();

    //--------------------------------------------------------------------------
    @Override
    public String toString();

    public Camera getCamera();

    public Model getModel();

    public boolean isGivenControl();

    public void setGivenControl(boolean givenControl);

    public Vector3f getPredictor();

}
