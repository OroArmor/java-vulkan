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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BufferLayout {
    protected final List<BufferElement> bufferElements = new ArrayList<>();
    protected final Map<BufferElement, Integer> offsets = new HashMap<>();
    protected int stride = 0;

    public List<BufferElement> getBufferElements() {
        return List.copyOf(bufferElements);
    }

    public int getStride() {
        return stride;
    }

    public BufferLayout push(BufferElement element) {
        offsets.put(element, stride);
        bufferElements.add(element);
        stride += element.count * element.size.getSize();
        return this;
    }

    public int getOffset(BufferElement element) {
        return offsets.getOrDefault(element, -1);
    }

    public record BufferElement(int count, Size size, boolean normalized) {
        public static int getSize(Size type) {
            return type.getSize();
        }

        public interface Size {
            int getSize();
        }

        public enum CommonBufferElement implements BufferElement.Size {
            BYTE(Byte.BYTES), CHARACTER(Character.BYTES), SHORT(Short.BYTES), INTEGER(Integer.BYTES), LONG(Long.BYTES),
            FLOAT(Float.BYTES), DOUBLE(Double.BYTES),
            VECTOR_2F(2 * Float.BYTES), VECTOR_3F(3 * Float.BYTES), VECTOR_4F(4 * Float.BYTES),
            MATRIX_4F(VECTOR_4F.size);

            private final int size;

            public int getSize() {
                return size;
            }

            CommonBufferElement(int size) {
                this.size = size;
            }
        }
    }
}
