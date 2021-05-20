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
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.oroarmor.profiler.Profile;
import com.oroarmor.vulkan.context.VulkanContext;
import com.oroarmor.vulkan.glfw.GLFWContext;
import com.oroarmor.vulkan.render.*;
import com.oroarmor.vulkan.render.BufferLayout.BufferElement.CommonBufferElement;
import com.oroarmor.profiler.Profiler;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanApplication implements AutoCloseable {
    protected final GLFWContext glfwContext;
    protected final VulkanContext vulkanContext;
    protected final VulkanRenderer vulkanRenderer;

    public static final float HEXAGON_RADIUS = 1f;
    public static final float HALF_RADIUS = HEXAGON_RADIUS / 2f;
    public static final float HEXAGON_HEIGHT = ((float) Math.sqrt(3) / 2f) * HEXAGON_RADIUS;
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

        VulkanBuffer vertexBuffer = new VulkanBuffer(vulkanContext, Vertex.LAYOUT, Arrays.asList(VERTICES), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
        VulkanBuffer vertexBuffer2 = new VulkanBuffer(vulkanContext, Vertex.LAYOUT, Arrays.stream(VERTICES).peek(vertex -> vertex.pos.mul(0.5f)).collect(Collectors.toList()), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
        VulkanBuffer indexBuffer = new VulkanBuffer(vulkanContext, new BufferLayout().push(new BufferLayout.BufferElement(1, CommonBufferElement.INTEGER, false)), intToIndex(INDICES), VK_BUFFER_USAGE_INDEX_BUFFER_BIT);

        Shader shader = new Shader(vulkanContext, vulkanRenderer, "com/oroarmor/vulkan/vulkan_shader.glsl", new Shader.VertexInputDescriptor(Vertex.LAYOUT));

        Consumer<VulkanRenderer> step1 = VulkanRenderer.renderIndexedWithShader(shader, vertexBuffer, indexBuffer);
        Consumer<VulkanRenderer> step2 = VulkanRenderer.renderIndexedWithShader(shader, vertexBuffer2, indexBuffer);

        Profiler.PROFILER.push("Complete window loop");
        while (!glfwContext.shouldClose()) {
            Profiler.PROFILER.profile(GLFW::glfwPollEvents, "Poll Events");
            vulkanRenderer.addRenderStep(step1);
            vulkanRenderer.addRenderStep(step2);
            vulkanRenderer.render();
            Profiler.PROFILER.pop();
            Profiler.PROFILER.push("Complete window loop");
        }
        Profiler.PROFILER.pop();

        System.out.println(Profiler.PROFILER.dump());

        vertexBuffer.close();
        indexBuffer.close();
        shader.close();
    }

    private static List<CopyableMemory> intToIndex(int[] arr) {
        return Arrays.stream(arr).boxed().map(CopyableMemory.IndexBufferMemory::new).collect(Collectors.toList());
    }

    @Override
    public void close() {
        glfwContext.close();
        vulkanContext.close();
    }

    public static class Vertex implements CopyableMemory {
        public static final int SIZEOF = (2 + 3) * Float.BYTES;

        public static BufferLayout LAYOUT = new BufferLayout().push(new BufferLayout.BufferElement(1, CommonBufferElement.VECTOR_2F, false)).push(new BufferLayout.BufferElement(1, CommonBufferElement.VECTOR_3F, false));

        static {
            assert LAYOUT.getStride() == SIZEOF : "Size does not match BufferLayout";
        }

        public Vector2f pos;
        public Vector3f color;

        public Vertex(Vector2f pos, Vector3f color) {
            this.pos = pos;
            this.color = color;
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
