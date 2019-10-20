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

/**
 *
 * @author Coa
 */
public class ShaderProgram {

    private final int program; // made to link all the shaders    
    private final List<Shader> shaders;

    private static ShaderProgram mainShader;
    private static ShaderProgram voxelShader;
    private static ShaderProgram waterShader;
    private static ShaderProgram intrfaceShader;

    public static void initAllShaders() { // requires initialized OpenGL capabilities
        // 1. Init main shader (skybox, camera)
        Shader mainVertexShader = new Shader("mainVS.glsl", Shader.VERTEX_SHADER);
        Shader mainFragmentShader = new Shader("mainFS.glsl", Shader.FRAGMENT_SHADER);
        List<Shader> mainShaders = new ArrayList<>();
        mainShaders.add(mainVertexShader);
        mainShaders.add(mainFragmentShader);
        mainShader = new ShaderProgram(mainShaders);
        // 2. Init voxel shader (solid blocks and fluid blocks)
        Shader voxelVertexShader = new Shader("voxelVS.glsl", Shader.VERTEX_SHADER);
        Shader voxelFragmentShader = new Shader("voxelFS.glsl", Shader.FRAGMENT_SHADER);
        List<Shader> voxelShaders = new ArrayList<>();
        voxelShaders.add(voxelVertexShader);
        voxelShaders.add(voxelFragmentShader);
        voxelShader = new ShaderProgram(voxelShaders);
        // 3. Init water shader (water effects)
        Shader waterVertexShader = new Shader("waterVS.glsl", Shader.VERTEX_SHADER);
        Shader waterFragmentShader = new Shader("waterFS.glsl", Shader.FRAGMENT_SHADER);
        List<Shader> waterShaders = new ArrayList<>();
        waterShaders.add(waterVertexShader);
        waterShaders.add(waterFragmentShader);
        waterShader = new ShaderProgram(waterShaders);
        // 4. Init interface shader (crosshair, text, menus and fonts)
        Shader intrfaceVertexShader = new Shader("intrfaceVS.glsl", Shader.VERTEX_SHADER);
        Shader intrfaceFragmentShader = new Shader("intrfaceFS.glsl", Shader.FRAGMENT_SHADER);
        List<Shader> intrfaceShaders = new ArrayList<>();
        intrfaceShaders.add(intrfaceVertexShader);
        intrfaceShaders.add(intrfaceFragmentShader);
        intrfaceShader = new ShaderProgram(intrfaceShaders);
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
            System.out.println(GL20.glGetShaderInfoLog(program, 1024));
            System.exit(1);
        }
    }

    public void validateProgram() {
        GL20.glValidateProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
            System.out.println(GL20.glGetShaderInfoLog(program, 1024));
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

    public void updateUniform(float number, String name) {
        int uniformLocation = GL20.glGetUniformLocation(program, name);
        GL20.glUniform1f(uniformLocation, number);
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

    public static ShaderProgram getWaterShader() {
        return waterShader;
    }

    public static ShaderProgram getIntrfaceShader() {
        return intrfaceShader;
    }

}
