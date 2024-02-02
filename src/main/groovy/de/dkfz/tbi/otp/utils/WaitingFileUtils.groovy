/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.infrastructure.FileService

/**
 * It looks like exists() is cached (in NFS?). The cache can be cleared by calling canRead() for files and
 * by calling list() (for directories canRead() does not clear the cache in all cases)
 * - at least in the cases that we observed.
 * To make the methods work with both files and directories, both are called.
 */
class WaitingFileUtils {
    /**
     * Waits until the specified file system object exists or the specified timeout elapsed.
     */
    @Deprecated
    static void waitUntilExists(File file) {
        FileService.waitUntilExists(file.toPath())
    }

    /**
     * Waits until the specified file system object does not exist or the specified timeout elapsed.
     */
    @Deprecated
    static void waitUntilDoesNotExist(File file) {
        FileService.waitUntilDoesNotExist(file.toPath())
    }
}
