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

import rs.alexanderstojanovich.evg.texture.Image;
import java.util.ArrayList;
import java.util.Arrays;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVidMode.Buffer;
import org.lwjgl.opengl.GL11;
import rs.alexanderstojanovich.evg.main.Game;

/**
 *
 * @author Coa
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

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
        init(width, height, title);
    }

    private void init(int width, int height, String title) {
        // initializing GLFW
        if (!GLFW.glfwInit()) {
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
            throw new RuntimeException("Failed to create the GLFW window!");
        }
        // setting the icon
        GLFWImage image = GLFWImage.malloc();
        Image icon = new Image(Game.INTRFACE_ENTRY, "ds_icon.png");
        image.set(icon.getWidth(), icon.getHeight(), icon.getContent());
        GLFWImage.Buffer buffer = GLFWImage.malloc(1);
        buffer.put(0, image);
        GLFW.glfwSetWindowIcon(windowID, buffer);
        //calculating the monitor
        monitorID = GLFW.glfwGetPrimaryMonitor();
        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitorID);
        monitorWidth = vidmode.width();
        monitorHeight = vidmode.height();
        centerTheWindow();
    }

    public void centerTheWindow() {
        // positioning the windowID to the center
        int xpos = (monitorWidth - width) / 2;
        int ypos = (monitorHeight - height) / 2;
        GLFW.glfwSetWindowPos(windowID, xpos, ypos);
    }

    public void fullscreen() {
        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitorID);
        GLFW.glfwSetWindowMonitor(windowID, monitorID, 0, 0, width, height, vidmode.refreshRate());
        fullscreen = true;
    }

    public void windowed() {
        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitorID);
        GLFW.glfwSetWindowMonitor(windowID, 0, 0, 0, width, height, vidmode.refreshRate());
        fullscreen = false;
    }

    public void enableVSync() {
        GLFW.glfwSwapInterval(1);
        vsync = true;
    }

    public void disableVSync() {
        GLFW.glfwSwapInterval(0);
        vsync = false;
    }

    public void render() {
        GLFW.glfwSwapBuffers(windowID);
    }

    public void loadContext() {
        // make the OpenGL context current
        GLFW.glfwMakeContextCurrent(windowID);
    }

    public static void unloadContext() {
        // unload context in favor to another thread
        GLFW.glfwMakeContextCurrent(0);
    }

    public boolean setResolution(int width, int height) {
        boolean success = false;
        if (width >= MIN_WIDTH && width <= monitorWidth && height >= MIN_HEIGHT && height <= monitorHeight) {
            if (Arrays.binarySearch(giveAllResolutions(), width + "x" + height) != -1) {
                this.width = width;
                this.height = height;
                GLFW.glfwSetWindowSize(windowID, width, height);
                GL11.glViewport(0, 0, width, height);
                success = true;
            }
        }
        return success;
    }

    public void destroy() {
        GLFW.glfwDestroyWindow(windowID);
        GLFW.glfwTerminate();
    }

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

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(windowID);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
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
