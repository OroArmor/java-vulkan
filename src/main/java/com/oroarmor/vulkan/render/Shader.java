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

package com.oroarmor.vulkan.render;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oroarmor.vulkan.VulkanUtil;
import com.oroarmor.vulkan.context.VulkanContext;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;

import static java.lang.ClassLoader.getSystemClassLoader;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

public class Shader implements AutoCloseable {
    protected static final long compiler;

    static {
        compiler = shaderc_compiler_initialize();

        if (compiler == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create shader compiler");
        }
    }

    protected final VulkanContext context;
    protected final VulkanRenderer renderer;

    protected final String shaderFile;
    protected final VertexInput inputTemplate;
    protected final Map<Stage, String> stageToSource;
    protected final Map<Stage, SPIRV> stageToCompiled;
    protected final Map<Stage, Long> stageToModule;

    public Shader(VulkanContext context, VulkanRenderer renderer, String shaderFile, VertexInput inputTemplate) {
        this.context = context;
        this.renderer = renderer;
        this.shaderFile = shaderFile;
        this.inputTemplate = inputTemplate;
        stageToSource = new HashMap<>();
        stageToCompiled = new HashMap<>();
        stageToModule = new HashMap<>();
        this.recompile();
    }

    protected void recompile() {
        stageToSource.clear();
        parseSourceFile();
        stageToCompiled.clear();
        compileStages();
        stageToModule.clear();
        convertToModules();
    }

    protected void convertToModules() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            stageToCompiled.forEach((stage, spirv) -> {
                VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.callocStack(stack);
                shaderModuleCreateInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
                shaderModuleCreateInfo.pCode(spirv.bytecode);

                LongBuffer pShader = stack.mallocLong(1);
                VulkanUtil.checkVulkanResult(vkCreateShaderModule(context.getLogicalDevice().getDevice(), shaderModuleCreateInfo, null, pShader), "Could not create shader");
                stageToModule.put(stage, pShader.get(0));
            });
        }
    }

    protected void compileStages() {
        stageToSource.forEach((stage, source) -> {
            long result = shaderc_compile_into_spv(compiler, source, stage.getShaderc_kind(), shaderFile, "main", MemoryUtil.NULL);

            if (result == MemoryUtil.NULL) {
                throw new RuntimeException("Failed to compile shader " + shaderFile + " into SPIR-V");
            }

            if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
                throw new RuntimeException("Failed to compile shader " + shaderFile + " into SPIR-V:\n\t" + shaderc_result_get_error_message(result));
            }

            stageToCompiled.put(stage, new SPIRV(result, shaderc_result_get_bytes(result)));
        });
    }

    protected void parseSourceFile() {
        String source = getSource();
        while (source.contains("#stage")) {
            int startStage = source.indexOf("#stage");
            int endStage = source.indexOf("#stage", startStage + 1) - 1;
            if (endStage < 0) {
                endStage = source.length();
            }
            String stageSource = source.substring(startStage, endStage);

            Pattern pattern = Pattern.compile("#stage (\\w*)");
            Matcher matcher = pattern.matcher(stageSource);
            if (!matcher.find()) {
                throw new RuntimeException("Unable to find stage from shader section\n" + stageSource);
            }
            String stageType = matcher.group(1);
            Stage stage = Stage.valueOf(stageType);
            stageToSource.put(stage, stageSource.replace(matcher.group(), ""));
            source = source.substring(endStage);
        }
    }

    protected String getSource() {
        try {
            byte[] bytes = Objects.requireNonNull(Shader.class.getClassLoader().getResourceAsStream(shaderFile)).readAllBytes(); //Files.readAllBytes(path);
            return new String(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Unable to find shader file " + shaderFile, e);
        }
    }

    public Map<Stage, Long> getStageToModule() {
        return stageToModule;
    }

    public VkPipelineShaderStageCreateInfo.Buffer createShaderStages(MemoryStack stack) {
        VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.callocStack(stageToModule.size(), stack);
        Iterator<Map.Entry<Stage, Long>> iterator = stageToModule.entrySet().iterator();
        for (int i = 0; i < stageToModule.size(); i++) {
            Map.Entry<Stage, Long> entry = iterator.next();
            getVkPipelineShaderStageCreateInfo(entry.getValue(), entry.getKey(), shaderStages.get(i), stack.UTF8Safe("main"));
        }
        return shaderStages;
    }

    public VertexInput getVertexInput() {
        return inputTemplate;
    }

    protected void getVkPipelineShaderStageCreateInfo(long module, Stage stage, VkPipelineShaderStageCreateInfo shaderStageInfo, ByteBuffer entrypoint) {
        shaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
        shaderStageInfo.stage(stage.getVulkanShaderStage());
        shaderStageInfo.module(module);
        shaderStageInfo.pName(entrypoint);
    }

    @Override
    public void close() {
        stageToCompiled.values().forEach(SPIRV::free);
        stageToModule.values().forEach(l -> vkDestroyShaderModule(context.getLogicalDevice().getDevice(), l, null));
    }

    public enum Stage {
        VERTEX_SHADER(shaderc_glsl_vertex_shader, VK_SHADER_STAGE_VERTEX_BIT),
        GEOMETRY_SHADER(shaderc_glsl_geometry_shader, VK_SHADER_STAGE_GEOMETRY_BIT),
        FRAGMENT_SHADER(shaderc_glsl_fragment_shader, VK_SHADER_STAGE_FRAGMENT_BIT);

        private final int shaderc_kind;
        private final int vulkanShaderStage;

        Stage(int kind, int vulkanShaderStage) {
            this.shaderc_kind = kind;
            this.vulkanShaderStage = vulkanShaderStage;
        }

        public int getShaderc_kind() {
            return shaderc_kind;
        }

        public int getVulkanShaderStage() {
            return vulkanShaderStage;
        }
    }

    protected static record SPIRV(long handle, ByteBuffer bytecode) implements NativeResource {
        @Override
        public void free() {
            shaderc_result_release(handle);
        }
    }

    public interface VertexInput extends CopyableMemory {
        default VkVertexInputBindingDescription.Buffer getBindingDescription() {
            VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.callocStack(1);
            bindingDescription.binding(0);
            bindingDescription.stride(this.sizeof());
            bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
            return bindingDescription;
        }

        default VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(){
            List<BufferLayout.BufferElement> elements = getLayout().getBufferElements();
            VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.create(elements.size());

            int i = 0;
            for(BufferLayout.BufferElement element : elements) {
                VkVertexInputAttributeDescription positionAttribute = attributeDescriptions.get(i);
                positionAttribute.binding(0).location(i++);
                positionAttribute.format(getFormat(element.size().getSize()));
                positionAttribute.offset(getLayout().getOffset(element));
            }

            return attributeDescriptions.rewind();
        }

        BufferLayout getLayout();

        private static int getFormat(int size) {
            return switch (size) {
                case (2 * Float.BYTES) -> VK_FORMAT_R32G32_SFLOAT;
                case (3 * Float.BYTES) -> VK_FORMAT_R32G32B32_SFLOAT;
                default -> VK_FORMAT_UNDEFINED;
            };
        }

        default VkPipelineVertexInputStateCreateInfo createVertexInputState(MemoryStack stack) {
            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.callocStack(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(getBindingDescription());
            vertexInputInfo.pVertexAttributeDescriptions(getAttributeDescriptions());
            return vertexInputInfo;
        }
    }
}