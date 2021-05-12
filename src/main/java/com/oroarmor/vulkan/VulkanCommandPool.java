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

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanCommandPool implements AutoCloseable {
    protected final long commandPool;
    protected final VulkanContext context;

    public VulkanCommandPool(VulkanContext context) {
        this.context = context;
        this.commandPool = createCommandPool();
    }

    protected long createCommandPool() {
        VulkanPhysicalDevice.QueueFamilyIndices indices = context.getPhysicalDevice().getQueueFamilyIndices();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(indices.graphicsFamily);
            poolInfo.flags(0);

            LongBuffer pCommandPool = stack.mallocLong(1);
            VulkanUtil.checkVulkanResult(vkCreateCommandPool(context.getLogicalDevice().getDevice(), poolInfo, null, pCommandPool), "Failed to create command pool");

            return pCommandPool.get(0);
        }
    }

    @Override
    public void close() {
        vkDestroyCommandPool(context.getLogicalDevice().getDevice(), commandPool, null);
    }
}
