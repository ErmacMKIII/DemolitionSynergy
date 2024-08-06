/*
 * Copyright (C) 2023 coas9
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
package rs.alexanderstojanovich.evg.models;

import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public interface Renderable {

    /**
     * Render model. (Draw all meshes)
     *
     * @param lightSources light sources {SUN, PLAYER_WEAPONS, OTHER LIGHT,
     * BLOCKS etc}
     * @param shaderProgram shaderProgram for the models
     */
    public void render(LightSources lightSources, ShaderProgram shaderProgram);

    /**
     * Render model. (Draw all meshes) Contour shader is more specific.
     *
     * @param lightSources light sources {SUN, PLAYER_WEAPONS, OTHER LIGHT,
     * BLOCKS etc}
     * @param shaderProgram shaderProgram for the models (contour shader
     * program)
     */
    public void renderContour(LightSources lightSources, ShaderProgram shaderProgram);
}
