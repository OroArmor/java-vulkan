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
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanVertexBuffers {
    public static long vertexBuffer;
    public static long vertexBufferMemory;

    public static void createVertexBuffer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);

            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(Vertex.SIZEOF * VulkanTests.VERTICES.length);

            bufferInfo.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pVertexBuffer = stack.longs(0);

            if (vkCreateBuffer(VulkanLogicalDevices.device, bufferInfo, null, pVertexBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Unable to create vertex buffer.");
            }

            vertexBuffer = pVertexBuffer.get(0);

            VkMemoryRequirements memRequirements = VkMemoryRequirements.callocStack(stack);
            vkGetBufferMemoryRequirements(VulkanLogicalDevices.device, vertexBuffer, memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stack));

            LongBuffer pVertexBufferMemory = stack.longs(0);

            if (vkAllocateMemory(VulkanLogicalDevices.device, allocInfo, null, pVertexBufferMemory) != VK_SUCCESS) {
                throw new RuntimeException("Unable to allocate vertex buffer memory.");
            }

            vertexBufferMemory = pVertexBufferMemory.get(0);

            vkBindBufferMemory(VulkanLogicalDevices.device, vertexBuffer, vertexBufferMemory, 0);

            PointerBuffer vertexData = stack.mallocPointer(1);

            vkMapMemory(VulkanLogicalDevices.device, vertexBufferMemory, 0, bufferInfo.size(), 0, vertexData);
            memCopy(vertexData.getByteBuffer(0, (int) bufferInfo.size()), VulkanTests.VERTICES);
            vkUnmapMemory(VulkanLogicalDevices.device, vertexBufferMemory);
        }
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
}
