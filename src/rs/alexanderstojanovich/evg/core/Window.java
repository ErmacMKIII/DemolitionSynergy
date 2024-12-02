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
package rs.alexanderstojanovich.evg.core;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVidMode.Buffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.ImageUtils;

/**
 * Main core class related with the window on which OpenGL is being rendered. It
 * wraps around GLFW window.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Window {

    private int width;
    private int height;
    private final String title;

    private long windowID;
    private long monitorID;

    private int monitorWidth;
    private int monitorHeight;

    private boolean vsync = false;
    private boolean fullscreen = false;

    public static final int MIN_WIDTH = 640;
    public static final int MIN_HEIGHT = 480;

    private static Window instance;

    protected IList<Long> monitors;

    protected GLFWImage icon;
    protected GLFWImage.Buffer images;
    protected GLFWImage glfwArrowImage;

    /**
     * Gets one instance of the window (creates the window if not exists).
     *
     * @param width window width
     * @param height window height
     * @param title window title
     *
     * @return single window instance
     */
    public static Window getInstance(int width, int height, String title) {
        if (instance == null) {
            instance = new Window(width, height, title);
        }
        return instance;
    }

    /**
     * Create new window
     *
     * @param width window width
     * @param height window height
     * @param title window title
     */
    private Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
        init(width, height, title);
    }

    /**
     * Return GLFW Error
     *
     * @return glfw error
     */
    private String getLastGlfwError() {
        // Allocate a PointerBuffer to hold the error description pointer
        PointerBuffer description = MemoryUtil.memAllocPointer(1); // Single pointer is enough
        // Get the error code and the description pointer
        int errorCode = GLFW.glfwGetError(description);

        String errorMessage;
        if (errorCode != GLFW.GLFW_NO_ERROR) {
            // Check if the description is available
            if (description.get(0) != 0) {
                errorMessage = MemoryUtil.memUTF8(description.get(0)); // Retrieve UTF-8 encoded error message
            } else {
                errorMessage = "No description available for error code: " + errorCode;
            }
        } else {
            errorMessage = "No GLFW error reported.";
        }

        // Free the PointerBuffer memory
        MemoryUtil.memFree(description);

        return errorMessage;
    }

    /**
     * Internal initialization of the window.
     *
     * @param width window width
     * @param height window height
     * @param title window title
     */
    private void init(int width, int height, String title) {
        // Set up an error callback to capture GLFW error messages
        GLFWErrorCallback.createPrint(System.err).set();

        // initializing GLFW
        if (!GLFW.glfwInit()) {
            // Retrieve the last GLFW error message
            String errorMsg = getLastGlfwError();

            DSLogger.reportFatalError(String.format("Unable to initialize GLFW! - %s", errorMsg), null);
            throw new IllegalStateException("Unable to initialize GLFW!");
        }
        // setting windowID hints
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, 0);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, 1);
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, 1);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, 1);
        // creating the windowID        
        // passing NULL instead of monitor to avoid full screen!
        windowID = GLFW.glfwCreateWindow(width, height, title, 0, 0);
        if (windowID == 0) {
            // Retrieve the last GLFW error message
            String errorMsg = getLastGlfwError();

            DSLogger.reportFatalError(String.format("Failed to create the GLFW window! - %s", errorMsg), null);
            throw new RuntimeException("Failed to create the GLFW window!");
        }
        // setting the icon        
        BufferedImage ds_image1 = ImageUtils.loadImage(Game.INTRFACE_ENTRY, "ds_icon.png");
        ByteBuffer dataBuffer1 = ImageUtils.getImageDataBuffer(ds_image1);
        icon = GLFWImage.createSafe(MemoryUtil.memAddressSafe(dataBuffer1));
        icon.set(ds_image1.getWidth(), ds_image1.getHeight(), dataBuffer1);
        images = GLFWImage.calloc(1);
        images.put(0, icon);
        GLFW.glfwSetWindowIcon(windowID, images);
        if (dataBuffer1.capacity() != 0) {
            MemoryUtil.memFree(dataBuffer1);
        }

        // setting the cursor (arrow)
        BufferedImage ds_image2 = ImageUtils.loadImage(Game.INTRFACE_ENTRY, "arrow.png");
        ByteBuffer dataBuffer2 = ImageUtils.getImageDataBuffer(ds_image2);
        glfwArrowImage = GLFWImage.createSafe(MemoryUtil.memAddressSafe(dataBuffer2));
        glfwArrowImage.set(ds_image2.getWidth(), ds_image2.getHeight(), dataBuffer2);

        long arrowId = GLFW.glfwCreateCursor(glfwArrowImage, 0, 0);
        GLFW.glfwSetCursor(windowID, arrowId);

        // get monitor(s)
        PointerBuffer glfwMonitors = GLFW.glfwGetMonitors();
        int pos = 0;
        Long[] buffer = new Long[8];
        if (glfwMonitors != null) {
            while (glfwMonitors.hasRemaining()) {
                buffer[pos++] = glfwMonitors.get();
            }
        }
        List<Long> localMonitors = Arrays.asList(buffer);
        monitors = new GapList<>(localMonitors.stream().filter(Objects::nonNull).collect(Collectors.toList()));

        int monitorIndex = Configuration.getInstance().getMonitor();

        if (monitorIndex < 0 || monitorIndex > monitors.size()) {
            DSLogger.reportFatalError("Invalid MonitorID was specified!", null);
            throw new RuntimeException("Invalid MonitorID was specified!");
        }

        if (monitorIndex == 0) {
            monitorID = GLFW.glfwGetPrimaryMonitor();
        } else {
            monitorID = monitors.get(monitorIndex - 1);
        }

        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitorID);
        if (vidmode != null) {
            monitorWidth = vidmode.width();
            monitorHeight = vidmode.height();
        }

        if (dataBuffer2.capacity() != 0) {
            MemoryUtil.memFree(dataBuffer2);
        }

        centerTheWindow();
    }

    /**
     * Centers the window to the center of the monitor screen
     */
    public void centerTheWindow() {
        // positioning the windowID to the center
        int xpos = (monitorWidth - width) / 2;
        int ypos = (monitorHeight - height) / 2;
        GLFW.glfwSetWindowPos(windowID, xpos, ypos);
    }

//    @Deprecated
//    public void fullscreen() {
//        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitorID);
//        GLFW.glfwSetWindowMonitor(windowID, monitorID, 0, 0, width, height, vidmode.refreshRate());
//        fullscreen = true;
//    }
//
//    @Deprecated
//    public void windowed() {
//        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitorID);
//        GLFW.glfwSetWindowMonitor(windowID, 0, 0, 0, width, height, vidmode.refreshRate());
//        fullscreen = false;
//    }
//
//    @Deprecated
//    public void enableVSync() {
//        GLFW.glfwSwapInterval(1);
//        vsync = true;
//    }
//
//    @Deprecated
//    public void disableVSync() {
//        GLFW.glfwSwapInterval(0);
//        vsync = false;
//    }
    /**
     * Set window title. (Game Server may change title)
     *
     * @param title new window title
     */
    public void setTitle(String title) {
        GLFW.glfwSetWindowTitle(windowID, title);
    }

    /**
     * Synchronize frame rate with refresh rate
     *
     * @param vsync enabled
     */
    public void setVSync(boolean vsync) {
        if (vsync) {
            GLFW.glfwSwapInterval(1);
        } else {
            GLFW.glfwSwapInterval(0);
        }
        this.vsync = vsync;
    }

    /**
     * Set app to fullscreen or windowed
     *
     * @param fullscreen true - fullscreen, false - windowed
     */
    public void setFullscreen(boolean fullscreen) {
        if (fullscreen) {
            GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitorID);
            GLFW.glfwSetWindowMonitor(windowID, monitorID, 0, 0, width, height, vidmode.refreshRate());
        } else {
            GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitorID);
            GLFW.glfwSetWindowMonitor(windowID, 0, 0, 0, width, height, vidmode.refreshRate());
        }
        this.fullscreen = fullscreen;
    }

    /**
     * Render window (swap buffers)
     */
    public void render() {
        GLFW.glfwSwapBuffers(windowID);
    }

    /**
     * Load OpenGL context in the window (see GameRenderer)
     */
    public void loadContext() {
        // make the OpenGL context current
        GLFW.glfwMakeContextCurrent(windowID);
    }

    /**
     * Lose OpenGL Context (nobody has it)
     */
    public static void unloadContext() {
        // unload context in favor to another thread
        GLFW.glfwMakeContextCurrent(0);
    }

    /**
     * Set Resolution if available for this monitor. Notice that this only
     * changes window dimensions in pixel. OpenGL view port remains unchanged.
     *
     * @param width resolution width
     * @param height resolution height
     * @return is resolution changed
     */
    public boolean setResolution(int width, int height) {
        boolean success = false;
        if (width >= MIN_WIDTH && width <= monitorWidth && height >= MIN_HEIGHT && height <= monitorHeight) {
            if (Arrays.binarySearch(giveAllResolutions(), width + "x" + height) != -1) {
                this.width = width;
                this.height = height;
                GLFW.glfwSetWindowSize(windowID, width, height);
                success = true;
            }
        }
        return success;
    }

    /**
     * Destroy window with context alltogether
     */
    public void destroy() {
        images.free();
//        Callbacks.glfwFreeCallbacks(windowID); <= Causing errors
        GLFW.glfwDestroyWindow(windowID);
        GLFW.glfwTerminate();
    }

    /**
     * Gives all possible resolutions for the primary monitor.
     *
     * @return array of strings [width x height]
     */
    public Object[] giveAllResolutions() {
        ArrayList<Object> res = new ArrayList<>();
        Buffer buffer = GLFW.glfwGetVideoModes(monitorID);
        if (buffer != null) {
            for (int i = 0; i < buffer.capacity(); i++) {
                GLFWVidMode vidMode = buffer.get();
                String s = vidMode.width() + "x" + vidMode.height();
                if (!res.contains(s)) {
                    res.add(s);
                }
            }
            buffer.flip();
        }
        return res.toArray();
    }

    /**
     * Should window be close & game finalized.
     *
     * @return is game set to end.
     */
    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(windowID);
    }

    /**
     * Request game end or closing the window
     */
    public void close() {
        GLFW.glfwSetWindowShouldClose(windowID, true);
    }

    /**
     * Save Screenshot (of the OpenGL render)
     *
     * @return image with read pixels
     */
    public BufferedImage getScreenshot() {
        final int rgba = 4;

        GL11.glReadBuffer(GL11.GL_FRONT);
        ByteBuffer buffer = MemoryUtil.memCalloc(width * height * rgba); // RGBA -> 4
        if (buffer.capacity() != 0 && MemoryUtil.memAddressSafe(buffer) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = (x + (width * y)) * rgba;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                image.setRGB(x, height - (y + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }

        if (buffer.capacity() != 0) {
            MemoryUtil.memFree(buffer);
        }

        return image;
    }

    public static Window getInstance() {
        return instance;
    }

    public List<Long> getMonitors() {
        return monitors;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getAspectRatio() {
        return width / (float) height;
    }

    public static float getMinAspectRatio() {
        return 4.0f / 3.0f;
    }

    public String getTitle() {
        return title;
    }

    public long getWindowID() {
        return windowID;
    }

    public long getMonitorID() {
        return monitorID;
    }

    public int getMonitorWidth() {
        return monitorWidth;
    }

    public int getMonitorHeight() {
        return monitorHeight;
    }

    public boolean isVsync() {
        return vsync;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

}
