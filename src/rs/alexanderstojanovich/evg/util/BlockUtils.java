/*
 * Copyright (C) 2023 coas9
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
package rs.alexanderstojanovich.evg.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector2f;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Block;
import static rs.alexanderstojanovich.evg.models.Block.INDICES;
import static rs.alexanderstojanovich.evg.models.Block.VERTICES;
import rs.alexanderstojanovich.evg.models.Vertex;

/**
 *
 * @author coas9
 */
public class BlockUtils {

    /*
    * Reads the block from Cube plaintext file (similar to OBJ). Obsolete.
     */
    @Deprecated
    private static void readFromTxtFile(String fileName) {
        InputStream in = Block.class.getResourceAsStream(Game.RESOURCES_DIR + fileName);
        if (in == null) {
            DSLogger.reportError("Cannot resource dir " + Game.RESOURCES_DIR + "!", null);
            return;
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("v:")) {
                    String[] things = line.replace("v:", "").trim().split(",|->", -1);
                    Vector3f pos = new Vector3f(Float.parseFloat(things[0]), Float.parseFloat(things[1]), Float.parseFloat(things[2]));
                    Vector3f normal = new Vector3f(Float.parseFloat(things[3]), Float.parseFloat(things[4]), Float.parseFloat(things[5]));
                    Vector2f uv = new Vector2f(Float.parseFloat(things[6]), Float.parseFloat(things[7]));
                    Vertex v = new Vertex(pos, normal, uv);
                    VERTICES.add(v);
                } else if (line.startsWith("i:")) {
                    String[] things = line.replace("i:", "").trim().split(" ", -1);
                    INDICES.add(Integer.valueOf(things[0]));
                    INDICES.add(Integer.valueOf(things[1]));
                    INDICES.add(Integer.valueOf(things[2]));
                }
            }
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
        if (br != null) {
            try {
                br.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        }
    }

    /*
    * Reads the block from Cube[X] plaintext file (similar to OBJ)
     */
    public static void readFromTxtFileMK2(String fileName) {
        VERTICES.clear();
        INDICES.clear();

        InputStream in = Block.class.getResourceAsStream(Game.RESOURCES_DIR + fileName);
        if (in == null) {
            DSLogger.reportError("Cannot resource dir " + Game.RESOURCES_DIR + "!", null);
            return;
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(in));
            List<Vector3f> positions = new ArrayList<>();
            List<Vector2f> uvs = new ArrayList<>();
            List<Vector3f> normals = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("v:")) {
                    String[] things = line.split("\\s+");
                    Vector3f pos = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    positions.add(pos);
                } else if (line.startsWith("t:")) {
                    String[] things = line.split("\\s+");
                    Vector2f uv = new Vector2f(Float.parseFloat(things[1]), Float.parseFloat(things[2]));
                    uvs.add(uv);
                } else if (line.startsWith("n:")) {
                    String[] things = line.split("\\s+");
                    Vector3f normal = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    normals.add(normal);
                } else if (line.startsWith("i:")) {
                    String[] things = line.split("\\s+");
                    for (String thing : things) {
                        if (thing.equals("i:")) {
                            continue;
                        }

                        String[] subThings = thing.split("/");

                        int indexOfVertex = Integer.parseInt(subThings[0]);
                        Vector3f pos = new Vector3f(positions.get(indexOfVertex));

                        int indexOfUv = Integer.parseInt(subThings[1]);
                        Vector2f uv = new Vector2f(uvs.get(indexOfUv));

                        int indexOfNormal = Integer.parseInt(subThings[2]);
                        Vector3f normal = new Vector3f(normals.get(indexOfNormal));

                        Vertex vertex = new Vertex(pos, normal, uv);

                        if (!VERTICES.contains(vertex)) {
                            VERTICES.add(vertex);
                        }

                        INDICES.add(VERTICES.lastIndexOf(vertex));
                    }

                }
            }
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
        if (br != null) {
            try {
                br.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        }
    }

}
