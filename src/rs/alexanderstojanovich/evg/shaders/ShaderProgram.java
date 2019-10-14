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

import java.util.List;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 *
 * @author Coa
 */
public class ShaderProgram {

    private int program; // made to link all the shaders    
    private final List<Shader> shaders;

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

    public int getProgram() {
        return program;
    }

    public List<Shader> getShaders() {
        return shaders;
    }

}
