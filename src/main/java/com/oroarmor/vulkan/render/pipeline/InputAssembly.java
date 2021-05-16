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
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public record InputAssembly(TopologyType type, boolean primitiveRestart) {
    public InputAssembly(TopologyType type) {
        this(type, false);
    }

    public VkPipelineInputAssemblyStateCreateInfo createInputAssembly(MemoryStack stack) {
        VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.callocStack(stack);
        inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
        inputAssembly.topology(type.vkPrimitiveTopology);
        inputAssembly.primitiveRestartEnable(primitiveRestart);
        return inputAssembly;
    }

    public enum TopologyType {
        POINT_LIST(VK_PRIMITIVE_TOPOLOGY_POINT_LIST),
        LINE_LIST(VK_PRIMITIVE_TOPOLOGY_LINE_LIST), LINE_STRIP(VK_PRIMITIVE_TOPOLOGY_LINE_STRIP),
        TRIANGLE_LIST(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST), TRIANGLE_STRIP(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP), TRIANGLE_FAN(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN),
        LINE_LIST_WITH_ADJACENCY(VK_PRIMITIVE_TOPOLOGY_LINE_LIST_WITH_ADJACENCY), LINE_STRIP_WITH_ADJACENCY(VK_PRIMITIVE_TOPOLOGY_LINE_STRIP_WITH_ADJACENCY),
        TRIANGLE_LIST_WITH_ADJACENCY(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST_WITH_ADJACENCY), TRIANGLE_STRIP_WITH_ADJACENCY(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP_WITH_ADJACENCY),
        PATCH_LIST(VK_PRIMITIVE_TOPOLOGY_PATCH_LIST);

        private final int vkPrimitiveTopology;

        TopologyType(int vkPrimitiveTopology) {
            this.vkPrimitiveTopology = vkPrimitiveTopology;
        }
    }

    public static InputAssembly getDefaultInputAssembly() {
        return new InputAssembly(TopologyType.TRIANGLE_LIST);
    }
}
