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
package rs.alexanderstojanovich.evg.light;

import java.util.Arrays;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LightSources {

    public static final int MAX_LIGHTS = 256;
    public final IList<LightSource> lightSrcList = new GapList<>(MAX_LIGHTS);

    public final boolean[] modified = new boolean[MAX_LIGHTS];

    public static final String MODEL_LIGHT_NUMBER_NAME = "modelLightNumber";
    public static final String MODEL_LIGHT_NAME = "modelLights";

    public final LightOverlay lightOverlay = new LightOverlay(GameObject.MY_WINDOW.getWidth(), GameObject.MY_WINDOW.getHeight(), new Texture("loverlay"));

    /**
     * Update lights unconditionally.
     *
     * @param shaderProgram shader Program where lights are used
     */
    public void updateLightsInShader(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(lightSrcList.size(), MODEL_LIGHT_NUMBER_NAME);
        shaderProgram.updateUniform(lightSrcList, MODEL_LIGHT_NAME);
    }

    /**
     * Update lights only if modified of any of them is set to true
     *
     * @param shaderProgram shader Program where lights are used
     */
    public void updateLightsInShaderIfModified(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(lightSrcList.size(), MODEL_LIGHT_NUMBER_NAME);
        shaderProgram.updateUniform(lightSrcList, modified, MODEL_LIGHT_NAME);
    }

    /**
     * Project lights to the screen.Makes feel of light environment.
     *
     * @param camera camera (3D)
     * @param lightSources Light Sources
     * @param shaderProgram light shader program
     */
    public static void render(Camera camera, LightSources lightSources, ShaderProgram shaderProgram) {
        lightSources.lightOverlay.bufferSmart();
        lightSources.lightOverlay.render(camera, lightSources, shaderProgram); // has shader bind
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
