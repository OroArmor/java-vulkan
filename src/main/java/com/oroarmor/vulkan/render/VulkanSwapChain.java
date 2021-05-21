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

package com.oroarmor.vulkan.render;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import com.oroarmor.vulkan.context.VulkanContext;
import com.oroarmor.vulkan.context.VulkanPhysicalDevice;
import com.oroarmor.vulkan.util.VulkanUtil;
import org.joml.Math;
import org.joml.Vector2i;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanSwapChain implements AutoCloseable {
    protected final int swapChainImageFormat;
    protected final VkExtent2D swapChainExtent;
    protected final int imageCount;
    protected final long swapChain;
    protected final List<Long> swapChainImages;

    protected final VkSurfaceFormatKHR swapSurfaceFormatCache;

    protected final VulkanContext context;
    protected final VulkanRenderer renderer;

    public VulkanSwapChain(VulkanContext context, VulkanRenderer renderer) {
        this.context = context;
        this.renderer = renderer;
        swapSurfaceFormatCache = chooseSwapSurfaceFormat();
        swapChainImageFormat = swapSurfaceFormatCache.format();
        swapChainExtent = chooseSwapExtent();
        imageCount = getImageCount();
        swapChain = createSwapChain();
        swapChainImages = createSwapChainImages();
    }

    protected int getImageCount() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanPhysicalDevice.SwapChainSupportDetails swapChainSupport = context.getPhysicalDevice().getSwapChainSupport();
            IntBuffer imageCount = stack.ints(swapChainSupport.capabilities.minImageCount() + 1);

            if (swapChainSupport.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapChainSupport.capabilities.maxImageCount()) {
                imageCount.put(0, swapChainSupport.capabilities.maxImageCount());
            }
            return imageCount.get(0);
        }
    }

    protected long createSwapChain() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanPhysicalDevice.SwapChainSupportDetails swapChainSupport = context.getPhysicalDevice().getSwapChainSupport();

            VkSurfaceFormatKHR surfaceFormat = swapSurfaceFormatCache;
            int presentMode = chooseSwapPresentMode();

            IntBuffer imageCount = stack.ints(this.imageCount);

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(context.getSurface().getSurface());

            // Image settings
            createInfo.minImageCount(imageCount.get(0));
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(swapChainExtent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            VulkanPhysicalDevice.QueueFamilyIndices indices = context.getPhysicalDevice().getQueueFamilyIndices();

            if (!indices.graphicsFamily.equals(indices.presentFamily)) {
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

            VulkanUtil.checkVulkanResult(vkCreateSwapchainKHR(context.getLogicalDevice().getDevice(), createInfo, null, pSwapChain), "Failed to create swap chain");

            return pSwapChain.get(0);
        }
    }

    protected List<Long> createSwapChainImages() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer imageCount = stack.ints(this.imageCount);
            vkGetSwapchainImagesKHR(context.getLogicalDevice().getDevice(), swapChain, imageCount, null);

            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));
            vkGetSwapchainImagesKHR(context.getLogicalDevice().getDevice(), swapChain, imageCount, pSwapchainImages);

            List<Long> swapChainImages = new ArrayList<>(imageCount.get(0));
            for (int i = 0; i < pSwapchainImages.capacity(); i++) {
                swapChainImages.add(pSwapchainImages.get(i));
            }

            return swapChainImages;
        }
    }

    protected VkSurfaceFormatKHR chooseSwapSurfaceFormat() {
        VkSurfaceFormatKHR.Buffer formats = context.getPhysicalDevice().getSwapChainSupport().getFormats();
        return formats.stream()
                .filter(availableFormat -> availableFormat.format() == VK_FORMAT_B8G8R8_UNORM)
                .filter(availableFormat -> availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                .findAny()
                .orElse(formats.get(0));
    }

    protected VkExtent2D chooseSwapExtent() {
        VkSurfaceCapabilitiesKHR capabilities = context.getPhysicalDevice().getNewSwapChainSupport().capabilities;
        if (capabilities.currentExtent().width() != VulkanUtil.UINT32_MAX) {
            return capabilities.currentExtent();
        }

        Vector2i screenSize = context.getGLFWContext().getScreenSize();

        VkExtent2D actualExtent = VkExtent2D.mallocStack().set(screenSize.x, screenSize.y);

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(Math.clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(Math.clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }

    protected int chooseSwapPresentMode() {
        List<Integer> presentModes = context.getPhysicalDevice().getSwapChainSupport().presentModes;
        for (Integer presentMode : presentModes) {
            if (presentMode == VK_PRESENT_MODE_MAILBOX_KHR) {
                return presentMode;
            }
        }
        return VK_PRESENT_MODE_FIFO_KHR;
    }

    @Override
    public void close() {
        for (long imageView : swapChainImages) {
            // TODO: Still broken, thought was fixed because never called this method
            // vkDestroyImageView(context.getLogicalDevice().getDevice(), imageView, null);
        }

        vkDestroySwapchainKHR(context.getLogicalDevice().getDevice(), swapChain, null);
    }

    public VkExtent2D getSwapChainExtent() {
        return swapChainExtent;
    }

    public long getSwapChain() {
        return swapChain;
    }

    public int getImageFormat() {
        return swapChainImageFormat;
    }
}
