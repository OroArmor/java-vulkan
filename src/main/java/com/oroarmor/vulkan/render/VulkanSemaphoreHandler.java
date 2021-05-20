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
import java.util.stream.Collectors;

import com.oroarmor.vulkan.util.VulkanUtil;
import com.oroarmor.vulkan.context.VulkanContext;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanSemaphoreHandler implements AutoCloseable {
    public static final int MAX_FRAMES_IN_FLIGHT = 5;
    protected final VulkanContext context;
    protected final List<VulkanSemaphore> semaphoreList;
    protected final List<Long> imagesInFlight;

    public VulkanSemaphoreHandler(VulkanContext context) {
        this.context = context;
        semaphoreList = createSemaphore();
        imagesInFlight = new ArrayList<>();
    }

    public void createImagesInFlight(VulkanRenderer renderer) {
        int swapChainImageCount = renderer.getSwapChain().getImageCount();
        imagesInFlight.clear();
        for (int i = 0; i < swapChainImageCount; i++) {
            imagesInFlight.add(VK_NULL_HANDLE);
        }
    }

    protected List<VulkanSemaphore> createSemaphore() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            List<VulkanSemaphore.Builder> builders = new ArrayList<>(MAX_FRAMES_IN_FLIGHT);
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                builders.add(new VulkanSemaphore.Builder(context));
            }

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.callocStack(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImageAvailableSemaphore = stack.longs(0);
            LongBuffer pRenderFinishedSemaphore = stack.longs(0);
            LongBuffer pFence = stack.longs(0);

            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                VulkanUtil.checkVulkanResult(vkCreateSemaphore(context.getLogicalDevice().getDevice(), semaphoreInfo, null, pImageAvailableSemaphore), "Failed to create image available semaphore " + i);
                VulkanUtil.checkVulkanResult(vkCreateSemaphore(context.getLogicalDevice().getDevice(), semaphoreInfo, null, pRenderFinishedSemaphore), "Failed to create render finished semaphore " + i);
                VulkanUtil.checkVulkanResult(vkCreateFence(context.getLogicalDevice().getDevice(), fenceInfo, null, pFence), "Failed to create in flight fence " + i);

                builders.get(i).imageAvailableSemaphore(pImageAvailableSemaphore.get(0)).renderFinishedSemaphore(pRenderFinishedSemaphore.get(0)).inFlightFence(pFence.get(0));
            }

            return builders.stream().map(VulkanSemaphore.Builder::build).collect(Collectors.toList());
        }
    }


    public List<VulkanSemaphore> getSemaphores() {
        return semaphoreList;
    }

    public List<Long> getImagesInFlight() {
        return imagesInFlight;
    }

    public static class VulkanSemaphore implements AutoCloseable {
        protected final long imageAvailableSemaphore, renderFinishedSemaphore, inFlightFence;
        protected final VulkanContext context;

        public VulkanSemaphore(long imageAvailableSemaphore, long renderFinishedSemaphore, long inFlightFence, VulkanContext context) {
            this.imageAvailableSemaphore = imageAvailableSemaphore;
            this.renderFinishedSemaphore = renderFinishedSemaphore;
            this.inFlightFence = inFlightFence;
            this.context = context;
        }

        public void close() {
            vkDestroySemaphore(context.getLogicalDevice().getDevice(), imageAvailableSemaphore, null);
            vkDestroySemaphore(context.getLogicalDevice().getDevice(), renderFinishedSemaphore, null);
            vkDestroyFence(context.getLogicalDevice().getDevice(), inFlightFence, null);
        }

        public long getImageAvailableSemaphore() {
            return imageAvailableSemaphore;
        }

        public long getInFlightFence() {
            return inFlightFence;
        }

        public long getRenderFinishedSemaphore() {
            return renderFinishedSemaphore;
        }

        public static class Builder {
            private long imageAvailableSemaphore, renderFinishedSemaphore, inFlightFence;
            private VulkanContext context;

            public Builder(VulkanContext context) {
                this.context = context;
            }


            public Builder imageAvailableSemaphore(long imageAvailableSemaphore) {
                this.imageAvailableSemaphore = imageAvailableSemaphore;
                return this;
            }

            public Builder renderFinishedSemaphore(long renderFinishedSemaphore) {
                this.renderFinishedSemaphore = renderFinishedSemaphore;
                return this;
            }

            public Builder inFlightFence(long inFlightFence) {
                this.inFlightFence = inFlightFence;
                return this;
            }

            public VulkanSemaphore build() {
                return new VulkanSemaphore(imageAvailableSemaphore, renderFinishedSemaphore, inFlightFence, context);
            }
        }
    }

    public void close() {
        semaphoreList.forEach(VulkanSemaphore::close);
    }
}
