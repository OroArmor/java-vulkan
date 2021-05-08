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

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collection;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import static com.oroarmor.vulkan.VulkanSurfaces.surface;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanTests {
    public static final int UINT32_MAX = 0xFFFFFFFF;
    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    public static VkInstance instance;
    public static long window;
    public static long debugMessenger;

    public static final boolean ENABLE_VALIDATION_LAYERS = DEBUG.get(true);

    public static void main(String[] args) {
        initGLFW();
        window = glfwCreateWindow(WIDTH, HEIGHT, "Vulkan", MemoryUtil.NULL, MemoryUtil.NULL);
        initVulkan();

        loop();
        cleanup();
    }

    private static void initVulkan() {
        instance = createVulkanInstance();
        VulkanDebug.setupDebugMessenger();
        VulkanSurfaces.createSurface();
        VulkanDevices.pickPhysicalDevice();
        VulkanLogicalDevices.createLogicalDevice(VulkanDevices.physicalDevice);
        VulkanSwapChains.createSwapChain();
        VulkanImageViews.createImageViews();
        VulkanGraphicsPipeline.createRenderPass();
        VulkanGraphicsPipeline.createGraphicsPipeline();
        VulkanFramebuffers.createFramebuffers();
        VulkanCommandPools.createCommandPool();
        VulkanCommandPools.createCommandBuffers();
        VulkanSemaphore.createSemaphore();
    }

    private static void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            drawFrame();
        }
    }

    public static void drawFrame() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer imageIndex = stack.mallocInt(1);
            vkAcquireNextImageKHR(VulkanLogicalDevices.device, VulkanSwapChains.swapChain, UINT64_MAX, VulkanSemaphore.imageAvailableSemaphore, VK_NULL_HANDLE, imageIndex);

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(stack.longs(VulkanSemaphore.imageAvailableSemaphore));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(VulkanCommandPools.commandBuffers.get(imageIndex.get(0))));

            LongBuffer signal = stack.longs(VulkanSemaphore.renderFinishedSemaphore);
            submitInfo.pSignalSemaphores(signal);

            if (vkQueueSubmit(VulkanLogicalDevices.graphicsQueue, submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
                throw new RuntimeException("Failed to submit draw call to command buffer.");
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(signal);

            LongBuffer swapChains = stack.longs(VulkanSwapChains.swapChain);

            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(swapChains);
            presentInfo.pImageIndices(imageIndex);
            presentInfo.pResults(null);

            vkQueuePresentKHR(VulkanLogicalDevices.presentQueue, presentInfo);
        }
    }

    private static void cleanup() {
        vkDestroySemaphore(VulkanLogicalDevices.device, VulkanSemaphore.renderFinishedSemaphore, null);
        vkDestroySemaphore(VulkanLogicalDevices.device, VulkanSemaphore.imageAvailableSemaphore, null);

        vkDestroyCommandPool(VulkanLogicalDevices.device, VulkanCommandPools.commandPool, null);

        for (long framebuffer : VulkanFramebuffers.swapChainFrameBuffers) {
            vkDestroyFramebuffer(VulkanLogicalDevices.device, framebuffer, null);
        }

        vkDestroyPipeline(VulkanLogicalDevices.device, VulkanGraphicsPipeline.graphicsPipeline, null);
        vkDestroyPipelineLayout(VulkanLogicalDevices.device, VulkanGraphicsPipeline.pipelineLayout, null);
        vkDestroyRenderPass(VulkanLogicalDevices.device, VulkanGraphicsPipeline.renderPass, null);

        for (long imageView : VulkanImageViews.swapChainImageViews) {
            vkDestroyImageView(VulkanLogicalDevices.device, imageView, null);
        }

        vkDestroySwapchainKHR(VulkanLogicalDevices.device, VulkanSwapChains.swapChain, null);
        vkDestroyDevice(VulkanLogicalDevices.device, null);

        if (ENABLE_VALIDATION_LAYERS) {
            VulkanDebug.destroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        }

        vkDestroySurfaceKHR(instance, surface, null);

        vkDestroyInstance(instance, null);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private static VkInstance createVulkanInstance() {
        if (ENABLE_VALIDATION_LAYERS && !VulkanValidationLayers.checkValidationLayerSupport()) {
            throw new RuntimeException("Validation Layers requested, but not available.");
        }


        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo applicationInfo = getVkApplicationInfo(stack);
            VkInstanceCreateInfo info = getVkInstanceCreateInfo(stack, applicationInfo);

            PointerBuffer instancePtr = stack.mallocPointer(1);

            int result = vkCreateInstance(info, null, instancePtr);

            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan Instance");
            }

            return new VkInstance(instancePtr.get(), info);
        }
    }

    private static VkInstanceCreateInfo getVkInstanceCreateInfo(MemoryStack stack, VkApplicationInfo applicationInfo) {
        VkInstanceCreateInfo info = VkInstanceCreateInfo.callocStack(stack);
        info.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
        info.pApplicationInfo(applicationInfo);
        info.ppEnabledExtensionNames(getRequiredExtensions());

        VulkanValidationLayers.addValidationLayers(stack, info);

        info.ppEnabledLayerNames(null);
        return info;
    }

    private static VkApplicationInfo getVkApplicationInfo(MemoryStack stack) {
        VkApplicationInfo applicationInfo = VkApplicationInfo.callocStack(stack);
        applicationInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
        applicationInfo.pApplicationName(stack.UTF8Safe("Hello Triangle"));
        applicationInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
        applicationInfo.pEngineName(stack.UTF8Safe("No Engine"));
        applicationInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
        applicationInfo.apiVersion(VK_API_VERSION_1_0);
        return applicationInfo;
    }

    private static PointerBuffer getRequiredExtensions() {
        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();
        PointerBuffer extensions = VulkanValidationLayers.getValidationLayers(glfwExtensions);
        if (extensions != null) return extensions;
        return glfwExtensions;
    }

    private static void initGLFW() {
        glfwInit();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
    }

    public static PointerBuffer asPointerBuffer(Collection<String> collection) {
        MemoryStack stack = MemoryStack.stackGet();
        PointerBuffer buffer = stack.mallocPointer(collection.size());
        collection.stream().map(stack::UTF8).forEach(buffer::put);

        return buffer.rewind();
    }
}
