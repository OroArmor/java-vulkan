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
import java.util.Arrays;
import java.util.stream.Collectors;

import com.oroarmor.vulkan.render.*;
import com.oroarmor.vulkan.render.BufferLayout.BufferElement.CommonBufferElement;
import com.oroarmor.vulkan.context.VulkanContext;
import com.oroarmor.vulkan.glfw.GLFWContext;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanApplication implements AutoCloseable {
    protected final GLFWContext glfwContext;
    protected final VulkanContext vulkanContext;
    protected final VulkanRenderer vulkanRenderer;

    public static final float HEXAGON_RADIUS = 1;
    public static final float HALF_RADIUS = HEXAGON_RADIUS / 2;
    public static final float HEXAGON_HEIGHT = ((float) Math.sqrt(3) / 2) * HEXAGON_RADIUS;
    public static final Vertex[] VERTICES = {
            new Vertex(new Vector2f(0, 0), new Vector3f(1.0f, 1.0f, 1.0f)),
            new Vertex(new Vector2f(-HEXAGON_RADIUS, 0), new Vector3f(1.0f, 0.0f, 0.0f)),
            new Vertex(new Vector2f(-HALF_RADIUS, HEXAGON_HEIGHT), new Vector3f(1.0f, 1.0f, 0.0f)),
            new Vertex(new Vector2f(HALF_RADIUS, HEXAGON_HEIGHT), new Vector3f(0.0f, 1.0f, 0.0f)),
            new Vertex(new Vector2f(HEXAGON_RADIUS, 0), new Vector3f(0.0f, 1.0f, 1.0f)),
            new Vertex(new Vector2f(HALF_RADIUS, -HEXAGON_HEIGHT), new Vector3f(0.0f, -0.0f, 1.0f)),
            new Vertex(new Vector2f(-HALF_RADIUS, -HEXAGON_HEIGHT), new Vector3f(1.0f, 0.0f, 1.0f))
    };

    public static final int[] INDICES = {0, 1, 2, 0, 2, 3, 0, 3, 4, 0, 4, 5, 0, 5, 6, 0, 6, 1};

    public VulkanApplication() {
        glfwContext = new GLFWContext(800, 600, "Hello Vulkan");
        vulkanContext = new VulkanContext(glfwContext);
        vulkanRenderer = new VulkanRenderer(vulkanContext, glfwContext);
    }

    public void run() {
        glfwContext.addKeyCallback((window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        VulkanBuffer vertexBuffer = new VulkanBuffer(vulkanContext, new BufferLayout().push(new BufferLayout.BufferElement(1, CommonBufferElement.VECTOR_2F, false)).push(new BufferLayout.BufferElement(1, CommonBufferElement.VECTOR_3F, false)), Arrays.asList(VERTICES), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
        VulkanBuffer indexBuffer = new VulkanBuffer(vulkanContext, new BufferLayout().push(new BufferLayout.BufferElement(1, CommonBufferElement.INTEGER, false)), Arrays.stream(INDICES).boxed().map(Integer::shortValue).map(CopyableMemory.CopyableShort::new).collect(Collectors.toList()), VK_BUFFER_USAGE_INDEX_BUFFER_BIT);

        Shader shader = new Shader(vulkanContext, vulkanRenderer, "com/oroarmor/vulkan/vulkan_shader.glsl");

        while (!glfwContext.shouldClose()) {
            glfwPollEvents();
            vulkanRenderer.render();
        }

        vertexBuffer.close();
        indexBuffer.close();
        shader.close();
    }

    @Override
    public void close() {
        glfwContext.close();
        vulkanContext.close();
    }

    public static class Vertex implements CopyableMemory {
        public static final int SIZEOF = (2 + 3) * Float.BYTES;
        public static final int OFFSET_POS = 0;
        public static final int OFFSET_COLOR = 2 * Float.BYTES;

        public Vector2f pos;
        public Vector3f color;

        public Vertex(Vector2f pos, Vector3f color) {
            this.pos = pos;
            this.color = color;
        }

        public static VkVertexInputBindingDescription.Buffer getBindingDescription() {
            VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.callocStack(1);

            bindingDescription.binding(0);
            bindingDescription.stride(Vertex.SIZEOF);
            bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

            return bindingDescription;
        }

        public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions() {
            VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.create(2);

            VkVertexInputAttributeDescription positionAttribute = attributeDescriptions.get(0);
            positionAttribute.binding(0).location(0);
            positionAttribute.format(VK_FORMAT_R32G32_SFLOAT);
            positionAttribute.offset(OFFSET_POS);

            VkVertexInputAttributeDescription colorAttribute = attributeDescriptions.get(1);
            colorAttribute.binding(0).location(1);
            colorAttribute.format(VK_FORMAT_R32G32B32_SFLOAT);
            colorAttribute.offset(OFFSET_COLOR);

            return attributeDescriptions.rewind();
        }

        @Override
        public void memCopy(ByteBuffer buffer) {
            buffer.putFloat(pos.x());
            buffer.putFloat(pos.y());

            buffer.putFloat(color.x());
            buffer.putFloat(color.y());
            buffer.putFloat(color.z());
        }

        @Override
        public int sizeof() {
            return SIZEOF;
        }
    }
}
