/*
 * MIT License
 *
 * Copyright (c) 2021 OroArmor (Eli Orona)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oroarmor.vulkan;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.glfw.GLFW.*;

public class GLFWContext implements AutoCloseable {
    private final long window;
    private final List<GLFWFramebufferSizeCallbackI> framebufferSizeCallbacks= new ArrayList<>();
    private final List<GLFWKeyCallbackI> keyCallbacks = new ArrayList<>();

    public GLFWContext(int width, int height, String name) {
        this(width, height, name, MemoryUtil.NULL, MemoryUtil.NULL);
    }

    public GLFWContext(int width, int height, String name, long monitor, long share) {
        if(!glfwInit()) {
            throw new GLFWException("GLFW failed to initialize.");
        }

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height, name, monitor, share);
        glfwSetFramebufferSizeCallback(window, (_window, _width, _height) -> framebufferSizeCallbacks.forEach(callback -> callback.invoke(_window, _width, _height)));
        glfwSetKeyCallback(window, (_window, _key, _scancode, _action, _mods) -> keyCallbacks.forEach(callback -> callback.invoke(_window,_key,_scancode,_action,_mods)));
    }

    public void addKeyCallback(@NotNull GLFWKeyCallbackI keyCallback) {
        keyCallbacks.add(keyCallback);
    }

    public void addFramebufferSizeCallback(@NotNull GLFWFramebufferSizeCallbackI framebufferSizeCallback) {
        framebufferSizeCallbacks.add(framebufferSizeCallback);
    }

    public long getWindow() {
        return window;
    }

    public void close() {
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }
}
