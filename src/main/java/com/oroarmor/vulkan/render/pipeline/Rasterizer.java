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

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public record Rasterizer(PolygonFillMode fillMode, float lineWidth, CullMode cullMode, FrontFace face,
                         boolean depthClampEnable, boolean depthBiasEnable, float depthConstantFactor, float depthClamp,
                         float depthSlopeFactor) {
    public Rasterizer(PolygonFillMode fillMode, CullMode cullMode, FrontFace face) {
        this(fillMode, 1.0f, cullMode, face, false, false, 0, 0, 0);
    }

    public VkPipelineRasterizationStateCreateInfo createRasterizer(MemoryStack stack) {
        VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.callocStack(stack);
        rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);

        rasterizer.depthClampEnable(depthClampEnable);
        rasterizer.rasterizerDiscardEnable(false);
        rasterizer.polygonMode(fillMode.vkPolygonMode);

        rasterizer.lineWidth(lineWidth);
        rasterizer.cullMode(cullMode.vkCullMode);
        rasterizer.frontFace(face.vkFrontFace);

        rasterizer.depthBiasEnable(depthBiasEnable);
        rasterizer.depthBiasConstantFactor(depthConstantFactor);
        rasterizer.depthBiasClamp(depthClamp);
        rasterizer.depthBiasSlopeFactor(depthSlopeFactor);

        return rasterizer;
    }

    public enum PolygonFillMode {
        FILL(VK_POLYGON_MODE_FILL), LINE(VK_POLYGON_MODE_LINE), POINT(VK_POLYGON_MODE_POINT);
        private final int vkPolygonMode;

        PolygonFillMode(int vkPolygonMode) {
            this.vkPolygonMode = vkPolygonMode;
        }
    }

    public enum CullMode {
        NONE(VK_CULL_MODE_NONE), FRONT_BIT(VK_CULL_MODE_FRONT_BIT), BACK_BIT(VK_CULL_MODE_BACK_BIT), FRONT_AND_BACK(VK_CULL_MODE_FRONT_AND_BACK);
        private final int vkCullMode;

        CullMode(int vkCullMode) {
            this.vkCullMode = vkCullMode;
        }
    }

    public enum FrontFace {
        COUNTER_CLOCKWISE(VK_FRONT_FACE_COUNTER_CLOCKWISE), CLOCKWISE(VK_FRONT_FACE_CLOCKWISE);
        private final int vkFrontFace;

        FrontFace(int vkFrontFace) {
            this.vkFrontFace = vkFrontFace;
        }
    }


    public static Rasterizer getDefaultRasterizer() {
        return new Rasterizer(PolygonFillMode.FILL, CullMode.BACK_BIT, FrontFace.COUNTER_CLOCKWISE);
    }
}
