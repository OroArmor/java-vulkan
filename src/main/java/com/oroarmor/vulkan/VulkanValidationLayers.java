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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;

import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanValidationLayers {
    public static final Set<String> VALIDATION_LAYERS = new HashSet<>(Set.of("VK_LAYER_KHRONOS_validation"));

    static boolean checkValidationLayerSupport() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer layerCounts = stack.ints(0);
            vkEnumerateInstanceLayerProperties(layerCounts, null);
            VkLayerProperties.Buffer availableLayers = VkLayerProperties.mallocStack(layerCounts.get(0), stack);
            vkEnumerateInstanceLayerProperties(layerCounts, availableLayers);
            Set<String> availableLayerNames = availableLayers.stream().map(VkLayerProperties::layerNameString).collect(Collectors.toSet());
            return availableLayerNames.containsAll(VALIDATION_LAYERS);
        }
    }

    static void addValidationLayers(MemoryStack stack, VkInstanceCreateInfo info) {
        if (VulkanTests.ENABLE_VALIDATION_LAYERS) {
            info.ppEnabledLayerNames(VulkanTests.asPointerBuffer(VALIDATION_LAYERS));
            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack);
            VulkanDebug.populateDebugMessengerCreateInfo(debugCreateInfo);
            info.pNext(debugCreateInfo.address());
        }
    }

    static PointerBuffer getValidationLayers(PointerBuffer glfwExtensions) {
        if (VulkanTests.ENABLE_VALIDATION_LAYERS) {
            MemoryStack stack = MemoryStack.stackGet();

            PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);

            extensions.put(glfwExtensions);
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

            // Rewind the buffer before returning it to reset its position back to 0
            return extensions.rewind();
        }
        return null;
    }
}
