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

import java.util.ArrayList;
import java.util.List;

import com.oroarmor.vulkan.context.VulkanContext;
import com.oroarmor.vulkan.util.VulkanUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanCommandBuffer implements AutoCloseable {
    protected final VulkanContext context;
    protected final VkCommandBuffer commandBuffer;
    protected boolean recorded = false;

    public VulkanCommandBuffer(VulkanContext context) {
        this.context = context;
        this.commandBuffer = createCommandBuffer();
    }

    private VulkanCommandBuffer(VulkanContext context, VkCommandBuffer commandBuffer) {
        this.context = context;
        this.commandBuffer = commandBuffer;
    }

    public static List<VulkanCommandBuffer> createCommandBuffers(int count, VulkanContext context) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(context.getCommandPool().getCommandPool());
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(count);

            PointerBuffer pCommandBuffer = stack.mallocPointer(count);

            VulkanUtil.checkVulkanResult(vkAllocateCommandBuffers(context.getLogicalDevice().getDevice(), allocInfo, pCommandBuffer), "Unable to allocate command buffers");

            List<VulkanCommandBuffer> commandBuffers = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                VkCommandBuffer vkCommandBuffer = new VkCommandBuffer(pCommandBuffer.get(i), context.getLogicalDevice().getDevice());
                commandBuffers.add(new VulkanCommandBuffer(context, vkCommandBuffer));
            }
            return commandBuffers;
        }
    }

    private VkCommandBuffer createCommandBuffer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(context.getCommandPool().getCommandPool());
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);

            VulkanUtil.checkVulkanResult(vkAllocateCommandBuffers(context.getLogicalDevice().getDevice(), allocInfo, pCommandBuffer), "Unable to allocate command buffers");
            return new VkCommandBuffer(pCommandBuffer.get(0), context.getLogicalDevice().getDevice());
        }
    }

    public VkCommandBuffer startRecording(int flags) {
        assert !recorded : "Buffer has already been recorded.";
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(flags); // Optional
            beginInfo.pInheritanceInfo(null); // Optional

            VulkanUtil.checkVulkanResult(vkBeginCommandBuffer(commandBuffer, beginInfo), "Failed to begin recording command buffer");

            return commandBuffer;
        }
    }

    public void finishRecording() {
        VulkanUtil.checkVulkanResult(vkEndCommandBuffer(commandBuffer), "Failed to record command buffer.");
        this.recorded = true;
    }

    public void close() {
        vkFreeCommandBuffers(context.getLogicalDevice().getDevice(), context.getCommandPool().getCommandPool(), commandBuffer);
    }

    public VkCommandBuffer getCommandBuffer() {
        return commandBuffer;
    }
}
