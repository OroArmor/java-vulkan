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

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;

import com.oroarmor.initial.VulkanLogicalDevices;
import com.oroarmor.initial.VulkanVertexBuffers;
import com.oroarmor.vulkan.VulkanUtil;
import com.oroarmor.vulkan.context.VulkanContext;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanBuffer implements AutoCloseable {
    protected final BufferData buffer;
    protected final VulkanContext context;
    protected final BufferLayout layout;
    protected final List<CopyableMemory> data;
    protected final int usage;

    public VulkanBuffer(VulkanContext context, BufferLayout layout, List<CopyableMemory> data, int usage) {
        this.context = context;
        this.layout = layout;
        this.data = data;
        this.usage = usage;
        buffer = createBuffer();
    }

    private BufferData createBuffer() {
        int size = layout.getStride() * data.size();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBufferMemory = stack.longs(0);
            LongBuffer pBuffer = stack.longs(0);

            createVulkanBuffer(size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pBuffer, pBufferMemory, stack);

            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);

            PointerBuffer dataPointer = stack.mallocPointer(size);
            vkMapMemory(context.getLogicalDevice().getDevice(), stagingBufferMemory, 0, size, 0, dataPointer);
            ByteBuffer dataBuffer = dataPointer.getByteBuffer(0, size);
            data.forEach(datum -> datum.memCopy(dataBuffer));

            vkUnmapMemory(context.getLogicalDevice().getDevice(), stagingBufferMemory);

            createVulkanBuffer(size, VK_BUFFER_USAGE_TRANSFER_DST_BIT | usage, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, pBuffer, pBufferMemory, stack);

            BufferData buffer = new BufferData(pBuffer.get(0), pBufferMemory.get(0));

            copyBuffer(stagingBuffer, buffer.bufferHandle, size);
            vkDestroyBuffer(context.getLogicalDevice().getDevice(), stagingBuffer, null);
            vkFreeMemory(context.getLogicalDevice().getDevice(), stagingBufferMemory, null);

            return buffer;
        }
    }

    protected void createVulkanBuffer(int size, int usage, int properties, LongBuffer pVertexBuffer, LongBuffer pVertexBufferMemory, MemoryStack stack) {
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);

        bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferInfo.size(size);

        bufferInfo.usage(usage);
        bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

        VulkanUtil.checkVulkanResult(vkCreateBuffer(context.getLogicalDevice().getDevice(), bufferInfo, null, pVertexBuffer), "Unable to create buffer");

        VkMemoryRequirements memRequirements = VkMemoryRequirements.callocStack(stack);
        vkGetBufferMemoryRequirements(context.getLogicalDevice().getDevice(), pVertexBuffer.get(0), memRequirements);

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
        allocInfo.allocationSize(memRequirements.size());
        allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), properties, stack));

        VulkanUtil.checkVulkanResult(vkAllocateMemory(context.getLogicalDevice().getDevice(), allocInfo, null, pVertexBufferMemory), "Unable to allocate buffer memory");

        vkBindBufferMemory(context.getLogicalDevice().getDevice(), pVertexBuffer.get(0), pVertexBufferMemory.get(0), 0);
    }

    protected void copyBuffer(long srcBuffer, long dstBuffer, int size) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            try (VulkanCommandBuffer commandBuffer = new VulkanCommandBuffer(context)) {
                VkCommandBuffer buffer = commandBuffer.startRecording(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

                VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
                copyRegion.srcOffset(0).dstOffset(0).size(size);

                vkCmdCopyBuffer(buffer, srcBuffer, dstBuffer, copyRegion);

                commandBuffer.finishRecording();

                VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
                submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
                PointerBuffer pCommandBuffers = stack.callocPointer(1);
                pCommandBuffers.put(0, buffer.address());
                submitInfo.pCommandBuffers(pCommandBuffers);

                vkQueueSubmit(context.getLogicalDevice().getGraphicsQueue(), submitInfo, VK_NULL_HANDLE);
                vkQueueWaitIdle(context.getLogicalDevice().getGraphicsQueue());
            }
        }
    }

    protected int findMemoryType(int typeFilter, int properties, MemoryStack stack) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.callocStack(stack);
        vkGetPhysicalDeviceMemoryProperties(context.getPhysicalDevice().getPhysicalDevice(), memProperties);

        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes().get(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }
        throw new RuntimeException("Failed to find suitable memory type");
    }

    @Override
    public void close()  {
        vkDestroyBuffer(context.getLogicalDevice().getDevice(), buffer.bufferHandle, null);
        vkFreeMemory(context.getLogicalDevice().getDevice(), buffer.bufferMemory, null);
    }


    public static record BufferData(long bufferHandle, long bufferMemory) {
    }
}
