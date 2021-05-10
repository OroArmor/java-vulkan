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
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanSemaphore {
    public static List<Long> imageAvailableSemaphore, renderFinishedSemaphore, inFlightFences, imagesInFlight;


    public static void createSemaphore() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            imageAvailableSemaphore = new ArrayList<>(VulkanTests.MAX_FRAMES_IN_FLIGHT);
            renderFinishedSemaphore = new ArrayList<>(VulkanTests.MAX_FRAMES_IN_FLIGHT);
            inFlightFences = new ArrayList<>(VulkanTests.MAX_FRAMES_IN_FLIGHT);
            imagesInFlight = new ArrayList<>(VulkanSwapChains.swapChainImages.size());
            for (int i = 0; i < VulkanSwapChains.swapChainImages.size(); i++) {
                imagesInFlight.add(VK_NULL_HANDLE);
            }

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.callocStack(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImageAvailableSemaphore = stack.longs(0);
            LongBuffer pRenderFinishedSemaphore = stack.longs(0);
            LongBuffer pFence = stack.longs(0);

            for (int i = 0; i < VulkanTests.MAX_FRAMES_IN_FLIGHT; i++) {
                if (vkCreateSemaphore(VulkanLogicalDevices.device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS ||
                        vkCreateSemaphore(VulkanLogicalDevices.device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS ||
                        vkCreateFence(VulkanLogicalDevices.device, fenceInfo, null, pFence) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create Semaphores");
                }

                imageAvailableSemaphore.add(pImageAvailableSemaphore.get(0));
                renderFinishedSemaphore.add(pImageAvailableSemaphore.get(0));
                inFlightFences.add(pFence.get(0));
            }
        }
    }
}
