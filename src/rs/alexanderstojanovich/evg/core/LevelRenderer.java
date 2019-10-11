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
package rs.alexanderstojanovich.evg.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Coa
 */
public class LevelRenderer {

    private ShaderProgram shaderProgram;
    private Block skybox;
    private List<Block> solidBlocks = new LinkedList<>();
    private List<Block> fluidBlocks = new LinkedList<>();

    private Critter observer;

    private byte[] buffer = new byte[0x100000];

    public LevelRenderer(ShaderProgram shaderProgram) {
        this.shaderProgram = shaderProgram;
        // setting skybox
        skybox = new Block("night.png", shaderProgram);
        skybox.setUVsForSkybox();
        skybox.setScale(1.0f / (4.0f * Game.EPSILON));
        // setting observer
        observer = new Critter("icosphere.obj", "marble.png", shaderProgram, new Vector3f(10.5f, 0, -3), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), 0.25f);
        observer.setGivenControl(true);
    }

    public void startNewLevel() {
        observer = new Critter("icosphere.obj", "marble.png", shaderProgram, new Vector3f(10.5f, 0, -3), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), 0.25f);
        observer.setGivenControl(true);
        solidBlocks.clear();
        fluidBlocks.clear();
        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                Block entity = new Block("doom0.png", shaderProgram);
                entity.setScale(1.0f);

                entity.getPos().x = 0.25f * i + i * 4;
                entity.getPos().y = 0.25f * j + j * 4;
                entity.getPos().z = 3.0f;

                entity.getPrimaryColor().x = 0.5f * i + 0.25f;
                entity.getPrimaryColor().y = 0.5f * j + 0.25f;
                entity.getPrimaryColor().z = 0.0f;
                entity.getPrimaryColor().w = 1.0f;

                entity.setLight(observer.getModel().getPos());

                solidBlocks.add(entity);
            }
        }
    }

    private void storeLevelToBuffer() {
        int pos = 0;
        buffer[0] = 'D';
        buffer[1] = 'S';
        pos += 2;
        Camera camera = this.observer.getCamera();
        byte[] campos = Vector3fUtils.vec3fToByteArray(camera.getPos());
        System.arraycopy(campos, 0, buffer, pos, campos.length);
        pos += campos.length;

        byte[] camfront = Vector3fUtils.vec3fToByteArray(camera.getFront());
        System.arraycopy(camfront, 0, buffer, pos, camfront.length);
        pos += camfront.length;

        byte[] camup = Vector3fUtils.vec3fToByteArray(camera.getUp());
        System.arraycopy(camup, 0, buffer, pos, camup.length);
        pos += camup.length;

        byte[] camright = Vector3fUtils.vec3fToByteArray(camera.getRight());
        System.arraycopy(camup, 0, buffer, pos, camright.length);
        pos += camright.length;

        buffer[pos++] = 'S';
        buffer[pos++] = 'O';
        buffer[pos++] = 'L';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        int solidNum = solidBlocks.size();
        buffer[pos++] = (byte) (solidNum);
        buffer[pos++] = (byte) (solidNum >> 8);

        for (Block solidBlock : solidBlocks) {
            byte[] texName = solidBlock.getPrimaryTexture().getImage().getFileName().getBytes();
            System.arraycopy(texName, 0, buffer, pos, 5);
            pos += 5;
            byte[] solidPos = Vector3fUtils.vec3fToByteArray(solidBlock.getPos());
            System.arraycopy(solidPos, 0, buffer, pos, solidPos.length);
            pos += solidPos.length;
            Vector4f primCol = solidBlock.getPrimaryColor();
            Vector3f col = new Vector3f(primCol.x, primCol.y, primCol.z);
            byte[] solidCol = Vector3fUtils.vec3fToByteArray(col);
            System.arraycopy(solidCol, 0, buffer, pos, solidCol.length);
            pos += solidCol.length;
        }

        buffer[pos++] = 'F';
        buffer[pos++] = 'L';
        buffer[pos++] = 'U';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        int fluidNum = fluidBlocks.size();
        buffer[pos++] = (byte) (fluidNum);
        buffer[pos++] = (byte) (fluidNum >> 8);

        for (Block fluidBlock : fluidBlocks) {
            byte[] texName = fluidBlock.getPrimaryTexture().getImage().getFileName().getBytes();
            System.arraycopy(texName, 0, buffer, pos, 5);
            pos += 5;
            byte[] solidPos = Vector3fUtils.vec3fToByteArray(fluidBlock.getPos());
            System.arraycopy(solidPos, 0, buffer, pos, solidPos.length);
            pos += solidPos.length;
            Vector4f primCol = fluidBlock.getPrimaryColor();
            Vector3f col = new Vector3f(primCol.x, primCol.y, primCol.z);
            byte[] solidCol = Vector3fUtils.vec3fToByteArray(col);
            System.arraycopy(solidCol, 0, buffer, pos, solidCol.length);
            pos += solidCol.length;
        }

        buffer[pos++] = 'E';
        buffer[pos++] = 'N';
        buffer[pos++] = 'D';
    }

    private boolean loadLevelFromBuffer() {
        boolean success = false;
        int pos = 0;
        if (buffer[0] == 'D' && buffer[1] == 'S') {
            solidBlocks.clear();
            fluidBlocks.clear();
            pos += 2;
            byte[] posArr = new byte[12];
            System.arraycopy(buffer, pos, posArr, 0, posArr.length);
            Vector3f campos = Vector3fUtils.vec3fFromByteArray(posArr);
            pos += posArr.length;

            byte[] frontArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, frontArr.length);
            Vector3f camfront = Vector3fUtils.vec3fFromByteArray(frontArr);
            pos += frontArr.length;

            byte[] upArr = new byte[12];
            System.arraycopy(buffer, pos, frontArr, 0, upArr.length);
            Vector3f camup = Vector3fUtils.vec3fFromByteArray(upArr);
            pos += upArr.length;

            byte[] rightArr = new byte[12];
            System.arraycopy(buffer, pos, rightArr, 0, rightArr.length);
            Vector3f camright = Vector3fUtils.vec3fFromByteArray(rightArr);
            pos += rightArr.length;

            Camera obsCamera = new Camera(campos, shaderProgram, camfront, camup, camright);
            Model obsModel = new Model("icosphere.obj", shaderProgram);
            obsModel.setScale(0.25f);
            observer = new Critter(obsCamera, obsModel);
            observer.setGivenControl(true);
            char[] solid = new char[5];
            for (int i = 0; i < solid.length; i++) {
                solid[i] = (char) buffer[pos++];
            }
            String strSolid = String.valueOf(solid);
            if (strSolid.equals("SOLID")) {
                int solidNum = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                pos += 2;
                for (int i = 0; i < solidNum; i++) {
                    char[] texNameArr = new char[5];
                    for (int k = 0; k < texNameArr.length; k++) {
                        texNameArr[k] = (char) buffer[pos++];
                    }
                    String texName = String.valueOf(texNameArr);

                    byte[] blockPosArr = new byte[12];
                    System.arraycopy(buffer, pos, blockPosArr, 0, blockPosArr.length);
                    Vector3f blockPos = Vector3fUtils.vec3fFromByteArray(blockPosArr);
                    pos += blockPosArr.length;

                    byte[] blockPosCol = new byte[12];
                    System.arraycopy(buffer, pos, blockPosCol, 0, blockPosCol.length);
                    Vector3f blockCol = Vector3fUtils.vec3fFromByteArray(blockPosCol);
                    pos += blockPosCol.length;

                    Vector4f primaryColor = new Vector4f(blockCol, 1.0f);

                    Block block = new Block(texName + ".png", shaderProgram, blockPos, primaryColor, false);
                    solidBlocks.add(block);
                }

                char[] fluid = new char[5];
                for (int i = 0; i < fluid.length; i++) {
                    fluid[i] = (char) buffer[pos++];
                }
                String strFluid = String.valueOf(fluid);
                if (strFluid.equals("FLUID")) {
                    int fluidNum = ((buffer[pos + 1] & 0xFF) << 8) | (buffer[pos] & 0xFF);
                    pos += 2;
                    for (int i = 0; i < fluidNum; i++) {
                        char[] texNameArr = new char[5];
                        for (int k = 0; k < texNameArr.length; k++) {
                            texNameArr[k] = (char) buffer[pos++];
                        }
                        String texName = String.valueOf(texNameArr);

                        byte[] blockPosArr = new byte[12];
                        System.arraycopy(buffer, pos, blockPosArr, 0, blockPosArr.length);
                        Vector3f blockPos = Vector3fUtils.vec3fFromByteArray(blockPosArr);
                        pos += blockPosArr.length;

                        byte[] blockPosCol = new byte[12];
                        System.arraycopy(buffer, pos, blockPosCol, 0, blockPosCol.length);
                        Vector3f blockCol = Vector3fUtils.vec3fFromByteArray(blockPosCol);
                        pos += blockPosCol.length;

                        Vector4f primaryColor = new Vector4f(blockCol, 0.5f);

                        Block block = new Block(texName + ".png", shaderProgram, blockPos, primaryColor, true);
                        fluidBlocks.add(block);
                    }

                    char[] end = new char[3];
                    for (int i = 0; i < end.length; i++) {
                        end[i] = (char) buffer[pos++];
                    }
                    String strEnd = String.valueOf(end);
                    if (strEnd.equals("END")) {
                        updateFluids();
                        success = true;
                    }
                }
            }
        }
        return success;
    }

    public void saveLevelToFile(String filename) {
        if (!filename.endsWith(".dat")) {
            filename += ".dat";
        }
        FileOutputStream fos = null;
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
        Arrays.fill(buffer, (byte) 0);
        storeLevelToBuffer(); // saves level to buffer first
        try {
            fos = new FileOutputStream(file);
            fos.write(buffer);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LevelRenderer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LevelRenderer.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(LevelRenderer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public boolean loadLevelFromFile(String filename) {
        boolean success = false;
        if (filename.isEmpty()) {
            return false;
        }
        if (!filename.endsWith(".dat")) {
            filename += ".dat";
        }
        File file = new File(filename);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            fis.read(buffer);
            success = loadLevelFromBuffer();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LevelRenderer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LevelRenderer.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(LevelRenderer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return success;
    }

    public void updateFluids() {
        for (Block fluidBlock : fluidBlocks) {
            fluidBlock.reconstructAllFaces();
        }

        for (Block fluidBlockI : fluidBlocks) {
            for (Block fluidBlockJ : fluidBlocks) {
                if (fluidBlockI != fluidBlockJ) {
                    int faceNum = fluidBlockI.faceAdjacentBy(fluidBlockJ);
                    fluidBlockI.removeFace(faceNum);
                }
            }
        }
    }

    public boolean isPlaceOccupiedBySolid(Vector3f pos) {
        boolean occ = false;
        for (int i = 0; i < solidBlocks.size() && !occ; i++) {
            occ = solidBlocks.get(i).getPos().x == pos.x
                    && solidBlocks.get(i).getPos().y == pos.y
                    && solidBlocks.get(i).getPos().z == pos.z;
        }
        return occ;
    }

    public boolean isPlaceOccupiedByFluid(Vector3f pos) {
        boolean occ = false;
        for (int i = 0; i < fluidBlocks.size() && !occ; i++) {
            occ = fluidBlocks.get(i).getPos().x == pos.x
                    && fluidBlocks.get(i).getPos().y == pos.y
                    && fluidBlocks.get(i).getPos().z == pos.z;
        }
        return occ;
    }

    public void animate() {
        for (int i = 0; i < fluidBlocks.size(); i++) {
            fluidBlocks.get(i).animate();
        }
    }

    public boolean isCameraInFluid() {
        boolean yea = false;
        for (int i = 0; i < fluidBlocks.size() && !yea; i++) {
            yea = fluidBlocks.get(i).contains(observer.getCamera().getPos());
        }
        return yea;
    }

    public boolean hasCollisionWithCritter(Critter critter, float amount, int direction) {
        Vector3f predCam = new Vector3f(critter.getCamera().getPos().x, critter.getCamera().getPos().y, critter.getCamera().getPos().z);
        Model predModel = new Model();
        predModel.setPos(critter.getModel().getPos());
        predModel.setScale(critter.getModel().getScale());
        switch (direction) {
            case Game.FORWARD: // FORWARD
                predCam = predCam.add(critter.getCamera().getFront().mul(amount));
                predModel.setPos(predModel.getPos().add(critter.getCamera().getFront().mul(amount)));
                break;
            case Game.BACKWARD: // BACKWARD
                predCam = predCam.sub(critter.getCamera().getFront().mul(amount));
                predModel.setPos(predModel.getPos().sub(critter.getCamera().getFront().mul(amount)));
                break;
            case Game.LEFT: // LEFT
                predCam = predCam.sub(critter.getCamera().getRight().mul(amount));
                predModel.setPos(predModel.getPos().sub(critter.getCamera().getRight().mul(amount)));
                break;
            case Game.RIGHT: // RIGHT
                predCam = predCam.add(critter.getCamera().getRight().mul(amount));
                predModel.setPos(predModel.getPos().add(critter.getCamera().getRight().mul(amount)));
                break;
        }
        boolean coll;
        coll = !(skybox.contains(predCam) || !skybox.intersects(predModel));
        for (int i = 0; i < solidBlocks.size() && !coll; i++) {
            Block entity = solidBlocks.get(i);
            coll = entity.contains(predCam) || entity.intersects(predModel);
        }
        return coll;
    }

    private void prepare() {
        if (isCameraInFluid()) {
            GL11.glDisable(GL11.GL_CULL_FACE);
        } else {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    public void render(Camera camera) {
        prepare();

        camera.render();

        skybox.render();

        if (Editor.selectedNew != null) {
            Editor.selectedNew.setLight(camera.getPos());
            Editor.selectedNew.render();
        }

        for (Block solidBlock : solidBlocks) {
            if (camera.doesSee(solidBlock)) {
                solidBlock.setLight(camera.getPos());
                solidBlock.render();
            }
        }
        for (Block fluidBlock : fluidBlocks) {
            if (camera.doesSee(fluidBlock)) {
                fluidBlock.setLight(camera.getPos());
                fluidBlock.render();
            }
        }
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }

    public void setShaderProgram(ShaderProgram shaderProgram) {
        this.shaderProgram = shaderProgram;
    }

    public Block getSkybox() {
        return skybox;
    }

    public void setSkybox(Block skybox) {
        this.skybox = skybox;
    }

    public List<Block> getSolidBlocks() {
        return solidBlocks;
    }

    public void setSolidBlocks(List<Block> solidBlocks) {
        this.solidBlocks = solidBlocks;
    }

    public List<Block> getFluidBlocks() {
        return fluidBlocks;
    }

    public void setFluidBlocks(List<Block> fluidBlocks) {
        this.fluidBlocks = fluidBlocks;
    }

    public Critter getObserver() {
        return observer;
    }

    public void setObserver(Critter observer) {
        this.observer = observer;
    }

}
