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

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Objects;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanGraphicsPipeline {
    public static long renderPass;
    public static long pipelineLayout;
    public static long graphicsPipeline;

    public static void createRenderPass() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer colorAttachment = createColorAttachment(stack);
            VkAttachmentReference.Buffer colorAttachmentRef = createColorAttachmentRef(stack);

            VkSubpassDescription.Buffer subpass = createSubpass(colorAttachmentRef, stack);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(colorAttachment);
            renderPassInfo.pSubpasses(subpass);

            VkSubpassDependency.Buffer dependency = getSubpassDependency(stack);
            renderPassInfo.pDependencies(dependency);

            LongBuffer pRenderPass = stack.mallocLong(1);

            if (vkCreateRenderPass(VulkanLogicalDevices.device, renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("failed to create render pass!");
            }

            renderPass = pRenderPass.get(0);
        }
    }

    public static VkSubpassDependency.Buffer getSubpassDependency(MemoryStack stack) {
        VkSubpassDependency.Buffer dependency = VkSubpassDependency.callocStack(1, stack);
        dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
        dependency.dstSubpass(0);
        dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
        dependency.srcAccessMask(0);
        dependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
        dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_COLOR_ATTACHMENT_READ_BIT);
        return dependency;
    }

    public static VkSubpassDescription.Buffer createSubpass(VkAttachmentReference.Buffer colorAttachmentRef, MemoryStack stack) {
        VkSubpassDescription.Buffer subpass = VkSubpassDescription.callocStack(1, stack);
        subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
        subpass.colorAttachmentCount(1);
        subpass.pColorAttachments(colorAttachmentRef);
        return subpass;
    }

    public static VkAttachmentReference.Buffer createColorAttachmentRef(MemoryStack stack) {
        VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.callocStack(1, stack);
        colorAttachmentRef.attachment(0);
        colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        return colorAttachmentRef;
    }

    public static VkAttachmentDescription.Buffer createColorAttachment(MemoryStack stack) {
        VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.callocStack(1, stack);
        colorAttachment.format(VulkanSwapChains.swapChainImageFormat);
        colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
        colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
        colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
        colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
        colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
        colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        return colorAttachment;
    }

    public static void createGraphicsPipeline() {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ShaderUtils.SPIRV vertexShaderCode = ShaderUtils.compileShaderFile("com/oroarmor/app/vulkan/vulkan_shader.vert", ShaderUtils.ShaderType.VERTEX_SHADER);
            ShaderUtils.SPIRV fragmentShaderCode = ShaderUtils.compileShaderFile("com/oroarmor/app/vulkan/vulkan_shader.frag", ShaderUtils.ShaderType.FRAGMENT_SHADER);

            long vertexModule = createShaderModule(vertexShaderCode.bytecode());
            long fragmentModule = createShaderModule(fragmentShaderCode.bytecode());

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.callocStack(2, stack);
            getVkPipelineShaderStageCreateInfo(stack, vertexModule, VK_SHADER_STAGE_VERTEX_BIT, "main", shaderStages.get(0));
            getVkPipelineShaderStageCreateInfo(stack, fragmentModule, VK_SHADER_STAGE_FRAGMENT_BIT, "main", shaderStages.get(1));

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = createVertexInputInfo(stack);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = createInputAssembly(stack);

            VkViewport.Buffer viewport = createVkViewport(stack);
            VkRect2D.Buffer scissor = createVkRect2D(stack);

            VkPipelineViewportStateCreateInfo viewportState = createViewportState(stack, viewport, scissor);
            VkPipelineRasterizationStateCreateInfo rasterizer = createRasterizer(stack);
            VkPipelineMultisampleStateCreateInfo multisampling = createMultisampling(stack);
            VkPipelineColorBlendStateCreateInfo colorBlending = createColorBlending(stack);

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.callocStack(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(VulkanDescriptorSets.descriptorSetLayout));

            LongBuffer pLayoutInfo = stack.longs(VK_NULL_HANDLE);

            if (vkCreatePipelineLayout(VulkanLogicalDevices.device, pipelineLayoutInfo, null, pLayoutInfo) != VK_SUCCESS) {
                throw new RuntimeException("Unable to create pipeline layout.");
            }

            pipelineLayout = pLayoutInfo.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = createGraphicsPipelineInfo(stack, shaderStages, vertexInputInfo, inputAssembly, viewportState, rasterizer, multisampling, colorBlending);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if (vkCreateGraphicsPipelines(VulkanLogicalDevices.device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline!");
            }

            graphicsPipeline = pGraphicsPipeline.get(0);

            vkDestroyShaderModule(VulkanLogicalDevices.device, fragmentModule, null);
            vkDestroyShaderModule(VulkanLogicalDevices.device, vertexModule, null);

            vertexShaderCode.free();
            fragmentShaderCode.free();
        }
    }

    private static VkGraphicsPipelineCreateInfo.Buffer createGraphicsPipelineInfo(MemoryStack stack, VkPipelineShaderStageCreateInfo.Buffer shaderStages, VkPipelineVertexInputStateCreateInfo vertexInputInfo, VkPipelineInputAssemblyStateCreateInfo inputAssembly, VkPipelineViewportStateCreateInfo viewportState, VkPipelineRasterizationStateCreateInfo rasterizer, VkPipelineMultisampleStateCreateInfo multisampling, VkPipelineColorBlendStateCreateInfo colorBlending) {
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
        pipelineInfo.renderPass(renderPass);
        pipelineInfo.subpass(0);
        pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
        pipelineInfo.basePipelineIndex(-1);
        return pipelineInfo;
    }

    public static VkPipelineColorBlendStateCreateInfo createColorBlending(MemoryStack stack) {
        VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.callocStack(stack);
        colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
        colorBlending.logicOpEnable(false);
        colorBlending.logicOp(VK_LOGIC_OP_COPY);
        colorBlending.pAttachments(createColorBlendAttachment());
        colorBlending.blendConstants(stack.floats(0, 0, 0, 0));
        return colorBlending;
    }

    public static VkPipelineColorBlendAttachmentState.Buffer createColorBlendAttachment() {
        VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.create(1);
        colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
        colorBlendAttachment.blendEnable(false);
        colorBlendAttachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
        colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD);
        colorBlendAttachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
        colorBlendAttachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
        colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD);
        return colorBlendAttachment;
    }

    public static VkPipelineMultisampleStateCreateInfo createMultisampling(MemoryStack stack) {
        VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.callocStack(stack);

        multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
        multisampling.sampleShadingEnable(false);
        multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

        return multisampling;
    }

    public static VkPipelineViewportStateCreateInfo createViewportState(MemoryStack stack, VkViewport.Buffer viewport, VkRect2D.Buffer scissor) {
        VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.callocStack(stack);
        viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
        viewportState.pViewports(viewport);
        viewportState.pScissors(scissor);
        return viewportState;
    }

    public static VkRect2D.Buffer createVkRect2D(MemoryStack stack) {
        VkRect2D.Buffer scissor = VkRect2D.create(1);
        scissor.offset(VkOffset2D.callocStack(stack).set(0, 0))
                .extent(VulkanSwapChains.swapChainExtent);
        return scissor;
    }

    public static VkViewport.Buffer createVkViewport(MemoryStack stack) {
        VkViewport.Buffer viewport = VkViewport.callocStack(1, stack);
        viewport.x(0.0f)
                .y(0.0f)
                .width(VulkanSwapChains.swapChainExtent.width())
                .height(VulkanSwapChains.swapChainExtent.height())
                .minDepth(0)
                .maxDepth(1);
        return viewport;
    }

    public static VkPipelineInputAssemblyStateCreateInfo createInputAssembly(MemoryStack stack) {
        VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.callocStack(stack);
        inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
        inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
        inputAssembly.primitiveRestartEnable(false);
        return inputAssembly;
    }

    public static VkPipelineVertexInputStateCreateInfo createVertexInputInfo(MemoryStack stack) {
        VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.callocStack(stack);
        vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);

        vertexInputInfo.pVertexBindingDescriptions(Vertex.getBindingDescription());
        vertexInputInfo.pVertexAttributeDescriptions(Vertex.getAttributeDescriptions());

        return vertexInputInfo;
    }

    public static VkPipelineRasterizationStateCreateInfo createRasterizer(MemoryStack stack) {
        VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.callocStack(stack);
        rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
        rasterizer.depthClampEnable(false);
        rasterizer.rasterizerDiscardEnable(false);
        rasterizer.polygonMode(VK_POLYGON_MODE_FILL);

        rasterizer.lineWidth(1);
        rasterizer.cullMode(VK_CULL_MODE_NONE);
        rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);

        rasterizer.depthBiasEnable(false);
        return rasterizer;
    }

    public static void getVkPipelineShaderStageCreateInfo(MemoryStack stack, long module, int vkShaderStageFragmentBit, String entrypoint, VkPipelineShaderStageCreateInfo shaderStageInfo) {
        shaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
        shaderStageInfo.stage(vkShaderStageFragmentBit);
        shaderStageInfo.module(module);
        shaderStageInfo.pName(Objects.requireNonNull(stack.UTF8Safe(entrypoint)));
    }

    public static long createShaderModule(ByteBuffer code) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.callocStack(stack);
            shaderModuleCreateInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            shaderModuleCreateInfo.pCode(code);

            LongBuffer pShader = stack.mallocLong(1);
            if (vkCreateShaderModule(VulkanLogicalDevices.device, shaderModuleCreateInfo, null, pShader) != VK_SUCCESS) {
                throw new RuntimeException("Could not create shader!");
            }
            return pShader.get(0);
        }
    }
}
