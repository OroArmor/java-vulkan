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

import com.oroarmor.vulkan.context.VulkanContext;
import com.oroarmor.vulkan.render.Shader;
import com.oroarmor.vulkan.render.VulkanRenderer;

public class VulkanGraphicsPipeline {
    protected final VulkanContext context;
    protected final VulkanRenderer renderer;

    protected Shader shader;
    protected VertexInputInfo vertexInputInfo;
    protected InputAssembly inputAssembly;
    protected Viewport viewport;
    protected Scissor scissor;
    protected ViewportState viewportState;
    protected Rasterizer rasterizer;
    protected Multisampler multisampler;
    protected ColorBlender colorBlender = ColorBlender.getDefaultColorBlender();
    protected PipelineLayout pipelineLayout;

    protected boolean changed = false;
    protected long graphicsPipeline;

    public VulkanGraphicsPipeline(VulkanContext context, VulkanRenderer renderer) {
        this.context = context;
        this.renderer = renderer;
    }

    public void rebuildIfNeeded() {
        if(changed) {
            graphicsPipeline = createGraphicsPipeline();
        }
    }

    protected long createGraphicsPipeline() {
        return 0;
    }
}
