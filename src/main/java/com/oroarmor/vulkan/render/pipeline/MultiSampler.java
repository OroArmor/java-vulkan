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
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public record MultiSampler(SampleCount count) {
    public VkPipelineMultisampleStateCreateInfo createMultisampler(MemoryStack stack) {
        VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.callocStack(stack);
        multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
        multisampling.sampleShadingEnable(false);
        multisampling.rasterizationSamples(count.vkSampleCountBit);
        return multisampling;
    }

    public enum SampleCount {
        COUNT_1_BIT(VK_SAMPLE_COUNT_1_BIT), COUNT_2_BIT(VK_SAMPLE_COUNT_2_BIT), COUNT_4_BIT(VK_SAMPLE_COUNT_4_BIT),
        COUNT_8_BIT(VK_SAMPLE_COUNT_8_BIT), COUNT_16_BIT(VK_SAMPLE_COUNT_16_BIT), COUNT_32_BIT(VK_SAMPLE_COUNT_32_BIT),
        COUNT_64_BIT(VK_SAMPLE_COUNT_64_BIT);

        private final int vkSampleCountBit;

        SampleCount(int vkSampleCountBit) {
            this.vkSampleCountBit = vkSampleCountBit;
        }
    }

    public static MultiSampler getDefaultMultiSampler() {
        return new MultiSampler(SampleCount.COUNT_1_BIT);
    }
}
