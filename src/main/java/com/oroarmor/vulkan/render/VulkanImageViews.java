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

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import com.oroarmor.vulkan.context.VulkanContext;
import com.oroarmor.vulkan.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanImageViews implements AutoCloseable {
    protected final List<Long> swapChainImageViews;
    protected final VulkanContext context;
    protected final VulkanRenderer renderer;

    public VulkanImageViews(VulkanContext context, VulkanRenderer renderer) {
        this.context = context;
        this.renderer = renderer;
        swapChainImageViews = createSwapChainImageViews();
    }

    private List<Long> createSwapChainImageViews() {
        List<Long> swapChainImageViews = new ArrayList<>(renderer.getSwapChain().swapChainImages.size());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pImageView = stack.mallocLong(1);

            for (long swapChainImage : renderer.getSwapChain().swapChainImages) {
                VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.callocStack(stack);

                createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                createInfo.image(swapChainImage);
                createInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
                createInfo.format(renderer.getSwapChain().getImageFormat());

                createInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .a(VK_COMPONENT_SWIZZLE_IDENTITY);

                createInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseMipLevel(0)
                        .levelCount(1)
                        .baseArrayLayer(0)
                        .layerCount(1);

                VulkanUtil.checkVulkanResult(vkCreateImageView(context.getLogicalDevice().getDevice(), createInfo, null, pImageView), "Failed to create image views");

                swapChainImageViews.add(pImageView.get(0));
            }
            return swapChainImageViews;
        }
    }

    @Override
    public void close() {
        for (long imageView : swapChainImageViews) {
            vkDestroyImageView(context.getLogicalDevice().getDevice(), imageView, null);
        }
    }

    public List<Long> getSwapChainImageViews() {
        return swapChainImageViews;
    }
}
