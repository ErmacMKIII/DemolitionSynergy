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
package rs.alexanderstojanovich.evg.shaders;

import java.nio.FloatBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.light.LightSource;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class ShaderProgram {

    private final int program; // made to link all the shaders    
    private final IList<Shader> shaders;

    private static ShaderProgram lightShader; // projects lights to the screen
    private static ShaderProgram mainShader; // renders skybox & sun
    private static ShaderProgram voxelShader; // renders blocks
    private static ShaderProgram waterBaseShader; // renders skybox & sun as water reflections
    private static ShaderProgram waterVoxelShader; // renders blocks as water reflections
    private static ShaderProgram intrfaceShader; // renders interface
    private static ShaderProgram intrfaceContourShader; // renders interface contour
    private static ShaderProgram playerShader; // renders spectator & player
    private static ShaderProgram weaponShader; // renders player weaponry
    private static ShaderProgram contourShader; // renders model decals in editor
    private static ShaderProgram skyboxShader; // skybox model shader (gradient in sky)
    private static ShaderProgram shadowBaseShader; // shadow model shader
    private static ShaderProgram shadowVoxelShader; // shadow model shader

    public static final Map<String, ShaderProgram> GL_SHADERS = new LinkedHashMap<>();
    public static final IList<ShaderProgram> ENVIRONMENTAL_SHADERS = new GapList<>();
    public static final IList<ShaderProgram> ACTOR_SHADERS = new GapList<>();
    public static final IList<ShaderProgram> WATER_SHADERS = new GapList<>();
    public static final IList<ShaderProgram> SHADOW_SHADERS = new GapList<>();
    public static final IList<ShaderProgram> INTRFACE_SHADERS = new GapList<>();

    /**
     * Init all the shaders. Must be done in Game Renderer.
     */
    public static void initAllShaders() { // requires initialized OpenGL capabilities
        // 1. Light shader (primitive)
        Shader lightVertexShader = new Shader(Game.EFFECTS_ENTRY, "lightVS.glsl", Shader.VERTEX_SHADER);
        Shader lightFragmentShader = new Shader(Game.EFFECTS_ENTRY, "lightFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> lightShaders = new GapList<>();
        lightShaders.add(lightVertexShader);
        lightShaders.add(lightFragmentShader);
        lightShader = new ShaderProgram(lightShaders);
        GL_SHADERS.put("Light", lightShader);
        // 2. Init main shader (skybox, NPCs, items)
        Shader mainVertexShader = new Shader(Game.EFFECTS_ENTRY, "mainVS.glsl", Shader.VERTEX_SHADER);
        Shader mainFragmentShader = new Shader(Game.EFFECTS_ENTRY, "mainFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> mainShaders = new GapList<>();
        mainShaders.add(mainVertexShader);
        mainShaders.add(mainFragmentShader);
        mainShader = new ShaderProgram(mainShaders);
        GL_SHADERS.put("Base", mainShader);
        // 3. Init voxel shader (solid blocks and fluid blocks)
        Shader voxelVertexShader = new Shader(Game.EFFECTS_ENTRY, "voxelVS.glsl", Shader.VERTEX_SHADER);
        Shader voxelFragmentShader = new Shader(Game.EFFECTS_ENTRY, "voxelFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> voxelShaders = new GapList<>();
        voxelShaders.add(voxelVertexShader);
        voxelShaders.add(voxelFragmentShader);
        voxelShader = new ShaderProgram(voxelShaders);
        GL_SHADERS.put("Voxel", voxelShader);
        // 4. Init base water shader (water effects)
        Shader waterBaseVertexShader = new Shader(Game.EFFECTS_ENTRY, "waterBaseVS.glsl", Shader.VERTEX_SHADER);
        Shader waterBaseFragmentShader = new Shader(Game.EFFECTS_ENTRY, "waterBaseFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> waterBaseShaders = new GapList<>();
        waterBaseShaders.add(waterBaseVertexShader);
        waterBaseShaders.add(waterBaseFragmentShader);
        waterBaseShader = new ShaderProgram(waterBaseShaders);
        GL_SHADERS.put("WaterBase", waterBaseShader);
        // 5. Init voxel water shader (water effects)
        Shader waterVoxelVertexShader = new Shader(Game.EFFECTS_ENTRY, "waterVoxelVS.glsl", Shader.VERTEX_SHADER);
        Shader waterVoxelFragmentShader = new Shader(Game.EFFECTS_ENTRY, "waterVoxelFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> waterVoxelShaders = new GapList<>();
        waterVoxelShaders.add(waterVoxelVertexShader);
        waterVoxelShaders.add(waterVoxelFragmentShader);
        waterVoxelShader = new ShaderProgram(waterVoxelShaders);
        GL_SHADERS.put("WaterVoxel", waterVoxelShader);
        // 6. Init interface shader (crosshair & fonts)
        Shader intrfaceVertexShader = new Shader(Game.EFFECTS_ENTRY, "intrfaceVS.glsl", Shader.VERTEX_SHADER);
        Shader intrfaceFragmentShader = new Shader(Game.EFFECTS_ENTRY, "intrfaceFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> intrfaceShaders = new GapList<>();
        intrfaceShaders.add(intrfaceVertexShader);
        intrfaceShaders.add(intrfaceFragmentShader);
        intrfaceShader = new ShaderProgram(intrfaceShaders);
        GL_SHADERS.put("Intrface", intrfaceShader);
        // 7. Init interface shader (crosshair & fonts)
        Shader intrfaceContourVertexShader = new Shader(Game.EFFECTS_ENTRY, "intrface_contourVS.glsl", Shader.VERTEX_SHADER);
        Shader intrfaceContourFragmentShader = new Shader(Game.EFFECTS_ENTRY, "intrface_contourFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> intrfaceContourShaders = new GapList<>();
        intrfaceContourShaders.add(intrfaceContourVertexShader);
        intrfaceContourShaders.add(intrfaceContourFragmentShader);
        intrfaceContourShader = new ShaderProgram(intrfaceContourShaders);
        GL_SHADERS.put("IntrfaceContour", intrfaceShader);
        // 8. Init player shader (camera)
        Shader playerVertexShader = new Shader(Game.EFFECTS_ENTRY, "playerVS.glsl", Shader.VERTEX_SHADER);
        Shader playerFragmentShader = new Shader(Game.EFFECTS_ENTRY, "playerFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> playerShaders = new GapList<>();
        playerShaders.add(playerVertexShader);
        playerShaders.add(playerFragmentShader);
        playerShader = new ShaderProgram(playerShaders);
        GL_SHADERS.put("Player", playerShader);
        // 9. Init weapon shader (player weapons)
        Shader weaponVertexShader = new Shader(Game.EFFECTS_ENTRY, "weaponVS.glsl", Shader.VERTEX_SHADER);
        Shader weaponFragmentShader = new Shader(Game.EFFECTS_ENTRY, "weaponFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> weaponShaders = new GapList<>();
        weaponShaders.add(weaponVertexShader);
        weaponShaders.add(weaponFragmentShader);
        weaponShader = new ShaderProgram(weaponShaders);
        GL_SHADERS.put("Weapon", weaponShader);
        // 10. Init contour shader (editor)
        Shader contourVertexShader = new Shader(Game.EFFECTS_ENTRY, "contourVS.glsl", Shader.VERTEX_SHADER);
        Shader contourFragmentShader = new Shader(Game.EFFECTS_ENTRY, "contourFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> contourShaders = new GapList<>();
        contourShaders.add(contourVertexShader);
        contourShaders.add(contourFragmentShader);
        contourShader = new ShaderProgram(contourShaders);
        GL_SHADERS.put("Contour", contourShader);
        // 11. Skybox model shader (gradient in sky)
        Shader skyboxVertexShader = new Shader(Game.EFFECTS_ENTRY, "skyboxVS.glsl", Shader.VERTEX_SHADER);
        Shader skyboxFragmentShader = new Shader(Game.EFFECTS_ENTRY, "skyboxFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> skyboxShaders = new GapList<>();
        skyboxShaders.add(skyboxVertexShader);
        skyboxShaders.add(skyboxFragmentShader);
        skyboxShader = new ShaderProgram(skyboxShaders);
        GL_SHADERS.put("Skybox", skyboxShader);
        // ---------------------------------------------------------------------
        // 12. Shadow base model shader
        Shader shadowBaseVertexShader = new Shader(Game.EFFECTS_ENTRY, "shadowBaseVS.glsl", Shader.VERTEX_SHADER);
        Shader shadowBaseFragmentShader = new Shader(Game.EFFECTS_ENTRY, "shadowBaseFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> shadowBaseShaders = new GapList<>();
        shadowBaseShaders.add(shadowBaseVertexShader);
        shadowBaseShaders.add(shadowBaseFragmentShader);
        shadowBaseShader = new ShaderProgram(shadowBaseShaders);
        GL_SHADERS.put("ShadowBase", shadowBaseShader);
        // ---------------------------------------------------------------------
        // 13. Shadow voxel (instanced) model shader
        Shader shadowVoxelVertexShader = new Shader(Game.EFFECTS_ENTRY, "shadowVoxelVS.glsl", Shader.VERTEX_SHADER);
        Shader shadowVoxelFragmentShader = new Shader(Game.EFFECTS_ENTRY, "shadowVoxelFS.glsl", Shader.FRAGMENT_SHADER);
        IList<Shader> shadowVoxelShaders = new GapList<>();
        shadowVoxelShaders.add(shadowVoxelVertexShader);
        shadowVoxelShaders.add(shadowVoxelFragmentShader);
        shadowVoxelShader = new ShaderProgram(shadowVoxelShaders);
        GL_SHADERS.put("ShadowVoxel", shadowVoxelShader);
        // ---------------------------------------------------------------------
        // ENVIRONMENTAL SHADERS (MAIN, CONTOUR, VOXEL, SKYBOX)
        //----------------------------------------------------------------------
        ENVIRONMENTAL_SHADERS.add(mainShader); // environnment & npc
        ENVIRONMENTAL_SHADERS.add(contourShader); // decal
        ENVIRONMENTAL_SHADERS.add(voxelShader); // blocks
        ENVIRONMENTAL_SHADERS.add(skyboxShader); // skybox
        // ---------------------------------------------------------------------
        // ACTOR SHADERS (PLAYER, WEAPON)
        //----------------------------------------------------------------------
        ACTOR_SHADERS.add(playerShader); // player        
        ACTOR_SHADERS.add(weaponShader); // weapon in hands
        // ---------------------------------------------------------------------
        // ---------------------------------------------------------------------        
        // WATER SHADERS
        //----------------------------------------------------------------------
        WATER_SHADERS.add(waterBaseShader);
        WATER_SHADERS.add(waterVoxelShader);
        // ---------------------------------------------------------------------
        // SHADOW SHADERS
        //----------------------------------------------------------------------
        SHADOW_SHADERS.add(shadowBaseShader);
        SHADOW_SHADERS.add(shadowVoxelShader);
        // ---------------------------------------------------------------------
        // INTRFACE SHADERS
        //----------------------------------------------------------------------
        INTRFACE_SHADERS.add(intrfaceShader);
        INTRFACE_SHADERS.add(intrfaceContourShader);
        DSLogger.reportDebug("Shaders initialized!", null);
    }

    /**
     * Create new shader program from vertex & fragment shader
     *
     * @param shaders to create shader program
     */
    public ShaderProgram(IList<Shader> shaders) {
        program = GL20.glCreateProgram();
        this.shaders = shaders;
        initProgram();
    }

    /**
     * Attach shader into the program
     *
     * @param shader shader (to attach)
     */
    public void attachShader(int shader) {
        GL20.glAttachShader(program, shader);
    }

    /**
     * Link attached shader
     */
    public void linkProgram() {
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            DSLogger.reportError(GL20.glGetShaderInfoLog(program, 1024), null);
            for (Shader shader : shaders) {
                GL20.glDeleteShader(shader.getShader());
            }
            GL20.glDeleteProgram(program);
            System.exit(1);
        }
    }

    public void validateProgram() {
        GL20.glValidateProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
            DSLogger.reportError(GL20.glGetShaderInfoLog(program, 1024), null);
            for (Shader shader : shaders) {
                GL20.glDeleteShader(shader.getShader());
            }
            GL20.glDeleteProgram(program);
            System.exit(1);
        }
    }

    /**
     * Wrapper for initialization
     */
    private void initProgram() {
        // attaching all the shaders
        for (Shader sh : shaders) {
            attachShader(sh.getShader());
        }
        // linking program
        linkProgram();
        // validating program
        validateProgram();
    }

    public void bind() {
        GL20.glUseProgram(program);
    }

    public static void unbind() {
        GL20.glUseProgram(0);
    }

    public void bindAttribute(int attribute, String variableName) {
        GL20.glBindAttribLocation(program, attribute, variableName);
    }

    public void updateUniform(int value, String name) {
        int uniformLocation = GL20.glGetUniformLocation(program, name);
        GL20.glUniform1i(uniformLocation, value);
    }

    public void updateUniform(float value, String name) {
        int uniformLocation = GL20.glGetUniformLocation(program, name);
        GL20.glUniform1f(uniformLocation, value);
    }

    public void updateUniform(Vector2f vect, String name) {
        int uniformLocation = GL20.glGetUniformLocation(program, name);
        GL20.glUniform2f(uniformLocation, vect.x, vect.y);
    }

    public void updateUniform(Vector3f vect, String name) {
        int uniformLocation = GL20.glGetUniformLocation(program, name);
        GL20.glUniform3f(uniformLocation, vect.x, vect.y, vect.z);
    }

    public void updateUniform(Vector3f[] vectArr, String name) {
        for (int i = 0; i < vectArr.length; i++) {
            int uniformLocation = GL20.glGetUniformLocation(program, name + "[" + i + "]");
            GL20.glUniform3f(uniformLocation, vectArr[i].x, vectArr[i].y, vectArr[i].z);
        }
    }

    public void updateUniform(LightSource[] lightSrc, String name) {
        for (int i = 0; i < lightSrc.length; i++) {
            int locPos = GL20.glGetUniformLocation(program, name + "[" + i + "].pos");
            GL20.glUniform3f(locPos, lightSrc[i].getPos().x, lightSrc[i].getPos().y, lightSrc[i].getPos().z);

            int locCol = GL20.glGetUniformLocation(program, name + "[" + i + "].color");
            GL20.glUniform3f(locCol, lightSrc[i].getColor().x, lightSrc[i].getColor().y, lightSrc[i].getColor().z);

            int locInt = GL20.glGetUniformLocation(program, name + "[" + i + "].intensity");
            GL20.glUniform1f(locInt, lightSrc[i].getIntensity());
        }
    }

    public void updateUniform(IList<LightSource> lightSrc, String name) {
        int index = 0;
        for (LightSource ls : lightSrc) {
            int locPos = GL20.glGetUniformLocation(program, name + "[" + index + "].pos");
            GL20.glUniform3f(locPos, ls.getPos().x, ls.getPos().y, ls.getPos().z);

            int locCol = GL20.glGetUniformLocation(program, name + "[" + index + "].color");
            GL20.glUniform3f(locCol, ls.getColor().x, ls.getColor().y, ls.getColor().z);

            int locInt = GL20.glGetUniformLocation(program, name + "[" + index + "].intensity");
            GL20.glUniform1f(locInt, ls.getIntensity());

            index++;
        }
    }

    public void updateUniform(IList<LightSource> lightSrc, boolean[] modified, String name) {
        int index = 0;
        for (LightSource ls : lightSrc) {
            if (modified[index]) {
                int locPos = GL20.glGetUniformLocation(program, name + "[" + index + "].pos");
                GL20.glUniform3f(locPos, ls.getPos().x, ls.getPos().y, ls.getPos().z);

                int locCol = GL20.glGetUniformLocation(program, name + "[" + index + "].color");
                GL20.glUniform3f(locCol, ls.getColor().x, ls.getColor().y, ls.getColor().z);

                int locInt = GL20.glGetUniformLocation(program, name + "[" + index + "].intensity");
                GL20.glUniform1f(locInt, ls.getIntensity());
            }
            index++;
        }
    }

    public void updateUniform(LightSource[] lightSrc, boolean[] modified, String name) {
        int index = 0;
        for (LightSource ls : lightSrc) {
            if (ls != null && modified[index]) {
                int locPos = GL20.glGetUniformLocation(program, name + "[" + index + "].pos");
                GL20.glUniform3f(locPos, ls.getPos().x, ls.getPos().y, ls.getPos().z);

                int locCol = GL20.glGetUniformLocation(program, name + "[" + index + "].color");
                GL20.glUniform3f(locCol, ls.getColor().x, ls.getColor().y, ls.getColor().z);

                int locInt = GL20.glGetUniformLocation(program, name + "[" + index + "].intensity");
                GL20.glUniform1f(locInt, ls.getIntensity());
            }
            index++;
        }
    }

    public void updateUniform(Vector4f vect, String name) {
        int uniformLocation = GL20.glGetUniformLocation(program, name);
        GL20.glUniform4f(uniformLocation, vect.x, vect.y, vect.z, vect.w);
    }

    public void updateUniform(Matrix4f mat, String name) {
        FloatBuffer fb = MemoryUtil.memCallocFloat(4 * 4);
        if (MemoryUtil.memAddressSafe(fb) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);// Memory allocation failed
            throw new RuntimeException("Could not allocate memory address!");
        }
        mat.get(fb);
        int uniformLocation = GL20.glGetUniformLocation(program, name);
        GL20.glUniformMatrix4fv(uniformLocation, false, fb);

        if (fb.capacity() != 0) {
            MemoryUtil.memFree(fb);
        }
    }

    /**
     * Delete program with whole attached shaders.
     */
    public void deleteProgram() {
        for (Shader shader : shaders) {
            GL20.glDeleteShader(shader.getShader());
        }
        GL20.glDeleteProgram(program);
    }

    /**
     * Delete all shader programs
     */
    public static void deleteAllShaders() {
        for (ShaderProgram sp : GL_SHADERS.values()) {
            sp.deleteProgram();
        }
    }

    public int getProgram() {
        return program;
    }

    public IList<Shader> getShaders() {
        return shaders;
    }

    public static ShaderProgram getLightShader() {
        return lightShader;
    }

    public static ShaderProgram getMainShader() {
        return mainShader;
    }

    public static ShaderProgram getVoxelShader() {
        return voxelShader;
    }

    public static ShaderProgram getWaterBaseShader() {
        return waterBaseShader;
    }

    public static ShaderProgram getIntrfaceShader() {
        return intrfaceShader;
    }

    public static ShaderProgram getWaterVoxelShader() {
        return waterVoxelShader;
    }

    public static ShaderProgram getPlayerShader() {
        return playerShader;
    }

    public static ShaderProgram getWeaponShader() {
        return weaponShader;
    }

    public static ShaderProgram getContourShader() {
        return contourShader;
    }

    public static ShaderProgram getSkyboxShader() {
        return skyboxShader;
    }

    public static ShaderProgram getShadowVoxelShader() {
        return shadowVoxelShader;
    }

    public static ShaderProgram getShadowBaseShader() {
        return shadowBaseShader;
    }

    public static ShaderProgram getIntrfaceContourShader() {
        return intrfaceContourShader;
    }

}
