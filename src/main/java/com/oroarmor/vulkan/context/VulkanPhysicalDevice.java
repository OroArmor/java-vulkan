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

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanPhysicalDevice {
    public static final Set<String> DEVICE_EXTENSIONS = Set.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME);

    protected final VulkanContext context;
    protected final VkPhysicalDevice physicalDevice;
    protected SwapChainSupportDetails swapChainSupport;
    protected QueueFamilyIndices queueFamilyIndices;

    public VulkanPhysicalDevice(VulkanContext context) {
        this.context = context;
        physicalDevice = pickPhysicalDevice();
    }

    private VkPhysicalDevice pickPhysicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            vkEnumeratePhysicalDevices(context.getInstance().getInstance(), deviceCount, null);

            if (deviceCount.get(0) == 0) {
                throw new RuntimeException("No physical devices found with vulkan support.");
            }

            PointerBuffer devices = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(context.getInstance().getInstance(), deviceCount, devices);

            for (int i = 0; i < devices.capacity(); i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(devices.get(i), context.getInstance().getInstance());

                if (isDeviceSuitable(device)) {
                    return device;
                }
            }

            throw new RuntimeException("No suitable GPU found");
        }
    }

    private boolean isDeviceSuitable(VkPhysicalDevice physicalDevice) {
        boolean extensionsSupported = checkDeviceExtensionSupport(physicalDevice);
        boolean swapChainAdequate = false;

        if (extensionsSupported) {
            SwapChainSupportDetails swapChainSupport = getSwapChainSupport(physicalDevice);
            swapChainAdequate = swapChainSupport.formats.hasRemaining() && swapChainSupport.presentModes.get(swapChainSupport.presentModes.size() - 1) == 0;
        }

        return findQueueFamilies(physicalDevice).isComplete() && extensionsSupported && swapChainAdequate;
    }

    private QueueFamilyIndices findQueueFamilies(VkPhysicalDevice physicalDevice) {
        if (queueFamilyIndices == null) {
            queueFamilyIndices = new QueueFamilyIndices();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer queueFamilyCount = stack.ints(0);
                vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);

                VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.mallocStack(queueFamilyCount.get(0), stack);
                vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilies);

                IntBuffer presentSupport = stack.ints(VK_FALSE);

                for (int i = 0; i < queueFamilies.capacity() || !queueFamilyIndices.isComplete(); i++) {
                    if ((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                        queueFamilyIndices.graphicsFamily = i;
                    }

                    vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, context.getSurface().getSurface(), presentSupport);

                    if (presentSupport.get(0) == VK_TRUE) {
                        queueFamilyIndices.presentFamily = i;
                    }
                }

            }
        }
        return queueFamilyIndices;
    }

    private SwapChainSupportDetails getSwapChainSupport(VkPhysicalDevice physicalDevice) {
        if (this.swapChainSupport == null) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                swapChainSupport = new SwapChainSupportDetails();
                swapChainSupport.capabilities = VkSurfaceCapabilitiesKHR.create();
                vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, context.getSurface().getSurface(), swapChainSupport.capabilities);

                IntBuffer count = stack.ints(0);

                vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, context.getSurface().getSurface(), count, null);
                if (count.get(0) != 0) {
                    swapChainSupport.formats = VkSurfaceFormatKHR.create(count.get(0));
                    vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, context.getSurface().getSurface(), count, swapChainSupport.formats);
                }

                vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, context.getSurface().getSurface(), count, null);
                if (count.get(0) != 0) {
                    IntBuffer presentModes = stack.mallocInt(count.get(0));
                    vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, context.getSurface().getSurface(), count, presentModes);
                    swapChainSupport.presentModes = new ArrayList<>();
                    for(int i = 0; i< presentModes.capacity(); i++){
                        swapChainSupport.presentModes.add(presentModes.get(i));
                    }
                }
            }
        }
        return swapChainSupport;
    }

    public boolean checkDeviceExtensionSupport(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer extensionCount = stack.ints(0);

            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.mallocStack(extensionCount.get(0), stack);

            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extensionCount, availableExtensions);

            Set<String> extensions = availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(Collectors.toSet());

            Set<String> missingExtensions = DEVICE_EXTENSIONS.stream().filter(s -> !extensions.contains(s)).collect(Collectors.toSet());

            if (!missingExtensions.isEmpty()) {
                System.err.println("Missing Requested extensions! " + missingExtensions);
            }

            return extensions.containsAll(DEVICE_EXTENSIONS);
        }
    }

    public QueueFamilyIndices getQueueFamilyIndices() {
        return queueFamilyIndices;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public SwapChainSupportDetails getSwapChainSupport() {
        return swapChainSupport;
    }

    public static class SwapChainSupportDetails {
        public VkSurfaceCapabilitiesKHR capabilities;
        public VkSurfaceFormatKHR.Buffer formats;
        public List<Integer> presentModes;

        public VkSurfaceFormatKHR.Buffer getFormats() {
            return formats;
        }
    }

    public static class QueueFamilyIndices {
        public Integer graphicsFamily;
        public Integer presentFamily;

        public boolean isComplete() {
            return graphicsFamily != null && presentFamily != null;
        }

        public int[] unique() {
            return IntStream.of(graphicsFamily, presentFamily).distinct().toArray();
        }

        public int[] array() {
            return new int[]{graphicsFamily, presentFamily};
        }
    }
}
