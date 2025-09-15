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

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.PlainTextReader;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Shader {

    protected final int type; // either vertex (pixel position) or fragment (pixel color)
    protected final String src; // source code of the shader (glsl language)
    protected final String fileName;
    protected int shader;

    public static int VERTEX_SHADER = GL20.GL_VERTEX_SHADER;
    public static int FRAGMENT_SHADER = GL20.GL_FRAGMENT_SHADER;

    // we'll need filename and type of shader (vertex or fragment)
    public Shader(String dirEntry, String filename, int type) {
        this.type = type;
        this.fileName = filename;
        src = PlainTextReader.readFromFile(dirEntry, filename);
        if (src != null && src.length() > 0) {
            init();
        } else {
            DSLogger.reportError("Invalid shader filename!", null);
            System.exit(1);
        }
    }

    private void init() {
        // creating the shader
        shader = GL20.glCreateShader(type);
        if (shader == 0) {
            DSLogger.reportError("Shader creation failed!", null);
            System.exit(1);
        }
        GL20.glShaderSource(shader, src);
        // compiling the shader
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            DSLogger.reportError("@" + fileName, null);
            DSLogger.reportError(GL20.glGetShaderInfoLog(shader, 1024), null);
            GL20.glDeleteShader(shader);
            System.exit(1);
        }
    }

    public int getType() {
        return type;
    }

    public String getSrc() {
        return src;
    }

    public int getShader() {
        return shader;
    }

    public String getFileName() {
        return fileName;
    }

}
