package com.oroarmor.vulkan;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK10.*;

public class Vertex {
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
}
