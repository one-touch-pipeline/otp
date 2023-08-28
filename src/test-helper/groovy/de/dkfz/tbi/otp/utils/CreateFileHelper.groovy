/*
 * Copyright 2011-2019 The OTP authors
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

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@SuppressWarnings('JavaIoPackageAccess') // helper class for FileAccess, so its fine
class CreateFileHelper {

    /**
     * @deprecated use {@link #createFile(Path)}
     */
    @Deprecated
    static File createFile(File file, String content = "some content") {
        if (!file.parentFile.exists()) {
            assert file.parentFile.mkdirs()
        }
        file.text = content
        return file
    }

    static Path createFile(Path file, String content = "some content") {
        if (!Files.exists(file.parent)) {
            assert Files.createDirectories(file.parent)
        }
        file.text = content
        return file
    }

    /**
     * @deprecated use {@link #createSymbolicLinkFile(Path)}
     */
    @Deprecated
    static File createSymbolicLinkFile(File source, File target = new File('/tmp')) {
        createSymbolicLinkFile(source.toPath(), target.toPath())
        return source
    }

    static Path createSymbolicLinkFile(Path source, Path target = Paths.get('/tmp')) {
        Files.createDirectories(source.parent)
        Files.createSymbolicLink(source, target)
        return source
    }

    static void createRoddyWorkflowConfig(File file, String label) {
        createFile(file, FileContentHelper.createXmlContentForRoddyWorkflowConfig(label))
    }
}
