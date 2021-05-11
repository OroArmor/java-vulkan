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

import com.oroarmor.vulkan.initial.VulkanLogicalDevices;

public class VulkanContext implements AutoCloseable {
    protected final VulkanInstance instance;
    protected final VulkanDebug debug;
    protected final GLFWContext glfwContext;
    protected final VulkanSurface surface;
    protected final VulkanPhysicalDevice physicalDevice;
    protected final VulkanLogicalDevice vulkanLogicalDevice;

    public VulkanContext(GLFWContext glfwContext) {
        this.glfwContext = glfwContext;
        debug = new VulkanDebug(false, this);
        instance = new VulkanInstance(new VulkanValidationLayers(debug), this);
        debug.setupDebugMessenger();
        surface = new VulkanSurface(this);
        physicalDevice = new VulkanPhysicalDevice(this);
        vulkanLogicalDevice = new VulkanLogicalDevice(this);
    }

    public VulkanInstance getVulkanInstance() {
        return instance;
    }

    public GLFWContext getGLFWContext() {
        return glfwContext;
    }

    @Override
    public void close() {
        vulkanLogicalDevice.close();
        debug.close();
        surface.close();
        instance.close();
    }

    public VulkanPhysicalDevice getVulkanPhysicalDevice() {
        return physicalDevice;
    }

    public VulkanSurface getVulkanSurface() {
        return surface;
    }

    public VulkanDebug getVulkanDebug() {
        return debug;
    }
}
