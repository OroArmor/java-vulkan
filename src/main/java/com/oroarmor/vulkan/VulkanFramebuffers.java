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

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanFramebuffers {
    public static List<Long> swapChainFrameBuffers;

    public static void createFramebuffers() {
        swapChainFrameBuffers = new ArrayList<>(VulkanImageViews.swapChainImageViews.size());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int i = 0; i < VulkanImageViews.swapChainImageViews.size(); i++) {
                LongBuffer imageViews = stack.longs(VulkanImageViews.swapChainImageViews.get(i));

                VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.callocStack(stack);
                framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
                framebufferInfo.renderPass(VulkanGraphicsPipeline.renderPass);
                framebufferInfo.pAttachments(imageViews);
                framebufferInfo.width(VulkanSwapChains.swapChainExtent.width());
                framebufferInfo.height(VulkanSwapChains.swapChainExtent.height());
                framebufferInfo.layers(1);

                LongBuffer pFramebuffer = stack.longs(0);
                if(vkCreateFramebuffer(VulkanLogicalDevices.device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer.");
                }
                swapChainFrameBuffers.add(pFramebuffer.get(0));
            }
        }
    }
}
