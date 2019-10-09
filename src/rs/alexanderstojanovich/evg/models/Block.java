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
package rs.alexanderstojanovich.evg.models;

import rs.alexanderstojanovich.evg.core.Texture;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.main.Game;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Coa
 */
public class Block extends Model {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int BOTTOM = 2;
    public static final int TOP = 3;
    public static final int BACK = 4;
    public static final int FRONT = 5;
    private boolean[] enabledFaces; // which faces we enabled for rendering and which we disabled    

    public Block(ShaderProgram shaderProgram) {
        super();
        this.shaderProgram = shaderProgram;
        enabledFaces = new boolean[6];
        Arrays.fill(enabledFaces, true);
        readFromTxtFile("cube.txt");
        bufferVertices();
        bufferIndices();
        calcDims();
    }

    public Block(String textureFileName, ShaderProgram shaderProgram) {
        super();
        this.primaryTexture = new Texture(textureFileName);
        this.shaderProgram = shaderProgram;
        this.enabledFaces = new boolean[6];
        Arrays.fill(enabledFaces, true);
        readFromTxtFile("cube.txt");
        bufferVertices();
        bufferIndices();
        calcDims();
    }

    public Block(String textureFileName, ShaderProgram shaderProgram, Vector3f pos, Vector4f primaryColor, boolean passable) {
        super();
        this.primaryTexture = new Texture(textureFileName);
        this.shaderProgram = shaderProgram;
        this.enabledFaces = new boolean[6];
        Arrays.fill(enabledFaces, true);
        this.pos = pos;
        this.primaryColor = primaryColor;
        this.passable = passable;
        readFromTxtFile("cube.txt");
        bufferVertices();
        bufferIndices();
        calcDims();
    }

    private void readFromTxtFile(String fileName) {
        InputStream in = getClass().getResourceAsStream(Game.RESOURCES_DIR + fileName);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("v:")) {
                    String[] things = line.split(" ");
                    Vector3f pos = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    Vector3f normal = new Vector3f(Float.parseFloat(things[5]), Float.parseFloat(things[6]), Float.parseFloat(things[7]));
                    Vector2f uv = new Vector2f(Float.parseFloat(things[9]), Float.parseFloat(things[10]));
                    Vertex v = new Vertex(pos, normal, uv);
                    vertices.add(v);
                } else if (line.startsWith("i:")) {
                    String[] things = line.split(" ");
                    indices.add(Integer.parseInt(things[1]));
                    indices.add(Integer.parseInt(things[2]));
                    indices.add(Integer.parseInt(things[3]));
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Block.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Block.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (br != null) {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(Block.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void bufferVertices() {
        // storing vertices and normals in the buffer
        FloatBuffer fb = BufferUtils.createFloatBuffer(vertices.size() * Vertex.SIZE);
        for (int i = 0; i < vertices.size(); i++) {
            fb.put(vertices.get(i).getPos().x);
            fb.put(vertices.get(i).getPos().y);
            fb.put(vertices.get(i).getPos().z);

            fb.put(vertices.get(i).getNormal().x);
            fb.put(vertices.get(i).getNormal().y);
            fb.put(vertices.get(i).getNormal().z);

            fb.put(vertices.get(i).getUv().x);
            fb.put(vertices.get(i).getUv().y);
        }
        fb.flip();
        // storing vertices and normals buffer on the graphics card
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fb, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private void bufferIndices() {
        // storing indices in the buffer
        IntBuffer ib = BufferUtils.createIntBuffer(indices.size());
        for (int i = 0; i < indices.size(); i++) {
            ib.put(indices.get(i));
        }
        ib.flip();
        // storing indices buffer on the graphics card
        ibo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ib, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void calcDims() {
        Vector3f vect = vertices.get(0).getPos();
        float xMin = vect.x;
        float yMin = vect.y;
        float zMin = vect.z;

        float xMax = vect.x;
        float yMax = vect.y;
        float zMax = vect.z;

        for (int i = 1; i < vertices.size(); i++) {
            vect = vertices.get(i).getPos();
            xMin = Math.min(xMin, vect.x);
            yMin = Math.min(yMin, vect.y);
            zMin = Math.min(zMin, vect.z);

            xMax = Math.max(xMax, vect.x);
            yMax = Math.max(yMax, vect.y);
            zMax = Math.max(zMax, vect.z);
        }

        width = Math.abs(xMax - xMin) * scale;
        height = Math.abs(yMax - yMin) * scale;
        depth = Math.abs(zMax - zMin) * scale;
    }

    @Override
    public String toString() {
        return "Block{" + "texture=" + primaryTexture.getImage().getFileName() + ", pos=" + pos + ", scale=" + scale + ", color=" + primaryColor + ", passable=" + passable + '}';
    }

    public int faceAdjacentBy(Block block) { // which face of "this" is adjacent to compared "block"
        int faceNum = -1;
        if (Math.abs((this.pos.x - this.width / 2) - (block.pos.x + block.width / 2)) <= 2 * Game.EPSILON
                && Math.abs(this.getPos().y - block.getPos().y) <= Game.EPSILON
                && Math.abs(this.getPos().z - block.getPos().z) <= Game.EPSILON) {
            faceNum = LEFT;
        } else if (Math.abs((this.pos.x + this.width / 2) - (block.pos.x - block.width / 2)) <= 2 * Game.EPSILON
                && Math.abs(this.getPos().y - block.getPos().y) <= Game.EPSILON
                && Math.abs(this.getPos().z - block.getPos().z) <= Game.EPSILON) {
            faceNum = RIGHT;
        } else if (Math.abs((this.pos.y - this.height / 2) - (block.pos.y + block.height / 2)) <= 2 * Game.EPSILON
                && Math.abs(this.getPos().z - block.getPos().z) <= Game.EPSILON
                && Math.abs(this.getPos().x - block.getPos().x) <= Game.EPSILON) {
            faceNum = BOTTOM;
        } else if (Math.abs((this.pos.y + this.height / 2) - (block.pos.y - block.height / 2)) <= 2 * Game.EPSILON
                && Math.abs(this.getPos().z - block.getPos().z) <= Game.EPSILON
                && Math.abs(this.getPos().x - block.getPos().x) <= Game.EPSILON) {
            faceNum = TOP;
        } else if (Math.abs((this.pos.z - this.depth / 2) - (block.pos.z + block.depth / 2)) <= 2 * Game.EPSILON
                && Math.abs(this.getPos().x - block.getPos().x) <= Game.EPSILON
                && Math.abs(this.getPos().y - block.getPos().y) <= Game.EPSILON) {
            faceNum = BACK;
        } else if (Math.abs((this.pos.z + this.depth / 2) - (block.pos.z - block.depth / 2)) <= 2 * Game.EPSILON
                && Math.abs(this.getPos().x - block.getPos().x) <= Game.EPSILON
                && Math.abs(this.getPos().y - block.getPos().y) <= Game.EPSILON) {
            faceNum = FRONT;
        }
        return faceNum;
    }

    public void removeFace(int faceNum) {
        if (faceNum >= 0 && faceNum <= 5 && enabledFaces[faceNum]) {
            ArrayList<Integer> collection = new ArrayList<Integer>();

            for (int i = 0; i < 4; i++) {
                collection.add(4 * faceNum + i);
            }
            if (indices.containsAll(collection)) {
                indices.removeAll(collection);
                enabledFaces[faceNum] = false;
                bufferIndices();
            }
        }
    }

    public void addFace(int faceNum) {
        if (faceNum >= 0 && faceNum <= 5 && !enabledFaces[faceNum]) {
            ArrayList<Integer> collection = new ArrayList<Integer>();

            collection.add(4 * faceNum);
            collection.add(4 * faceNum + 1);
            collection.add(4 * faceNum + 2);

            collection.add(4 * faceNum + 2);
            collection.add(4 * faceNum + 3);
            collection.add(4 * faceNum);

            if (!indices.containsAll(collection)) {
                indices.addAll(collection);
                enabledFaces[faceNum] = true;
                bufferIndices();
            }
        }
    }

    public void reconstructAllFaces() {
        for (int i = 0; i < 6; i++) {
            if (!enabledFaces[i]) {
                addFace(i);
            }
        }
    }

    public void destructAllFaces() {
        for (int i = 0; i < 6; i++) {
            if (enabledFaces[i]) {
                removeFace(i);
            }
        }
    }

    public void setUVsForSkybox() {
        revertGroupsOfVertices();
        // LEFT
        vertices.get(4 * LEFT).getUv().x = 0.5f;
        vertices.get(4 * LEFT).getUv().y = 1.0f / 3.0f;

        vertices.get(4 * LEFT + 1).getUv().x = 0.25f;
        vertices.get(4 * LEFT + 1).getUv().y = 1.0f / 3.0f;

        vertices.get(4 * LEFT + 2).getUv().x = 0.25f;
        vertices.get(4 * LEFT + 2).getUv().y = 2.0f / 3.0f;

        vertices.get(4 * LEFT + 3).getUv().x = 0.5f;
        vertices.get(4 * LEFT + 3).getUv().y = 2.0f / 3.0f;
        // BACK
        for (int i = 0; i < 4; i++) {
            vertices.get(4 * BACK + i).getUv().x = vertices.get(4 * LEFT + i).getUv().x - 0.25f;
            vertices.get(4 * BACK + i).getUv().y = vertices.get(4 * LEFT + i).getUv().y;
        }
        // FRONT
        for (int i = 0; i < 4; i++) {
            vertices.get(4 * FRONT + i).getUv().x = vertices.get(4 * LEFT + i).getUv().x + 0.25f;
            vertices.get(4 * FRONT + i).getUv().y = vertices.get(4 * LEFT + i).getUv().y;
        }
        // RIGHT
        for (int i = 0; i < 4; i++) {
            vertices.get(4 * RIGHT + i).getUv().x = vertices.get(4 * FRONT + i).getUv().x + 0.25f;
            vertices.get(4 * RIGHT + i).getUv().y = vertices.get(4 * FRONT + i).getUv().y;
        }
        // TOP
        for (int i = 0; i < 4; i++) {
            vertices.get(4 * TOP + i).getUv().x = vertices.get(4 * LEFT + i).getUv().x;
            vertices.get(4 * TOP + i).getUv().y = vertices.get(4 * LEFT + i).getUv().y - 1.0f / 3.0f;
        }
        // BOTTOM
        for (int i = 0; i < 4; i++) {
            vertices.get(4 * BOTTOM + i).getUv().x = vertices.get(4 * LEFT + i).getUv().x;
            vertices.get(4 * BOTTOM + i).getUv().y = vertices.get(4 * LEFT + i).getUv().y + 1.0f / 3.0f;
        }
        bufferVertices();
    }

    private void revertGroupsOfVertices() {
        Collections.reverse(vertices.subList(4 * LEFT, 4 * LEFT + 3));
        Collections.reverse(vertices.subList(4 * RIGHT, 4 * RIGHT + 3));
        Collections.reverse(vertices.subList(4 * BOTTOM, 4 * BOTTOM + 3));
        Collections.reverse(vertices.subList(4 * TOP, 4 * TOP + 3));
        Collections.reverse(vertices.subList(4 * BACK, 4 * BACK + 3));
        Collections.reverse(vertices.subList(4 * FRONT, 4 * FRONT + 3));
    }

    public boolean[] getEnabledFaces() {
        return enabledFaces;
    }

    public void setEnabledFaces(boolean[] enabledFaces) {
        this.enabledFaces = enabledFaces;
    }

}
