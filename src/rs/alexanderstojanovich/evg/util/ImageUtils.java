/*
 * Copyright (C) 2021 Alexander Stojanovich <coas91@rocketmail.com>
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.lwjgl.system.MemoryUtil;
import rs.alexanderstojanovich.evg.main.Game;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class ImageUtils {

    /**
     * Loads image from zip archive or external content.
     *
     * @param dirEntry directory where image is located
     * @param fileName image filename
     * @return read image
     */
    public static BufferedImage loadImage(String dirEntry, String fileName) {
        File extern = new File(dirEntry + fileName);
        File archive = new File(Game.DATA_ZIP);
        ZipFile zipFile = null;
        InputStream imgInput = null;
        if (extern.exists()) {
            try {
                imgInput = new FileInputStream(extern);
            } catch (FileNotFoundException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else if (archive.exists()) {
            try {
                zipFile = new ZipFile(archive);
                for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                    if (zipEntry.getName().equals(dirEntry + fileName)) {
                        imgInput = zipFile.getInputStream(zipEntry);
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
        if (imgInput == null) {
            DSLogger.reportError("Cannot find resource " + dirEntry + fileName + "!", null);
            return null;
        } else {
            try {
                return ImageIO.read(imgInput);
            } catch (IOException ex) {
                DSLogger.reportError("Error during loading image " + dirEntry + fileName + "!", null);
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }

        return null;
    }

    /**
     * Get Image Buffer from the source image.
     *
     * @param srcImage image to get byte buffer from
     * @return byte buffer image data
     */
    public static ByteBuffer getImageDataBuffer(BufferedImage srcImage) {
        ByteBuffer imageBuffer;
        WritableRaster raster;
        BufferedImage dstImage;

        ColorModel glAlphaColorModel = new ComponentColorModel(ColorSpace
                .getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8, 8},
                true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

        raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                srcImage.getWidth(), srcImage.getHeight(), 4, null);
        dstImage = new BufferedImage(glAlphaColorModel, raster, false,
                new Hashtable());

        // copy the source image into the produced image
        Graphics2D g2d = (Graphics2D) dstImage.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.0f));
        g2d.drawImage(srcImage, 0, 0, null);

        byte[] data = ((DataBufferByte) dstImage.getRaster().getDataBuffer())
                .getData();

        imageBuffer = MemoryUtil.memCalloc(data.length);
        if (MemoryUtil.memAddressSafe(imageBuffer) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);// Memory allocation failed
            throw new RuntimeException("Could not allocate memory address!");
        }
        imageBuffer.put(data, 0, data.length);
        imageBuffer.flip();

        return imageBuffer;
    }

    /**
     * Gets content of this image as Byte Buffer (for textures)
     *
     * @param srcImg source image
     * @param texSize texture size (power of two)
     * @return content as byte buffer for creating texture
     */
    public static ByteBuffer getImageDataBuffer(BufferedImage srcImg, int texSize) {
        ByteBuffer imageBuffer;
        WritableRaster raster;
        BufferedImage texImage;

        ColorModel glAlphaColorModel = new ComponentColorModel(ColorSpace
                .getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8, 8},
                true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

        raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                texSize, texSize, 4, null);
        texImage = new BufferedImage(glAlphaColorModel, raster, false,
                new Hashtable());

        int width = srcImg.getWidth();
        int height = srcImg.getHeight();
        double sx = 1.0 / (1.0 + (width - texSize) / (double) texSize);
        double sy = 1.0 / (1.0 + (height - texSize) / (double) texSize);

        AffineTransform xform = new AffineTransform();
        xform.scale(sx, sy);
        AffineTransformOp atOp = new AffineTransformOp(xform, null);
        final BufferedImage dstImg = atOp.filter(srcImg, null);

        // copy the source image into the produced image
        Graphics2D g2d = (Graphics2D) texImage.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);

        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.0f));
        g2d.drawImage(dstImg, 0, 0, null);

        // build a byte buffer from the temporary image
        // that be used by OpenGL to produce a texture.
        byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer())
                .getData();

        imageBuffer = MemoryUtil.memCalloc(data.length);
        if (MemoryUtil.memAddressSafe(imageBuffer) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);// Memory allocation failed
            throw new RuntimeException("Could not allocate memory address!");
        }
        imageBuffer.put(data, 0, data.length);
        imageBuffer.flip();

        return imageBuffer;
    }
}
