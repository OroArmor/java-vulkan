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

import java.nio.LongBuffer;

import com.oroarmor.vulkan.context.VulkanContext;
import com.oroarmor.vulkan.render.Shader;
import com.oroarmor.vulkan.render.VulkanRenderer;
import com.oroarmor.vulkan.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanGraphicsPipeline implements AutoCloseable {
    protected final VulkanContext context;
    protected final VulkanRenderer renderer;

    protected Shader shader;
    protected InputAssembly inputAssembly = InputAssembly.getDefaultInputAssembly();
    protected Viewport viewport;
    protected Scissor scissor;
    protected Rasterizer rasterizer = Rasterizer.getDefaultRasterizer();
    protected MultiSampler multiSampler = MultiSampler.getDefaultMultiSampler();
    protected ColorBlender colorBlender = ColorBlender.getDefaultColorBlender();

    protected boolean changed = false;
    protected long graphicsPipeline;
    protected long pipelineLayout;

    public VulkanGraphicsPipeline(VulkanContext context, VulkanRenderer renderer) {
        this.context = context;
        this.renderer = renderer;
        scissor = Scissor.getDefaultScissor(this.renderer);
        viewport = Viewport.getDefaultViewport(this.renderer);
    }

    public void rebuildIfNeeded() {
        if (changed) {
            pipelineLayout = createPipelineLayout();
            graphicsPipeline = createGraphicsPipeline();
            changed = false;
        }
    }

    protected long createGraphicsPipeline() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineShaderStageCreateInfo.Buffer shaderStages = shader.createShaderStages(stack);

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = this.shader.getVertexInput().createVertexInputState(stack);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = this.inputAssembly.createInputAssembly(stack);

            VkViewport.Buffer viewport = this.viewport.createVkViewport(stack);
            VkRect2D.Buffer scissor = this.scissor.createVkRect2D(stack);

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.callocStack(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.pViewports(viewport);
            viewportState.pScissors(scissor);

            VkPipelineRasterizationStateCreateInfo rasterizer = this.rasterizer.createRasterizer(stack);
            VkPipelineMultisampleStateCreateInfo multisampling = this.multiSampler.createMultisampler(stack);
            VkPipelineColorBlendStateCreateInfo colorBlending = this.colorBlender.createColorBlendState(stack);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = createGraphicsPipelineInfo(stack, shaderStages, vertexInputInfo, inputAssembly, viewportState, rasterizer, multisampling, colorBlending);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            VulkanUtil.checkVulkanResult(vkCreateGraphicsPipelines(context.getLogicalDevice().getDevice(), VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline), "Failed to create graphics pipeline");

            return pGraphicsPipeline.get(0);
        }
    }

    private long createPipelineLayout() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.callocStack(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
//        pipelineLayoutInfo.pSetLayouts(stack.longs(VulkanDescriptorSets.descriptorSetLayout));

            LongBuffer pLayoutInfo = stack.longs(VK_NULL_HANDLE);

            VulkanUtil.checkVulkanResult(vkCreatePipelineLayout(context.getLogicalDevice().getDevice(), pipelineLayoutInfo, null, pLayoutInfo), "Unable to create pipeline layout");

            return pLayoutInfo.get(0);
        }
    }

    protected VkGraphicsPipelineCreateInfo.Buffer createGraphicsPipelineInfo(MemoryStack stack, VkPipelineShaderStageCreateInfo.Buffer shaderStages, VkPipelineVertexInputStateCreateInfo vertexInputInfo, VkPipelineInputAssemblyStateCreateInfo inputAssembly, VkPipelineViewportStateCreateInfo viewportState, VkPipelineRasterizationStateCreateInfo rasterizer, VkPipelineMultisampleStateCreateInfo multisampling, VkPipelineColorBlendStateCreateInfo colorBlending) {
        VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.callocStack(1, stack);

        pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
        pipelineInfo.pStages(shaderStages);

        pipelineInfo.pVertexInputState(vertexInputInfo);
        pipelineInfo.pInputAssemblyState(inputAssembly);
        pipelineInfo.pViewportState(viewportState);
        pipelineInfo.pRasterizationState(rasterizer);
        pipelineInfo.pMultisampleState(multisampling);
        pipelineInfo.pColorBlendState(colorBlending);

        pipelineInfo.layout(pipelineLayout);
        pipelineInfo.renderPass(this.renderer.getRenderPass().getRenderPass());
        pipelineInfo.subpass(0);
        pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
        pipelineInfo.basePipelineIndex(-1);
        return pipelineInfo;
    }

    public void close() {
        vkDestroyPipeline(context.getLogicalDevice().getDevice(), graphicsPipeline, null);
        vkDestroyPipelineLayout(context.getLogicalDevice().getDevice(), pipelineLayout, null);
    }

    public long getPipeline() {
        return graphicsPipeline;
    }

    public long getPipelineLayout() {
        return pipelineLayout;
    }

    public void setShader(Shader shader) {
        if (this.shader == shader) {
            return;
        }
        this.shader = shader;
        changed = true;
    }

    public void setInputAssembly(InputAssembly inputAssembly) {
        this.inputAssembly = inputAssembly;
        changed = true;
    }

    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
        changed = true;
    }

    public void setScissor(Scissor scissor) {
        this.scissor = scissor;
        changed = true;
    }

    public void setRasterizer(Rasterizer rasterizer) {
        this.rasterizer = rasterizer;
        changed = true;
    }

    public void setMultiSampler(MultiSampler multiSampler) {
        this.multiSampler = multiSampler;
        changed = true;
    }

    public void setColorBlender(ColorBlender colorBlender) {
        this.colorBlender = colorBlender;
        changed = true;
    }
}
