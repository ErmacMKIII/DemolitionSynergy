/*
 * Copyright (C) 2022 Alexander Stojanovich <coas91@rocketmail.com>
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
package rs.alexanderstojanovich.evg.level;

import java.util.Arrays;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LightSources {

    public static final int MAX_LIGHTS = 256;
    protected final IList<LightSource> lightSrcList = new GapList<>(MAX_LIGHTS);

    public final boolean[] modified = new boolean[MAX_LIGHTS];

    public static final String MODEL_LIGHT_NUMBER_NAME = "modelLightNumber";
    public static final String MODEL_LIGHT_NAME = "modelLights";

    /**
     * Update lights if modified of any of them is set to true
     *
     * @param shaderProgram shader Program where lights are used
     */
    public void updateLightsInShader(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(lightSrcList.size(), MODEL_LIGHT_NUMBER_NAME);
        shaderProgram.updateUniform(lightSrcList, modified, MODEL_LIGHT_NAME);
    }

    public void setAllModified() {
        Arrays.fill(modified, true);
    }

    public void resetAllModified() {
        Arrays.fill(modified, false);
    }

    public IList<LightSource> getLightSrcList() {
        return lightSrcList;
    }

    public boolean[] getModified() {
        return modified;
    }

}
