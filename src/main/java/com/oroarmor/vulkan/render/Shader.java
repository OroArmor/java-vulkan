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

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oroarmor.initial.VulkanLogicalDevices;
import com.oroarmor.vulkan.context.VulkanContext;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

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
    protected final Map<Stage, String> stageToSource;
    protected final Map<Stage, SPIRV> stageToCompiled;
    protected final Map<Stage, Long> stageToModule;

    public Shader(VulkanContext context, VulkanRenderer renderer, String shaderFile) {
        this.context = context;
        this.renderer = renderer;
        this.shaderFile = shaderFile;
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
                if (vkCreateShaderModule(context.getLogicalDevice().getDevice(), shaderModuleCreateInfo, null, pShader) != VK_SUCCESS) {
                    throw new RuntimeException("Could not create shader!");
                }
                stageToModule.put(stage, pShader.get(0));
            });
        }
    }

    protected void compileStages() {
        stageToSource.forEach((stage, source) -> {
            long result = shaderc_compile_into_spv(compiler, source, stage.kind, shaderFile, "main", MemoryUtil.NULL);

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
        try {
            String externalUrlToSource = Objects.requireNonNull(getSystemClassLoader().getResource(shaderFile)).toExternalForm();
            String source = new String(Files.readAllBytes(Paths.get(new URI(externalUrlToSource))));
            while (source.contains("#stage")) {
                int startStage = source.indexOf("#stage");
                int endStage = source.indexOf("#stage", startStage + 1) - 1;
                if (endStage < 0) {
                    endStage = source.length();
                }
                String stageSource = source.substring(startStage, endStage);

                Pattern p = Pattern.compile("#stage (\\w*)");
                Matcher m = p.matcher(stageSource);
                if (!m.find()) {
                    throw new RuntimeException("Unable to find stage from shader.");
                }
                String stageType = m.group(1);
                Stage stage = Stage.valueOf(stageType);
                stageToSource.put(stage, stageSource.replace(m.group(), ""));
                source = source.substring(endStage);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Stage, Long> getStageToModule() {
        return stageToModule;
    }

    @Override
    public void close() {
        stageToCompiled.values().forEach(SPIRV::free);
        stageToModule.values().forEach(l -> vkDestroyShaderModule(context.getLogicalDevice().getDevice(), l, null));
    }

    public enum Stage {
        VERTEX_SHADER(shaderc_glsl_vertex_shader),
        GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
        FRAGMENT_SHADER(shaderc_glsl_fragment_shader);

        private final int kind;

        Stage(int kind) {
            this.kind = kind;
        }
    }

    protected static record SPIRV(long handle, ByteBuffer bytecode) implements NativeResource {
        @Override
        public void free() {
            shaderc_result_release(handle);
        }
    }
}