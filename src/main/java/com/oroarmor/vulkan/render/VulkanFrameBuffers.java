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

import com.oroarmor.vulkan.VulkanUtil;
import com.oroarmor.vulkan.context.VulkanContext;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanFrameBuffers implements AutoCloseable {
    protected final List<Long> frameBuffers;
    protected final VulkanContext context;
    protected final VulkanRenderer renderer;

    public VulkanFrameBuffers(VulkanContext context, VulkanRenderer renderer) {
        this.context = context;
        this.renderer = renderer;
        frameBuffers = createFrameBuffers();
    }

    protected List<Long> createFrameBuffers() {
        List<Long> frameBuffers = new ArrayList<>(renderer.getImageViews().getSwapChainImageViews().size());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer imageViews = stack.mallocLong(1);
            LongBuffer pFramebuffer = stack.mallocLong(1);
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.callocStack(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderer.getRenderPass().getRenderPass());
            framebufferInfo.width(renderer.getSwapChain().getSwapChainExtent().width());
            framebufferInfo.height(renderer.getSwapChain().getSwapChainExtent().height());
            framebufferInfo.layers(1);
            for (Long imageView : renderer.getImageViews().getSwapChainImageViews()) {
                imageViews.put(0, imageView);

                framebufferInfo.pAttachments(imageViews);
                VulkanUtil.checkVulkanResult(vkCreateFramebuffer(context.getLogicalDevice().getDevice(), framebufferInfo, null, pFramebuffer), "Failed to create framebuffer");
                frameBuffers.add(pFramebuffer.get(0));
            }
            return frameBuffers;
        }
    }

    public List<Long> getFrameBuffers () {
        return frameBuffers;
    }

    public void close() {
        frameBuffers.forEach(framebuffer -> vkDestroyFramebuffer(context.getLogicalDevice().getDevice(), framebuffer, null));
    }
}
