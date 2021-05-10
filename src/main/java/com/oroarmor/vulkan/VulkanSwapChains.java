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

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanSwapChains {
    public static long swapChain;
    public static List<Long> swapChainImages;
    public static int swapChainImageFormat;
    public static VkExtent2D swapChainExtent;

    public static class SwapChainSupportDetails {
        public VkSurfaceCapabilitiesKHR capabilities;
        public VkSurfaceFormatKHR.Buffer formats;
        public IntBuffer presentModes;
    }

    public static void createSwapChain() {
        try(MemoryStack stack = MemoryStack.stackPush()) {

            SwapChainSupportDetails swapChainSupport = querySwapChainSupport(VulkanDevices.physicalDevice, stack);

            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);
            int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
            VkExtent2D extent = chooseSwapExtent(swapChainSupport.capabilities);

            IntBuffer imageCount = stack.ints(swapChainSupport.capabilities.minImageCount() + 1);

            if(swapChainSupport.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapChainSupport.capabilities.maxImageCount()) {
                imageCount.put(0, swapChainSupport.capabilities.maxImageCount());
            }

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(VulkanSurfaces.surface);

            // Image settings
            createInfo.minImageCount(imageCount.get(0));
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            VulkanQueues.QueueFamilyIndices indices = VulkanQueues.findQueueFamilies(VulkanDevices.physicalDevice);

            if(!indices.graphicsFamily.equals(indices.presentFamily)) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(swapChainSupport.capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);

            createInfo.oldSwapchain(VK_NULL_HANDLE);

            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);

            if(vkCreateSwapchainKHR(VulkanLogicalDevices.device, createInfo, null, pSwapChain) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain.");
            }

            swapChain = pSwapChain.get(0);

            vkGetSwapchainImagesKHR(VulkanLogicalDevices.device, swapChain, imageCount, null);

            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

            vkGetSwapchainImagesKHR(VulkanLogicalDevices.device, swapChain, imageCount, pSwapchainImages);

            swapChainImages = new ArrayList<>(imageCount.get(0));

            for(int i = 0; i < pSwapchainImages.capacity(); i++) {
                swapChainImages.add(pSwapchainImages.get(i));
            }

            swapChainImageFormat = surfaceFormat.format();
            swapChainExtent = VkExtent2D.create().set(extent);
        }
    }

    public static VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer formats) {
        return formats.stream()
                .filter(availableFormat -> availableFormat.format() == VK_FORMAT_B8G8R8_UNORM)
                .filter(availableFormat -> availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                .findAny()
                .orElse(formats.get(0));
    }

    public static SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack) {
        SwapChainSupportDetails details = new SwapChainSupportDetails();
        details.capabilities = VkSurfaceCapabilitiesKHR.mallocStack(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, VulkanSurfaces.surface, details.capabilities);

        IntBuffer count = stack.ints(0);

        vkGetPhysicalDeviceSurfaceFormatsKHR(device, VulkanSurfaces.surface, count, null);
        if (count.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.mallocStack(count.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, VulkanSurfaces.surface, count, details.formats);
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(device, VulkanSurfaces.surface, count, null);
        if (count.get(0) != 0) {
            details.presentModes = stack.mallocInt(count.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, VulkanSurfaces.surface, count, details.presentModes);
        }

        return details;
    }

    public static int chooseSwapPresentMode(IntBuffer availablePresentModes) {
        for (int i = 0; i < availablePresentModes.capacity(); i++) {
            if (availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                return availablePresentModes.get(i);
            }
        }
        return VK_PRESENT_MODE_FIFO_KHR;
    }

    public static VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities) {
        if (capabilities.currentExtent().width() != VulkanTests.UINT32_MAX) {
            return capabilities.currentExtent();
        }

        VkExtent2D actualExtent = VkExtent2D.mallocStack().set(VulkanTests.WIDTH, VulkanTests.HEIGHT);

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }

    public static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }
}
