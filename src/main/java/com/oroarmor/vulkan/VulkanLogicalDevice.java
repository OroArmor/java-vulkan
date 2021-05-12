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
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanLogicalDevice implements AutoCloseable {
    protected final VkDevice device;
    protected final VkQueue graphicsQueue;
    protected final VkQueue presentQueue;
    protected final VulkanContext context;

    public VulkanLogicalDevice(VulkanContext context) {
        this.context = context;
        device = createVulkanDevice();
        graphicsQueue = createDeviceQueue(context.getPhysicalDevice().getQueueFamilyIndices().graphicsFamily);
        presentQueue = createDeviceQueue(context.getPhysicalDevice().getQueueFamilyIndices().presentFamily);
    }

    private VkQueue createDeviceQueue(Integer queueFamily) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);

            vkGetDeviceQueue(device, queueFamily, 0, pQueue);
            return new VkQueue(pQueue.get(0), device);
        }
    }

    private VkDevice createVulkanDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanPhysicalDevice.QueueFamilyIndices indices = context.getPhysicalDevice().getQueueFamilyIndices();

            int[] uniqueQueueFamilies = indices.unique();
            VkDeviceQueueCreateInfo.Buffer deviceQueueCreateInfo = VkDeviceQueueCreateInfo.callocStack(uniqueQueueFamilies.length, stack);

            for (int i = 0; i < uniqueQueueFamilies.length; i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = deviceQueueCreateInfo.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.callocStack(stack);

            deviceCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            deviceCreateInfo.pQueueCreateInfos(deviceQueueCreateInfo);

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.callocStack(stack);
            deviceCreateInfo.pEnabledFeatures(deviceFeatures);

            deviceCreateInfo.ppEnabledExtensionNames(VulkanUtil.asPointerBuffer(VulkanPhysicalDevice.DEVICE_EXTENSIONS));

            if (context.getDebug().isDebugEnabled()) {
                deviceCreateInfo.ppEnabledLayerNames(VulkanUtil.asPointerBuffer(VulkanValidationLayers.VALIDATION_LAYERS));
            }

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);

            VulkanUtil.checkVulkanResult(vkCreateDevice(context.getPhysicalDevice().getPhysicalDevice(), deviceCreateInfo, null, pDevice),"Failed to create logical device.");

            return new VkDevice(pDevice.get(0), context.getPhysicalDevice().getPhysicalDevice(), deviceCreateInfo);
        }
    }

    @Override
    public void close() {
        vkDestroyDevice(device, null);
    }

    public VkDevice getDevice() {
        return device;
    }
}
