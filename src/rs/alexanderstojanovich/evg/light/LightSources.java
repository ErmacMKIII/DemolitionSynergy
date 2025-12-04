/*
 * Copyright (C) 2022 Aleksandar Stojanovic <coas91@rocketmail.com>
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.intrface.Intrface;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class LightSources {

    public static final Vector3f ZERO_VEC3 = new Vector3f();

    public static final LightSources NONE = new LightSources();

    public static final int MAX_LIGHTS = 256;
    public final IList<LightSource> sourceList = new GapList<>();
    public final boolean[] modified = new boolean[MAX_LIGHTS];

    public static final String MODEL_LIGHT_NUMBER_NAME = "modelLightNumber";
    public static final String MODEL_LIGHT_NAME = "modelLights";

    public final LightOverlay lightOverlay;
    public LinkedHashMap<Vector3f, LightSource> lightMap = new LinkedHashMap<>();

    public LightSources() {
        this.lightOverlay = new LightOverlay(Window.MIN_WIDTH, Window.MIN_HEIGHT, Texture.getOrDefault("waterfx"), null);
    }

    /**
     * Update lights unconditionally.
     *
     * @param shaderProgram shader Program where lights are used
     */
    public void updateLightsInShader(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(sourceList.size(), MODEL_LIGHT_NUMBER_NAME);
        shaderProgram.updateUniform(sourceList, MODEL_LIGHT_NAME);
    }

    /**
     * Update lights only if modified of any of them is set to true
     *
     * @param shaderProgram shader Program where lights are used
     */
    public void updateLightsInShaderIfModified(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(sourceList.size(), MODEL_LIGHT_NUMBER_NAME);
        shaderProgram.updateUniform(sourceList, modified, MODEL_LIGHT_NAME);
    }

    /**
     * Project lights to the screen.Makes feel of light environment.
     *
     * @param intrface intrface
     * @param camera camera (3D)
     * @param lc Level Container (contains Light Sources)
     * @param shaderProgram light shader program
     */
    public static void render(Intrface intrface, Camera camera, LevelContainer lc, ShaderProgram shaderProgram) {
        lc.lightSources.lightOverlay.bufferSmart(intrface);
        lc.lightSources.lightOverlay.render(intrface, camera, lc, shaderProgram); // has shader bind
    }

    public void clearLights() {
        sourceList.clear();
        lightMap.clear();
    }

    /**
     * Adds a light source.
     *
     * @param ls light source
     */
    public void addLight(LightSource ls) {
        sourceList.add(ls);
        lightMap.put(ls.pos, ls);
    }

    /**
     * Update light source (with new pos).
     *
     * @param index index in source list
     * @param ls light source
     */
    public void updateLight(int index, LightSource ls) {
        LightSource ls0 = sourceList.get(index);
        if (lightMap.containsKey(ls0.pos)) {
            lightMap.replace(ls.pos, ls);
        }
    }

    /**
     * Removes a light source.
     *
     * @param ls light source
     */
    public void removeLight(LightSource ls) {
        sourceList.remove(ls);
        lightMap.remove(ls.pos);
    }

    /**
     * Retain lights
     *
     * @param indexLastExclusive last index to keep
     */
    public void retainLights(int indexLastExclusive) {
        sourceList.retain(0, indexLastExclusive);
        Set<Vector3f> keySet = lightMap.keySet();
        for (Vector3f pos : keySet) {
            LightSource light = sourceList.filter(ls -> ls.pos.equals(pos)).getFirstOrNull();
            if (light != null && sourceList.indexOf(light) >= indexLastExclusive) {
                sourceList.remove(light);
                lightMap.remove(pos);
            }
        }
    }

    /**
     * Removes a light source.
     *
     * @param pos light source position
     */
    public void removeLight(Vector3f pos) {
        LightSource ls = lightMap.get(pos);
        if (ls != null) {
            sourceList.remove(ls);
            lightMap.remove(ls.pos);
        } else {
            sourceList.removeIf(lsx -> lsx.pos.equals(pos));
        }
    }

    /**
     * Get light source to modified.
     *
     * @param index index of light source
     * @return is modified
     */
    public boolean isModified(int index) {
        return this.modified[index];
    }

    /**
     * Get light source to modified.
     *
     * @param pos position of the light
     * @return is light source modified
     */
    public boolean isModified(Vector3f pos) {
        LightSource ls = lightMap.get(pos);
        if (ls != null) {
            int index = sourceList.indexOf(ls);
            if (index != -1) {
                return this.modified[index];
            }
        }

        return false;
    }

    /**
     * Set light source to modified.
     *
     * @param index index of light source
     * @param modified modified boolean
     */
    public void setModified(int index, boolean modified) {
        this.modified[index] = modified;
    }

    /**
     * Set light source to modified.
     *
     * @param pos position of the light
     * @param modified modified boolean
     */
    public void setModified(Vector3f pos, boolean modified) {
        LightSource ls = lightMap.get(pos);
        if (ls != null) {
            int index = sourceList.indexOf(ls);
            if (index != -1) {
                this.modified[index] = modified;
            }
        }
    }

    public void setAllModified() {
        Arrays.fill(modified, true);
    }

    public void resetAllModified() {
        Arrays.fill(modified, false);
    }

    public IList<LightSource> getSourceList() {
        return sourceList;
    }

    public Map<Vector3f, LightSource> getLightMap() {
        return lightMap;
    }

    public LightOverlay getLightOverlay() {
        return lightOverlay;
    }

    public boolean[] getModified() {
        return modified;
    }

}
