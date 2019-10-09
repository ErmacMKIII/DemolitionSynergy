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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Model;
import org.lwjgl.opengl.GL11;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

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
                Block entity = new Block("doom.png", shaderProgram);
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

    public boolean saveLevelToFile(String filename) {
        boolean success = false;
        if (filename.isEmpty()) {
            return false;
        }
        if (!filename.endsWith(".txt")) {
            filename += ".txt";
        }
        try {

            Collections.shuffle(solidBlocks);
            Collections.shuffle(fluidBlocks);

            Collections.sort(solidBlocks);
            Collections.sort(fluidBlocks);

            File f = new File(filename);
            FileWriter fw = new FileWriter(f);
            PrintWriter pw = new PrintWriter(fw);

            pw.println(observer.toString());

            for (int i = 0; i < solidBlocks.size(); i++) {
                Block solidEntity = solidBlocks.get(i);
                pw.println(solidEntity.toString());
            }
            for (int j = 0; j < fluidBlocks.size(); j++) {
                Block fluidEntity = fluidBlocks.get(j);
                pw.println(fluidEntity.toString());
            }
            pw.close();
            success = true;
        } catch (IOException ex) {
            Logger.getLogger(MasterRenderer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return success;
    }

    public boolean loadLevelFromFile(String filename) {
        boolean success = false;
        if (filename.isEmpty()) {
            return false;
        }
        if (!filename.endsWith(".txt")) {
            filename += ".txt";
        }
        try {
            File f = new File(filename);
            if (!f.exists()) {
                return false;
            } else {
                FileReader fr = new FileReader(f);
                BufferedReader br = new BufferedReader(fr);
                solidBlocks.clear();
                fluidBlocks.clear();
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("Critter{") && line.endsWith("}")) {
                        // Camera data
                        String cameraData = line.substring(line.indexOf("Camera{"), line.indexOf("}") + 1);
                        String[] data = cameraData.substring(cameraData.indexOf("{") + 1, cameraData.lastIndexOf("}")).trim().split(",");

                        String[] posData = data[0].substring(data[0].indexOf("(") + 1, data[0].lastIndexOf(")")).split(";");
                        Vector3f pos = new Vector3f(Float.parseFloat(posData[0]), Float.parseFloat(posData[1]), Float.parseFloat(posData[2]));

                        String[] frontData = data[1].substring(data[1].indexOf("(") + 1, data[1].lastIndexOf(")")).split(";");
                        Vector3f front = new Vector3f(Float.parseFloat(frontData[0]), Float.parseFloat(frontData[1]), Float.parseFloat(frontData[2]));

                        String[] upData = data[2].substring(data[2].indexOf("(") + 1, data[2].lastIndexOf(")")).split(";");
                        Vector3f up = new Vector3f(Float.parseFloat(upData[0]), Float.parseFloat(upData[1]), Float.parseFloat(upData[2]));

                        String[] rightData = data[3].substring(data[3].indexOf("(") + 1, data[3].lastIndexOf(")")).split(";");
                        Vector3f right = new Vector3f(Float.parseFloat(rightData[0]), Float.parseFloat(rightData[1]), Float.parseFloat(rightData[2]));

                        Camera camera = new Camera(pos, shaderProgram, front, up, right);
                        // Model data                        
                        String modelData = line.substring(line.indexOf("Model{"), line.indexOf("}", line.indexOf("Model{")) + 1);
                        data = modelData.substring(modelData.indexOf("{") + 1, modelData.lastIndexOf("}")).trim().split(",");

                        String modelFileName = data[0].substring(data[0].indexOf("=") + 1);

                        String textureFileName = data[1].substring(data[1].indexOf("=") + 1);

                        posData = data[2].substring(data[2].indexOf("(") + 1, data[2].lastIndexOf(")")).split(";");
                        pos = new Vector3f(Float.parseFloat(posData[0]), Float.parseFloat(posData[1]), Float.parseFloat(posData[2]));

                        float scale = Float.parseFloat(data[3].substring(data[3].indexOf("=") + 1));

                        String[] colorData = data[4].substring(data[4].indexOf("(") + 1, data[4].lastIndexOf(")")).split(";");
                        Vector4f color = new Vector4f(Float.parseFloat(colorData[0]), Float.parseFloat(colorData[1]), Float.parseFloat(colorData[2]), Float.parseFloat(colorData[3]));

                        boolean passable = Boolean.parseBoolean(data[5].substring(data[5].indexOf("=") + 1));

                        Model model = new Model(modelFileName, textureFileName, shaderProgram, pos, color, passable);
                        model.setScale(scale);
                        // making the critter                                                
                        observer = new Critter(camera, model);
                        observer.setGivenControl(true);
                    } else if (line.startsWith("Block{") && line.endsWith("}")) {
                        String[] data = line.substring(line.indexOf("{") + 1, line.lastIndexOf("}")).trim().split(",");

                        String textureFileName = data[0].substring(data[0].indexOf("=") + 1);

                        String[] posData = data[1].substring(data[1].indexOf("(") + 1, data[1].lastIndexOf(")")).split(";");
                        Vector3f pos = new Vector3f(Float.parseFloat(posData[0]), Float.parseFloat(posData[1]), Float.parseFloat(posData[2]));

                        float scale = Float.parseFloat(data[2].substring(data[2].indexOf("=") + 1));

                        String[] colorData = data[3].substring(data[3].indexOf("(") + 1, data[3].lastIndexOf(")")).split(";");
                        Vector4f color = new Vector4f(Float.parseFloat(colorData[0]), Float.parseFloat(colorData[1]), Float.parseFloat(colorData[2]), Float.parseFloat(colorData[3]));

                        boolean passable = Boolean.parseBoolean(data[4].substring(data[4].indexOf("=") + 1));

                        Block entity = new Block(textureFileName, shaderProgram, pos, color, passable);
                        entity.setScale(scale);
                        if (passable) {
                            fluidBlocks.add(entity);
                        } else {
                            solidBlocks.add(entity);
                        }
                    }
                }
                br.close();
                Collections.shuffle(solidBlocks);
                Collections.shuffle(fluidBlocks);
                Collections.sort(solidBlocks);
                Collections.sort(fluidBlocks);
                updateFluids();
                success = true;
            }
        } catch (IOException ex) {
            Logger.getLogger(MasterRenderer.class.getName()).log(Level.SEVERE, null, ex);
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

        for (int i = 0; i < solidBlocks.size(); i++) {
            if (camera.doesSee(solidBlocks.get(i))) {
                solidBlocks.get(i).setLight(camera.getPos());
                solidBlocks.get(i).render();
            }
        }
        for (int i = 0; i < fluidBlocks.size(); i++) {
            if (camera.doesSee(fluidBlocks.get(i))) {
                fluidBlocks.get(i).setLight(camera.getPos());
                fluidBlocks.get(i).render();
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
