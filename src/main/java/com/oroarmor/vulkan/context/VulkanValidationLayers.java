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

package com.oroarmor.vulkan.context;

import java.nio.IntBuffer;
import java.util.Set;
import java.util.stream.Collectors;

import com.oroarmor.vulkan.util.VulkanUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;

import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties;

public class VulkanValidationLayers {
    public static final Set<String> VALIDATION_LAYERS = Set.of("VK_LAYER_KHRONOS_validation");

    protected final VulkanContext context;

    public VulkanValidationLayers(VulkanContext context) {
        this.context = context;
    }

    public void addValidationLayers(VkInstanceCreateInfo info) {
        if (context.getDebug().isDebugEnabled()) {
            try(MemoryStack stack = MemoryStack.stackPush()) {
                info.ppEnabledLayerNames(VulkanUtil.asPointerBuffer(VALIDATION_LAYERS));
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack);
                context.getDebug().populateDebugMessengerCreateInfo(debugCreateInfo);
                info.pNext(debugCreateInfo.address());
            }
        }
    }

    public PointerBuffer getRequiredExtensions(PointerBuffer requiredGLFWExtensions) {
        if (context.getDebug().isDebugEnabled()) {
            MemoryStack stack = MemoryStack.stackGet();

            PointerBuffer extensions = stack.mallocPointer(requiredGLFWExtensions.capacity() + 1);

            extensions.put(requiredGLFWExtensions);
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

            return extensions.rewind();
        }
        return requiredGLFWExtensions;
    }

    public boolean checkValidationLayerSupport() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer layerCounts = stack.ints(0);
            vkEnumerateInstanceLayerProperties(layerCounts, null);
            VkLayerProperties.Buffer availableLayers = VkLayerProperties.mallocStack(layerCounts.get(0), stack);
            vkEnumerateInstanceLayerProperties(layerCounts, availableLayers);
            Set<String> availableLayerNames = availableLayers.stream().map(VkLayerProperties::layerNameString).collect(Collectors.toSet());
            return availableLayerNames.containsAll(VALIDATION_LAYERS);
        }
    }
}
