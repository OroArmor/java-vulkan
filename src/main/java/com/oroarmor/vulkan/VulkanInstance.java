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

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanInstance implements AutoCloseable {
    protected final VkInstance instance;
    protected final String name;
    protected final String engineName;
    protected final VulkanContext context;

    public VulkanInstance(VulkanContext context) {
        this("VulkanApplication", "No Engine", context);
    }

    public VulkanInstance(String name, String engineName, VulkanContext context) {
        this.name = name;
        this.engineName = engineName;
        this.context = context;
        instance = createVulkanInstance();
    }

    protected VkInstance createVulkanInstance() {
        if (context.getDebug().isDebugEnabled() && !context.getValidationLayers().checkValidationLayerSupport()) {
            throw new RuntimeException("Validation Layers requested, but not available.");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo applicationInfo = getVkApplicationInfo();
            VkInstanceCreateInfo info = getVkInstanceCreateInfo(applicationInfo);

            PointerBuffer instancePtr = stack.mallocPointer(1);

            VulkanUtil.checkVulkanResult(vkCreateInstance(info, null, instancePtr), "Failed to create Vulkan Instance");

            return new VkInstance(instancePtr.get(0), info);
        }
    }

    protected VkInstanceCreateInfo getVkInstanceCreateInfo(VkApplicationInfo applicationInfo) {
        MemoryStack stack = MemoryStack.stackGet();
        VkInstanceCreateInfo info = VkInstanceCreateInfo.callocStack(stack);

        info.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
        info.pApplicationInfo(applicationInfo);

        PointerBuffer requiredGLFWInstanceExtensions = glfwGetRequiredInstanceExtensions();
        info.ppEnabledExtensionNames(context.getValidationLayers().getRequiredExtensions(requiredGLFWInstanceExtensions));
        context.getValidationLayers().addValidationLayers(info);

        info.ppEnabledLayerNames(null);
        return info;
    }

    protected VkApplicationInfo getVkApplicationInfo() {
        MemoryStack stack = MemoryStack.stackGet();
        VkApplicationInfo applicationInfo = VkApplicationInfo.callocStack(stack);
        applicationInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
        applicationInfo.pApplicationName(stack.UTF8Safe(name));
        applicationInfo.applicationVersion(makeApplicationVersion());

        applicationInfo.pEngineName(stack.UTF8Safe(engineName));
        applicationInfo.engineVersion(makeEngineVersion());

        applicationInfo.apiVersion(getVulkanAPIVersion());

        return applicationInfo;

    }

    protected int makeApplicationVersion() {
        return VK_MAKE_VERSION(1, 0, 0);
    }

    protected int makeEngineVersion() {
        return VK_MAKE_VERSION(1, 0, 0);
    }

    protected int getVulkanAPIVersion() {
        return VK_API_VERSION_1_0;
    }

    public VkInstance getInstance() {
        return instance;
    }

    public void close() {
        vkDestroyInstance(instance, null);
    }
}
