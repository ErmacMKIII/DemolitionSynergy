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
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Blocks;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Coa
 */
public class LevelRenderer {

    private final Window myWindow;
    private final Block skybox;
    private final Blocks solidBlocks = new Blocks();
    private final Blocks fluidBlocks = new Blocks();

    private Critter observer;

    private final byte[] buffer = new byte[0x100000];
    private int pos = 0;

    public static final float SKYBOX_SCALE = Math.round(1.0f / Game.EPSILON);
    public static final float SKYBOX_WIDTH = Math.round(Math.pow(SKYBOX_SCALE, 1.0f / 3.0f));

    public static final int MAX_NUM_OF_SOLID_BLOCKS = 65535;
    public static final int MAX_NUM_OF_FLUID_BLOCKS = 65535;

    private int progress = 0;

    public LevelRenderer(Window myWindow) {
        this.myWindow = myWindow;
        // setting skybox
        skybox = new Block(true, Texture.NIGHT);
        skybox.setUVsForSkybox();
        skybox.setScale(SKYBOX_SCALE);
        // setting observer
        observer = new Critter("icosphere.obj", Texture.MARBLE, new Vector3f(10.5f, 0.0f, -3.0f), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), 0.25f);
        observer.setGivenControl(true);
        PerspectiveRenderer.updatePerspective(myWindow.getWidth(), myWindow.getHeight(), ShaderProgram.getVoxelShader());
    }

    public void startNewLevel() {
        progress = 0;
        observer = new Critter("icosphere.obj", Texture.MARBLE, new Vector3f(10.5f, 0.0f, -3.0f), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), 0.25f);
        observer.setGivenControl(true);
        solidBlocks.getBlockList().clear();
        fluidBlocks.getBlockList().clear();
        fluidBlocks.setVerticesReversed(false);
        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                Block entity = new Block(false, Texture.DOOM0);
                entity.setScale(1.0f);

                entity.getPos().x = 4.0f * i;
                entity.getPos().y = 4.0f * j;
                entity.getPos().z = 3.0f;

                entity.getPrimaryColor().x = 0.5f * i + 0.25f;
                entity.getPrimaryColor().y = 0.5f * j + 0.25f;
                entity.getPrimaryColor().z = 0.0f;
                entity.getPrimaryColor().w = 1.0f;

                entity.setLight(observer.getModel().getPos());

                solidBlocks.getBlockList().add(entity);

                progress += Math.round(100.0f / 9.0f);
            }
        }
        solidBlocks.setBuffered(false);
        fluidBlocks.setBuffered(false);
        progress = 100;
    }

    public boolean generateRandomLevel(Integer numberOfBlocks) {
        boolean success = false;
        observer = new Critter("icosphere.obj", Texture.MARBLE, new Vector3f(10.5f, 0.0f, -3.0f), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), 0.25f);
        observer.setGivenControl(false);
        solidBlocks.getBlockList().clear();
        fluidBlocks.getBlockList().clear();
        fluidBlocks.setVerticesReversed(false);
        if (numberOfBlocks > 0 && numberOfBlocks <= MAX_NUM_OF_SOLID_BLOCKS + MAX_NUM_OF_FLUID_BLOCKS) {
            RandomLevelGenerator.generate(this, numberOfBlocks);
            updateAll();
            updateFluids();
            success = true;
        }
        solidBlocks.setBuffered(false);
        fluidBlocks.setBuffered(false);

        observer.setGivenControl(true);
        return success;
    }

    private boolean storeLevelToBuffer() {
        boolean success = false;
        if (progress > 0) {
            return false;
        }
        progress = 0;
        pos = 0;
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

        int solidNum = solidBlocks.getBlockList().size();
        buffer[pos++] = (byte) (solidNum);
        buffer[pos++] = (byte) (solidNum >> 8);

        for (Block solidBlock : solidBlocks.getBlockList()) {
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

            progress += Math.round(100.0f / (solidBlocks.getBlockList().size() + fluidBlocks.getBlockList().size()));
        }

        buffer[pos++] = 'F';
        buffer[pos++] = 'L';
        buffer[pos++] = 'U';
        buffer[pos++] = 'I';
        buffer[pos++] = 'D';

        int fluidNum = fluidBlocks.getBlockList().size();
        buffer[pos++] = (byte) (fluidNum);
        buffer[pos++] = (byte) (fluidNum >> 8);

        for (Block fluidBlock : fluidBlocks.getBlockList()) {
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

            progress += Math.round(100.0f / (solidBlocks.getBlockList().size() + fluidBlocks.getBlockList().size()));
        }

        buffer[pos++] = 'E';
        buffer[pos++] = 'N';
        buffer[pos++] = 'D';

        progress = 100;

        if (progress == 100) {
            success = true;
        }

        return success;
    }

    private boolean loadLevelFromBuffer() {
        boolean success = false;
        if (progress > 0) {
            return false;
        }
        progress = 0;
        pos = 0;
        if (buffer[0] == 'D' && buffer[1] == 'S') {
            solidBlocks.getBlockList().clear();
            fluidBlocks.getBlockList().clear();
            fluidBlocks.setVerticesReversed(false);
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

            Camera obsCamera = new Camera(campos, camfront, camup, camright);
            Model obsModel = new Model(false, "icosphere.obj", Texture.MARBLE);
            obsModel.setScale(0.01f);
            observer = new Critter(obsCamera, obsModel);
            observer.setGivenControl(true);
            char[] solid = new char[5];
            for (int i = 0; i < solid.length; i++) {
                solid[i] = (char) buffer[pos++];
            }
            String strSolid = String.valueOf(solid);
            progress += 10;
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

                    Block block = new Block(false, Texture.TEX_MAP.get(texName), blockPos, primaryColor, false);
                    solidBlocks.getBlockList().add(block);
                }

                solidBlocks.setBuffered(false);

                progress += 40;
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

                        Block block = new Block(false, Texture.TEX_MAP.get(texName), blockPos, primaryColor, true);
                        fluidBlocks.getBlockList().add(block);
                    }

                    fluidBlocks.setBuffered(false);

                    progress += 40;
                    char[] end = new char[3];
                    for (int i = 0; i < end.length; i++) {
                        end[i] = (char) buffer[pos++];
                    }
                    String strEnd = String.valueOf(end);
                    if (strEnd.equals("END")) {
                        updateAll();
                        updateFluids();
                        progress += 10;
                        success = true;
                    }
                }
            }
        }
        return success;
    }

    public boolean saveLevelToFile(String filename) {
        boolean success = false;
        if (!filename.endsWith(".dat")) {
            filename += ".dat";
        }
        FileOutputStream fos = null;
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
        Arrays.fill(buffer, (byte) 0);
        success = storeLevelToBuffer(); // saves level to bufferVertices first
        try {
            fos = new FileOutputStream(file);
            fos.write(buffer, 0, pos); // save bufferVertices to file at pos mark
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
        return success;
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
        if (!file.exists()) {
            return false; // this prevents further fail
        }
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

    public void updateSolidNeighbors() {
        for (Block solidBlockI : solidBlocks.getBlockList()) {
            for (Block solidBlockJ : solidBlocks.getBlockList()) {
                if (solidBlockI != solidBlockJ) {
                    int faceNum = solidBlockI.faceAdjacentBy(solidBlockJ);
                    if (faceNum != Block.NONE) {
                        solidBlockI.getAdjacentBlockMap().put(faceNum, solidBlockJ);
                    }
                }
            }
        }
    }

    public void updateFluidNeighbors() {
        for (Block fluidBlockI : fluidBlocks.getBlockList()) {
            for (Block fluidBlockJ : fluidBlocks.getBlockList()) {
                if (fluidBlockI != fluidBlockJ) {
                    int faceNum = fluidBlockI.faceAdjacentBy(fluidBlockJ);
                    if (faceNum != Block.NONE) {
                        fluidBlockI.getAdjacentBlockMap().put(faceNum, fluidBlockJ);
                    }
                }
            }
        }
    }

    public void updateSolidToFluidNeighbors() {
        for (Block solidBlock : solidBlocks.getBlockList()) {
            for (Block fluidBlock : fluidBlocks.getBlockList()) {
                int faceNum = solidBlock.faceAdjacentBy(fluidBlock);
                if (faceNum != Block.NONE) {
                    solidBlock.getAdjacentBlockMap().put(faceNum, fluidBlock);
                }
            }
        }
    }

    public void updateFluidToSolidNeighbors() {
        for (Block fluidBlock : fluidBlocks.getBlockList()) {
            for (Block solidBlock : solidBlocks.getBlockList()) {
                int faceNum = fluidBlock.faceAdjacentBy(solidBlock);
                if (faceNum != Block.NONE) {
                    solidBlock.getAdjacentBlockMap().put(faceNum, fluidBlock);
                }
            }
        }
    }

    public void updateAll() {
        updateSolidNeighbors();
        updateFluidNeighbors();
        updateSolidToFluidNeighbors();
        updateFluidToSolidNeighbors();
    }

    public void updateFluids() {
        for (Block fluidBlock : fluidBlocks.getBlockList()) {
            fluidBlock.enableAllFaces(false);
            for (int i = 0; i <= 5; i++) {
                Block otherFluidBlock = fluidBlock.getAdjacentBlockMap().get(i);
                if (otherFluidBlock != null) {
                    fluidBlock.disableFace(i, false);
                }
            }
        }
    }

    public boolean isPlaceOccupiedBySolid(Vector3f pos) {
        boolean occ = false;
        for (int i = 0; i < solidBlocks.getBlockList().size() && !occ; i++) {
            occ = solidBlocks.getBlockList().get(i).getPos().x == pos.x
                    && solidBlocks.getBlockList().get(i).getPos().y == pos.y
                    && solidBlocks.getBlockList().get(i).getPos().z == pos.z;
        }
        return occ;
    }

    public boolean isPlaceOccupiedByFluid(Vector3f pos) {
        boolean occ = false;
        for (int i = 0; i < fluidBlocks.getBlockList().size() && !occ; i++) {
            occ = fluidBlocks.getBlockList().get(i).getPos().x == pos.x
                    && fluidBlocks.getBlockList().get(i).getPos().y == pos.y
                    && fluidBlocks.getBlockList().get(i).getPos().z == pos.z;
        }
        return occ;
    }

    public void animate() {
        for (Block fluidBlock : fluidBlocks.getBlockList()) {
            fluidBlock.animate(true);
        }
    }

    public boolean isCameraInFluid() {
        boolean yea = false;
        for (int i = 0; i < fluidBlocks.getBlockList().size() && !yea; i++) {
            yea = fluidBlocks.getBlockList().get(i).contains(observer.getCamera().getPos());
        }
        return yea;
    }

    public boolean hasCollisionWithCritter(Critter critter) {
        boolean coll;
        coll = (!skybox.containsExactly(critter.getPredictor()) || !skybox.intersectsExactly(critter.getPredModel()));
        for (int i = 0; i < solidBlocks.getBlockList().size() && !coll; i++) {
            Block entity = solidBlocks.getBlockList().get(i);
            coll = entity.contains(critter.getPredictor()) || entity.intersects(critter.getPredModel()); // relaxed (without exactly) is better restriction
        }
        return coll;
    }

    public void render(ShaderProgram shaderProgram) { // render for regular level rendering
        Camera obsCamera = observer.getCamera();
        obsCamera.render(shaderProgram);
        skybox.render(shaderProgram);

        Block editorNew = Editor.getSelectedNew();
        if (editorNew != null) {
            editorNew.setLight(obsCamera.getPos());
            if (!editorNew.isBuffered()) {
                editorNew.bufferAll();
            }
            editorNew.render(shaderProgram);
        }

        if (!solidBlocks.isBuffered()) {
            solidBlocks.bufferAll();
        }

        Predicate<Block> solidBlockPredicate = new Predicate<Block>() {
            @Override
            public boolean test(Block t) {
                return (obsCamera.doesSee(t)
                        && t.canBeSeenBy(obsCamera.getFront(), obsCamera.getPos()));
            }
        };
        solidBlocks.renderIf(shaderProgram, obsCamera.getPos(), solidBlockPredicate);

        if (!fluidBlocks.isBuffered()) {
            fluidBlocks.bufferAll();
        }

        Predicate<Block> fluidBlockPredicate = new Predicate<Block>() {
            @Override
            public boolean test(Block t) {
                return (obsCamera.doesSee(t)
                        && t.canBeSeenBy(obsCamera.getFront(), obsCamera.getPos())
                        && t.hasFaces());
            }
        };

        fluidBlocks.setCameraInFluid(isCameraInFluid());
        fluidBlocks.prepare();
        fluidBlocks.renderIf(shaderProgram, obsCamera.getPos(), fluidBlockPredicate);
    }

    public void render(Camera camera, ShaderProgram shaderProgram) { // render for both regular level rendering and framebuffer (water renderer)

        camera.render(shaderProgram);
        skybox.render(shaderProgram);

        Block editorNew = Editor.getSelectedNew();
        if (editorNew != null) {
            editorNew.setLight(camera.getPos());
            if (!editorNew.isBuffered()) {
                editorNew.bufferAll();
            }
            editorNew.render(shaderProgram);
        }

        if (!solidBlocks.isBuffered()) {
            solidBlocks.bufferAll();
        }

        Predicate<Block> solidBlockPredicate = new Predicate<Block>() {
            @Override
            public boolean test(Block t) {
                return (camera.doesSee(t)
                        && t.canBeSeenBy(camera.getFront(), camera.getPos()));
            }
        };
        solidBlocks.renderIf(shaderProgram, camera.getPos(), solidBlockPredicate);

        if (!fluidBlocks.isBuffered()) {
            fluidBlocks.bufferAll();
        }

        Predicate<Block> fluidBlockPredicate = new Predicate<Block>() {
            @Override
            public boolean test(Block t) {
                return (camera.doesSee(t)
                        && t.canBeSeenBy(camera.getFront(), camera.getPos())
                        && t.hasFaces());
            }
        };

        fluidBlocks.setCameraInFluid(isCameraInFluid());
        fluidBlocks.prepare();
        fluidBlocks.renderIf(shaderProgram, camera.getPos(), fluidBlockPredicate);
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public Block getSkybox() {
        return skybox;
    }

    public Blocks getSolidBlocks() {
        return solidBlocks;
    }

    public Blocks getFluidBlocks() {
        return fluidBlocks;
    }

    public Critter getObserver() {
        return observer;
    }

    public void setObserver(Critter observer) {
        this.observer = observer;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

}
