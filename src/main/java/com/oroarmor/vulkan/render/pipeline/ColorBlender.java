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

import java.nio.FloatBuffer;

import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public record ColorBlender(ColorComponent component, boolean enableBlend, BlendFactor sourceColorBlendFactor,
                           BlendMode colorBlendMode, BlendFactor sourceAlphaBlendFactor,
                           BlendFactor destinationAlphaBlendFactor, BlendMode alphaBlendMode, Vector4f blendConstants) {

    public VkPipelineColorBlendStateCreateInfo createColorBlendState(MemoryStack stack) {
        VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.callocStack(stack);
        colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
        colorBlending.logicOpEnable(false);
        colorBlending.logicOp(VK_LOGIC_OP_COPY);
        colorBlending.pAttachments(createColorBlendAttachment());
        colorBlending.flags();
        FloatBuffer pBlendConstants = stack.callocFloat(4);
        colorBlending.blendConstants(blendConstants.get(pBlendConstants));
        return colorBlending;
    }

    protected VkPipelineColorBlendAttachmentState.Buffer createColorBlendAttachment() {
        VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.create(1);
        colorBlendAttachment.colorWriteMask(component.vkColorComponent);
        colorBlendAttachment.blendEnable(enableBlend);
        colorBlendAttachment.srcColorBlendFactor(sourceColorBlendFactor.vkBlendFactor);
        colorBlendAttachment.colorBlendOp(colorBlendMode.vkBlendOp);
        colorBlendAttachment.srcAlphaBlendFactor(sourceAlphaBlendFactor.vkBlendFactor);
        colorBlendAttachment.dstAlphaBlendFactor(destinationAlphaBlendFactor.vkBlendFactor);
        colorBlendAttachment.alphaBlendOp(alphaBlendMode.vkBlendOp);
        return colorBlendAttachment;
    }

    public enum ColorComponent {
        RED(VK_COLOR_COMPONENT_R_BIT), GREEN(VK_COLOR_COMPONENT_G_BIT), BLUE(VK_COLOR_COMPONENT_B_BIT),
        ALPHA(VK_COLOR_COMPONENT_A_BIT),
        RGB(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT),
        RGBA(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);

        private final int vkColorComponent;

        ColorComponent(int vkColorComponent) {
            this.vkColorComponent = vkColorComponent;
        }
    }

    public enum BlendFactor {
        ZERO(VK_BLEND_FACTOR_ZERO), ONE(VK_BLEND_FACTOR_ONE), SRC_COLOR(VK_BLEND_FACTOR_SRC_COLOR),
        ONE_MINUS_SRC_COLOR(VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR), DST_COLOR(VK_BLEND_FACTOR_DST_COLOR),
        ONE_MINUS_DST_COLOR(VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR), SRC_ALPHA(VK_BLEND_FACTOR_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA), DST_ALPHA(VK_BLEND_FACTOR_DST_ALPHA),
        ONE_MINUS_DST_ALPHA(VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA), CONSTANT_COLOR(VK_BLEND_FACTOR_CONSTANT_COLOR),
        ONE_MINUS_CONSTANT_COLOR(VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR),
        CONSTANT_ALPHA(VK_BLEND_FACTOR_CONSTANT_ALPHA), ONE_MINUS_CONSTANT_ALPHA(VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA),
        SRC_ALPHA_SATURATE(VK_BLEND_FACTOR_SRC_ALPHA_SATURATE), SRC1_COLOR(VK_BLEND_FACTOR_SRC1_COLOR),
        ONE_MINUS_SRC1_COLOR(VK_BLEND_FACTOR_ONE_MINUS_SRC1_COLOR), SRC1_ALPHA(VK_BLEND_FACTOR_SRC1_ALPHA),
        ONE_MINUS_SRC1_ALPHA(VK_BLEND_FACTOR_ONE_MINUS_SRC1_ALPHA);

        private final int vkBlendFactor;

        BlendFactor(int vkBlendFactor) {
            this.vkBlendFactor = vkBlendFactor;
        }
    }

    public enum BlendMode {
        ADD(VK_BLEND_OP_ADD), SUBTRACT(VK_BLEND_OP_SUBTRACT), REVERSE_SUBTRACT(VK_BLEND_OP_REVERSE_SUBTRACT),
        MIN(VK_BLEND_OP_MIN), MAX(VK_BLEND_OP_MAX);

        private final int vkBlendOp;

        BlendMode(int vkBlendOp) {
            this.vkBlendOp = vkBlendOp;
        }
    }

    public static ColorBlender getDefaultColorBlender() {
        return new ColorBlender(ColorComponent.RGBA, true, BlendFactor.SRC_ALPHA, BlendMode.ADD,
                BlendFactor.ONE, BlendFactor.ZERO, BlendMode.ADD, new Vector4f(0, 0, 0, 0));
    }
}
