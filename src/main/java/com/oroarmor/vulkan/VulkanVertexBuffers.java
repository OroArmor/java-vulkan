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

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanVertexBuffers {
    public static long vertexBuffer;
    public static long vertexBufferMemory;

    public static long indexBuffer;
    public static long indexBufferMemory;

    public static void createVertexBuffer() {
        int size = Vertex.SIZEOF * VulkanTests.VERTICES.length;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBufferMemory = stack.longs(0);
            LongBuffer pBuffer = stack.longs(0);

            createBuffer(size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pBuffer, pBufferMemory, stack);

            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);

            PointerBuffer vertexData = stack.mallocPointer(1);
            vkMapMemory(VulkanLogicalDevices.device, stagingBufferMemory, 0, size, 0, vertexData);
            memCopy(vertexData.getByteBuffer(0, size), VulkanTests.VERTICES);
            vkUnmapMemory(VulkanLogicalDevices.device, stagingBufferMemory);

            createBuffer(size, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, pBuffer, pBufferMemory, stack);

            vertexBuffer = pBuffer.get(0);
            vertexBufferMemory = pBufferMemory.get(0);

            copyBuffer(stagingBuffer, vertexBuffer, size);
            vkDestroyBuffer(VulkanLogicalDevices.device, stagingBuffer, null);
            vkFreeMemory(VulkanLogicalDevices.device, stagingBufferMemory, null);
        }
    }

    public static void createIndexBuffer() {
        int size = Integer.BYTES * VulkanTests.INDICES.length;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBufferMemory = stack.longs(0);
            LongBuffer pBuffer = stack.longs(0);

            createBuffer(size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pBuffer, pBufferMemory, stack);

            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);

            PointerBuffer vertexData = stack.mallocPointer(1);
            vkMapMemory(VulkanLogicalDevices.device, stagingBufferMemory, 0, size, 0, vertexData);
            memCopy(vertexData.getByteBuffer(0, size), VulkanTests.INDICES);
            vkUnmapMemory(VulkanLogicalDevices.device, stagingBufferMemory);

            createBuffer(size, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, pBuffer, pBufferMemory, stack);

            indexBuffer = pBuffer.get(0);
            indexBufferMemory = pBufferMemory.get(0);

            copyBuffer(stagingBuffer, indexBuffer, size);
            vkDestroyBuffer(VulkanLogicalDevices.device, stagingBuffer, null);
            vkFreeMemory(VulkanLogicalDevices.device, stagingBufferMemory, null);
        }
    }

    private static void createBuffer(int size, int usage, int properties, LongBuffer pVertexBuffer, LongBuffer pVertexBufferMemory, MemoryStack stack) {
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);

        bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferInfo.size(size);

        bufferInfo.usage(usage);
        bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

        if (vkCreateBuffer(VulkanLogicalDevices.device, bufferInfo, null, pVertexBuffer) != VK_SUCCESS) {
            throw new RuntimeException("Unable to create vertex buffer.");
        }

        VkMemoryRequirements memRequirements = VkMemoryRequirements.callocStack(stack);
        vkGetBufferMemoryRequirements(VulkanLogicalDevices.device, pVertexBuffer.get(0), memRequirements);

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
        allocInfo.allocationSize(memRequirements.size());
        allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), properties, stack));

        if (vkAllocateMemory(VulkanLogicalDevices.device, allocInfo, null, pVertexBufferMemory) != VK_SUCCESS) {
            throw new RuntimeException("Unable to allocate vertex buffer memory.");
        }

        vkBindBufferMemory(VulkanLogicalDevices.device, pVertexBuffer.get(0), pVertexBufferMemory.get(0), 0);
    }

    private static void memCopy(ByteBuffer byteBuffer, Vertex[] vertices) {
        for (Vertex vertex : vertices) {
            byteBuffer.putFloat(vertex.pos.x());
            byteBuffer.putFloat(vertex.pos.y());

            byteBuffer.putFloat(vertex.color.x());
            byteBuffer.putFloat(vertex.color.y());
            byteBuffer.putFloat(vertex.color.z());
        }
    }

    private static void memCopy(ByteBuffer byteBuffer, int[] indices) {
        for (int i : indices) {
            byteBuffer.putShort((short) i);
        }
    }

        public static int findMemoryType(int typeFilter, int properties, MemoryStack stack) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.callocStack(stack);
        vkGetPhysicalDeviceMemoryProperties(VulkanDevices.physicalDevice, memProperties);

        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes().get(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }
        throw new RuntimeException("Failed to find suitable memory type");
    }

    public static void copyBuffer(long srcBuffer, long dstBuffer, int size) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandPool(VulkanCommandPools.commandPool);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.callocPointer(1);
            vkAllocateCommandBuffers(VulkanLogicalDevices.device, allocInfo, pCommandBuffer);
            VkCommandBuffer buffer = new VkCommandBuffer(pCommandBuffer.get(0), VulkanLogicalDevices.device);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            vkBeginCommandBuffer(buffer, beginInfo);

            VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
            copyRegion.srcOffset(0).dstOffset(0).size(size);

            vkCmdCopyBuffer(buffer, srcBuffer, dstBuffer, copyRegion);
            vkEndCommandBuffer(buffer);

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(pCommandBuffer);

            vkQueueSubmit(VulkanLogicalDevices.graphicsQueue, submitInfo, VK_NULL_HANDLE);
            vkQueueWaitIdle(VulkanLogicalDevices.graphicsQueue);

            vkFreeCommandBuffers(VulkanLogicalDevices.device, VulkanCommandPools.commandPool, pCommandBuffer);
        }
    }
}
