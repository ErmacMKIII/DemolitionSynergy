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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Coa
 */
public class Model implements Comparable<Model> {

    private String modelFileName;

    protected List<Vertex> vertices = new ArrayList<>();
    protected List<Integer> indices = new ArrayList<>(); // refers which vertex we want to use when       
    protected Texture primaryTexture;
    protected Texture secondaryTexture;
    protected Texture tertiaryTexture;

    protected float width; // X axis dimension
    protected float height; // Y axis dimension
    protected float depth; // Z axis dimension

    protected int vbo; // vertex buffer object
    protected int ibo; // index buffer object        

    protected Vector3f pos = new Vector3f();
    protected float scale = 1.0f; // changing scale also changes width, height and depth

    protected float rX = 0.0f;
    protected float rY = 0.0f;
    protected float rZ = 0.0f;

    protected Vector4f primaryColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    protected Vector4f secondaryColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    protected Vector4f tertiaryColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

    protected Vector3f light = new Vector3f();

    protected boolean solid = true; // is movement through this model possible
    // fluid models are solid whilst solid ones aren't               

    protected Matrix4f modelMatrix = new Matrix4f();

    protected float xMin, yMin, zMin;
    protected float xMax, yMax, zMax;

    protected boolean buffered = false; // is it buffered, it must be buffered before rendering otherwise FATAL ERROR

    public static final Model PISTOL = new Model(true, Game.PLAYER_ENTRY, "pistol.obj",
            Texture.PISTOL, new Vector3f(1.0f, -1.0f, 3.0f), LevelContainer.SKYBOX_COLOR, false);
    public static final Model SUB_MACHINE_GUN = new Model(true, Game.PLAYER_ENTRY, "sub_machine_gun.obj",
            Texture.SUB_MACHINE_GUN, new Vector3f(1.0f, -1.0f, 3.0f), LevelContainer.SKYBOX_COLOR, false);
    public static final Model SHOTGUN = new Model(true, Game.PLAYER_ENTRY, "shotgun.obj",
            Texture.SHOTGUN, new Vector3f(1.0f, -1.0f, 3.0f), LevelContainer.SKYBOX_COLOR, false);
    public static final Model ASSAULT_RIFLE = new Model(true, Game.PLAYER_ENTRY, "assault_rifle.obj",
            Texture.ASSAULT_RIFLE, new Vector3f(1.0f, -1.0f, 3.0f), LevelContainer.SKYBOX_COLOR, false);
    public static final Model MACHINE_GUN = new Model(true, Game.PLAYER_ENTRY, "machine_gun.obj",
            Texture.MACHINE_GUN, new Vector3f(1.0f, -1.0f, 3.0f), LevelContainer.SKYBOX_COLOR, false);
    public static final Model SNIPER_RIFLE = new Model(true, Game.PLAYER_ENTRY, "sniper_rifle.obj",
            Texture.SNIPER_RIFLE, new Vector3f(1.0f, -1.0f, 3.0f), LevelContainer.SKYBOX_COLOR, false);

    public static final Model[] WEAPONS = {PISTOL, SUB_MACHINE_GUN, SHOTGUN, ASSAULT_RIFLE, MACHINE_GUN, SNIPER_RIFLE};

    static {
        for (Model weapon : WEAPONS) {
            weapon.scale = 6.0f;
            weapon.rY = (float) (-Math.PI / 2.0f);
        }
    }

    protected Model() { // constructor for overriding; it does nothing; also for prediction model for collision        

    }

    public Model(boolean selfBuffer, String dirEntry, String modelFileName) {
        this.modelFileName = modelFileName;
        readFromObjFile(dirEntry, modelFileName);
        if (selfBuffer) {   // used for self buffering (old school), if Blocks class is being used to load blocks keep it off.
            bufferVertices();
            bufferIndices();
            buffered = true;
        }
        calcDims();
    }

    public Model(boolean selfBuffer, String dirEntry, String modelFileName, Texture primaryTexture) {
        this.modelFileName = modelFileName;
        this.primaryTexture = primaryTexture;
        readFromObjFile(dirEntry, modelFileName);
        if (selfBuffer) {
            bufferVertices();
            bufferIndices();
            buffered = true;
        }
        calcDims();
    }

    public Model(boolean selfBuffer, String dirEntry, String modelFileName, Texture primaryTexture, Vector3f pos, Vector4f primaryColor, boolean solid) {
        this.modelFileName = modelFileName;
        this.primaryTexture = primaryTexture;
        readFromObjFile(dirEntry, modelFileName);
        if (selfBuffer) {
            bufferVertices();
            bufferIndices();
            buffered = true;
        }
        this.pos = pos;
        calcDims();
        this.primaryColor = primaryColor;
        this.solid = solid;
    }

    private void readFromObjFile(String dirEntry, String fileName) {
        File extern = new File(dirEntry + fileName);
        File archive = new File(Game.DATA_ZIP);
        ZipFile zipFile = null;
        InputStream objInput = null;
        if (extern.exists()) {
            try {
                objInput = new FileInputStream(extern);
            } catch (FileNotFoundException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else if (archive.exists()) {
            try {
                zipFile = new ZipFile(archive);
                for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                    if (zipEntry.getName().equals(dirEntry + fileName)) {
                        objInput = zipFile.getInputStream(zipEntry);
                        break;
                    }
                }
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else {
            DSLogger.reportError("Cannot find zip archive " + Game.DATA_ZIP + " or relevant ingame files!", null);
        }
        //----------------------------------------------------------------------
        if (objInput == null) {
            DSLogger.reportError("Cannot find resource " + dirEntry + fileName + "!", null);
            return;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(objInput));
        List<Vector2f> uvs = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] things = line.split(" ");
                if (things[0].equals("v")) {
                    Vector3f pos = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    Vertex vertex = new Vertex(pos);
                    vertices.add(vertex);
                } else if (things[0].equals("vt")) {
                    Vector2f uv = new Vector2f(Float.parseFloat(things[1]), 1.0f - Float.parseFloat(things[2]));
                    uvs.add(uv);
                } else if (things[0].equals("vn")) {
                    Vector3f normal = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    normals.add(normal);
                } else if (things[0].equals("f")) {
                    String[] subThings = {things[1], things[2], things[3]};
                    for (String subThing : subThings) {
                        String[] data = subThing.split("/");
                        int index = Integer.parseInt(data[0]) - 1;
                        indices.add(index);
                        if (!data[1].isEmpty()) {
                            vertices.get(index).setUv(uvs.get(Integer.parseInt(data[1]) - 1));
                        }
                        if (!data[2].isEmpty()) {
                            vertices.get(index).setNormal(normals.get(Integer.parseInt(data[2]) - 1));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            try {
                objInput.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                    DSLogger.reportFatalError(ex.getMessage(), ex);
                }
            }
        }
    }

    private void bufferVertices() {
        // storing vertices and normals in the buffer
        FloatBuffer fb = BufferUtils.createFloatBuffer(vertices.size() * Vertex.SIZE);
        for (Vertex vertex : vertices) {
            if (vertex.isEnabled()) {
                fb.put(vertex.getPos().x);
                fb.put(vertex.getPos().y);
                fb.put(vertex.getPos().z);

                fb.put(vertex.getNormal().x);
                fb.put(vertex.getNormal().y);
                fb.put(vertex.getNormal().z);

                fb.put(vertex.getUv().x);
                fb.put(vertex.getUv().y);
            }
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
        for (Integer index : indices) {
            ib.put(index);
        }
        ib.flip();
        // storing indices buffer on the graphics card
        ibo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ib, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void bufferAll() { // explicit call to buffer unbuffered before the rendering
        bufferVertices();
        bufferIndices();
        buffered = true;
    }

    public void calcNormals() {
        for (int i = 0; i < indices.size(); i += 3) {
            int i0 = indices.get(i);
            int i1 = indices.get(i + 1);
            int i2 = indices.get(i + 2);

            Vector3f v1 = vertices.get(i1).getPos().sub(vertices.get(i0).getPos());
            Vector3f v2 = vertices.get(i2).getPos().sub(vertices.get(i0).getPos());

            Vector3f normal = v1.cross(v2).normalize();
            vertices.get(i0).setNormal(vertices.get(i0).getNormal().add(normal));
            vertices.get(i1).setNormal(vertices.get(i1).getNormal().add(normal));
            vertices.get(i2).setNormal(vertices.get(i2).getNormal().add(normal));
        }

        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).setNormal(vertices.get(i).getNormal().normalize());
        }
    }

    public void nullifyNormals() {
        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).setNormal(new Vector3f());
        }
    }

    public void render(ShaderProgram shaderProgram) {
        if (!buffered) {
            return; // this is very critical!!
        }

        Texture.enable();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // this is for pos
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // this is for normal
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // this is for uv

        if (shaderProgram != null) {
            shaderProgram.bind();
            transform(shaderProgram);
            useLight(shaderProgram);
            if (primaryTexture != null) { // this is primary texture
                primaryColor(shaderProgram);
                primaryTexture.bind(0, shaderProgram, "modelTexture0");
            }
            if (secondaryTexture != null) { // this is editor overlay texture
                secondaryColor(shaderProgram);
                secondaryTexture.bind(1, shaderProgram, "modelTexture1");
            }
            if (tertiaryTexture != null) { // this is reflective texture
                tertiaryColor(shaderProgram);
                tertiaryTexture.bind(2, shaderProgram, "modelTexture2");
            }
        }
        GL11.glDrawElements(GL11.GL_TRIANGLES, indices.size(), GL11.GL_UNSIGNED_INT, 0);
        Texture.unbind(0);
        Texture.unbind(1);
        Texture.unbind(2);
        ShaderProgram.unbind();

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        Texture.disable();
    }

    public void calcModelMatrix() {
        Matrix4f translationMatrix = new Matrix4f().setTranslation(pos);
        Matrix4f rotationMatrix = new Matrix4f().setRotationXYZ(rX, rY, rZ);
        Matrix4f scaleMatrix = new Matrix4f().scale(scale);

        Matrix4f temp = new Matrix4f();
        modelMatrix = translationMatrix.mul(rotationMatrix.mul(scaleMatrix, temp), temp);
    }

    protected void transform(ShaderProgram shaderProgram) {
        calcModelMatrix();
        shaderProgram.updateUniform(modelMatrix, "modelMatrix");
    }

    protected void primaryColor(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(primaryColor, "modelColor0");
    }

    protected void secondaryColor(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(secondaryColor, "modelColor1");
    }

    protected void tertiaryColor(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(tertiaryColor, "modelColor2");
    }

    protected void useLight(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(light, "modelLight");
    }

    private void calcDims() {
        Vector3f vect = vertices.get(0).getPos();
        xMin = vect.x;
        yMin = vect.y;
        zMin = vect.z;

        xMax = vect.x;
        yMax = vect.y;
        zMax = vect.z;

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

    public boolean contains(Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x >= pos.x - width / 2.0f && x.x <= pos.x + width / 2.0f;
        boolean boolY = x.y >= pos.y - height / 2.0f && x.y <= pos.y + height / 2.0f;
        boolean boolZ = x.z >= pos.z - depth / 2.0f && x.z <= pos.z + depth / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public boolean containsExactly(Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x > pos.x - width / 2.0f && x.x < pos.x + width / 2.0f;
        boolean boolY = x.y > pos.y - height / 2.0f && x.y < pos.y + height / 2.0f;
        boolean boolZ = x.z > pos.z - depth / 2.0f && x.z < pos.z + depth / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public boolean intersects(Model model) {
        boolean coll = false;
        boolean boolX = this.pos.x - this.width / 2.0f <= model.pos.x + model.width / 2.0f
                && this.pos.x + this.width / 2.0f >= model.pos.x - model.width / 2.0f;
        boolean boolY = this.pos.y - this.height / 2.0f <= model.pos.y + model.height / 2.0f
                && this.pos.y + this.height / 2.0f >= model.pos.y - model.height / 2.0f;
        boolean boolZ = this.pos.z - this.depth / 2.0f <= model.pos.z + model.depth / 2.0f
                && this.pos.z + this.depth / 2.0f >= model.pos.z - model.depth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public boolean intersectsExactly(Model model) {
        boolean coll = false;
        boolean boolX = this.pos.x - this.width / 2.0f < model.pos.x + model.width / 2.0f
                && this.pos.x + this.width / 2.0f > model.pos.x - model.width / 2.0f;
        boolean boolY = this.pos.y - this.height / 2.0f < model.pos.y + model.height / 2.0f
                && this.pos.y + this.height / 2.0f > model.pos.y - model.height / 2.0f;
        boolean boolZ = this.pos.z - this.depth / 2.0f < model.pos.z + model.depth / 2.0f
                && this.pos.z + this.depth / 2.0f > model.pos.z - model.depth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public boolean intersectsRay(Vector3f l, Vector3f l0) {
        boolean ints = false; // l is direction and l0 is the point
        for (Vertex vertex : vertices) {
            Vector3f temp = new Vector3f();
            Vector3f x0 = vertex.getPos().add(pos, temp); // point on the plane translated
            Vector3f n = vertex.getNormal(); // normal of the plane
            if (l.dot(n) != 0.0f) {
                float d = x0.sub(l0).dot(n) / l.dot(n);
                Vector3f x = l.mul(d, temp).add(l0, temp);
                if (contains(x)) {
                    ints = true;
                    break;
                }
            }
        }
        return ints;
    }

    @Override
    public String toString() {
        return "Model{" + "modelFileName=" + modelFileName + ", texture=" + primaryTexture.getImage().getFileName() + ", pos=" + pos + ", scale=" + scale + ", color=" + primaryColor + ", solid=" + solid + '}';
    }

    public void animate(boolean selfBuffer) {
        for (int i = 0; i < indices.size(); i += 3) {
            Vertex a = vertices.get(indices.get(i));
            Vertex b = vertices.get(indices.get(i + 1));
            Vertex c = vertices.get(indices.get(i + 2));
            Vector2f temp = c.getUv();
            c.setUv(b.getUv());
            b.setUv(a.getUv());
            a.setUv(temp);
        }

        if (selfBuffer) {
            bufferVertices();
        }
    }

    public float getAvgSize() {
        float xAbs = Math.abs(xMax - xMin);
        float yAbs = Math.abs(yMax - yMin);
        float zAbs = Math.abs(zMax - zMin);

        return (xAbs + yAbs + zAbs) / 3.0f;
    }

    public void autoSize() {
//        float avg = getAvgSize();
//        for (Vertex vertex : vertices) {
//            vertex.getPos().div(avg);
//        }
//        scale = 1.0f;
//        calcDims();
//        bufferVertices();
    }

    @Override
    public int compareTo(Model model) {
        return Float.compare(this.getPos().y, model.getPos().y);
    }

    public void calcDimsPub() {
        calcDims();
        calcModelMatrix();
    }

    public float giveSurfacePos() {
        return (this.pos.y - this.height / 2.0f);
    }

    public String getModelFileName() {
        return modelFileName;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<Integer> getIndices() {
        return indices;
    }

    public Texture getPrimaryTexture() {
        return primaryTexture;
    }

    public Texture getSecondaryTexture() {
        return secondaryTexture;
    }

    public Texture getTertiaryTexture() {
        return tertiaryTexture;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getDepth() {
        return depth;
    }

    public int getVbo() {
        return vbo;
    }

    public int getIbo() {
        return ibo;
    }

    public Vector3f getPos() {
        return pos;
    }

    public Matrix4f getModelMatrix() {
        return modelMatrix;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        this.width *= scale;
        this.height *= scale;
        this.depth *= scale;
    }

    public float getrX() {
        return rX;
    }

    public void setrX(float rX) {
        this.rX = rX;
    }

    public float getrY() {
        return rY;
    }

    public void setrY(float rY) {
        this.rY = rY;
    }

    public float getrZ() {
        return rZ;
    }

    public void setrZ(float rZ) {
        this.rZ = rZ;
    }

    public Vector4f getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(Vector4f primaryColor) {
        this.primaryColor = primaryColor;
    }

    public Vector4f getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(Vector4f secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public Vector4f getTertiaryColor() {
        return tertiaryColor;
    }

    public void setTertiaryColor(Vector4f tertiaryColor) {
        this.tertiaryColor = tertiaryColor;
    }

    public Vector3f getLight() {
        return light;
    }

    public void setLight(Vector3f light) {
        this.light = light;
    }

    public boolean isSolid() {
        return solid;
    }

    public void setSolid(boolean solid) {
        this.solid = solid;
    }

    public void setPos(Vector3f pos) {
        this.pos = pos;
    }

    public void setPrimaryTexture(Texture primaryTexture) {
        this.primaryTexture = primaryTexture;
    }

    public void setSecondaryTexture(Texture secondaryTexture) {
        this.secondaryTexture = secondaryTexture;
    }

    public void setTertiaryTexture(Texture tertiaryTexture) {
        this.tertiaryTexture = tertiaryTexture;
    }

    public boolean isBuffered() {
        return buffered;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Model) {
            Model that = (Model) obj;
            return (this.vertices.equals(that.vertices)
                    && this.indices.equals(that.indices)
                    && this.pos.equals(that.pos)
                    && this.primaryTexture.equals(that.primaryTexture)
                    && this.primaryColor.equals(that.primaryColor)
                    && this.width == that.width
                    && this.height == that.height
                    && this.depth == that.depth
                    && this.solid == that.solid);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.vertices);
        hash = 43 * hash + Objects.hashCode(this.indices);
        hash = 43 * hash + Objects.hashCode(this.primaryTexture);
        hash = 43 * hash + Float.floatToIntBits(this.width);
        hash = 43 * hash + Float.floatToIntBits(this.height);
        hash = 43 * hash + Float.floatToIntBits(this.depth);
        hash = 43 * hash + Objects.hashCode(this.pos);
        hash = 43 * hash + Objects.hashCode(this.primaryColor);
        hash = 43 * hash + (this.solid ? 1 : 0);
        return hash;
    }

}
