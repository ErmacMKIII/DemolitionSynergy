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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import rs.alexanderstojanovich.evg.main.Game;

/**
 *
 * @author Coa
 */
public class Shader {

    private final int type;
    private final String src;

    private int shader;

    public static int VERTEX_SHADER = GL20.GL_VERTEX_SHADER;
    public static int FRAGMENT_SHADER = GL20.GL_FRAGMENT_SHADER;

    public static final String EFFECTS_DIR = "/effects/";

    // we'll need filename and type of shader (vertex or fragment)
    public Shader(String filename, int type) {
        this.type = type;
        src = readFromFile(filename);
        if (src.length() > 0) {
            init();
        } else {
            System.err.println("Invalid shader filename!");
            System.exit(1);
        }
    }

    private String readFromFile(String fileName) {
        StringBuilder text = new StringBuilder();
        InputStream in = getClass().getResourceAsStream(Game.RESOURCES_DIR + EFFECTS_DIR + fileName);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line).append("\n");
            }
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Shader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Shader.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (br != null) {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(Shader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return text.toString();
    }

    private void init() {
        // creating the shader
        shader = GL20.glCreateShader(type);
        if (shader == 0) {
            System.err.println("Shader creation failed!");
            System.exit(1);
        }
        GL20.glShaderSource(shader, src);
        // ccompiling the shader
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.out.println(GL20.glGetShaderInfoLog(shader, 1024));
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

}
