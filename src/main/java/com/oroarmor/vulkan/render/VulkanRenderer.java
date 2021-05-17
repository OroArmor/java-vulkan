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

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.oroarmor.vulkan.VulkanUtil;
import com.oroarmor.vulkan.context.VulkanContext;
import com.oroarmor.vulkan.glfw.GLFWContext;
import com.oroarmor.vulkan.render.pipeline.VulkanGraphicsPipeline;
import com.oroarmor.vulkan.util.Profiler;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static com.oroarmor.vulkan.VulkanUtil.UINT64_MAX;
import static com.oroarmor.vulkan.render.VulkanSemaphoreHandler.MAX_FRAMES_IN_FLIGHT;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderer {
    protected final VulkanContext vulkanContext;
    protected final GLFWContext glfwContext;

    protected VulkanSwapChain swapChain;
    protected VulkanImageViews imageViews;
    protected VulkanRenderPass renderPass;
    protected VulkanFrameBuffers frameBuffers;

    protected VulkanGraphicsPipeline graphicsPipeline;
    protected List<VkCommandBuffer> commandBuffers;

    protected final List<Consumer<VulkanRenderer>> renderSteps;
    private boolean frameBufferResized = false;
    private int frame;

    protected final Profiler profiler;

    public VulkanRenderer(VulkanContext vulkanContext, GLFWContext glfwContext) {
        this.vulkanContext = vulkanContext;
        this.glfwContext = glfwContext;
        renderSteps = new ArrayList<>();
        profiler = new Profiler("renderer");
        swapChain = new VulkanSwapChain(vulkanContext, this);
    }

    public void addRenderStep(Consumer<VulkanRenderer> renderStep) {
        renderSteps.add(renderStep);
    }

    public static Consumer<VulkanRenderer> renderIndexedWithShader(Shader shader, VulkanBuffer vertexBuffer, VulkanBuffer indexBuffer) {
        return renderer -> {
            renderer.graphicsPipeline.setShader(shader);
            renderer.profiler.profile(renderer.graphicsPipeline::rebuildIfNeeded, "Rebuild Graphics Pipeline");
            renderer.profiler.push("Add commands");
            try (MemoryStack stack = MemoryStack.stackPush()) {
                for (VkCommandBuffer commandBuffer : renderer.commandBuffers) {
                    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, renderer.graphicsPipeline.getPipeline());
                    vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(vertexBuffer.getBufferData().bufferHandle()), stack.longs(0));
                    vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getBufferData().bufferHandle(), 0, VK_INDEX_TYPE_UINT32);
                    vkCmdDrawIndexed(commandBuffer, indexBuffer.getSize(), 1, 0, 0, 0);
                }
            }
            renderer.profiler.pop();
        };
    }

    public void render() {
        profiler.push("render");
        profiler.profile(this::createRenderContext, "Create render context");
        profiler.profile(this::createPipeline, "Create pipeline");
        profiler.profile(this::computeRenderSteps, "Compute render steps");
        profiler.profile(this::submitRender, "Submit render");
        profiler.profile(this::cleanupPipeline, "Cleanup pipeline");
        profiler.profile(this::cleanUpRenderContext, "Cleanup context");
        profiler.pop();
    }

    protected void submitRender() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            profiler.push("Acquire next image");
            IntBuffer imageIndex = stack.mallocInt(1);
            VulkanSemaphoreHandler.VulkanSemaphore currentSemaphore = vulkanContext.getSemaphoreHandler().getSemaphores().get(frame);
            int result = vkAcquireNextImageKHR(vulkanContext.getLogicalDevice().getDevice(), swapChain.getSwapChain(), UINT64_MAX, currentSemaphore.getImageAvailableSemaphore(), VK_NULL_HANDLE, imageIndex);

            if (result == VK_ERROR_OUT_OF_DATE_KHR) {
                return;
            } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
                throw new RuntimeException("Unable to acquire swap chain image.");
            }
            profiler.pop();

            if (vulkanContext.getSemaphoreHandler().getImagesInFlight().get(imageIndex.get(0)) != VK_NULL_HANDLE) {
                vkWaitForFences(vulkanContext.getLogicalDevice().getDevice(), currentSemaphore.getInFlightFence(), true, UINT64_MAX);
            }
            vulkanContext.getSemaphoreHandler().getImagesInFlight().set(imageIndex.get(0), currentSemaphore.getInFlightFence());

//            updateUniformBuffer(imageIndex.get(0));

            profiler.push("Reset current fence before sumbit");
            vkResetFences(vulkanContext.getLogicalDevice().getDevice(), currentSemaphore.getInFlightFence());
            profiler.pop();

            profiler.push("Submit queue");
            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(stack.longs(currentSemaphore.getImageAvailableSemaphore()));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(commandBuffers.get(imageIndex.get(0))));

            LongBuffer signal = stack.longs(currentSemaphore.getRenderFinishedSemaphore());
            submitInfo.pSignalSemaphores(signal);

            VulkanUtil.checkVulkanResult(vkQueueSubmit(vulkanContext.getLogicalDevice().getGraphicsQueue(), submitInfo, currentSemaphore.getInFlightFence()), "Failed to submit draw call to command buffer");
            profiler.pop();

            profiler.push("Present rendered image");
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(signal);

            LongBuffer swapChains = stack.longs(swapChain.getSwapChain());

            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(swapChains);
            presentInfo.pImageIndices(imageIndex);
            presentInfo.pResults(null);

            result = vkQueuePresentKHR(vulkanContext.getLogicalDevice().getPresentQueue(), presentInfo);

            if (result != VK_SUCCESS && !(result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR || frameBufferResized)) {
                throw new RuntimeException("Unable to present swap chain image.");
            }
            profiler.pop();

            profiler.push("Wait for present queue to idle");
            vkQueueWaitIdle(vulkanContext.getLogicalDevice().getPresentQueue());
            profiler.pop();

            frame = (frame + 1) % MAX_FRAMES_IN_FLIGHT;
        }
    }

    protected void createPipeline() {
        this.graphicsPipeline = new VulkanGraphicsPipeline(vulkanContext, this);
        commandBuffers = createCommandBuffers();
    }

    protected List<VkCommandBuffer> createCommandBuffers() {
        int commandBuffersCount = frameBuffers.getFrameBuffers().size();
        List<VkCommandBuffer> commandBuffers = new ArrayList<>(commandBuffersCount);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(vulkanContext.getCommandPool().getCommandPool());
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(commandBuffersCount);

            PointerBuffer pCommandPools = stack.mallocPointer(commandBuffersCount);

            VulkanUtil.checkVulkanResult(vkAllocateCommandBuffers(vulkanContext.getLogicalDevice().getDevice(), allocInfo, pCommandPools), "Unable to allocate command buffers.");

            for (int i = 0; i < commandBuffersCount; i++) {
                commandBuffers.add(new VkCommandBuffer(pCommandPools.get(i), vulkanContext.getLogicalDevice().getDevice()));
            }

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(0); // Optional
            beginInfo.pInheritanceInfo(null); // Optional

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
            renderPassInfo.renderPass(renderPass.getRenderPass());
            renderPassInfo.renderArea(VkRect2D.callocStack(stack).set(VkOffset2D.callocStack(stack).set(0, 0), swapChain.getSwapChainExtent()));

            VkClearValue.Buffer clearValue = VkClearValue.callocStack(1, stack);
            clearValue.color().float32(stack.floats(0, 0, 0, 1));
            renderPassInfo.pClearValues(clearValue);

            for (int i = 0; i < commandBuffers.size(); i++) {
                VkCommandBuffer commandBuffer = commandBuffers.get(i);

                VulkanUtil.checkVulkanResult(vkBeginCommandBuffer(commandBuffer, beginInfo), "Failed to begin recording command buffer!");
                renderPassInfo.framebuffer(frameBuffers.getFrameBuffers().get(i));
                vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
            }
        }

        return commandBuffers;
    }

    protected void computeRenderSteps() {
        profiler.push("Add render steps");
        renderSteps.forEach(step -> profiler.profile(() -> step.accept(this), "Compute Render step " + step.toString()));
        profiler.pop();
        profiler.push("End Command Buffers");
        for (VkCommandBuffer buffer : commandBuffers) {
            vkCmdEndRenderPass(buffer);
            VulkanUtil.checkVulkanResult(vkEndCommandBuffer(buffer), "Failed to record command buffer.");
        }
        profiler.pop();
    }

    protected void cleanupPipeline() {
        commandBuffers.forEach(vkCommandBuffer -> vkFreeCommandBuffers(vulkanContext.getLogicalDevice().getDevice(), vulkanContext.getCommandPool().getCommandPool(), vkCommandBuffer));
        commandBuffers.clear();
        graphicsPipeline.close();
    }

    protected void createRenderContext() {
        if(vulkanContext.getSemaphoreHandler().getImagesInFlight().isEmpty()) {
            vulkanContext.getSemaphoreHandler().createImagesInFlight(this);
        }

        profiler.push("Create Image Views");
        imageViews = new VulkanImageViews(vulkanContext, this);
        profiler.pop();
        profiler.push("Create Render Pass ");
        renderPass = new VulkanRenderPass(vulkanContext, this);
        profiler.pop();
        profiler.push("Create Frame Buffers");
        frameBuffers = new VulkanFrameBuffers(vulkanContext, this);
        profiler.pop();
    }

    protected void cleanUpRenderContext() {
        renderSteps.clear();
        frameBuffers.close();
        renderPass.close();
        imageViews.close();
//        swapChain.close();
    }

    public VulkanSwapChain getSwapChain() {
        return swapChain;
    }

    public VulkanRenderPass getRenderPass() {
        return renderPass;
    }

    public VulkanImageViews getImageViews() {
        return imageViews;
    }

    public VulkanGraphicsPipeline getGraphicsPipeline() {
        return graphicsPipeline;
    }

    public Profiler getProfiler() {
        return this.profiler;
    }
}
