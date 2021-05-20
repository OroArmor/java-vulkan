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

import java.nio.LongBuffer;

import com.oroarmor.vulkan.util.VulkanUtil;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

public class VulkanSurface implements AutoCloseable {
    protected final long surface;
    private final VulkanContext context;

    public VulkanSurface(VulkanContext context) {
        this.context = context;
        surface = createSurface();
    }

    protected long createSurface() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);
            VulkanUtil.checkVulkanResult(GLFWVulkan.glfwCreateWindowSurface(context.getInstance().getInstance(), context.getGLFWContext().getWindow(), null, pSurface), "Unable to create window surface");
            return pSurface.get(0);
        }
    }

    public long getSurface() {
        return surface;
    }

    public void close() {
        KHRSurface.vkDestroySurfaceKHR(context.getInstance().getInstance(), surface, null);
    }
}
