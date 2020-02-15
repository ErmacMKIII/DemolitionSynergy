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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Coa
 */
public class Shader {

    private final int type; // either vertex (pixel position) or fragment (pixel color)
    private final String src; // source code of the shader (glsl language)

    private int shader;

    public static int VERTEX_SHADER = GL20.GL_VERTEX_SHADER;
    public static int FRAGMENT_SHADER = GL20.GL_FRAGMENT_SHADER;

    // we'll need filename and type of shader (vertex or fragment)
    public Shader(String dirEntry, String filename, int type) {
        this.type = type;
        src = readFromFile(dirEntry, filename);
        if (src != null && src.length() > 0) {
            init();
        } else {
            DSLogger.reportError("Invalid shader filename!", null);
            System.exit(1);
        }
    }

    private String readFromFile(String dirEntry, String fileName) {
        File file = new File(Game.DATA_ZIP);
        if (!file.exists()) {
            DSLogger.reportError("Cannot find zip archive " + Game.DATA_ZIP + "!", null);
            return null;
        }
        StringBuilder text = new StringBuilder();
        ZipFile zipFile = null;
        BufferedReader br = null;
        try {
            zipFile = new ZipFile(file);
            InputStream txtInput = null;
            for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                if (zipEntry.getName().equals(dirEntry + fileName)) {
                    txtInput = zipFile.getInputStream(zipEntry);
                }
            }
            if (txtInput == null) {
                DSLogger.reportError("Cannot find resource " + dirEntry + fileName + "!", null);
                return null;
            }
            br = new BufferedReader(new InputStreamReader(txtInput));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line).append("\n");
            }
            br.close();
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                    DSLogger.reportFatalError(ex.getMessage(), ex);
                }
            }
        }
        return text.toString();
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
            DSLogger.reportError(GL20.glGetShaderInfoLog(shader, 1024), null);
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
