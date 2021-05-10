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

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanCommandPools {
    public static long commandPool;
    public static List<VkCommandBuffer> commandBuffers;

    public static void createCommandPool() {
        VulkanQueues.QueueFamilyIndices indices = VulkanQueues.findQueueFamilies(VulkanDevices.physicalDevice);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(indices.graphicsFamily);
            poolInfo.flags(0);

            LongBuffer pCommandPool = stack.mallocLong(1);
            if (vkCreateCommandPool(VulkanLogicalDevices.device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool.");
            }
            commandPool = pCommandPool.get(0);
        }
    }

    public static void createCommandBuffers() {
        int commandBuffersCount = VulkanFramebuffers.swapChainFrameBuffers.size();
        commandBuffers = new ArrayList<>(commandBuffersCount);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(commandBuffersCount);

            PointerBuffer pCommandPools = stack.mallocPointer(commandBuffersCount);

            if (vkAllocateCommandBuffers(VulkanLogicalDevices.device, allocInfo, pCommandPools) != VK_SUCCESS) {
                throw new RuntimeException("Unable to allocate command buffers.");
            }

            for (int i = 0; i < commandBuffersCount; i++) {
                commandBuffers.add(new VkCommandBuffer(pCommandPools.get(i), VulkanLogicalDevices.device));
            }

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(0); // Optional
            beginInfo.pInheritanceInfo(null); // Optional

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
            renderPassInfo.renderPass(VulkanGraphicsPipeline.renderPass);
            renderPassInfo.renderArea(VkRect2D.callocStack(stack).set(VkOffset2D.callocStack(stack).set(0, 0), VulkanSwapChains.swapChainExtent));

            VkClearValue.Buffer clearValue = VkClearValue.callocStack(1, stack);
            clearValue.color().float32(stack.floats(0, 0, 0, 1));
            renderPassInfo.pClearValues(clearValue);

            for (int i = 0; i < commandBuffers.size(); i++) {
                VkCommandBuffer commandBuffer = commandBuffers.get(i);


                if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to begin recording command buffer!");
                }

                renderPassInfo.framebuffer(VulkanFramebuffers.swapChainFrameBuffers.get(i));

                vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, VulkanGraphicsPipeline.graphicsPipeline);
                vkCmdDraw(commandBuffer, 3, 1, 0, 0);
                vkCmdEndRenderPass(commandBuffer);

                if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to record command buffer.");
                }
            }
        }
    }

}
