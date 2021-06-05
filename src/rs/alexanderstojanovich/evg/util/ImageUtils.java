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
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import org.lwjgl.BufferUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class ImageUtils {

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

        imageBuffer = BufferUtils.createByteBuffer(data.length);
        imageBuffer.put(data, 0, data.length);
        imageBuffer.flip();

        return imageBuffer;
    }
}
