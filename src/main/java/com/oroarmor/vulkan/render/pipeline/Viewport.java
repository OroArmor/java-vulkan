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

package com.oroarmor.vulkan.render.pipeline;

import com.oroarmor.vulkan.render.VulkanRenderer;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkViewport;

public record Viewport(Vector2f position, VkExtent2D size, Vector2i depth) {
    public VkViewport.Buffer createVkViewport(MemoryStack stack) {
        return VkViewport.callocStack(1, stack)
                .x(position.x)
                .y(position.y)
                .width(size.width())
                .height(size.height())
                .minDepth(depth.x)
                .maxDepth(depth.y);
    }

    public static Viewport getDefaultViewport(VulkanRenderer renderer) {
        return new Viewport(new Vector2f(), renderer.getSwapChain().getSwapChainExtent(), new Vector2i(0, 1));
    }
}
