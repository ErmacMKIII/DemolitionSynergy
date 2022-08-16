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

import java.util.ArrayList;
import java.util.List;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LightSources {

    public static final int MAX_LIGHTS = 256;
    protected final List<LightSource> lightSrcList = new ArrayList<>(MAX_LIGHTS);

    protected boolean modified = false;

    public static final String MODEL_LIGHT_NUMBER_NAME = "modelLightNumber";
    public static final String MODEL_LIGHT_NAME = "modelLights";

    public synchronized boolean updateLightsInShader(ShaderProgram shaderProgram) {
        boolean uniformsUpdated = false;
        if (modified) {
            shaderProgram.updateUniform(lightSrcList.size(), MODEL_LIGHT_NUMBER_NAME);
            shaderProgram.updateUniform(lightSrcList, MODEL_LIGHT_NAME);
            uniformsUpdated = true;
        }
        return uniformsUpdated;
    }

    public synchronized List<LightSource> getLightSrcList() {
        return lightSrcList;
    }

    public synchronized boolean isModified() {
        return modified;
    }

    public synchronized void setModified(boolean modified) {
        this.modified = modified;
    }

}
