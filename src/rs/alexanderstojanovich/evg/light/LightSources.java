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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.intrface.Intrface;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LightSources {

    public static final Vector3f ZERO_VEC3 = new Vector3f();
    public static final float AMBIENT_LIGHT = 0.35f;

    public static final LightSources NONE = new LightSources();

    public static final int MAX_LIGHTS = 256;
    public final IList<LightSource> sourceList = new GapList<>();
    public final boolean[] modified = new boolean[MAX_LIGHTS];

    public static final String MODEL_LIGHT_NUMBER_NAME = "modelLightNumber";
    public static final String MODEL_LIGHT_NAME = "modelLights";

    public final LightOverlay lightOverlay;
    public LinkedHashMap<Vector3f, LightSource> lightMap = new LinkedHashMap<>();

    public LightSources() {
        this.lightOverlay = new LightOverlay(Window.MIN_WIDTH, Window.MIN_HEIGHT, new Texture("loverlay", Texture.Format.RGB5_A1));
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
     * @param lightSources Light Sources
     * @param shaderProgram light shader program
     */
    public static void render(Intrface intrface, Camera camera, LightSources lightSources, ShaderProgram shaderProgram) {
        lightSources.lightOverlay.bufferSmart(intrface);
        lightSources.lightOverlay.render(intrface, camera, lightSources, shaderProgram); // has shader bind
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

    /**
     * Weakening attenuation from light source by target.
     *
     * @param lightSrcPos light source position
     * @param targetPos target position
     * @return
     */
    protected static float attenuation(Vector3f lightSrcPos, Vector3f targetPos) {
        float distance = lightSrcPos.distance(targetPos);
        float attenuation = 1.0f / (1.0f - 0.07f * distance + 1.8f * distance * distance);
        attenuation = org.joml.Math.clamp(attenuation, 0.0f, 1.0f);

        return attenuation;
    }

    /**
     * Calculates diffuse light for light direction and normal
     *
     * @param lightDir light direction
     * @param normal normal of model
     * @return diffuse light
     */
    protected static float diffuseLight(Vector3f lightDir, Vector3f normal) {
        return Math.max(normal.dot(lightDir), 0.0f);
    }

    /**
     * Calculates specular light for light direction and normal
     *
     * @param lightDir light direction
     * @param normal normal of model
     * @return specular light
     */
    protected static float specularLight(Vector3f lightDir, Vector3f normal) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f reflectDir = lightDir.negate(temp1).reflect(normal, temp2);
        float specularLight = (float) Math.pow(Math.max(lightDir.dot(reflectDir), 0.0f), 32.0f);

        return specularLight;
    }

    /**
     * Calculate Ambient + Diffuse + Specular as vertex color from single light
     * source. Much more simple light calculations. Calculation is done for
     * whole model.
     *
     * @param ls light sources
     * @param modelPos model position
     * @param vertexCount vertex count to scale light calculation
     *
     * @return ambient + diffuse + specular as 3 elem struct
     */
//    public static LightStruct lightColorRGB(LightSource ls, Vector3f modelPos, int vertexCount) {
//        // copy start vertex color
//        Vector3f ambient = new Vector3f(AMBIENT_LIGHT);
//        Vector3f diffuse = new Vector3f(0.0f);
//        Vector3f specular = new Vector3f(0.0f);
//
//        float distSqr = Vector3f.distanceSquared(
//                ls.pos.x, ls.pos.y, ls.pos.z,
//                modelPos.x, modelPos.y, modelPos.z
//        );
//
//        Vector3f temp1 = new Vector3f();
//        Vector3f temp2 = new Vector3f();
//
//        Vector3f temp3 = new Vector3f();
//        Vector3f temp4 = new Vector3f();
//
//        if (distSqr != 0.0f) {
//            Vector3f lightDir = ls.pos.sub(modelPos, temp1);
//            if (lightDir.lengthSquared() != 0.0f) {
//                lightDir = lightDir.normalize(temp2);
//                float attenuation = attenuation(ls.pos, modelPos);
//                for (int j = 0; j < 6; j++) {
//                    Vector3f normalX = Block.FACE_NORMALS[j];
//                    Vector3f temp5 = new Vector3f();
//
//                    normalX = normalX.normalize(temp3);
//                    diffuse = diffuse.fma(diffuseLight(lightDir, normalX) * ls.intensity * attenuation, ls.color, temp4);
//                    specular = specular.fma(specularLight(lightDir, normalX) * ls.intensity * attenuation, ls.color, temp5);
//                }
//            }
//        } else {
//            diffuse = diffuse.fma(2.0f, ls.color, temp3);
//            specular = specular.fma(2.0f, ls.color, temp4);
//        }
//
//        return new LightStruct(ambient, diffuse, specular);
//    }
    /**
     * Calculate Ambient + Diffuse + Specular as vertex color from single light
     * source. Calculation is done per vertex.
     *
     * @param ls light sources
     * @param modelPos model position
     * @param vxPos pixel or vertex position
     * @param normal normal associated with the vertex
     * @param modelMatrix modelMat4 matrix to transform
     *
     * @return ambient + diffuse + specular as 3 elem struct
     */
//    public static LightStruct lightColorRGB(LightSource ls, Vector3f modelPos, Vector3f vxPos, Vector3f normal, Matrix4f modelMatrix) {
//        // copy start vertex color
//        Vector3f ambient = new Vector3f(AMBIENT_LIGHT);
//        Vector3f diffuse = new Vector3f(0.0f);
//        Vector3f specular = new Vector3f(0.0f);
//
//        Vector3f temp1 = new Vector3f();
//
//        float distSqr = Vector3f.distanceSquared(
//                ls.pos.x, ls.pos.y, ls.pos.z,
//                modelPos.x, modelPos.y, modelPos.z
//        );
//
//        Vector3f temp2 = new Vector3f();
//        Vector4f temp3 = new Vector4f();
//        Vector3f temp4 = new Vector3f();
//
//        if (distSqr != 0.0f) {
//            Vector4f varVertexPos4f = new Vector4f(vxPos, 1.0f);
//            varVertexPos4f = modelMatrix.transform(varVertexPos4f, temp3);
//            Vector3f varVertexPos3f = new Vector3f(varVertexPos4f.x, varVertexPos4f.y, varVertexPos4f.z);
//            Vector3f lightDir = ls.pos.sub(varVertexPos3f, temp2);
//            if (lightDir.lengthSquared() != 0.0f) {
//                for (int j = 0; j < 6; j++) {
//                    float attenuation = attenuation(ls.pos, varVertexPos3f);
//                    Vector3f lightDirX = lightDir.mul(Block.FACE_NORMALS[j], temp1);
//                    Vector3f temp5 = new Vector3f();
//                    Vector3f temp6 = new Vector3f();
//                    Vector3f temp7 = new Vector3f();
//                    if (lightDirX.lengthSquared() != 0.0f) {
//                        lightDirX = lightDirX.normalize(temp5);
//                        diffuse = diffuse.fma(diffuseLight(lightDirX, normal) * ls.intensity * attenuation, ls.color, temp6);
//                        specular = specular.fma(specularLight(lightDirX, normal) * ls.intensity * attenuation, ls.color, temp7);
//                    } else {
//                        lightDir = lightDir.normalize(temp5);
//                        diffuse = diffuse.fma(ls.intensity * attenuation / 6.0f, ls.color, temp6);
//                        specular = specular.fma(ls.intensity * attenuation / 6.0f, ls.color, temp7);
//                    }
//                }
//            }
//        } else {
//            diffuse = diffuse.fma(ls.intensity / 512.0f, ls.color, temp2);
//            specular = specular.fma(ls.intensity / 512.0f, ls.color, temp4);
//        }
//
//        return new LightStruct(ambient, diffuse, specular);
//    }
    /**
     * Render Ambient + Diffuse + Specular for each model (block) in model mesh
     *
     * @param lsx light source collection
     * @param model to update (light) color
     */
//    public static void updateLights(LightSources lsx, Model model) {
//        Mesh mesh = model.meshes.getFirst();
//        final Vector3f ambient = new Vector3f(GlobalColors.BLACK);
//        final Vector3f diffuse = new Vector3f(GlobalColors.BLACK);
//        final Vector3f specular = new Vector3f(GlobalColors.BLACK);
//
//        for (Vertex vx : mesh.vertices) {
//            // no need to change colors on disabled (vertices)
//            if (vx.isEnabled()) {
//                // reset color to black
//                // add color so it is not black
//                for (LightSource ls : lsx.sourceList) {
//                    LightStruct lightColorRGB = LightSources.lightColorRGB(
//                            ls,
//                            model.pos,
//                            vx.getPos(),
//                            vx.getNormal(),
//                            model.getModelMatrix()
//                    );
//                    // add light color from each light source to vertex
//                    ambient.add(lightColorRGB.ambient);
//                    diffuse.add(lightColorRGB.diffuse);
//                    specular.add(lightColorRGB.specular);
//                }
//            }
//        }
//
//        ambient.div(mesh.vertices.size());
//        diffuse.div(mesh.vertices.size());
//        specular.div(mesh.vertices.size());
//
//        Material mat = model.materials.getFirst();
//        mat.setAmbient(new Vector4f(ambient, 1.0f));
//        mat.setAmbient(new Vector4f(diffuse, 1.0f));
//        mat.setAmbient(new Vector4f(specular, 1.0f));
//    }
    /**
     * Render Ambient + Diffuse + Specular for each model (block) in model mesh
     *
     * @param ls single light source
     * @param model to update (light) color
     */
//    public static void updateLights(LightSource ls, Model model) {
//        Mesh mesh = model.meshes.getFirst();
//        final Vector3f ambient = new Vector3f(GlobalColors.BLACK);
//        final Vector3f diffuse = new Vector3f(GlobalColors.BLACK);
//        final Vector3f specular = new Vector3f(GlobalColors.BLACK);
//
//        for (Vertex vx : mesh.vertices) {
//            // no need to change colors on disabled (vertices)
//            if (vx.isEnabled()) {
//                // reset color to black
//                // add color so it is not black
//                LightStruct lightColorRGB = LightSources.lightColorRGB(
//                        ls,
//                        model.pos,
//                        vx.getPos(),
//                        vx.getNormal(),
//                        model.getModelMatrix()
//                );
//                // add light color from each light source to vertex
//                ambient.add(lightColorRGB.ambient);
//                diffuse.add(lightColorRGB.diffuse);
//                specular.add(lightColorRGB.specular);
//            }
//        }
//        ambient.div(mesh.vertices.size());
//        diffuse.div(mesh.vertices.size());
//        specular.div(mesh.vertices.size());
//
//        Material mat = model.materials.getFirst();
//        mat.setAmbient(new Vector4f(ambient, 1.0f));
//        mat.setAmbient(new Vector4f(diffuse, 1.0f));
//        mat.setAmbient(new Vector4f(specular, 1.0f));
//    }
    /**
     * Get light color from Ambient + Diffuse + Specular
     *
     * @param ambient ambient light color
     * @param diffuse diffuse light color
     * @param specular specular light color
     * @return
     */
    public static Vector4f lightColor(Vector3f ambient, Vector3f diffuse, Vector3f specular) {
        float red = ambient.x + diffuse.x + specular.x;
        float green = ambient.y + diffuse.y + specular.y;
        float blue = ambient.z + diffuse.z + specular.z;
        float alpha = 1.0f;

        return new Vector4f(new Vector3f(red, green, blue), alpha);
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
