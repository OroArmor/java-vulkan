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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * This class loads text files and returns them as a string.
 *
 * @author OroArmor
 *
 */
public final class ResourceLoader {
	/**
	 *
	 * @param resourceAsStream The input stream for the file
	 * @return The data in the file returned as a string
	 */
	public static String loadFileString(InputStream resourceAsStream) {
		return new String(loadFileBytes(resourceAsStream));
	}

	/**
	 *
	 * @param resourceAsStream The input stream for the file
	 * @return The data in the file returned as a string
	 */
	public static byte[] loadFileBytes(InputStream resourceAsStream) {
		try {
			byte[] fileBytes = new byte[resourceAsStream.available()];
			int readBytes = resourceAsStream.read(fileBytes);
			if(readBytes == -1) {
				throw new Exception("Could not read InputStream");
			}
			resourceAsStream.close();
			return fileBytes;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new byte[0];
	}

	/**
	 *
	 * @param filePath A string to the file
	 * @return The data of the file as a string
	 */
	public static String loadFileString(String filePath) {
		String fileString = "";
		File file = new File(filePath);

		try {
			fileString = loadFileString(new FileInputStream(file));
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

		return fileString;
	}
}
