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
package rs.alexanderstojanovich.evg.models;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.joml.Intersectionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Pair;
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Block extends Model {

    public static final int NONE = -1;
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int BOTTOM = 2;
    public static final int TOP = 3;
    public static final int BACK = 4;
    public static final int FRONT = 5;
    // which faces we enabled for rendering and which we disabled
    private final boolean[] enabledFaces = new boolean[6];

    private boolean verticesReversed = false;

    public static final List<Vector3f> FACE_NORMALS = new ArrayList<>();

    public static final int VERTEX_COUNT = 24;
    public static final int INDICES_COUNT = 36;

    public static final List<Vertex> VERTICES = new GapList<>();
    public static final List<Integer> INDICES = new ArrayList<>();

    public static final Comparator<Block> Y_AXIS_COMP = new Comparator<Block>() {
        @Override
        public int compare(Block o1, Block o2) {
            if (o1.getPos().y > o2.getPos().y) {
                return 1;
            } else if (o1.getPos().y == o2.getPos().y) {
                return 0;
            } else {
                return -1;
            }
        }
    };

    static {
        FACE_NORMALS.add(new Vector3f(-1.0f, 0.0f, 0.0f));
        FACE_NORMALS.add(new Vector3f(1.0f, 0.0f, 0.0f));
        FACE_NORMALS.add(new Vector3f(0.0f, -1.0f, 0.0f));
        FACE_NORMALS.add(new Vector3f(0.0f, 1.0f, 0.0f));
        FACE_NORMALS.add(new Vector3f(0.0f, 0.0f, -1.0f));
        FACE_NORMALS.add(new Vector3f(0.0f, 0.0f, 1.0f));

        readFromTxtFile("cube.txt");
    }

    public Block(String texName) {
        super("block.txt", texName);
        Arrays.fill(enabledFaces, true);
        deepCopyTo(vertices);
        indices.addAll(INDICES);
        calcDims();
    }

    public Block(String texName, Vector3f pos, Vector3f primaryColor, boolean solid) {
        super("block.txt", texName, pos, primaryColor, solid);
        Arrays.fill(enabledFaces, true);
        deepCopyTo(vertices);
        indices.addAll(INDICES);
        calcDims();
    }

    // cuz regular shallow copy doesn't work, for List of integers is applicable
    public static void deepCopyTo(List<Vertex> vertices) {
        for (Vertex v : VERTICES) {
            vertices.add(new Vertex(new Vector3f(v.getPos()), new Vector3f(v.getNormal()), new Vector2f(v.getUv())));
        }
    }

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
                    INDICES.add(Integer.parseInt(things[0]));
                    INDICES.add(Integer.parseInt(things[1]));
                    INDICES.add(Integer.parseInt(things[2]));
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

    @Override
    public void bufferIndices() {
        // storing indices in the buffer
        IntBuffer ib = createIntBuffer(getFaceBits());
        // storing indices buffer on the graphics card
        if (ibo == 0) {
            ibo = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ib, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    @Override
    public void bufferAll() { // explicit call to buffer unbuffered before the rendering
        bufferVertices();
        bufferIndices();
        buffered = true;
    }

    private void calcDims() {
        final Vector3f minv = new Vector3f(-1.0f, -1.0f, -1.0f);
        final Vector3f maxv = new Vector3f(1.0f, 1.0f, 1.0f);

        width = Math.abs(maxv.x - minv.x) * scale;
        height = Math.abs(maxv.y - minv.y) * scale;
        depth = Math.abs(maxv.z - minv.z) * scale;
    }

    @Override
    public String toString() {
        return "Block{" + "texture=" + texName + ", pos=" + pos + ", scale=" + scale + ", color=" + primaryColor + ", solid=" + solid + '}';
    }

    public int faceAdjacentBy(Block block) { // which face of "this" is adjacent to compared "block"
        int faceNum = NONE;
        if (((this.pos.x - this.width / 2.0f) - (block.pos.x + block.width / 2.0f)) == 0.0f
                && (this.getPos().y - block.getPos().y) == 0.0f
                && (this.getPos().z - block.getPos().z) == 0.0f) {
            faceNum = LEFT;
        } else if (((this.pos.x + this.width / 2.0f) - (block.pos.x - block.width / 2.0f)) == 0.0f
                && (this.getPos().y - block.getPos().y) == 0.0f
                && (this.getPos().z - block.getPos().z) == 0.0f) {
            faceNum = RIGHT;
        } else if (((this.pos.y - this.height / 2.0f) - (block.pos.y + block.height / 2.0f)) == 0.0f
                && (this.getPos().z - block.getPos().z) == 0.0f
                && (this.getPos().x - block.getPos().x) == 0.0f) {
            faceNum = BOTTOM;
        } else if (((this.pos.y + this.height / 2.0f) - (block.pos.y - block.height / 2.0f)) == 0.0f
                && (this.getPos().z - block.getPos().z) == 0.0f
                && (this.getPos().x - block.getPos().x) == 0.0f) {
            faceNum = TOP;
        } else if (((this.pos.z - this.depth / 2.0f) - (block.pos.z + block.depth / 2.0f)) == 0.0f
                && (this.getPos().x - block.getPos().x) == 0.0f
                && (this.getPos().y - block.getPos().y) == 0.0f) {
            faceNum = BACK;
        } else if (((this.pos.z + this.depth / 2.0f) - (block.pos.z - block.depth / 2.0f)) == 0.0f
                && (this.getPos().x - block.getPos().x) == 0.0f
                && (this.getPos().y - block.getPos().y) == 0.0f) {
            faceNum = FRONT;
        }
        return faceNum;
    }

    public Pair<Integer, Integer> getFaceVertices(int faceNum) {
        return new Pair<>(4 * faceNum, 4 * (faceNum + 1));
    }

    public boolean canBeSeenBy(Vector3f front, Vector3f pos) {
        boolean bool = false;
        int counter = 0;
        for (Vector3f normal : FACE_NORMALS) {
            Vector3f temp1 = new Vector3f();
            Vector3f vx = normal.add(this.pos, temp1).normalize(temp1);
            Vector3f temp2 = new Vector3f();
            Vector3f vy = front.add(pos, temp2).normalize(temp2);
            if (Math.abs(vx.dot(vy)) >= 0.25f) {
                counter++;
                break;
            }
        }
        if (counter >= 1 && counter <= 3) {
            bool = true;
        }
        return bool;
    }

    public void disableFace(int faceNum) {
        Pair<Integer, Integer> faceVertices = getFaceVertices(faceNum);
        for (int i = faceVertices.getKey(); i < faceVertices.getValue(); i++) {
            vertices.get(i).setEnabled(false);
        }
        this.enabledFaces[faceNum] = false;
    }

    public void enableFace(int faceNum) {
        Pair<Integer, Integer> faceVertices = getFaceVertices(faceNum);
        for (int i = faceVertices.getKey(); i < faceVertices.getValue(); i++) {
            vertices.get(i).setEnabled(true);
        }
        this.enabledFaces[faceNum] = true;
    }

    public void enableAllFaces() {
        for (Vertex vertex : vertices) {
            vertex.setEnabled(true);
        }
        Arrays.fill(enabledFaces, true);
    }

    public void disableAllFaces() {
        for (Vertex vertex : vertices) {
            vertex.setEnabled(false);
        }
        Arrays.fill(enabledFaces, false);
    }

    public void reverseFaceVertexOrder() {
        for (int j = 0; j <= 5; j++) {
            Pair<Integer, Integer> faceVertices = getFaceVertices(j);
            Collections.reverse(vertices.subList(faceVertices.getKey(), faceVertices.getValue()));
        }
        verticesReversed = !verticesReversed;
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

    }

    private void revertGroupsOfVertices() {
        Collections.reverse(vertices.subList(4 * LEFT, 4 * LEFT + 3));
        Collections.reverse(vertices.subList(4 * RIGHT, 4 * RIGHT + 3));
        Collections.reverse(vertices.subList(4 * BOTTOM, 4 * BOTTOM + 3));
        Collections.reverse(vertices.subList(4 * TOP, 4 * TOP + 3));
        Collections.reverse(vertices.subList(4 * BACK, 4 * BACK + 3));
        Collections.reverse(vertices.subList(4 * FRONT, 4 * FRONT + 3));
    }

    public boolean hasFaces() {
        boolean arg = false;
        for (Boolean bool : enabledFaces) {
            arg = arg || bool;
            if (arg) {
                break;
            }
        }
        return arg;
    }

    public int getNumOfEnabledFaces() {
        int num = 0;
        for (int i = 0; i <= 5; i++) {
            if (enabledFaces[i]) {
                num++;
            }
        }
        return num;
    }

    public int getNumOfEnabledVertices() {
        int num = 0;
        for (Vertex vertex : vertices) {
            if (vertex.isEnabled()) {
                num++;
            }
        }
        return num;
    }

    public boolean[] getEnabledFaces() {
        return enabledFaces;
    }

    public boolean isVerticesReversed() {
        return verticesReversed;
    }

    // used in Blocks Series to get face represenation in bits form
    public int getFaceBits() {
        int bits = 0;
        for (int j = 0; j <= 5; j++) {
            if (enabledFaces[j]) {
                int mask = 1 << j;
                bits |= mask;
            }
        }
        return bits;
    }

    // used in static Level container to get compressed positioned sets
    public static int getNeighborBits(Vector3f pos, Set<Vector3f> vectorSet) {
        int bits = 0;
        for (int j = 0; j <= 5; j++) { // j - face number
            Vector3f adjPos = Block.getAdjacentPos(pos, j);
            if (vectorSet.contains(adjPos)) {
                int mask = 1 << j;
                bits |= mask;
            }
        }
        return bits;
    }

    // set faces based on faceBits representation
    public void setFaceBits(int faceBits, boolean selfBuffer) {
        for (int j = 0; j <= 5; j++) {
            int mask = 1 << j;
            int bit = (faceBits & mask) >> j;
            if (bit == 1) {
                enableFace(j);
            } else {
                disableFace(j);
            }
        }
    }

    // make int buffer base on bits form of faces
    public static IntBuffer createIntBuffer(int faceBits) {
        // creating indices
        List<Integer> indices = new ArrayList<>();
        int j = 0; // is face number (which increments after the face is added)
        while (faceBits > 0) {
            int bit = faceBits & 1; // compare the rightmost bit with one and assign it to bit
            if (bit == 1) {
                indices.add(4 * j);
                indices.add(4 * j + 1);
                indices.add(4 * j + 2);

                indices.add(4 * j + 2);
                indices.add(4 * j + 3);
                indices.add(4 * j);

                j++;
            }
            faceBits >>= 1; // move bits to the right so they are compared again            
        }
        // storing indices in the buffer
        IntBuffer intBuff = BufferUtils.createIntBuffer(indices.size());
        for (Integer index : indices) {
            intBuff.put(index);
        }
        intBuff.flip();
        return intBuff;
    }

    // returns array of adjacent free face numbers (those faces without adjacent neighbor nearby)
    // used by Random Level Generator
    public List<Integer> getAdjacentFreeFaceNumbers() {
        List<Integer> result = new ArrayList<>();
        for (int j = 0; j <= 5; j++) {
            Vector3f adjPos = getAdjacentPos(j);
            if (!LevelContainer.ALL_SOLID_MAP.containsKey(Vector3fUtils.hashCode(adjPos))
                    && !LevelContainer.ALL_FLUID_MAP.containsKey(Vector3fUtils.hashCode(adjPos))) {
                result.add(j);
            }
        }
        return result;
    }

    // assuming that blocks are the same scale
    public Vector3f getAdjacentPos(int faceNum) {
        Vector3f result = new Vector3f();
        result.x = pos.x;
        result.y = pos.y;
        result.z = pos.z;

        switch (faceNum) {
            case Block.LEFT:
                result.x -= 2.0f;
                break;
            case Block.RIGHT:
                result.x += 2.0f;
                break;
            case Block.BOTTOM:
                result.y -= 2.0f;
                break;
            case Block.TOP:
                result.y += 2.0f;
                break;
            case Block.BACK:
                result.z -= 2.0f;
                break;
            case Block.FRONT:
                result.z += 2.0f;
                break;
            default:
                break;
        }

        return result;
    }

    // assuming that blocks are the same scale
    public static Vector3f getAdjacentPos(Vector3f pos, int faceNum) {
        Vector3f result = new Vector3f();
        result.x = pos.x;
        result.y = pos.y;
        result.z = pos.z;

        switch (faceNum) {
            case Block.LEFT:
                result.x -= 2.0f;
                break;
            case Block.RIGHT:
                result.x += 2.0f;
                break;
            case Block.BOTTOM:
                result.y -= 2.0f;
                break;
            case Block.TOP:
                result.y += 2.0f;
                break;
            case Block.BACK:
                result.z -= 2.0f;
                break;
            case Block.FRONT:
                result.z += 2.0f;
                break;
            default:
                break;
        }

        return result;
    }

    public static int faceAdjacentBy(Vector3f blkPosA, Vector3f blkPosB) { // which face of blk "A" is adjacent to compared blk "B"
        int faceNum = -1;
        if (Math.abs((blkPosA.x - 1.0f) - (blkPosB.x + 1.0f)) == 0.0f
                && Math.abs(blkPosA.y - blkPosB.y) <= 0.0f
                && Math.abs(blkPosA.z - blkPosB.z) <= 0.0f) {
            faceNum = LEFT;
        } else if (Math.abs((blkPosA.x + 1.0f) - (blkPosB.x - 1.0f)) == 0.0f
                && Math.abs(blkPosA.y - blkPosB.y) == 0.0f
                && Math.abs(blkPosA.z - blkPosB.z) == 0.0f) {
            faceNum = RIGHT;
        } else if (Math.abs((blkPosA.y - 1.0f) - (blkPosB.y + 1.0f)) == 0.0f
                && Math.abs(blkPosA.z - blkPosB.z) == 0.0f
                && Math.abs(blkPosA.x - blkPosB.x) == 0.0f) {
            faceNum = BOTTOM;
        } else if (Math.abs((blkPosA.y + 1.0f) - (blkPosB.y - 1.0f)) == 0.0f
                && Math.abs(blkPosA.z - blkPosB.z) == 0.0f
                && Math.abs(blkPosA.x - blkPosB.x) == 0.0f) {
            faceNum = TOP;
        } else if (Math.abs((blkPosA.z - 1.0f) - (blkPosB.z + 1.0f)) == 0.0f
                && Math.abs(blkPosA.x - blkPosB.x) == 0.0f
                && Math.abs(blkPosA.y - blkPosB.y) == 0.0f) {
            faceNum = BACK;
        } else if (Math.abs((blkPosA.z + 1.0f) - (blkPosB.z - 1.0f)) == 0.0f
                && Math.abs(blkPosA.x - blkPosB.x) == 0.0f
                && Math.abs(blkPosA.y - blkPosB.y) == 0.0f) {
            faceNum = FRONT;
        }
        return faceNum;
    }

    public static boolean intersectsRay(Vector3f blockPos, Vector3f l, Vector3f l0) {
        boolean ints = false;
        Vector3f temp1 = new Vector3f();
        Vector3f min = blockPos.sub(1.0f, 1.0f, 1.0f, temp1);
        Vector3f temp2 = new Vector3f();
        Vector3f max = blockPos.add(1.0f, 1.0f, 1.0f, temp2);
        Vector2f result = new Vector2f();
        ints = Intersectionf.intersectRayAab(l0, l, min, max, result);
        return ints;
    }

    public byte[] toByteArray() {
        byte[] byteArray = new byte[29];
        int offset = 0;
        byte[] texNameArr = texName.getBytes();
        System.arraycopy(texNameArr, 0, byteArray, offset, 5);
        offset += 5;
        byte[] posArr = Vector3fUtils.vec3fToByteArray(pos);
        System.arraycopy(posArr, 0, byteArray, offset, posArr.length); // 12 B
        offset += posArr.length;
        byte[] colArr = Vector3fUtils.vec3fToByteArray(primaryColor);
        System.arraycopy(colArr, 0, byteArray, offset, colArr.length); // 12 B

        return byteArray;
    }

    public static Block fromByteArray(byte[] byteArray, boolean solid) {
        int offset = 0;
        char[] texNameArr = new char[5];
        for (int k = 0; k < texNameArr.length; k++) {
            texNameArr[k] = (char) byteArray[offset++];
        }
        String texName = String.valueOf(texNameArr);

        byte[] blockPosArr = new byte[12];
        System.arraycopy(byteArray, offset, blockPosArr, 0, blockPosArr.length);
        Vector3f blockPos = Vector3fUtils.vec3fFromByteArray(blockPosArr);
        offset += blockPosArr.length;

        byte[] blockPosCol = new byte[12];
        System.arraycopy(byteArray, offset, blockPosCol, 0, blockPosCol.length);
        Vector3f blockCol = Vector3fUtils.vec3fFromByteArray(blockPosCol);

        Block block = new Block(texName, blockPos, blockCol, solid);

        return block;
    }

}
