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

package com.oroarmor.vulkan.context;

import com.oroarmor.vulkan.glfw.GLFWContext;
import com.oroarmor.vulkan.render.VulkanSemaphoreHandler;

public class VulkanContext implements AutoCloseable {
    protected final VulkanInstance instance;
    protected final VulkanDebug debug;
    protected final GLFWContext glfwContext;
    protected final VulkanSurface surface;
    protected final VulkanPhysicalDevice physicalDevice;
    protected final VulkanLogicalDevice logicalDevice;
    protected final VulkanCommandPool commandPool;
    protected final VulkanValidationLayers validationLayers;
    protected final VulkanSemaphoreHandler semaphoreHandler;

    public VulkanContext(GLFWContext glfwContext) {
        this.glfwContext = glfwContext;
        debug = new VulkanDebug(true, this);
        validationLayers = new VulkanValidationLayers(this);
        instance = new VulkanInstance(this);
        debug.setupDebugMessenger();
        surface = new VulkanSurface(this);
        physicalDevice = new VulkanPhysicalDevice(this);
        logicalDevice = new VulkanLogicalDevice(this);
        commandPool = new VulkanCommandPool(this);
        semaphoreHandler = new VulkanSemaphoreHandler(this);
    }

    public VulkanInstance getInstance() {
        return instance;
    }

    public GLFWContext getGLFWContext() {
        return glfwContext;
    }

    @Override
    public void close() {
        semaphoreHandler.close();
        commandPool.close();
        logicalDevice.close();
        debug.close();
        surface.close();
        instance.close();
    }

    public VulkanPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VulkanSurface getSurface() {
        return surface;
    }

    public VulkanDebug getDebug() {
        return debug;
    }

    public VulkanLogicalDevice getLogicalDevice() {
        return logicalDevice;
    }

    public VulkanValidationLayers getValidationLayers() {
        return validationLayers;
    }

    public VulkanCommandPool getCommandPool() {
        return commandPool;
    }
}
