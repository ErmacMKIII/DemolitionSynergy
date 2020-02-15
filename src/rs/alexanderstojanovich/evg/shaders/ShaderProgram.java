/*
 * Copyright (C) 2019 Coa
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
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Coa
 */
public class ShaderProgram {

    private final int program; // made to link all the shaders    
    private final List<Shader> shaders;

    private static ShaderProgram mainShader;
    private static ShaderProgram voxelShader;
    private static ShaderProgram waterBaseShader;
    private static ShaderProgram waterVoxelShader;
    private static ShaderProgram intrfaceShader;

    private static final ShaderProgram[] SHADER_PROGRAMS = new ShaderProgram[5];

    public static void initAllShaders() { // requires initialized OpenGL capabilities
        // 1. Init main shader (skybox, camera)
        Shader mainVertexShader = new Shader(Game.EFFECTS_ENTRY, "mainVS.glsl", Shader.VERTEX_SHADER);
        Shader mainFragmentShader = new Shader(Game.EFFECTS_ENTRY, "mainFS.glsl", Shader.FRAGMENT_SHADER);
        List<Shader> mainShaders = new ArrayList<>();
        mainShaders.add(mainVertexShader);
        mainShaders.add(mainFragmentShader);
        mainShader = new ShaderProgram(mainShaders);
        SHADER_PROGRAMS[0] = mainShader;
        // 2. Init voxel shader (solid blocks and fluid blocks)
        Shader voxelVertexShader = new Shader(Game.EFFECTS_ENTRY, "voxelVS.glsl", Shader.VERTEX_SHADER);
        Shader voxelFragmentShader = new Shader(Game.EFFECTS_ENTRY, "voxelFS.glsl", Shader.FRAGMENT_SHADER);
        List<Shader> voxelShaders = new ArrayList<>();
        voxelShaders.add(voxelVertexShader);
        voxelShaders.add(voxelFragmentShader);
        voxelShader = new ShaderProgram(voxelShaders);
        SHADER_PROGRAMS[1] = voxelShader;
        // 3. Init base water shader (water effects)
        Shader waterBaseVertexShader = new Shader(Game.EFFECTS_ENTRY, "waterBaseVS.glsl", Shader.VERTEX_SHADER);
        Shader waterBaseFragmentShader = new Shader(Game.EFFECTS_ENTRY, "waterBaseFS.glsl", Shader.FRAGMENT_SHADER);
        List<Shader> waterBaseShaders = new ArrayList<>();
        waterBaseShaders.add(waterBaseVertexShader);
        waterBaseShaders.add(waterBaseFragmentShader);
        waterBaseShader = new ShaderProgram(waterBaseShaders);
        SHADER_PROGRAMS[2] = waterBaseShader;
        // 4. Init voxel water shader (water effects)
        Shader waterVoxelVertexShader = new Shader(Game.EFFECTS_ENTRY, "waterVoxelVS.glsl", Shader.VERTEX_SHADER);
        Shader waterVoxelFragmentShader = new Shader(Game.EFFECTS_ENTRY, "waterVoxelFS.glsl", Shader.FRAGMENT_SHADER);
        List<Shader> waterVoxelShaders = new ArrayList<>();
        waterVoxelShaders.add(waterVoxelVertexShader);
        waterVoxelShaders.add(waterVoxelFragmentShader);
        waterVoxelShader = new ShaderProgram(waterVoxelShaders);
        SHADER_PROGRAMS[3] = waterVoxelShader;
        // 5. Init interface shader (crosshair)
        Shader intrfaceVertexShader = new Shader(Game.EFFECTS_ENTRY, "intrfaceVS.glsl", Shader.VERTEX_SHADER);
        Shader intrfaceFragmentShader = new Shader(Game.EFFECTS_ENTRY, "intrfaceFS.glsl", Shader.FRAGMENT_SHADER);
        List<Shader> intrfaceShaders = new ArrayList<>();
        intrfaceShaders.add(intrfaceVertexShader);
        intrfaceShaders.add(intrfaceFragmentShader);
        intrfaceShader = new ShaderProgram(intrfaceShaders);
        SHADER_PROGRAMS[4] = intrfaceShader;
    }

    public ShaderProgram(List<Shader> shaders) {
        program = GL20.glCreateProgram();
        this.shaders = shaders;
        initProgram();
    }

    public void attachShader(int shader) {
        GL20.glAttachShader(program, shader);
    }

    public void linkProgram() {
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            DSLogger.reportError(GL20.glGetShaderInfoLog(program, 1024), null);
            System.exit(1);
        }
    }

    public void validateProgram() {
        GL20.glValidateProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
            DSLogger.reportError(GL20.glGetShaderInfoLog(program, 1024), null);
            System.exit(1);
        }
    }

    private void initProgram() {
        // attaching all the shaders
        for (int i = 0; i < shaders.size(); i++) {
            attachShader(shaders.get(i).getShader());
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

    public void updateUniform(Vector4f vect, String name) {
        int uniformLocation = GL20.glGetUniformLocation(program, name);
        GL20.glUniform4f(uniformLocation, vect.x, vect.y, vect.z, vect.w);
    }

    public void updateUniform(Matrix4f mat, String name) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(4 * 4);
        mat.get(fb);
        int uniformLocation = GL20.glGetUniformLocation(program, name);
        GL20.glUniformMatrix4fv(uniformLocation, false, fb);
    }

    public int getProgram() {
        return program;
    }

    public List<Shader> getShaders() {
        return shaders;
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

    public static ShaderProgram[] getSHADER_PROGRAMS() {
        return SHADER_PROGRAMS;
    }

}
